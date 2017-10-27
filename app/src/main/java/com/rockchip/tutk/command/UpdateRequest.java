package com.rockchip.tutk.command;

import com.google.gson.Gson;

import java.io.Serializable;

/**
 * Created by waha on 2017/2/23.
 */

public class UpdateRequest extends CommandBase
        implements JsonInterface, Serializable {
    private String name;
    private String value;
    private long crc;
    private long FileSize;
    private boolean result;
    private boolean write2storage;

    public UpdateRequest(String value, long crc, long fileSize) {
        this.name = "FileDownload";
        this.value = value;
        this.crc = crc;
        this.FileSize = fileSize;
        this.type = "Action";
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public long getCrc() {
        return crc;
    }

    public long getFileSize() {
        return FileSize;
    }

    public boolean isWrite2storage() {
        return write2storage;
    }

    public boolean isResult() {
        return result;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setCrc(long crc) {
        this.crc = crc;
    }

    public void setFileSize(long fileSize) {
        FileSize = fileSize;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public void setWrite2storage(boolean write2storage) {
        this.write2storage = write2storage;
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
