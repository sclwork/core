package com.scliang.core.base;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.TextUtils;

import com.scliang.core.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.ref.SoftReference;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2019/2/20.
 **/
public class OnCheckPermissionImpl implements OnCheckPermissionsListener {
  private SharedPreferences mSP;
  private SoftReference<BaseActivity> mActivate;

  public OnCheckPermissionImpl() {
  }

  public OnCheckPermissionImpl(BaseActivity activity) {
    mActivate = new SoftReference<>(activity);
    if (activity != null) {
      mSP = activity.getSharedPreferences("OnCheckPermission", Context.MODE_PRIVATE);
    }
  }

  public OnCheckPermissionImpl(BaseFragment fragment) {
    mActivate = new SoftReference<>(fragment == null ? null : (BaseActivity) fragment.getActivity());
    Activity activity = fragment == null ? null : fragment.getActivity();
    if (activity != null) {
      mSP = activity.getSharedPreferences("OnCheckPermission", Context.MODE_PRIVATE);
    }
  }

  /**
   * 打开App系统设置页面
   * @param text 提示文案
   */
  public static void showToSettingConfirmDialog(BaseFragment fragment, CharSequence text) {
    if (fragment != null) {
      showToSettingConfirmDialog((BaseActivity) fragment.getActivity(), text);
    }
  }

  /**
   * 打开App系统设置页面
   * @param text 提示文案
   */
  public static void showToSettingConfirmDialog(BaseActivity activity, CharSequence text) {
    if (activity != null) {
      activity.showToSettingConfirmDialog(text);
    }
  }

  @Override
  public void onNoPermission(String permission) {
    BaseActivity activity = mActivate == null ? null : mActivate.get();
    if (activity != null) {
      if (Manifest.permission.RECORD_AUDIO.equals(permission)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          activity.requestPermission(permission);
          boolean isFirst = !checkPermissionRequested(permission);
          if (!isFirst && !activity.shouldShowRequestPermissionRationale(permission)) {
            showToSettingConfirmDialog(activity,
                activity.getString(R.string.no_permission_audio_record));
          }
          if (isFirst) {
            appendPermissionRequestedToSP(permission);
          }
        }
      } else if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          activity.requestPermission(permission);
          boolean isFirst = !checkPermissionRequested(permission);
          if (!isFirst && !activity.shouldShowRequestPermissionRationale(permission)) {
            showToSettingConfirmDialog(activity,
                activity.getString(R.string.no_permission_external_storage));
          }
          if (isFirst) {
            appendPermissionRequestedToSP(permission);
          }
        }
      } else if (Manifest.permission.CAMERA.equals(permission)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          activity.requestPermission(permission);
          boolean isFirst = !checkPermissionRequested(permission);
          if (!isFirst && !activity.shouldShowRequestPermissionRationale(permission)) {
            showToSettingConfirmDialog(activity,
                activity.getString(R.string.no_permission_camera));
          }
          if (isFirst) {
            appendPermissionRequestedToSP(permission);
          }
        }
      } else if (Manifest.permission.READ_PHONE_STATE.equals(permission)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          activity.requestPermission(permission);
          boolean isFirst = !checkPermissionRequested(permission);
          if (!isFirst && !activity.shouldShowRequestPermissionRationale(permission)) {
            showToSettingConfirmDialog(activity,
                activity.getString(R.string.no_permission_read_phone_state));
          }
          if (isFirst) {
            appendPermissionRequestedToSP(permission);
          }
        }
      } else if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          activity.requestPermission(permission);
          boolean isFirst = !checkPermissionRequested(permission);
          if (!isFirst && !activity.shouldShowRequestPermissionRationale(permission)) {
            showToSettingConfirmDialog(activity,
                activity.getString(R.string.no_permission_location));
          }
          if (isFirst) {
            appendPermissionRequestedToSP(permission);
          }
        }
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          activity.requestPermission(permission);
          boolean isFirst = !checkPermissionRequested(permission);
          if (!isFirst && !activity.shouldShowRequestPermissionRationale(permission)) {
            showToSettingConfirmDialog(activity,
                activity.getString(R.string.no_permission_default));
          }
          if (isFirst) {
            appendPermissionRequestedToSP(permission);
          }
        }
      }
    }
  }

  private boolean checkPermissionRequested(String permission) {
    if (TextUtils.isEmpty(permission)) {
      return false;
    } else {
      try {
        JSONArray array = new JSONArray(mSP == null ? "[]" :
            mSP.getString("Permissions", "[]"));
        return array.toString().contains(permission);
      } catch (JSONException e) {
        return false;
      }
    }
  }

  private void appendPermissionRequestedToSP(String permission) {
    if (!checkPermissionRequested(permission) && mSP != null) {
      try {
        JSONArray array = new JSONArray(mSP.getString("Permissions", "[]"));
        array.put(permission);
        mSP.edit().putString("Permissions", array.toString()).apply();
      } catch (JSONException ignored) {
      }
    }
  }
}
