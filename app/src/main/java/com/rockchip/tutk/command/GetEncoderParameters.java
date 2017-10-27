package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Administrator on 2017/3/21.
 */

public class GetEncoderParameters extends CommandBase implements JsonInterface{

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "getEncoderParameters");
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
