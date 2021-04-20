//package com.scliang.core.bridge;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Rect;
//import android.net.Uri;
//import android.util.AttributeSet;
//import android.view.View;
//import android.webkit.DownloadListener;
//import android.webkit.ValueCallback;
//import android.webkit.WebChromeClient;
//import android.webkit.WebSettings;
//import android.webkit.WebView;
//import android.webkit.WebViewClient;
//
//import com.scliang.core.ui.BaseViewGroup;
//
///**
// * SCore Bridge
// * Created by ShangChuanliang
// * on 17/9/30.
// */
//@SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
//public class BaseWebView extends BaseViewGroup implements DownloadListener {
//    private WebView mWebView;
//    private Rect mWebViewRect = new Rect();
////    private int mSavedHeight;
//
////    private int mTouchSlop;
////    private boolean isBeingDragged = false;
////    private float mLastMotionY;
////    private float mTouchDownY = 0;
////    private int mDragOffset;
////    private int mRefreshDragLength = -1;
////    private int mRefreshViewId;
////    private BaseRecyclerDragView mRefreshView;
////    private Rect mRefreshViewRect = new Rect();
////    private boolean isNoticeDragged = false;
////    private boolean isRestoring = false;
////    private Scroller mDragScroller;
//
//    public BaseWebView(Context context) {
//        super(context);
//    }
//
//    public BaseWebView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//    }
//
//    public BaseWebView(Context context, AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//    }
//
//    @SuppressLint("JavascriptInterface")
//    public final void addJavascriptInterface(Object object, String name) {
//        mWebView.addJavascriptInterface(object, name);
//    }
//
//    public final void removeJavascriptInterface(String name) {
//        mWebView.removeJavascriptInterface(name);
//    }
//
//    public final void evaluateJavascript(String script, ValueCallback<String> resultCallback) {
//        mWebView.evaluateJavascript(script, resultCallback);
//    }
//
//    @Override
//    protected void onInit() {
////        ViewConfiguration configuration = ViewConfiguration.get(getContext());
////        mTouchSlop = configuration.getScaledTouchSlop();
//
//        mWebView = new WebView(getContext());
//        addView(mWebView);
//
//        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
//        mWebView.setDownloadListener(this);
//
//        WebSettings settings = mWebView.getSettings();
//        if (settings != null) {
//            settings.setJavaScriptEnabled(true);
//            settings.setGeolocationEnabled(true);
//
//            settings.setDomStorageEnabled(true);
//            settings.setAllowFileAccess(true);
//            settings.setAppCacheEnabled(true);
//            settings.setAllowContentAccess(true);
//            settings.setLightTouchEnabled(true);
//
//            settings.setDatabaseEnabled(true);
//            settings.setNeedInitialFocus(true);
//
//            settings.setJavaScriptCanOpenWindowsAutomatically(true);
//            settings.setLoadWithOverviewMode(true);
//            settings.setUseWideViewPort(true);
//            settings.setSupportZoom(true);
//            settings.setBuiltInZoomControls(true);
//            settings.setDisplayZoomControls(false);
//
//            // 移除系统开放的JS接口
//            removeJavascriptInterface("searchBoxJavaBridge_");
//            removeJavascriptInterface("accessibility");
//            removeJavascriptInterface("accessibilityTraversal");
//        }
//    }
//
//    @Override
//    public void onDownloadStart(String url,
//                                String userAgent,
//                                String contentDisposition,
//                                String mimeType, long contentLength) {
//        Uri uri = Uri.parse(url);
//        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        getContext().startActivity(intent);
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int width = View.MeasureSpec.getSize(widthMeasureSpec);
//        int height = View.MeasureSpec.getSize(heightMeasureSpec);
////        mSavedHeight = height;
//
////        // RefreshView
////        if (mRefreshView != null) {
////            mRefreshView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
////                    View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST));
////            mRefreshViewRect.set(0, -mRefreshView.getMeasuredHeight(),
////                    mRefreshView.getMeasuredWidth(),
////                    0);
////            if (mRefreshDragLength < 0) {
////                mRefreshDragLength = mRefreshView.getMeasuredHeight();
////            }
////        }
//        // WebView
//        mWebView.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
//                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
//        mWebViewRect.set(0, 0, mWebView.getMeasuredWidth(), mWebView.getMeasuredHeight());
//
//        setMeasuredDimension(width, height);
//    }
//
//    @Override
//    protected void onLayout(boolean changed, int l, int t, int r, int b) {
////        // RefreshView
////        if (isBeingDragged && mRefreshView != null && mRefreshView.getParent() == this) {
////            mRefreshView.layout(mRefreshViewRect.left, mRefreshViewRect.top + mDragOffset,
////                    mRefreshViewRect.right, mRefreshViewRect.bottom + mDragOffset);
////        }
//        // WebView
//        mWebView.layout(mWebViewRect.left, mWebViewRect.top/* + mDragOffset*/,
//                mWebViewRect.right, mWebViewRect.bottom/* + mDragOffset*/);
////        // Scroller
////        if (isRestoring && mDragScroller != null && mDragScroller.computeScrollOffset()) {
////            if (mDragScroller.isFinished()) {
////                mDragScroller = null;
////                isRestoring = false;
////                mDragOffset = 0;
////                completeRefresh();
////            } else {
////                mDragOffset = mDragScroller.getCurrY();
////            }
////            post(new Runnable() {
////                @Override
////                public void run() {
////                    requestLayout();
////                }
////            });
////        }
//    }
//
////    @Override
////    public boolean onInterceptTouchEvent(MotionEvent event) {
////        if (mRefreshView == null) {
////            return super.onInterceptTouchEvent(event);
////        }
////
////        final int action = event.getAction();
////        if ((action == MotionEvent.ACTION_MOVE) && (isBeingDragged)) {
////            return true;
////        }
////
////        switch (action & MotionEvent.ACTION_MASK) {
////            case MotionEvent.ACTION_MOVE: {
////                boolean isBeingDraggedY = false;
////                final float y = event.getY();
////                final float yDiff = y - mLastMotionY;
////                if (yDiff > mTouchSlop && mWebView.getScrollY() == 0) {
////                    isBeingDraggedY = true;
////                    mLastMotionY = y;
////                    if (getParent() != null) {
////                        getParent().requestDisallowInterceptTouchEvent(true);
////                    }
////                }
////                isBeingDragged = isBeingDraggedY;
////                mTouchDownY = event.getY();
////            }
////            break;
////            case MotionEvent.ACTION_DOWN: {
////                mLastMotionY = event.getY();
////                mTouchDownY = event.getY();
////                isBeingDragged = false;
////            }
////            break;
////            case MotionEvent.ACTION_UP:
////            case MotionEvent.ACTION_CANCEL: {
////                isBeingDragged = false;
////            }
////            break;
////        }
////        return super.onInterceptTouchEvent(event);
////    }
//
////    @Override
////    public boolean onTouchEvent(MotionEvent event) {
////        final int action = event.getAction();
////        switch (action & MotionEvent.ACTION_MASK) {
////            case MotionEvent.ACTION_MOVE: {
////                int yOffset = (int) (event.getY() - mTouchDownY);
////                if ((yOffset > 0 && mRefreshView != null)) {
////                    updateDragOffset(yOffset / 2);
////                }
//////                mTouchDownY = event.getY();
////                // BeingDragged
////                if (!isNoticeDragged) {
////                    isNoticeDragged = true;
////                    beingDragged();
////                }
////                return true;
////            }
////            case MotionEvent.ACTION_DOWN: {
////                isNoticeDragged = false;
////                mTouchDownY = event.getY();
////                return true;
////            }
////            case MotionEvent.ACTION_UP:
////            case MotionEvent.ACTION_CANCEL: {
////                isNoticeDragged = false;
////                if (mDragOffset != 0 && mDragOffset >= mRefreshDragLength) {
////                    startRefresh();
////                }
////                restoreDragView();
////            }
////            break;
////        }
////        return super.onTouchEvent(event);
////    }
//
//    public WebView getWebView() {
//        return mWebView;
//    }
//
//    public WebSettings getSettings() {
//        return mWebView.getSettings();
//    }
//
//    public void setWebViewClient(WebViewClient client) {
//        mWebView.setWebViewClient(client);
//    }
//
//    public void setWebChromeClient(WebChromeClient client) {
//        mWebView.setWebChromeClient(client);
//    }
//
//    public void loadUrl(String url) {
//        mWebView.loadUrl(url);
//    }
//
//    public void reload() {
//        mWebView.reload();
//    }
//
//    public boolean canGoBack() {
//        return mWebView.canGoBack();
//    }
//
//    public void goBack() {
//        mWebView.goBack();
//    }
//
////    /**
////     * 获得WebView的高度
////     * @return WebView的高度
////     */
////    public int getSavedHeight() {
////        return mSavedHeight;
////    }
//
////    public void setRefreshView(@LayoutRes int id) {
////        mRefreshViewId = id;
////        inflateRefreshView();
////    }
//
////    protected void onRefresh() {
////        mWebView.reload();
////    }
//
////    private void completeRefresh() {
////        if (mRefreshView != null) {
////            removeView(mRefreshView);
////        }
////    }
//
////    private void updateDragOffset(int offset) {
////        mDragOffset = offset;
////        noticeDragOffsetChanged();
////        requestLayout();
////    }
//
////    private void beingDragged() {
////        if (mDragOffset > 0) {
////            inflateRefreshView();
////            if (mRefreshView != null) {
////                addView(mRefreshView);
////            }
////        }
////    }
//
////    private void noticeDragOffsetChanged() {
////        if (mDragOffset > 0 && mRefreshView != null) {
////            mRefreshView.updateDragOffset(mDragOffset, mRefreshDragLength);
////        } else if (mDragOffset == 0) {
////            if (mRefreshView != null) {
////                mRefreshView.updateDragOffset(0, mRefreshDragLength);
////            }
////        }
////    }
//
////    private void restoreDragView() {
////        mDragScroller = new Scroller(getContext(), new DecelerateInterpolator());
////        mDragScroller.startScroll(0, mDragOffset, 0, -mDragOffset, 150);
////        isRestoring = true;
////        requestLayout();
////    }
//
////    private void startRefresh() {
////        onRefresh();
////    }
//
////    private void inflateRefreshView() {
////        try {
////            View tmp = LayoutInflater.from(getContext())
////                    .inflate(mRefreshViewId, this, false);
////            if (tmp instanceof BaseRecyclerDragView) {
////                mRefreshView = (BaseRecyclerDragView) tmp;
////            } else {
////                mRefreshView = null;
////            }
////        } catch (Exception e) {
////            mRefreshView = null;
////        }
////    }
//}
