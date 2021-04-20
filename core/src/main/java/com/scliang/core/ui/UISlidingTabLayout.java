package com.scliang.core.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.scliang.core.R;

public class UISlidingTabLayout extends HorizontalScrollView {
    int leftOffsetOfFirstTab = 0;
    private int TAB_VIEW_PADDING_DIPS = 18;
    private int TAB_VIEW_HEIGHT_DIPS = 60;
    private int TAB_VIEW_TEXT_SIZE_SP = 16;
    private int TAB_VIEW_TEXT_COUNT_SIZE_SP = 10;
    private int TAB_VIEW_SUB_SIZE_SP = 13;
//    private int mTabViewLayoutId;
//    private int mTabViewTextViewId;
    private ViewPager mViewPager;

    private final UISlidingTabStrip mTabStrip;
    private boolean isItemEqually = false;

    private @DrawableRes int mTabStripBackgroundResId;

    private OnTabClickListener mOnTabClickListener;

    public void setOnTabClickListener(OnTabClickListener listener) {
        mOnTabClickListener = listener;
    }

    public void setTabViewHeightDips(int value) {
        TAB_VIEW_HEIGHT_DIPS = value;
    }

    public void setTabViewTextSizeSp(int value) {
        TAB_VIEW_TEXT_SIZE_SP = value;
    }

    public void setTabViewPaddingDips(int value) {
        TAB_VIEW_PADDING_DIPS = value;
    }

    public void setLengthAdajustDips(int value) {
        mTabStrip.setLengthAdjustDips(value);
    }

    public void setIndicatorColors(int colors) {
        mTabStrip.setSelectedIndicatorColors(colors);
    }

    public UISlidingTabLayout(Context context) {
        this(context, null);
    }

    public UISlidingTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("NewApi")
    public UISlidingTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);
        // Make sure that the Tab Strips fills this View
        setFillViewport(true);

        mTabStrip = new UISlidingTabStrip(context);
        addView(mTabStrip, LayoutParams.MATCH_PARENT, dp2px(TAB_VIEW_HEIGHT_DIPS));
    }

    public void setCustomTabColorizer(UISlidingTabStrip.TabColorizer tabColorizer) {
        mTabStrip.setCustomTabColorizer(tabColorizer);
    }

    public void setTextNormalColor(int color) {
        mTabStrip.setCustomTextNormalColor(color);
    }

    public void setSubTextNormalColor(int color) {
        mTabStrip.setCustomSubTextNormalColor(color);
    }

    public void setTextSelectedColor(int color) {
        mTabStrip.setCustomTextColor(color);
    }

    public void setItemEqually() {
        isItemEqually = true;
        refresh();
    }

    public void updateSubText(String title, String sub) {
        mTabStrip.updateSubText(title, sub);
    }

    public void updateTitleCount(String title, String count) {
        mTabStrip.updateTitleCount(title, count);
    }

    public void updateTitleCount(String title, String count, boolean widthUpdatePos) {
        mTabStrip.updateTitleCount(title, count, widthUpdatePos);
    }

    public void updateTitleSuffix(String title, String suffix) {
        mTabStrip.updateTitleSuffix(title, suffix);
    }

    public void setSelectedIndicatorColors(int... colors) {
        mTabStrip.setSelectedIndicatorColors(colors);
    }

    public void setDividerColors(int... colors) {
        mTabStrip.setDividerColors(colors);
    }

//    public void setCustomTabView(int layoutResId, int textViewId) {
//        mTabViewLayoutId = layoutResId;
//        mTabViewTextViewId = textViewId;
//    }

    public void setViewPager(ViewPager viewPager) {
        mTabStrip.removeAllViews();
        mViewPager = viewPager;
        if (viewPager != null) {
            viewPager.addOnPageChangeListener(new InternalViewPagerListener());
            populateTabStrip();
        }
    }

    public void setTabStripBackground(@DrawableRes int backgroundResId) {
        mTabStripBackgroundResId = backgroundResId;
        if (mViewPager != null) {
            mTabStrip.removeAllViews();
            populateTabStrip();
        }
    }

    public void refresh() {
        if (mViewPager != null) {
            mTabStrip.removeAllViews();
            populateTabStrip();
        }
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

    @SuppressLint("ResourceType")
    protected LinearLayout createDefaultTabView(Context context) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        if (mTabStripBackgroundResId > 0) {
            try {
                Drawable background = context.getResources().getDrawable(mTabStripBackgroundResId);
                root.setBackground(background);
            } catch (Resources.NotFoundException e) {
                root.setBackgroundColor(0x0);
            }
        } else {
            root.setBackgroundColor(0x0);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0);
        lp.weight = 1;

        RelativeLayout titleContainer = new RelativeLayout(context);

        TextView titleView = new TextView(context);
        titleView.setId(R.id.id_sliding_tab_title);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP);
        titleView.setTextColor(0xff000000);
        int padding = dp2px(TAB_VIEW_PADDING_DIPS);
        if (isItemEqually) {
            titleView.setPadding(0, padding / 2, 0, 0);
        } else {
            titleView.setPadding(padding, padding / 2, padding, 0);
        }

        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        rlp.addRule(RelativeLayout.CENTER_IN_PARENT);
        titleContainer.addView(titleView, rlp);

        TextView titleCountView = new TextView(context);
//        titleCountView.setText("999+");
        titleCountView.setTextColor(0xffffffff);
        titleCountView.setBackgroundResource(R.drawable.shape_read_tip_bg);
        titleCountView.setPadding(dp2px(5), dp2px(1), dp2px(5), dp2px(1));
        titleCountView.setGravity(Gravity.CENTER);
        titleCountView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_COUNT_SIZE_SP);
        titleCountView.setVisibility(GONE);
        // !!! 在UISlidingTabStrip的updateTitleCount方法里会有修改 !!!
        rlp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//        rlp.addRule(RelativeLayout.ALIGN_RIGHT, R.id.id_sliding_tab_title);
        rlp.addRule(RelativeLayout.RIGHT_OF, R.id.id_sliding_tab_title);
        rlp.topMargin = dp2px(10);
        titleContainer.addView(titleCountView, rlp);

        root.addView(titleContainer, lp);

        lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        TextView subView = new TextView(context);
        subView.setGravity(Gravity.CENTER);
        subView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_SUB_SIZE_SP);
        subView.setTextColor(0xff666666);
        subView.setPadding(padding, 0, padding, padding / 2);
        root.addView(subView, lp);

        return root;
    }

    private void populateTabStrip() {
        final PagerAdapter adapter = mViewPager.getAdapter();
        final OnClickListener tabClickListener = new TabClickListener();
        if (adapter != null) {
            for (int i = 0; i < adapter.getCount(); i++) {
                LinearLayout tabView;
//                if (mTabViewLayoutId != 0) {
//                    tabView = LayoutInflater.from(getContext()).inflate(mTabViewLayoutId, mTabStrip,
//                            false);
//                    tabTitleView = tabView.findViewById(mTabViewTextViewId);
//                }

//                if (tabView == null) {
                    tabView = createDefaultTabView(getContext());
//                }

//                if (tabTitleView == null && TextView.class.isInstance(tabView)) {
//                    tabTitleView = (TextView) tabView;
//                }

                TextView titleView = (TextView) (((RelativeLayout) tabView.getChildAt(0)).getChildAt(0));
                TextView subView = (TextView) tabView.getChildAt(1);

                String[] titles = adapter.getPageTitle(i).toString().split("_");
                String title = "";
                if (titles.length > 0) {
                    title = titles[0];
                    if (titles.length > 1) {
                        titleView.setText(title);
                        subView.setText(titles[1]);
                        titleView.setVisibility(VISIBLE);
                        subView.setVisibility(VISIBLE);
                    } else {
                        titleView.setText(title);
                        titleView.setVisibility(VISIBLE);
                        titleView.setPadding(titleView.getPaddingLeft(), 0,
                                titleView.getPaddingRight(), 0);
                        subView.setVisibility(GONE);
                    }
                }
                tabView.setOnClickListener(tabClickListener);

                int screenWidth = getResources().getDisplayMetrics().widthPixels;
                int padding = dp2px(TAB_VIEW_PADDING_DIPS);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        isItemEqually ? 0 : LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
                if (isItemEqually) {
                    layoutParams.weight = 1;
                }

                Rect bounds = new Rect();
                if (i == 0) {
                    int width = bounds.width() + 2 * padding;
                    leftOffsetOfFirstTab = screenWidth / 2 - width / 2;
                    layoutParams.leftMargin = 0;
                    layoutParams.gravity = Gravity.CENTER_VERTICAL;
                    mTabStrip.addView(tabView, layoutParams);
                } else if (i == (adapter.getCount() - 1)) {
                    layoutParams.leftMargin = 0;
                    layoutParams.rightMargin = 0;
                    layoutParams.gravity = Gravity.CENTER_VERTICAL;
                    mTabStrip.addView(tabView, layoutParams);
                } else {
                    layoutParams.gravity = Gravity.CENTER_VERTICAL;
                    mTabStrip.addView(tabView, layoutParams);
                }
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mViewPager != null) {
            scrollToTab(mViewPager.getCurrentItem(), 0);
        }
    }

    private void scrollToTab(int tabIndex, int positionOffset) {
        final int tabStripChildCount = mTabStrip.getChildCount();
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount) {
            return;
        }

        View selectedChild = mTabStrip.getChildAt(tabIndex);
        if (selectedChild != null) {
            int targetScrollX = selectedChild.getLeft() + positionOffset;
            scrollTo(targetScrollX - leftOffsetOfFirstTab, 0);
        }

        for (int i = 0; i < mTabStrip.getChildCount(); i++) {
            View child = mTabStrip.getChildAt(i);
            child.setSelected(false);
        }

        if (selectedChild != null) {
            selectedChild.setSelected(true);
        }
    }

    private int mInternalViewPagerSelectedPos = -1;
    private class InternalViewPagerListener implements ViewPager.OnPageChangeListener {
        private int mScrollState;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (mInternalViewPagerSelectedPos == -1) {
                changeSelectPage(position, positionOffset);
            }
            mInternalViewPagerSelectedPos = position;
        }

        @SuppressLint("NewApi")
        @Override
        public void onPageScrollStateChanged(int state) {
            mScrollState = state;
            if (mScrollState == ViewPager.SCROLL_STATE_IDLE && mInternalViewPagerSelectedPos >= 0) {
                changeSelectPage(mInternalViewPagerSelectedPos, 0);
            }
        }

        @Override
        public void onPageSelected(int position) {
            if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
                mTabStrip.onViewPagerPageChanged(position, 0f);
                scrollToTab(position, 0);
            }
        }

        private void changeSelectPage(int position, float positionOffset) {
            int tabStripChildCount = mTabStrip.getChildCount();
            if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount)) {
                return;
            }

            mTabStrip.onViewPagerPageChanged(position, positionOffset);

            View selectedTitle = mTabStrip.getChildAt(position);
            int extraOffset = (selectedTitle != null)
                    ? (int) (positionOffset * selectedTitle.getWidth())
                    : 0;
            scrollToTab(position, extraOffset);
        }
    }

    private class TabClickListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            for (int i = 0; i < mTabStrip.getChildCount(); i++) {
                if (v == mTabStrip.getChildAt(i)) {
                    mViewPager.setCurrentItem(i);
                    if (mOnTabClickListener != null) {
                        mOnTabClickListener.onTabClick(i);
                    }
                    return;
                }
            }
        }
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    public interface OnTabClickListener {
        void onTabClick(int position);
    }
}
