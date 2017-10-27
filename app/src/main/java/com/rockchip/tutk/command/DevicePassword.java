package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2017/5/12.
 */

public class DevicePassword extends CommandBase implements JsonInterface {
    private String mOldPassword;
    private String mPassword;

    public DevicePassword(String mOldPassword, String mPassword) {
        this.mOldPassword = mOldPassword;
        this.mPassword = mPassword;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("oldPwd", mOldPassword);
            jsonObject.put("newPwd", mPassword);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
