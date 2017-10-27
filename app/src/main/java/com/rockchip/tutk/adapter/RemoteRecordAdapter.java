package com.rockchip.tutk.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.model.RecordModel;
import com.rockchip.tutk.view.FootViewHolder;
import com.rockchip.tutk.view.OnRecordSelectedListener;
import com.rockchip.tutk.view.RemoteRecordHolder;

import java.util.List;

/**
 * Created by waha on 2017/4/5.
 */

public class RemoteRecordAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;
    private List<RecordModel> mDatas;
    private OnRecordSelectedListener mListener;
    private boolean mIsLoading;
    private Object mLock = new Object();
    private TUTKDevice mTutkDevice;

    public RemoteRecordAdapter(List<RecordModel> datas, OnRecordSelectedListener listener,
                               TUTKDevice tutkDevice) {
        mDatas = datas;
        mListener = listener;
        mTutkDevice = tutkDevice;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflate = LayoutInflater.from(parent.getContext());
        if (TYPE_ITEM == viewType) {
            View v = inflate.inflate(R.layout.remote_record_item, parent, false);
            return new RemoteRecordHolder(v, mListener);
        } else if (TYPE_FOOTER == viewType) {
            View v = inflate.inflate(R.layout.list_foot_view, parent, false);
            return new FootViewHolder(v);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (null == mDatas) {
            return;
        }
        RecordModel model = mDatas.get(position);
        if (TYPE_ITEM == model.getItemType()) {
            ((RemoteRecordHolder) holder).setData(model, mTutkDevice);
        }
    }

    @Override
    public int getItemCount() {
        return null == mDatas ? 0 : mDatas.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (null == mDatas) {
            return super.getItemViewType(position);
        }
        return mDatas.get(position).getItemType();
    }

    public void addMoreItem() {
        synchronized (mLock) {
            if (!mIsLoading) {
                mIsLoading = true;
                RecordModel model = new RecordModel();
                model.setItemType(TYPE_FOOTER);
                mDatas.add(mDatas.size(), model);
                notifyDataSetChanged();
            }
        }
    }

    public void finishLoad() {
        mIsLoading = false;
    }

    public void removeFoot() {
        synchronized (mLock) {
            if (null != mDatas && mDatas.size() > 0) {
                if (TYPE_FOOTER == mDatas.get(mDatas.size() - 1).getItemType()) {
                    mDatas.remove(mDatas.size() - 1);
                }
            }
        }
    }
}