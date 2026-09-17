package ro.noadsx15.app

object DnsPacket {
    fun host(query: ByteArray): String? {
        if (query.size < 17) return null
        var i = 12; val labels = ArrayList<String>()
        while (i < query.size) {
            val length = query[i++].toInt() and 255
            if (length == 0) break
            if (length > 63 || i + length > query.size) return null
            labels.add(String(query, i, length, Charsets.US_ASCII)); i += length
        }
        return labels.joinToString(".").takeIf { it.isNotEmpty() }
    }

    fun questionEnd(query: ByteArray): Int {
        var i=12
        while(i<query.size){ val n=query[i++].toInt() and 255; if(n==0)return (i+4).coerceAtMost(query.size); if(n>63||i+n>query.size)return -1;i+=n }
        return -1
    }

    fun nxdomain(query: ByteArray): ByteArray {
        val end=questionEnd(query); val response=query.copyOf(if(end>0)end else query.size)
        if(response.size>=12){response[2]=0x81.toByte();response[3]=0x83.toByte();response[4]=0;response[5]=1;for(i in 6..11)response[i]=0}
        return response
    }
}
