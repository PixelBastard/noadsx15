package ro.noadsx15.hybrid;

import java.nio.charset.StandardCharsets;
import java.util.*;

public final class DnsPacket {
 private DnsPacket(){}
 public static String host(byte[] q){if(q.length<17)return null;int i=12;List<String> labels=new ArrayList<>();while(i<q.length){int n=q[i++]&255;if(n==0)break;if(n>63||i+n>q.length)return null;labels.add(new String(q,i,n,StandardCharsets.US_ASCII));i+=n;}return labels.isEmpty()?null:String.join(".",labels);}
 public static int questionEnd(byte[]q){int i=12;while(i<q.length){int n=q[i++]&255;if(n==0)return Math.min(i+4,q.length);if(n>63||i+n>q.length)return-1;i+=n;}return-1;}
 public static byte[] nxdomain(byte[]q){int e=questionEnd(q);byte[]r=Arrays.copyOf(q,e>0?e:q.length);if(r.length>=12){r[2]=(byte)0x81;r[3]=(byte)0x83;r[4]=0;r[5]=1;for(int i=6;i<=11;i++)r[i]=0;}return r;}
}
