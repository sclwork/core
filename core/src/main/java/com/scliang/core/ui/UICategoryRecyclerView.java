package com.scliang.core.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.scliang.core.R;
import com.scliang.core.base.BaseFragment;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/11/7.
 */
public class UICategoryRecyclerView extends BaseViewGroup {
    private SoftReference<BaseFragment> mFragment = new SoftReference<>(null);
    private List<Category> mCategories;
    private List<UIRecyclerView> mRecyclerViews;
    private List<LinearLayout> mItemViews;
    private List<ViewItem> mItems;
    private CategoriesPagerAdapter mCategoriesPagerAdapter;

    private Class<? extends RecyclerView.Adapter> ADAPTER_CLASS;
    private OnCategoryRecyclerListener mOnCategoryRecyclerListener;
    private OnCreateRecyclerTopContainerListener mOnCreateRecyclerTopContainerListener;
    private @LayoutRes
    int mRefreshViewId;
    private @LayoutRes
    int mLoadMoreViewId;
    private @LayoutRes
    int mLoadAlledViewId;

    private OnCategoryClickListener mOnCategoryClickListener;

    private UISlidingTabLayout mSlidingTab;
    private Rect mSlidingTabRect = new Rect();
    private FrameLayout mSlidingRightActionContainer;
    private Rect mSlidingRightActionContainerRect = new Rect();
    private FrameLayout mPagerTopContainer;
    private Rect mPagerTopContainerRect = new Rect();
    private ViewPager mViewPager;
    private Rect mViewPagerRect = new Rect();
    private boolean mShowSlidingTab = true;

    public UICategoryRecyclerView(Context context) {
        super(context);
    }

    public UICategoryRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public UICategoryRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onInit() {
        super.onInit();
        // UISlidingTabLayout
        mSlidingTab = new UISlidingTabLayout(getContext());
        mSlidingTab.setOnTabClickListener(new UISlidingTabLayout.OnTabClickListener() {
            @Override
            public void onTabClick(int position) {
                if (position >= 0 && mCategories != null && position < mCategories.size()) {
                    if (mOnCategoryClickListener != null) {
                        mOnCategoryClickListener.onCategoryClick(mCategories.get(position));
                    }
                }
            }
        });
        addView(mSlidingTab);
        // SlidingRightActionContainer
        mSlidingRightActionContainer = new FrameLayout(getContext());
        addView(mSlidingRightActionContainer);
        // PagerTopContainer
        mPagerTopContainer = new FrameLayout(getContext());
        addView(mPagerTopContainer);
        // ViewPager
        mViewPager = new ViewPager(getContext());
        addView(mViewPager);
        // 关联
        mCategories = new ArrayList<>();
        mRecyclerViews = new ArrayList<>();
        mItemViews = new ArrayList<>();
        mItems = new ArrayList<>();
        mCategoriesPagerAdapter = new CategoriesPagerAdapter(this);
        mViewPager.setAdapter(mCategoriesPagerAdapter);
        mSlidingTab.setViewPager(mViewPager);
    }

    @Override
    protected void onInitAttrs(AttributeSet attrs) {
        super.onInitAttrs(attrs);
        if (attrs == null) {
            return;
        }

        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.UICategoryRecyclerView, 0, 0);
        try {
            int slidingTabBackgroundColor = typedArray.getColor(R.styleable.UICategoryRecyclerView_slidingTabBackgroundColor, 0xffffffff);
            if (mSlidingTab != null) mSlidingTab.setBackgroundColor(slidingTabBackgroundColor);
            int viewPagerBackgroundColor = typedArray.getColor(R.styleable.UICategoryRecyclerView_viewPagerBackgroundColor, 0xffffffff);
            if (mViewPager != null) mViewPager.setBackgroundColor(viewPagerBackgroundColor);
        } finally {
            typedArray.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);

        if (mShowSlidingTab) {
            // SlidingRightActionContainer
            mSlidingRightActionContainer.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.AT_MOST),
                    View.MeasureSpec.makeMeasureSpec(mSlidingTabRect.height(), View.MeasureSpec.EXACTLY));
            int left = width - mSlidingRightActionContainer.getMeasuredWidth();
            mSlidingRightActionContainerRect.set(left, 0,
                    left + mSlidingRightActionContainer.getMeasuredWidth(),
                    mSlidingRightActionContainer.getMeasuredHeight());

            // UISlidingTabLayout
            mSlidingTab.measure(View.MeasureSpec.makeMeasureSpec(
                    width - mSlidingRightActionContainerRect.width(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dp2px(200), View.MeasureSpec.AT_MOST));
            mSlidingTabRect.set(0, 0,
                    mSlidingTab.getMeasuredWidth(),
                    mSlidingTab.getMeasuredHeight());
        }

        // PagerTopContainer
        mPagerTopContainer.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST));
        int top = mShowSlidingTab ? mSlidingTabRect.bottom : 0;
        mPagerTopContainerRect.set(0, top,
                mPagerTopContainer.getMeasuredWidth(),
                top + mPagerTopContainer.getMeasuredHeight());

        // ViewPager
        mViewPager.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(
                        height - mPagerTopContainerRect.bottom,
                        View.MeasureSpec.EXACTLY));
        top = mPagerTopContainerRect.bottom;
        mViewPagerRect.set(0, top,
                mViewPager.getMeasuredWidth(),
                top + mViewPager.getMeasuredHeight());

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (mShowSlidingTab) {
            // UISlidingTabLayout
            mSlidingTab.layout(mSlidingTabRect.left, mSlidingTabRect.top,
                    mSlidingTabRect.right, mSlidingTabRect.bottom);
            // SlidingRightActionContainer
            mSlidingRightActionContainer.layout(mSlidingRightActionContainerRect.left, mSlidingRightActionContainerRect.top,
                    mSlidingRightActionContainerRect.right, mSlidingRightActionContainerRect.bottom);
        }
        // PagerTopContainer
        mPagerTopContainer.layout(mPagerTopContainerRect.left, mPagerTopContainerRect.top,
                mPagerTopContainerRect.right, mPagerTopContainerRect.bottom);
        // ViewPager
        mViewPager.layout(mViewPagerRect.left, mViewPagerRect.top,
                mViewPagerRect.right, mViewPagerRect.bottom);
    }

    public void setFragment(BaseFragment fragment) {
        mFragment = new SoftReference<>(fragment);
    }

    public int getCurrentItem() {
        return mViewPager.getCurrentItem();
    }

    public void setCurrentItem(int item, boolean smoothScroll) {
        mViewPager.setCurrentItem(item, smoothScroll);
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        mOnCategoryClickListener = listener;
    }

    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
        mViewPager.addOnPageChangeListener(listener);
    }

    public void setSlidingTabItemBackground(@DrawableRes int backgroundResId) {
        if (mSlidingTab != null) {
            mSlidingTab.setTabStripBackground(backgroundResId);
        }
    }

    public void setSlidingTabIndicatorLRPadding(int dp) {
        if (mSlidingTab != null) {
            mSlidingTab.setLengthAdajustDips(dp);
        }
    }

    public void setSlidingTabIndicatorColor(@ColorInt int color) {
        if (mSlidingTab != null) {
            mSlidingTab.setIndicatorColors(color);
        }
    }

    public void setSlidingTabHeightDips(int value) {
        if (mSlidingTab != null) {
            mSlidingTab.setTabViewHeightDips(value);
        }
    }

    public void setSlidingTabViewTextSizeSp(int sp) {
        if (mSlidingTab != null) {
            mSlidingTab.setTabViewTextSizeSp(sp);
        }
    }

    public void setSlidingTabTextNormalColor(@ColorInt int color) {
        if (mSlidingTab != null) {
            mSlidingTab.setTextNormalColor(color);
        }
    }

    public void setSlidingTabSubTextNormalColor(@ColorInt int color) {
        if (mSlidingTab != null) {
            mSlidingTab.setSubTextNormalColor(color);
        }
    }

    public void setSlidingTabTextSelectedColor(@ColorInt int color) {
        if (mSlidingTab != null) {
            mSlidingTab.setTextSelectedColor(color);
        }
    }

    public void updateSlidingTabSubText(String title, String sub) {
        if (mSlidingTab != null) {
            mSlidingTab.updateSubText(title, sub);
        }
    }

    public void updateSlidingTabTitleCount(String title, String count) {
        if (mSlidingTab != null) {
            mSlidingTab.updateTitleCount(title, count);
        }
    }

    public void updateSlidingTabTitleCount(String title, String count, boolean widthUpdatePos) {
        if (mSlidingTab != null) {
            mSlidingTab.updateTitleCount(title, count, widthUpdatePos);
        }
    }

    public void updateSlidingTabSuffix(String title, String suffix) {
        if (mSlidingTab != null) {
            mSlidingTab.updateTitleSuffix(title, suffix);
        }
    }

    public final void setSlidingRightActionContextView(@LayoutRes int id) {
        if (mSlidingRightActionContainer != null) {
            try {
                mSlidingRightActionContainer.addView(LayoutInflater.from(getContext())
                                .inflate(id, mSlidingRightActionContainer, false),
                        new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                                FrameLayout.LayoutParams.MATCH_PARENT));
            } catch (Exception ignored) {}
        }
    }

    public final void setPagerTopContextView(@LayoutRes int id) {
        if (mPagerTopContainer != null) {
            try {
                mPagerTopContainer.removeAllViews();
                mPagerTopContainer.addView(LayoutInflater.from(getContext())
                                .inflate(id, mPagerTopContainer, false),
                        new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT));
            } catch (Exception ignored) {
                mPagerTopContainer.removeAllViews();
            }
        }
    }

    public void setRefreshView(@LayoutRes int id) {
        mRefreshViewId = id;
        if (mRecyclerViews != null) {
            for (UIRecyclerView recyclerView : mRecyclerViews) {
                recyclerView.setRefreshView(mRefreshViewId);
            }
        }
    }

    public void setLoadMoreView(@LayoutRes int id) {
        mLoadMoreViewId = id;
        if (mRecyclerViews != null) {
            for (UIRecyclerView recyclerView : mRecyclerViews) {
                recyclerView.setLoadMoreView(mLoadMoreViewId);
            }
        }
    }

    public void setLoadAlledView(@LayoutRes int id) {
        mLoadAlledViewId = id;
        if (mRecyclerViews != null) {
            for (UIRecyclerView recyclerView : mRecyclerViews) {
                recyclerView.setLoadAlledViewId(mLoadAlledViewId);
            }
        }
    }

    public void setCategoryItemEqually() {
        if (mSlidingTab != null) {
            mSlidingTab.setItemEqually();
        }
    }

    public void setSlidingTabVisibility(boolean show) {
        mShowSlidingTab = show;
        post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });
    }

    /**
     * 设置用于显示内容数据的RecyclerView.Adapter-Class
     */
    public void setContentAdapterClass(Class<? extends RecyclerView.Adapter> clz) {
        ADAPTER_CLASS = clz;
    }

    /**
     * 设置OnCategoryRecyclerListener
     */
    public void setOnCategoryRecyclerListener(OnCategoryRecyclerListener listener) {
        mOnCategoryRecyclerListener = listener;
    }

    /**
     * 设置创建RecyclerTopContainer的监听器
     */
    public void setOnCreateRecyclerTopContainerListener(OnCreateRecyclerTopContainerListener listener) {
        mOnCreateRecyclerTopContainerListener = listener;
    }

    /**
     * 更新Categories
     */
    public void updateCategories(List<Category> categories) {
        updateCategories(categories, null);
    }

    /**
     * 更新Categories
     */
    public void updateCategories(List<Category> categories, OnCategoriesUpdatedListener listener) {
        int current = mViewPager.getCurrentItem();
        mItems.clear();
        mCategories.clear();
        if (categories != null) {
            mCategories.addAll(categories);
        }
        for (int i = 0; i < mCategories.size(); i++) {
            ViewItem item = new ViewItem();
            item.category = mCategories.get(i);
            if (i < mRecyclerViews.size()) {
                item.recyclerView = mRecyclerViews.get(i);
                item.itemView = mItemViews.get(i);
            } else {
                UIRecyclerView recyclerView = createRecyclerView();
                mRecyclerViews.add(recyclerView);
                item.recyclerView = recyclerView;

                FrameLayout topContainer = new FrameLayout(getContext());
                LinearLayout itemView = createLinearLayout();
                itemView.addView(topContainer, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0
                );
                llp.weight = 1;
                itemView.addView(recyclerView, llp);
                mItemViews.add(itemView);
                item.itemView = itemView;
            }

            if (mOnCategoryRecyclerListener != null) {
                mOnCategoryRecyclerListener.onCreateRecyclerView(
                        item.category, item.recyclerView);
            }

            View top = item.itemView.getChildAt(0);
            if (top instanceof FrameLayout) {
                FrameLayout topContainer = (FrameLayout) top;
                topContainer.removeAllViews();
                if (mOnCreateRecyclerTopContainerListener != null) {
                    View topContextView = mOnCreateRecyclerTopContainerListener
                            .onCreateRecyclerTopContainer(item.category, topContainer);
                    if (topContextView != null) {
                        topContainer.addView(topContextView, new FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                        ));
                    }
                }
            }

            item.refreshListener = new RefreshListener(this, item);
            item.loadMoreListener = new LoadMoreListener(this, item);
            item.recyclerView.setOnRefreshListener(item.refreshListener);
            item.recyclerView.setOnLoadMoreListener(item.loadMoreListener);
            item.recyclerView.setRefreshView(mRefreshViewId);
            item.recyclerView.setLoadMoreView(mLoadMoreViewId);
            item.recyclerView.setRefreshable(true);
            item.recyclerView.setLoadMoreable(true);
            mItems.add(item);

            // 定制自定义操作
            if (listener != null) {
                listener.onCategoriesUpdated(item.category, item.recyclerView);
            }
        }
        mCategoriesPagerAdapter.notifyDataSetChanged();
        if (current >= mCategoriesPagerAdapter.getCount()) {
            mViewPager.setCurrentItem(0);
        }
        if (mSlidingTab != null) {
            mSlidingTab.refresh();
        }
    }

    private LinearLayout createLinearLayout() {
        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        return linearLayout;
    }

    private UIRecyclerView createRecyclerView() {
        UIRecyclerView recyclerView = new UIRecyclerView(getContext());
        if (ADAPTER_CLASS == null) {
            recyclerView.setAdapter(new SimpleAdapter());
        } else {
            try {
                RecyclerView.Adapter adapter = ADAPTER_CLASS.newInstance();
                if (adapter instanceof IBaseFragmentAdapter) {
                    ((IBaseFragmentAdapter)adapter).setFragment(mFragment.get());
                }
                recyclerView.setAdapter(adapter);
            } catch (InstantiationException | IllegalAccessException e) {
                recyclerView.setAdapter(new SimpleAdapter());
            }
        }
        return recyclerView;
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    private static class ViewItem {
        public Category category;
        public UIRecyclerView recyclerView;
        public LinearLayout itemView;
        private RefreshListener refreshListener;
        private LoadMoreListener loadMoreListener;
    }

    private static class RefreshListener implements UIRecyclerView.OnRefreshListener {
        private SoftReference<UICategoryRecyclerView> mView;
        private SoftReference<ViewItem> mItem;

        RefreshListener(UICategoryRecyclerView view, ViewItem item) {
            mView = new SoftReference<>(view);
            mItem = new SoftReference<>(item);
        }

        @Override
        public void onRefresh() {
            UICategoryRecyclerView view = mView.get();
            ViewItem item = mItem.get();
            if (view != null && item != null) {
                if (view.mOnCategoryRecyclerListener != null) {
                    view.mOnCategoryRecyclerListener.onStartRefresh(
                            item.category, item.recyclerView
                    );
                }
            }
        }
    }

    private static class LoadMoreListener implements UIRecyclerView.OnLoadMoreListener {
        private SoftReference<UICategoryRecyclerView> mView;
        private SoftReference<ViewItem> mItem;

        LoadMoreListener(UICategoryRecyclerView view, ViewItem item) {
            mView = new SoftReference<>(view);
            mItem = new SoftReference<>(item);
        }

        @Override
        public void onLoadMore() {
            UICategoryRecyclerView view = mView.get();
            ViewItem item = mItem.get();
            if (view != null && item != null) {
                if (view.mOnCategoryRecyclerListener != null) {
                    view.mOnCategoryRecyclerListener.onStartLoadMore(
                            item.category, item.recyclerView );
                }
            }
        }
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    private static class CategoriesPagerAdapter extends PagerAdapter {
        private SoftReference<UICategoryRecyclerView> mRecyclerView;

        CategoriesPagerAdapter(UICategoryRecyclerView view) {
            mRecyclerView = new SoftReference<>(view);
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (mRecyclerView != null && mRecyclerView.get() != null) {
                List<ViewItem> items = mRecyclerView.get().mItems;
                Category category = items.get(position).category;
                if (category != null) {
//                    return category.count >= 0 ?
//                            (category.title + "_" + category.count) : category.title;
                    return category.title;
                } else {
                    return "";
                }
            } else {
                return "";
            }
        }

        @Override
        public int getCount() {
            if (mRecyclerView != null && mRecyclerView.get() != null) {
                return mRecyclerView.get().mItems.size();
            } else {
                return 0;
            }
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            View view;
            if (mRecyclerView != null && mRecyclerView.get() != null) {
                view = mRecyclerView.get().mItems.get(position).itemView;
            } else {
                view = new FrameLayout(container.getContext());
            }
            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            View view;
            if (mRecyclerView != null && mRecyclerView.get() != null) {
                view = mRecyclerView.get().mItems.get(position).itemView;
            } else {
                view = null;
            }
            if (view != null) {
                container.removeView(view);
            }
        }
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    public interface OnCategoryRecyclerListener {
        void onCreateRecyclerView(Category category, UIRecyclerView recyclerView);
        void onStartRefresh(Category category, UIRecyclerView recyclerView);
        void onStartLoadMore(Category category, UIRecyclerView recyclerView);
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    public interface OnCreateRecyclerTopContainerListener {
        View onCreateRecyclerTopContainer(Category category, ViewGroup parent);
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    public interface OnCategoriesUpdatedListener {
        void onCategoriesUpdated(Category category, UIRecyclerView recyclerView);
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    private static class SimpleVHolder extends RecyclerView.ViewHolder {
        SimpleVHolder(View itemView) {
            super(itemView);
        }
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    private static class SimpleAdapter extends RecyclerView.Adapter<SimpleVHolder> {
        private List<String> mItems = new ArrayList<>();

        SimpleAdapter() {
            mItems.add("Item0");mItems.add("Item1");mItems.add("Item2");mItems.add("Item3");
            mItems.add("Item4");mItems.add("Item5");mItems.add("Item6");mItems.add("Item7");
            mItems.add("Item8");mItems.add("Item9");mItems.add("ItemA");mItems.add("ItemB");
            mItems.add("ItemC");mItems.add("ItemD");mItems.add("ItemE");mItems.add("ItemF");
        }

        @Override
        public SimpleVHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.view_simple_item, parent, false);
            return new SimpleVHolder(view);
        }

        @Override
        public void onBindViewHolder(SimpleVHolder holder, int position) {
            if (holder.itemView != null) {
                TextView text = holder.itemView.findViewById(R.id.text);
                if (text != null) {
                    text.setText(mItems.get(position));
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }
    }




    ////////////////////////////////////////////////////
    ////////////////////////////////////////////////////
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }
}
