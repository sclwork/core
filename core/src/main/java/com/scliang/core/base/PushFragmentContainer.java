package com.scliang.core.base;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;

import com.scliang.core.R;

/**
 * Created by ShangChuanliang
 * on 16/5/4.
 */
public final class PushFragmentContainer extends DialogFragment
        implements DialogInterface.OnKeyListener {
    private FrameLayout mContainer;
    private BaseFragment mContentFragment;
    private DialogInterface.OnDismissListener mOnDismissListener;

    final void onRequestPermissionsResult(String permission, boolean granted) {
        if (mContentFragment != null) {
            mContentFragment.onRequestPermissionsResult(permission, granted);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mContentFragment != null) {
            mContentFragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_push, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mContainer = view.findViewById(R.id.container);
    }

    @Override
    public void onStart() {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setOnKeyListener(this);
            setDialogAttrs(dialog);
            if (!dialog.isShowing()) {
                mContainer.removeAllViews();
                mContainer.setVisibility(View.INVISIBLE);
            }
            if (mContentFragment != null) {
                BaseFragment saved = (BaseFragment) getChildFragmentManager()
                        .findFragmentByTag("Content");
                if (saved == null) {
                    getChildFragmentManager().beginTransaction()
                            .add(mContainer.getId(), mContentFragment, "Content")
                            .commitAllowingStateLoss();
                } else {
                    if (saved == mContentFragment) {
                        getChildFragmentManager().beginTransaction()
                                .show(mContentFragment)
                                .commitAllowingStateLoss();
                    } else {
                        getChildFragmentManager().beginTransaction()
                                .replace(mContainer.getId(), mContentFragment, "Content")
                                .commitAllowingStateLoss();
                    }
                }
                dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        final Animation anim = new TranslateAnimation(
                                Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0,
                                Animation.RELATIVE_TO_SELF, 1, Animation.RELATIVE_TO_SELF, 0);
                        anim.setDuration(200);
                        anim.setInterpolator(new DecelerateInterpolator());
                        anim.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                mContainer.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                        mContainer.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mContainer.clearAnimation();
                                mContainer.startAnimation(anim);
                            }
                        }, 200);
                    }
                });
                super.onStart();
            } else {
                super.onStart();
                dismiss();
            }
        }
    }

    @Override
    public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP &&
                keyCode == KeyEvent.KEYCODE_BACK) {
            dismiss();
            return true;
        }
        return false;
    }

    private void setDialogAttrs(Dialog dialog) {
        if (dialog == null) {
            return;
        }

        Context context = getContext();
        if (context == null) {
            return;
        }

        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (manager == null) {
            return;
        }

        DisplayMetrics dm = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(dm);

        Window window = dialog.getWindow();
        if (window == null) {
            return;
        }

        WindowManager.LayoutParams lp = window.getAttributes();
        if (!BaseActivity.sUseFullScreen) {
            try {
                FrameLayout content = null;

                Activity activity = getActivity();
                if (activity != null) {
                    Window win = activity.getWindow();
                    if (win != null) {
                        content = (FrameLayout) win.getDecorView();
                    }
                }

                int statusHeight = 0;
                if (activity instanceof BaseActivity) {
                    statusHeight = ((BaseActivity)activity).getStatusBarHeight();
                }

                if (content != null) {
                    int visibility = content.getSystemUiVisibility();
                    boolean fullscreen = (visibility & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) > 0;
                    View root = content.findViewById(R.id.root);
                    int height = root == null ? 0 : root.getHeight();
                    if (height == 0) {
                        View childOfContent = content.getChildAt(0);
                        if (fullscreen) {
                            if (childOfContent != null) {
                                lp.height = childOfContent.getHeight();
                            }
                        } else {
                            if (childOfContent instanceof ViewGroup) {
                                childOfContent = ((ViewGroup) childOfContent).getChildAt(1);
                                if (childOfContent != null) {
                                    lp.height = childOfContent.getHeight();
                                }
                            }
                        }
                    } else {
                        lp.height = height - (fullscreen ? statusHeight : 0);
                    }
                }
            } catch (Exception ignored) {}
        }
        lp.width = dm.widthPixels;
        dialog.getWindow().setAttributes(lp);
    }

    /**
     * 显示给定的对话框
     * @param manager
     * @param tag
     * @param contentFragment 内容Fragment
     */
    public void show(FragmentManager manager, String tag, BaseFragment contentFragment) {
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.BaseDialog);
        mContentFragment = contentFragment;
        mContentFragment.setPushContainer(this);
        try {
            show(manager, tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭对话框
     */
    @Override
    public void dismiss() {
        dismissAnimation(new Runnable() {
            @Override
            public void run() {
                PushFragmentContainer.super.dismiss();
            }
        });
    }

    /**
     * 关闭对话框
     */
    @Override
    public void dismissAllowingStateLoss() {
        dismissAnimation(new Runnable() {
            @Override
            public void run() {
                PushFragmentContainer.super.dismissAllowingStateLoss();
            }
        });
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mOnDismissListener != null) {
            mOnDismissListener.onDismiss(dialog);
        }
    }

    /**
     * 设置OnDismissListener
     */
    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        mOnDismissListener = listener;
    }

    private void dismissAnimation(final Runnable callback) {
        if (mContentFragment == null) {
            callback.run();
        } else {
            final Animation anim = new TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 0,
                    Animation.RELATIVE_TO_SELF, 0, Animation.RELATIVE_TO_SELF, 1);
            anim.setDuration(200);
            anim.setInterpolator(new AccelerateInterpolator());
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mContainer.setVisibility(View.INVISIBLE);
                    mContainer.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            callback.run();
                        }
                    }, 60);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            mContainer.clearAnimation();
            mContainer.startAnimation(anim);
        }
    }
}
