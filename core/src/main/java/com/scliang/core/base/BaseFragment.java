package com.scliang.core.base;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.scliang.core.analysis.Analysis;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/9/28.
 */
public abstract class BaseFragment
        extends Fragment
        implements Questioner {
    private String mRequestId;
    private boolean mQuestionerResponsable;
    private SoftReference<PushFragmentContainer> mPushContainer;

    @Override
    public String giveRequestId() {
        if (TextUtils.isEmpty(mRequestId)) {
            mRequestId = getClass().getName() + "-Questioner-" + System.currentTimeMillis();
        }
        return mRequestId;
    }

    @Override
    public boolean responseCallbackable() {
        return getActivity() != null;
    }

    protected View onCreateViewHere(@NonNull LayoutInflater inflater,
                                    @Nullable ViewGroup container,
                                    @Nullable Bundle savedInstanceState) {
        return null;
    }

    protected void onViewCreatedHere(@NonNull View view,
                                     @Nullable Bundle savedInstanceState) {
    }

    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater,
                                   @Nullable ViewGroup container,
                                   @Nullable Bundle savedInstanceState) {
        View contentView = onCreateViewHere(inflater, container, savedInstanceState);
        // 保证onViewCreated被调用
        if (contentView == null) {
            contentView = new FrameLayout(inflater.getContext());
        }
        return contentView;
    }

    @Override
    public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 启动时获取一次配置信息
        Configuration configuration = getResources().getConfiguration();
        onConfigurationChanged(configuration);

        // 调用子类
        onViewCreatedHere(view, savedInstanceState);

        // 调用Activity
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).onRootFragmentViewCreated(view, savedInstanceState);
        }
    }

    @Override
    public final void onResume() {
        super.onResume();
        // 统计分析
        Analysis.getInstance().onPageStart(getClass().getName());
        // sub-class
        onResume(null);
    }

    public void onResume(Bundle args) {
    }

    @Override
    public final void onPause() {
        super.onPause();
        // 统计分析
        Analysis.getInstance().onPageEnd(getClass().getName());
        // sub-class
        onPause(null);
    }

    public void onPause(Bundle args) {
    }

    @Override
    public void onDestroyView() {
        // 取消所有数据请求
        Data.getInstance().cancel(this);
        // super
        super.onDestroyView();
    }

    @Nullable
    public <T extends View> T findViewById(@IdRes int id) {
        final View view = getView();
        if (view == null) {
            return null;
        } else {
            T findView = view.findViewById(id);
            if (findView == null) {
                BaseActivity activity = (BaseActivity) getActivity();
                if (activity == null) {
                    return null;
                } else {
                    findView = (T) activity.findViewById(id);
                }
            }
            return findView;
        }
    }

    void setPushContainer(PushFragmentContainer container) {
        mPushContainer = new SoftReference<>(container);
    }

    /**
     * 关闭所属的PushDialog(如果存在)
     */
    public void closePushDialog() {
        if (mPushContainer != null) {
            PushFragmentContainer dialog = mPushContainer.get();
            if (dialog != null) {
                dialog.dismiss();
            }
        }
    }

    /**
     * 返回窗口的Toolbar
     */
    public Toolbar getToolbar() {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            return ((BaseActivity) activity).getToolbar();
        } else {
            return null;
        }
    }

    /**
     * Toolbar右边动作按钮回调
     */
    protected void onMenuItemClick(int groupId, int itemId) {
    }

    final void keyboardOpened() {
        onKeyboardOpened();
    }

    final void keyboardClosed() {
        onKeyboardClosed();
    }

    // 在Resize设置下调用了AndroidBug5497Workaround方法后，键盘打开后
    protected void onKeyboardOpened() {
    }

    // 在Resize设置下调用了AndroidBug5497Workaround方法后，键盘关闭后
    protected void onKeyboardClosed() {
    }

    final void keyboardOpenedB() {
        onKeyboardOpenedB();
    }

    final void keyboardClosedB() {
        onKeyboardClosedB();
    }

    // 在Resize设置下调用了AndroidBug5497Workaround方法后，键盘打开后
    protected void onKeyboardOpenedB() {
    }

    // 在Resize设置下调用了AndroidBug5497Workaround方法后，键盘关闭后
    protected void onKeyboardClosedB() {
    }

    final boolean keyDown(int keyCode, KeyEvent event) {
        return onKeyDown(keyCode, event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
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
     * 此页面触发的所有请求，如果请求中响应符合ShotOff、Banned、Reject的要求，就会调用到这个方法
     */
    @Override
    public void onQuestionerError(int code, String msg) {
        // 清除此页面所有的网络请求
        Data.getInstance().cancel(this);
        // 递交给BaseApplication来处理
        BaseApplication.getApp().questionerError(code, msg);
    }

//    /**
//     * 此页面触发的所有请求，如果请求中响应符合Banned的要求，就会调用到这个方法
//     */
//    @Override
//    public void onQuestionerBanned(String msg) {
//        // 清除此页面所有的网络请求
//        Data.getInstance().cancel(this);
//        // 递交给BaseApplication来处理
//        BaseApplication.getApp().questionerBanned(msg);
//    }

    /**
     * 判断是否启用onQuestionerResponseSuccess方法
     * @return true: 请求isSuccess时onQuestionerResponseSuccess会被调用
     */
    @Override
    public boolean questionerResponsable() {
        return mQuestionerResponsable;
    }

    /**
     * questionerResponsable返回true时，
     * 此页面触发的所有请求，如果结果类含有isSuccess方法并返回true，就会调用到这个方法
     */
    @Override
    public void onQuestionerResponseSuccess() {
        // 隐藏MaskView，显示ContentView
        setContentContextViewVisibility(true);
        hideContentMaskView();
    }

    final void requestPermissionsResult(String permission, int grantResult) {
        // 通知子类
        onRequestPermissionsResult(permission, grantResult == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * 权限请求结果回调
     * @param permission 权限名称
     * @param granted 是否请求成功
     */
    public void onRequestPermissionsResult(String permission, boolean granted) {
        // nothing
        Logger.d(getClass().getName(),
                "onRequestPermissionsResult : " +
                        permission + (granted ? "true" : "false"));
    }

    /**
     * 根据手机的分辨率dp单位转为px像素
     * @param dp dp
     * @return px
     */
    public int dp2px(float dp) {
        try {
            final float scale = getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
        } catch (Throwable e) {
            try {
                final Context context = BaseApplication.getApp();
                if (context == null) { return (int) dp; }
                final float scale = context.getResources().getDisplayMetrics().density;
                return (int) (dp * scale + 0.5f);
            } catch (Throwable ee) {
                return (int) dp;
            }
        }
    }

    /**
     * 发布一个本地广播
     * @param action Action
     * @param args Args
     */
    public void sendLocalBroadcast(String action, Bundle args) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).sendLocalBroadcast(action, args);
        }
    }

    /**
     * 处理收到的本地广播
     * @param action Action
     * @param args Args
     */
    public void onReceiveLocalBroadcastReceiver(String action, Bundle args) {
        Logger.d(getClass().getName(), "ReceiveLocalBroadcastReceiver Action: " + action);
    }

    /**
     * 每当网络连接后,回调到这里
     * @param type 连接后的网络类型  one of {@link ConnectivityManager#TYPE_MOBILE}, {@link
     * ConnectivityManager#TYPE_WIFI}, {@link ConnectivityManager#TYPE_WIMAX}, {@link
     * ConnectivityManager#TYPE_ETHERNET},  {@link ConnectivityManager#TYPE_BLUETOOTH}, or other
     * types defined by {@link ConnectivityManager}
     */
    final void onNetworkConnected(int type) {
        Logger.d(getClass().getName(), "NetworkConnected Type: " + type);
        if (checkRefreshAfterNetworkConnected(type)) {
            onRefreshAfterNetworkConnected(type);
        }
        List<Fragment> fragments = getChildFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment frag : fragments) {
                if (frag instanceof BaseFragment) {
                    ((BaseFragment) frag).onNetworkConnected(type);
                }
            }
        }
    }

    /**
     * 每当网络断开后,回调到这里
     */
    public void onNetworkDisconnected() {
        Logger.d(getClass().getName(), "NetworkDisconnected");
    }

//    /**
//     * 每当有来电时回调到这里
//     */
//    public void onCallRinging() {
//        Logger.d(getClass().getName(), "CallRinging");
//    }

    /**
     * 判断每次网络连接后是否需要刷新
     * @return true: 每当网络连接后就会回调到onRefreshAfterNetworkConnected方法里面;
     *         false: 忽略-不做任何操作
     */
    protected boolean checkRefreshAfterNetworkConnected(int type) {
        return false;
    }

    /**
     * 每当网络连接后，根据checkRefreshAfterNetworkConnected的判断是否回调到这个方法
     */
    public void onRefreshAfterNetworkConnected(int type) {
        // nothing
    }

    /**
     * 判断当前网络是否为移动网络
     */
    public boolean isNetworkMobile() {
        Activity activity = getActivity();
        return activity != null && ((BaseActivity) activity).isNetworkMobile();
    }

    /**
     * 判断是否有可用网络
     */
    public boolean hasConnectedNetwork() {
        Activity activity = getActivity();
        return activity != null && ((BaseActivity) activity).hasConnectedNetwork();
    }

    /**
     * 判断是否需要显示无网络MaskView
     */
    public void checkShowNoConnectedNetworkMaskView() {
        if (!hasConnectedNetwork()) {
            showContentMaskView();
        }
    }

    /**
     * 点击网络错误提示View中重新加载按钮时
     */
    public void onNetworkReload() {
        Logger.d(getClass().getName(), "Network Reload");
    }

    /**
     * 所属Activity中onCreate之前调用
     * @param activity 所属Activity
     * @param savedInstanceState onCreate中的savedInstanceState
     */
    protected void onPreActivityCreate(@NonNull BaseActivity activity,
                                       @Nullable Bundle savedInstanceState) {
        // 所属BaseActivity中onCreate之前调用
    }

    /**
     * 显示Toolbar的导航按钮
     */
    public void showToolbarNavigationIcon() {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).showToolbarNavigationIcon();
        }
    }

    /**
     * 隐藏Toolbar的导航按钮
     */
    public void hideToolbarNavigationIcon() {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).hideToolbarNavigationIcon();
        }
    }

    /**
     * 设置Toolbar的背景
     * @param background
     */
    public void setToolbarBackground(Drawable background) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarBackground(background);
        }
    }

    /**
     * 设置为给定的ToolbarType
     * @param type
     */
    public void setToolbarType(BaseActivity.ToolbarType type) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarType(type);
        }
    }

    /**
     * 设置Toolbar-BottomLine是否显示
     * @param show true:显示BottomLine;false:不显示
     */
    public void setToolbarBottomLineVisibility(boolean show) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarBottomLineVisibility(show);
        }
    }

    /**
     * 设置Toolbar-BottomLine是否显示
     * @param show true:显示BottomLine;false:不显示
     */
    public void setToolbarBottomLineVisibility(boolean show, @ColorInt int color) {
        Activity activity = getActivity();
        if (activity != null && activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarBottomLineVisibility(show, color);
        }
    }

    /**
     * 设置Toolbar的右边操作动作按钮
     * @param menuId
     */
    public void setToolbarMenu(@MenuRes int menuId) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarMenu(menuId);
        }
    }

    /**
     * 设置Toolbar居中的标题
     * @param title
     */
    public void setToolbarCenterTitle(@StringRes int title) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarCenterTitle(title);
        }
    }

    /**
     * 设置Toolbar居中的标题
     * @param title
     */
    public void setToolbarCenterTitle(CharSequence title) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarCenterTitle(title);
        }
    }

    /**
     * 设置Toolbar居中的标题
     * @param title
     * @param color 如:0xffcccccc
     * @param size 单位:DIP
     */
    public void setToolbarCenterTitle(CharSequence title, int color, int size) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarCenterTitle(title, color, size);
        }
    }

    /**
     * 设置Toolbar居中的自定义视图
     * @param view
     */
    public void setToolbarCenterCustomView(View view) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarCenterCustomView(view);
        }
    }

    /**
     * 设置Toolbar左边自定义视图
     * @param view
     */
    public void setToolbarLeftCustomView(View view) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarLeftCustomView(view);
        }
    }

    /**
     * 设置Toolbar右边自定义视图
     * @param view
     */
    public void setToolbarRightCustomView(View view) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarRightCustomView(view);
        }
    }

    /**
     * 设置是否右滑支持
     */
    public void setSlideable(boolean slideable) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setSlideable(slideable);
        }
    }

    /**
     * 设置内容蒙版View，用于覆盖在通过setContentView方法设置的View之上，并拦截点击事件
     */
    public void setContentMaskView(@LayoutRes int id) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setContentMaskView(id);
        }
    }

    /**
     * 设置内容蒙版View，用于覆盖在通过setContentView方法设置的View之上，并拦截点击事件
     */
    public void setContentMaskView(View view) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setContentMaskView(view);
        }
    }

    /**
     * 是否显示内容蒙版View
     */
    public void setContentMaskViewVisibility(boolean show, @IdRes int reloadId) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setContentMaskViewVisibility(show, reloadId);
        }
        // 这个页面调用了checkShowNoConnectedNetworkMaskView
        // 无网络时会显示无网络提示蒙版
        // 所以这里需要返回true，请求成功后框架会把无网络提示蒙版关闭
        mQuestionerResponsable = show;
    }

    /**
     * 显示内容蒙版View
     */
    public void showContentMaskView() {
        setContentMaskViewVisibility(true, 0);
    }

    /**
     * 显示内容蒙版View
     */
    public void showContentMaskView(@IdRes int reloadId) {
        setContentMaskViewVisibility(true, reloadId);
    }

    /**
     * 隐藏内容蒙版View
     */
    public void hideContentMaskView() {
        setContentMaskViewVisibility(false, 0);
    }

    /**
     * 是否显示内容View，通过setContentView设置的View
     */
    public void setContentContextViewVisibility(boolean show) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setContentContextViewVisibility(show);
        }
    }

    /**
     * 判断ContentMaskView是否正在显示
     */
    public boolean isContentMaskViewShowing() {
        Activity activity = getActivity();
        return activity instanceof BaseActivity &&
                ((BaseActivity) activity).isContentMaskViewShowing();
    }

    /**
     * 所属的ActivityFinish之前时调用在这个方法里面
     */
    public void onPreFinish(BaseActivity activity) {
        // nothing
    }

    /**
     * NewIntent从所属BaseActivity调用过来
     */
    public void onNewIntent(Intent intent) {
        // nothing
    }

    /**
     * 点击返回操作
     * @return true:Activity不再处理返回; false:需要Activity处理
     */
    public boolean onBackPressed() {
        return false;
    }

    /**
     * 关闭所属的Activity
     */
    public void finish() {
        finish(null);
    }

    /**
     * 关闭所属的Activity
     * @param data setResult中的返回数据
     */
    public void finish(Bundle data) {
        Intent intent = null;
        if (data != null) {
            intent = new Intent();
            intent.putExtras(data);
        }
        BaseActivity activity = (BaseActivity) getActivity();
        if (activity != null) {
            if (intent != null) {
                activity.setResult(Activity.RESULT_OK, intent);
            }
            activity.finish();
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

    /**
     * 显示一个Toast
     */
    public void toast(String text) {
        toast(text, null);
    }

    /**
     * 显示一个Toast
     */
    public void toast(String text, Runnable dismissRunnable) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            BaseActivity act = (BaseActivity) activity;
            act.toast(text, dismissRunnable);
        }
    }

    /**
     * 显示一个从底部动画推上来的对话框
     * @param contentFragment 要显示的内容Fragment
     * @return 如果返回null,说明没有正常显示对话框
     */
    public PushFragmentContainer showPushDialog(BaseFragment contentFragment) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            return ((BaseActivity) activity).showPushDialog(contentFragment);
        } else {
            return null;
        }
    }

    /**
     * 显示一个从底部动画推上来的对话框
     * @param contentFragment 要显示的内容Fragment
     * @return
     */
    public PushFragmentContainer showPushDialog(final BaseFragment contentFragment, final DialogInterface.OnDismissListener onDismissListener) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            return ((BaseActivity) activity).showPushDialog(contentFragment, onDismissListener);
        } else {
            return null;
        }
    }

    /**
     * 显示一个只有圆形ProgressBar的提示对话框
     * 只负责显示出来，关闭需要自行控制
     */
    public void showSimpleLoadDialog_Fullscreen() {
        if (isResumed()) {
            Activity activity = getActivity();
            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).showSimpleLoadDialog_Fullscreen();
            }
        }
    }

    /**
     * 显示一个只有圆形ProgressBar的提示对话框
     * 只负责显示出来，关闭需要自行控制
     */
    public void showSimpleLoadDialog() {
        if (isResumed()) {
            Activity activity = getActivity();
            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).showSimpleLoadDialog();
            }
        }
    }

    /**
     * 显示一个Airbnb动画的提示对话框
     * 只负责显示出来，关闭需要自行控制
     */
    public void showSimpleLoadDialog_Fullscreen(final String assetsJsonFileName) {
        if (isResumed()) {
            Activity activity = getActivity();
            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).showSimpleLoadDialog_Fullscreen(assetsJsonFileName);
            }
        }
    }

    /**
     * 显示一个Airbnb动画的提示对话框
     * 只负责显示出来，关闭需要自行控制
     */
    public void showSimpleLoadDialog(final String assetsJsonFileName) {
        if (isResumed()) {
            Activity activity = getActivity();
            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).showSimpleLoadDialog(assetsJsonFileName);
            }
        }
    }

    /**
     * 更新正在显示着的SimpleLoadDialog中的Tip
     */
    public void updateSimpleLoadDialogTip(final String tip) {
        if (isResumed()) {
            Activity activity = getActivity();
            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).updateSimpleLoadDialogTip(tip);
            }
        }
    }

    /**
     * 更新正在显示着的SimpleLoadDialog中的Tip
     */
    public void updateSimpleLoadDialogTip(final String tip,
                                          final int tipColor,
                                          final int tipSize) {
        if (isResumed()) {
            Activity activity = getActivity();
            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).updateSimpleLoadDialogTip(tip, tipColor, tipSize);
            }
        }
    }

    /**
     * 关闭正在显示着的只有圆形ProgressBar的提示对话框
     */
    public void closeSimpleLoadDialog() {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).closeSimpleLoadDialog();
        }
    }

    /**
     * 关闭正在显示着的只有圆形ProgressBar的提示对话框
     */
    public void closeSimpleLoadDialog(long delayMillis) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).closeSimpleLoadDialog(delayMillis);
        }
    }

    /**
     * 显示一个只有圆形ProgressBar的提示View
     * 只负责显示出来，关闭需要自行控制
     */
    public void showSimpleLoadToast() {
        if (isResumed()) {
            Activity activity = getActivity();
            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).showSimpleLoadToast();
            }
        }
    }

    /**
     * 关闭正在显示着的只有圆形ProgressBar的提示View
     */
    public void closeSimpleLoadToast() {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).closeSimpleLoadToast();
        }
    }

    /**
     * 显示确认对话框
     * @param text 显示的文本
     * @param ok 确定按钮的文本
     * @param okCallback 点击[确定]按钮的回调
     * @param dismissListener dlg关闭监听回调
     */
    public void showConfirmDialog(final CharSequence text,
                                  final CharSequence ok,
                                  final Runnable okCallback,
                                  final DialogInterface.OnDismissListener dismissListener) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            BaseActivity act = (BaseActivity) activity;
            act.showConfirmDialog(text, ok, okCallback, dismissListener);
        }
    }

    /**
     * 显示确认对话框
     * @param text 显示的文本
     * @param ok 确定按钮的文本
     * @param cancel 取消按钮的文本
     * @param okCallback 点击[确定]按钮的回调
     * @param cancelCallback 点击[取消]按钮的回调
     * @param dismissListener dlg关闭监听回调
     */
    public void showConfirmDialog(final CharSequence text,
                                  final CharSequence ok,
                                  final CharSequence cancel,
                                  final Runnable okCallback,
                                  final Runnable cancelCallback,
                                  final DialogInterface.OnDismissListener dismissListener) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            BaseActivity act = (BaseActivity) activity;
            act.showConfirmDialog(text, ok, cancel, okCallback, cancelCallback, dismissListener);
        }
    }

    /**
     * 打开App系统设置页面
     * @param text 提示文案
     */
    public void showToSettingConfirmDialog(CharSequence text) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            BaseActivity act = (BaseActivity) activity;
            act.showToSettingConfirmDialog(text);
        }
    }

    /**
     * 启动一个新页面
     */
    public void startFragment(Class<? extends BaseFragment> fragment) {
        startFragment(fragment, null, null);
    }

    /**
     * 启动一个新页面
     */
    public void startFragment(Class<? extends BaseFragment> fragment,
                              Bundle args) {
        startFragment(fragment, null, args);
    }

    /**
     * 启动一个新页面
     */
    public void startFragment(Class<? extends BaseFragment> fragment,
                              int requestCode) {
        startFragment(fragment, null, null, requestCode);
    }

    /**
     * 启动一个新页面
     */
    public void startFragment(Class<? extends BaseFragment> fragment,
                              Bundle args,
                              int requestCode) {
        startFragment(fragment, null, args, requestCode);
    }

    /**
     * 启动一个新页面
     */
    public void startFragment(Class<? extends BaseFragment> fragment,
                              Class<? extends BaseActivity> container) {
        startFragment(fragment, container, null);
    }

    /**
     * 启动一个新页面
     */
    public void startFragment(Class<? extends BaseFragment> fragment,
                              Class<? extends BaseActivity> container,
                              Bundle args) {
        startFragment(fragment, container, args, 0);
    }

    /**
     * 启动一个新页面
     */
    public void startFragment(Class<? extends BaseFragment> fragment,
                              Class<? extends BaseActivity> container,
                              Bundle args,
                              int requestCode) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            BaseActivity act = (BaseActivity) activity;
            act.startFragment(fragment, container, args, requestCode);
        }
    }

    /**
     * Analysis事件统计
     * @param eventId 自定义统计事件ID
     */
    public void analysisEvent(String eventId) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            BaseActivity act = (BaseActivity) activity;
            act.analysisEvent(eventId);
        }
    }

    /**
     * Analysis事件统计
     * @param eventId 自定义统计事件ID
     * @param params Event Params
     */
    public void analysisEvent(String eventId, HashMap<String, String> params) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            BaseActivity act = (BaseActivity) activity;
            act.analysisEvent(eventId, params);
        }
    }
}
