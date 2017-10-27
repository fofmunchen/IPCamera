package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by waha on 2017/3/23.
 */
public class ODTCommand extends CommandBase implements JsonInterface {
    boolean value;

    public ODTCommand(boolean value) {
        this.value = value;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "ODT");
            jsonObject.put("value", value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
