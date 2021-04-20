package com.scliang.core.demo;

import android.os.Bundle;

import com.scliang.core.media.image.ucrop.UCropActivity;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/9/30.
 */
public class DemoImageCropActivity extends UCropActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showToolbarNavigationIcon();
        setToolbarMenu(R.menu.menu_help_label);
    }

    @Override
    protected void onMenuItemClick(int groupId, int itemId) {
        if (itemId == R.id.menu_id_help) {
            cropAndSaveImage();
        }
    }
}
