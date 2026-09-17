package ro.noadsx15.app

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

class BlocklistStore(private val context: Context) {
    companion object {
        private const val BLOCK_FILE = "blocked-domains.txt"
        private const val ALLOW_FILE = "allowed-domains.txt"
        private val SOURCES = listOf(
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/pro.mini-onlydomains.txt",
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/popupads-onlydomains.txt",
            "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/filter.txt",
            "https://adguardteam.github.io/AdGuardSDNSFilter/Filters/adguard_popup_filter.txt"
        )
        private val FALLBACK = setOf(
            "doubleclick.net","googlesyndication.com","googleadservices.com","adnxs.com","adsrvr.org",
            "amazon-adsystem.com","applovin.com","unityads.unity3d.com","unity3d.com","criteo.com",
            "taboola.com","outbrain.com","scorecardresearch.com","app-measurement.com","appsflyer.com",
            "adjust.com","branch.io","moatads.com","pubmatic.com","rubiconproject.com","openx.net"
        )
    }
    private val blockedRef = AtomicReference<Set<String>>(FALLBACK)
    private val allowedRef = AtomicReference<Set<String>>(emptySet())

    init { reload() }

    fun reload() {
        val blockedFile = File(context.filesDir, BLOCK_FILE)
        blockedRef.set(if (blockedFile.exists()) blockedFile.readLines().map(DomainRules::normalize).filter { it.contains('.') }.toHashSet() else FALLBACK)
        val allowFile = File(context.filesDir, ALLOW_FILE)
        allowedRef.set(if (allowFile.exists()) allowFile.readLines().map(DomainRules::normalize).filter { it.contains('.') }.toHashSet() else emptySet())
    }

    fun isBlocked(host: String): Boolean {
        if (DomainRules.matches(host, allowedRef.get())) return false
        return DomainRules.matches(host, blockedRef.get())
    }

    fun count(): Int = blockedRef.get().size
    fun allowCount(): Int = allowedRef.get().size

    fun addAllowed(host: String) {
        val n = DomainRules.normalize(host)
        if (!n.contains('.')) throw IllegalArgumentException("Domeniu invalid")
        val next = allowedRef.get().toMutableSet().apply { add(n) }
        atomicWrite(File(context.filesDir, ALLOW_FILE), next.sorted().joinToString("
"))
        allowedRef.set(next)
    }

    fun addBlocked(host: String) {
        val n = DomainRules.normalize(host)
        if (!n.contains('.')) throw IllegalArgumentException("Domeniu invalid")
        val next = blockedRef.get().toMutableSet().apply { add(n) }
        atomicWrite(File(context.filesDir, BLOCK_FILE), next.sorted().joinToString("
"))
        blockedRef.set(next)
    }

    fun update(progress: (String) -> Unit): Int {
        val blocked = HashSet<String>(250_000)
        val allowed = HashSet<String>()
        for ((index, source) in SOURCES.withIndex()) {
            progress("Descarc lista ${index + 1}/${SOURCES.size}…")
            val connection = URL(source).openConnection() as HttpURLConnection
            connection.connectTimeout = 15_000; connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "NoAdsX15/3.0")
            connection.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> DomainRules.parseLine(line)?.let { (allow, host) -> if (allow) allowed.add(host) else blocked.add(host) } }
            }
            connection.disconnect()
        }
        if (blocked.size < 20_000) throw IllegalStateException("Listele descărcate sunt prea mici: ${blocked.size}")
        val finalBlocked = blocked.filterNot { DomainRules.matches(it, allowed) }.toHashSet()
        atomicWrite(File(context.filesDir, BLOCK_FILE), finalBlocked.sorted().joinToString("
"))
        atomicWrite(File(context.filesDir, ALLOW_FILE), allowed.sorted().joinToString("
"))
        blockedRef.set(finalBlocked); allowedRef.set(allowed)
        context.getSharedPreferences("state", Context.MODE_PRIVATE).edit().putLong("last_update", System.currentTimeMillis()).apply()
        return finalBlocked.size
    }

    private fun atomicWrite(target: File, content: String) {
        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeText(content)
        if (!temp.renameTo(target)) { target.writeText(content); temp.delete() }
    }
}
