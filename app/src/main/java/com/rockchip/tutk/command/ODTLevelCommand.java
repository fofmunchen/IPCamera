package com.rockchip.tutk.command;

import com.rockchip.tutk.DeviceInfo;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by waha on 2017/3/23.
 */
public class ODTLevelCommand extends CommandBase implements JsonInterface {
    private String level;

    public ODTLevelCommand(String level) {
        this.level = level;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "ODTLevel");
            jsonObject.put("value", Integer.valueOf(level)+ DeviceInfo.ODT_LEVEL_STANDARD);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
