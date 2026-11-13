package com.mitele.tv;

import android.app.Activity;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WebAppInterface {
    private final Activity activity;

    public WebAppInterface(Activity activity) {
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
}
