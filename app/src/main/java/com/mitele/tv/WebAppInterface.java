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
    public void playNativeHls(String url) {
        activity.playNativeHls(url);
    }

    @JavascriptInterface
    public void stopNativeHls() {
        activity.stopNativeHls();
    }

    @JavascriptInterface
    public void reloadRemoteApp() {
        activity.reloadRemoteApp();
    }
}
