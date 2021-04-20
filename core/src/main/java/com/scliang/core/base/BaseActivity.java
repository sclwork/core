package com.scliang.core.base;

import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.material.navigation.NavigationView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.PagerAdapter;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.scliang.core.R;
import com.scliang.core.analysis.Analysis;
import com.scliang.core.base.dialog.SimpleConfirmDialog;
import com.scliang.core.base.dialog.SimpleLoadDialog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/9/28.
 */
public abstract class BaseActivity
        extends AppCompatActivity
        implements Questioner,
        SlideViewPager.OnPageChangeListener,
        Toolbar.OnMenuItemClickListener {

    /**
     定义了Toolbar的显示位置
     HIDE  : 不显示Toolbar
     TOP   : Toolbar显示在Container上面,处于同一水平高度
     FLOAT : Toolbar悬浮在Container上面,在不同水平高度
     */
    public enum ToolbarType {
        HIDE,  // 不显示Toolbar
        TOP,   // Toolbar显示在Container上面,处于同一水平高度
        FLOAT, // Toolbar悬浮在Container上面,在不同水平高度
    }

    // 操作UI时,尽量通过UIHandler进行
    private Handler mUIHandler;

    // 请求权限时使用
    public static final int REQUEST_CODE_ASK_PERMISSIONS = 89;
    public static final int RequestPermissionTimeout = 1000;
    private static Map<String, Long> RequestPermissionTimeMap = new HashMap<>();

    private String mRequestId;
    private boolean mQuestionerResponsable;

    private ArrayList<View> mSlidePages = new ArrayList<>();
    private SlideViewPager mSlidePager;
    private LinearLayout mPageLeftView;
    private LinearLayout mPageMainView;
    private View mPageMainContent;
    private boolean isContentViewShowed;

    private RootContainer mRootContainer;
    private ContentContainer mContentContainer;
    private ContentContext mContentContext;
    private ContentMask mContentMask;
    private ProgressBar mContentLoadToast;
    private ToolbarContainer mToolbarContainer;
    private BaseFragment  mRootFragment;

    // Toolbar的类型,默认不显示Toolbar
    private ToolbarType mToolbarType = ToolbarType.HIDE;
    // 每个页面中包含一个Toolbar,子类可控制其是否显示并设置其功能按钮等
    protected Toolbar mToolbar;
    private int mToolbarHeight;
    private boolean mToolbarBottomLineVisibility;
    private FrameLayout mToolbarCenterContainer;
    private FrameLayout mToolbarLeftContainer;
    private FrameLayout mToolbarRightContainer;

    // 对话框们
    private PushFragmentContainer mPushFragmentContainer;
    private SimpleLoadDialog mSimpleLoadDialog;
    private final byte[] mLoadSync = new byte[]{0};

    // 软键盘状态 0:关闭 1:打开
    private final static int KeyboardStateClosed = 0;
    private final static int KeyboardStateOpened = 1;
    private int mKeyboardState = KeyboardStateClosed;

    // 标记是否采用全屏
    public static final boolean sUseFullScreen = false;

    // Fix:IllegalStateException: Can not perform this action after onSaveInstanceState
    private Method mFixNoteStateNotSavedMethod;
    private Object mFixFragmentMgr;
    private String[] mFixActivityClassName = {"Activity", "FragmentActivity"};

    // 是否采用DrawerLayout布局
    protected boolean useDrawerLayout() { return false; }
    protected @DrawableRes int giveDrawerHomeActionDrawableId() { return 0; }
    protected DrawerLayout mDrawerLayout;
    protected NavigationView mNavigationView;

    @Override
    public String giveRequestId() {
        if (TextUtils.isEmpty(mRequestId)) {
            mRequestId = getClass().getName() + "-Questioner-" + System.currentTimeMillis();
        }
        return mRequestId;
    }

    @Override
    public boolean responseCallbackable() {
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Fix:IllegalStateException: Can not perform this action after onSaveInstanceState
        invokeFragmentManagerNoteStateNotSaved();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 调用onPreActivityCreate
        if (mRootFragment != null) {
            mRootFragment.onPreActivityCreate(this, savedInstanceState);
        }

        super.onCreate(savedInstanceState);
        mUIHandler = new Handler(Looper.getMainLooper());

        // 判断是否支持右滑退出手势
        if (checkSldeable()) {
            super.setContentView(initSlideViews(R.layout.activity_base));
        } else {
            Window window = getWindow();
            window.setBackgroundDrawable(new ColorDrawable(giveWindowColor()));
            // 判断是否采用DrawerLayout布局
            if (useDrawerLayout()) {
                super.setContentView(R.layout.activity_base_drawer);
            } else {
                super.setContentView(R.layout.activity_base);
            }
        }
        mDrawerLayout = findViewById(R.id.base_drawer_layout);
        mNavigationView = findViewById(R.id.base_navigation_view);
        mRootContainer = findViewById(R.id.root);
        mContentContainer = findViewById(R.id.container);
        mContentContext = findViewById(R.id.container_context);
        mContentMask = findViewById(R.id.container_mask);
        if (mContentMask != null) mContentMask.setVisibility(View.GONE);
        mContentLoadToast = findViewById(R.id.container_load_toast);
        if (mContentLoadToast != null) mContentLoadToast.setVisibility(View.GONE);
        mToolbarContainer = findViewById(R.id.toolbar_container);

        mToolbar = findViewById(R.id.toolbar);
        if (mToolbar != null) {
            mToolbar.setContentInsetsAbsolute(0, 0);
        }
        mToolbarCenterContainer = findViewById(R.id.toolbar_center_container);
        mToolbarLeftContainer = findViewById(R.id.toolbar_left_container);
        mToolbarRightContainer = findViewById(R.id.toolbar_right_container);
        setToolbarBackground(new ColorDrawable(0xffffffff));

        fullWindow();
        if (sUseFullScreen) {
            mToolbarContainer.paddingStatusBar();
        }

        useApplicationMaskView();

        String fragmentName = getIntent().getStringExtra("fragment");
        if (fragmentName == null || fragmentName.isEmpty()) {
            mRootFragment = setupRootFragment(savedInstanceState);
            if (mRootFragment != null) {
                mContentContext.removeAllViews();
                getSupportFragmentManager().beginTransaction()
                        .add(mContentContext.getId(), mRootFragment, "RootFragment")
                        .commit();
            }
        } else {
            Bundle args = getIntent().getBundleExtra("args");
            try {
                Class clz = Class.forName(fragmentName);
                if (clz != null) {
                    Object tmp = clz.newInstance();
                    if (tmp instanceof BaseFragment) {
                        mRootFragment = (BaseFragment) tmp;
                    }
                }
            } catch (Exception ignored) {
            }

            if (mRootFragment != null) {
                mContentContext.removeAllViews();
                mRootFragment.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .add(mContentContext.getId(), mRootFragment, "RootFragment")
                        .commit();
            }
        }

        // 依赖Toolbar高度的操作需要在这个回调里面进行 !!!
        getToolbarHeight(new Runnable() {
            @Override
            public void run() {
                // 默认ToolbarType
                setToolbarType(ToolbarType.TOP);
            }
        });

        // 设置默认的DrawerHomeAction按钮图标
        if (useDrawerLayout()) {
            initDefaultDrawerHomeAction();
        }

        // 启动时获取一次配置信息
        Configuration configuration = getResources().getConfiguration();
        onConfigurationChanged(configuration);
    }

    @Override
    protected final void onResume() {
        super.onResume();
        // 统计分析
        Analysis.getInstance().onResume(this);
        if (mRootFragment == null) {
            Analysis.getInstance().onPageStart(getClass().getName());
        }
        // sub-class
        onResume(null);
    }

    protected void onResume(Bundle args) {
    }

    @Override
    protected final void onPause() {
        super.onPause();
        // 统计分析
        Analysis.getInstance().onPause(this);
        if (mRootFragment == null) {
            Analysis.getInstance().onPageEnd(getClass().getName());
        }
        // sub-class
        onPause(null);
    }

    protected void onPause(Bundle args) {
    }

    @Override
    protected void onDestroy() {
        // 取消所有数据请求
        Data.getInstance().cancel(this);
        // super
        super.onDestroy();
    }

    public void rootFragmentViewCreated(Fragment fragment,
                                        @NonNull View view,
                                        @Nullable Bundle savedInstanceState) {
        if (fragment == null) {
            return;
        }

        boolean has = false;
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for(Fragment frag : fragments) {
                has = frag == fragment;
                if (has) {
                    break;
                }
            }
        }

        if (!has) {
            has = fragment == mRootFragment;
        }

        if (!has) {
            return;
        }

        // 可调用
        onRootFragmentViewCreated(view, savedInstanceState);
    }

    // 如果RootFragment存在，当RootFragmentViewCreated时回调到这里
    protected void onRootFragmentViewCreated(@NonNull View view,
                                             @Nullable Bundle savedInstanceState) {
    }

    @Nullable
    @Override
    public <T extends View> T findViewById(@IdRes int id) {
        if (checkSldeable()) {
            return mPageMainView.findViewById(id);
        } else {
            return super.findViewById(id);
        }
    }

    @Override
    public void finish() {
        if (mRootFragment != null) {
            mRootFragment.onPreFinish(this);
        }
        super.finish();
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        setContentViewCore(getLayoutInflater().inflate(layoutResID, mContentContext, false), null);
    }

    @Override
    public void setContentView(View view) {
        setContentViewCore(view, null);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        setContentViewCore(view, params);
    }

    @Override
    public final void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_ASK_PERMISSIONS) {
            Application application = getApplication();
            if (application instanceof BaseApplication) {
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
//                    final BaseActivity activity = this;
//                    final String permission = permissions[0];
//                    if (Manifest.permission.RECORD_AUDIO.equals(permission) ||
//                            Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission) ||
//                            Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
//                        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
////                            Toast.makeText(this, R.string.no_permission_guide_media, Toast.LENGTH_LONG).show();
////                            toast(getString(R.string.no_permission_guide_media));
//                          showToSettingConfirmDialog(getString(R.string.no_permission_guide_media));
//                        }
//                    }
//                }
                ((BaseApplication) application)
                    .requestPermissionsResult(permissions, grantResults);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 适配横屏
        if (checkSldeable()) {
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);
            SlideViewPager.LayoutParams lp = new SlideViewPager.LayoutParams(
                    dm.widthPixels, ViewGroup.LayoutParams.MATCH_PARENT);
            mPageLeftView.setLayoutParams(lp);
            RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                    dm.widthPixels,
                    RelativeLayout.LayoutParams.MATCH_PARENT
            );
            mPageMainContent.setLayoutParams(rlp);
            // 只支持竖屏可滑动退出
            mSlidePager.setSlideable(newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE);
        }
        // 根据横竖屏状态适配StatusBar背景颜色
        setStatusBarColor(giveStatusBarColor());
        // Padding|DissPadding NavigationBar
        if (sUseFullScreen) {
            paddingNavigationBar(newConfig);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment instanceof BaseFragment) {
                    ((BaseFragment) fragment).onNewIntent(intent);
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 兼容在BaseFragment中调用BaseActivity中的startFragment
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null) {
                    fragment.onActivityResult(requestCode, resultCode, data);
                }
            }
        }
    }

    /**
     * Toolbar右边动作按钮回调
     */
    @Override
    public final boolean onMenuItemClick(MenuItem item) {
        onMenuItemClick(item.getGroupId(), item.getItemId());
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment frag : fragments) {
                if (frag != null && frag instanceof BaseFragment) {
                    ((BaseFragment)frag).onMenuItemClick(item.getGroupId(), item.getItemId());
                }
            }
        }
        return true;
    }

    /**
     * Toolbar右边动作按钮回调
     */
    protected void onMenuItemClick(int groupId, int itemId) {
    }

    @Override
    public void onBackPressed() {
        Fragment fragment = null;
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment frag : fragments) {
                if (frag != null && frag.isVisible()) {
                    fragment = frag;
                }
            }
        }
        try {
            if (fragment != null) {
                if (fragment instanceof BaseFragment) {
                    if (!((BaseFragment) fragment).onBackPressed()) {
                        if (!checkCloseDrawerLayout()) {
                            super.onBackPressed();
                        }
                    }
                } else {
                    if (!checkCloseDrawerLayout()) {
                        super.onBackPressed();
                    }
                }
            } else {
                if (!checkCloseDrawerLayout()) {
                    super.onBackPressed();
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mRootFragment == null) {
            return super.onKeyDown(keyCode, event);
        } else {
            if (mRootFragment.keyDown(keyCode, event)) {
                return true;
            } else {
                return super.onKeyDown(keyCode, event);
            }
        }
    }

    final void keyboardOpened() {
        onKeyboardOpened();
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment instanceof BaseFragment) {
                    ((BaseFragment) fragment).keyboardOpened();
                }
            }
        }
        mKeyboardState = KeyboardStateOpened;
        Logger.d(getClass().getSimpleName(), "Keyboard Opened ...");
    }

    final void keyboardClosed() {
        onKeyboardClosed();
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment instanceof BaseFragment) {
                    ((BaseFragment) fragment).keyboardClosed();
                }
            }
        }
        mKeyboardState = KeyboardStateClosed;
        Logger.d(getClass().getSimpleName(), "Keyboard Closed ...");
    }

    final void keyboardOpenedB() {
        onKeyboardOpenedB();
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment instanceof BaseFragment) {
                    ((BaseFragment) fragment).keyboardOpenedB();
                }
            }
        }
    }

    final void keyboardClosedB() {
        onKeyboardClosedB();
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null && fragment instanceof BaseFragment) {
                    ((BaseFragment) fragment).keyboardClosedB();
                }
            }
        }
    }

    // 获得RootContainer
    protected RootContainer getRootContainer() {
        return mRootContainer;
    }

    // 在Resize设置下调用了AndroidBug5497Workaround方法后，键盘打开后
    protected void onKeyboardOpened() {
    }

    // 在Resize设置下调用了AndroidBug5497Workaround方法后，键盘关闭后
    protected void onKeyboardClosed() {
        // 如果mShowPushDialogRunnable可执行
        if (mShowPushDialogRunnable != null) {
            mShowPushDialogRunnable.run();
            mShowPushDialogRunnable = null;
        }
    }

    // 在Resize设置下调用了AndroidBug5497Workaround方法后，键盘打开后
    protected void onKeyboardOpenedB() {
    }

    // 在Resize设置下调用了AndroidBug5497Workaround方法后，键盘关闭后
    protected void onKeyboardClosedB() {
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

    /**
     * 根据手机的分辨率dp单位转为px像素
     * @param dp dp
     * @return px
     */
    public int dp2px(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

  /**
   * 打开App系统设置页面
   * @param text 提示文案
   */
  public void showToSettingConfirmDialog(CharSequence text) {
    showConfirmDialog(text,
        getString(R.string.no_permission_to_setting_ok),
        getString(R.string.no_permission_to_setting_cancel),
        new Runnable() {
          @Override
          public void run() {
            new PermissionPageUtils(getApplicationContext()).jumpPermissionPage();
          }
        }, new Runnable() {
          @Override
          public void run() {
          }
        }, new DialogInterface.OnDismissListener() {
          @Override
          public void onDismiss(DialogInterface dialog) {
          }
        });
  }

    /**
     * 点击Toolbar返回按钮的操作
     */
    protected void onToolbarBackPressed() {
        onBackPressed();
    }

    /**
     * 点击ToolbarDrawerHome按钮的操作
     */
    protected void onToolbarDrawerHomePressed() {
        if (mDrawerLayout == null) {
            return;
        }

        if (mNavigationView == null) {
            return;
        }

        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
        } else {
            mDrawerLayout.openDrawer(mNavigationView);
        }
    }

    /**
     * 尝试关闭DrawerLayout
     */
    public boolean checkCloseDrawerLayout() {
        if (mDrawerLayout == null) {
            return false;
        }

        if (mNavigationView == null) {
            return false;
        }

        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
            return true;
        }

        return false;
    }

    /**
     * 设置是否右滑支持
     */
    public void setSlideable(boolean slideable) {
        if (mSlidePager != null) {
            mSlidePager.setSlideable(slideable);
        }
    }

    // 添加默认的返回按钮图标
    private void initBackAction() {
        mToolbar.setNavigationIcon(R.drawable.ic_fanhui);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftInput();
                onToolbarBackPressed();
            }
        });
    }

    // 设置默认的DrawerHomeAction按钮图标
    private void initDefaultDrawerHomeAction() {
        int id = giveDrawerHomeActionDrawableId();
        mToolbar.setNavigationIcon(id == 0 ? R.drawable.ic_drawer_home : id);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftInput();
                onToolbarDrawerHomePressed();
            }
        });
    }

    /**
     * 使用BaseApplication中的MaskView
     */
    protected void useApplicationMaskView() {
        setContentMaskView(BaseApplication.getApp().generateContentMaskView());
    }

    /**
     * 设置内容蒙版View，用于覆盖在通过setContentView方法设置的View之上，并拦截点击事件
     */
    public void setContentMaskView(@LayoutRes int id) {
        try { setContentMaskView(getLayoutInflater().inflate(id, mContentMask, false));
        } catch (Exception ignored) {}
    }

    /**
     * 设置内容蒙版View，用于覆盖在通过setContentView方法设置的View之上，并拦截点击事件
     */
    public void setContentMaskView(View view) {
        mContentMask.removeAllViews();
        if (view != null) mContentMask.addView(view);
    }

    /**
     * 是否显示内容蒙版View
     */
    public void setContentMaskViewVisibility(boolean show, @IdRes int reloadId) {
        mContentMask.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            TextView reload = mContentMask.findViewById(reloadId);
            if (reload == null) {
                reload = mContentMask.findViewById(BaseApplication.getApp()
                        .onGenerateContentMaskReloadActionId());
            }
            if (reload != null) reload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    networkReload();
                }
            });
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
        mContentContext.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * 判断ContentMaskView是否正在显示
     */
    public boolean isContentMaskViewShowing() {
        return mContentMask.getVisibility() == View.VISIBLE;
    }

    /**
     * 显示Toolbar的导航按钮
     */
    public void showToolbarNavigationIcon() {
        initBackAction();
    }

    /**
     * 隐藏Toolbar的导航按钮
     */
    public void hideToolbarNavigationIcon() {
        mToolbar.setNavigationIcon(null);
    }

    /**
     * 返回窗口的Toolbar
     */
    public Toolbar getToolbar() {
        return mToolbar;
    }

    /**
     * 设置Toolbar-BottomLine是否显示
     * @param show true:显示BottomLine;false:不显示
     */
    public void setToolbarBottomLineVisibility(boolean show) {
        setToolbarBottomLineVisibility(show, -1);
    }

    /**
     * 设置Toolbar-BottomLine是否显示
     * @param show true:显示BottomLine;false:不显示
     */
    public void setToolbarBottomLineVisibility(boolean show, @ColorInt int color) {
        mToolbarBottomLineVisibility = show;
        // 只有ToolBar类型为TOP|FLOAT的时候才可以显示
        if (mToolbarType == ToolbarType.TOP || mToolbarType == ToolbarType.FLOAT) {
            View bottomLine = findViewById(R.id.toolbar_bottom_line);
            if (bottomLine != null) {
                if (color >= 0) bottomLine.setBackgroundColor(color);
                bottomLine.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        } else {
            View bottomLine = findViewById(R.id.toolbar_bottom_line);
            if (bottomLine != null) {
                bottomLine.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 设置为给定的ToolbarType
     */
    public void setToolbarType(ToolbarType type) {
        mToolbarType = type;
        FrameLayout.LayoutParams containerLp =
                (FrameLayout.LayoutParams) mContentContainer.getLayoutParams();
        if (containerLp == null) {
            containerLp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
        }
        switch (mToolbarType) {
            default:
            case HIDE:  // 不显示Toolbar
                mToolbarContainer.setVisibility(View.INVISIBLE);
                setToolbarBottomLineVisibility(false);
                containerLp.topMargin = 0;
                break;
            case TOP:   // Toolbar显示在Container上面,处于同一水平高度
                mToolbarContainer.setVisibility(View.VISIBLE);
                setToolbarBottomLineVisibility(mToolbarBottomLineVisibility);
                containerLp.topMargin = mToolbarHeight;
                break;
            case FLOAT: // Toolbar悬浮在Container上面,在不同水平高度
                mToolbarContainer.setVisibility(View.VISIBLE);
                setToolbarBottomLineVisibility(mToolbarBottomLineVisibility);
                containerLp.topMargin = 0;
                break;
        }
        mContentContainer.setLayoutParams(containerLp);
    }

    /**
     * 获得窗口Toolbar的显示类型
     * @return
     */
    public ToolbarType getToolbarType() {
        return mToolbarType;
    }

    /**
     * 设置Toolbar的背景
     * @param background
     */
    public void setToolbarBackground(Drawable background) {
        mToolbarContainer.setBackground(background);
    }

    /**
     * 设置ToolbarPaddingTop
     */
    public void setToolbarPaddingStatusBar() {
        mToolbarContainer.paddingStatusBar();
    }

    /**
     * 设置ContentPaddingTop
     */
    public void setContentPaddingStatusBar() {
        mContentContainer.paddingStatusBar();
    }

    /**
     * 设置Toolbar的右边操作动作按钮
     * @param menuId
     */
    public void setToolbarMenu(@MenuRes int menuId) {
        mToolbar.getMenu().clear();
        mToolbar.inflateMenu(menuId);
        mToolbar.setOnMenuItemClickListener(this);
    }

    /**
     * 设置Toolbar居中的标题
     * @param title
     */
    public void setToolbarCenterTitle(@StringRes int title) {
        setToolbarCenterTitle(getString(title), 0xff333333, 17);
    }

    /**
     * 设置Toolbar居中的标题
     * @param title
     */
    public void setToolbarCenterTitle(CharSequence title) {
        setToolbarCenterTitle(title, 0xff333333, 17);
    }

    /**
     * 设置Toolbar居中的标题
     * @param title
     * @param color 如:0xffcccccc
     * @param size 单位:DIP
     */
    public void setToolbarCenterTitle(CharSequence title, int color, int size) {
        mToolbarCenterContainer.removeAllViews();
        TextView tv = new TextView(this);
        tv.setLines(1);
        tv.setMaxLines(1);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size);
        tv.setText(title);
        tv.setGravity(Gravity.CENTER);
        tv.getPaint().setFakeBoldText(true);
        mToolbarCenterContainer.addView(tv);
    }

    /**
     * 设置Toolbar居中的标题
     * @param title
     */
    public void setToolbarCenterTitle(CharSequence title, boolean autoSize) {
        setToolbarCenterTitle(title, 0xff333333, autoSize);
    }

    /**
     * 设置Toolbar居中的标题
     * @param title
     * @param color 如:0xffcccccc
     * @param autoSize [10sp,17sp]
     */
    public void setToolbarCenterTitle(CharSequence title, int color, boolean autoSize) {
        if (autoSize) {
            mToolbarCenterContainer.removeAllViews();
            TextView tv = (TextView) LayoutInflater.from(this).inflate(R.layout.view_toolbar_title,
              mToolbarCenterContainer, false);
            tv.setTextColor(color);
            tv.setText(title);
            mToolbarCenterContainer.addView(tv);
        } else {
            setToolbarCenterTitle(title, color, 17);
        }
    }

    /**
     * 设置Toolbar居中的自定义视图
     * @param view
     */
    public void setToolbarCenterCustomView(View view) {
        mToolbarCenterContainer.removeAllViews();
        if (view != null && view.getParent() == null) {
            mToolbarCenterContainer.addView(view);
        }
    }

    /**
     * 设置Toolbar左边的自定义视图
     * @param view
     */
    public void setToolbarLeftCustomView(View view) {
        mToolbarLeftContainer.removeAllViews();
        if (view != null && view.getParent() == null) {
            mToolbarLeftContainer.addView(view);
        }
    }

    /**
     * 设置Toolbar右边的自定义视图
     * @param view
     */
    public void setToolbarRightCustomView(View view) {
        mToolbarRightContainer.removeAllViews();
        if (view != null && view.getParent() == null) {
            mToolbarRightContainer.addView(view);
        }
    }

    // 尝试获得Toolbar的高度,并记录在mToolbarHeight中
    private void getToolbarHeight(final Runnable callback) {
        mToolbarContainer.measure(mToolbarContainer.getMeasuredWidthAndState(),
                mToolbarContainer.getMeasuredHeightAndState());
        mToolbarHeight = mToolbarContainer.getMeasuredHeight();
        if (callback != null) {
            callback.run();
        }
    }

    // =========================================================
    private class ContentPagerAdapter extends PagerAdapter {

        @Override
        public int getCount() { return mSlidePages.size(); }

        @Override
        public CharSequence getPageTitle(int position) { return ""; }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View page = mSlidePages.get(position);
            container.addView(page, 0);
            return page;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View page = mSlidePages.get(position);
            container.removeView(page);
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) { return arg0 == arg1; }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset,
                               int positionOffsetPixels) {
        if (position == 0 && positionOffsetPixels > 0) {
            hideSoftInput();
        }
    }

    @Override
    public void onPageSelected(int position) {}

    @Override
    public void onPageScrollStateChanged(int state) {
        if(state == 0 && mSlidePager.getCurrentItem() == 0) {
            mSlidePager.setOnPageChangeListener(null);
            onBackPressed();
            overridePendingTransition(0, 0);
        }
        else if(state == 0 && mSlidePager.getCurrentItem() == 1) {
            if(!isContentViewShowed) {
                isContentViewShowed = true;
                onContentViewShowed();
            }
        }
    }

    /**
     * 每当Content完整显示后,回调到这里
     */
    protected void onContentViewShowed() {}

    /**
     * 配置是否支持右滑退出手势
     * @return
     */
    protected boolean checkSldeable() {
        Drawable background = getWindow().getDecorView().getBackground();
        return background != null && background instanceof ColorDrawable &&
                ((ColorDrawable) background).getColor() == 0;
    }

    // 隐藏软键盘
    private void hideSoftInput() {
        if (mRootContainer != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mRootContainer.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    // 初始化SlideViews
    protected View initSlideViews(@LayoutRes int resId) {
        mPageMainContent = LayoutInflater.from(this).inflate(resId, null);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mSlidePager = new SlideViewPager(this);
        mSlidePager.setLayoutParams(lp);
        mSlidePager.setScrollDuration(6);
        mSlidePager.setOnPageChangeListener(this);

        ContentPagerAdapter mSlidePagerAdapter = new ContentPagerAdapter();

        lp = new ViewGroup.LayoutParams(dm.widthPixels, ViewGroup.LayoutParams.MATCH_PARENT);
        mPageLeftView = new LinearLayout(this);
        mPageLeftView.setLayoutParams(lp);
        mPageLeftView.setOrientation(LinearLayout.HORIZONTAL);

        lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        FrameLayout pagerMaskView = new FrameLayout(this);
        pagerMaskView.setLayoutParams(lp);
        pagerMaskView.setBackgroundResource(R.drawable.pager_mask);
        mPageLeftView.addView(pagerMaskView);

        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                dm.widthPixels,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        mPageMainContent.setLayoutParams(rlp);

        lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        RelativeLayout contentRootView = new RelativeLayout(this);
        contentRootView.setLayoutParams(lp);
        contentRootView.addView(mPageMainContent);

        lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        mPageMainView = new LinearLayout(this);
        mPageMainView.setLayoutParams(lp);
        mPageMainView.setOrientation(LinearLayout.VERTICAL);
        mPageMainView.addView(contentRootView);

        mSlidePages.add(mPageLeftView);
        mSlidePages.add(mPageMainView);

        mSlidePager.setAdapter(mSlidePagerAdapter);
        mSlidePager.post(new Runnable() {public void run() {
            mSlidePager.setCurrentItem(1, false);
        }});
        return mSlidePager;
    }

    // 请求权限得到返回后回调到这里
    final void requestPermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
            // 通知PushFragmentContainer
            if (mPushFragmentContainer != null) {
                mPushFragmentContainer.onRequestPermissionsResult(permissions[i],
                        grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
            // 通知子类
            onRequestPermissionsResult(permissions[i],
                    grantResults[i] == PackageManager.PERMISSION_GRANTED);
            // 检查是否包含Fragment,并逐个通知Fragment
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            if (fragments != null && fragments.size() > 0) {
                for (Fragment frag : fragments) {
                    if (frag != null && frag instanceof BaseFragment) {
                        ((BaseFragment) frag).requestPermissionsResult(
                                permissions[i], grantResults[i]);
                    }
                }
            }
        }
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
     * 请求权限
     * @param permission 申请的权限名称
     */
    public final void requestPermission(final String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !TextUtils.isEmpty(permission)) {
            Long time = RequestPermissionTimeMap.get(permission);
            long requestTime = time == null ? 0 : time;
            boolean canRequest = System.currentTimeMillis() - requestTime > RequestPermissionTimeout;
            if (canRequest) {
                ActivityCompat.requestPermissions(this, new String[]{permission},
                    REQUEST_CODE_ASK_PERMISSIONS);
                RequestPermissionTimeMap.put(permission, System.currentTimeMillis());
            }
        }
    }

    /**
     * 全屏
     */
    protected void fullWindow() {
        Window window = getWindow();
        if (sUseFullScreen) {
            window.clearFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS |
                            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
            );
        } else {
            window.clearFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (hasNavigationBar()) {
                if (sUseFullScreen) {
                    window.getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                } else {
                    window.getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            } else {
                if (sUseFullScreen) {
                    window.getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                } else {
                    window.getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                }
            }
        } else {
            if (sUseFullScreen) {
                if (hasNavigationBar()) {
                    window.getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                } else {
                    window.getDecorView().setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(giveStatusBarColor());
            window.setNavigationBarColor(giveNavigationBarColor());
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
    }

    /**
     * 定制Window的背景颜色
     */
    protected int giveWindowColor() {
        return 0xfff7f8f9;
    }

    /**
     * 定制StatusBar的颜色
     */
    protected int giveStatusBarColor() {
        int appGiveColor = 0x00000000;
        Application app = getApplication();
        if (app != null && app instanceof BaseApplication) {
            appGiveColor = ((BaseApplication)app).giveStatusBarColor();
        }
        String manufacturer = Build.MANUFACTURER;
        int version = Build.VERSION.SDK_INT;
        if (("xiaomi".equalsIgnoreCase(manufacturer) && version < Build.VERSION_CODES.M) ||
            ("meizu".equalsIgnoreCase(manufacturer) && version < Build.VERSION_CODES.M) || // 临时适配全部Meizu
            ("oppo".equalsIgnoreCase(manufacturer) && version < Build.VERSION_CODES.M) || // 临时适配全部vivo
            ("vivo".equalsIgnoreCase(manufacturer) && version < Build.VERSION_CODES.M)) { // 临时适配全部vivo
            Configuration configuration = getResources().getConfiguration();
            boolean isPortrait = configuration.orientation != Configuration.ORIENTATION_LANDSCAPE;
            return isPortrait ? 0xff666666 : appGiveColor;
        } else {
            return appGiveColor;
        }
    }

    /**
     * 定制NavigationBar的颜色
     */
    protected int giveNavigationBarColor() {
        return 0xff111111;
    }

    /**
     * 设置StatusBar的颜色
     * @param color x0xxxxxxxx
     */
    protected void setStatusBarColor(int color) {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(color);
        }
    }

    /**
     * 获得StatusBar的高度
     */
    protected int getStatusBarHeight() {
        int result = 0;
        Resources resources = getResources();
        int resourceId = resources.getIdentifier(
                "status_bar_height", "dimen", "android"
        );

        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }

        return result;
    }

    /**
     * 设置NavigationBar的颜色
     * @param color x0xxxxxxxx
     */
    protected void setNavigationBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setNavigationBarColor(color);
        }
    }

    /**
     * 判断手机是否有NavigationBar
     */
    protected boolean hasNavigationBar() {
        boolean hasNavigationBar = false;

        Resources resources = getResources();
        int id = resources.getIdentifier("config_showNavigationBar", "bool", "android");
        if (id > 0) {
            hasNavigationBar = resources.getBoolean(id);
        }

        try {
            Class systemPropertiesClass = Class.forName("android.os.SystemProperties");
            Method m = systemPropertiesClass.getMethod("get", String.class);
            String navBarOverride = (String) m.invoke(systemPropertiesClass, "qemu.hw.mainkeys");
            if ("1".equals(navBarOverride)) {
                hasNavigationBar = false;
            } else if ("0".equals(navBarOverride)) {
                hasNavigationBar = true;
            }
        } catch (Exception ignored) {
        }

        return hasNavigationBar;
    }

    /**
     * 判断NavigationBar是否在手机屏幕的下方，
     * 在手机横屏时，有些NavigationBar是在右边
     */
    protected boolean isNavigationBarBottom() {
        DisplayMetrics realSize = new DisplayMetrics();
        DisplayMetrics windowSize = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        display.getRealMetrics(realSize);
        display.getMetrics(windowSize);
        return windowSize.heightPixels != realSize.heightPixels;
    }

    /**
     * 根据屏幕方向得到NavigationBar的高度，有些横屏时会比竖屏时高度较小
     * @param orientation 屏幕方向
     */
    protected int getNavigationBarHeight(int orientation) {
        int result = 0;
        Resources resources = getResources();
        int resourceId = resources.getIdentifier(
            (orientation == Configuration.ORIENTATION_PORTRAIT) ?
            "navigation_bar_height" : "navigation_bar_height_landscape",
                "dimen", "android"
        );

        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId);
        }

        return result;
    }

    /*
     * 全屏时，设置BaseContainer的padding，避免NavigationBar盖住内容
     */
    private void paddingNavigationBar(Configuration newConfig) {
        if (mRootContainer != null && hasNavigationBar()) {
//            boolean isAbove27 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1;
            int left = 0;
            String configStr = newConfig.toString().replaceAll("-", ",");
            if (configStr.contains("appBounds=Rect(")) {
                int start = configStr.indexOf("appBounds=Rect(") + "appBounds=Rect(".length();
                int end = configStr.indexOf(")", start);
                String[] appBounds = configStr.substring(start, end).replaceAll(" ", "").split(",");
                left = Integer.valueOf(appBounds[0]);
            }
            if (isNavigationBarBottom()) {
                mRootContainer.setPadding(
                        0,
                        0,
                        0,
                        getNavigationBarHeight(Configuration.ORIENTATION_PORTRAIT)
                );
            } else {
                if (left == 0) {
//                    if (isAbove27) {
                        mRootContainer.setPadding(
                                0,
                                0,
                                0,
                                0
                        );
//                    } else {
//                        mRootContainer.setPadding(
//                                0,
//                                0,
//                                getNavigationBarHeight(Configuration.ORIENTATION_LANDSCAPE),
//                                0
//                        );
//                    }
                } else {
                    mRootContainer.setPadding(
                            getNavigationBarHeight(Configuration.ORIENTATION_LANDSCAPE),
                            0,
                            0,
                            0
                    );
                }
            }
        }
    }

    /**
     * 设置RootFragment
     * @param savedInstanceState Bundle
     * @return BaseFragment
     */
    protected BaseFragment setupRootFragment(Bundle savedInstanceState) {
        return null;
    }

    // 设置ContentView操作 - 内部操作
    private void setContentViewCore(View child, ViewGroup.LayoutParams params) {
        if (child == null) return;
        if (mRootFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(mRootFragment)
                    .commit();
            mRootFragment = null;
        }
        mContentContext.removeAllViews();
        if (params == null) {
            mContentContext.addView(child);
        } else {
            mContentContext.addView(child, params);
        }
    }

    // Fix:IllegalStateException: Can not perform this action after onSaveInstanceState
    private void invokeFragmentManagerNoteStateNotSaved() {
        //java.lang.IllegalStateException: Can not perform this action after onSaveInstanceState
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            return;
        }
        try {
            if (mFixNoteStateNotSavedMethod != null && mFixFragmentMgr != null) {
                mFixNoteStateNotSavedMethod.invoke(mFixFragmentMgr);
                return;
            }
            Class cls = getClass();
            do {
                cls = cls.getSuperclass();
            } while (!(mFixActivityClassName[0].equals(cls.getSimpleName())
                    || mFixActivityClassName[1].equals(cls.getSimpleName())));

            Field fragmentMgrField = prepareField(cls, "mFragments");
            if (fragmentMgrField != null) {
                mFixFragmentMgr = fragmentMgrField.get(this);
                mFixNoteStateNotSavedMethod = getDeclaredMethod(mFixFragmentMgr, "noteStateNotSaved");
                if (mFixNoteStateNotSavedMethod != null) {
                    mFixNoteStateNotSavedMethod.invoke(mFixFragmentMgr);
                }
            }
        } catch (Exception ignored) { }
    }
    // Fix:IllegalStateException: Can not perform this action after onSaveInstanceState
    private Field prepareField(Class<?> c, String fieldName) throws NoSuchFieldException {
        while (c != null) {
            try {
                Field f = c.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            } finally {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException();
    }
    // Fix:IllegalStateException: Can not perform this action after onSaveInstanceState
    private Method getDeclaredMethod(Object object, String methodName, Class<?>... parameterTypes) {
        Method method = null;
        for (Class<?> clazz = object.getClass(); clazz != Object.class; clazz = clazz.getSuperclass()) {
            try {
                method = clazz.getDeclaredMethod(methodName, parameterTypes);
                return method;
            } catch (Exception ignored) { }
        }
        return null;
    }

    // 处理本地广播接收
    final void receiveLocalBroadcastReceiver(String action, Bundle args) {
        onReceiveLocalBroadcastReceiver(action, args);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment frag : fragments) {
                if (frag != null && frag instanceof BaseFragment) {
                    ((BaseFragment) frag).onReceiveLocalBroadcastReceiver(action, args);
                }
            }
        }
    }

    // 处理网络连接,广播分发
    final void onNetworkConnectedCore(int type) {
        onNetworkConnected(type);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment frag : fragments) {
                if (frag != null && frag instanceof BaseFragment) {
                    ((BaseFragment) frag).onNetworkConnected(type);
                }
            }
        }
    }

    // 处理网络断开,广播分发
    final void onNetworkDisconnectedCore() {
        onNetworkDisconnected();
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment frag : fragments) {
                if (frag != null && frag instanceof BaseFragment) {
                    ((BaseFragment) frag).onNetworkDisconnected();
                }
            }
        }
    }

    // 点击网络错误提示View中重新加载按钮时
    final void networkReload() {
        onNetworkReload();
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment frag : fragments) {
                if (frag != null && frag instanceof BaseFragment) {
                    ((BaseFragment) frag).onNetworkReload();
                }
            }
        }
    }

//    // 分发来电回调
//    final void onCallRingingCore() {
//        onCallRinging();
//        List<Fragment> fragments = getSupportFragmentManager().getFragments();
//        if (fragments != null) {
//            for (Fragment frag : fragments) {
//                if (frag != null && frag instanceof BaseFragment) {
//                    ((BaseFragment) frag).onCallRinging();
//                }
//            }
//        }
//    }

    /**
     * 发布一个本地广播
     * @param action Action
     * @param args Args
     */
    public void sendLocalBroadcast(String action, Bundle args) {
        BaseApplication.getApp().sendLocalBroadcast(action, args);
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
        ConnectivityManager cm =(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected() &&
                    info.getType() == ConnectivityManager.TYPE_MOBILE;
        } else {
            return false;
        }
    }

    /**
     * 判断是否有可用网络
     */
    public boolean hasConnectedNetwork() {
        ConnectivityManager cm =(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } else {
            return false;
        }
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
     * 显示一个Toast
     */
    public void toast(String text) {
        toast(text, null);
    }

    /**
     * 显示一个Toast
     */
    public void toast(String text, final Runnable dismissRunnable) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        final Toast toast = Toast.makeText(this, Html.fromHtml(text), Toast.LENGTH_SHORT);
        final View view = getLayoutInflater().inflate(R.layout.view_toast, getRootContainer(), false);
        final TextView textView = view.findViewById(R.id.text);
        if (textView != null) { textView.setText(Html.fromHtml(text)); }
        toast.setView(view);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
        if (dismissRunnable != null) {
            mUIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    dismissRunnable.run();
                }
            }, 300);
        }
//        Bundle args = new Bundle();
//        args.putCharSequence("Text", Html.fromHtml(text));
//        SimpleToastDialog dialog = new SimpleToastDialog();
//        dialog.setArguments(args);
//        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
//            @Override
//            public void onDismiss(DialogInterface dialog) {
//                if (dismissRunnable != null) dismissRunnable.run();
//            }
//        });
//        dialog.show(getSupportFragmentManager(), "ToastDialog");
    }

    private Runnable mShowPushDialogRunnable;

    /**
     * 显示一个从底部动画推上来的对话框
     * @param contentFragment 要显示的内容Fragment
     * @return
     */
    public PushFragmentContainer showPushDialog(final BaseFragment contentFragment) {
        return showPushDialog(contentFragment, null);
    }

    /**
     * 显示一个从底部动画推上来的对话框
     * @param contentFragment 要显示的内容Fragment
     * @return
     */
    public PushFragmentContainer showPushDialog(final BaseFragment contentFragment, final DialogInterface.OnDismissListener onDismissListener) {
        // 已存在
        if (mPushFragmentContainer != null) {
            return mPushFragmentContainer;
        }
        // 创建新的PushDialog
        final PushFragmentContainer dialog = new PushFragmentContainer();
        if (sUseFullScreen) {
            mShowPushDialogRunnable = null;
            showPushDialogCore(contentFragment, dialog, getClass().getName());
        } else {
            if (mKeyboardState == KeyboardStateOpened) {
                // 如果键盘是打开状态，需要等键盘关闭后再执行showPushDialog操作
                // 会在onKeyboardClosed里执行showPushDialog操作
                mShowPushDialogRunnable = new Runnable() {
                    @Override
                    public void run() {
                        showPushDialogCore(contentFragment, dialog, getClass().getName());
                    }
                };
                // 关闭键盘
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                }
            } else {
                mShowPushDialogRunnable = null;
                // 如果键盘是关闭状态，立刻执行shoPushDialog操作
                showPushDialogCore(contentFragment, dialog, getClass().getName());
            }
        }
        mPushFragmentContainer = dialog;
        mPushFragmentContainer.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mPushFragmentContainer = null;
                if (onDismissListener != null) {
                    onDismissListener.onDismiss(dialog);
                }
            }
        });
        return dialog;
    }

    private void showPushDialogCore(final BaseFragment contentFragment,
                                    final PushFragmentContainer dialog,
                                    final String name) {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                dialog.show(getSupportFragmentManager(), name, contentFragment);
            }
        });
    }

    /**
     * 显示一个只有圆形ProgressBar的提示对话框
     * 只负责显示出来，关闭需要自行控制
     */
    public void showSimpleLoadDialog_Fullscreen() {
        synchronized (mLoadSync) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mLoadSync) {
                        if (mSimpleLoadDialog == null) {
                            mSimpleLoadDialog = new SimpleLoadDialog();
                            mSimpleLoadDialog.showFullscreen(getSupportFragmentManager(), getClass().getName());
                        }
                    }
                }
            });
        }
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
                            mSimpleLoadDialog.show(getSupportFragmentManager(), getClass().getName());
                        }
                    }
                }
            });
        }
    }

    /**
     * 显示一个Airbnb动画的提示对话框
     * 只负责显示出来，关闭需要自行控制
     */
    public void showSimpleLoadDialog_Fullscreen(final String assetsJsonFileName) {
        synchronized (mLoadSync) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mLoadSync) {
                        if (mSimpleLoadDialog == null) {
                            mSimpleLoadDialog = new SimpleLoadDialog();
                            if (!TextUtils.isEmpty(assetsJsonFileName)) {
                                Bundle args = new Bundle();
                                args.putString("AssetsJson", assetsJsonFileName);
                                mSimpleLoadDialog.setArguments(args);
                            }
                            mSimpleLoadDialog.showFullscreen(getSupportFragmentManager(), getClass().getName());
                        }
                    }
                }
            });
        }
    }

    /**
     * 显示一个Airbnb动画的提示对话框
     * 只负责显示出来，关闭需要自行控制
     */
    public void showSimpleLoadDialog(final String assetsJsonFileName) {
        synchronized (mLoadSync) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mLoadSync) {
                        if (mSimpleLoadDialog == null) {
                            mSimpleLoadDialog = new SimpleLoadDialog();
                            if (!TextUtils.isEmpty(assetsJsonFileName)) {
                                Bundle args = new Bundle();
                                args.putString("AssetsJson", assetsJsonFileName);
                                mSimpleLoadDialog.setArguments(args);
                            }
                            mSimpleLoadDialog.show(getSupportFragmentManager(), getClass().getName());
                        }
                    }
                }
            });
        }
    }

    /**
     * 更新正在显示着的SimpleLoadDialog中的Tip
     */
    public void updateSimpleLoadDialogTip(final String tip) {
        mUpdateSimpleLoadDialogTipCount = 200;
        updateSimpleLoadDialogTipCore(tip);
    }

    /**
     * 更新正在显示着的SimpleLoadDialog中的Tip
     */
    private void updateSimpleLoadDialogTipCore(final String tip) {
        synchronized (mLoadSync) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mLoadSync) {
                        if (mSimpleLoadDialog != null) {
                            boolean res = mSimpleLoadDialog.updateTip(tip);
                            if (!res && mUpdateSimpleLoadDialogTipCount > 0) {
                                mUpdateSimpleLoadDialogTipCount--;
                                mUIHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateSimpleLoadDialogTipCore(tip);
                                    }
                                }, 5);
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * 更新正在显示着的SimpleLoadDialog中的Tip
     */
    public void updateSimpleLoadDialogTip(final String tip,
                                          final int tipColor,
                                          final int tipSize) {
        mUpdateSimpleLoadDialogTipCount = 200;
        updateSimpleLoadDialogTipCore(tip, tipColor, tipSize);
    }

    /**
     * 更新正在显示着的SimpleLoadDialog中的Tip
     */
    private void updateSimpleLoadDialogTipCore(final String tip,
                                               final int tipColor,
                                               final int tipSize) {
        synchronized (mLoadSync) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mLoadSync) {
                        if (mSimpleLoadDialog != null) {
                            boolean res = mSimpleLoadDialog.updateTip(tip, tipColor, tipSize);
                            if (!res && mUpdateSimpleLoadDialogTipCount > 0) {
                                mUpdateSimpleLoadDialogTipCount--;
                                mUIHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateSimpleLoadDialogTipCore(tip, tipColor, tipSize);
                                    }
                                }, 5);
                            }
                        }
                    }
                }
            });
        }
    }

    private int mUpdateSimpleLoadDialogTipCount = 0;

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
     * 关闭正在显示着的只有圆形ProgressBar的提示对话框
     */
    public void closeSimpleLoadDialog(long delayMillis) {
        synchronized (mLoadSync) {
            mUIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    synchronized (mLoadSync) {
                        if (mSimpleLoadDialog != null) {
                            mSimpleLoadDialog.dismissAllowingStateLoss();
                            mSimpleLoadDialog = null;
                        }
                    }
                }
            }, delayMillis);
        }
    }

    /**
     * 显示一个只有圆形ProgressBar的提示View
     * 只负责显示出来，关闭需要自行控制
     */
    public void showSimpleLoadToast() {
        synchronized (mLoadSync) {
            mUIHandler.post(new Runnable() {
                @Override
                public void run() {
                    synchronized (mLoadSync) {
                        if (mContentLoadToast != null) {
                            mContentLoadToast.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }
    }

    /**
     * 关闭正在显示着的只有圆形ProgressBar的提示View
     */
    public void closeSimpleLoadToast() {
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mLoadSync) {
                    if (mContentLoadToast != null) {
                        mContentLoadToast.setVisibility(View.GONE);
                    }
                }
            }
        });
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
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                SimpleConfirmDialog dialog = new SimpleConfirmDialog();
                dialog.onlyOKAction(true);
                dialog.setOnDismissListener(dismissListener);
                dialog.show(getSupportFragmentManager(), getClass().getName(),
                        text, ok, "", okCallback, null);
            }
        });
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
        mUIHandler.post(new Runnable() {
            @Override
            public void run() {
                SimpleConfirmDialog dialog = new SimpleConfirmDialog();
                dialog.setOnDismissListener(dismissListener);
                dialog.show(getSupportFragmentManager(), getClass().getName(),
                        text, ok, cancel, okCallback, cancelCallback);
            }
        });
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
        startFragment(fragment, container, null, 0);
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
        Class<?> _container = container;
        if (_container == null) {
            _container = UniversalActivity.class;
        }

        Intent intent = new Intent(this, _container);
        intent.putExtra("fragment", fragment.getName());
        if (args != null) {
            intent.putExtra("args", args);
        }

        if (requestCode > 0) {
            startActivityForResult(intent, requestCode);
        } else {
            startActivity(intent);
        }
    }

    /**
     * Analysis事件统计
     * @param eventId 自定义统计事件ID
     */
    public void analysisEvent(String eventId) {
        Analysis.getInstance().onEvent(this, eventId);
    }

    /**
     * Analysis事件统计
     * @param eventId 自定义统计事件ID
     * @param params Event Params
     */
    public void analysisEvent(String eventId, HashMap<String, String> params) {
        Analysis.getInstance().onEvent(this, eventId, params);
    }
}
