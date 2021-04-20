package com.scliang.core.demo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.scliang.core.base.BaseFragment;
import com.scliang.core.ui.Category;
import com.scliang.core.ui.UICategoryRecyclerView;
import com.scliang.core.ui.UIRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * JCore
 * Created by ShangChuanliang
 * on 2017/11/7.
 */
public class DemoCategoryListFragment extends BaseFragment {

    @Override
    protected View onCreateViewHere(@NonNull LayoutInflater inflater,
                                    @Nullable ViewGroup container,
                                    @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category_list_demo, container, false);
    }

    @Override
    protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreatedHere(view, savedInstanceState);
        UICategoryRecyclerView recyclerView = view.findViewById(R.id.category);
        recyclerView.setRefreshView(R.layout.view_recycler_refresh);
        recyclerView.setLoadMoreView(R.layout.view_recycler_loadmore);
        recyclerView.setOnCategoryRecyclerListener(new UICategoryRecyclerView.OnCategoryRecyclerListener() {
            @Override
            public void onCreateRecyclerView(Category category, UIRecyclerView recyclerView) {
            }

            @Override
            public void onStartRefresh(final Category category,
                                       final UIRecyclerView recyclerView) {
                recyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.completeRefresh();
                    }
                }, 2000);
            }
            @Override
            public void onStartLoadMore(final Category category,
                                        final UIRecyclerView recyclerView) {
                recyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.completeLoadMore();
                    }
                }, 2000);
            }
        });
        List<Category> categories = new ArrayList<>();
        categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps"));
        categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS1", "wps1"));
        categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS2", "wps2"));
//            categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS3", "wps3"));
//            categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS4", "wps4"));
//            categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS5", "wps5"));
//            categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS6", "wps6"));
//            categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS7", "wps7"));
//            categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS8", "wps8"));
//            categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS9", "wps9"));
//            categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS0", "wps0"));
//            categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS22", "wps22"));
//            categories.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS33", "wps33"));
        recyclerView.setCategoryItemEqually();
        recyclerView.updateCategories(categories);
        recyclerView.updateSlidingTabTitleCount("WPS", "999+");
        recyclerView.updateSlidingTabTitleCount("WPS1", "point");
        recyclerView.updateSlidingTabTitleCount("WPS2", "99");
//            recyclerView.updateSlidingTabTitleCount("WPS", "");
    }
}
