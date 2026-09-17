package ro.noadsx15.hybrid;

import android.content.Context;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public final class BlocklistStore {
    private static final String[] SOURCES={
        "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/pro.mini-onlydomains.txt",
        "https://raw.githubusercontent.com/hagezi/dns-blocklists/main/wildcard/popupads-onlydomains.txt"
    };
    private static final Set<String> FALLBACK=new HashSet<>(Arrays.asList("doubleclick.net","googlesyndication.com","googleadservices.com","adnxs.com","adsrvr.org","amazon-adsystem.com","applovin.com","unity3d.com","criteo.com","taboola.com","outbrain.com","pubmatic.com","rubiconproject.com","openx.net"));
    private final Context context; private final AtomicReference<Set<String>> blocked=new AtomicReference<>(FALLBACK); private final AtomicReference<Set<String>> allowed=new AtomicReference<>(Collections.emptySet());
    public BlocklistStore(Context c){context=c.getApplicationContext();reload();}
    public void reload(){blocked.set(read(new File(context.getFilesDir(),"blocked.txt"),FALLBACK));allowed.set(read(new File(context.getFilesDir(),"allowed.txt"),Collections.emptySet()));}
    private Set<String> read(File f,Set<String> fallback){if(!f.exists())return new HashSet<>(fallback);Set<String>s=new HashSet<>();try(BufferedReader r=new BufferedReader(new FileReader(f))){String l;while((l=r.readLine())!=null){String d=DomainRules.normalize(l);if(d.contains("."))s.add(d);}}catch(IOException ignored){}return s.isEmpty()?new HashSet<>(fallback):s;}
    public boolean isBlocked(String host){return !DomainRules.matches(host,allowed.get())&&DomainRules.matches(host,blocked.get());}
    public int size(){return blocked.get().size();}
    public int update(Progress progress)throws Exception{Set<String> next=new HashSet<>(250000);int ok=0;for(int i=0;i<SOURCES.length;i++){progress.onProgress("Descarc lista "+(i+1)+"/"+SOURCES.length+"…");HttpURLConnection c=null;try{c=(HttpURLConnection)new URL(SOURCES[i]).openConnection();c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","NoAdsX15/4.0");if(c.getResponseCode()<200||c.getResponseCode()>299)continue;try(BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()))){String l;while((l=r.readLine())!=null){String d=DomainRules.parse(l);if(d!=null)next.add(d);}}ok++;}finally{if(c!=null)c.disconnect();}}if(ok==0||next.size()<10000)throw new IOException("Liste insuficiente: "+next.size());writeAtomic(new File(context.getFilesDir(),"blocked.txt"),next);blocked.set(next);context.getSharedPreferences("state",0).edit().putLong("updated",System.currentTimeMillis()).apply();return next.size();}
    private void writeAtomic(File target,Set<String> values)throws IOException{File temp=new File(target.getParentFile(),target.getName()+".tmp");try(BufferedWriter w=new BufferedWriter(new FileWriter(temp))){for(String v:new TreeSet<>(values)){w.write(v);w.newLine();}}if(!temp.renameTo(target)){try(BufferedReader r=new BufferedReader(new FileReader(temp));BufferedWriter w=new BufferedWriter(new FileWriter(target))){String l;while((l=r.readLine())!=null){w.write(l);w.newLine();}}temp.delete();}}
    public interface Progress{void onProgress(String message);}
}
