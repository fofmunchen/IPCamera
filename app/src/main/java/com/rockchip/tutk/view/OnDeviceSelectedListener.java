package com.rockchip.tutk.view;


import com.rockchip.tutk.TUTKDevice;

public interface OnDeviceSelectedListener {
    void deviceOffline(TUTKDevice device);
    void onDeviceSettingClick(TUTKDevice device);
    void onDeviceSelected(TUTKDevice info);
    void onDeviceDelete(TUTKDevice info);
}
