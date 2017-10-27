package com.rockchip.tutk.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rockchip.tutk.R;
import com.rockchip.tutk.model.RecordModel;
import com.rockchip.tutk.view.OnRecordSelectedListener;
import com.rockchip.tutk.view.VideoViewHolder;

import java.util.List;

/**
 * Created by waha on 2017/3/6.
 */

public class RecordAdapter extends RecyclerView.Adapter<VideoViewHolder> {
    private List<RecordModel> mDatas;
    private OnRecordSelectedListener mListener;

    public RecordAdapter(List<RecordModel> datas, OnRecordSelectedListener listener) {
        mDatas = datas;
        mListener = listener;
    }

    @Override
    public VideoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflate = LayoutInflater.from(parent.getContext());
        View v = inflate.inflate(R.layout.video_item_layout, parent, false);
        return new VideoViewHolder(v, mListener);
    }

    @Override
    public void onBindViewHolder(VideoViewHolder holder, int position) {
        if (null != mDatas) {
            holder.setData(mDatas.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return null == mDatas ? 0 : mDatas.size();
    }
}
