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
        // ??????onViewCreated?????????
        if (contentView == null) {
            contentView = new FrameLayout(inflater.getContext());
        }
        return contentView;
    }

    @Override
    public final void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ?????????????????????????????????
        Configuration configuration = getResources().getConfiguration();
        onConfigurationChanged(configuration);

        // ????????????
        onViewCreatedHere(view, savedInstanceState);

        // ??????Activity
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).onRootFragmentViewCreated(view, savedInstanceState);
        }
    }

    @Override
    public final void onResume() {
        super.onResume();
        // ????????????
        Analysis.getInstance().onPageStart(getClass().getName());
        // sub-class
        onResume(null);
    }

    public void onResume(Bundle args) {
    }

    @Override
    public final void onPause() {
        super.onPause();
        // ????????????
        Analysis.getInstance().onPageEnd(getClass().getName());
        // sub-class
        onPause(null);
    }

    public void onPause(Bundle args) {
    }

    @Override
    public void onDestroyView() {
        // ????????????????????????
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
     * ???????????????PushDialog(????????????)
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
     * ???????????????Toolbar
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
     * Toolbar????????????????????????
     */
    protected void onMenuItemClick(int groupId, int itemId) {
    }

    final void keyboardOpened() {
        onKeyboardOpened();
    }

    final void keyboardClosed() {
        onKeyboardClosed();
    }

    // ???Resize??????????????????AndroidBug5497Workaround???????????????????????????
    protected void onKeyboardOpened() {
    }

    // ???Resize??????????????????AndroidBug5497Workaround???????????????????????????
    protected void onKeyboardClosed() {
    }

    final void keyboardOpenedB() {
        onKeyboardOpenedB();
    }

    final void keyboardClosedB() {
        onKeyboardClosedB();
    }

    // ???Resize??????????????????AndroidBug5497Workaround???????????????????????????
    protected void onKeyboardOpenedB() {
    }

    // ???Resize??????????????????AndroidBug5497Workaround???????????????????????????
    protected void onKeyboardClosedB() {
    }

    final boolean keyDown(int keyCode, KeyEvent event) {
        return onKeyDown(keyCode, event);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * ??????????????????
     * @param bParams b Params
     * @param <T> Result
     * @return Call
     */
    public <S,T> Call<T> request(Class<S> SERVICE, String apiName,
                                 Map<String, String> bParams, DataCallback<T> callback) {
        return Data.getInstance().request(this, SERVICE, apiName, bParams, callback);
    }

    /**
     * ??????????????????
     * @param params Request Params
     * @param <T> Result
     * @return Call
     */
    public <S,T> Call<T> request(Class<S> SERVICE, String apiName,
                                 String[] params, DataCallback<T> callback) {
        return Data.getInstance().request(this, SERVICE, apiName, params, callback);
    }

    /**
     * ?????????????????????????????????????????????
     * @param call ????????????????????????
     */
    public void cancel(Call<?> call) {
        Data.getInstance().cancel(this, call);
    }

    /**
     * ????????????????????????????????????????????????????????????ShotOff???Banned???Reject???????????????????????????????????????
     */
    @Override
    public void onQuestionerError(int code, String msg) {
        // ????????????????????????????????????
        Data.getInstance().cancel(this);
        // ?????????BaseApplication?????????
        BaseApplication.getApp().questionerError(code, msg);
    }

//    /**
//     * ????????????????????????????????????????????????????????????Banned???????????????????????????????????????
//     */
//    @Override
//    public void onQuestionerBanned(String msg) {
//        // ????????????????????????????????????
//        Data.getInstance().cancel(this);
//        // ?????????BaseApplication?????????
//        BaseApplication.getApp().questionerBanned(msg);
//    }

    /**
     * ??????????????????onQuestionerResponseSuccess??????
     * @return true: ??????isSuccess???onQuestionerResponseSuccess????????????
     */
    @Override
    public boolean questionerResponsable() {
        return mQuestionerResponsable;
    }

    /**
     * questionerResponsable??????true??????
     * ??????????????????????????????????????????????????????isSuccess???????????????true??????????????????????????????
     */
    @Override
    public void onQuestionerResponseSuccess() {
        // ??????MaskView?????????ContentView
        setContentContextViewVisibility(true);
        hideContentMaskView();
    }

    final void requestPermissionsResult(String permission, int grantResult) {
        // ????????????
        onRequestPermissionsResult(permission, grantResult == PackageManager.PERMISSION_GRANTED);
    }

    /**
     * ????????????????????????
     * @param permission ????????????
     * @param granted ??????????????????
     */
    public void onRequestPermissionsResult(String permission, boolean granted) {
        // nothing
        Logger.d(getClass().getName(),
                "onRequestPermissionsResult : " +
                        permission + (granted ? "true" : "false"));
    }

    /**
     * ????????????????????????dp????????????px??????
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
     * ????????????????????????
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
     * ???????????????????????????
     * @param action Action
     * @param args Args
     */
    public void onReceiveLocalBroadcastReceiver(String action, Bundle args) {
        Logger.d(getClass().getName(), "ReceiveLocalBroadcastReceiver Action: " + action);
    }

    /**
     * ?????????????????????,???????????????
     * @param type ????????????????????????  one of {@link ConnectivityManager#TYPE_MOBILE}, {@link
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
     * ?????????????????????,???????????????
     */
    public void onNetworkDisconnected() {
        Logger.d(getClass().getName(), "NetworkDisconnected");
    }

//    /**
//     * ?????????????????????????????????
//     */
//    public void onCallRinging() {
//        Logger.d(getClass().getName(), "CallRinging");
//    }

    /**
     * ?????????????????????????????????????????????
     * @return true: ????????????????????????????????????onRefreshAfterNetworkConnected????????????;
     *         false: ??????-??????????????????
     */
    protected boolean checkRefreshAfterNetworkConnected(int type) {
        return false;
    }

    /**
     * ??????????????????????????????checkRefreshAfterNetworkConnected????????????????????????????????????
     */
    public void onRefreshAfterNetworkConnected(int type) {
        // nothing
    }

    /**
     * ???????????????????????????????????????
     */
    public boolean isNetworkMobile() {
        Activity activity = getActivity();
        return activity != null && ((BaseActivity) activity).isNetworkMobile();
    }

    /**
     * ???????????????????????????
     */
    public boolean hasConnectedNetwork() {
        Activity activity = getActivity();
        return activity != null && ((BaseActivity) activity).hasConnectedNetwork();
    }

    /**
     * ?????????????????????????????????MaskView
     */
    public void checkShowNoConnectedNetworkMaskView() {
        if (!hasConnectedNetwork()) {
            showContentMaskView();
        }
    }

    /**
     * ????????????????????????View????????????????????????
     */
    public void onNetworkReload() {
        Logger.d(getClass().getName(), "Network Reload");
    }

    /**
     * ??????Activity???onCreate????????????
     * @param activity ??????Activity
     * @param savedInstanceState onCreate??????savedInstanceState
     */
    protected void onPreActivityCreate(@NonNull BaseActivity activity,
                                       @Nullable Bundle savedInstanceState) {
        // ??????BaseActivity???onCreate????????????
    }

    /**
     * ??????Toolbar???????????????
     */
    public void showToolbarNavigationIcon() {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).showToolbarNavigationIcon();
        }
    }

    /**
     * ??????Toolbar???????????????
     */
    public void hideToolbarNavigationIcon() {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).hideToolbarNavigationIcon();
        }
    }

    /**
     * ??????Toolbar?????????
     * @param background
     */
    public void setToolbarBackground(Drawable background) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarBackground(background);
        }
    }

    /**
     * ??????????????????ToolbarType
     * @param type
     */
    public void setToolbarType(BaseActivity.ToolbarType type) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarType(type);
        }
    }

    /**
     * ??????Toolbar-BottomLine????????????
     * @param show true:??????BottomLine;false:?????????
     */
    public void setToolbarBottomLineVisibility(boolean show) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarBottomLineVisibility(show);
        }
    }

    /**
     * ??????Toolbar-BottomLine????????????
     * @param show true:??????BottomLine;false:?????????
     */
    public void setToolbarBottomLineVisibility(boolean show, @ColorInt int color) {
        Activity activity = getActivity();
        if (activity != null && activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarBottomLineVisibility(show, color);
        }
    }

    /**
     * ??????Toolbar???????????????????????????
     * @param menuId
     */
    public void setToolbarMenu(@MenuRes int menuId) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarMenu(menuId);
        }
    }

    /**
     * ??????Toolbar???????????????
     * @param title
     */
    public void setToolbarCenterTitle(@StringRes int title) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarCenterTitle(title);
        }
    }

    /**
     * ??????Toolbar???????????????
     * @param title
     */
    public void setToolbarCenterTitle(CharSequence title) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarCenterTitle(title);
        }
    }

    /**
     * ??????Toolbar???????????????
     * @param title
     * @param color ???:0xffcccccc
     * @param size ??????:DIP
     */
    public void setToolbarCenterTitle(CharSequence title, int color, int size) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarCenterTitle(title, color, size);
        }
    }

    /**
     * ??????Toolbar????????????????????????
     * @param view
     */
    public void setToolbarCenterCustomView(View view) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarCenterCustomView(view);
        }
    }

    /**
     * ??????Toolbar?????????????????????
     * @param view
     */
    public void setToolbarLeftCustomView(View view) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarLeftCustomView(view);
        }
    }

    /**
     * ??????Toolbar?????????????????????
     * @param view
     */
    public void setToolbarRightCustomView(View view) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setToolbarRightCustomView(view);
        }
    }

    /**
     * ????????????????????????
     */
    public void setSlideable(boolean slideable) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setSlideable(slideable);
        }
    }

    /**
     * ??????????????????View????????????????????????setContentView???????????????View??????????????????????????????
     */
    public void setContentMaskView(@LayoutRes int id) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setContentMaskView(id);
        }
    }

    /**
     * ??????????????????View????????????????????????setContentView???????????????View??????????????????????????????
     */
    public void setContentMaskView(View view) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setContentMaskView(view);
        }
    }

    /**
     * ????????????????????????View
     */
    public void setContentMaskViewVisibility(boolean show, @IdRes int reloadId) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setContentMaskViewVisibility(show, reloadId);
        }
        // ?????????????????????checkShowNoConnectedNetworkMaskView
        // ??????????????????????????????????????????
        // ????????????????????????true?????????????????????????????????????????????????????????
        mQuestionerResponsable = show;
    }

    /**
     * ??????????????????View
     */
    public void showContentMaskView() {
        setContentMaskViewVisibility(true, 0);
    }

    /**
     * ??????????????????View
     */
    public void showContentMaskView(@IdRes int reloadId) {
        setContentMaskViewVisibility(true, reloadId);
    }

    /**
     * ??????????????????View
     */
    public void hideContentMaskView() {
        setContentMaskViewVisibility(false, 0);
    }

    /**
     * ??????????????????View?????????setContentView?????????View
     */
    public void setContentContextViewVisibility(boolean show) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).setContentContextViewVisibility(show);
        }
    }

    /**
     * ??????ContentMaskView??????????????????
     */
    public boolean isContentMaskViewShowing() {
        Activity activity = getActivity();
        return activity instanceof BaseActivity &&
                ((BaseActivity) activity).isContentMaskViewShowing();
    }

    /**
     * ?????????ActivityFinish????????????????????????????????????
     */
    public void onPreFinish(BaseActivity activity) {
        // nothing
    }

    /**
     * NewIntent?????????BaseActivity????????????
     */
    public void onNewIntent(Intent intent) {
        // nothing
    }

    /**
     * ??????????????????
     * @return true:Activity??????????????????; false:??????Activity??????
     */
    public boolean onBackPressed() {
        return false;
    }

    /**
     * ???????????????Activity
     */
    public void finish() {
        finish(null);
    }

    /**
     * ???????????????Activity
     * @param data setResult??????????????????
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
     * ??????????????????getString
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
     * ??????????????????getString
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
     * ????????????Toast
     */
    public void toast(String text) {
        toast(text, null);
    }

    /**
     * ????????????Toast
     */
    public void toast(String text, Runnable dismissRunnable) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            BaseActivity act = (BaseActivity) activity;
            act.toast(text, dismissRunnable);
        }
    }

    /**
     * ????????????????????????????????????????????????
     * @param contentFragment ??????????????????Fragment
     * @return ????????????null,?????????????????????????????????
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
     * ????????????????????????????????????????????????
     * @param contentFragment ??????????????????Fragment
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
     * ????????????????????????ProgressBar??????????????????
     * ????????????????????????????????????????????????
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
     * ????????????????????????ProgressBar??????????????????
     * ????????????????????????????????????????????????
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
     * ????????????Airbnb????????????????????????
     * ????????????????????????????????????????????????
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
     * ????????????Airbnb????????????????????????
     * ????????????????????????????????????????????????
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
     * ????????????????????????SimpleLoadDialog??????Tip
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
     * ????????????????????????SimpleLoadDialog??????Tip
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
     * ????????????????????????????????????ProgressBar??????????????????
     */
    public void closeSimpleLoadDialog() {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).closeSimpleLoadDialog();
        }
    }

    /**
     * ????????????????????????????????????ProgressBar??????????????????
     */
    public void closeSimpleLoadDialog(long delayMillis) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).closeSimpleLoadDialog(delayMillis);
        }
    }

    /**
     * ????????????????????????ProgressBar?????????View
     * ????????????????????????????????????????????????
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
     * ????????????????????????????????????ProgressBar?????????View
     */
    public void closeSimpleLoadToast() {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).closeSimpleLoadToast();
        }
    }

    /**
     * ?????????????????????
     * @param text ???????????????
     * @param ok ?????????????????????
     * @param okCallback ??????[??????]???????????????
     * @param dismissListener dlg??????????????????
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
     * ?????????????????????
     * @param text ???????????????
     * @param ok ?????????????????????
     * @param cancel ?????????????????????
     * @param okCallback ??????[??????]???????????????
     * @param cancelCallback ??????[??????]???????????????
     * @param dismissListener dlg??????????????????
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
     * ??????App??????????????????
     * @param text ????????????
     */
    public void showToSettingConfirmDialog(CharSequence text) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            BaseActivity act = (BaseActivity) activity;
            act.showToSettingConfirmDialog(text);
        }
    }

    /**
     * ?????????????????????
     */
    public void startFragment(Class<? extends BaseFragment> fragment) {
        startFragment(fragment, null, null);
    }

    /**
     * ?????????????????????
     */
    public void startFragment(Class<? extends BaseFragment> fragment,
                              Bundle args) {
        startFragment(fragment, null, args);
    }

    /**
     * ?????????????????????
     */
    public void startFragment(Class<? extends BaseFragment> fragment,
                              int requestCode) {
        startFragment(fragment, null, null, requestCode);
    }

    /**
     * ?????????????????????
     */
    public void startFragment(Class<? extends BaseFragment> fragment,
                              Bundle args,
                              int requestCode) {
        startFragment(fragment, null, args, requestCode);
    }

    /**
     * ?????????????????????
     */
    public void startFragment(Class<? extends BaseFragment> fragment,
                              Class<? extends BaseActivity> container) {
        startFragment(fragment, container, null);
    }

    /**
     * ?????????????????????
     */
    public void startFragment(Class<? extends BaseFragment> fragment,
                              Class<? extends BaseActivity> container,
                              Bundle args) {
        startFragment(fragment, container, args, 0);
    }

    /**
     * ?????????????????????
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
     * Analysis????????????
     * @param eventId ?????????????????????ID
     */
    public void analysisEvent(String eventId) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            BaseActivity act = (BaseActivity) activity;
            act.analysisEvent(eventId);
        }
    }

    /**
     * Analysis????????????
     * @param eventId ?????????????????????ID
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
