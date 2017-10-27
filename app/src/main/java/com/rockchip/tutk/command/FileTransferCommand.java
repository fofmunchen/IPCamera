package com.rockchip.tutk.command;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by qiujian on 2017/1/22.
 */

public class FileTransferCommand extends CommandBase {

    String path;

    public FileTransferCommand(String path) {
        this.path = path;
    }

    @Override
    public String Json() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", "FileTransfer");
            jsonObject.put("value", path);
            jsonObject.put("type",type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
