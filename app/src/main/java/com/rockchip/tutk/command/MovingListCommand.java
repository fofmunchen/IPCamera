package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by waha on 2017/3/20.
 */
public class MovingListCommand extends CommandBase implements JsonInterface {
    private String state;

    public MovingListCommand(String state) {
        this.state = state;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "MovingSensitivity");
            jsonObject.put("value", Integer.valueOf(state));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
