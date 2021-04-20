package com.scliang.core.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/9/20.
 */
public abstract class LoopVPAdapter<T> extends PagerAdapter
    implements ViewPager.OnPageChangeListener {
    private OnLoopResetCurrentItemListener mOnLoopResetCurrentItemListener;
    //    当前页面
    private int currentPosition = 0;

    protected SoftReference<Context> mContext;
    protected SoftReference<ViewPager> mViewPager;
    protected List<T> mItems = new ArrayList<>();

    public LoopVPAdapter(Context context) {
        mContext = new SoftReference<>(context);
    }

    public void setOnLoopResetCurrentItemListener(OnLoopResetCurrentItemListener listener) {
        mOnLoopResetCurrentItemListener = listener;
    }

    public void updateItems(List<T> items, ViewPager viewPager) {
        mItems.clear();
        // 如果数据大于一条
        if(items.size() > 1) {
            // 添加最后一页到第一页
            items.add(0, items.get(items.size() - 1));
            // 添加第一页(经过上行的添加已经是第二页了)到最后一页
            items.add(items.get(1));
        }

        mItems.addAll(items);
        mViewPager = new SoftReference<>(viewPager);
//        viewPager.setAdapter(this);
        viewPager.addOnPageChangeListener(this);
        viewPager.setCurrentItem(1,false);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        return getItemView(container, position, mItems.get(position));
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        View view = (View) object;
        container.removeView(view);
    }

    protected abstract View getItemView(@NonNull ViewGroup container, int position, T data);

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
        currentPosition = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        // 若viewpager滑动未停止，直接返回
        if (state != ViewPager.SCROLL_STATE_IDLE) return;
        // 若当前为第一张，设置页面为倒数第二张
        if (currentPosition == 0) {
            ViewPager viewPager = mViewPager == null ? null : mViewPager.get();
            if (viewPager != null) {
                viewPager.setCurrentItem(mItems.size() - 2, false);
                if (mOnLoopResetCurrentItemListener != null) {
                    mOnLoopResetCurrentItemListener.onLoopResetCurrentItem(viewPager.getCurrentItem());
                }
            }
        }
        // 若当前为倒数第一张，设置页面为第二张
        else if (currentPosition == mItems.size() - 1) {
            ViewPager viewPager = mViewPager == null ? null : mViewPager.get();
            if (viewPager != null) {
                viewPager.setCurrentItem(1, false);
                if (mOnLoopResetCurrentItemListener != null) {
                    mOnLoopResetCurrentItemListener.onLoopResetCurrentItem(viewPager.getCurrentItem());
                }
            }
        }
    }

    public interface OnLoopResetCurrentItemListener {
        void onLoopResetCurrentItem(int item);
    }
}
