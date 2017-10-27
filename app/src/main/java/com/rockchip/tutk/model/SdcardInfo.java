package com.rockchip.tutk.model;

/**
 * Created by waha on 2017/4/7.
 */

public class SdcardInfo {
    private boolean isExist;
    private long freeSize;
    private long totalSize;

    public boolean isExist() {
        return isExist;
    }

    public void setExist(boolean exist) {
        isExist = exist;
    }

    public long getFreeSize() {
        return freeSize;
    }

    public void setFreeSize(long freeSize) {
        this.freeSize = freeSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public String getDesc() {
        StringBuilder sb = new StringBuilder();
        if (isExist) {
            long usedSize = totalSize-freeSize;
            if (usedSize > 1024) {
                sb.append(String.format("%.2f", ((float) usedSize) / 1024) + "G");
            } else {
                sb.append(usedSize + "M");
            }
            sb.append("/");
            if (totalSize > 1024) {
                sb.append(String.format("%.2f", ((float) totalSize) / 1024) + "G");
            } else {
                sb.append(totalSize + "M");
            }
        }
        return sb.toString();
    }
}
