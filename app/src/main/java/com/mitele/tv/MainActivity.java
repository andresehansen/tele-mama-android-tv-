package com.mitele.tv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends Activity {

    private static final String TAG = "MiTeleTV";
    private WebView webView;
    private long lastBackPressTime = 0;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Manejador global de excepciones para evitar cierres silenciosos
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Log.e(TAG, "FATAL CRASH DETECTED", throwable);
            runOnUiThread(() -> showErrorScreen("Error Inesperado en la Aplicación", throwable));
        });

        try {
            // Mantener la pantalla encendida siempre que la tele esté en la app
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            // Crear el WebView
            webView = new WebView(this);
            setContentView(webView);

            // Ocultar barras de sistema de manera segura (después de contentView)
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
            settings.setCacheMode(WebSettings.LOAD_DEFAULT);
            settings.setUseWideViewPort(true);
            settings.setLoadWithOverviewMode(true);

            // Puente Java <-> JavaScript con control de fallos
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
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    Log.e(TAG, "WebView Error: " + description + " (" + failingUrl + ")");
                }
            });

            // Cargar la aplicación web empaquetada
            webView.loadUrl("file:///android_asset/www/index.html");

        } catch (Throwable t) {
            Log.e(TAG, "Excepción durante onCreate", t);
            showErrorScreen("No se pudo iniciar el reproductor", t);
        }
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
                    finish();
                } else {
                    lastBackPressTime = now;
                    Toast.makeText(MainActivity.this, "Presione Atrás de nuevo para salir", Toast.LENGTH_SHORT).show();
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
        if (webView != null) webView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
