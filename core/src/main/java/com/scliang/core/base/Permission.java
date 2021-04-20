package com.scliang.core.base;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import androidx.core.content.ContextCompat;

import java.util.List;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2017/9/29.
 *
 * android.permission-group.CALENDAR
 *      android.permission.READ_CALENDAR
 *      android.permission.WRITE_CALENDAR
 *
 * android.permission-group.CAMERA
 *      android.permission.CAMERA
 *
 * android.permission-group.CONTACTS
 *      android.permission.READ_CONTACTS
 *      android.permission.WRITE_CONTACTS
 *
 * android.permission-group.LOCATION
 *      android.permission.ACCESS_FINE_LOCATION
 *      android.permission.ACCESS_COARSE_LOCATION
 *
 * android.permission-group.MICROPHONE
 *      android.permission.RECORD_AUDIO
 *
 * android.permission-group.PHONE
 *      android.permission.READ_PHONE_STATE
 *      android.permission.CALL_PHONE
 *      android.permission.READ_CALL_LOG
 *      android.permission.WRITE_CALL_LOG
 *      android.permission.USE_SIP
 *      android.permission.PROCESS_OUTGOING_CALLS
 *      com.android.voicemail.permission.ADD_VOICEMAIL
 *
 * android.permission-group.SENSORS
 *      android.permission.BODY_SENSORS
 *
 * android.permission-group.SMS
 *      android.permission.SEND_SMS
 *      android.permission.READ_SMS
 *      android.permission.RECEIVE_SMS
 *      android.permission.RECEIVE_MMS
 *      android.permission.RECEIVE_WAP_PUSH
 *
 * android.permission-group.STORAGE
 *      android.permission.READ_EXTERNAL_STORAGE
 *      android.permission.WRITE_EXTERNAL_STORAGE
 *
 */
public class Permission {
    private Permission() {}

    /**
     * 判断是否拥有读写日历的权限
     */
    public static boolean hasCalendarPermission(OnCheckPermissionsListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = BaseApplication.getApp();
            int res = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_CALENDAR);
            boolean has = res == PackageManager.PERMISSION_GRANTED;
            if (!has && listener != null) {
                listener.onNoPermission(Manifest.permission.WRITE_CALENDAR);
            }
            return has;
        } else {
            return true;
        }
    }

    /**
     * 判断是否拥有使用摄像头的权限
     */
    public static boolean hasCameraPermission(OnCheckPermissionsListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = BaseApplication.getApp();
            int res = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.CAMERA);
          boolean has = res == PackageManager.PERMISSION_GRANTED;
          if (!has && listener != null) {
            listener.onNoPermission(Manifest.permission.CAMERA);
          }
          return has;
        } else {
            try {
                Camera camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                List<Camera.Size> previewSizes = camera.getParameters().getSupportedPreviewSizes();
                return previewSizes != null && previewSizes.size() > 0;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * 判断是否拥有读写通讯录的权限
     */
    public static boolean hasContactsPermission(OnCheckPermissionsListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = BaseApplication.getApp();
            int res = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_CONTACTS);
          boolean has = res == PackageManager.PERMISSION_GRANTED;
          if (!has && listener != null) {
            listener.onNoPermission(Manifest.permission.WRITE_CONTACTS);
          }
          return has;
        } else {
            return true;
        }
    }

    /**
     * 判断是否拥有使用地理位置的权限
     */
    public static boolean hasLocationPermission(OnCheckPermissionsListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = BaseApplication.getApp();
            int res = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION);
          boolean has = res == PackageManager.PERMISSION_GRANTED;
          if (!has && listener != null) {
            listener.onNoPermission(Manifest.permission.ACCESS_FINE_LOCATION);
          }
          return has;
        } else {
            return true;
        }
    }

    /**
     * 判断是否拥有使用录音的权限
     */
    public static boolean hasRecorderPermission(OnCheckPermissionsListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = BaseApplication.getApp();
            int res = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.RECORD_AUDIO);
          boolean has = res == PackageManager.PERMISSION_GRANTED;
          if (!has && listener != null) {
            listener.onNoPermission(Manifest.permission.RECORD_AUDIO);
          }
          return has;
        } else {
            return true;
        }
    }

    /**
     * 判断是否拥有读取电话状态的权限
     */
    public static boolean hasReadPhoneStatePermission(OnCheckPermissionsListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = BaseApplication.getApp();
            int res = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_PHONE_STATE);
          boolean has = res == PackageManager.PERMISSION_GRANTED;
          if (!has && listener != null) {
            listener.onNoPermission(Manifest.permission.READ_PHONE_STATE);
          }
          return has;
        } else {
            return true;
        }
    }

    /**
     * 判断是否拥有拨打电话的权限
     */
    public static boolean hasCallPhonePermission(OnCheckPermissionsListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = BaseApplication.getApp();
            int res = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.CALL_PHONE);
          boolean has = res == PackageManager.PERMISSION_GRANTED;
          if (!has && listener != null) {
            listener.onNoPermission(Manifest.permission.CALL_PHONE);
          }
          return has;
        } else {
            return true;
        }
    }

    /**
     * 判断是否拥有使用生命体征传感器的权限
     */
    public static boolean hasBodySensorsPermission(OnCheckPermissionsListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = BaseApplication.getApp();
            int res = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.BODY_SENSORS);
          boolean has = res == PackageManager.PERMISSION_GRANTED;
          if (!has && listener != null) {
            listener.onNoPermission(Manifest.permission.BODY_SENSORS);
          }
          return has;
        } else {
            return true;
        }
    }

    /**
     * 判断是否拥有读写短信的权限
     */
    public static boolean hasSmsPermission(OnCheckPermissionsListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = BaseApplication.getApp();
            int res = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.READ_SMS);
          boolean has = res == PackageManager.PERMISSION_GRANTED;
          if (!has && listener != null) {
            listener.onNoPermission(Manifest.permission.READ_SMS);
          }
          return has;
        } else {
            return true;
        }
    }

    /**
     * 判断是否拥有读写SDCard的权限
     */
    public static boolean hasStoragePermission(OnCheckPermissionsListener listener) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Context context = BaseApplication.getApp();
            int res = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
          boolean has = res == PackageManager.PERMISSION_GRANTED;
          if (!has && listener != null) {
            listener.onNoPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
          }
          return has;
        } else {
            return true;
        }
    }
}
