package com.scliang.core.demo;

import android.bluetooth.BluetoothProfile;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.scliang.core.base.BaseActivity;
import com.scliang.core.base.result.BasicResult;
import com.scliang.core.ble.BLE;
import com.scliang.core.ble.BleDevice;
import com.scliang.core.ble.OnBLEChangeListener;
import com.scliang.core.ui.BaseSimpleFragment;

import java.util.List;

public class DemoBLEListFragment
    extends BaseSimpleFragment<BasicResult, BasicResult>
    implements OnBLEChangeListener {
  private List<BleDevice> mDevices;

  @Override
  protected void onViewCreatedHere(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreatedHere(view, savedInstanceState);
    showToolbarNavigationIcon();
    setToolbarCenterTitle(com.scliang.core.R.string.title_ble_devices);
    setToolbarBottomLineVisibility(true, 0x66dddddd);
    setToolbarMenu(com.scliang.core.R.menu.menu_ble_scan);

    BLE.getInstance().registerBLEChangeListener(this);
    mDevices = BLE.getInstance().getShowableDevices();

    List<BleDevice> boundDevices = BLE.getInstance().getBoundDevices();
    Toast.makeText(view.getContext(), "Bound: " + boundDevices.size(), Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onDestroyView() {
    BLE.getInstance().unregisterBLEChangeListener(this);
    BLE.getInstance().stopScan();
    super.onDestroyView();
  }

  @Override
  protected void onMenuItemClick(int groupId, int itemId) {
    if (itemId == com.scliang.core.R.id.menu_ble_scan_start) {
      BLE.getInstance().open((BaseActivity) getActivity());
      BLE.getInstance().startScan();
    } else if (itemId == com.scliang.core.R.id.menu_ble_scan_stop) {
      BLE.getInstance().stopScan();
    }
  }

  @Override
  protected int getItemCount() {
    return mDevices == null ? 0 : mDevices.size();
  }

  @Override
  protected int getItemViewType(int position) {
    return 0;
  }

  @Override
  protected SimpleVHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == 0) {
      final View view = LayoutInflater.from(getContext())
          .inflate(com.scliang.core.R.layout.view_simple_ble_device_item, parent, false);
      return new DeviceVHolder(view);
    }

    return super.onCreateViewHolder(parent, viewType);
  }

  @Override
  protected void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof DeviceVHolder) {
      ((DeviceVHolder) holder).update(mDevices == null ? null : mDevices.get(position));
    }
  }

  @Override
  public void onBLEDeviceInfoChanged() {
    mDevices = BLE.getInstance().getShowableDevices();
    reloadUI();
  }

  @Override
  public void onReceiveLocalBroadcastReceiver(String action, Bundle args) {
    // BLE开始连接设备
    if ("BLEConnectDeviceStarted".equals(action)) {
      showSimpleLoadDialog();
    }
    // BLE连接成功
    else if ("BLEConnectDeviceSuccess".equals(action)) {
      closeSimpleLoadDialog();
    }
    // BLE连接失败
    else if ("BLEConnectDeviceFail".equals(action)) {
      closeSimpleLoadDialog();
    }
    // BLE连接断开
    else if ("BLEConnectDeviceDisconnected".equals(action)) {
      closeSimpleLoadDialog();
    }
  }

  private static final class DeviceVHolder extends SimpleVHolder {
    private Drawable mConnectedDrawable;
    private Drawable mDiscoveredDrawable;
    private TextView mNameView;

    DeviceVHolder(View itemView) {
      super(itemView);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        mConnectedDrawable = itemView.getContext().getResources()
            .getDrawable(com.scliang.core.R.drawable.ucrop_ic_done);
        if (mConnectedDrawable != null) {
          mConnectedDrawable.setTint(0xffffcc00);
          mConnectedDrawable.setBounds(0, 0,
              mConnectedDrawable.getIntrinsicWidth(),
              mConnectedDrawable.getIntrinsicHeight());
        }
        mDiscoveredDrawable = itemView.getContext().getResources()
            .getDrawable(com.scliang.core.R.drawable.ucrop_ic_done);
        if (mDiscoveredDrawable != null) {
          mDiscoveredDrawable.setTint(0xff339933);
          mDiscoveredDrawable.setBounds(0, 0,
              mDiscoveredDrawable.getIntrinsicWidth(),
              mDiscoveredDrawable.getIntrinsicHeight());
        }
      } else {
        mConnectedDrawable = itemView.getContext().getResources()
            .getDrawable(com.scliang.core.R.drawable.ucrop_ic_done);
        if (mConnectedDrawable != null) {
          mConnectedDrawable.setBounds(0, 0,
              mConnectedDrawable.getIntrinsicWidth(),
              mConnectedDrawable.getIntrinsicHeight());
        }
        mDiscoveredDrawable = itemView.getContext().getResources()
            .getDrawable(com.scliang.core.R.drawable.pictures_selected);
        if (mDiscoveredDrawable != null) {
          mDiscoveredDrawable.setBounds(0, 0,
              mDiscoveredDrawable.getIntrinsicWidth(),
              mDiscoveredDrawable.getIntrinsicHeight());
        }
      }

      mNameView = itemView.findViewById(com.scliang.core.R.id.item_action);
      if (mNameView != null) mNameView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          Object obj = view.getTag();
          if (obj instanceof BleDevice) {
            BleDevice device = (BleDevice) obj;
            BLE.getInstance().connectDevice(device, true);
//                        BLE.getInstance().connectDevice((BleDevice) obj);
//            StringBuilder sb = new StringBuilder();
//            sb.append("BondState: ").append(device.getBondState()).append('\n');
//            Map<String, BluetoothGattCharacteristic> characteristics = device.getCharacteristics();
//            if (characteristics != null && characteristics.size() > 0) {
//              sb.append("Characteristics");
//              for (BluetoothGattCharacteristic characteristic : characteristics.values()) {
//                if (characteristic != null) {
//                  sb.append('\n');
//                  sb.append(characteristic.getStringValue(0));
//                }
//              }
//            }
//            if (sb.length() > 0) {
//              BaseBLEDevicesFragment frag = mFragment == null ? null : mFragment.get();
//              if (frag != null) {
//                frag.showConfirmDialog(sb.toString(), "OK", new Runnable() {
//                  @Override
//                  public void run() {
//                  }
//                }, new DialogInterface.OnDismissListener() {
//                  @Override
//                  public void onDismiss(DialogInterface dialog) {
//                  }
//                });
//              }
//            }
          }
        }
      });
//      if (mNameView != null) mNameView.setOnLongClickListener(new View.OnLongClickListener() {
//        @Override
//        public boolean onLongClick(View view) {
//          Object obj = view.getTag();
//          if (obj instanceof BleDevice) {
//            BleDevice device = (BleDevice) obj;
//            int bondState = device.getBondState();
//            if (bondState == BOND_NONE) {
//              boolean res = device.createBond();
//              Toast.makeText(mNameView.getContext(), res ? "Success" : "Faild", Toast.LENGTH_SHORT).show();
//              if (res) {
//                BLE.getInstance().connectDevice(device);
//              }
//            }
//            BluetoothGatt gatt = device.getGatt();
//            if (gatt != null) {
//            }
//          }
//          return true;
//        }
//      });
    }

    public void update(BleDevice device) {
      if (device == null) {
        return;
      }

      if (mNameView != null) {
        mNameView.setTag(device);
        mNameView.setCompoundDrawables(null, null,
            device.getState() == BluetoothProfile.STATE_CONNECTED ?
                (device.getGatt() == null ?
                    mConnectedDrawable : mDiscoveredDrawable) : null,
            null);
        String name = device.getName();
        if (TextUtils.isEmpty(name)) {
          mNameView.setText(Html.fromHtml(
              "<font color='#999999'>" +
                  device.getAddress() + "</font>"));
        } else {
          mNameView.setText(Html.fromHtml(
              name + "<br/><font color='#999999'>" +
                  device.getAddress() + "</font>"));
        }
      }
    }
  }
}
