package com.scliang.core.bridge;

import android.net.Uri;
import android.os.Build;
import androidx.annotation.Nullable;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/6/19.
 */
public abstract class BaseChromeClient extends WebChromeClient {

    // For Android  >= 4.1 <= 4.4
    public final void openFileChooser(ValueCallback<Uri> valueCallback, String acceptType, String capture) {}

    // For Android > 4.4
    @Override
    public final boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
        return super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
    }

    /**
     * 4.1以上版本共用 - 子类应该重写这个方法实现文件选择
     */
    public boolean onShowUniversalFileChooser(@Nullable WebView webView,
                                              @Nullable ValueCallback<Uri[]> filePathCallback,
                                              @Nullable FileChooserParams fileChooserParams,
                                              @Nullable String acceptType,
                                              @Nullable String capture) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (filePathCallback != null) filePathCallback.onReceiveValue(null);
        }
        return false;
    }
}
