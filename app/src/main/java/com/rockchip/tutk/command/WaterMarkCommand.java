package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/6/28.
 */
public class WaterMarkCommand extends CommandBase implements JsonInterface {
    boolean mWaterMark;

    public WaterMarkCommand(boolean mWaterMark) {
        this.mWaterMark = mWaterMark;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "WaterMark");
            jsonObject.put("value", mWaterMark);
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
