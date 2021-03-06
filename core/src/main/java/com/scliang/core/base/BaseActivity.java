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
     ?????????Toolbar???????????????
     HIDE  : ?????????Toolbar
     TOP   : Toolbar?????????Container??????,????????????????????????
     FLOAT : Toolbar?????????Container??????,?????????????????????
     */
    public enum ToolbarType {
        HIDE,  // ?????????Toolbar
        TOP,   // Toolbar?????????Container??????,????????????????????????
        FLOAT, // Toolbar?????????Container??????,?????????????????????
    }

    // ??????UI???,????????????UIHandler??????
    private Handler mUIHandler;

    // ?????????????????????
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

    // Toolbar?????????,???????????????Toolbar
    private ToolbarType mToolbarType = ToolbarType.HIDE;
    // ???????????????????????????Toolbar,?????????????????????????????????????????????????????????
    protected Toolbar mToolbar;
    private int mToolbarHeight;
    private boolean mToolbarBottomLineVisibility;
    private FrameLayout mToolbarCenterContainer;
    private FrameLayout mToolbarLeftContainer;
    private FrameLayout mToolbarRightContainer;

    // ????????????
    private PushFragmentContainer mPushFragmentContainer;
    private SimpleLoadDialog mSimpleLoadDialog;
    private final byte[] mLoadSync = new byte[]{0};

    // ??????????????? 0:?????? 1:??????
    private final static int KeyboardStateClosed = 0;
    private final static int KeyboardStateOpened = 1;
    private int mKeyboardState = KeyboardStateClosed;

    // ????????????????????????
    public static final boolean sUseFullScreen = false;

    // Fix:IllegalStateException: Can not perform this action after onSaveInstanceState
    private Method mFixNoteStateNotSavedMethod;
    private Object mFixFragmentMgr;
    private String[] mFixActivityClassName = {"Activity", "FragmentActivity"};

    // ????????????DrawerLayout??????
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
        // ??????onPreActivityCreate
        if (mRootFragment != null) {
            mRootFragment.onPreActivityCreate(this, savedInstanceState);
        }

        super.onCreate(savedInstanceState);
        mUIHandler = new Handler(Looper.getMainLooper());

        // ????????????????????????????????????
        if (checkSldeable()) {
            super.setContentView(initSlideViews(R.layout.activity_base));
        } else {
            Window window = getWindow();
            window.setBackgroundDrawable(new ColorDrawable(giveWindowColor()));
            // ??????????????????DrawerLayout??????
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

        // ??????Toolbar???????????????????????????????????????????????? !!!
        getToolbarHeight(new Runnable() {
            @Override
            public void run() {
                // ??????ToolbarType
                setToolbarType(ToolbarType.TOP);
            }
        });

        // ???????????????DrawerHomeAction????????????
        if (useDrawerLayout()) {
            initDefaultDrawerHomeAction();
        }

        // ?????????????????????????????????
        Configuration configuration = getResources().getConfiguration();
        onConfigurationChanged(configuration);
    }

    @Override
    protected final void onResume() {
        super.onResume();
        // ????????????
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
        // ????????????
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
        // ????????????????????????
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

        // ?????????
        onRootFragmentViewCreated(view, savedInstanceState);
    }

    // ??????RootFragment????????????RootFragmentViewCreated??????????????????
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
        // ????????????
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
            // ??????????????????????????????
            mSlidePager.setSlideable(newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE);
        }
        // ???????????????????????????StatusBar????????????
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
        // ?????????BaseFragment?????????BaseActivity??????startFragment
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
     * Toolbar????????????????????????
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
     * Toolbar????????????????????????
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

    // ??????RootContainer
    protected RootContainer getRootContainer() {
        return mRootContainer;
    }

    // ???Resize??????????????????AndroidBug5497Workaround???????????????????????????
    protected void onKeyboardOpened() {
    }

    // ???Resize??????????????????AndroidBug5497Workaround???????????????????????????
    protected void onKeyboardClosed() {
        // ??????mShowPushDialogRunnable?????????
        if (mShowPushDialogRunnable != null) {
            mShowPushDialogRunnable.run();
            mShowPushDialogRunnable = null;
        }
    }

    // ???Resize??????????????????AndroidBug5497Workaround???????????????????????????
    protected void onKeyboardOpenedB() {
    }

    // ???Resize??????????????????AndroidBug5497Workaround???????????????????????????
    protected void onKeyboardClosedB() {
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

    /**
     * ????????????????????????dp????????????px??????
     * @param dp dp
     * @return px
     */
    public int dp2px(float dp) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

  /**
   * ??????App??????????????????
   * @param text ????????????
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
     * ??????Toolbar?????????????????????
     */
    protected void onToolbarBackPressed() {
        onBackPressed();
    }

    /**
     * ??????ToolbarDrawerHome???????????????
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
     * ????????????DrawerLayout
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
     * ????????????????????????
     */
    public void setSlideable(boolean slideable) {
        if (mSlidePager != null) {
            mSlidePager.setSlideable(slideable);
        }
    }

    // ?????????????????????????????????
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

    // ???????????????DrawerHomeAction????????????
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
     * ??????BaseApplication??????MaskView
     */
    protected void useApplicationMaskView() {
        setContentMaskView(BaseApplication.getApp().generateContentMaskView());
    }

    /**
     * ??????????????????View????????????????????????setContentView???????????????View??????????????????????????????
     */
    public void setContentMaskView(@LayoutRes int id) {
        try { setContentMaskView(getLayoutInflater().inflate(id, mContentMask, false));
        } catch (Exception ignored) {}
    }

    /**
     * ??????????????????View????????????????????????setContentView???????????????View??????????????????????????????
     */
    public void setContentMaskView(View view) {
        mContentMask.removeAllViews();
        if (view != null) mContentMask.addView(view);
    }

    /**
     * ????????????????????????View
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
        mContentContext.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /**
     * ??????ContentMaskView??????????????????
     */
    public boolean isContentMaskViewShowing() {
        return mContentMask.getVisibility() == View.VISIBLE;
    }

    /**
     * ??????Toolbar???????????????
     */
    public void showToolbarNavigationIcon() {
        initBackAction();
    }

    /**
     * ??????Toolbar???????????????
     */
    public void hideToolbarNavigationIcon() {
        mToolbar.setNavigationIcon(null);
    }

    /**
     * ???????????????Toolbar
     */
    public Toolbar getToolbar() {
        return mToolbar;
    }

    /**
     * ??????Toolbar-BottomLine????????????
     * @param show true:??????BottomLine;false:?????????
     */
    public void setToolbarBottomLineVisibility(boolean show) {
        setToolbarBottomLineVisibility(show, -1);
    }

    /**
     * ??????Toolbar-BottomLine????????????
     * @param show true:??????BottomLine;false:?????????
     */
    public void setToolbarBottomLineVisibility(boolean show, @ColorInt int color) {
        mToolbarBottomLineVisibility = show;
        // ??????ToolBar?????????TOP|FLOAT????????????????????????
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
     * ??????????????????ToolbarType
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
            case HIDE:  // ?????????Toolbar
                mToolbarContainer.setVisibility(View.INVISIBLE);
                setToolbarBottomLineVisibility(false);
                containerLp.topMargin = 0;
                break;
            case TOP:   // Toolbar?????????Container??????,????????????????????????
                mToolbarContainer.setVisibility(View.VISIBLE);
                setToolbarBottomLineVisibility(mToolbarBottomLineVisibility);
                containerLp.topMargin = mToolbarHeight;
                break;
            case FLOAT: // Toolbar?????????Container??????,?????????????????????
                mToolbarContainer.setVisibility(View.VISIBLE);
                setToolbarBottomLineVisibility(mToolbarBottomLineVisibility);
                containerLp.topMargin = 0;
                break;
        }
        mContentContainer.setLayoutParams(containerLp);
    }

    /**
     * ????????????Toolbar???????????????
     * @return
     */
    public ToolbarType getToolbarType() {
        return mToolbarType;
    }

    /**
     * ??????Toolbar?????????
     * @param background
     */
    public void setToolbarBackground(Drawable background) {
        mToolbarContainer.setBackground(background);
    }

    /**
     * ??????ToolbarPaddingTop
     */
    public void setToolbarPaddingStatusBar() {
        mToolbarContainer.paddingStatusBar();
    }

    /**
     * ??????ContentPaddingTop
     */
    public void setContentPaddingStatusBar() {
        mContentContainer.paddingStatusBar();
    }

    /**
     * ??????Toolbar???????????????????????????
     * @param menuId
     */
    public void setToolbarMenu(@MenuRes int menuId) {
        mToolbar.getMenu().clear();
        mToolbar.inflateMenu(menuId);
        mToolbar.setOnMenuItemClickListener(this);
    }

    /**
     * ??????Toolbar???????????????
     * @param title
     */
    public void setToolbarCenterTitle(@StringRes int title) {
        setToolbarCenterTitle(getString(title), 0xff333333, 17);
    }

    /**
     * ??????Toolbar???????????????
     * @param title
     */
    public void setToolbarCenterTitle(CharSequence title) {
        setToolbarCenterTitle(title, 0xff333333, 17);
    }

    /**
     * ??????Toolbar???????????????
     * @param title
     * @param color ???:0xffcccccc
     * @param size ??????:DIP
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
     * ??????Toolbar???????????????
     * @param title
     */
    public void setToolbarCenterTitle(CharSequence title, boolean autoSize) {
        setToolbarCenterTitle(title, 0xff333333, autoSize);
    }

    /**
     * ??????Toolbar???????????????
     * @param title
     * @param color ???:0xffcccccc
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
     * ??????Toolbar????????????????????????
     * @param view
     */
    public void setToolbarCenterCustomView(View view) {
        mToolbarCenterContainer.removeAllViews();
        if (view != null && view.getParent() == null) {
            mToolbarCenterContainer.addView(view);
        }
    }

    /**
     * ??????Toolbar????????????????????????
     * @param view
     */
    public void setToolbarLeftCustomView(View view) {
        mToolbarLeftContainer.removeAllViews();
        if (view != null && view.getParent() == null) {
            mToolbarLeftContainer.addView(view);
        }
    }

    /**
     * ??????Toolbar????????????????????????
     * @param view
     */
    public void setToolbarRightCustomView(View view) {
        mToolbarRightContainer.removeAllViews();
        if (view != null && view.getParent() == null) {
            mToolbarRightContainer.addView(view);
        }
    }

    // ????????????Toolbar?????????,????????????mToolbarHeight???
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
     * ??????Content???????????????,???????????????
     */
    protected void onContentViewShowed() {}

    /**
     * ????????????????????????????????????
     * @return
     */
    protected boolean checkSldeable() {
        Drawable background = getWindow().getDecorView().getBackground();
        return background != null && background instanceof ColorDrawable &&
                ((ColorDrawable) background).getColor() == 0;
    }

    // ???????????????
    private void hideSoftInput() {
        if (mRootContainer != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mRootContainer.getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    // ?????????SlideViews
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

    // ??????????????????????????????????????????
    final void requestPermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults) {
        for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
            // ??????PushFragmentContainer
            if (mPushFragmentContainer != null) {
                mPushFragmentContainer.onRequestPermissionsResult(permissions[i],
                        grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
            // ????????????
            onRequestPermissionsResult(permissions[i],
                    grantResults[i] == PackageManager.PERMISSION_GRANTED);
            // ??????????????????Fragment,???????????????Fragment
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
     * ????????????
     * @param permission ?????????????????????
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
     * ??????
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
     * ??????Window???????????????
     */
    protected int giveWindowColor() {
        return 0xfff7f8f9;
    }

    /**
     * ??????StatusBar?????????
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
            ("meizu".equalsIgnoreCase(manufacturer) && version < Build.VERSION_CODES.M) || // ??????????????????Meizu
            ("oppo".equalsIgnoreCase(manufacturer) && version < Build.VERSION_CODES.M) || // ??????????????????vivo
            ("vivo".equalsIgnoreCase(manufacturer) && version < Build.VERSION_CODES.M)) { // ??????????????????vivo
            Configuration configuration = getResources().getConfiguration();
            boolean isPortrait = configuration.orientation != Configuration.ORIENTATION_LANDSCAPE;
            return isPortrait ? 0xff666666 : appGiveColor;
        } else {
            return appGiveColor;
        }
    }

    /**
     * ??????NavigationBar?????????
     */
    protected int giveNavigationBarColor() {
        return 0xff111111;
    }

    /**
     * ??????StatusBar?????????
     * @param color x0xxxxxxxx
     */
    protected void setStatusBarColor(int color) {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(color);
        }
    }

    /**
     * ??????StatusBar?????????
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
     * ??????NavigationBar?????????
     * @param color x0xxxxxxxx
     */
    protected void setNavigationBarColor(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setNavigationBarColor(color);
        }
    }

    /**
     * ?????????????????????NavigationBar
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
     * ??????NavigationBar?????????????????????????????????
     * ???????????????????????????NavigationBar????????????
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
     * ????????????????????????NavigationBar??????????????????????????????????????????????????????
     * @param orientation ????????????
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
     * ??????????????????BaseContainer???padding?????????NavigationBar????????????
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
     * ??????RootFragment
     * @param savedInstanceState Bundle
     * @return BaseFragment
     */
    protected BaseFragment setupRootFragment(Bundle savedInstanceState) {
        return null;
    }

    // ??????ContentView?????? - ????????????
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

    // ????????????????????????
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

    // ??????????????????,????????????
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

    // ??????????????????,????????????
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

    // ????????????????????????View????????????????????????
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

//    // ??????????????????
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
     * ????????????????????????
     * @param action Action
     * @param args Args
     */
    public void sendLocalBroadcast(String action, Bundle args) {
        BaseApplication.getApp().sendLocalBroadcast(action, args);
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
     * ???????????????????????????
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
     * ????????????Toast
     */
    public void toast(String text) {
        toast(text, null);
    }

    /**
     * ????????????Toast
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
     * ????????????????????????????????????????????????
     * @param contentFragment ??????????????????Fragment
     * @return
     */
    public PushFragmentContainer showPushDialog(final BaseFragment contentFragment) {
        return showPushDialog(contentFragment, null);
    }

    /**
     * ????????????????????????????????????????????????
     * @param contentFragment ??????????????????Fragment
     * @return
     */
    public PushFragmentContainer showPushDialog(final BaseFragment contentFragment, final DialogInterface.OnDismissListener onDismissListener) {
        // ?????????
        if (mPushFragmentContainer != null) {
            return mPushFragmentContainer;
        }
        // ????????????PushDialog
        final PushFragmentContainer dialog = new PushFragmentContainer();
        if (sUseFullScreen) {
            mShowPushDialogRunnable = null;
            showPushDialogCore(contentFragment, dialog, getClass().getName());
        } else {
            if (mKeyboardState == KeyboardStateOpened) {
                // ???????????????????????????????????????????????????????????????showPushDialog??????
                // ??????onKeyboardClosed?????????showPushDialog??????
                mShowPushDialogRunnable = new Runnable() {
                    @Override
                    public void run() {
                        showPushDialogCore(contentFragment, dialog, getClass().getName());
                    }
                };
                // ????????????
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                }
            } else {
                mShowPushDialogRunnable = null;
                // ??????????????????????????????????????????shoPushDialog??????
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
     * ????????????????????????ProgressBar??????????????????
     * ????????????????????????????????????????????????
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
     * ????????????????????????ProgressBar??????????????????
     * ????????????????????????????????????????????????
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
     * ????????????Airbnb????????????????????????
     * ????????????????????????????????????????????????
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
     * ????????????Airbnb????????????????????????
     * ????????????????????????????????????????????????
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
     * ????????????????????????SimpleLoadDialog??????Tip
     */
    public void updateSimpleLoadDialogTip(final String tip) {
        mUpdateSimpleLoadDialogTipCount = 200;
        updateSimpleLoadDialogTipCore(tip);
    }

    /**
     * ????????????????????????SimpleLoadDialog??????Tip
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
     * ????????????????????????SimpleLoadDialog??????Tip
     */
    public void updateSimpleLoadDialogTip(final String tip,
                                          final int tipColor,
                                          final int tipSize) {
        mUpdateSimpleLoadDialogTipCount = 200;
        updateSimpleLoadDialogTipCore(tip, tipColor, tipSize);
    }

    /**
     * ????????????????????????SimpleLoadDialog??????Tip
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
     * ????????????????????????????????????ProgressBar??????????????????
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
     * ????????????????????????????????????ProgressBar??????????????????
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
     * ????????????????????????ProgressBar?????????View
     * ????????????????????????????????????????????????
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
     * ????????????????????????????????????ProgressBar?????????View
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
        startFragment(fragment, container, null, 0);
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
     * Analysis????????????
     * @param eventId ?????????????????????ID
     */
    public void analysisEvent(String eventId) {
        Analysis.getInstance().onEvent(this, eventId);
    }

    /**
     * Analysis????????????
     * @param eventId ?????????????????????ID
     * @param params Event Params
     */
    public void analysisEvent(String eventId, HashMap<String, String> params) {
        Analysis.getInstance().onEvent(this, eventId, params);
    }
}
