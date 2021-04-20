package com.scliang.core.bridge;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.scliang.core.R;
import com.scliang.core.base.BaseFragment;
import com.scliang.core.base.RootContainer;
import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient;
import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * SCore Bridge
 * Created by ShangChuanliang
 * on 17/9/30.
 */
@SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
public abstract class BaseX5WebFragment extends BaseFragment implements DownloadListener {
  /**
   * Args中的LOAD_URL参数
   */
  public static final String LOAD_URL = "BaseWebFragment.LoadUrl";
  /**
   * Args中的EXTRA_HEADERS参数
   */
  public static final String EXTRA_HEADERS = "BaseWebFragment.ExtraHeaders";
  /**
   * Default IJSBridge-JSObject Name
   * <br/>
   * Value: [SCore]
   */
  public static final String DEFAULT_JSOBJECT_NAME = "SCore";
  // H5页面中调用的JS类名
  private String mJSObjectName;
  // 从子类获得IJSBridge-JSObject Name
  protected abstract String onGenerateJSObjectName();
  // 设置JS-Native方式，是否使用JsPrompt
  protected abstract boolean onGenerateUseJsPrompt();
  private boolean mUseJsPrompt;

  // 用于视频全屏播放的CustomView
  private int mCurrentOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
  private static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
  private FrameLayout mFullscreenContainer;
  private View mCustomView;
  private IX5WebChromeClient.CustomViewCallback mCustomViewCallback;
  private ProgressBar mLoadingBar;

  private FrameLayout mTopContainer;
  //    // 用于和H5通讯的WebView,可以是JWebView或其子类;
//    // 子类可以通过覆盖onCreateWebView方法设置自定义的BaseWebViewWebView或其子类;
  protected com.tencent.smtt.sdk.WebView mWebView;
  protected IBridge mBridge;

  // Datas
  private String mUrl;
  private IBridge wBridge;
  private String wJSObjName;
  private String wUrl;
  private Map<String, String> mExtraHeaders;

  public static List<String> createExtraHeaders(Map<String, String> headers) {
    if (headers == null || headers.size() <= 0) {
      return null;
    }

    List<String> list = new ArrayList<>();
    for (String key : headers.keySet()) {
      String value = headers.get(key);
      list.add(String.format(Locale.CHINESE, "%s:::%s", key, value));
    }
    return list;
  }

  public BaseX5WebFragment() {
    String jsObjectName = onGenerateJSObjectName();
    if (TextUtils.isEmpty(jsObjectName)) {
      jsObjectName = DEFAULT_JSOBJECT_NAME;
    }
    // Set Default IJSBridge
    mUseJsPrompt = onGenerateUseJsPrompt();
    BasicBridgeHandler handler = new BasicBridgeHandler(Looper.getMainLooper());
    IBridge bridge = new BasicBridge(handler, mUseJsPrompt);
    setBridge(bridge, jsObjectName);
  }

  @Override
  public final View onCreateViewHere(@NonNull LayoutInflater inflater,
                                     @Nullable ViewGroup container,
                                     @Nullable Bundle savedInstanceState) {
//        mWebView = createWebView(inflater, container, savedInstanceState);
//        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//        mWebView.setLayoutParams(lp);
    View root = inflater.inflate(R.layout.fragment_base_x5_web, container, false);
    mTopContainer = root.findViewById(R.id.top_container);
    mWebView = root.findViewById(R.id.web_view);
    mLoadingBar = root.findViewById(R.id.loading_progress);
    mLoadingBar.setVisibility(View.INVISIBLE);

    // WebView启动硬件加速
    mWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
    mWebView.setDownloadListener(this);
    // 移除系统开放的JS接口
    mWebView.removeJavascriptInterface("searchBoxJavaBridge_");
    mWebView.removeJavascriptInterface("accessibility");
    mWebView.removeJavascriptInterface("accessibilityTraversal");
    // settings
    WebSettings settings = mWebView.getSettings();
    if (settings != null) {
      settings.setJavaScriptEnabled(true);
      settings.setGeolocationEnabled(true);

      settings.setDomStorageEnabled(true);
      settings.setAllowFileAccess(true);
      settings.setAppCacheEnabled(true);
      settings.setAllowContentAccess(true);
      settings.setLightTouchEnabled(true);

      settings.setDatabaseEnabled(true);
      settings.setNeedInitialFocus(true);

      settings.setJavaScriptCanOpenWindowsAutomatically(true);
      settings.setLoadWithOverviewMode(true);
      settings.setUseWideViewPort(true);
      settings.setSupportZoom(true);
      settings.setBuiltInZoomControls(true);
      settings.setDisplayZoomControls(false);
    }
    return root;
  }

  @Override
  protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreatedHere(view, savedInstanceState);
    if (mWebView != null) {
      // w Datas
      if (wBridge != null) {
        setBridgeCore(wBridge, wJSObjName);
        wBridge = null;
        wJSObjName = null;
      }

      // set clients
      mWebView.setWebViewClient(createWebClient());
      setWebChromeClientCore();

      if (!TextUtils.isEmpty(wUrl)) {
        loadUrl(wUrl);
        wUrl = null;
      }
    }
    Bundle args = getArguments();
    if (args != null) {
      final List<String> ehs = args.getStringArrayList(EXTRA_HEADERS);
      if (ehs != null && ehs.size() > 0) {
        mExtraHeaders = new HashMap<>();
        for (String eh : ehs) {
          if (!TextUtils.isEmpty(eh)) {
            String[] kv = eh.split(":::");
            if (kv.length == 2) {
              mExtraHeaders.put(kv[0], kv[1]);
            }
          }
        }
      }
      final String initUrl = args.getString(LOAD_URL);
      if (!TextUtils.isEmpty(initUrl)) {
        loadUrl(initUrl);
      }
    }

    // 检查是否有可用网络
    checkConnectedNetwork();
  }

  @Override
  public void onResume(Bundle args) {
    super.onResume(args);
    mWebView.resumeTimers();
    mWebView.onResume();
  }

  @Override
  public void onPause(Bundle args) {
    super.onPause(args);
    mWebView.onPause();
    mWebView.pauseTimers();
  }

  @Override
  public void onDestroy() {
    mWebView.destroy();
    super.onDestroy();
  }

  @Override
  public boolean onBackPressed() {
    if (checkHideCustomView()) {
      return true;
    } else {
      return goBack() || super.onBackPressed();
    }
  }

  @Override
  protected boolean checkRefreshAfterNetworkConnected(int type) {
    return isContentMaskViewShowing();
  }

  @Override
  public void onRefreshAfterNetworkConnected(int type) {
    refresh();
  }

  @Override
  public void onNetworkReload() {
    refresh();
  }

//    protected final BaseWebView createWebView(LayoutInflater inflater,
//                                              @Nullable ViewGroup container,
//                                              @Nullable Bundle savedInstanceState) {
//        BaseWebView webView = onCreateWebView(inflater, container, savedInstanceState);
//        if (webView == null) {
//            webView = new BaseWebView(inflater.getContext());
//        }
//        return webView;
//    }

  @Override
  public void onDownloadStart(String url,
                              String userAgent,
                              String contentDisposition,
                              String mimeType, long contentLength) {
    final Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    Uri uri = Uri.parse(url);
    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    activity.startActivity(intent);
  }

  protected final BaseX5WebClient createWebClient() {
    BaseX5WebClient client = onCreateWebClient();
    if (client == null) {
      client = new JWebClient();
    }
    return client;
  }

  protected final BaseX5ChromeClient createChromeClient() {
    BaseX5ChromeClient client = onCreateChromeClient();
    if (client == null) {
      client = new JChromeClient();
    }
    return client;
  }

//    /**
//     * 子类可以通过覆盖这个方法设置自定义的JWebView及其子类
//     * @param inflater inflater
//     * @param container container
//     * @param savedInstanceState savedInstanceState
//     * @return JWebView或其子类
//     */
//    protected BaseWebView onCreateWebView(LayoutInflater inflater,
//                                          @Nullable ViewGroup container,
//                                          @Nullable Bundle savedInstanceState) {
//        return null;
//    }

  /**
   * 子类可以通过覆盖这个方法设置自定义的BaseWebClient
   *
   * @return BaseWebClient
   */
  protected BaseX5WebClient onCreateWebClient() {
    return null;
  }

  /**
   * 子类可以通过覆盖这个方法设置自定义的WebChromeClient
   *
   * @return WebChromeClient
   */
  protected BaseX5ChromeClient onCreateChromeClient() {
    return null;
  }

  /**
   * 获得当前WebView
   *
   * @return WebView
   */
  public WebView getWebView() {
    return mWebView;
  }

  /**
   * 设置TopContextView
   *
   * @param id TopContextView
   */
  public final void setTopContextView(@LayoutRes int id) {
    if (mTopContainer != null && getActivity() != null) {
      try {
        mTopContainer.removeAllViews();
        mTopContainer.addView(LayoutInflater.from(getActivity()).inflate(id, mTopContainer, false),
            new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
      } catch (Exception ignored) {
        mTopContainer.removeAllViews();
      }
    }
  }

//    /**
//     * 设置下拉刷新的视图
//     * @param id 视图Id
//     */
//    public void setRefreshView(@LayoutRes int id) {
//        if (mWebView != null) {
//            mWebView.setRefreshView(id);
//        }
//    }

  /**
   * 执行WebView的GoBack
   *
   * @return true:goBack Success; false:goBack Failed
   */
  public boolean goBack() {
    if (mWebView == null) {
      return false;
    } else {
      if (mWebView.canGoBack()) {
        mWebView.goBack();
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * 给WebView设定JS通信桥梁
   *
   * @param bridge IJSBridge JS通信桥梁
   * @param name   H5页面中调用的JS类名
   */
  public final void setBridge(IBridge bridge, String name) {
    setBridgeCore(bridge, name);
    setWebChromeClientCore();
  }

  @SuppressLint("JavascriptInterface")
  private void setBridgeCore(IBridge bridge, String name) {
    if (mWebView != null) {
      if (!TextUtils.isEmpty(mJSObjectName)) {
        mWebView.removeJavascriptInterface(mJSObjectName);
        mJSObjectName = null;
      }

      mJSObjectName = name;

      if (!mUseJsPrompt && bridge != null && !TextUtils.isEmpty(mJSObjectName)) {
        mWebView.addJavascriptInterface(bridge, mJSObjectName);
      }

      mBridge = bridge;
      if (mBridge != null) {
        mBridge.setTarget(mWebView);
      }
    } else {
      wBridge = bridge;
      wJSObjName = name;
    }
  }

  private void setWebChromeClientCore() {
    if (mWebView != null) {
      mWebView.setWebChromeClient(new BasicX5ChromeClient(
          createChromeClient(), getJSBridge(),
          mUseJsPrompt, mLoadingBar,
          mOnCustomViewChangeListener,
          new Runnable() {
            @Override
            public void run() {
              onProgressCompleted();
            }
          }));
    }
  }

  protected void onProgressCompleted() {
    // nothing
  }

  /**
   * 获得当前WebView的JSBridge
   *
   * @return IJSBridge
   */
  public IBridge getJSBridge() {
    return mBridge == null ? wBridge : mBridge;
  }

  /**
   * 注册一个操作
   *
   * @param handlerName handlerName
   * @param callback    callback
   */
  public void registerHandler(String handlerName, IBridgeCallback callback) {
    IBridge bridge = getJSBridge();
    if (bridge != null) {
      bridge.registerHandler(handlerName, callback);
    }
  }

  /**
   * 解注册一个操作
   *
   * @param handlerName handlerName
   */
  public void unregisterHandler(String handlerName) {
    IBridge bridge = getJSBridge();
    if (bridge != null) {
      bridge.unregisterHandler(handlerName);
    }
  }

  /**
   * 加载给定的Url
   *
   * @param url url
   */
  public final void loadUrl(String url) {
    mUrl = processUrl(url);
    if (mWebView != null) {
      preLoadUrl(mUrl);
      if (mExtraHeaders == null || mExtraHeaders.size() <= 0) {
        mWebView.loadUrl(mUrl);
      } else {
        mWebView.loadUrl(mUrl, mExtraHeaders);
      }
    } else {
      wUrl = mUrl;
    }
  }

  /**
   * 刷新当前页面
   */
  public final void refresh() {
//        if (!TextUtils.isEmpty(mUrl)) {
//            loadUrl(mUrl);
//        }
    if (mWebView != null) {
      mWebView.reload();
    }
  }

  protected String processUrl(String url) {
    return url;
  }

  protected void preLoadUrl(String url) {
  }

  protected boolean checkHideCustomView() {
    if (mCustomView != null) {
      hideCustomView();
      return true;
    } else {
      return false;
    }
  }

  // 检查是否有可用网络
  private void checkConnectedNetwork() {
    if (!hasConnectedNetwork()) {
      setToolbarCenterTitle("");
      showContentMaskView();
    }
  }

  private final class JWebClient extends BaseX5WebClient {
    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
      super.onPageStarted(view, url, favicon);
      if (hasConnectedNetwork()) {
        setContentContextViewVisibility(true);
        hideContentMaskView();
      }
    }
  }

  private final class JChromeClient extends BaseX5ChromeClient {
    @Override
    public void onReceivedTitle(WebView view, String title) {
      super.onReceivedTitle(view, title);
      final String regexA = "^([hH][tT]{2}[pP]://|[hH][tT]{2}[pP][sS]://)(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\/])+$";
      final String regexB = "^(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\/])+$";
      final Pattern patternA = Pattern.compile(regexA);
      final Pattern patternB = Pattern.compile(regexB);
      if (!patternA.matcher(title).matches() && !patternB.matcher(title).matches()) {
        if (hasConnectedNetwork()) {
          setToolbarCenterTitle(title);
        }
      }
    }
  }

  private BasicX5ChromeClient.OnCustomViewChangeListener mOnCustomViewChangeListener =
      new BasicX5ChromeClient.OnCustomViewChangeListener() {
    @Override
    public View getVideoLoadingProgressView() {
      final Activity activity = getActivity();
      if (activity == null) {
        return null;
      }

      FrameLayout frameLayout = new FrameLayout(activity);
      frameLayout.setLayoutParams(new ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      return frameLayout;
    }

    @Override
    public void onShowCustomView(View view, IX5WebChromeClient.CustomViewCallback callback) {
      mCurrentOrientation = getResources().getConfiguration().orientation;
      showCustomView(view, callback);
    }

    @Override
    public void onHideCustomView() {
      hideCustomView();
    }
  };

  private class FullscreenHolder extends FrameLayout {
    public FullscreenHolder(Context context) {
      super(context);
      setBackgroundColor(context.getResources().getColor(android.R.color.black));
    }
  }

  private void setStatusBarVisibility(boolean visible) {
    final Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    int flag = visible ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
    activity.getWindow().setFlags(flag, WindowManager.LayoutParams.FLAG_FULLSCREEN);
  }

  // 视频播放全屏
  private void showCustomView(View view, IX5WebChromeClient.CustomViewCallback callback) {
    if (mCustomView != null) {
      callback.onCustomViewHidden();
      return;
    }

    final Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    FrameLayout decor = (FrameLayout) activity.getWindow().getDecorView();
    RootContainer rootContainer = decor.findViewById(R.id.root);
    mFullscreenContainer = new FullscreenHolder(activity);
    mFullscreenContainer.addView(view, COVER_SCREEN_PARAMS);
    if (rootContainer == null) {
      decor.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
    } else {
      rootContainer.addView(mFullscreenContainer, COVER_SCREEN_PARAMS);
    }
    mCustomView = view;
    setStatusBarVisibility(false);
    mCustomViewCallback = callback;

    // 横屏
    if (mCurrentOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }
  }

  // 隐藏视频全屏
  private void hideCustomView() {
    if (mCustomView == null) {
      return;
    }

    final Activity activity = getActivity();
    if (activity == null) {
      return;
    }

    setStatusBarVisibility(true);
    FrameLayout decor = (FrameLayout) activity.getWindow().getDecorView();
    RootContainer rootContainer = decor.findViewById(R.id.root);
    if (rootContainer == null) {
      decor.removeView(mFullscreenContainer);
    } else {
      rootContainer.removeView(mFullscreenContainer);
    }
    mFullscreenContainer = null;
    mCustomView = null;
    mCustomViewCallback.onCustomViewHidden();
    mWebView.setVisibility(View.VISIBLE);

    // 竖屏
    activity.setRequestedOrientation(mCurrentOrientation);
  }
}
