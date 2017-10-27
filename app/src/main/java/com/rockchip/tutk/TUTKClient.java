package com.rockchip.tutk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Message;
import android.util.Log;

import com.rockchip.tutk.constants.Constants;
import com.rockchip.tutk.constants.GlobalValue;
import com.rockchip.tutk.utils.MsgManager;
import com.rockchip.tutk.utils.UserManager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by qiujian on 2017/1/3.
 */

public class TUTKClient {

    private String TAG = "TUTKClient";

    Context mContext;

    private final HashMap<String, TUTKDevice> mDevices = new HashMap<String, TUTKDevice>();

    private final ArrayList<Listener> mListeners = new ArrayList<Listener>();

    private final ArrayList<String> mIgnoredDevices = new ArrayList<String>();

    BroadcastReceiver mTutkRecver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String uid = intent.getStringExtra(TUTKManager.TUTK_UID);
            String extra = intent.getStringExtra(TUTKManager.TUTK_EXTRA);
            Log.i("wz", "mTutkRecver onReceive:" + action);

            synchronized (mDevices) {
                TUTKDevice tutkDevice = mDevices.get(uid);
                if (TUTKManager.ACTION_TUTK_DEVICE_ATTACHED.equals(action)) {
                    UserManager.saveUserDeviceList(uid);
                    GlobalValue.deviceUID = uid;
                    if (tutkDevice == null) {
                        Log.i("wz", "tutkDevice == null");
                        tutkDevice = openDeviceLocked(uid);
                    }
                    if (tutkDevice != null) {
                        TUTKManager.addDevice(tutkDevice);
                        for (Listener listener : mListeners) {
                            listener.deviceAdded(tutkDevice);
                        }
                    }
                    if(extra!=null&&extra.equals("REFRESH"))
                    {
                        Log.i("wz", "REFRESH UI");
                        Message msg = new Message();
                        msg.what = Constants.UserMSG.REFRESH;
                        MsgManager.sendToHandler("FragementCamera",msg);
                    }
                } else if (TUTKManager.ACTION_TUTK_DEVICE_DETACHED.equals(action)) {
                    if (tutkDevice != null) {
                        TUTKManager.removeDevice(tutkDevice);
                        mDevices.remove(uid);
                        mIgnoredDevices.remove(uid);
                        for (Listener listener : mListeners) {
                            listener.deviceRemoved(tutkDevice);
                        }
                    }
                }
            }
        }
    };


    public TUTKClient(Context context) {
        this.mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(TUTKManager.ACTION_TUTK_DEVICE_ATTACHED);
        filter.addAction(TUTKManager.ACTION_TUTK_DEVICE_DETACHED);
        context.registerReceiver(mTutkRecver, filter);
    }

    private TUTKDevice openDeviceLocked(String uid) {
        if (!mIgnoredDevices.contains(uid)) {
            TUTKDevice tutkDevice =new TUTKDevice(uid);
            if (tutkDevice != null) {
                mDevices.put(uid, tutkDevice);
                return tutkDevice;
            }
        }
        return null;
    }

    public void close() {
        mContext.unregisterReceiver(mTutkRecver);
        for (TUTKDevice device :mDevices.values()) {
            device.close();
        }
    }


    public void addListener(Listener listener) {
        Log.d(TAG,"----addListener----"+listener);
        synchronized (mDevices) {
            if (!mListeners.contains(listener)) {
                mListeners.add(listener);
            }
        }
    }


    public void removeListener(Listener listener) {
        Log.d(TAG,"----removeListener----"+listener);
        synchronized (mDevices) {
            mListeners.remove(listener);
        }
    }

    public interface Listener {
        /**
         * Called when a new device has been added
         *
         * @param device the new device that was added
         */
        public void deviceAdded(TUTKDevice device);

        /**
         * Called when a new device has been removed
         *
         * @param device the device that was removed
         */
        public void deviceRemoved(TUTKDevice device);
    }
}
