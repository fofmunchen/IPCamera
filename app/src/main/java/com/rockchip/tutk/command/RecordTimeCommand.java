package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/6/28.
 */
public class RecordTimeCommand extends CommandBase implements JsonInterface {
    String mRecordTime;

    public RecordTimeCommand(String mRecordTime) {
        this.mRecordTime = mRecordTime;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "RecordTime");
            jsonObject.put("value", Integer.valueOf(mRecordTime));
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
