package top.ahmadb.gregicalityutils.mixin.gregicadditions;

import gregicadditions.item.behaviors.monitorPlugin.onlinepic.DownloadThread;
import gregicadditions.item.behaviors.monitorPlugin.onlinepic.TextureCache;
import org.apache.commons.compress.utils.IOUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

@Mixin(value = DownloadThread.class, remap = false)
public abstract class MixinDownloadThread {

    @Shadow public static TextureCache TEXTURE_CACHE;
    @Shadow public static DateFormat FORMAT;

    /**
     * @author ahmadb
     * @reason Fix NullPointerException on HTTP 304 Not Modified responses when loading cached pictures.
     */
    @Overwrite
    public static byte[] load(String url) throws IOException, DownloadThread.FoundVideoException {
        TextureCache.CacheEntry entry = TEXTURE_CACHE.getEntry(url);
        long requestTime = System.currentTimeMillis();
        URLConnection connection = new URL(url).openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        int responseCode = -1;
        
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            if (entry != null) {
                if (entry.getEtag() != null) {
                    httpConnection.setRequestProperty("If-None-Match", entry.getEtag());
                } else if (entry.getTime() != -1) {
                    httpConnection.setRequestProperty("If-Modified-Since", FORMAT.format(new Date(entry.getTime())));
                }
            }
            responseCode = httpConnection.getResponseCode();
        }

        // FIX 1: Handle HTTP 304 Not Modified IMMEDIATELY before attempting to read headers/streams
        if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED && entry != null) {
            File file = entry.getFile();
            if (file.exists()) {
                try (FileInputStream fileStream = new FileInputStream(file)) {
                    return IOUtils.toByteArray(fileStream);
                }
            }
        }

        InputStream in = null;
        try {
            in = connection.getInputStream();
            
            // FIX 2: Null-check getContentType() to prevent NPE if the header is missing
            String contentType = connection.getContentType();
            if (contentType != null && !contentType.startsWith("image")) {
                throw new DownloadThread.FoundVideoException();
            }

            String etag = connection.getHeaderField("ETag");
            long lastModifiedTimestamp;
            long expireTimestamp = -1;
            String maxAge = connection.getHeaderField("max-age");
            if (maxAge != null && !maxAge.isEmpty()) {
                try {
                    expireTimestamp = requestTime + Long.parseLong(maxAge) * 1000;
                } catch (NumberFormatException ignored) {}
            }
            String expires = connection.getHeaderField("Expires");
            if (expires != null && !expires.isEmpty()) {
                try {
                    expireTimestamp = FORMAT.parse(expires).getTime();
                } catch (ParseException ignored) {}
            }
            String lastModified = connection.getHeaderField("Last-Modified");
            if (lastModified != null && !lastModified.isEmpty()) {
                try {
                    lastModifiedTimestamp = FORMAT.parse(lastModified).getTime();
                } catch (ParseException e) {
                    lastModifiedTimestamp = requestTime;
                }
            } else {
                lastModifiedTimestamp = requestTime;
            }
            
            if (entry != null) {
                if (etag != null && !etag.isEmpty()) {
                    entry.setEtag(etag);
                }
                entry.setTime(lastModifiedTimestamp);
            }
            
            byte[] data = IOUtils.toByteArray(in);
            TEXTURE_CACHE.save(url, etag, lastModifiedTimestamp, expireTimestamp, data);
            return data;
        } finally {
            IOUtils.closeQuietly(in);
        }
    }
}