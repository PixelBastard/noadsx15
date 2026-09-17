package ro.noadsx15.hybrid;

import java.util.Locale;
import java.util.Set;

public final class DomainRules {
    private DomainRules() {}
    public static String normalize(String s) { return s == null ? "" : s.trim().replaceAll("^\.+|\.+$", "").toLowerCase(Locale.US); }
    public static String parse(String line) {
        if (line == null) return null;
        String v=line.trim();
        if(v.isEmpty()||v.startsWith("#")||v.startsWith("!")||v.startsWith("@@"))return null;
        if(v.startsWith("||"))v=v.substring(2);
        int dollar=v.indexOf('$');if(dollar>=0)v=v.substring(0,dollar);
        while(v.endsWith("^")||v.endsWith("|"))v=v.substring(0,v.length()-1);
        if(v.startsWith("0.0.0.0 ")||v.startsWith("127.0.0.1 "))v=v.substring(v.indexOf(' ')+1).trim();
        if(v.contains("/")||v.contains("*")||v.contains(" ")||!v.contains("."))return null;
        v=normalize(v);return v.matches("[a-z0-9._-]+")?v:null;
    }
    public static boolean matches(String host, Set<String> rules) {
        String c=normalize(host);while(!c.isEmpty()){if(rules.contains(c))return true;int dot=c.indexOf('.');if(dot<0)return false;c=c.substring(dot+1);}return false;
    }
}
