package com.scliang.core.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.scliang.core.R;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/5/18.
 */
public class UIExpandTextView extends LinearLayout {
    public static final int DEFAULT_MAX_LINES = 3;
    private TextView mContentText;
    private TextView mContentAction;

    private String mText;
    private int mShowLines;
    private String mActionExpandText;
    private String mActionRetractText;
    private int mContentColor;
    private int mActionColor;
    private int mContentSize;
    private int mActionSize;
    private boolean hasRetract;
    private Drawable mActionExpandDrawable;
    private Drawable mActionRetractDrawable;

    private ExpandStatusListener mExpandStatusListener;
    private boolean isExpand;

    public UIExpandTextView(Context context) {
        super(context);
        initAttrs(null);
        initView();
    }

    public UIExpandTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs);
        initView();
    }

    public UIExpandTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(attrs);
        initView();
    }

    private void initAttrs(AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.UIExpandTextView, 0, 0);
            try {
                mText = typedArray.getString(R.styleable.UIExpandTextView_text);
                mShowLines = typedArray.getInt(R.styleable.UIExpandTextView_showLines, DEFAULT_MAX_LINES);
                mActionExpandText = typedArray.getString(R.styleable.UIExpandTextView_actionExpandText);
                mActionRetractText = typedArray.getString(R.styleable.UIExpandTextView_actionRetractText);
                mContentColor = typedArray.getColor(R.styleable.UIExpandTextView_contentColor, 0xff333333);
                mActionColor = typedArray.getColor(R.styleable.UIExpandTextView_actionColor, 0xff43adc8);
                mContentSize = typedArray.getDimensionPixelSize(R.styleable.UIExpandTextView_contentSize, 30);
                mActionSize = typedArray.getDimensionPixelSize(R.styleable.UIExpandTextView_actionSize, 20);
                hasRetract = typedArray.getBoolean(R.styleable.UIExpandTextView_hasRetract, true);
                mActionExpandDrawable = typedArray.getDrawable(R.styleable.UIExpandTextView_actionExpandImage);
                mActionRetractDrawable = typedArray.getDrawable(R.styleable.UIExpandTextView_actionImage);
            } finally {
                typedArray.recycle();
            }
        }

        if (TextUtils.isEmpty(mText)) {
            mText = "";
        }

        if (TextUtils.isEmpty(mActionExpandText)) {
            mActionExpandText = "展开全部";
        }

        if (TextUtils.isEmpty(mActionRetractText)) {
            mActionRetractText = "收起";
        }

        if (mActionExpandDrawable == null) mActionExpandDrawable = getResources().getDrawable(R.drawable.expand_all);
        mActionExpandDrawable.setBounds(0, 0,
                mActionExpandDrawable.getIntrinsicWidth(), mActionExpandDrawable.getIntrinsicHeight());
        if (mActionRetractDrawable == null) mActionRetractDrawable = getResources().getDrawable(R.drawable.retract_all);
        mActionRetractDrawable.setBounds(0, 0,
                mActionRetractDrawable.getIntrinsicWidth(), mActionRetractDrawable.getIntrinsicHeight());
    }

    private void initView() {
        setOrientation(LinearLayout.VERTICAL);
        LayoutInflater.from(getContext()).inflate(R.layout.view_expand_textview, this);
        mContentText = findViewById(R.id.content_text);
        mContentText.setTextColor(mContentColor);
        mContentText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mContentSize);

        if (mShowLines > 0) {
            mContentText.setMaxLines(mShowLines);
        }

        mContentAction = findViewById(R.id.content_action);
        mContentAction.setTextColor(mActionColor);
        mContentAction.setTextSize(TypedValue.COMPLEX_UNIT_PX, mActionSize);
        mContentAction.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                String textStr = mContentAction.getText().toString().trim();
                if (mActionExpandText.equals(textStr)) {
                    mContentText.setMaxLines(Integer.MAX_VALUE);
                    if (hasRetract) {
                        mContentAction.setText(mActionRetractText);
                        mContentAction.setCompoundDrawables(null, null, mActionRetractDrawable, null);
                        mContentAction.setVisibility(VISIBLE);
                    } else {
                        mContentAction.setVisibility(GONE);
                    }
                    setExpand(true);
                } else {
                    mContentText.setMaxLines(mShowLines);
                    mContentAction.setText(mActionExpandText);
                    mContentAction.setCompoundDrawables(null, null, mActionExpandDrawable, null);
                    mContentAction.setVisibility(VISIBLE);
                    setExpand(false);
                }

                // 通知外部状态已变更
                if (mExpandStatusListener != null) {
                    mExpandStatusListener.statusChanged(isExpand());
                }
            }
        });

        setText(mText);

        if (isExpand) {
            mContentAction.setCompoundDrawables(null, null, mActionRetractDrawable, null);
        } else {
            mContentAction.setCompoundDrawables(null, null, mActionExpandDrawable, null);
        }
    }

    public void setText(final CharSequence content) {
        mContentText.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mContentText.getViewTreeObserver().removeOnPreDrawListener(this);
                int linCount = mContentText.getLineCount();
                if (linCount > mShowLines) {
                    if (isExpand) {
                        mContentText.setMaxLines(Integer.MAX_VALUE);
                        if (hasRetract) {
                            mContentAction.setText(mActionRetractText);
                            mContentAction.setCompoundDrawables(null, null, mActionRetractDrawable, null);
                            mContentAction.setVisibility(VISIBLE);
                        } else {
                            mContentAction.setVisibility(GONE);
                        }
                    } else {
                        mContentText.setMaxLines(mShowLines);
                        mContentAction.setText(mActionExpandText);
                        mContentAction.setCompoundDrawables(null, null, mActionExpandDrawable, null);
                        mContentAction.setVisibility(VISIBLE);
                    }
                } else {
                    mContentAction.setVisibility(View.GONE);
                }
                return true;
            }
        });

        mContentText.setText(content);
    }

    public void setExpand(boolean isExpand) {
        this.isExpand = isExpand;
    }

    public boolean isExpand() {
        return this.isExpand;
    }

    public void setExpandStatusListener(ExpandStatusListener listener) {
        this.mExpandStatusListener = listener;
    }

    public interface ExpandStatusListener {
        void statusChanged(boolean isExpand);
    }
}
