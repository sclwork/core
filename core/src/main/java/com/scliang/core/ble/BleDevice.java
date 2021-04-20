package com.scliang.core.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

import static android.bluetooth.BluetoothDevice.BOND_NONE;

/**
 * SCore
 * Created by ShangChuanliang
 * on 2018/11/15.
 */
public class BleDevice {
  public static final String UUID_NAME = "00002a00-0000-1000-8000-00805f9b34fb";

  private BluetoothGatt mGatt;
  private BluetoothDevice mDevice;
  private Map<String, BluetoothGattCharacteristic> mCharacteristics;

  private String mName;
  private int mSortNum;
  private int mState = -1111;

  public BleDevice(BluetoothDevice device) {
    mDevice = device;
    // init Sort Num
    mSortNum = mDevice == null || TextUtils.isEmpty(mDevice.getName()) ? 0 : 10;
    mCharacteristics = new HashMap<>();
  }

  public int getBondState() {
    if (mDevice == null) {
      return BOND_NONE;
    } else {
      return mDevice.getBondState();
    }
  }

  public boolean createBond() {
    if (mDevice == null) {
      return false;
    } else {
      return mDevice.createBond();
    }
  }

  public void setName(String name) {
    mName = name;
    if (mState == BluetoothProfile.STATE_CONNECTED) {
      mSortNum = 20;
    } else {
      mSortNum = 10;
    }
  }

  public String getName() {
    return TextUtils.isEmpty(mName) ? (mDevice == null ? "" : mDevice.getName()) : mName;
  }

  public String getAddress() {
    return mDevice == null ? "" : mDevice.getAddress();
  }

  public void setGatt(BluetoothGatt gatt) {
    mGatt = gatt;
  }

  public BluetoothGatt getGatt() {
    return mGatt;
  }

  public BluetoothDevice getDevice() {
    return mDevice;
  }

  public int getSortNum() {
    return mSortNum;
  }

  public void setState(int state) {
    mState = state;
    if (mState == BluetoothProfile.STATE_CONNECTED) {
      mSortNum = 20;
    } else {
      mSortNum = TextUtils.isEmpty(getName()) ? 0 : 10;
    }
  }

  public int getState() {
    return mState;
  }

  public void putCharacteristic(String uuid, BluetoothGattCharacteristic characteristic) {
    if (mCharacteristics != null && !TextUtils.isEmpty(uuid)) {
      if (characteristic == null) {
        mCharacteristics.remove(uuid);
      } else {
        mCharacteristics.put(uuid, characteristic);
      }
    }
  }

  public Map<String, BluetoothGattCharacteristic> getCharacteristics() {
    return mCharacteristics;
  }

  @Override
  public int hashCode() {
    return mDevice == null ? super.hashCode() : mDevice.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof BleDevice)) {
      return false;
    }

    return mDevice == null ? super.equals(obj) :
        mDevice.equals(((BleDevice) obj).mDevice);
  }
}
