package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2016/7/7.
 */
public class RecordVideo extends CommandBase implements  JsonInterface {
    boolean mRecordVideo;

    public RecordVideo(boolean mRecordVideo) {
        this.mRecordVideo = mRecordVideo;
    }

    @Override
    public String Json() {
        JSONObject jsonObject=new JSONObject();
        try {
            jsonObject.put("name","Record");
            jsonObject.put("value",mRecordVideo);
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
