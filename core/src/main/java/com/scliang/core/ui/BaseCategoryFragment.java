package com.scliang.core.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.chad.library.adapter.base.BaseViewHolder;
import com.scliang.core.R;
import com.scliang.core.base.BaseFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/11/7.
 */
public abstract class BaseCategoryFragment extends BaseFragment {
    private static final int sSpanCount = 3;
    private String mMyLabel = "我的";
    private String mMyTip = "";
    private String mRecommendLabel = "推荐";
    private List<Category> mCategory = new ArrayList<>();
    private CategoryAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private ItemTouchHelper mHelper;
    private OnCategoryDragListener mOnCategoryDragListener;

    public void setOnCategoryDragListener(OnCategoryDragListener listener) {
        mOnCategoryDragListener = listener;
    }

    @Override
    protected final View onCreateViewHere(@NonNull LayoutInflater inflater,
                                          @Nullable ViewGroup container,
                                          @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_category, container, false);
    }

    @Override
    protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreatedHere(view, savedInstanceState);
        mRecyclerView = findViewById(R.id.recyclerView);
    }

    /**
     * 在updateCategories调用之前调用
     */
    public void setMyLabel(String label) {
        mMyLabel = label;
    }

    /**
     * 在updateCategories调用之前调用
     */
    public void setMyTip(String tip) {
        mMyTip = tip;
    }

    /**
     * 在updateCategories调用之前调用
     */
    public void setRecommendLabel(String label) {
        mRecommendLabel = label;
    }

    public void updateCategories(List<Category> selected, List<Category> unselected) {
        mCategory.clear();
        processLogic(selected, unselected);
    }

    public ArrayList<Category> getSelectedCategories() {
        ArrayList<Category> selected = new ArrayList<>();
        for (Category category : mCategory) {
            if (category.itemType == Category.TYPE_MY_CATEGORY) {
                selected.add(category);
            }
        }
        return selected;
    }

    public ArrayList<Category> getUnSelectedCategories() {
        ArrayList<Category> unselected = new ArrayList<>();
        for (Category category : mCategory) {
            if (category.itemType == Category.TYPE_OTHER_CATEGORY) {
                unselected.add(category);
            }
        }
        return unselected;
    }

    private void setDataType(List<Category> datas, int type) {
        for (int i = 0; i < datas.size(); i++) {
            datas.get(i).setItemType(type);
        }
    }

    private void processLogic(List<Category> selected, List<Category> unselected) {
        mCategory.add(new Category(Category.TYPE_MY, mMyLabel, ""));
//        Bundle bundle = getArguments();
//        List<Category> selectedDatas = (List<Category>) bundle.getSerializable("DATA_SELECTED");
//        List<Category> unselectedDatas = (List<Category>) bundle.getSerializable("DATA_UNSELECTED");
        setDataType(selected, Category.TYPE_MY_CATEGORY);
        setDataType(unselected, Category.TYPE_OTHER_CATEGORY);

        mCategory.addAll(selected);
        mCategory.add(new Category(Category.TYPE_OTHER, mRecommendLabel, ""));
        mCategory.addAll(unselected);

        mAdapter = new CategoryAdapter(mCategory);
        mAdapter.setMyTip(mMyTip);
        GridLayoutManager manager = new GridLayoutManager(getActivity(), sSpanCount);
        mRecyclerView.setLayoutManager(manager);
        mRecyclerView.setAdapter(mAdapter);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                int itemViewType = mAdapter.getItemViewType(position);
                return itemViewType == Category.TYPE_MY_CATEGORY ||
                        itemViewType == Category.TYPE_OTHER_CATEGORY ? 1 : sSpanCount;
            }
        });
        ItemDragHelperCallBack callBack = new ItemDragHelperCallBack(dragListener);
        mHelper = new ItemTouchHelper(callBack);
        mAdapter.setOnCategoryDragListener(dragListener);
        //attachRecyclerView
        mHelper.attachToRecyclerView(mRecyclerView);
    }

    private OnCategoryDragListener dragListener = new OnCategoryDragListener() {
        @Override
        public void onStarDrag(BaseViewHolder baseViewHolder) {
            //开始拖动
//            Logger.d("BaseCategoryFragment", "开始拖动");
            mHelper.startDrag(baseViewHolder);
        }

        @Override
        public void onItemMove(int starPos, int endPos) {
//        if (starPos < 0||endPos<0) return;
            if (endPos == 1) {
                return;
            }
            onMove(starPos, endPos);
            //我的频道之间移动
            if (mOnCategoryDragListener != null) {
                //去除标题所占的一个index
                mOnCategoryDragListener.onItemMove(starPos - 1, endPos - 1);
            }
        }

        @Override
        public void onMoveToMyChannel(int starPos, int endPos) {
            //移动到我的频道
            onMove(starPos, endPos);
            if (mOnCategoryDragListener != null) {
                mOnCategoryDragListener.onMoveToMyChannel(
                        starPos - 1 - mAdapter.getMyChannelSize(), endPos - 1);
            }
        }

        @Override
        public void onMoveToOtherChannel(int starPos, int endPos) {
            //移动到推荐频道
            onMove(starPos, endPos);
            if (mOnCategoryDragListener != null) {
                mOnCategoryDragListener.onMoveToOtherChannel(
                        starPos - 1, endPos - 2 - mAdapter.getMyChannelSize());
            }
        }
    };

    private void onMove(int starPos, int endPos) {
        Category startChannel = mCategory.get(starPos);
        //先删除之前的位置
        mCategory.remove(starPos);
        //添加到现在的位置
        mCategory.add(endPos, startChannel);
        mAdapter.notifyItemMoved(starPos, endPos);
    }
}
