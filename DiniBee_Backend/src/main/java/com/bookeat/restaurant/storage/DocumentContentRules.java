package com.bookeat.restaurant.storage;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
public final class DocumentContentRules {
    public static final int MAX_BYTES=5*1024*1024;
    private static final Pattern KEY=Pattern.compile("restaurants/[0-9a-f-]{36}/documents/[0-9a-f-]{36}");
    private DocumentContentRules() {}
    public static String detect(byte[] data) {
        if(data.length==0 || data.length>MAX_BYTES) throw new IllegalArgumentException("Invalid file size");
        if(data.length>=5 && new String(data,0,5,StandardCharsets.US_ASCII).equals("%PDF-")) return "application/pdf";
        int[] png={137,80,78,71,13,10,26,10};
        boolean match=data.length>=8;
        for(int i=0;match && i<8;i++) match=(data[i]&255)==png[i];
        if(match) return "image/png";
        if(data.length>=3 && (data[0]&255)==255 && (data[1]&255)==216 && (data[2]&255)==255) return "image/jpeg";
        throw new IllegalArgumentException("Only PDF, PNG and JPEG documents are supported");
    }
    public static String safeFilename(String name) {
        if(name==null) return "document";
        String safe=name.replace('\\','/'); safe=safe.substring(safe.lastIndexOf('/')+1).replaceAll("[^A-Za-z0-9._-]","_");
        return safe.isBlank()?"document":safe.substring(0,Math.min(safe.length(),150));
    }
    public static void validateKey(String key) {
        if(key==null || !KEY.matcher(key).matches()) throw new IllegalArgumentException("Invalid document key");
    }
}
