package com.mitele.tv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends Activity {

    private static final String TAG = "AndyTV";
    // URL remota en GitHub Pages: permite que la app se actualice sola en la TV sin reinstalar el APK
    private static final String REMOTE_APP_URL = "https://andresehansen.github.io/tele-mama-android-tv-/";
    private static final String LOCAL_FALLBACK_URL = "file:///android_asset/www/index.html";

    private FrameLayout rootLayout;
    private VideoView nativeVideoView;
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

            // Contenedor raíz que aloja el reproductor nativo debajo y el WebView encima
            rootLayout = new FrameLayout(this);
            rootLayout.setBackgroundColor(Color.BLACK);

            // VideoView nativo de Android (decodifica por hardware audio AC3/ADTS/AAC de canales HLS)
            nativeVideoView = new VideoView(this);
            FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            );
            nativeVideoView.setLayoutParams(videoParams);
            nativeVideoView.setVisibility(View.GONE);

            nativeVideoView.setOnPreparedListener(mp -> {
                try {
                    mp.setVolume(1.0f, 1.0f);
                    mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                } catch (Throwable ignored) {}
                nativeVideoView.start();
                if (webView != null) {
                    webView.evaluateJavascript("if(window.onNativeVideoPlaying) window.onNativeVideoPlaying();", null);
                }
            });

            nativeVideoView.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Native VideoView error: what=" + what + " extra=" + extra);
                if (webView != null) {
                    webView.evaluateJavascript("if(window.onNativeVideoError) window.onNativeVideoError();", null);
                }
                return true;
            });

            // Crear el WebView principal encima del VideoView
            webView = new WebView(this);
            FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            );
            webView.setLayoutParams(webParams);
            webView.setBackgroundColor(Color.parseColor("#080c14"));

            rootLayout.addView(nativeVideoView);
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
            });

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
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

    public void playNativeHls(String url) {
        runOnUiThread(() -> {
            try {
                if (webView != null) {
                    webView.setBackgroundColor(Color.TRANSPARENT);
                }
                if (nativeVideoView != null) {
                    nativeVideoView.stopPlayback();
                    nativeVideoView.setVisibility(View.VISIBLE);
                    nativeVideoView.setVideoURI(Uri.parse(url));
                    nativeVideoView.requestFocus();
                    if (webView != null) {
                        webView.requestFocus();
                    }
                }
            } catch (Throwable t) {
                Log.e(TAG, "Error en playNativeHls", t);
                if (webView != null) {
                    webView.evaluateJavascript("if(window.onNativeVideoError) window.onNativeVideoError();", null);
                }
            }
        });
    }

    public void stopNativeHls() {
        runOnUiThread(() -> {
            try {
                if (nativeVideoView != null) {
                    if (nativeVideoView.isPlaying()) {
                        nativeVideoView.stopPlayback();
                    }
                    nativeVideoView.setVisibility(View.GONE);
                }
                if (webView != null) {
                    webView.setBackgroundColor(Color.parseColor("#080c14"));
                }
            } catch (Throwable t) {
                Log.w(TAG, "Error en stopNativeHls", t);
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (webView == null) return super.onKeyDown(keyCode, event);

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackKey();
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_CHANNEL_UP) {
            webView.evaluateJavascript("if(window.onAndroidChannelUp) window.onAndroidChannelUp();", null);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_CHANNEL_DOWN) {
            webView.evaluateJavascript("if(window.onAndroidChannelDown) window.onAndroidChannelDown();", null);
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nativeVideoView != null && nativeVideoView.isPlaying()) {
            nativeVideoView.pause();
        }
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (nativeVideoView != null) {
            nativeVideoView.stopPlayback();
            nativeVideoView = null;
        }
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
