package com.scliang.core.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.scliang.core.R;

public class UISlidingTabStrip extends LinearLayout {
    private static final int SELECTED_INDICATOR_THICKNESS_DIPS = 2;
    private static final int SELECTED_INDICATOR_TOP_OFFSET_DIPS = 0;
    private int SELECTED_INDICATOR_LENGTH_ADJUST_DIPS = 25;
    private static final int DEFAULT_SELECTED_INDICATOR_COLOR = 0xFF33B5E5;
    private static final byte DEFAULT_DIVIDER_COLOR_ALPHA = 0x20;
    private final int mSelectedIndicatorThickness;
    private final Paint mSelectedIndicatorPaint;
    private int mSelectedPosition;
    private float mSelectionOffset;
    private TabColorizer mCustomTabColorizer;
    private final SimpleTabColorizer mDefaultTabColorizer;
    private DisplayMetrics dm = new DisplayMetrics();

    public void setLengthAdjustDips(int value) {
        SELECTED_INDICATOR_LENGTH_ADJUST_DIPS = value;
        postInvalidate();
    }

    private int mNormalStrColor = 0xff000000;
    private int mNormalSubStrColor = 0xff666666;
    private int mStrColor = 0xff33b5e5;

    /**
     * Allows complete control over the colors drawn in the tab layout. Set with
     */
    public interface TabColorizer {

        /**
         * @return return the color of the indicator used when {@code position} is selected.
         */
        int getIndicatorColor(int position);

        /**
         * @return return the color of the divider drawn to the right of {@code position}.
         */
        int getDividerColor(int position);

    }

    public UISlidingTabStrip(Context context) {
        this(context, null);
    }

    public UISlidingTabStrip(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(false);

        final float density = getResources().getDisplayMetrics().density;

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorForeground, outValue, true);
        final int themeForegroundColor = outValue.data;

        mDefaultTabColorizer = new SimpleTabColorizer();
        mDefaultTabColorizer.setIndicatorColors(DEFAULT_SELECTED_INDICATOR_COLOR);
        mDefaultTabColorizer.setDividerColors(setColorAlpha(themeForegroundColor,
                DEFAULT_DIVIDER_COLOR_ALPHA));

        mSelectedIndicatorThickness = (int) (SELECTED_INDICATOR_THICKNESS_DIPS * density);
        mSelectedIndicatorPaint = new Paint();

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (wm != null) wm.getDefaultDisplay().getMetrics(dm);
    }

    public void setCustomTabColorizer(TabColorizer customTabColorizer) {
        mCustomTabColorizer = customTabColorizer;
        invalidate();
    }

    public void setCustomTextNormalColor(int strColor) {
        mNormalStrColor = strColor;
    }

    public void setCustomSubTextNormalColor(int strColor) {
        mNormalSubStrColor = strColor;
    }

    public void setCustomTextColor(int strColor) {
        mStrColor = strColor;
    }

    public void setSelectedIndicatorColors(int... colors) {
        // Make sure that the custom colorizer is removed
        mCustomTabColorizer = null;
        mDefaultTabColorizer.setIndicatorColors(colors);
        invalidate();
    }

    public void setDividerColors(int... colors) {
        // Make sure that the custom colorizer is removed
        mCustomTabColorizer = null;
        mDefaultTabColorizer.setDividerColors(colors);
        invalidate();
    }

    public void updateSubText(String title, String sub) {
        if (!TextUtils.isEmpty(title)) {
            for (int i = 0; i < getChildCount(); i++) {
                View tabView = getChildAt(i);
                if (tabView instanceof LinearLayout) {
                    TextView titleView = (TextView) (((RelativeLayout)((LinearLayout) tabView).getChildAt(0)).getChildAt(0));
                    TextView subView = (TextView) ((LinearLayout) tabView).getChildAt(1);
                    if (titleView != null && subView != null) {
                        String tTitle = titleView.getText() == null ? "" : (titleView.getText().toString());
                        if (tTitle.contains(title)) {
                            subView.setText(sub);
                            subView.setVisibility(VISIBLE);
                        }
                    }
                }
            }
        }
    }

    public void updateTitleCount(String title, String count) {
        updateTitleCount(title, count, false);
    }

    public void updateTitleCount(String title, String count, boolean widthUpdatePos) {
        if (!TextUtils.isEmpty(title)) {
            for (int i = 0; i < getChildCount(); i++) {
                View tabView = getChildAt(i);
                if (tabView instanceof LinearLayout) {
                    TextView titleView = (TextView) (((RelativeLayout)((LinearLayout) tabView).getChildAt(0)).getChildAt(0));
                    TextView titleCountView = (TextView) (((RelativeLayout)((LinearLayout) tabView).getChildAt(0)).getChildAt(1));
                    if (titleView != null && titleCountView != null) {
                        String tTitle = titleView.getText() == null ? "" : (titleView.getText().toString());
                        if (tTitle.contains(title)) {
                            if (TextUtils.isEmpty(count)) {
                                titleCountView.setVisibility(GONE);
                            } else {
                                if ("point".equals(count)) {
                                    titleCountView.setText("");
                                    titleCountView.setBackgroundResource(R.drawable.shape_read_tip_point_bg);
                                    titleCountView.setPadding(0, 0, 0, 0);
                                    titleCountView.setVisibility(VISIBLE);
                                    RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(dp2px(6), dp2px(6));
                                    if (widthUpdatePos) {
                                        rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                                        rlp.topMargin = dp2px(10);
                                        rlp.leftMargin = 0;
                                        rlp.rightMargin = dp2px(10);
                                    } else {
                                        rlp.addRule(RelativeLayout.RIGHT_OF, R.id.id_sliding_tab_title);
                                        rlp.topMargin = dp2px(10);
                                        rlp.leftMargin = dp2px(5);
                                    }
                                    titleCountView.setLayoutParams(rlp);
                                } else {
                                    titleCountView.setText(count);
                                    titleCountView.setBackgroundResource(R.drawable.shape_read_tip_bg);
                                    titleCountView.setPadding(dp2px(5), dp2px(1), dp2px(5), dp2px(1));
                                    titleCountView.setVisibility(VISIBLE);
                                    RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                                      RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                                    if (widthUpdatePos) {
                                        if (dm.widthPixels > 1080) {
                                            rlp.addRule(RelativeLayout.RIGHT_OF, R.id.id_sliding_tab_title);
                                            rlp.topMargin = dp2px(10);
                                            rlp.leftMargin = 0;
                                        } else {
                                            rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                                            rlp.topMargin = dp2px(10);
                                            rlp.leftMargin = 0;
                                        }
                                    } else {
                                        rlp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                                        rlp.topMargin = dp2px(10);
                                        rlp.leftMargin = 0;
                                        rlp.rightMargin = dp2px(10);
                                    }
                                    titleCountView.setLayoutParams(rlp);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    public void updateTitleSuffix(String title, String suffix) {
        if (!TextUtils.isEmpty(title)) {
            for (int i = 0; i < getChildCount(); i++) {
                View tabView = getChildAt(i);
                if (tabView instanceof LinearLayout) {
                    TextView titleView = (TextView) (((RelativeLayout)((LinearLayout) tabView).getChildAt(0)).getChildAt(0));
                    if (titleView != null) {
                        String tTitle = titleView.getText() == null ? "" : (titleView.getText().toString());
                        if (tTitle.contains(title)) {
                            if (TextUtils.isEmpty(suffix)) {
                                titleView.setText(title);
                            } else {
                                titleView.setText(title + suffix);
                            }
                        }
                    }
                }
            }
        }
    }

    public void onViewPagerPageChanged(int position, float positionOffset) {
        mSelectedPosition = position;
        mSelectionOffset = positionOffset;
        invalidate();
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

    @Override
    protected void onDraw(Canvas canvas) {
        final int height = getHeight();
        final int childCount = getChildCount();
        final TabColorizer tabColorizer = mCustomTabColorizer != null
                ? mCustomTabColorizer
                : mDefaultTabColorizer;

        // Thick colored underline below the current selection
        if (childCount > 0) {
            for (int i = 0; i < childCount; i++) {
                View tabView = getChildAt(i);
                if (tabView instanceof LinearLayout) {
                    TextView titleView = (TextView) (((RelativeLayout)((LinearLayout) tabView).getChildAt(0)).getChildAt(0));
                    TextView subView = (TextView) ((LinearLayout) tabView).getChildAt(1);
                    titleView.setTextColor(mNormalStrColor);
                    subView.setTextColor(mNormalSubStrColor);
                } else if (tabView instanceof TextView) {
                    ((TextView) tabView).setTextColor(mNormalStrColor);
                } else if (tabView instanceof RelativeLayout) {
                    RelativeLayout container = (RelativeLayout) tabView;
                    if (container.getChildCount() > 0) {
                        for (int j = 0; j < container.getChildCount(); j++) {
                            View view = container.getChildAt(j);
                            if (view != null && view instanceof TextView) {
                                TextView title = (TextView) view;
                                title.setTextColor(mNormalStrColor);
                            }
                        }
                    }
                }
            }

            View tabView = getChildAt(mSelectedPosition);
            if (tabView != null) {

                if (tabView instanceof LinearLayout) {
                    TextView titleView = (TextView) (((RelativeLayout)((LinearLayout) tabView).getChildAt(0)).getChildAt(0));
                    TextView subView = (TextView) ((LinearLayout) tabView).getChildAt(1);
                    titleView.setTextColor(mStrColor);
                    subView.setTextColor(mStrColor);
                } else if (tabView instanceof TextView) {
                    ((TextView) tabView).setTextColor(mStrColor);
                } else if (tabView instanceof RelativeLayout) {
                    RelativeLayout container = (RelativeLayout) tabView;
                    if (container.getChildCount() > 0) {
                        for (int j = 0; j < container.getChildCount(); j++) {
                            View view = container.getChildAt(j);
                            if (view != null && view instanceof TextView) {
                                TextView title = (TextView) view;
                                title.setTextColor(mStrColor);
                            }
                        }
                    }
                }

                int left = tabView.getLeft();
                int right = tabView.getRight();
                int color = tabColorizer.getIndicatorColor(mSelectedPosition);


                if (mSelectionOffset > 0f && mSelectedPosition < (getChildCount() - 1)) {
                    int nextColor = tabColorizer.getIndicatorColor(mSelectedPosition + 1);
                    if (color != nextColor) {
                        color = blendColors(nextColor, color, mSelectionOffset);
                    }

                    View nextTitle = getChildAt(mSelectedPosition + 1);

                    if (mSelectionOffset <= 0.5f && mSelectionOffset > 0f) {

                        float tempSelctionOffset = mSelectionOffset * 2;
                        right = (int) (tempSelctionOffset * nextTitle.getRight() +
                                (1.0f - tempSelctionOffset) * right);
                    } else if (mSelectionOffset > 0.5f && mSelectionOffset <= 1f) {

                        float tempSelectionOffset = (mSelectionOffset - 0.5f) * 2;
                        left = (int) (tempSelectionOffset * nextTitle.getLeft() +
                                (1.0f - tempSelectionOffset) * left);
                        right = nextTitle.getRight();
                    }
                }

                mSelectedIndicatorPaint.setColor(color);

                final float density = getResources().getDisplayMetrics().density;
                canvas.drawRect(left + SELECTED_INDICATOR_LENGTH_ADJUST_DIPS * density,
                        height - mSelectedIndicatorThickness - SELECTED_INDICATOR_TOP_OFFSET_DIPS * density,
                        right - SELECTED_INDICATOR_LENGTH_ADJUST_DIPS * density,
                        height - SELECTED_INDICATOR_TOP_OFFSET_DIPS * density,
                        mSelectedIndicatorPaint);
            }
        }
    }

    /**
     * Set the alpha value of the {@code color} to be the given {@code alpha} value.
     */
    private static int setColorAlpha(int color, byte alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Blend {@code color1} and {@code color2} using the given ratio.
     *
     * @param ratio of which to blend. 1.0 will return {@code color1}, 0.5 will give an even blend,
     *              0.0 will return {@code color2}.
     */
    private static int blendColors(int color1, int color2, float ratio) {
        final float inverseRation = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRation);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRation);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    private static class SimpleTabColorizer implements TabColorizer {
        private int[] mIndicatorColors;
        private int[] mDividerColors;

        @Override
        public final int getIndicatorColor(int position) {
            return mIndicatorColors[position % mIndicatorColors.length];
        }

        @Override
        public final int getDividerColor(int position) {
            return mDividerColors[position % mDividerColors.length];
        }

        void setIndicatorColors(int... colors) {
            mIndicatorColors = colors;
        }

        void setDividerColors(int... colors) {
            mDividerColors = colors;
        }
    }
}