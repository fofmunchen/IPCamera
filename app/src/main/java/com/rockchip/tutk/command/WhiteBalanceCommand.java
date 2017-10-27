package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/6/28.
 */
public class WhiteBalanceCommand extends CommandBase implements JsonInterface {
    String mWhiteBalance;

    public WhiteBalanceCommand(String mWhiteBalance) {
        this.mWhiteBalance = mWhiteBalance;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "WhiteBalance");
            jsonObject.put("value", mWhiteBalance);
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
