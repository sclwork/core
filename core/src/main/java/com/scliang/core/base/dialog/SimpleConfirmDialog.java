package com.scliang.core.base.dialog;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.scliang.core.R;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/12/4.
 */
public class SimpleConfirmDialog extends BaseDialog {
    private TextView mText;
    private TextView mCancel;
    private TextView mOK;
    private CharSequence mTextString;
    private CharSequence mOKString;
    private CharSequence mCancelString;
    private Runnable mOKCallback;
    private Runnable mCancelCallBack;
    private boolean mOnlyOKAction;

    /**
     * 设置点击[确定]按钮的回调
     */
    public void setOKCallback(Runnable callback) {
        mOKCallback = callback;
    }

    /**
     * 设置点击[取消]按钮的回调
     */
    public void setCancelCallBack(Runnable callback){
        mCancelCallBack = callback;
    }

    @Override
    protected View onCreateContextView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.view_dialog_simple_confirm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        view.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dismiss();
//            }
//        });
        mText = view.findViewById(R.id.text);
        mCancel = view.findViewById(R.id.cancel);
        mCancel.setText(mCancelString);
        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if(mCancelCallBack!=null){
                    mCancelCallBack.run();
                }
            }
        });
        ViewGroup vg = (ViewGroup) mCancel.getParent();
        if (vg != null) {
            vg.setVisibility(mOnlyOKAction ? View.GONE : View.VISIBLE);
        }
        mOK = view.findViewById(R.id.ok);
        mText.setText(mTextString);
        mOK.setText(mOKString);
        mOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (mOKCallback != null) {
                    mOKCallback.run();
                }
            }
        });
        mOK.setBackgroundResource(mOnlyOKAction ?
                R.drawable.btn_confirm_bottom_action : R.drawable.btn_confirm_right_action);
    }

    /**
     * 显示给定的对话框
     *
     * @param manager
     * @param tag
     * @param text            要显示的文本
     * @param ok
     * @param cancel
     * @param okCallback 点击[确定]按钮的回调
     * @param cancelCallBack 点击[取消]按钮的回调
     */
    public void show(FragmentManager manager, String tag,
                     CharSequence text,
                     CharSequence ok, CharSequence cancel,
                     Runnable okCallback, Runnable cancelCallBack) {
        mTextString = text;
        mCancelString = cancel;
        mOKString = ok;
        setOKCallback(okCallback);
        setCancelCallBack(cancelCallBack);
        try {
            show(manager, tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置是否仅仅支持OK按钮
     */
    public void onlyOKAction(boolean onlyOKAction) {
        mOnlyOKAction = onlyOKAction;
        if (mCancel != null) {
            ViewGroup vg = (ViewGroup) mCancel.getParent();
            if (vg != null) {
                vg.setVisibility(mOnlyOKAction ? View.GONE : View.VISIBLE);
            }
        }
        if (mOK != null) {
            mOK.setBackgroundResource(onlyOKAction ?
                    R.drawable.btn_confirm_bottom_action : R.drawable.btn_confirm_right_action);
        }
    }
}
