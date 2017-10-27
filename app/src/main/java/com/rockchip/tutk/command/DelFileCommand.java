package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by waha on 2017/4/7.
 */

public class DelFileCommand extends CommandBase {
    private String path;

    public DelFileCommand(String path) {
        this.path = path;
    }

    @Override
     public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "DelFile");
            jsonObject.put("value", path);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
