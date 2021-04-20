package com.scliang.core.demo;

import android.os.Bundle;

import com.scliang.core.base.BaseActivity;
import com.scliang.core.base.BaseFragment;
import com.scliang.core.base.Logger;
import com.scliang.core.base.NUtils;

public class DemoActivity extends BaseActivity {

//  @Override
//  protected boolean useDrawerLayout() {
//    return true;
//  }

  @Override
  protected BaseFragment setupRootFragment(Bundle savedInstanceState) {
//        return new DemoCategoryListFragment();
//        return new DemoCameraFragment();
//        return new DemoVoiceFragment();
//        return new DemoVideoFragment();
//        return new DemoAudioFragment();
//        return new DemoWebFragment();
        return new DemoFragment();
//        return new DemoHighlightTextLineFragment();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_demo);
//        setStatusBarColor(0xffffffff);
//    setToolbarMenu(R.menu.menu_help_label);

//        // ble
//        BLE.getInstance().open();
//        BLE.getInstance().startScan();

    // Core
    Logger.d("JNI-Core", NUtils.getNativeVersion());
  }

//    @Override
//    public void onRequestPermissionsResult(String permission, boolean granted) {
//        if (Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission)) {
//            // ble
//            BLE.getInstance().open();
//            BLE.getInstance().startScan();
//        }
//    }

  @Override
  protected void onDestroy() {
//        // ble
//        BLE.getInstance().stopScan();
    super.onDestroy();
  }

  @Override
  protected void onMenuItemClick(int groupId, int itemId) {
    if (itemId == R.id.menu_id_help) {
//            //需要裁剪的图片路径
//            Uri sourceUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() , "111.jpg"));
//            //裁剪完毕的图片存放路径
//            Uri destinationUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory() , "111_2.jpg"));
//            UCrop.Options options = new UCrop.Options();
//            options.setHideBottomControls(true);
//            options.setRootViewBackgroundColor(0xffffffff);
//            options.withAspectRatio(16, 9);
//            UCrop.of(DemoImageCropActivity.class, sourceUri, destinationUri)
//                    .withOptions(options)
//                    .start(this);
      startFragment(DemoBLEListFragment.class);
    }
  }
}
