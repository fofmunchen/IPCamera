package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/12/22.
 */

public class VideoListCommand extends CommandBase {
    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "VideoList");
            jsonObject.put("type", type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
