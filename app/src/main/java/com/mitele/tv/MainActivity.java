package com.mitele.tv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.util.Rational;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

@OptIn(markerClass = UnstableApi.class)
public class MainActivity extends Activity {

    private static final String TAG = "AndyTV";
    // URL remota en GitHub Pages: permite que la app se actualice sola en la TV sin reinstalar el APK
    private static final String REMOTE_APP_URL = "https://andresehansen.github.io/tele-mama-android-tv-/";
    private static final String LOCAL_FALLBACK_URL = "file:///android_asset/www/index.html";
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36";

    // Lista ligera de dominios de anuncios y popups bloqueados a nivel nativo
    private static final String[] AD_BLOCK_DOMAINS = {
        "doubleclick.net", "googlesyndication.com", "googleadservices.com",
        "popads.net", "popcash.net", "propellerads.com", "adnxs.com",
        "exoclick.com", "juicyads.com", "realsrv.com", "histats.com",
        "acscdn.com", "usrpubtrk.com", "clickadu.com", "adsterra.com",
        "hilltopads.net", "trafficjunky.net", "zedo.com", "outbrain.com",
        "taboola.com", "yandex.ru/ads", "mc.yandex.ru"
    };

    private FrameLayout rootLayout;
    private PlayerView exoPlayerView;
    private ExoPlayer exoPlayer;
    private WebView webView;
    private long lastBackPressTime = 0;
    private boolean usingFallback = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Manejador global de excepciones para evitar cierres silenciosos
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "FATAL CRASH DETECTED", throwable);
            runOnUiThread(() -> showErrorScreen("Error Inesperado en AndyTV", throwable));
        });

        try {
            // Mantener la pantalla encendida siempre que la tele esté en la app
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            // Contenedor raíz que aloja el reproductor ExoPlayer debajo y el WebView encima
            rootLayout = new FrameLayout(this);
            rootLayout.setBackgroundColor(Color.BLACK);

            // Reproductor nativo Media3 ExoPlayer (baja latencia, soporte HLS/DASH, audio AC3/EAC3/AAC y headers HTTP)
            exoPlayerView = new PlayerView(this);
            FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            );
            exoPlayerView.setLayoutParams(videoParams);
            exoPlayerView.setUseController(false);
            exoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            exoPlayerView.setVisibility(View.GONE);

            initExoPlayer(null);

            // Crear el WebView principal encima del PlayerView
            webView = new WebView(this);
            FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            webView.setLayoutParams(webParams);
            webView.setBackgroundColor(Color.parseColor("#080c14"));

            rootLayout.addView(exoPlayerView);
            rootLayout.addView(webView);
            setContentView(rootLayout);

            // Ocultar barras de sistema de manera segura (después de setContentView)
            safeHideSystemUI();

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setDatabaseEnabled(true);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setAllowFileAccessFromFileURLs(true);
            settings.setAllowUniversalAccessFromFileURLs(true);
            // Bloquear apertura automática de ventanas emergentes por JS
            settings.setJavaScriptCanOpenWindowsAutomatically(false);
            settings.setSupportMultipleWindows(true);
            // LOAD_DEFAULT con timestamp en la URL principal garantiza recibir siempre la última versión remota
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);

            // Limpiar "; wv" del User-Agent para que YouTube no bloquee el reproductor embebido (Error 153)
            String defaultUa = settings.getUserAgentString();
            if (defaultUa != null) {
                String cleanUa = defaultUa.replace("; wv)", ")").replace(" wv ", " ");
                settings.setUserAgentString(cleanUa);
            }

            // Habilitar cookies de terceros (requerido por YouTube Live embed)
            try {
                CookieManager cookieManager = CookieManager.getInstance();
                cookieManager.setAcceptCookie(true);
                cookieManager.setAcceptThirdPartyCookies(webView, true);
            } catch (Throwable t) {
                Log.w(TAG, "No se pudieron configurar cookies:", t);
            }

            // Puente Java <-> JavaScript
            try {
                webView.addJavascriptInterface(new WebAppInterface(this), "AndroidBridge");
            } catch (Throwable t) {
                Log.w(TAG, "No se pudo registrar AndroidBridge:", t);
            }

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    Log.d(TAG, "[WebConsole] " + consoleMessage.message());
                    return super.onConsoleMessage(consoleMessage);
                }

                @Override
                public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                    // Escudo Anti-Popups: bloquea cualquier intento de embeds deportivos de abrir ventanas nuevas
                    Log.d(TAG, "Popup bloqueado por escudo nativo");
                    return false;
                }
            });

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                    if (request != null && request.getUrl() != null) {
                        String url = request.getUrl().toString().toLowerCase(Locale.ROOT);
                        if (isAdDomain(url)) {
                            return new WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8))
                            );
                        }
                    }
                    return super.shouldInterceptRequest(view, request);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    if (request != null && request.getUrl() != null) {
                        String url = request.getUrl().toString();
                        String lower = url.toLowerCase(Locale.ROOT);
                        // Bloquear esquemas externos o redirecciones publicitarias fuera de la app
                        if (!lower.startsWith("http://") && !lower.startsWith("https://") && !lower.startsWith("file://")) {
                            return true;
                        }
                        if (isAdDomain(lower)) {
                            return true;
                        }
                        // Si un iframe intenta redirigir la ventana principal a otro dominio, bloquearlo
                        if (request.isForMainFrame()
                                && !lower.contains("andresehansen.github.io")
                                && !lower.startsWith("file:///android_asset/")) {
                            Log.w(TAG, "Redirección externa bloqueada en frame principal: " + url);
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    // Si falla la carga de la página principal en la nube (por ej. sin internet temporal),
                    // cae automáticamente a la copia interna empaquetada en el APK
                    if (request != null && request.isForMainFrame() && !usingFallback) {
                        usingFallback = true;
                        Log.w(TAG, "Fallo conexión remota, cargando respaldo local del APK");
                        view.loadUrl(LOCAL_FALLBACK_URL);
                    }
                }
            });

            // Cargar siempre la versión viva desde GitHub Pages (Actualización OTA instantánea sin reinstalar APK)
            webView.loadUrl(REMOTE_APP_URL + "?t=" + System.currentTimeMillis());

        } catch (Throwable t) {
            Log.e(TAG, "Excepción durante onCreate", t);
            showErrorScreen("No se pudo iniciar AndyTV", t);
        }
    }

    private boolean isAdDomain(String urlLower) {
        for (String domain : AD_BLOCK_DOMAINS) {
            if (urlLower.contains(domain)) {
                return true;
            }
        }
        return false;
    }

    private void initExoPlayer(Map<String, String> customHeaders) {
        try {
            if (exoPlayer != null) {
                exoPlayer.release();
                exoPlayer = null;
            }

            String userAgent = DEFAULT_USER_AGENT;
            Map<String, String> requestHeaders = new HashMap<>();
            if (customHeaders != null) {
                for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                    String k = entry.getKey();
                    String v = entry.getValue();
                    if (k != null && v != null && !v.trim().isEmpty()) {
                        if ("user-agent".equalsIgnoreCase(k.trim())) {
                            userAgent = v.trim();
                        } else {
                            requestHeaders.put(k.trim(), v.trim());
                        }
                    }
                }
            }

            DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(userAgent)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(12000);

            if (!requestHeaders.isEmpty()) {
                httpDataSourceFactory.setDefaultRequestProperties(requestHeaders);
            }

            // Buffer optimizado para zapping rápido en TV sin micro-cortes
            DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    12000, // Min buffer
                    35000, // Max buffer
                    1500,  // Buffer para empezar a reproducir rápido
                    3000   // Buffer tras re-buffering
                )
                .build();

            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build();

            exoPlayer = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(this).setDataSourceFactory(httpDataSourceFactory))
                .setLoadControl(loadControl)
                .setAudioAttributes(audioAttributes, true)
                .build();

            exoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_READY) {
                        if (webView != null) {
                            webView.evaluateJavascript("if(window.onNativeVideoPlaying) window.onNativeVideoPlaying();", null);
                        }
                    }
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    Log.e(TAG, "ExoPlayer error: " + error.getMessage(), error);
                    if (webView != null) {
                        webView.evaluateJavascript("if(window.onNativeVideoError) window.onNativeVideoError();", null);
                    }
                }
            });

            if (exoPlayerView != null) {
                exoPlayerView.setPlayer(exoPlayer);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Error inicializando ExoPlayer", t);
        }
    }

    public void playNativeHls(String url) {
        playNativeHlsWithHeaders(url, null);
    }

    public void playNativeHlsWithHeaders(String url, String headersJson) {
        runOnUiThread(() -> {
            try {
                if (webView != null) {
                    webView.setBackgroundColor(Color.TRANSPARENT);
                }

                Map<String, String> headersMap = new HashMap<>();
                if (headersJson != null && !headersJson.trim().isEmpty()) {
                    try {
                        JSONObject json = new JSONObject(headersJson);
                        Iterator<String> keys = json.keys();
                        while (keys.hasNext()) {
                            String k = keys.next();
                            headersMap.put(k, json.optString(k, ""));
                        }
                    } catch (Throwable ignored) {}
                }

                initExoPlayer(headersMap);

                if (exoPlayerView != null && exoPlayer != null) {
                    exoPlayerView.setVisibility(View.VISIBLE);
                    MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
                    exoPlayer.setMediaItem(mediaItem);
                    exoPlayer.prepare();
                    exoPlayer.setPlayWhenReady(true);
                    if (webView != null) {
                        webView.requestFocus();
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error en playNativeHlsWithHeaders", t);
                if (webView != null) {
                    webView.evaluateJavascript("if(window.onNativeVideoError) window.onNativeVideoError();", null);
                }
            }
        });
    }

    public void stopNativeHls() {
        runOnUiThread(() -> {
            try {
                if (exoPlayer != null) {
                    exoPlayer.stop();
                    exoPlayer.clearMediaItems();
                }
                if (exoPlayerView != null) {
                    exoPlayerView.setVisibility(View.GONE);
                }
                if (webView != null) {
                    webView.setBackgroundColor(Color.parseColor("#080c14"));
                }
            } catch (Throwable t) {
                Log.w(TAG, "Error en stopNativeHls", t);
            }
        });
    }

    public void setVideoAspectMode(String mode) {
        runOnUiThread(() -> {
            if (exoPlayerView == null || mode == null) return;
            switch (mode.toLowerCase(Locale.ROOT)) {
                case "fill":
                    exoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
                    break;
                case "cover":
                case "zoom":
                    exoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
                    break;
                default:
                    exoPlayerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
                    break;
            }
        });
    }

    public void enterPiPMode() {
        runOnUiThread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    PictureInPictureParams params = new PictureInPictureParams.Builder()
                        .setAspectRatio(new Rational(16, 9))
                        .build();
                    enterPictureInPictureMode(params);
                } else {
                    Toast.makeText(MainActivity.this, "PiP requiere Android 8.0 o superior", Toast.LENGTH_SHORT).show();
                }
            } catch (Throwable t) {
                Log.w(TAG, "No se pudo iniciar modo PiP", t);
            }
        });
    }

    public void reloadRemoteApp() {
        runOnUiThread(() -> {
            if (webView != null) {
                usingFallback = false;
                webView.clearCache(true);
                webView.loadUrl(REMOTE_APP_URL + "?t=" + System.currentTimeMillis());
                Toast.makeText(MainActivity.this, "Actualizando AndyTV desde la nube...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showErrorScreen(String title, Throwable t) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            String stackTrace = sw.toString();

            ScrollView scrollView = new ScrollView(this);
            scrollView.setBackgroundColor(Color.parseColor("#080c14"));
            scrollView.setPadding(40, 40, 40, 40);

            TextView tv = new TextView(this);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(18);
            tv.setText("⚠️ " + title + "\n\nDetalle técnico:\n" + t.getMessage() + "\n\nTraza:\n" + stackTrace);

            scrollView.addView(tv);
            setContentView(scrollView);
        } catch (Throwable ignored) {
            finish();
        }
    }

    private void safeHideSystemUI() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                    controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } else {
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                );
            }
        } catch (Throwable t) {
            Log.w(TAG, "safeHideSystemUI advertencia:", t);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            safeHideSystemUI();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (webView != null && event != null) {
            int keyCode = event.getKeyCode();
            boolean isChUp = (keyCode == KeyEvent.KEYCODE_CHANNEL_UP || keyCode == KeyEvent.KEYCODE_PAGE_UP);
            boolean isChDown = (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN || keyCode == KeyEvent.KEYCODE_PAGE_DOWN);
            if (isChUp || isChDown) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                    String fn = isChUp
                        ? "if(window.onAndroidChannelUp) window.onAndroidChannelUp();"
                        : "if(window.onAndroidChannelDown) window.onAndroidChannelDown();";
                    webView.evaluateJavascript(fn, null);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webView == null) return super.onKeyDown(keyCode, event);

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackKey();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void handleBackKey() {
        if (webView == null) {
            finish();
            return;
        }
        webView.evaluateJavascript("window.onAndroidBackPressed ? window.onAndroidBackPressed() : false;", value -> {
            boolean handledInWeb = "true".equalsIgnoreCase(value);
            if (!handledInWeb) {
                long now = System.currentTimeMillis();
                if (now - lastBackPressTime < 2500) {
                    stopNativeHls();
                    finish();
                } else {
                    lastBackPressTime = now;
                    Toast.makeText(MainActivity.this, "Presione Atrás de nuevo para salir de AndyTV", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        safeHideSystemUI();
        if (webView != null) webView.onResume();
        if (exoPlayer != null && exoPlayerView != null && exoPlayerView.getVisibility() == View.VISIBLE) {
            exoPlayer.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Si entró en modo Picture-in-Picture, NO pausar el video ni el WebView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode()) {
            return;
        }
        if (exoPlayer != null && exoPlayer.isPlaying()) {
            exoPlayer.setPlayWhenReady(false);
        }
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
