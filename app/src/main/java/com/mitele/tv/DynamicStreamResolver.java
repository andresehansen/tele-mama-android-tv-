package com.mitele.tv;

import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DynamicStreamResolver:
 * Extractor genérico de flujos HLS protegidos por token de sesión temporal.
 * No contiene dominios hardcodeados en el código fuente.
 */
public class DynamicStreamResolver {

    private static final String TAG = "DynamicStreamResolver";
    private static final String CHROME_DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 8000;

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    // Patrones Regex genéricos
    private static final Pattern PATTERN_EMBED2 = Pattern.compile("https?://[^\\s\"'<>]*/embed2/[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_STREAM_PHP = Pattern.compile("https?://[^\\s\"'<>]*/stream\\.php\\?[^\\s\"'<>]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_STREAM_URL = Pattern.compile("const\\s+streamUrl\\s*=\\s*[\"'](https?://[^\\s\"']+\\.m3u8\\?token=[^\\s\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_FALLBACK_M3U8 = Pattern.compile("[\"'](https?://[^\\s\"']+\\.m3u8\\?token=[^\\s\"']+)[\"']", Pattern.CASE_INSENSITIVE);

    public static class StreamResult {
        public final boolean success;
        public final String m3u8Url;
        public final String headersJson;
        public final boolean isFallbackWeb;
        public final String error;

        private StreamResult(boolean success, String m3u8Url, String headersJson, boolean isFallbackWeb, String error) {
            this.success = success;
            this.m3u8Url = m3u8Url;
            this.headersJson = headersJson;
            this.isFallbackWeb = isFallbackWeb;
            this.error = error;
        }

        public static StreamResult success(String m3u8Url, String headersJson) {
            return new StreamResult(true, m3u8Url, headersJson, false, null);
        }

        public static StreamResult fallbackWeb(String reason) {
            return new StreamResult(false, null, null, true, reason);
        }

        public static StreamResult failure(String error) {
            return new StreamResult(false, null, null, false, error);
        }
    }

    public interface ResolveCallback {
        void onResolved(StreamResult result);
    }

    public static void resolveStreamAsync(final String rawUrl, final String referer, final String type, final ResolveCallback callback) {
        EXECUTOR.execute(() -> {
            try {
                StreamResult result = resolveStreamSync(rawUrl, referer, type);
                if (callback != null) {
                    callback.onResolved(result);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error resolviendo stream dinámico: " + t.getMessage(), t);
                if (callback != null) {
                    callback.onResolved(StreamResult.failure("Excepción: " + t.getMessage()));
                }
            }
        });
    }

    private static StreamResult resolveStreamSync(String rawUrl, String referer, String type) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) {
            return StreamResult.failure("URL vacía");
        }

        String targetEmbedUrl = rawUrl.trim();
        String currentReferer = (referer != null && !referer.trim().isEmpty()) ? referer.trim() : rawUrl;

        // Si es tipo web_bridge, primero extraemos el embed interno si existe
        if ("web_bridge".equalsIgnoreCase(type)) {
            Log.d(TAG, "Procesando fuente web_bridge: " + rawUrl);
            String bridgeHtml = fetchHtml(rawUrl, currentReferer);
            if (bridgeHtml == null || bridgeHtml.isEmpty()) {
                return StreamResult.fallbackWeb("No se pudo obtener el contenido de la página puente");
            }

            Matcher mEmbed = PATTERN_EMBED2.matcher(bridgeHtml);
            if (mEmbed.find()) {
                targetEmbedUrl = mEmbed.group(0).replace("&amp;", "&");
                currentReferer = rawUrl;
                Log.d(TAG, "Embed2 encontrado en puente: " + targetEmbedUrl);
            } else {
                // Si la página puente no expone embed directo, cae a WebView seguro
                Log.d(TAG, "Página puente sin embed2 directo, usando WebView de respaldo");
                return StreamResult.fallbackWeb("Página puente sin extractor directo");
            }
        }

        // Paso 1: Hacer GET a targetEmbedUrl para extraer la URL de stream.php
        Log.d(TAG, "Paso 1: Solicitando embed: " + targetEmbedUrl);
        String embedHtml = fetchHtml(targetEmbedUrl, currentReferer);
        if (embedHtml == null || embedHtml.isEmpty()) {
            return StreamResult.failure("Error obteniendo HTML del embed (Paso 1)");
        }

        Matcher mStreamPhp = PATTERN_STREAM_PHP.matcher(embedHtml);
        if (!mStreamPhp.find()) {
            return StreamResult.failure("No se encontró stream.php en el embed");
        }
        String streamPhpUrl = mStreamPhp.group(0).replace("&amp;", "&");
        Log.d(TAG, "Paso 1 exitoso. stream.php: " + streamPhpUrl);

        // Paso 2: Hacer GET a streamPhpUrl con Referer = targetEmbedUrl
        Log.d(TAG, "Paso 2: Solicitando stream.php: " + streamPhpUrl);
        String streamPhpHtml = fetchHtml(streamPhpUrl, targetEmbedUrl);
        if (streamPhpHtml == null || streamPhpHtml.isEmpty()) {
            return StreamResult.failure("Error obteniendo HTML de stream.php (Paso 2)");
        }

        Matcher mStreamUrl = PATTERN_STREAM_URL.matcher(streamPhpHtml);
        String finalM3u8Url = null;
        if (mStreamUrl.find()) {
            finalM3u8Url = mStreamUrl.group(1);
        } else {
            Matcher mFallback = PATTERN_FALLBACK_M3U8.matcher(streamPhpHtml);
            if (mFallback.find()) {
                finalM3u8Url = mFallback.group(1);
            }
        }

        if (finalM3u8Url == null || finalM3u8Url.isEmpty()) {
            return StreamResult.failure("No se encontró streamUrl m3u8 con token en stream.php");
        }

        Log.d(TAG, "Paso 2 exitoso. m3u8 obtenido: " + finalM3u8Url);

        // Paso 3: Construir headers Referer y Origin para ExoPlayer
        try {
            Uri parsed = Uri.parse(streamPhpUrl);
            String origin = parsed.getScheme() + "://" + parsed.getAuthority();
            String streamReferer = origin + "/";

            JSONObject headers = new JSONObject();
            headers.put("Referer", streamReferer);
            headers.put("Origin", origin);

            return StreamResult.success(finalM3u8Url, headers.toString());
        } catch (Throwable t) {
            return StreamResult.success(finalM3u8Url, "{}");
        }
    }

    private static String fetchHtml(String targetUrl, String referer) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setInstanceFollowRedirects(true);

            conn.setRequestProperty("User-Agent", CHROME_DESKTOP_UA);
            if (referer != null && !referer.isEmpty()) {
                conn.setRequestProperty("Referer", referer);
            }
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "es-ES,es;q=0.9,en;q=0.8");

            int code = conn.getResponseCode();
            if (code >= 300 && code < 400) {
                String redirect = conn.getHeaderField("Location");
                if (redirect != null && !redirect.isEmpty()) {
                    return fetchHtml(redirect, targetUrl);
                }
            }

            if (code != 200) {
                Log.w(TAG, "HTTP " + code + " al solicitar " + targetUrl);
                return null;
            }

            InputStream in = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            reader.close();
            return sb.toString();
        } catch (Throwable t) {
            Log.w(TAG, "Error en fetchHtml para " + targetUrl + ": " + t.getMessage());
            return null;
        } finally {
            if (conn != null) {
                try { conn.disconnect(); } catch (Throwable ignored) {}
            }
        }
    }
}
