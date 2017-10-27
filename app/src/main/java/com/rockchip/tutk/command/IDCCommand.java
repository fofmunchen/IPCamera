package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/6/28.
 */
public class IDCCommand extends CommandBase implements JsonInterface {
    boolean mIDC;

    public IDCCommand(boolean value) {
        this.mIDC = value;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "IDC");
            jsonObject.put("value", mIDC);
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
