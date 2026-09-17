package ro.noadsx15.app

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.VpnService
import android.os.*
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.net.InetAddress
import java.text.DateFormat
import java.util.Date

class MainActivity:Activity(){
 private lateinit var rules:BlocklistStore;private lateinit var badge:TextView;private lateinit var info:TextView;private lateinit var update:Button
 private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){render(i?.getStringExtra("message"))}}
 override fun onCreate(b:Bundle?){super.onCreate(b);rules=BlocklistStore(this);if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),1);setContentView(ui());render(null)}
 override fun onStart(){super.onStart();val f=IntentFilter(DnsFilterService.STATUS);if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,RECEIVER_NOT_EXPORTED)else @Suppress("DEPRECATION") registerReceiver(receiver,f)}
 override fun onStop(){try{unregisterReceiver(receiver)}catch(_:Throwable){};super.onStop()}
 private fun ui():View{val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(38),dp(22),dp(28));setBackgroundColor(Color.rgb(248,250,252))}
  root.addView(TextView(this).apply{text="NoAds X15";textSize=32f;gravity=Gravity.CENTER;setTextColor(Color.rgb(15,23,42))},lp(-1,60))
  root.addView(TextView(this).apply{text="Standalone 3.0 • filtrare globală DNS";textSize=15f;gravity=Gravity.CENTER;setTextColor(Color.rgb(71,85,105))},lp(-1,42))
  badge=TextView(this).apply{textSize=18f;gravity=Gravity.CENTER;setTextColor(Color.WHITE)};info=TextView(this).apply{textSize=14f;gravity=Gravity.CENTER;setPadding(0,dp(12),0,dp(12));setTextColor(Color.rgb(71,85,105))};root.addView(badge,lp(-1,52));root.addView(info,lp(-1,76))
  root.addView(btn("Activează protecția",Color.rgb(37,99,235)){startProtection()},lp(-1,54));root.addView(btn("Oprește protecția",Color.rgb(220,38,38)){stopProtection()},lp(-1,54))
  update=btn("Actualizează listele",Color.rgb(15,118,110)){updateLists()};root.addView(update,lp(-1,54));root.addView(btn("Testează DNS",Color.rgb(124,58,237)){testDns()},lp(-1,54));root.addView(btn("Setări VPN Android",Color.rgb(71,85,105)){startActivity(Intent(Settings.ACTION_VPN_SETTINGS))},lp(-1,54))
  val custom=EditText(this).apply{hint="domeniu pentru blocare/permitere";setSingleLine();background=round(Color.WHITE,12);setPadding(dp(14),0,dp(14),0)};root.addView(custom,lp(-1,54))
  val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};row.addView(btn("Blochează",Color.rgb(185,28,28)){runCatching{rules.addBlocked(custom.text.toString());render("Regulă de blocare adăugată")}.onFailure{toast("Domeniu invalid")}},LinearLayout.LayoutParams(0,dp(52),1f));row.addView(btn("Permite",Color.rgb(22,163,74)){runCatching{rules.addAllowed(custom.text.toString());render("Excepție adăugată")}.onFailure{toast("Domeniu invalid")}},LinearLayout.LayoutParams(0,dp(52),1f));root.addView(row)
  root.addView(TextView(this).apply{text="Blochează reclame, trackere și multe domenii de popup în browsere și aplicații. Nu poate elimina elementele vizuale same-origin sau reclamele YouTube integrate în video.";textSize=13f;setTextColor(Color.rgb(100,116,139));setPadding(0,dp(18),0,0)})
  return ScrollView(this).apply{addView(root)}}
 private fun startProtection(){val i=VpnService.prepare(this);if(i!=null)startActivityForResult(i,10)else startNow()}
 private fun startNow(){val i=Intent(this,DnsFilterService::class.java).setAction(DnsFilterService.START);if(Build.VERSION.SDK_INT>=26)startForegroundService(i)else startService(i);render("Pornire…")}
 private fun stopProtection(){startService(Intent(this,DnsFilterService::class.java).setAction(DnsFilterService.STOP));stopService(Intent(this,DnsFilterService::class.java));render("Oprit")}
 override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(r==10&&c==RESULT_OK)startNow()}
 private fun updateLists(){if(DnsFilterService.running){toast("Oprește protecția înainte de actualizare");return};update.isEnabled=false;info.text="Pregătesc actualizarea…";Thread{val result=runCatching{rules.update{msg->runOnUiThread{info.text=msg}}};runOnUiThread{update.isEnabled=true;result.onSuccess{render("Liste actualizate: $it domenii")}.onFailure{render("Actualizarea a eșuat: ${it.message}")}}}.start()}
 private fun testDns(){info.text="Testez…";Thread{val msg=try{"DNS OK: ${InetAddress.getByName("example.com").hostAddress}"}catch(e:Throwable){"DNS eșuat: ${e.javaClass.simpleName}"};runOnUiThread{info.text=msg}}.start()}
 private fun render(message:String?){val on=DnsFilterService.running;badge.text=if(on)"ACTIV" else "OPRIT";badge.background=round(if(on)Color.rgb(22,163,74)else Color.rgb(100,116,139),22);val last=getSharedPreferences("state",MODE_PRIVATE).getLong("last_update",0);val date=if(last==0L)"niciodată" else DateFormat.getDateTimeInstance().format(Date(last));info.text=message?:"${rules.count()} domenii • update: $date
Blocate ${DnsFilterService.blocked.get()} • Permise ${DnsFilterService.allowed.get()}"}
 private fun btn(t:String,c:Int,a:()->Unit)=Button(this).apply{text=t;isAllCaps=false;textSize=15f;setTextColor(Color.WHITE);background=round(c,13);setOnClickListener{a()}}
 private fun round(c:Int,r:Int)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat()};private fun lp(w:Int,h:Int)=LinearLayout.LayoutParams(w,dp(h)).apply{setMargins(0,dp(5),0,dp(5))};private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt();private fun toast(t:String)=Toast.makeText(this,t,Toast.LENGTH_SHORT).show()
}
