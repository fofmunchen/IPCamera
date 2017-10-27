package com.rockchip.tutk;

import android.util.Log;

import java.util.HashMap;

/**
 * Created by qiujian on 2017/1/3.
 */

public class TUTKManager {
    private static String TAG = "TUTKManager";
    public static String ACTION_TUTK_DEVICE_ATTACHED = "ACTION_TUTK_DEVICE_ATTACHED";
    public static String ACTION_TUTK_DEVICE_DETACHED = "ACTION_TUTK_DEVICE_DETACHED";
    public static String TUTK_UID = "UID";
    public static String TUTK_EXTRA = "EXTRA";
    public static String TYPE = "TYPE";
    public static String TUTK_DEVICE_NAME = "DEVICE_NAME";

    static HashMap<String, TUTKDevice> deviceHashMap = new HashMap<>();

    private TUTKManager() {

    }

    public static TUTKDevice getByUID(String uid) {
        Log.d("wz", "getByUID deviceHashMap size is " +deviceHashMap.size());
        TUTKDevice tutkDevice = deviceHashMap.get(uid);
        return tutkDevice;
    }

    public static void addDevice(TUTKDevice device) {
        Log.d("wz", "addDevice"+device.mUID);
        deviceHashMap.put(device.mUID, device);
    }

    public static void removeDevice(TUTKDevice device) {
        Log.d("wz", "removeDevice");
        deviceHashMap.remove(device.mUID);
    }
}
