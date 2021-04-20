package com.scliang.core.base.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.scliang.core.R;
import com.scliang.core.base.BaseApplication;
import com.scliang.core.base.Data;
import com.scliang.core.base.DataCallback;
import com.scliang.core.base.Questioner;

import java.util.Map;

import retrofit2.Call;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/11/30.
 */
public abstract class BaseDialog extends DialogFragment
        implements Questioner, DialogInterface.OnKeyListener {
    protected String mRequestId;
    protected SimpleLoadDialog mSimpleLoadDialog;
    protected final byte[] mLoadSync = new byte[]{0};
    protected Handler mUIHandler;
    protected FrameLayout mContainer;
    protected DialogInterface.OnDismissListener mOnDismissListener;

    protected abstract View onCreateContextView(LayoutInflater inflater, ViewGroup container);

    protected FrameLayout.LayoutParams onCreateContextLayoutParams() {
        return null;
    }

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_base, container, false);
        mContainer = view.findViewById(R.id.container);
        View child = onCreateContextView(inflater, mContainer);
        if (child != null) {
            FrameLayout.LayoutParams flp = onCreateContextLayoutParams();
            if (flp == null) flp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
            mContainer.addView(child, flp);
        }
        return mContainer;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mUIHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public final void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setCancelable(false);
            dialog.setOnKeyListener(this);
        }
        onStart(dialog);
    }

    protected void onStart(Dialog dialog) {}

    public int showFullscreen(FragmentTransaction transaction, String tag) {
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.BaseDialog_Fullscreen);
        try {
            return super.show(transaction, tag);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int show(FragmentTransaction transaction, String tag) {
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.BaseDialog);
        try {
            return super.show(transaction, tag);
        } catch (Exception e) {
            return -1;
        }
    }

    public void showFullscreen(FragmentManager manager, String tag) {
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.BaseDialog_Fullscreen);
        try { super.show(manager, tag); } catch (Exception ignored) {}
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.BaseDialog);
        try { super.show(manager, tag); } catch (Exception ignored) {}
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (mOnDismissListener != null) mOnDismissListener.onDismiss(dialog);
        super.onDismiss(dialog);
    }

    @Override
    public String giveRequestId() {if (TextUtils.isEmpty(mRequestId)) {
        mRequestId = getClass().getName() + "-Questioner-" + System.currentTimeMillis();
    }
        return mRequestId;
    }

    @Override
    public boolean responseCallbackable() {
        return getActivity() != null;
    }

    @Override
    public void onQuestionerError(int code, String msg) {
    }

    @Override
    public boolean questionerResponsable() {
        return false;
    }

    @Override
    public void onQuestionerResponseSuccess() {
    }

    /**
     * 请求网络数据
     * @param bParams b Params
     * @param <T> Result
     * @return Call
     */
    public <S,T> Call<T> request(Class<S> SERVICE, String apiName,
                                 Map<String, String> bParams, DataCallback<T> callback) {
        return Data.getInstance().request(this, SERVICE, apiName, bParams, callback);
    }

    /**
     * 请求网络数据
     * @param params Request Params
     * @param <T> Result
     * @return Call
     */
    public <S,T> Call<T> request(Class<S> SERVICE, String apiName,
                                 String[] params, DataCallback<T> callback) {
        return Data.getInstance().request(this, SERVICE, apiName, params, callback);
    }

    /**
     * 取消当前页面中的给定的网络请求
     * @param call 待取消的网络请求
     */
    public void cancel(Call<?> call) {
        Data.getInstance().cancel(this, call);
    }

    /**
     * 显示一个只有圆形ProgressBar的提示对话框
     * 只负责显示出来，关闭需要自行控制
     */
    public void showSimpleLoadDialog() {
        synchronized (mLoadSync) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mLoadSync) {
                        if (mSimpleLoadDialog == null) {
                            mSimpleLoadDialog = new SimpleLoadDialog();
                            mSimpleLoadDialog.show(getChildFragmentManager(), getClass().getName());
                        }
                    }
                }
            });
        }
    }

    /**
     * 关闭正在显示着的只有圆形ProgressBar的提示对话框
     */
    public void closeSimpleLoadDialog() {
        synchronized (mLoadSync) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mLoadSync) {
                        if (mSimpleLoadDialog != null) {
                            mSimpleLoadDialog.dismissAllowingStateLoss();
                            mSimpleLoadDialog = null;
                        }
                    }
                }
            });
        }
    }

    /**
     * 发布一个本地广播
     * @param action Action
     * @param args Args
     */
    public void sendLocalBroadcast(String action, Bundle args) {
        BaseApplication.getApp().sendLocalBroadcast(action, args);
    }

    /**
     * 设置对话框关闭时的回调
     * @param listener listener
     */
    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    /**
     * 设置主题背景颜色
     * @param color 背景颜色
     */
    public void setContainerBackgroundColor(@ColorInt int color) {
        mContainer.setBackgroundColor(color);
    }

    public void postDelayed(Runnable action, long delayMillis) {
        if (mContainer != null) {
            mContainer.postDelayed(action, delayMillis);
        }
    }

    /**
     * 安全方式调用getString
     * @param resId resId
     * @return string
     */
    public String getStringSafe(@StringRes int resId) {
        if (getActivity() == null) {
            return "";
        } else {
            return getString(resId);
        }
    }

    /**
     * 安全方式调用getString
     * @param resId resId
     * @return string
     */
    public String getStringSafe(@StringRes int resId, Object... formatArgs) {
        if (getActivity() == null) {
            return "";
        } else {
            return getString(resId, formatArgs);
        }
    }
}
