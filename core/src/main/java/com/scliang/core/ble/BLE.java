package com.scliang.core.ble;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import androidx.core.content.ContextCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.scliang.core.base.BaseActivity;
import com.scliang.core.base.BaseApplication;
import com.scliang.core.base.Logger;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/11/14.
 */
public class BLE {
  private BLE() {
  }

  private static class SingletonHolder {
    private static final BLE INSTANCE = new BLE();
  }

  public static BLE getInstance() {
    return BLE.SingletonHolder.INSTANCE;
  }

  public static final int REQUEST_ENABLE_BT = 6089;

  private SoftReference<Context> mContext;
  private Handler mUIHandler;
  private Handler mOPHandler;
  private boolean isSupport;
  private BluetoothAdapter mAdapter;
  private Object mScanCallback;
  private List<BleDevice> mDevices = new ArrayList<>();
  private final List<SoftReference<OnBLEChangeListener>> mBLEChangeListeners = new ArrayList<>();
  private final List<String> mManualConnectDeviceAddresses = new ArrayList<>();

  /**
   * 使用ApplicationContext初始化BLE工具
   */
  public void init(BaseApplication application) {
    final Context context = application.getApplicationContext();
    mContext = new SoftReference<>(context);
    mUIHandler = new Handler(Looper.getMainLooper());
    HandlerThread thread = new HandlerThread("BLE-OP-" + System.currentTimeMillis());
    thread.start();
    mOPHandler = new Handler(thread.getLooper());

    isSupport = isSupport(context);

    if (isSupport) {
      mAdapter = BluetoothAdapter.getDefaultAdapter();
    } else {
      mAdapter = null;
    }

    registerStateReceiver();
  }

  public void registerBLEChangeListener(OnBLEChangeListener listener) {
    if (listener != null) {
      synchronized (mBLEChangeListeners) {
        mBLEChangeListeners.add(new SoftReference<>(listener));
      }
    }
  }

  public void unregisterBLEChangeListener(OnBLEChangeListener listener) {
    if (listener != null) {
      synchronized (mBLEChangeListeners) {
        List<SoftReference<OnBLEChangeListener>> delete = new ArrayList<>();
        for (SoftReference<OnBLEChangeListener> ref : mBLEChangeListeners) {
          if (ref != null) {
            OnBLEChangeListener lis = ref.get();
            if (lis == null || lis == listener) {
              delete.add(ref);
            }
          }
        }
        for (SoftReference<OnBLEChangeListener> ref : delete) {
          mBLEChangeListeners.remove(ref);
        }
      }
    }
  }

  public List<BleDevice> getBoundDevices() {
    List<BleDevice> devices = new ArrayList<>();
    if (mAdapter == null) {
      return devices;
    }

    Set<BluetoothDevice> boundDevices = mAdapter.getBondedDevices();
    for (BluetoothDevice d : boundDevices) {
      devices.add(new BleDevice(d));
    }
    return devices;
  }

  public void open(BaseActivity activity) {
    final Context context = mContext == null ? null : mContext.get();
    if (context == null) {
      return;
    }

    int res = ContextCompat.checkSelfPermission(context,
        Manifest.permission.ACCESS_COARSE_LOCATION);
    if (res != PackageManager.PERMISSION_GRANTED) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        BaseApplication.requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
      }
      return;
    }

    if (!isSupport) {
      return;
    }

    if (mAdapter == null) {
      return;
    }

    synchronized (BLE.class) {
      mDevices.clear();
    }
    releaseAllScanClient();

    if (mAdapter.isEnabled()) {
      return;
    }

//    if (!mAdapter.enable()) {
      if (activity != null) {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        activity.startActivityForResult(intent, REQUEST_ENABLE_BT);
      }
//    }
  }

  public void startScan() {
    if (!isSupport) {
      return;
    }

    if (mAdapter == null) {
      return;
    }

    if (!mAdapter.isEnabled()) {
      return;
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      if (mScanCallback == null) {
        mScanCallback = new ScanCallback() {
          @Override
          public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BleDevice device = new BleDevice(result.getDevice());
            if (!mDevices.contains(device)) {
              synchronized (BLE.class) {
                mDevices.add(device);
              }
              // 有Name的情况下才发起通知更新
              if (!TextUtils.isEmpty(device.getName())) {
                mOPHandler.post(new Runnable() {
                  @Override
                  public void run() {
                    // 排序
                    sortDevices();
                    // 通知更新
                    updateDeviceInfos();
                  }
                });
              }
            }
          }

          @Override
          public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
          }

          @Override
          public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
          }
        };
      }

      BluetoothLeScanner scanner = mAdapter.getBluetoothLeScanner();
      if (scanner != null) {
        scanner.startScan((ScanCallback) mScanCallback);
      }
    } else {
      mAdapter.startLeScan(mLeScanCallback);
    }
  }

  public void stopScan() {
    if (!isSupport) {
      return;
    }

    if (mAdapter == null) {
      return;
    }

    if (!mAdapter.isEnabled()) {
      return;
    }

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
      BluetoothLeScanner scanner = mAdapter.getBluetoothLeScanner();
      if (scanner != null) {
        scanner.stopScan((ScanCallback) mScanCallback);
      }
    } else {
      mAdapter.stopLeScan(mLeScanCallback);
    }
  }

  public List<BleDevice> getFoundDevices() {
    return mDevices;
  }

  public List<BleDevice> getShowableDevices() {
    List<BleDevice> devices = new ArrayList<>();
    synchronized (BLE.class) {
      for (BleDevice device : mDevices) {
        if (device != null &&
            (!TextUtils.isEmpty(device.getName()) ||
                device.getState() == BluetoothProfile.STATE_CONNECTED ||
                device.getGatt() != null)) {
          devices.add(device);
        }
      }
    }
    return devices;
  }

  public void connectDevice(BleDevice device) {
    connectDevice(device, false);
  }

  public void connectDevice(BleDevice device, boolean manual) {
    if (device == null || device.getDevice() == null) {
      return;
    }

    Context context = mContext == null ? null : mContext.get();
    if (context == null) {
      return;
    }

    if (manual) {
      final String address = device.getAddress();
      if (!TextUtils.isEmpty(address)) {
        mManualConnectDeviceAddresses.add(address);
        Bundle args = new Bundle();
        args.putString("DeviceAddress", address);
        BaseApplication.getApp().sendLocalBroadcast("BLEConnectDeviceStarted", args);
      }
    }

    BluetoothDevice bluetoothDevice = device.getDevice();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      bluetoothDevice.connectGatt(context, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
    } else {
      bluetoothDevice.connectGatt(context, false, mGattCallback);
    }
  }

  public static String getDeviceName(BluetoothDevice device) {
    if (device == null) {
      return "";
    }

    String name = "";

    try {
      @SuppressLint("PrivateApi")
      Method alias = BluetoothDevice.class.getDeclaredMethod("getAlias");
      if (alias != null) {
        alias.setAccessible(true);
        name = (String) alias.invoke(device);
      }
    } catch (NoSuchMethodException |
        InvocationTargetException |
        IllegalAccessException ignored) {
    }

    if (TextUtils.isEmpty(name)) {
      name = device.getName();
    }

    if (TextUtils.isEmpty(name)) {
      name = device.getAddress();
    } else {
      name = name + "  (" + device.getAddress() + ")";
    }

    return name;
  }

  public static boolean isSupport(Context context) {
    if (context == null) {
      return false;
    }

    return context.getPackageManager()
        .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
  }

  private void registerStateReceiver() {
    final Context context = mContext == null ? null : mContext.get();
    if (context == null) {
      return;
    }

    if (isSupport && mAdapter != null) {
      IntentFilter filter = new IntentFilter();
      filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
      filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
      context.registerReceiver(mStateReceiver, filter);
    }
  }

  private BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      final String action = intent.getAction();
      if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
        Logger.i("BLE", "State: " + state);
        if (state == BluetoothAdapter.STATE_ON) {
          startScan();
        }
        if (state == BluetoothAdapter.STATE_TURNING_OFF || state == BluetoothAdapter.STATE_OFF) {
          stopScan();
        }
      } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        Toast.makeText(context, "BondState: " + device.getBondState(), Toast.LENGTH_SHORT).show();
      }
    }
  };

  private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
      BleDevice bleDevice = new BleDevice(device);
      if (!mDevices.contains(bleDevice)) {
        synchronized (BLE.class) {
          mDevices.add(bleDevice);
        }
        // 有Name的情况下才发起通知更新
        if (!TextUtils.isEmpty(bleDevice.getName())) {
          mOPHandler.post(new Runnable() {
            @Override
            public void run() {
              // 排序
              sortDevices();
              // 通知更新
              updateDeviceInfos();
            }
          });
        }
      }
    }
  };

  private void releaseAllScanClient() {
    if (mAdapter == null) {
      return;
    }

    try {
      Field field = BluetoothAdapter.class.getDeclaredField("mManagerService");
      if (field == null) return;
      field.setAccessible(true);

      Object mIBluetoothManager = field.get(mAdapter);
      if (mIBluetoothManager == null) return;

      Method method = mIBluetoothManager.getClass().getDeclaredMethod("getBluetoothGatt");
      if (method == null) return;
      method.setAccessible(true);

      Object iGatt = method.invoke(mIBluetoothManager);
      if (iGatt == null) return;

      Method unregisterClient = iGatt.getClass().getDeclaredMethod("unregisterClient", int.class);
      if (unregisterClient == null) return;
      unregisterClient.setAccessible(true);

      Method stopScan;
      int type;
      try {
        type = 0;
        stopScan = iGatt.getClass().getDeclaredMethod("stopScan", int.class, boolean.class);
      } catch (Exception e) {
        type = 1;
        stopScan = iGatt.getClass().getDeclaredMethod("stopScan", int.class);
      }

      if (stopScan == null) return;
      stopScan.setAccessible(true);

      for (int mClientIf = 0; mClientIf <= 40; mClientIf++) {
        if (type == 0) {
          try {
            stopScan.invoke(iGatt, mClientIf, false);
          } catch (Exception ignored) {
          }
        }
        if (type == 1) {
          try {
            stopScan.invoke(iGatt, mClientIf);
          } catch (Exception ignored) {
          }
        }
        try {
          unregisterClient.invoke(iGatt, mClientIf);
        } catch (Exception ignored) {
        }
      }
      stopScan.setAccessible(false);
      unregisterClient.setAccessible(false);


      Method unm = iGatt.getClass().getDeclaredMethod("unregAll");
      if (unm == null) return;
      unm.setAccessible(true);
      unm.invoke(iGatt);

    } catch (Exception ignored) {
    }
  }

  private boolean refreshDeviceCache(BluetoothGatt gatt) {
    try {
      Method localMethod = gatt.getClass().getMethod("refresh", new Class[0]);
      if (localMethod != null) {
        return ((Boolean) localMethod.invoke(gatt, new Object[0])).booleanValue();
      } else {
        Logger.i("BLE", "RefreshDeviceCache Device: " + gatt.getDevice() + ", Method is null.");
        return false;
      }
    } catch (Exception e) {
      Logger.i("BLE", "RefreshDeviceCache Device: " + gatt.getDevice() + ", Exception:" + e.getLocalizedMessage());
      return false;
    }
  }

  private void updateDeviceInfos() {
    if (mUIHandler == null) {
      return;
    }

    mUIHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        synchronized (mBLEChangeListeners) {
          for (SoftReference<OnBLEChangeListener> ref : mBLEChangeListeners) {
            if (ref != null) {
              OnBLEChangeListener listener = ref.get();
              if (listener != null) {
                listener.onBLEDeviceInfoChanged();
              }
            }
          }
        }
      }
    }, 100);
  }

  private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      super.onConnectionStateChange(gatt, status, newState);
      updateDeviceState(gatt, newState);
      final BluetoothDevice device = gatt.getDevice();
      final String address = device.getAddress();
      Logger.d("BLE", "ConnectionStateChange Device: " + device.toString() + ", State: " + status + ", NewState: " + newState);
      if (status == BluetoothGatt.GATT_SUCCESS) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
          gatt.discoverServices();
          Logger.i("BLE", "Connect Device: " + gatt.getDevice());
          // device connect success
          if (!TextUtils.isEmpty(address)) {
            if (mManualConnectDeviceAddresses.contains(address)) {
              Bundle args = new Bundle();
              args.putString("DeviceAddress", address);
              BaseApplication.getApp().sendLocalBroadcast("BLEConnectDeviceSuccess", args);
              mManualConnectDeviceAddresses.remove(address);
            }
          }
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
          gatt.disconnect();
          gatt.close();
          // device connect dis
          if (!TextUtils.isEmpty(address)) {
            if (mManualConnectDeviceAddresses.contains(address)) {
              Bundle args = new Bundle();
              args.putString("DeviceAddress", address);
              BaseApplication.getApp().sendLocalBroadcast("BLEConnectDeviceDisconnected", args);
              mManualConnectDeviceAddresses.remove(address);
            }
          }
        }
      } else {
        if (status == 133) {
          boolean res = refreshDeviceCache(gatt);
          Logger.i("BLE", "Refresh Device: " + gatt.getDevice() + ", " + res);
        }
        gatt.disconnect();
        gatt.close();
        // device connect dis
        if (!TextUtils.isEmpty(address)) {
          if (mManualConnectDeviceAddresses.contains(address)) {
            Bundle args = new Bundle();
            args.putString("DeviceAddress", address);
            BaseApplication.getApp().sendLocalBroadcast("BLEConnectDeviceFail", args);
            mManualConnectDeviceAddresses.remove(address);
          }
        }
      }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      super.onServicesDiscovered(gatt, status);
      setGatt(gatt);
      BluetoothDevice device = gatt.getDevice();
      Logger.d("BLE", "ServicesDiscovered Device: " + device.toString() + ", State: " + status);
      for (BluetoothGattService service : gatt.getServices()) {
        for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
          gatt.setCharacteristicNotification(characteristic, true);
          gatt.readCharacteristic(characteristic);
        }
      }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
      super.onCharacteristicChanged(gatt, characteristic);
      Logger.i("BLE", "Characteristic Changed: " + characteristic.getStringValue(0));
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      super.onCharacteristicRead(gatt, characteristic, status);
      Logger.i("BLE", "Characteristic Read: " + characteristic.getStringValue(0));
      // update device name
      updateDeviceName(gatt, characteristic);
      // put characteristic
      putCharacteristic(gatt, characteristic);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      super.onCharacteristicWrite(gatt, characteristic, status);
      Logger.i("BLE", "Characteristic Write: " + characteristic.getStringValue(0));
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      super.onDescriptorRead(gatt, descriptor, status);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      super.onDescriptorWrite(gatt, descriptor, status);
    }
  };

  private void updateDeviceName(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    if (BleDevice.UUID_NAME.equals(characteristic.getUuid().toString())) {
      String name = characteristic.getStringValue(0);
      BleDevice device = new BleDevice(gatt.getDevice());
      BleDevice bleDevice = null;
      int index = mDevices.indexOf(device);
      if (index >= 0) {
        bleDevice = mDevices.get(index);
      }
      if (bleDevice != null) {
        bleDevice.setName(name);
      }

      mOPHandler.post(new Runnable() {
        @Override
        public void run() {
          // 排序
          sortDevices();
          // 通知更新
          updateDeviceInfos();
        }
      });
    }
  }

  private void updateDeviceState(BluetoothGatt gatt, int state) {
    BleDevice device = new BleDevice(gatt.getDevice());
    BleDevice bleDevice = null;
    int index = mDevices.indexOf(device);
    if (index >= 0) {
      bleDevice = mDevices.get(index);
    }
    if (bleDevice != null) {
      bleDevice.setState(state);
      bleDevice.setGatt(null);
    }

    mOPHandler.post(new Runnable() {
      @Override
      public void run() {
        // 排序
        sortDevices();
        // 通知更新
        updateDeviceInfos();
      }
    });
  }

  private void setGatt(BluetoothGatt gatt) {
    BleDevice device = new BleDevice(gatt.getDevice());
    BleDevice bleDevice = null;
    int index = mDevices.indexOf(device);
    if (index >= 0) {
      bleDevice = mDevices.get(index);
    }
    if (bleDevice != null) {
      bleDevice.setGatt(gatt);
    }
  }

  private void putCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
    BleDevice device = new BleDevice(gatt.getDevice());
    BleDevice bleDevice = null;
    int index = mDevices.indexOf(device);
    if (index >= 0) {
      bleDevice = mDevices.get(index);
    }
    if (bleDevice != null) {
      bleDevice.putCharacteristic(characteristic.getUuid().toString(), characteristic);
    }
  }

  private void sortDevices() {
    synchronized (BLE.class) {
      Collections.sort(mDevices, new Comparator<BleDevice>() {
        @Override
        public int compare(BleDevice d1, BleDevice d2) {
          return d2.getSortNum() - d1.getSortNum();
        }
      });
    }
  }
}
