package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/6/28.
 */
public class FormatCommand extends CommandBase implements JsonInterface {
    boolean mFormat;

    public FormatCommand(boolean mFormat) {
        this.mFormat = mFormat;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "Format");
            jsonObject.put("value", mFormat);
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
