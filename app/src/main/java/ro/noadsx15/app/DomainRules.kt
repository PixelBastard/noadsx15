package ro.noadsx15.app

object DomainRules {
    fun normalize(value: String): String = value.trim().trim('.').lowercase()

    fun parseLine(line: String): Pair<Boolean, String>? {
        var value = line.trim()
        if (value.isEmpty() || value.startsWith("!") || value.startsWith("#")) return null
        val allow = value.startsWith("@@")
        if (allow) value = value.removePrefix("@@")
        value = value.substringBefore('$').trim()
        if (value.startsWith("||")) value = value.removePrefix("||")
        value = value.removeSuffix("^").removePrefix("|").removeSuffix("|")
        if (value.startsWith("0.0.0.0 ") || value.startsWith("127.0.0.1 ")) value = value.substringAfter(' ')
        if (value.contains('/') || value.contains('*') || value.contains(' ') || !value.contains('.')) return null
        val host = normalize(value)
        if (!host.matches(Regex("[a-z0-9._-]+"))) return null
        return allow to host
    }

    fun matches(host: String, rules: Set<String>): Boolean {
        var candidate = normalize(host)
        while (true) {
            if (candidate in rules) return true
            val dot = candidate.indexOf('.')
            if (dot < 0) return false
            candidate = candidate.substring(dot + 1)
        }
    }
}
