package com.scliang.core.base;

import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * SCore
 * Created by ShangChuanliang
 * on 16/11/22.
 */
public class AndroidBug5497Workaround {
    // For more information, see https://code.google.com/p/android/issues/detail?id=5497
    // To use this class, simply invoke assistActivity() on an Activity that already has its content view set.
    public static void assistActivity(BaseActivity activity) {
        new AndroidBug5497Workaround(activity);
    }

    private BaseActivity mActivity;
    private View mChildOfContent;
    private int usableHeightPrevious;
    private FrameLayout.LayoutParams frameLayoutParams;
    private int statusBarH;
    private boolean mActOpened;

    private AndroidBug5497Workaround(BaseActivity activity) {
        mActivity = activity;
        statusBarH = mActivity.getStatusBarHeight();
        FrameLayout content = (FrameLayout) mActivity.getWindow().getDecorView();
        if (content != null) {
            mChildOfContent = content.getChildAt(0);
            mChildOfContent.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    possiblyResizeChildOfContent();
                }
            });
            frameLayoutParams = (FrameLayout.LayoutParams) mChildOfContent.getLayoutParams();
        }
    }

    private void possiblyResizeChildOfContent() {
        int usableHeightNow = computeUsableHeight();
        if (usableHeightNow != usableHeightPrevious) {
            final boolean opened;
            int usableHeightSansKeyboard = mChildOfContent/*.getRootView()*/.getHeight();
            int heightDifference = usableHeightSansKeyboard - usableHeightNow;
//            if (!BaseActivity.sUseFullScreen) {
//                usableHeightSansKeyboard -= getNavigationBarHeight(mActivity);
//            }
            if (heightDifference > (usableHeightSansKeyboard / 4)) {
                if (BaseActivity.sUseFullScreen) {
                    if (usableHeightSansKeyboard == getDisplayRealHeight(mActivity)) {
                        usableHeightSansKeyboard += getNavigationBarHeight(mActivity);
                    }
                }
                opened = true;
            } else {
                opened = false;
            }

            usableHeightPrevious = usableHeightNow;

            if (opened) {
                mActivity.keyboardOpened();
            } else {
                if (mActOpened) {
                    mActivity.keyboardClosedB();
                }
            }

            if (opened) {
                // keyboard probably just became visible
                if (BaseActivity.sUseFullScreen) {
                    frameLayoutParams.height = usableHeightSansKeyboard - heightDifference;
                } else {
                    frameLayoutParams.height = usableHeightSansKeyboard;
                }
            } else {
                // keyboard probably just became hidden
                frameLayoutParams.height = usableHeightSansKeyboard;
            }

            mChildOfContent.requestLayout();
            if (opened) {
                mActivity.keyboardOpenedB();
            } else {
                if (mActOpened) {
                    mActivity.keyboardClosed();
                }
            }

            mActOpened = opened;
        }
    }

    private int computeUsableHeight() {
        Rect r = new Rect();
        mChildOfContent.getWindowVisibleDisplayFrame(r);
        if (r.top == 0) {
            r.top = statusBarH;//状态栏目的高度
        }
        return (r.bottom - r.top);
    }

    private int getDisplayRealHeight(BaseActivity activity) {
        DisplayMetrics realSize = new DisplayMetrics();
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getMetrics(realSize);
        display.getRealMetrics(realSize);
        return realSize.heightPixels;
    }

    private int getDisplayHeight(BaseActivity activity) {
        DisplayMetrics realSize = new DisplayMetrics();
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getMetrics(realSize);
        return realSize.heightPixels;
    }

    private int getNavigationBarHeight(BaseActivity activity) {
        return getDisplayRealHeight(activity) - getDisplayHeight(activity);
    }
}
