package com.rockchip.tutk.model;

import java.io.Serializable;

/**
 * Created by waha on 2017/3/9.
 */

public class SdcardModel implements Serializable {
    private String name;
    private String desc;
    private String uid;
    private String AlarmTime;
    private boolean needFormat;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getAlarmTime() {
        return AlarmTime;
    }

    public void setAlarmTime(String alarmTime) {
        AlarmTime = alarmTime;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public boolean isNeedFormat() {
        return needFormat;
    }

    public void setNeedFormat(boolean needFormat) {
        this.needFormat = needFormat;
    }
}
