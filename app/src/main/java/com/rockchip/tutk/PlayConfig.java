package com.rockchip.tutk;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by waha on 2017/4/1.
 */

public class PlayConfig {
    public static String GetPlayConfig = "GetPlayConfig";
    public static String PlayBrightness = "PlayBrightness";
    public static String PlayVolume = "PlayVolume";
    public static String PlayContrast = "PlayContrast";
    public static String PlaySaturation = "PlaySaturation";
    public static String PlaySharpness = "PlaySharpness";

    private int playBrightness;
    private int playVolume;
    private int playContrast;
    private int playSaturation;
    private int playSharpness;

    public boolean initPlayConfigInfo(String json) {
        try {
            JSONArray item_list = new JSONObject(json).getJSONArray("item_list");
            for (int i = 0; i < item_list.length(); i++) {
                JSONObject item = item_list.getJSONObject(i);
                if (PlayBrightness.equals(item.getString(DeviceInfo.Name))) {
                    playBrightness = item.getInt(DeviceInfo.Value);
                } else if (PlayVolume.equals(item.getString(DeviceInfo.Name))) {
                    playVolume = item.getInt(DeviceInfo.Value);
                } else if (PlayContrast.equals(item.getString(DeviceInfo.Name))) {
                    playContrast = item.getInt(DeviceInfo.Value);
                } else if (PlaySaturation.equals(item.getString(DeviceInfo.Name))) {
                    playSaturation = item.getInt(DeviceInfo.Value);
                } else if (PlaySharpness.equals(item.getString(DeviceInfo.Name))) {
                    playSharpness = item.getInt(DeviceInfo.Value);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public int getPlayBrightness() {
        return playBrightness;
    }

    public void setPlayBrightness(int playBrightness) {
        this.playBrightness = playBrightness;
    }

    public int getPlayVolume() {
        return playVolume;
    }

    public void setPlayVolume(int playVolume) {
        this.playVolume = playVolume;
    }

    public int getPlayContrast() {
        return playContrast;
    }

    public void setPlayContrast(int playContrast) {
        this.playContrast = playContrast;
    }

    public int getPlaySaturation() {
        return playSaturation;
    }

    public void setPlaySaturation(int playSaturation) {
        this.playSaturation = playSaturation;
    }

    public int getPlaySharpness() {
        return playSharpness;
    }

    public void setPlaySharpness(int playSharpness) {
        this.playSharpness = playSharpness;
    }
}
