package com.rockchip.tutk.command;

import com.google.gson.Gson;

import java.io.Serializable;

/**
 * Created by waha on 2017/5/9.
 */

public class Mp4CutRequest extends CommandBase
        implements JsonInterface, Serializable {
    private String name;
    private String value;

    public Mp4CutRequest(String value) {
        this.name = "Mp4FileCut";
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
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
