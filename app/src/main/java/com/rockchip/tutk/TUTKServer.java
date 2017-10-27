package com.rockchip.tutk;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import com.rockchip.tutk.activity.CamMainActivity;
import com.rockchip.tutk.activity.SdcardActivity;
import com.rockchip.tutk.model.MdNotifyInfo;
import com.rockchip.tutk.model.SdcardModel;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.RDTAPIs;


import java.util.ArrayList;
import java.util.HashMap;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Connect_Stop;
import static com.tutk.IOTC.IOTCAPIs.IOTC_DeInitialize;
import static com.tutk.IOTC.IOTCAPIs.IOTC_ER_NoERROR;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Initialize2;

public class TUTKServer extends Service implements TUTKClient.Listener, TUTKDevice.NotifyCallback {

    LocalBinder mBinder = new LocalBinder();


    private String TAG = "TUTKServer";


    private final HashMap<String, TUTKDevice> mDevices = new HashMap<String, TUTKDevice>();

    private final ArrayList<Listener> mListeners = new ArrayList<Listener>();

    TUTKClient mTUTKClient;

    private Context mMainActivity;

    private int mErrNum = -1;

    @Override
    public void deviceAdded(TUTKDevice device) {
        for (Listener listener : mListeners) {
            try {
                device.addListener(this);
                listener.deviceAdded(device);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void deviceRemoved(TUTKDevice device) {
        for (Listener listener : mListeners) {
            try {
                listener.deviceRemoved(device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onEvent(MdNotifyInfo notifyInfo) {
        Log.d(TAG, "notify onevent");

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
        notification.setSmallIcon(R.drawable.logo);
        notification.setAutoCancel(true);	    //点击自动消息
        if("stop".equals(notifyInfo.getDesc())){
            notification.setDefaults(Notification.DEFAULT_VIBRATE|Notification.DEFAULT_LIGHTS);
            notification.setSound(Uri.parse("android.resource://"
                    + getPackageName() + "/" +R.raw.beep));
            notification.setContentTitle(notifyInfo.getTitle());
            notification.setContentText(notifyInfo.getAlarmTime());
        } else if("start".equals(notifyInfo.getDesc()) || null == notifyInfo.getBitmap()){
            notification.setDefaults(Notification.DEFAULT_VIBRATE|Notification.DEFAULT_LIGHTS);
            notification.setSound(Uri.parse("android.resource://"
                    + getPackageName() + "/" +R.raw.record));
            notification.setContentTitle(notifyInfo.getTitle());
            notification.setContentText(notifyInfo.getAlarmTime());
        } else if (null != notifyInfo.getBitmap()){
            RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.notify_layout);
            contentView.setTextViewText(R.id.txt_notify_title, notifyInfo.getTitle());
            contentView.setTextViewText(R.id.txt_notify_content, notifyInfo.getAlarmTime());
            contentView.setImageViewBitmap(R.id.img_notify, notifyInfo.getBitmap());
            notification.setContent(contentView);
            notification.setDefaults(Notification.DEFAULT_ALL);	        //铃声,振动,呼吸灯
        }
        Intent intent = new Intent(this, CamMainActivity.class);    //点击通知进入的界面
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        notification.setContentIntent(contentIntent);

        synchronized(notification){
            notificationManager.notify(0,notification.build());
        }

    }

    @Override
    public void onPictureTaked(String patch) {
        //Toast.makeText(this,patch,Toast.LENGTH_SHORT).show();
        Log.d(TAG,"---------------------------"+patch);
    }

    @Override
    public void onSdcardAlarm(SdcardModel model) {
        Log.d(TAG,"rev sdcard alarm");
        if(null != model){
            Intent intent = new Intent(this, SdcardActivity.class);
            intent.putExtra("sdcard", model);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onPlayConfigLoaded(int msgWhat, boolean result) {

    }

    @Override
    public void onRecordListLoaded(String json) {

    }

    @Override
    public void onDelFile(String json) {

    }

    public class LocalBinder extends Binder {
        public TUTKServer getService() {
            return TUTKServer.this;
        }
    }

    /**
     * An interface for being notified when devices are attached
     * or removed.
     */
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

    public TUTKServer() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        initlize();
        mTUTKClient = new TUTKClient(this);
        mTUTKClient.addListener(this);
    }

    @Override
    public void onDestroy() {
        mTUTKClient.close();
        close();
        super.onDestroy();
        Log.d(TAG, "onDestroy");


    }

    /**
     * Registers a .Listener interface to receive
     * notifications when devices are added or removed.
     *
     * @param listener the listener to register
     */
    public void addListener(Listener listener) {
        Log.d(TAG, "----addListener----" + listener);
        synchronized (mDevices) {
            if (!mListeners.contains(listener)) {
                mListeners.add(listener);
            }
        }
    }

    /**
     * Unregisters a .Listener interface.
     *
     * @param listener the listener to unregister
     */
    public void removeListener(Listener listener) {
        Log.d(TAG, "----removeListener----" + listener);
        synchronized (mDevices) {
            mListeners.remove(listener);
        }
    }

    public void setClientActivity(Context mainActivity) {
        Log.d(TAG, "----setClientActivity----" + mainActivity);
        mMainActivity = mainActivity;
    }

    private boolean initlize() {
        if (mErrNum != IOTC_ER_NoERROR) {
            mErrNum = IOTC_Initialize2(0);
            Log.d(TAG, "IOTC_Initialize(.)=" + mErrNum);
            if (mErrNum < 0) {
                return false;
            }
        }
        int avInitialize = AVAPIs.avInitialize(3);
        Log.d(TAG, "AVAPIs.avInitialize(3);" + avInitialize);
        int rdt_initialize = RDTAPIs.RDT_Initialize();
        Log.d(TAG, "RDTAPIs.RDT_Initialize();" + rdt_initialize);
        return true;
    }

    public void close() {

        IOTC_Connect_Stop();
        RDTAPIs.RDT_DeInitialize();
        AVAPIs.avDeInitialize();
        IOTC_DeInitialize();
        mErrNum = IOTCAPIs.IOTC_ER_TIMEOUT;
        Log.d(TAG, "IOTC_DeInitialize()");
        if (mErrNum == IOTCAPIs.IOTC_ER_NoERROR) {


        }

    }


}
