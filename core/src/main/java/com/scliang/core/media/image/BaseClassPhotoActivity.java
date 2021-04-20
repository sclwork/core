package com.scliang.core.media.image;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.scliang.core.R;
import com.scliang.core.base.BaseActivity;

import org.json.JSONArray;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public abstract class BaseClassPhotoActivity extends BaseActivity
        implements ListImageDirPopupWindow.OnImageDirSelected, ClassPhotoSelectListener {
    /**
     * 返回选择相册
     */
    public static final int GO_BACK_PIC = 5000;
    /**
     * 返回选择相册是否初始化时间
     */
    public static final int TAG_INIT_TIME = 10000;

    public static Activity activity;
    /**
     * 存储文件夹中的图片数量
     */
    private int mPicsSize;
    /**
     * 图片数量最多的文件夹
     */
    private File mImgDir;
    /**
     * 所有的图片
     */
    private List<String> mImgs;
    private ArrayList<String> mSelectImages = new ArrayList<>();

    private GridView mGirdView;
    private ClassPhotoAdapter mAdapter;
    /**
     * 临时的辅助类，用于防止同一个文件夹的多次扫描
     */
    private HashSet<String> mDirPaths = new HashSet<>();

    /**
     * 扫描拿到所有的图片文件夹
     */
    private List<ImageFloder> mImageFloders = new ArrayList<>();

    private TextView mBtnCancel;
    private RelativeLayout mBottomLy;

    private TextView mChooseDir;
    private TextView mImageCount;
    int totalCount = 0;
    private int mScreenHeight;

    private boolean isSelectOnce = false;
    private int mSelectMaxCount = 1;

    private ListImageDirPopupWindow mListImageDirPopupWindow;
    private TextView mBtnCompleted;
    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            // 为View绑定数据
            data2View();
            // 初始化展示文件夹的popupWindw
            initListDirPopupWindow();
        }
    };

    private FilenameFilter photoFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String filename) {
            if (filename.endsWith(".jpg")
                    || filename.endsWith(".png")
                    || filename.endsWith(".jpeg"))
                return true;
            return false;
        }
    };

    /**
     * 为View绑定数据
     */
    private void data2View() {
        if (mImgDir == null) {
            toast("抱歉，没有找到可用的图片");
            return;
        }

        mImgs = Arrays.asList(mImgDir.list(photoFilter));
        mAdapter = new ClassPhotoAdapter(BaseClassPhotoActivity.this, mImgs, mSelectImages,
                R.layout.view_grid_item,
                mImgDir.getAbsolutePath(), isSelectOnce, mSelectMaxCount, this);
        mGirdView.setAdapter(mAdapter);
        mImageCount.setText(String.format(Locale.CHINESE, "%d 张", totalCount));
        onPhotoSelect();
    }

    /**
     * 初始化展示文件夹的popupWindow
     */
    private void initListDirPopupWindow() {
        mListImageDirPopupWindow = new ListImageDirPopupWindow(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                mImageFloders, LayoutInflater.from(getApplicationContext())
                .inflate(R.layout.view_list_dir, getRootContainer(), false));

        mListImageDirPopupWindow.setOnDismissListener(new OnDismissListener() {

            @Override
            public void onDismiss() {
                // 设置背景颜色变暗
                WindowManager.LayoutParams lp = getWindow().getAttributes();
                lp.alpha = 1.0f;
                getWindow().setAttributes(lp);
            }
        });
        // 设置选择文件夹的回调
        mListImageDirPopupWindow.setOnImageDirSelected(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setToolbarType(ToolbarType.HIDE);
        setContentView(R.layout.activity_class_photo);
//        View container = findViewById(R.id.class_photo_container);
//        if (container != null) {
//            container.setPadding(0, getStatusBarHeight(), 0, 0);
//        }
        activity = BaseClassPhotoActivity.this;
        DisplayMetrics outMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        mScreenHeight = outMetrics.heightPixels;

        isSelectOnce = getIntent().getBooleanExtra("SelectOnce", false);
        mSelectMaxCount = getIntent().getIntExtra("SelectMaxCount", 1);

        ArrayList<String> selectImages = getIntent().getStringArrayListExtra("SelectedImages");
        if (selectImages != null && selectImages.size() > 0) {
            mSelectImages.addAll(selectImages);
        }

        requestPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        initView();
        getImages();
        initEvent();

    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    public static void requestPermission(Activity activity, String permission) {
        if (ContextCompat.checkSelfPermission(activity, permission)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, 0);
        }
    }

    private void getImages() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String firstImage = null;
                Uri mImageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver mContentResolver = BaseClassPhotoActivity.this.getContentResolver();
                Cursor mCursor = mContentResolver.query(mImageUri, null, null, null, null);
                if (mCursor != null) {
                    while (mCursor.moveToNext()) {
                        // 获取图片的路径
                        String path = mCursor.getString(mCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                        // 拿到第一张图片的路径
                        if (firstImage == null)
                            firstImage = path;
                        // 获取该图片的父路径名
                        File parentFile = new File(path).getParentFile();
                        if (parentFile == null)
                            continue;
                        String dirPath = parentFile.getAbsolutePath();
                        ImageFloder imageFloder = null;
                        // 利用一个HashSet防止多次扫描同一个文件夹（不加这个判断，图片多起来还是相当恐怖的~~）
                        if (mDirPaths.contains(dirPath)) {
                            continue;
                        } else {
                            mDirPaths.add(dirPath);
                            // 初始化imageFloder
                            imageFloder = new ImageFloder();
                            imageFloder.setDir(dirPath);
                            imageFloder.setFirstImagePath(path);
                        }

                        String[] fileList = parentFile.list(photoFilter);
                        int picSize = 0;
                        if (fileList != null && fileList.length > 0) {
                            picSize = fileList.length;
                        }

                        totalCount += picSize;

                        imageFloder.setCount(picSize);
                        mImageFloders.add(imageFloder);

                        if (picSize > mPicsSize) {
                            mPicsSize = picSize;
                            mImgDir = parentFile;
                        }
                    }
                    mCursor.close();
                }

                // 扫描完成，辅助的HashSet也就可以释放内存了
                mDirPaths = null;

                // 通知Handler扫描图片完成
                mHandler.sendEmptyMessage(1);

            }
        }).start();

    }

    private void initView() {
        mGirdView = findViewById(R.id.id_gridView);
        mChooseDir = findViewById(R.id.id_choose_dir);
        mImageCount = findViewById(R.id.id_total_count);
        mBtnCancel = findViewById(R.id.classphotoactivity_btn_cancel);
        mBottomLy = findViewById(R.id.id_bottom_ly);
        mBtnCompleted = findViewById(R.id.classphotoactivity_btn_startrecorde);
        if (isSelectOnce) {
            setToolbarType(ToolbarType.TOP);
            mBtnCancel.setVisibility(View.INVISIBLE);
            mBtnCompleted.setVisibility(View.INVISIBLE);
            showToolbarNavigationIcon();
            View container = findViewById(R.id.class_photo_container);
            if (container != null && container instanceof ViewGroup) {
                ((ViewGroup)container).removeView(mBottomLy);
                setToolbarCenterCustomView(mBottomLy);
            }
        } else {
            setToolbarType(ToolbarType.HIDE);
            mBtnCancel.setVisibility(View.VISIBLE);
            mBtnCompleted.setVisibility(View.VISIBLE);
            hideToolbarNavigationIcon();
        }
    }

    private void initEvent() {
        mBtnCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        mBottomLy.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListImageDirPopupWindow == null) {
                    return;
                }

                mListImageDirPopupWindow.setAnimationStyle(R.anim.anim_push_popwindow_out);
                mListImageDirPopupWindow.showAsDropDown(mBottomLy);
            }
        });

        mBtnCompleted.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                clickComplete();
            }
        });
    }

    private void clickComplete() {
        ArrayList<String> selecteds = mAdapter.mSelectedImage;
        if (selecteds != null) {
            JSONArray array = new JSONArray();
            for (int i = 0; i < selecteds.size(); i++) {
                String t = selecteds.get(i);
                array.put(t);
            }
            Bundle args = new Bundle();
            args.putString("SelectedImages", array.toString());
            Intent data = new Intent();
            data.putExtras(args);
            setResult(RESULT_OK, data);
            finish();
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    @Override
    public void selected(ImageFloder floder) {
        mImgDir = new File(floder.getDir());
        mImgs = Arrays.asList(mImgDir.list(photoFilter));
        mAdapter = new ClassPhotoAdapter(BaseClassPhotoActivity.this, mImgs, mSelectImages,
                R.layout.view_grid_item, mImgDir.getAbsolutePath(), isSelectOnce, mSelectMaxCount, this);
        mGirdView.setAdapter(mAdapter);
        mImageCount.setText(String.format(Locale.CHINESE, "%d 张", floder.getCount()));
        mChooseDir.setText(floder.getName().substring(1));
        if (mListImageDirPopupWindow != null) {
            mListImageDirPopupWindow.dismiss();
        }
        onPhotoSelect();
    }

    @Override
    public void onPhotoSelect() {
        if (isSelectOnce) {
            ArrayList<String> selecteds = mAdapter.mSelectedImage;
            if (selecteds.size() > 0) {
                clickComplete();
            }
        } else {
            if (mAdapter != null) {
                updateCompletedText();
            }
        }
    }

    @Override
    public void onPhotoDelete(String path) {
        if (mAdapter != null) {
            mAdapter.checkimgBean(path, 0);
            mAdapter.notifyDataSetChanged();
        }
        updateCompletedText();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == GO_BACK_PIC) {
            ArrayList<String> beanArrayList = (ArrayList<String>) data.getSerializableExtra("photoList");
            int tag = data.getIntExtra("TAG", -1);

            if (tag == TAG_INIT_TIME) {
                if (mAdapter != null) {
                    mAdapter.mSelectedImage = beanArrayList;
                }
            } else {
                if (mAdapter != null) {
                    mAdapter.mSelectedImage.clear();
                }
                for (int i = 0; i < beanArrayList.size(); i++) {
                    String iab = beanArrayList.get(i);
                    if (mAdapter != null) {
                        mAdapter.mSelectedImage.add(iab);
                    }
                }
            }

            if (mAdapter != null) {
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.mSelectedImage.clear();
        }
    }

    private void updateCompletedText() {
        if (mAdapter.mSelectedImage.size() <= 0) {
            mBtnCompleted.setText(R.string.media_ok_t1);
        } else {
            mBtnCompleted.setText(getString(R.string.media_ok_t2,
                    String.format(Locale.CHINESE, "%d/%d", mAdapter.mSelectedImage.size(), mSelectMaxCount)));
        }
    }
}
