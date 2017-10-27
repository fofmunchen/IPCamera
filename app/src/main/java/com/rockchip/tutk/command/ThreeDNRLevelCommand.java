package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by waha on 2017/3/22.
 */
public class ThreeDNRLevelCommand extends CommandBase implements JsonInterface {
    String level;

    public ThreeDNRLevelCommand(String level) {
        this.level = level;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "3DNRLevel");
            jsonObject.put("value", Integer.valueOf(level));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
