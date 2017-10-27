package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/6/28.
 */
public class WifiPasswordCommand extends CommandBase implements JsonInterface {
    private String mPassword;

    public WifiPasswordCommand(String mPassword) {
        this.mPassword = mPassword;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "Password");
            jsonObject.put("value", mPassword);
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
