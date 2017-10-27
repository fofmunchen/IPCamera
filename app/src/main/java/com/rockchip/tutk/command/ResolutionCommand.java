package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/6/28.
 */
public class ResolutionCommand extends CommandBase implements JsonInterface {

    String mResolutionl;

    public ResolutionCommand(String mResolutionl) {
        this.mResolutionl = mResolutionl;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "Resolutionl");
            jsonObject.put("value", mResolutionl);
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
