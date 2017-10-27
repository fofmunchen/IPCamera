package com.rockchip.tutk.model;

import android.graphics.Bitmap;

/**
 * Created by waha on 2017/5/19.
 */

public class MdNotifyInfo {
    private String UID;
    private String alarmTime;
    private String desc;
    private Bitmap bitmap;
    private String title = "移动侦测";

    public String getAlarmTime() {
        return alarmTime;
    }

    public void setAlarmTime(String alarmTime) {
        this.alarmTime = alarmTime;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getUID() {
        return UID;
    }

    public void setUID(String UID) {
        this.UID = UID;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
