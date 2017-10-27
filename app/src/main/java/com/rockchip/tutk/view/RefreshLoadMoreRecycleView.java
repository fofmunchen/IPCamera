package com.rockchip.tutk.view;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class RefreshLoadMoreRecycleView extends RecyclerView implements RecyclerView.OnTouchListener {
    private boolean isLoadMore;//加载更多标志
    private boolean isLoadEnd;//加载到最后的标志
    private boolean isLoadStart;//顶部的标志
    private boolean isRefresh;//下拉刷新标志
    private int lastVisibleItem;//最后一项
    private IOnScrollListener listener;//事件监听
    private float mLastY;//监听移动的位置

    public RefreshLoadMoreRecycleView(Context context) {
        this(context, null);
    }

    public RefreshLoadMoreRecycleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshLoadMoreRecycleView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void init(Context context) {
        isLoadEnd = false;
        isLoadStart = true;

        this.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                //SCROLL_STATE_DRAGGING  和   SCROLL_STATE_IDLE 两种效果自己看着来
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    loadData();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //上滑
                if (dy > 0) {
                    //是否滑到底部
                    if (!recyclerView.canScrollVertically(1)) {
                        isLoadEnd = true;
                    } else {
                        isLoadEnd = false;
                    }
                } else if (dy < 0) {
                    //是否滑到顶部
                    if (!recyclerView.canScrollVertically(-1)) {
                        isLoadStart = true;
                    } else {
                        isLoadStart = false;
                    }

                }
            }
        });
        this.setOnTouchListener(this);
    }


    private void loadData() {
        if (isLoadEnd) {
            // 判断是否已加载所有数据
            if (isLoadMore) {//未加载完所有数据，加载数据，并且还原isLoadEnd值为false，重新定位列表底部
                if (getListener() != null) {
                    getListener().onLoadMore();
                }
            } else {//加载完了所有的数据
                if (getListener() != null) {
                    getListener().onLoaded();
                }
            }
            isLoadEnd = false;
        } else if (isLoadStart) {
            if (isRefresh) {
                if (getListener() != null) {
                    getListener().onRefresh();
                }
                isLoadStart = false;
            }
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if (mLastY == -1) {
            mLastY = motionEvent.getRawY();
        }
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_MOVE:
                final float deltaY = motionEvent.getRawY() - mLastY;
                mLastY = motionEvent.getRawY();
                //向上移动
                if (deltaY < 0) {
                    //是否滑到底部
                    if (!this.canScrollVertically(1)) {
                        isLoadEnd = true;
                    } else {
                        isLoadEnd = false;
                    }
                }
                //向下移动
                else if (deltaY > 0) {
                    //是否滑到顶部
                    if (!this.canScrollVertically(-1)) {
                        isLoadStart = true;
                    } else {
                        isLoadStart = false;
                    }
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mLastY = motionEvent.getRawY();
                break;
            default://重置
                mLastY = -1;
                break;
        }

        return false;
    }

    //事件监听
    public interface IOnScrollListener {
        void onRefresh();

        void onLoadMore();

        void onLoaded();
    }

    public IOnScrollListener getListener() {
        return listener;
    }

    //设置事件监听
    public void setListener(IOnScrollListener listener) {
        this.listener = listener;
    }

    public Boolean getLoadMore() {
        return isLoadMore;
    }

    //设置是否支持加载更多
    public void setLoadMoreEnable(Boolean loadMore) {
        isLoadMore = loadMore;
    }

    public Boolean getRefresh() {
        return isRefresh;
    }

    //设置是否支持下拉刷新
    public void setRefreshEnable(Boolean refresh) {
        isRefresh = refresh;
    }
}
