package com.rockchip.tutk.model;

import com.rockchip.tutk.adapter.RemoteRecordAdapter;

/**
 * Created by waha on 2017/3/6.
 */

public class RecordModel {
    private String path;
    private String displayName;
    private int type; //pic/video see MsgDatas.class or md/nomal
    private int ItemType = RemoteRecordAdapter.TYPE_ITEM;
    private boolean exist;
    private String thumbnailPath;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public int getItemType() {
        return ItemType;
    }

    public void setItemType(int itemType) {
        ItemType = itemType;
    }

    public boolean isExist() {
        return exist;
    }

    public void setExist(boolean exist) {
        this.exist = exist;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }
}
