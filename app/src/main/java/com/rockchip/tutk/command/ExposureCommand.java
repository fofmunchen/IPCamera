package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/6/28.
 */
public class ExposureCommand extends CommandBase implements JsonInterface {
    String mExposure;

    public ExposureCommand(String mExposure) {
        this.mExposure = mExposure;
    }

    @Override
    public String Json() {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "Exposure");
            jsonObject.put("value", Integer.valueOf(mExposure));
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
