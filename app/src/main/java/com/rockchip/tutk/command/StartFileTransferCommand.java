package com.rockchip.tutk.command;

import com.google.gson.Gson;

/**
 * Created by qiujian on 2017/1/22.
 */

public class StartFileTransferCommand extends CommandBase {
    private String value;
    private long startPos;
    private long endPos;
    private String name = "StartFileTransfer";

    public StartFileTransferCommand(String path) {
        this(path, 0, 0);
    }

    public StartFileTransferCommand(String path, long startPos, long endPos) {
        this.value = path;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public long getStartPos() {
        return startPos;
    }

    public void setStartPos(long startPos) {
        this.startPos = startPos;
    }

    public long getEndPos() {
        return endPos;
    }

    public void setEndPos(long endPos) {
        this.endPos = endPos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String Json() {
        Gson gson = new Gson();
        try {
            return gson.toJson(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
