package com.rockchip.tutk;

import com.rockchip.tutk.model.SdcardInfo;
import com.rockchip.tutk.utils.MsgDatas;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by qiujian on 2017/1/18.
 */

public class DeviceInfo {
    public static String Name = "item_name";
    public static String Value = "item_value";
    public static String Resolutionl = "Resolutionl";
    public static String RecordTime = "RecordTime";
    public static String Exposure = "Exposure";
    public static String WhiteBalance = "WhiteBalance";
    public static String WaterMark = "WaterMark";
    public static String Moving = "Moving";
    public static String ThreeDNR = "3DNR";
    public static String ThreeDNRLevel = "3DNRLevel";
    public static String IDC = "IDC";
    public static String Sound = "Sound";
    public static String Fault = "fault";
    public static String MovingSensitivity = "MovingSensitivity";
    public static String ODT = "ODT";
    public static String ODTLevel = "ODTLevel";
    public static String SDcardInfo = "SdcardInfo";
    public static String Format = "Format";
    public static final int ODT_LEVEL_STANDARD = 9;
    public static final int ODT_DEFAULT_LEVEL = 20;
    public List<EncoderParameter> encoderParameters = new ArrayList<>();

    public DeviceInfo() {

    }

    public DeviceInfo(JSONArray item_list) {
        initDeviceInfo(item_list);
    }

    public boolean initDeviceInfo(JSONArray item_list) {
        try {

            for (int i = 0; i < item_list.length(); i++) {
                JSONObject item = item_list.getJSONObject(i);
                if (Resolutionl.equals(item.getString(Name))) {
                    mResolution = item.getString(Value);
                } else if (RecordTime.equals(item.getString(Name))) {
                    mRecordTime = item.getInt(Value);
                } else if (Exposure.equals(item.getString(Name))) {
                    mExposure = item.getInt(Value);
                } else if (WhiteBalance.equals(item.getString(Name))) {
                    mWhiteBalance = item.getString(Value);
                } else if (WaterMark.equals(item.getString(Name))) {
                    mWaterMark = item.getBoolean(Value);
                } else if (Moving.equals(item.getString(Name))) {
                    mMoving = item.getBoolean(Value);
                } else if (ThreeDNR.equals(item.getString(Name))) {
                    m3DNR = item.getBoolean(Value);
                } else if (IDC.equals(item.getString(Name))) {
                    mIDC = item.getBoolean(Value);
                } else if (Sound.equals(item.getString(Name))) {
                    mSound = item.getBoolean(Value);
                } else if (Fault.equals(item.getString(Name))) {
                    String fault = item.getString(Value);
                    if (null != fault && fault.contains("sdcard")) {
                        mSdcardFault = false;//true;
                    }
                } else if (MovingSensitivity.equals(item.get(Name))) {
                    moveSensitivity = item.getInt(Value);
                } else if (ThreeDNRLevel.equals(item.get(Name))) {
                    m3DNRLevel = item.getInt(Value);
                } else if (ODT.equals(item.get(Name))) {
                    odt = item.getBoolean(Value);
                } else if (ODTLevel.equals(item.get(Name))) {
                    if(-1 == item.getInt(Value)){
                        odtLevel = item.getInt(Value);
                    }else{
                        odtLevel = item.getInt(Value) - ODT_LEVEL_STANDARD;
                    }
                } else if(SDcardInfo.equals(item.get(Name))) {
                    if(null != item.getString(Value)){
                        String[] desc = item.getString(Value).split(",");
                        if(null != desc && desc.length == 3){
                            sdcardInfo.setExist(Integer.parseInt(desc[0]) == 1);
                            sdcardInfo.setFreeSize(Long.parseLong(desc[1]));
                            sdcardInfo.setTotalSize(Long.parseLong(desc[2]));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    String mDeviceName;
    String mResolution;
    String playResolution;
    int mRecordTime;
    int mExposure;
    String mWhiteBalance;
    boolean mWaterMark;
    boolean mMoving;
    boolean m3DNR;
    private int m3DNRLevel = 1;
    boolean mIDC;
    boolean mSound;
    private int moveSensitivity = 4;
    private boolean mSdcardFault;
    private boolean odt;
    private int odtLevel;
    private SdcardInfo sdcardInfo = new SdcardInfo();
    private PlayConfig playConfig = new PlayConfig();
    private int playFps = MsgDatas.DEFAULT_FPS;

    public String getDeviceName() {
        return mDeviceName;
    }

    public void setDeviceName(String name) {
        this.mDeviceName = name;
    }

    public String getPlayResolution() {
        return playResolution;
    }

    public void setPlayResolution(String name) {
        this.playResolution = name;
    }


    public String getmResolution() {
        return mResolution;
    }

    public void setmResolution(String mResolution) {
        this.mResolution = mResolution;
    }

    public int getmRecordTime() {
        return mRecordTime;
    }

    public void setmRecordTime(int mRecordTime) {
        this.mRecordTime = mRecordTime;
    }

    public int getmExposure() {
        return mExposure;
    }

    public void setmExposure(int mExposure) {
        this.mExposure = mExposure;
    }

    public String getmWhiteBalance() {
        return mWhiteBalance;
    }

    public void setmWhiteBalance(String mWhiteBalance) {
        this.mWhiteBalance = mWhiteBalance;
    }

    public boolean ismWaterMark() {
        return mWaterMark;
    }

    public void setmWaterMark(boolean mWaterMark) {
        this.mWaterMark = mWaterMark;
    }

    public boolean ismMoving() {
        return mMoving;
    }

    public void setmMoving(boolean mMoving) {
        this.mMoving = mMoving;
    }

    public boolean isIDC() {
        return mIDC;
    }

    public void setIDC(boolean IDC) {
        this.mIDC = IDC;
    }

    public boolean is3DNR() {
        return m3DNR;
    }

    public void set3DNR(boolean dnr) {
        this.m3DNR = dnr;
    }

    public boolean isSound() {
        return mSound;
    }

    public void setSound(boolean sound) {
        this.mSound = sound;
    }

    public boolean isSdcardFault() {
        return mSdcardFault;
    }

    public void setSdcardFault(boolean sdcardFault) {
        this.mSdcardFault = sdcardFault;
    }

    public int getMoveSensitivity() {
        return moveSensitivity;
    }

    public void setMoveSensitivity(int moveSensitivity) {
        this.moveSensitivity = moveSensitivity;
    }

    public int get3DNRLevel() {
        return m3DNRLevel;
    }

    public void set3DNRLevel(int level) {
        m3DNRLevel = level;
    }

    public boolean isOdt() {
        return odt;
    }

    public void setOdt(boolean odt) {
        this.odt = odt;
    }

    public int getOdtLevel() {
        return odtLevel;
    }

    public void setOdtLevel(int odtLevel) {
        this.odtLevel = odtLevel;
    }

    public PlayConfig getPlayConfig() {
        return playConfig;
    }

    public void setPlayConfig(PlayConfig playConfig) {
        this.playConfig = playConfig;
    }

    public String getSdcardDesc(){
        return sdcardInfo.getDesc();
    }

    public SdcardInfo getSdcardInfo() {
        return sdcardInfo;
    }

    public int getPlayFps() {
        return playFps;
    }

    public void setPlayFps(int playFps) {
        this.playFps = playFps;
    }
}
