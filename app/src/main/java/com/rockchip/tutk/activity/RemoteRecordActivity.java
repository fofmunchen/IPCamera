package com.rockchip.tutk.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKDevice.RDTChannelUsedStatus;
import com.rockchip.tutk.TUTKManager;
import com.rockchip.tutk.adapter.RemoteRecordAdapter;
import com.rockchip.tutk.command.DelFileCommand;
import com.rockchip.tutk.command.FileTransferCommand;
import com.rockchip.tutk.command.StartFileTransferCommand;
import com.rockchip.tutk.model.MdNotifyInfo;
import com.rockchip.tutk.model.RecordModel;
import com.rockchip.tutk.model.SdcardModel;
import com.rockchip.tutk.utils.MsgDatas;
import com.rockchip.tutk.utils.RecordUtils;
import com.rockchip.tutk.view.DateTimePickDialog;
import com.rockchip.tutk.view.LoadingView;
import com.rockchip.tutk.view.OnRecordSelectedListener;
import com.rockchip.tutk.view.RefreshLoadMoreRecycleView;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.RDTAPIs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.support.v7.widget.RecyclerView.*;
import static com.tutk.IOTC.AVIOCTRLDEFs.RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ;

/**
 * Created by waha on 2017/4/1.
 */

public class RemoteRecordActivity extends AppCompatActivity implements
        TUTKDevice.OnTutkError, TUTKDevice.NotifyCallback,
        View.OnClickListener, DialogInterface.OnClickListener,
        RefreshLoadMoreRecycleView.IOnScrollListener,
        OnRecordSelectedListener {
    private static final String TAG = "RemoteRecord";
    private TUTKDevice mTUTKDevice;
    private LoadingView mLoadingView;
    private RefreshLoadMoreRecycleView videoView;
    private RemoteRecordAdapter mAdapter;
    private List<RecordModel> mList = new ArrayList<>();
    private TextView txtStartTime;
    private TextView txtEndTime;
    private TextView txtEmpty;
    private DateTimePickDialog mDateDialog;
    private RecordModel mDelModel;
    private RecordModel mDownloadModel;
    private RecordModel mPlayModel;
    private int mType = MsgDatas.TYPE_NOMAL;
    private DownLoadThread mDownLoadThread;
    private AlertDialog mPlayItemDialog;
    private int mRdtId = -1;
    private Object mLock = new Object();
    private boolean mCancel;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MsgDatas.MSG_SERVER_RECORD_START: {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("name", "GetRecordList");
                        jsonObject.put("start_time",
                                txtStartTime.getText().toString().replace("-", ""));
                        jsonObject.put("end_time",
                                txtEndTime.getText().toString().replace("-", ""));
                        jsonObject.put("page_count", 20);
                        jsonObject.put("last_name", getLastFileName());
                        jsonObject.put("type", mType);
                        sendMsg2Remote(jsonObject.toString(), MsgDatas.MSG_SERVER_RECORD_TIMEOUT);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case MsgDatas.MSG_SERVER_RECORD_TIMEOUT: {
                    if (null != mAdapter) {
                        mAdapter.finishLoad();
                    }
                    if (null != mLoadingView && mLoadingView.isShowing()) {
                        mLoadingView.cancel();
                        if (!mCancel) {
                            Toast.makeText(RemoteRecordActivity.this, R.string.network_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                }
                case MsgDatas.MSG_SERVER_RECORD_FAILED: {
                    if (null != mAdapter) {
                        mAdapter.finishLoad();
                    }
                    if (null != mLoadingView && mLoadingView.isShowing()) {
                        mLoadingView.cancel();
                        //Toast.makeText(RemoteRecordActivity.this,
                        //        R.string.txt_get_info_failed, Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
                case MsgDatas.MSG_SERVER_RECORD_SUCCESS: {
                    if (null != mAdapter) {
                        mAdapter.finishLoad();
                        mAdapter.notifyDataSetChanged();
                    }
                    if (null != mLoadingView && mLoadingView.isShowing()) {
                        mLoadingView.cancel();
                    }
                    if (null != mList && mList.size() > 0) {
                        txtEmpty.setVisibility(View.GONE);
                        videoView.setVisibility(View.VISIBLE);
                    } else {
                        txtEmpty.setVisibility(View.VISIBLE);
                        videoView.setVisibility(View.GONE);
                    }
                    break;
                }
                case MsgDatas.MSG_DEL_FILE_TIMEOUT: {
                    if (null != mLoadingView && mLoadingView.isShowing()) {
                        mLoadingView.cancel();
                    }
                    Toast.makeText(RemoteRecordActivity.this,
                            R.string.network_failed, Toast.LENGTH_SHORT).show();
                    break;
                }
                case MsgDatas.MSG_DEL_FILE_SUCCESS: {
                    synchronized (mLock) {
                        if (null != mLoadingView && mLoadingView.isShowing()) {
                            mLoadingView.cancel();
                        }
                        int delPosition = -1;
                        for (int i = mList.size() - 1; i >= 0; i--) {
                            if (mList.get(i).equals(mDelModel)) {
                                delPosition = i;
                                break;
                            }
                        }
                        if (-1 != delPosition) {
                            mAdapter.notifyItemRemoved(delPosition);
                            mList.remove(delPosition);
                            mAdapter.notifyItemRangeChanged(delPosition, mAdapter.getItemCount());
                        }
                        mDelModel = null;
                    }
                    break;
                }
                case MsgDatas.MSG_DEL_FILE_FAILED: {
                    if (null != mLoadingView && mLoadingView.isShowing()) {
                        mLoadingView.cancel();
                    }
                    Toast.makeText(RemoteRecordActivity.this,
                            R.string.del_file_failed, Toast.LENGTH_SHORT).show();
                    break;
                }
                case MsgDatas.MSG_DOWNLOAD_FINISH: {
                    synchronized (mLock) {
                        String toast = msg.obj.toString();
                        if (!mCancel && !TextUtils.isEmpty(toast)) {
                            Toast.makeText(RemoteRecordActivity.this, toast, Toast.LENGTH_SHORT).show();
                        } else if (!mCancel) {
                            int position = -1;
                            if (null != mDownloadModel) {
                                for (int i = mList.size() - 1; i >= 0; i--) {
                                    if (mList.get(i).equals(mDownloadModel)) {
                                        position = i;
                                        break;
                                    }
                                }
                                mDownloadModel.setExist(true);
                                if (-1 != position) {
                                    mAdapter.notifyItemRangeChanged(position, mAdapter.getItemCount());
                                }
                                mDownloadModel = null;
                            }
                        }
                        if (null != mLoadingView && mLoadingView.isShowing()) {
                            mLoadingView.cancel();
                        }
                    }
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        init();
        initUI();
        initData();
    }

    private String getLastFileName() {
        String name = "";
        if (null != mList && mList.size() > 0) {
            for (int i = mList.size() - 1; i >= 0; i--) {
                if (!TextUtils.isEmpty(mList.get(i).getDisplayName())) {
                    name = mList.get(i).getDisplayName();
                    break;
                }
            }
        }
        return name;
    }

    private void init() {
        Intent intent = getIntent();
        if (intent == null) return;

        String uid = intent.getStringExtra(TUTKManager.TUTK_UID);
        if (uid != null){// && uid.length() == 20) {
            mTUTKDevice = TUTKManager.getByUID(uid);
        }
        if (null == mTUTKDevice) {
            Toast.makeText(this,
                    R.string.txt_get_info_failed, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            mTUTKDevice.addListener(this);
        }

        String deviceName = intent.getStringExtra(TUTKManager.TUTK_DEVICE_NAME);
        if (deviceName != null && deviceName.length() > 0) {
            if (null != getSupportActionBar()) {
                getSupportActionBar().setTitle(deviceName);
            }
        }
    }

    private void initUI() {
        txtEmpty = (TextView) findViewById(R.id.txt_empty);
        videoView = (RefreshLoadMoreRecycleView) findViewById(R.id.video_list);
        videoView.setListener(this);
        LayoutManager lm = new LinearLayoutManager(this, VERTICAL, false);
        videoView.setHasFixedSize(true);
        videoView.addItemDecoration(new DividerItemDecoration(this,
                LinearLayoutManager.VERTICAL));
        videoView.setLayoutManager(lm);
        mAdapter = new RemoteRecordAdapter(mList, this, mTUTKDevice);
        videoView.setAdapter(mAdapter);
        videoView.setLoadMoreEnable(true);
        txtStartTime = (TextView) findViewById(R.id.txt_startTime);
        txtEndTime = (TextView) findViewById(R.id.txt_endTime);
        txtStartTime.setOnClickListener(this);
        txtEndTime.setOnClickListener(this);
        findViewById(R.id.img_search).setOnClickListener(this);
    }

    private void initData() {
        mCancel = false;
        mDateDialog = new DateTimePickDialog(this);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String curTime = format.format(new Date());
        txtStartTime.setText(curTime);
        txtEndTime.setText(curTime);

        mList.clear();
        RecordUtils.mkdirPath(RecordUtils.getDownloadPath());
        refresh();
    }

    private void refresh() {
        if (null == mLoadingView || !mLoadingView.isShowing()) {
            mLoadingView = new LoadingView(this, getResources().getString(R.string.txt_getting_info));
            mLoadingView.show();
        }
        mHandler.removeMessages(MsgDatas.MSG_SERVER_RECORD_START);
        mHandler.sendEmptyMessageDelayed(MsgDatas.MSG_SERVER_RECORD_START, 800);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (mType == MsgDatas.TYPE_NOMAL) {
            menu.add(0, Menu.FIRST, 0, R.string.txt_video_check)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        } else {
            menu.add(0, Menu.FIRST, 0, R.string.txt_video)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if (mType == MsgDatas.TYPE_MD) {
            menu.add(0, Menu.FIRST + 1, 0, R.string.txt_md_check)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        } else {
            menu.add(0, Menu.FIRST + 1, 0, R.string.txt_md)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == Menu.FIRST) {
            mType = MsgDatas.TYPE_NOMAL;
            mList.clear();
            refresh();
            supportInvalidateOptionsMenu();
            return true;
        } else if (item.getItemId() == Menu.FIRST + 1) {
            mType = MsgDatas.TYPE_MD;
            mList.clear();
            refresh();
            supportInvalidateOptionsMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        if (mTUTKDevice != null) {
            mTUTKDevice.addOnTutkErrorListener(this);
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        if (null != mTUTKDevice) {
            mTUTKDevice.removeOnTutkErrorListener(this);
        }
        if (null != mLoadingView && mLoadingView.isShowing()) {
            mLoadingView.cancel();
            mLoadingView = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mCancel = true;
        mTUTKDevice.removeListener(this);
        //mTUTKDevice.close();
        mHandler.removeMessages(MsgDatas.MSG_SERVER_RECORD_TIMEOUT);
        mHandler.removeMessages(MsgDatas.MSG_DEL_FILE_TIMEOUT);
        if (null != mDownLoadThread) {
            mDownLoadThread.setCancel(true);
        }
        if (mRdtId > -1) {
            new Thread() {
                @Override
                public void run() {
                    int ret = RDTAPIs.RDT_Abort(mRdtId);
                    Log.v(TAG, "ondestory RDT_Abort rdtid:" + mRdtId + ",ret=" + ret);
                    mRdtId = -1;
                    //mTUTKDevice.getSession().setRdtId(-1);
                }
            }.start();
        }
        if (null != mTUTKDevice) {
            RDTChannelUsedStatus rdtChannelStatus = mTUTKDevice.getRdtChannelUsedStatus();
            if (RDTChannelUsedStatus.CHANNEL_DOWNLOAD_MP4 == rdtChannelStatus
                    || RDTChannelUsedStatus.CHANNEL_DOWNLOAD_MP4_THUMBS == rdtChannelStatus) {
                mTUTKDevice.setRdtChannelUsed(RDTChannelUsedStatus.CHANNEL_FREE);
            }
        }
        super.onDestroy();
    }

    @Override
    public void onError(final int code) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RemoteRecordActivity.this, String.format("与设备断开连接 [%d]", code), Toast.LENGTH_SHORT).show();
            }
        });
        setResult(Activity.RESULT_OK);
        finish();
    }

    @Override
    public void play(RecordModel model) {
        mPlayModel = model;
        showPlayItem(model);
    }

    private void showPlayItem(RecordModel model) {
        if (null != mPlayItemDialog) {
            mPlayItemDialog.cancel();
        }
        String[] ss = null;
        if (!model.isExist()) {
            ss = new String[]{
                    getResources().getString(R.string.play_area_net),
                    getResources().getString(R.string.play_remote),
                    getResources().getString(R.string.txt_cancel),
            };
        } else {
            ss = new String[]{
                    //getResources().getString(R.string.play_remote),
                    getResources().getString(R.string.play_local),
                    getResources().getString(R.string.del_download),
                    getResources().getString(R.string.txt_cancel),
            };
        }
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.CYAN);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        for (int i = 0; i < ss.length; i++) {
            if (i != 0) {
                params.setMargins(0, 1, 0, 0);
            }
            TextView txtView = new TextView(this);
            txtView.setPadding(25, 30, 25, 30);
            txtView.setTextColor(Color.GRAY);
            txtView.setTextSize(18);
            txtView.setText(ss[i]);
            txtView.setTag(ss[i]);
            txtView.setGravity(Gravity.CENTER);
            txtView.setLayoutParams(params);
            txtView.setBackgroundColor(Color.WHITE);
            layout.addView(txtView);
            txtView.setOnClickListener(this);
        }
        mPlayItemDialog = new AlertDialog.Builder(this).setView(layout).show();
    }

    @Override
    public void del(final RecordModel model) {
        mDelModel = model;
        new AlertDialog.Builder(this).setMessage(getResources().getString(R.string.warn_del_file))
                .setPositiveButton(android.R.string.yes, this)
                .setNegativeButton(android.R.string.no, this).show();
    }

    @Override
    public void download(RecordModel model) {
        mDownloadModel = model;
        downLoadFile(model);
    }

    private void downLoadFile(RecordModel model) {
        if (null == mLoadingView || !mLoadingView.isShowing()) {
            mLoadingView = new LoadingView(this, getResources().getString(R.string.downloading_file));
            mLoadingView.show();
        }
        if (null != mDownLoadThread) {
            mDownLoadThread.setCancel(true);
            if (null != mTUTKDevice && RDTChannelUsedStatus.CHANNEL_DOWNLOAD_MP4
                    == mTUTKDevice.getRdtChannelUsedStatus()) {
                mTUTKDevice.setRdtChannelUsed(RDTChannelUsedStatus.CHANNEL_FREE);
            }
        }
        String downLoadPath = RecordUtils.getDownloadPath();
        boolean ret = RecordUtils.mkdirPath(downLoadPath);
        if (!ret) {
            Toast.makeText(this, R.string.crete_file_error, Toast.LENGTH_SHORT).show();
            mLoadingView.cancel();
            return;
        }
        mDownLoadThread = new DownLoadThread(model.getPath(), downLoadPath + model.getDisplayName());
        mDownLoadThread.start();
    }

    @Override
    public void onEvent(MdNotifyInfo notifyInfo) {

    }

    @Override
    public void onPictureTaked(String patch) {

    }

    @Override
    public void onSdcardAlarm(SdcardModel model) {

    }

    @Override
    public void onPlayConfigLoaded(int msgWhat, boolean result) {

    }

    @Override
    public void onRecordListLoaded(String json) {
        try {
            if (null != mAdapter) {
                mAdapter.removeFoot();
            }
            JSONObject jsonObject = new JSONObject(json);
            if (!jsonObject.getBoolean("result")) {
                mHandler.sendEmptyMessage(MsgDatas.MSG_SERVER_RECORD_FAILED);
                mHandler.removeMessages(MsgDatas.MSG_SERVER_RECORD_TIMEOUT);
                return;
            } else if (jsonObject.getInt("count") > 0) {
                JSONArray item_list = jsonObject.getJSONArray("item_list");
                String dirPath = jsonObject.getString("dirpath");
                for (int i = 0; i < item_list.length(); i++) {
                    JSONObject item = item_list.getJSONObject(i);
                    RecordModel model = new RecordModel();
                    model.setDisplayName(item.getString("file_name"));
                    model.setType(mType);
                    model.setPath(dirPath + "/" + model.getDisplayName());
                    String thumbnailsPath = "/mnt/sdcard/.MISC/.THUMB-FRONT/";
                    if (mType == MsgDatas.TYPE_MD) {
                        thumbnailsPath = "/mnt/sdcard/.MISC/.THUMB-LOCK-FRONT/";
                    }
                    model.setThumbnailPath(thumbnailsPath + model.getDisplayName().replace(".mp4", ".jpg"));
                    File file = new File(RecordUtils.getDownloadPath() + model.getDisplayName());
                    if (file.exists()) {
                        model.setExist(true);
                    }
                    mList.add(model);
                }
            }
            mHandler.sendEmptyMessage(MsgDatas.MSG_SERVER_RECORD_SUCCESS);
            mHandler.removeMessages(MsgDatas.MSG_SERVER_RECORD_TIMEOUT);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDelFile(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.getBoolean("result")) {
                mHandler.removeMessages(MsgDatas.MSG_DEL_FILE_TIMEOUT);
                mHandler.sendEmptyMessage(MsgDatas.MSG_DEL_FILE_SUCCESS);
            } else {
                mHandler.removeMessages(MsgDatas.MSG_DEL_FILE_TIMEOUT);
                mHandler.sendEmptyMessage(MsgDatas.MSG_DEL_FILE_FAILED);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.txt_startTime: {
                mDateDialog.showDialog(txtStartTime);
                break;
            }
            case R.id.txt_endTime: {
                mDateDialog.showDialog(txtEndTime);
                break;
            }
            case R.id.img_search: {
                mList.clear();
                refresh();
                break;
            }
            default:
                if (v instanceof TextView) {
                    dealPlayClick((TextView) v);
                }
                break;
        }
    }

    private void dealPlayClick(TextView v) {
        if (null == v.getText()) {
            return;
        }
        String title = v.getText().toString();
        if (getResources().getString(R.string.play_remote).equals(title)) {
            if (null != mPlayItemDialog) {
                mPlayItemDialog.cancel();
                mPlayItemDialog = null;
            }
            Intent intent = new Intent(RemoteRecordActivity.this, DownAndPlayActivity.class);
            intent.putExtra(TUTKManager.TUTK_UID, mTUTKDevice.getUID());
            intent.putExtra(DownAndPlayActivity.EXTRA_FILENAME, mPlayModel.getDisplayName());
            intent.putExtra(DownAndPlayActivity.EXTRA_FILEPATH, mPlayModel.getPath());
            startActivityForResult(intent, MsgDatas.REQUEST_RECORD_PLAY);
        } else if (getResources().getString(R.string.play_local).equals(title)) {
            if (null != mPlayItemDialog) {
                mPlayItemDialog.cancel();
                mPlayItemDialog = null;
            }
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse("file:///" + RecordUtils.getDownloadPath() + mPlayModel.getDisplayName());
                intent.setDataAndType(uri, "video/*");
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.not_video_app, Toast.LENGTH_SHORT).show();
            }
        } else if (getResources().getString(R.string.del_download).equals(title)) {
            if (null != mPlayItemDialog) {
                mPlayItemDialog.cancel();
                mPlayItemDialog = null;
            }
            File f = new File(RecordUtils.getDownloadPath() + mPlayModel.getDisplayName());
            if (!f.exists() || f.delete()) {
                mPlayModel.setExist(false);
                int position = -1;
                synchronized (mLock) {
                    if (null != mPlayModel) {
                        for (int i = mList.size() - 1; i >= 0; i--) {
                            if (mList.get(i).equals(mPlayModel)) {
                                position = i;
                                break;
                            }
                        }
                        if (-1 != position) {
                            mAdapter.notifyItemRangeChanged(position, mAdapter.getItemCount());
                        }
                        mPlayModel = null;
                    }
                }
            }
        } else if (getResources().getString(R.string.txt_cancel).equals(title)) {
            if (null != mPlayItemDialog) {
                mPlayItemDialog.cancel();
                mPlayItemDialog = null;
            }
        } else if (getResources().getString(R.string.play_area_net).equals(title)) {
            if (null != mPlayItemDialog) {
                mPlayItemDialog.cancel();
                mPlayItemDialog = null;
            }
            Intent intent = new Intent(this, RecordPlayActivity.class);
            intent.putExtra(TUTKManager.TUTK_UID, mTUTKDevice.getUID());
            intent.putExtra(RecordPlayActivity.EXTRA_FILENAME, mPlayModel.getDisplayName());
            intent.putExtra(RecordPlayActivity.EXTRA_FILETYPE, mType);
            startActivityForResult(intent, MsgDatas.REQUEST_RECORD_PLAY);
        }
    }

    @Override
    public void onRefresh() {

    }

    @Override
    public void onLoadMore() {
        refresh();
    }

    @Override
    public void onLoaded() {

    }

    private void sendMsg2Remote(final String msg, final int msgWhat) {
        new Thread() {
            @Override
            public void run() {
                int write = -1;
                mHandler.sendEmptyMessageDelayed(msgWhat, MsgDatas.NETWORK_TIMEOUT);
                try {
                    byte[] bytes = msg.getBytes();
                    write = AVAPIs.avSendIOCtrl(mTUTKDevice.getSession().getAVIndex(),
                            RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, bytes, bytes.length);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (write != 0) {
                    mHandler.removeMessages(msgWhat);
                    mHandler.sendEmptyMessage(msgWhat);
                }
            }
        }.start();
    }

    private class DownLoadThread extends Thread {
        private boolean cancel;
        private String pathRemote;
        private String pathLocal;

        public void setCancel(boolean cancel) {
            this.cancel = cancel;
        }

        public DownLoadThread(String pathRemote, String pathLocal) {
            this.pathRemote = pathRemote;
            this.pathLocal = pathLocal;
        }

        @Override
        public void run() {
            int countNo = 0;
            while (!cancel) {
                RDTChannelUsedStatus rdtChannelUsedStatus = mTUTKDevice.getRdtChannelUsedStatus();
                if (RDTChannelUsedStatus.CHANNEL_FREE != rdtChannelUsedStatus) {
                    try {
                        Log.v(TAG, "rdt channel is used " + rdtChannelUsedStatus + ", sleep time:" + countNo);
                        countNo++;
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (countNo > 6) {
                        cancel = true;
                    }
                    continue;
                }
                break;
            }
            mTUTKDevice.setRdtChannelUsed(TUTKDevice.RDTChannelUsedStatus.CHANNEL_DOWNLOAD_MP4);
            int ret = cancel ? -1 : downLoad(pathRemote, pathLocal);
            if (mRdtId > -1) {
                int retDestory = RDTAPIs.RDT_Abort(mRdtId);
                Log.v(TAG, "abort rdtid:" + mRdtId + " after doing ret=" + retDestory);
                if (retDestory > -1 || retDestory == RDTAPIs.RDT_ER_INVALID_RDT_ID
                        || retDestory == RDTAPIs.RDT_ER_REMOTE_ABORT) {
                    mTUTKDevice.getSession().setRdtId(-1);
                }
                mRdtId = -1;
            }
            mTUTKDevice.setRdtChannelUsed(RDTChannelUsedStatus.CHANNEL_FREE);
            Message message = new Message();
            message.what = MsgDatas.MSG_DOWNLOAD_FINISH;
            if (ret == 0) {
                message.obj = getResources().getString(R.string.download_failed);
                File file = new File(pathLocal);
                if (file.exists()) {
                    file.delete();
                }
            } else if (ret < 0) {
                File file = new File(pathLocal);
                if (file.exists()) {
                    file.delete();
                }
                message.obj = getResources().getString(R.string.network_failed);
            } else {
                message.obj = "";
            }
            mHandler.sendMessage(message);
        }

        private int downLoad(String pathRemote, String pathLocal) {
            int sid = mTUTKDevice.getSession().getSID();
            if (sid < 0) {
                return -1;
            }
            mRdtId = mTUTKDevice.getSession().getRdtId();
            if (mRdtId >= 0) {
                int ret = RDTAPIs.RDT_Abort(mRdtId);
                Log.w(TAG, "RDT_Abort old rdtid:" + mRdtId + ", ret=" + ret);
                if (ret > -1 || ret == RDTAPIs.RDT_ER_INVALID_RDT_ID
                        || ret == RDTAPIs.RDT_ER_REMOTE_ABORT) {
                    mTUTKDevice.getSession().setRdtId(-1);
                }
            }
            if (cancel) {
                Log.e(TAG, "return become cancel");
                return 1;
            }
            mRdtId = RDTAPIs.RDT_Create(sid, MsgDatas.NETWORK_TIMEOUT * 2, 1);
            if (mRdtId < 0) {
                Log.e(TAG, "doing failed sid=" + sid + " ,mRdtId=" + mRdtId);
                return -1;
            }
            mTUTKDevice.getSession().setRdtId(mRdtId);
            if (cancel) {
                Log.e(TAG, "return become cancel");
                return 1;
            }
            Log.v(TAG, "downloadFile retId=" + mRdtId + ", pathRemote="
                    + pathRemote + ", pathLocal=" + pathLocal);
            OutputStream outputStream = null;
            BufferedOutputStream bufferedOutputStream = null;
            try {
                FileTransferCommand fileTransferCommand = new FileTransferCommand(pathRemote);
                String json = fileTransferCommand.Json();
                int write = RDTAPIs.RDT_Write(mRdtId, json.getBytes(), json.getBytes().length);
                if (write < 0) {
                    Log.e(TAG, "FileTransfer timeout");
                    return write;
                }
                Log.d(TAG, "write:" + write);
                byte[] buff = new byte[1024 * 200];
                int rdt_read = RDTAPIs.RDT_Read(mRdtId, buff, buff.length, MsgDatas.NETWORK_TIMEOUT);
                long fileSize = 0;
                String fileInfo = new String(buff, 0, rdt_read);
                File file;
                JSONArray jsonArray = new JSONArray(fileInfo);
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                fileSize = jsonObject.getLong("FileSize");
                String value = jsonObject.getString("value");
                file = new File(pathLocal);
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();

                StartFileTransferCommand startFileTransferCommand = new StartFileTransferCommand(pathRemote);
                String commad = startFileTransferCommand.Json();
                write = RDTAPIs.RDT_Write(mRdtId, commad.getBytes(), commad.getBytes().length);
                if (write < 0) {
                    Log.e(TAG, "StartFileTransfer timeout");
                    return write;
                }

                long receive = 0;
                if (outputStream == null) {
                    outputStream = new FileOutputStream(file, true);
                    bufferedOutputStream = new BufferedOutputStream(outputStream);
                }
                while (!cancel && receive < fileSize) {
                    int i = RDTAPIs.RDT_Read(mRdtId, buff, buff.length, MsgDatas.NETWORK_TIMEOUT);
                    Log.d(TAG, "receive:" + i);
                    if (i <= 0) {
                        Log.e(TAG, "break receive");
                        return -1;
                    }
                    bufferedOutputStream.write(buff, 0, i);
                    receive += i;
                    Log.d(TAG, "fileSize:" + fileSize + " received=" + receive);
                    if (receive == fileSize) {
                        Log.d(TAG, "receive end");
                        break;
                    }
                }
                return 1;
            } catch (Exception e) {
                Log.e(TAG, "happen error when download");
                e.printStackTrace();
            } finally {
                try {
                    if (null != bufferedOutputStream) {
                        bufferedOutputStream.flush();
                        bufferedOutputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    if (null != outputStream) {
                        outputStream.flush();
                        outputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (DialogInterface.BUTTON_POSITIVE == which && null != mDelModel) {
            if (null == mLoadingView || !mLoadingView.isShowing()) {
                mLoadingView = new LoadingView(this, getResources().getString(R.string.deling_file));
                mLoadingView.show();
            }
            DelFileCommand command = new DelFileCommand(mDelModel.getPath());
            sendMsg2Remote(command.Json(), MsgDatas.MSG_DEL_FILE_TIMEOUT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (MsgDatas.REQUEST_RECORD_PLAY == requestCode) {
            if (Activity.RESULT_OK == resultCode) {
                finish();
            }
        }
    }
}
