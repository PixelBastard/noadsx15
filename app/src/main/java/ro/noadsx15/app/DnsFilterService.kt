package ro.noadsx15.app

import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.*
import java.io.*
import java.net.*
import java.util.concurrent.atomic.AtomicLong

class DnsFilterService : VpnService() {
    companion object {
        const val START="ro.noadsx15.START"; const val STOP="ro.noadsx15.STOP"; const val STATUS="ro.noadsx15.STATUS"
        @Volatile var running=false
        val blocked=AtomicLong(0); val allowed=AtomicLong(0)
    }
    private var descriptor: ParcelFileDescriptor?=null
    private var worker: Thread?=null
    private lateinit var rules: BlocklistStore

    override fun onCreate(){super.onCreate();rules=BlocklistStore(this);channel()}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        if(intent?.action==STOP){shutdown();return START_NOT_STICKY}
        if(running)return START_STICKY
        startForeground(30,notification("Pornire…"))
        return try{
            descriptor=Builder().setSession("NoAds X15 3.0").setMtu(1500).addAddress("10.111.222.2",32).addRoute("10.111.222.1",32).addDnsServer("10.111.222.1").establish()?:error("Tunel indisponibil")
            running=true;worker=Thread({loop()},"NoAdsDNS").also{it.start()};broadcast("Activ • ${rules.count()} domenii");START_STICKY
        }catch(e:Throwable){broadcast("Eroare: ${e.message}");shutdown();START_NOT_STICKY}
    }
    private fun loop(){val fd=descriptor?:return;try{FileInputStream(fd.fileDescriptor).use{input->FileOutputStream(fd.fileDescriptor).use{output->val buffer=ByteArray(65535);while(running){val size=input.read(buffer);if(size>0)process(buffer,size)?.let{output.write(it);output.flush()}}}}}catch(_:Throwable){if(running)shutdown()}}
    private fun process(packet:ByteArray,size:Int):ByteArray?{
        if(size<28||(packet[0].toInt() and 0xF0)!=0x40)return null
        val ihl=(packet[0].toInt() and 15)*4;if(ihl<20||size<ihl+8||(packet[9].toInt() and 255)!=17)return null
        val dstPort=u16(packet,ihl+2);if(dstPort!=53)return null
        val query=packet.copyOfRange(ihl+8,size);val host=DnsPacket.host(query)?.lowercase()?:return null
        val dns=if(rules.isBlocked(host)){blocked.incrementAndGet();DnsPacket.nxdomain(query)}else{allowed.incrementAndGet();forward(query)?:return null}
        if((blocked.get()+allowed.get())%25L==0L)broadcast("Blocate ${blocked.get()} • Permise ${allowed.get()}")
        return reply(packet,ihl,dns,u16(packet,ihl))
    }
    private fun forward(query:ByteArray):ByteArray?{
        val servers=arrayOf("1.1.1.1","9.9.9.9")
        for(server in servers){var socket:DatagramSocket?=null;try{socket=DatagramSocket();if(!protect(socket))continue;socket.soTimeout=2200;socket.send(DatagramPacket(query,query.size,InetAddress.getByName(server),53));val b=ByteArray(4096);val p=DatagramPacket(b,b.size);socket.receive(p);return b.copyOf(p.length)}catch(_:Throwable){}finally{socket?.close()}}
        return null
    }
    private fun reply(req:ByteArray,ihl:Int,dns:ByteArray,clientPort:Int):ByteArray{
        val total=28+dns.size;val out=ByteArray(total);out[0]=0x45;put16(out,2,total);out[4]=req[4];out[5]=req[5];out[8]=64;out[9]=17
        System.arraycopy(req,16,out,12,4);System.arraycopy(req,12,out,16,4);put16(out,20,53);put16(out,22,clientPort);put16(out,24,8+dns.size);System.arraycopy(dns,0,out,28,dns.size);put16(out,10,checksum(out,0,20));return out
    }
    private fun u16(b:ByteArray,i:Int)=((b[i].toInt() and 255) shl 8) or (b[i+1].toInt() and 255)
    private fun put16(b:ByteArray,i:Int,v:Int){b[i]=(v ushr 8).toByte();b[i+1]=v.toByte()}
    private fun checksum(b:ByteArray,o:Int,l:Int):Int{var s=0L;var i=o;while(i<o+l-1){s+=u16(b,i);i+=2};if(l%2==1)s+=(b[o+l-1].toInt() and 255) shl 8;while(s ushr 16!=0L)s=(s and 65535)+(s ushr 16);return s.inv().toInt() and 65535}
    private fun broadcast(message:String)=sendBroadcast(Intent(STATUS).setPackage(packageName).putExtra("message",message))
    private fun shutdown(){running=false;try{descriptor?.close()}catch(_:Throwable){};descriptor=null;worker?.interrupt();worker=null;broadcast("Oprit");if(Build.VERSION.SDK_INT>=24)stopForeground(STOP_FOREGROUND_REMOVE)else @Suppress("DEPRECATION") stopForeground(true);stopSelf()}
    override fun onDestroy(){running=false;try{descriptor?.close()}catch(_:Throwable){};worker?.interrupt();super.onDestroy()}
    override fun onRevoke(){shutdown();super.onRevoke()}
    private fun channel(){if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("noads","NoAds X15",NotificationManager.IMPORTANCE_LOW))}
    private fun notification(text:String)=Notification.Builder(this,"noads").setContentTitle("NoAds X15 3.0").setContentText(text).setSmallIcon(android.R.drawable.ic_secure).setOngoing(true).build()
}
