package com.mitele.tv;

import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WebAppInterface {
    private final MainActivity activity;

    public WebAppInterface(MainActivity activity) {
        this.activity = activity;
    }

    @JavascriptInterface
    public void exitApp() {
        activity.runOnUiThread(activity::finish);
    }

    @JavascriptInterface
    public void showToast(String message) {
        activity.runOnUiThread(() -> Toast.makeText(activity, message, Toast.LENGTH_SHORT).show());
    }

    @JavascriptInterface
    public void resolveAndPlayWebHls(String url, String referer, String type) {
        activity.resolveAndPlayWebHls(url, referer, type);
    }

    @JavascriptInterface
    public void playNativeHls(String url) {
        activity.playNativeHlsWithHeaders(url, null);
    }

    @JavascriptInterface
    public void playNativeHlsWithHeaders(String url, String headersJson) {
        activity.playNativeHlsWithHeaders(url, headersJson);
    }

    @JavascriptInterface
    public void stopNativeHls() {
        activity.stopNativeHls();
    }

    @JavascriptInterface
    public void setVideoAspectMode(String mode) {
        activity.setVideoAspectMode(mode);
    }

    @JavascriptInterface
    public void enterPiP() {
        activity.enterPiPMode();
    }

    @JavascriptInterface
    public void reloadRemoteApp() {
        activity.reloadRemoteApp();
    }
}
