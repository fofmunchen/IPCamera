package com.rockchip.tutk.view;

/**
 * Created by waha on 2017/3/6.
 */

import com.rockchip.tutk.model.RecordModel;

public interface OnRecordSelectedListener {
    void play(RecordModel model);

    void del(RecordModel model);

    void download(RecordModel model);
}
