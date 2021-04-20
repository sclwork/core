package com.scliang.core.media.image;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.scliang.core.R;
import com.scliang.core.base.BaseActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ClassPhotoAdapter extends CommonAdapter<String> {
    private Activity mContext;
    private ClassPhotoSelectListener mListener;
    /**
     * 用户选择的图片集合，存储为图片的完整路径
     */
    public ArrayList<String> mSelectedImage = new ArrayList<>();

    /**
     * 文件夹路径
     */
    private String mDirPath;

    private boolean isSelectOnce = false;
    private int mSelectMaxCount = 9;

    public ClassPhotoAdapter(Activity context, List<String> mDatas, List<String> mSelected, int itemLayoutId,
                             String dirPath, boolean selectOnce, int selectMaxCount, ClassPhotoSelectListener listener) {
        super(context, mDatas, itemLayoutId);
        mContext = context;
        this.mListener = listener;
        this.mDirPath = dirPath;
        isSelectOnce = selectOnce;
        mSelectMaxCount = selectMaxCount;
        mSelectedImage.addAll(mSelected);
    }

    @Override
    public void convert(final PhotoViewHolder helper, final String item) {
        //设置no_pic
        helper.setImageResource(R.id.id_item_image, R.drawable.pictures_no);
        //设置no_selected
        helper.setImageResource(R.id.id_item_select, R.drawable.picture_unselected);
        //设置图片
        helper.setImageByUrl(R.id.id_item_image, mDirPath + "/" + item);

        final ImageView mImageView = helper.getView(R.id.id_item_image);
        final ImageView mSelect = helper.getView(R.id.id_item_select);

        mImageView.setColorFilter(null);
        //设置ImageView的点击事件
        mImageView.setOnClickListener(new OnClickListener() {
            //选择，则将图片变暗，反之则反之
            @Override
            public void onClick(View v) {

                String imgurl = mDirPath + "/" + item;
                // 已经选择过该图片
                if (checkimgBean(imgurl, 0)) {
                    mSelect.setImageResource(R.drawable.picture_unselected);
                    mImageView.setColorFilter(null);
                } else {// 未选择该图片
                    if (mSelectedImage.size() > (mSelectMaxCount - 1)) {
                        ((BaseActivity)mContext).toast(mContext.getString(R.string.media_select_max_count, mSelectMaxCount));
                        return;
                    }
                    mSelectedImage.add(imgurl);
                    mSelect.setImageResource(R.drawable.pictures_selected);
                    mImageView.setColorFilter(Color.parseColor("#77000000"));
                }
                mListener.onPhotoSelect();

            }
        });

        boolean selected = mSelectedImage.contains(item);
        mSelect.setImageResource(selected ? R.drawable.pictures_selected : R.drawable.picture_unselected);


        /**
         * 已经选择过的图片，显示出选择过的效果
         */
        if (checkimgBean(mDirPath + "/" + item, -1)) {
            mSelect.setImageResource(R.drawable.pictures_selected);
            mImageView.setColorFilter(Color.parseColor("#77000000"));
        }
//		else{
//			mSelect.setVisibility(View.INVISIBLE);
//		}

        mSelect.setVisibility(isSelectOnce ? View.INVISIBLE : View.VISIBLE);
    }


    public void deleteFile(String audioPath) {
        File file = new File(audioPath);
        if (file.exists()) {
            file.delete();
        }
    }

    public boolean checkimgBean(String url, int tag) {
        for (int i = 0; i < mSelectedImage.size(); i++) {
            String imgurl = mSelectedImage.get(i);
            if (url.equals(imgurl)) {
                if (tag == 0) {
//                    deleteFile(iab.getAudioPath());
                    mSelectedImage.remove(i);
                }
                return true;
            }
        }
        return false;
    }
}
