package com.scliang.core.demo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;

import com.chad.library.adapter.base.BaseViewHolder;
import com.scliang.core.ui.BaseCategoryFragment;
import com.scliang.core.ui.Category;
import com.scliang.core.ui.OnCategoryDragListener;

import java.util.ArrayList;
import java.util.List;

/**
 * SCore Demo
 * Created by ShangChuanliang
 * on 2017/11/7.
 */
public class DemoCategoryFragment extends BaseCategoryFragment
        implements OnCategoryDragListener {

    @Override
    protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreatedHere(view, savedInstanceState);
        setOnCategoryDragListener(this);

        // debug
        List<Category> selected = new ArrayList<>();
        selected.add(new Category(Category.TYPE_MY_CATEGORY, "全部", "all"));
        selected.add(new Category(Category.TYPE_MY_CATEGORY, "Android", "android"));
        selected.add(new Category(Category.TYPE_MY_CATEGORY, "iOS", "ios"));
        List<Category> unselected = new ArrayList<>();
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps"));
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps1"));
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps2"));
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps3"));
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps4"));
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps5"));
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps6"));
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps7"));
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps8"));
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps9"));
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps0"));
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps22"));
        unselected.add(new Category(Category.TYPE_OTHER_CATEGORY, "WPS", "wps33"));
        updateCategories(selected, unselected);
    }

    @Override
    public void onItemMove(int starPos, int endPos) {
    }

    @Override
    public void onMoveToMyChannel(int starPos, int endPos) {
    }

    @Override
    public void onMoveToOtherChannel(int starPos, int endPos) {
    }

    @Override
    public void onStarDrag(BaseViewHolder baseViewHolder) {
    }
}
