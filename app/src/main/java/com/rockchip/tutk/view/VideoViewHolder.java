package com.rockchip.tutk.view;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.rockchip.tutk.R;
import com.rockchip.tutk.model.RecordModel;
import com.rockchip.tutk.utils.AsyncLoadpicTask;
import com.rockchip.tutk.utils.MsgDatas;

/**
 * Created by waha on 2017/3/6.
 */

public class VideoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private TextView txtName;
    private ImageView imgThumb;
    private ImageView imgDel;
    private OnRecordSelectedListener mListener;
    private RecordModel mModel;
    private AsyncLoadpicTask mTask;

    public VideoViewHolder(View itemView, OnRecordSelectedListener listener) {
        super(itemView);

        mListener = listener;
        imgThumb = (ImageView) itemView.findViewById(R.id.img_thumb);
        txtName = (TextView) itemView.findViewById(R.id.txt_name);
        imgDel = (ImageView) itemView.findViewById(R.id.img_del);
        //itemView.setOnClickListener(this);
    }

    public void setData(RecordModel model) {
        mModel = model;
        txtName.setText(model.getDisplayName());
        imgDel.setOnClickListener(this);
        txtName.setOnClickListener(this);
        imgThumb.setImageResource(MsgDatas.TYPE_IMAGE == model.getType() ? R.drawable.image : R.drawable.movie);
        imgThumb.setTag(model.getPath());
        if (null != mTask) {
            mTask.cancel(true);
        }
        mTask = new AsyncLoadpicTask(imgThumb);
        mTask.execute(model.getPath(), String.valueOf(model.getType()));
    }

    @Override
    public void onClick(View v) {
        if (null == mListener) {
            return;
        }
        if (R.id.img_del == v.getId()) {
            mListener.del(mModel);
        } else if (R.id.txt_name == v.getId()) {
            mListener.play(mModel);
        } else {
            mListener.play(mModel);
        }
    }
}
