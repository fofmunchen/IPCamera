package com.rockchip.tutk.model;

/**
 * Created by waha on 2017/5/6.
 */

public class Mp4Info {
    private long ftypSize;
    private long freeSize;
    private long mdatSize;
    private long moovSize;
    private long footSize;
    private long totalSize;
    private long mdatDownloadedSize;
    private boolean result;

    public long getFtypSize() {
        return ftypSize;
    }

    public void setFtypSize(long ftypSize) {
        this.ftypSize = ftypSize;
    }

    public long getFreeSize() {
        return freeSize;
    }

    public void setFreeSize(long freeSize) {
        this.freeSize = freeSize;
    }

    public long getMdatSize() {
        return mdatSize;
    }

    public void setMdatSize(long mdatSize) {
        this.mdatSize = mdatSize;
    }

    public long getMoovSize() {
        return moovSize;
    }

    public void setMoovSize(long moovSize) {
        this.moovSize = moovSize;
    }

    public long getFootSize() {
        return footSize;
    }

    public void setFootSize(long footSize) {
        this.footSize = footSize;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getDownLoadedMdat() {
        return mdatDownloadedSize;
    }

    public void addDownLoadedMdat(long download) {
        mdatDownloadedSize += download;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public long getHeadSize() {
        return ftypSize + freeSize;
    }

    public long getUsefulSize() {
        return ftypSize + freeSize + mdatSize + moovSize;
    }

    public long getMoovStartPos() {
        return ftypSize + freeSize + mdatSize;
    }

}
