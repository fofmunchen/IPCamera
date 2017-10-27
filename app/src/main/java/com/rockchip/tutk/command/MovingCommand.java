package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/6/28.
 */
public class MovingCommand extends CommandBase implements JsonInterface {
    boolean mMoving;

    public MovingCommand(boolean mMoving) {
        this.mMoving = mMoving;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "Moving");
            jsonObject.put("value", mMoving);
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
