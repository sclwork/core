package com.scliang.core.base.dialog;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.scliang.core.R;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/12/12.
 */
public class SimpleToastDialog extends BaseDialog {
//    private Timer mShowTimer;

    @Override
    protected View onCreateContextView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.view_toast_old, container, false);
    }

    @Override
    protected FrameLayout.LayoutParams onCreateContextLayoutParams() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (onNeedTransparentBackground()) {
            setContainerBackgroundColor(0x00000000);
        }
        Bundle args = getArguments();
        if (args != null) {
            TextView textView = view.findViewById(R.id.text);
            if (textView != null) {
                textView.setText(args.getCharSequence("Text"));
            }
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    dismiss();
                } catch (Exception ignored) {}
            }
        }, 3000);
    }

//    @Override
//    public void show(FragmentManager manager, String tag) {
//        super.show(manager, tag);
//        startShowTimer();
//    }

//    @Override
//    public int show(FragmentTransaction transaction, String tag) {
//        int res = super.show(transaction, tag);
//        startShowTimer();
//        return res;
//    }

//    @Override
//    public void onStop() {
//        super.onStop();
//        if (mShowTimer != null) {
//            mShowTimer.cancel();
//            mShowTimer = null;
//        }
//    }

    protected boolean onNeedTransparentBackground() {
        return true;
    }

//    private void startShowTimer() {
//        if (mShowTimer != null) {
//            mShowTimer.cancel();
//            mShowTimer = null;
//        }
//        mShowTimer = new Timer();
//        mShowTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                dismiss();
//            }
//        }, 3000);
//    }
}
