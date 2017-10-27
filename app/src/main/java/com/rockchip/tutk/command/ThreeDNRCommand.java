package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/6/28.
 */
public class ThreeDNRCommand extends CommandBase implements JsonInterface {
    boolean m3DNR;

    public ThreeDNRCommand(boolean value) {
        this.m3DNR = value;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "3DNR");
            jsonObject.put("value", m3DNR);
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
