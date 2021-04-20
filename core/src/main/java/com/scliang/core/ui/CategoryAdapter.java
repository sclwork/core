package com.scliang.core.ui;

import android.graphics.Bitmap;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.scliang.core.R;

import java.util.List;
import java.util.Locale;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/11/7.
 */
public class CategoryAdapter extends BaseMultiItemQuickAdapter<Category, BaseViewHolder> {
//    private BaseViewHolder mEditViewHolder;
    private boolean mIsEdit;
    private long startTime;
    // touch 间隔时间  用于分辨是否是 "点击"
    private static final long SPACE_TIME = 100;
    private RecyclerView mRecyclerView;
    private String mMyTip = "";

    public CategoryAdapter(List<Category> data) {
        super(data);
        mIsEdit = true;
        addItemType(Category.TYPE_MY, R.layout.view_item_category_title);
        addItemType(Category.TYPE_MY_CATEGORY, R.layout.view_item_category);
        addItemType(Category.TYPE_OTHER, R.layout.view_item_category_title);
        addItemType(Category.TYPE_OTHER_CATEGORY, R.layout.view_item_category);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        mRecyclerView = (RecyclerView) parent;
        return super.onCreateViewHolder(parent, viewType);
    }

    private OnCategoryDragListener onCategoryDragListener;

    public void setOnCategoryDragListener(OnCategoryDragListener listener) {
        onCategoryDragListener = listener;
    }

    public void setMyTip(String tip) {
        mMyTip = tip;
    }

    @Override
    protected void convert(final BaseViewHolder baseViewHolder, final Category category) {
        final boolean isAll = "全部".equals(category.title);
        switch (baseViewHolder.getItemViewType()) {
            case Category.TYPE_MY:
                //我的频道
//                //赋值，以便之后修改文字
//                mEditViewHolder = baseViewHolder;
                baseViewHolder.setText(R.id.tvTitle, category.title);
                baseViewHolder.setText(R.id.tvEdit, mMyTip);
//                        .setOnClickListener(R.id.tvEdit, new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                if (!mIsEdit) {
//                                    startEditMode(true);
//                                    baseViewHolder.setText(R.id.tvEdit, "完成");
//                                } else {
//                                    startEditMode(false);
//                                    baseViewHolder.setText(R.id.tvEdit, "编辑");
//                                }
//                            }
//                        });
                break;
            case Category.TYPE_OTHER:
                //频道推荐
                baseViewHolder.setText(R.id.tvTitle, category.title)
                        .setVisible(R.id.tvEdit, false).setVisible(R.id.vTop, true);
                break;
            case Category.TYPE_MY_CATEGORY:
                //我的频道列表
                baseViewHolder
                        .setVisible(R.id.ivDelete, mIsEdit && !isAll)//编辑模式就显示删除按钮
                        .setOnLongClickListener(R.id.rlItemView, new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
//                                if (!mIsEdit) {
//                                    //开启编辑模式
//                                    startEditMode(true);
//                                    mEditViewHolder.setText(R.id.tvEdit, "完成");
//                                }
                                if (!isAll && onCategoryDragListener != null) {
                                    onCategoryDragListener.onStarDrag(baseViewHolder);
                                }
                                return true;
                            }
                        }).setOnTouchListener(R.id.tvCategory, new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (!mIsEdit) return false;//正常模式无需监听触摸
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                startTime = System.currentTimeMillis();
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if (System.currentTimeMillis() - startTime > SPACE_TIME) {
                                    //当MOVE事件与DOWN事件的触发的间隔时间大于100ms时，则认为是拖拽starDrag
                                    if (onCategoryDragListener != null) {
                                        onCategoryDragListener.onStarDrag(baseViewHolder);
                                    }
                                }
                                break;
                            case MotionEvent.ACTION_CANCEL:
                            case MotionEvent.ACTION_UP:
                                startTime = 0;
                                break;
                        }
                        return false;
                    }
                }).getView(R.id.ivDelete).setTag(true);//在我的频道里面设置true标示，之后会根据这个标示来判断编辑模式是否显示
                baseViewHolder.setText(R.id.tvCategory,
                        category.count >= 0 ?
                                Html.fromHtml(String.format(Locale.CHINESE,
                                        "%s &nbsp <font color='#666666'>%d</font>",
                                        category.title, category.count)) : category.title)
                        .setOnClickListener(R.id.ivDelete, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //执行删除，移动到推荐频道列表
                        if (mIsEdit && !isAll) {
                            int otherFirstPosition = getOtherFirstPosition();
                            int currentPosition = getViewHolderPosition(baseViewHolder);
                            //获取到目标View
                            View targetView = mRecyclerView.getLayoutManager().findViewByPosition(otherFirstPosition);
                            //获取当前需要移动的View
                            View currentView = mRecyclerView.getLayoutManager().findViewByPosition(currentPosition);
                            // 如果targetView不在屏幕内,则indexOfChild为-1  此时不需要添加动画,因为此时notifyItemMoved自带一个向目标移动的动画
                            // 如果在屏幕内,则添加一个位移动画
                            if (mRecyclerView.indexOfChild(targetView) >= 0 && otherFirstPosition != -1) {
                                RecyclerView.LayoutManager manager = mRecyclerView.getLayoutManager();
                                int spanCount = ((GridLayoutManager) manager).getSpanCount();
                                int targetX = targetView.getLeft();
                                int targetY = targetView.getTop();
                                int myChannelSize = getMyChannelSize();//这里我是为了偷懒 ，算出来我的频道的大小
                                if (myChannelSize % spanCount == 1) {
                                    //我的频道最后一行 之后一个，移动后
                                    targetY -= targetView.getHeight();
                                }

                                //我的频道 移动到 推荐频道的第一个
                                category.setItemType(Category.TYPE_OTHER_CATEGORY);//改为推荐频道类型

                                if (onCategoryDragListener != null) {
                                    onCategoryDragListener.onMoveToOtherChannel(currentPosition, otherFirstPosition - 1);
                                }
                                startAnimation(currentView, targetX, targetY);
                            } else {
                                category.setItemType(Category.TYPE_OTHER_CATEGORY);//改为推荐频道类型
                                if (otherFirstPosition == -1) otherFirstPosition = mData.size();
                                if (onCategoryDragListener != null) {
                                    onCategoryDragListener.onMoveToOtherChannel(currentPosition, otherFirstPosition - 1);
                                }
                            }
                        }
                    }
                });
                break;
            case Category.TYPE_OTHER_CATEGORY:
                //频道推荐列表
                baseViewHolder.setText(R.id.tvCategory,
                        category.count >= 0 ?
                                Html.fromHtml(String.format(Locale.CHINESE,
                                        "%s &nbsp <font color='#666666'>%d</font>",
                                        category.title, category.count)) : category.title)
                        .setVisible(R.id.ivDelete, false).setVisible(R.id.ivAdd, true)
                        .setOnClickListener(R.id.tvCategory, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (mIsEdit) {
                                    int myLastPosition = getMyLastPosition();
                                    int currentPosition = getViewHolderPosition(baseViewHolder);
                                    //获取到目标View
                                    View targetView = mRecyclerView.getLayoutManager().findViewByPosition(myLastPosition);
                                    //获取当前需要移动的View
                                    View currentView = mRecyclerView.getLayoutManager().findViewByPosition(currentPosition);
                                    // 如果targetView不在屏幕内,则indexOfChild为-1  此时不需要添加动画,因为此时notifyItemMoved自带一个向目标移动的动画
                                    // 如果在屏幕内,则添加一个位移动画
                                    if (mRecyclerView.indexOfChild(targetView) >= 0 && myLastPosition != -1) {
                                        RecyclerView.LayoutManager manager = mRecyclerView.getLayoutManager();
                                        int spanCount = ((GridLayoutManager) manager).getSpanCount();
                                        int targetX = targetView.getLeft() + targetView.getWidth();
                                        int targetY = targetView.getTop();

                                        int myChannelSize = getMyChannelSize();//这里我是为了偷懒 ，算出来我的频道的大小
                                        if (myChannelSize % spanCount == 0) {
                                            //添加到我的频道后会换行，所以找到倒数第4个的位置

                                            View lastFourthView = mRecyclerView.getLayoutManager().findViewByPosition(getMyLastPosition() - 3);
//                                        View lastFourthView = mRecyclerView.getChildAt(getMyLastPosition() - 3);
                                            targetX = lastFourthView == null ? 0 : lastFourthView.getLeft();
                                            targetY = lastFourthView == null ? 0 : (lastFourthView.getTop() + lastFourthView.getHeight());
                                        }


                                        // 推荐频道 移动到 我的频道的最后一个
                                        category.setItemType(Category.TYPE_MY_CATEGORY);//改为推荐频道类型
                                        if (onCategoryDragListener != null)
                                            onCategoryDragListener.onMoveToMyChannel(currentPosition, myLastPosition + 1);
                                        startAnimation(currentView, targetX, targetY);
                                    } else {
                                        category.setItemType(Category.TYPE_MY_CATEGORY);//改为推荐频道类型
                                        if (myLastPosition == -1) myLastPosition = 0;//我的频道没有了，改成0
                                        if (onCategoryDragListener != null)
                                            onCategoryDragListener.onMoveToMyChannel(currentPosition, myLastPosition + 1);
                                    }
                                }
                            }
                        });
                break;
        }
    }

    public int getMyChannelSize() {
        int size = 0;
        for (int i = 0; i < mData.size(); i++) {
            Category category = mData.get(i);
            if (category.getItemType() == Category.TYPE_MY_CATEGORY) {
                size++;
            }
        }
        return size;
    }

    private int getViewHolderPosition(RecyclerView.ViewHolder holder) {
        int pos = 0;
        for (int i = 0; i < getItemCount(); i++) {
            RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForAdapterPosition(i);
            if (viewHolder != null && holder.itemView == viewHolder.itemView) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    private void startAnimation(final View currentView, int targetX, int targetY) {
        final ViewGroup parent = (ViewGroup) mRecyclerView.getParent();
        final ImageView mirrorView = addMirrorView(parent, currentView);
        TranslateAnimation animator = getTranslateAnimator(targetX - currentView.getLeft(), targetY - currentView.getTop());
        currentView.setVisibility(View.INVISIBLE);//暂时隐藏
        mirrorView.startAnimation(animator);
        animator.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                parent.removeView(mirrorView);//删除添加的镜像View
                if (currentView.getVisibility() == View.INVISIBLE) {
                    currentView.setVisibility(View.VISIBLE);//显示隐藏的View
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    /**
     * 添加需要移动的 镜像View
     */
    private ImageView addMirrorView(ViewGroup parent, View view) {
        view.destroyDrawingCache();
        //首先开启Cache图片 ，然后调用view.getDrawingCache()就可以获取Cache图片
        view.setDrawingCacheEnabled(true);
        ImageView mirrorView = new ImageView(view.getContext());
        //获取该view的Cache图片
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        mirrorView.setImageBitmap(bitmap);
        //销毁掉cache图片
        view.setDrawingCacheEnabled(false);
        int[] locations = new int[2];
        view.getLocationOnScreen(locations);//获取当前View的坐标
        int[] parenLocations = new int[2];
        mRecyclerView.getLocationOnScreen(parenLocations);//获取RecyclerView所在坐标
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(bitmap.getWidth(), bitmap.getHeight());
        params.setMargins(locations[0], locations[1] - parenLocations[1], 0, 0);
        parent.addView(mirrorView, params);//在RecyclerView的Parent添加我们的镜像View，parent要是FrameLayout这样才可以放到那个坐标点
        return mirrorView;
    }

    private int ANIM_TIME = 360;

    /**
     * 获取位移动画
     */
    private TranslateAnimation getTranslateAnimator(float targetX, float targetY) {
        TranslateAnimation translateAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.ABSOLUTE, targetX,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.ABSOLUTE, targetY);
        // RecyclerView默认移动动画250ms 这里设置360ms 是为了防止在位移动画结束后 remove(view)过早 导致闪烁
        translateAnimation.setDuration(ANIM_TIME);
        translateAnimation.setFillAfter(true);
        return translateAnimation;
    }

    /**
     * 获取推荐频道列表的第一个position
     *
     * @return
     */
    private int getOtherFirstPosition() {
        //之前找到了第一个pos直接返回
//        if (mOtherFirstPosition != 0) return mOtherFirstPosition;
        for (int i = 0; i < mData.size(); i++) {
            Category category = mData.get(i);
            if (Category.TYPE_OTHER_CATEGORY == category.getItemType()) {
                //找到第一个直接返回
                return i;
            }
        }
        return -1;
    }

    /**
     * 我的频道最后一个的position
     *
     * @return
     */
    private int getMyLastPosition() {
        for (int i = mData.size() - 1; i > -1; i--) {
            Category category = mData.get(i);
            if (Category.TYPE_MY_CATEGORY == category.getItemType()) {
                //找到第一个直接返回
                return i;
            }
        }
        return -1;
    }

    /**
     * 开启编辑模式
     */
    private void startEditMode(boolean isEdit) {
        mIsEdit = isEdit;
        int visibleChildCount = mRecyclerView.getChildCount();
        for (int i = 0; i < visibleChildCount; i++) {
            View view = mRecyclerView.getChildAt(i);
            TextView categoryText = view.findViewById(R.id.tvCategory);
            String category = categoryText == null ? "" : categoryText.getText().toString();
            if ("全部".equals(category)) {
                continue;
            }
            ImageView imgEdit = view.findViewById(R.id.ivDelete);
            if (imgEdit != null) {
                boolean isVis = imgEdit.getTag() != null && (boolean) imgEdit.getTag();
                imgEdit.setVisibility(isVis && isEdit ? View.VISIBLE : View.INVISIBLE);
            }
        }
    }
}
