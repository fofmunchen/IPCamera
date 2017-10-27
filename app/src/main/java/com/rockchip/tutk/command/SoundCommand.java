package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/7/4.
 */
public class SoundCommand extends CommandBase implements JsonInterface {
    boolean mSound;

    public SoundCommand(boolean mSound) {
        this.mSound = mSound;
    }

    @Override
    public String Json() {
        JSONObject jsonObject=new JSONObject();
        try {
            jsonObject.put("name","Sound");
            jsonObject.put("value",mSound);
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
