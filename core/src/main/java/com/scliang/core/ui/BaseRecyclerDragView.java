package com.scliang.core.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;

import java.lang.ref.SoftReference;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/10/26.
 */
public abstract class BaseRecyclerDragView extends BaseViewGroup {
    protected UIHandler mUIHandler;

    public BaseRecyclerDragView(Context context) {
        super(context);
    }

    public BaseRecyclerDragView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseRecyclerDragView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        super.onInit();
        // UI Handler
        mUIHandler = new UIHandler(Looper.getMainLooper(), this);
    }

    public final void updateDragOffset(int offset, int length) {
        Message msg = mUIHandler.obtainMessage(100);
        msg.arg1 = offset;
        msg.arg2 = length;
        mUIHandler.sendMessage(msg);
    }

    public final void startAction() {
        mUIHandler.sendEmptyMessage(200);
    }

    public final void completeAction() {
        mUIHandler.sendEmptyMessage(300);
    }

    protected void onUpdateDragOffset(int offset, int length) {
        // nothing
    }

    protected void onStartAction() {
        // nothing
    }

    protected void onCompleteAction() {
        // nothing
    }




    /////////////////////////////////////////////
    /////////////////////////////////////////////
    private static class UIHandler extends Handler {
        private SoftReference<BaseRecyclerDragView> mView;

        public UIHandler(Looper looper, BaseRecyclerDragView view) {
            super(looper);
            mView = new SoftReference<>(view);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // onUpdateDragOffset
                case 100: {
                    BaseRecyclerDragView view = mView.get();
                    if (view != null) {
                        view.onUpdateDragOffset(msg.arg1, msg.arg2);
                    }
                } break;
                // onStartAction
                case 200: {
                    BaseRecyclerDragView view = mView.get();
                    if (view != null) {
                        view.onStartAction();
                    }
                } break;
                // onCompleteAction
                case 300: {
                    BaseRecyclerDragView view = mView.get();
                    if (view != null) {
                        view.onCompleteAction();
                    }
                } break;
            }
        }
    }
}
