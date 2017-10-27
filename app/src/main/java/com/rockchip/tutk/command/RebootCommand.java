package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2017/3/28.
 */

public class RebootCommand extends CommandBase implements  JsonInterface {

    @Override
    public String Json() {
        JSONObject jsonObject=new JSONObject();
        try {
            jsonObject.put("name","Reboot");
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
