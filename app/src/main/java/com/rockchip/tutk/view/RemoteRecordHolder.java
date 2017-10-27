package com.rockchip.tutk.view;

import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.model.RecordModel;
import com.rockchip.tutk.utils.AsyncLoadRemotepicTask;
import com.rockchip.tutk.utils.MsgDatas;

/**
 * Created by waha on 2017/4/6.
 */

public class RemoteRecordHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private TextView txtName;
    private ImageView imgThumb;
    private ImageView imgDel;
    private ImageView imgDownload;
    private OnRecordSelectedListener mListener;
    private RecordModel mModel;
    private AsyncLoadRemotepicTask mTask;

    public RemoteRecordHolder(View itemView, OnRecordSelectedListener listener) {
        super(itemView);

        mListener = listener;
        imgThumb = (ImageView) itemView.findViewById(R.id.img_thumb);
        txtName = (TextView) itemView.findViewById(R.id.txt_name);
        imgDel = (ImageView) itemView.findViewById(R.id.img_del);
        imgDownload = (ImageView) itemView.findViewById(R.id.img_download);
        //itemView.setOnClickListener(this);
    }

    public void setData(RecordModel model, TUTKDevice device) {
        mModel = model;
        txtName.setText(model.getDisplayName().replaceAll(".mp4", ""));
        imgDel.setOnClickListener(this);
        txtName.setOnClickListener(this);
        imgDownload.setOnClickListener(this);
        imgThumb.setImageResource(R.drawable.movie);
        if (model.isExist()) {
            imgDownload.setVisibility(View.INVISIBLE);
        } else {
            imgDownload.setVisibility(View.VISIBLE);
        }
        if (null != device) {
            imgThumb.setTag(model.getThumbnailPath());
            if (null != mTask) {
                mTask.cancel(true);
            }
            mTask = new AsyncLoadRemotepicTask(imgThumb, device);
            mTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, model.getThumbnailPath());
        }
    }

    @Override
    public void onClick(View v) {
        if (null == mListener) {
            return;
        }
        if (R.id.img_del == v.getId()) {
            mListener.del(mModel);
        } else if (R.id.img_download == v.getId()) {
            mListener.download(mModel);
        } else if (R.id.txt_name == v.getId()) {
            mListener.play(mModel);
        } else {
            mListener.play(mModel);
        }
    }
}
