package com.rockchip.tutk.command;

import java.io.Serializable;

/**
 * Created by waha on 2017/2/23.
 */

public class UpdateResponse extends CommandBase
        implements JsonInterface, Serializable {
    private String name;
    private String value;
    private long crc;
    private long FileSize;
    private String FileCheckStatus;
    private String reason;
    private boolean result;

    public UpdateResponse() {
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getFileCheckStatus() {
        return FileCheckStatus;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public void setFileCheckStatus(String fileCheckStatus) {
        FileCheckStatus = fileCheckStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }

    public long getCrc() {
        return crc;
    }

    public void setCrc(long crc) {
        this.crc = crc;
    }

    public long getFileSize() {
        return FileSize;
    }

    public void setFileSize(long fileSize) {
        FileSize = fileSize;
    }

    @Override
    public String Json() {
        return null;
    }
}
