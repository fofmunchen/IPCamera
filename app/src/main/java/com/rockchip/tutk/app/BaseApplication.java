package com.rockchip.tutk.app;

import android.app.Application;

/**
 * Created by Administrator on 2016/7/11.
 */
public class BaseApplication extends Application {
    private static final String TAG = "CAM_BaseApplication";
    private static BaseApplication mApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        mApplication = this;
    }

    public static BaseApplication getInstance() {
        return mApplication;
    }
}
