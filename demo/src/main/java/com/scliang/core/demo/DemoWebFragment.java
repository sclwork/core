package com.scliang.core.demo;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.scliang.core.bridge.BaseChromeClient;
import com.scliang.core.bridge.BaseWebFragment;
import com.scliang.core.bridge.IBridgeCallback;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/9/30.
 */
public class DemoWebFragment extends BaseWebFragment {

    @Override
    protected BaseChromeClient onCreateChromeClient() {
        return new DemoWebChromeClient();
    }

    @Override
    protected String onGenerateJSObjectName() {
        return "Demo";
    }

    @Override
    protected boolean onGenerateUseJsPrompt() {
        return true;
    }

    @Override
    protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreatedHere(view, savedInstanceState);
//        setToolbarMenu(R.menu.menu_help_label);
        setToolbarCenterTitle("WebView全屏播放");

        WebView webView = getWebView();
        WebSettings settings = webView.getSettings();
        if (settings != null) {
            settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        }

//        setRefreshView(R.layout.view_recycler_refresh);

        registerHandler("input", new IBridgeCallback() {
            @Override
            public void onCallback(String handlerName, String data) {
                toast(data);
            }
        });

        loadUrl("http://www.baidu.com");
    }

//    @Override
//    protected void onMenuItemClick(int groupId, int itemId) {
//        if (itemId == R.id.menu_id_help) {
//            IBridge bridge = getJSBridge();
//            if (bridge != null) {
//                bridge.toJS("toJS", "{}", new IToJSResultCallback() {
//                    @Override
//                    public void onJSResultCallback(String result) {
//                        Logger.d("DemoWebFragment", "to-js-result: " + result);
//                    }
//                });
//            }
//        }
//    }

    private static class DemoWebChromeClient extends BaseChromeClient {

        @Override
        public boolean onShowUniversalFileChooser(@Nullable WebView webView,
                                                  @Nullable ValueCallback<Uri[]> filePathCallback,
                                                  @Nullable FileChooserParams fileChooserParams,
                                                  @Nullable String acceptType,
                                                  @Nullable String capture) {
            return super.onShowUniversalFileChooser(webView, filePathCallback, fileChooserParams,
                    acceptType, capture);
        }
    }
}
