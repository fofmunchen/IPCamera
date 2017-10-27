package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/6/28.
 */
public class ContrastCommand extends CommandBase implements JsonInterface {
    String mContrast;

    public ContrastCommand(String mContrast) {
        this.mContrast = mContrast;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "Contrast");
            jsonObject.put("value", Integer.valueOf(mContrast));
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
