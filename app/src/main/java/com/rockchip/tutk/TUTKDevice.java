package com.rockchip.tutk;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.rockchip.tutk.command.FileTransferCommand;
import com.rockchip.tutk.command.SettingsCommand;
import com.rockchip.tutk.command.StartFileTransferCommand;
import com.rockchip.tutk.db.UserInfo;
import com.rockchip.tutk.model.MdNotifyInfo;
import com.rockchip.tutk.model.SdcardModel;
import com.rockchip.tutk.utils.MsgDatas;
import com.rockchip.tutk.utils.RecordUtils;
import com.rockchip.tutk.utils.SharedPreference;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.AVIOCTRLDEFs;
import com.tutk.IOTC.RDTAPIs;
import com.tutk.IOTC.St_SInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.rockchip.tutk.command.GetEncoderParameters;

import static com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_AUDIOSTART;
import static com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_START;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Connect_ByUID_Parallel;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Connect_Stop_BySID;
import static com.tutk.IOTC.IOTCAPIs.IOTC_ER_CAN_NOT_FIND_DEVICE;
import static com.tutk.IOTC.IOTCAPIs.IOTC_ER_CONNECT_IS_CALLING;
import static com.tutk.IOTC.IOTCAPIs.IOTC_ER_DEVICE_NOT_LISTENING;
import static com.tutk.IOTC.IOTCAPIs.IOTC_ER_EXCEED_MAX_SESSION;
import static com.tutk.IOTC.IOTCAPIs.IOTC_ER_FAIL_GET_LOCAL_IP;
import static com.tutk.IOTC.IOTCAPIs.IOTC_ER_FAIL_RESOLVE_HOSTNAME;
import static com.tutk.IOTC.IOTCAPIs.IOTC_ER_NOT_INITIALIZED;
import static com.tutk.IOTC.IOTCAPIs.IOTC_ER_SERVER_NOT_RESPONSE;
import static com.tutk.IOTC.IOTCAPIs.IOTC_ER_TIMEOUT;
import static com.tutk.IOTC.IOTCAPIs.IOTC_ER_UNKNOWN_DEVICE;
import static com.tutk.IOTC.IOTCAPIs.IOTC_ER_UNLICENSE;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Get_Nat_Type;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Get_SessionID;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Session_Check;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Session_Close;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Session_Get_Free_Channel;

/**
 * Created by qiujian on 2017/1/3.
 */

public class TUTKDevice {

    private String TAG = "TUTKDevice";

    String mUID;

    DeviceInfo mDeviceInfo;

    int mSID = -1;
    int speakSession = -1;
    boolean onLine = false;

    Object mLock = new Object();

    List<TUTKSession> sessionList = new ArrayList<>();
    List<TUTKSession> videoSessionList = new ArrayList<>();

    protected St_SInfo mSt_SInfo = new St_SInfo();

    private UserInfo mUser;

    public enum RDTChannelUsedStatus{
        CHANNEL_FREE,//not used
        CHANNEL_NOTIFY,//notify
        CHANNEL_DOWNLOAD_MP4,//download mp4
        CHANNEL_DOWNLOAD_MP4_THUMBS,//download mp4 thumbs
        CHANNEL_PLAY_MP4//play mp4
    }
    private RDTChannelUsedStatus mRdtChannelUsedStatus = RDTChannelUsedStatus.CHANNEL_FREE;
    private ThumbnailsThread mThumbThread;

    public TUTKDevice(String uid) {
        this.mUID = uid;
        mDeviceInfo = new DeviceInfo();
        String customeName = SharedPreference.getString(mUID, "");
        if (customeName.length() > 0) {
            mDeviceInfo.setDeviceName(customeName);
        } else {
            mDeviceInfo.setDeviceName(mUID);
        }
    }

    public String getUID() {
        return mUID;
    }

    public boolean isOnLine() {
        return onLine;
    }

    public TUTKSession getSession() {
        synchronized (mLock) {
            synchronized (sessionList) {
                for (int i = 0; i < sessionList.size(); i++) {
                    TUTKSession session = sessionList.get(i);
                    return session;
                }
                return null;
            }
        }
    }

    public int connect(){
        int ret = -1;

        if (mSID >= 0) {
            IOTC_Connect_Stop_BySID(mSID);
        }

        mSID =IOTC_Get_SessionID();

        if (mSID <0){
            Log.d(TAG,String.format("IOTC_Get_SessionID() ret = [%d]",mSID));
            return  mSID;
        }

        ret = IOTC_Connect_ByUID_Parallel(mUID,mSID);
        Log.d(TAG,String.format("IOTC_Connect_ByUID_Parallel(%s,%d) ret = [%d]",mUID,mSID,ret));

        IOTC_Session_Check(mSID, mSt_SInfo);
        String net = ((mSt_SInfo.Mode == 0) ? "P2P" : "Relay") + ", NAT=type" + IOTC_Get_Nat_Type();
        Log.d(TAG,net);

        if (ret >=0){
            Log.d(TAG,String.format("IOTC Connect OK SID[%d] ret=[%d]\n",mSID,ret));
            TUTKSession session = new TUTKSession(ret);
            onLine = true;
            sessionList.add(session);
        }else {
            Log.d(TAG,String.format("IOTC Connect ERROR ret[%d]\n",ret));
        }

        return  ret;
    }

    public void  disconnect(){
        if (mSID>=0){
            IOTC_Session_Close(mSID);
            Log.d(TAG,String.format("IOTC_Session_Close([%d])",mSID));
            mSID = -1;
        }
    }

    public int login(String user,String pwd) {
        int ret = -1;
        TUTKSession tutkSession = null;
        synchronized (mLock) {
            tutkSession = getSession();
            if (tutkSession != null && tutkSession.getAVIndex() < 0) {
                ret =tutkSession.login(user,pwd);
            }
            if (ret>=0){
                tutkSession.setAvIndex(ret);
                MsgThread msgThread = new MsgThread(this);
                msgThread.start();
            }
        }
        return ret;
    }

    public boolean login() {
        synchronized (mLock) {
            if (getSession() != null) {
                return true;
            }
            int sid = -1;
            if (mSID >= 0) {
                IOTC_Connect_Stop_BySID(mSID);
            }
            mSID = IOTC_Get_SessionID();
            if (mSID < 0) {
                Log.d(TAG, "IOTC_Get_SessionID(.)=" + mSID);
                onLine = false;
                return false;
            }
            String str = ("IOTC_Connect_ByUID_Parallel(.)=" + sid);
            IOTC_Session_Check(mSID, mSt_SInfo);
            str = ((mSt_SInfo.Mode == 0) ? "P2P" : "Relay") + ", NAT=type" + IOTC_Get_Nat_Type();
            Log.d(TAG, str);
            Log.d(TAG, "SID1:" + mSID);
            sid = IOTC_Connect_ByUID_Parallel(mUID, mSID);
            Log.d(TAG, "SID2:" + sid);
            IOTC_Session_Check(mSID, mSt_SInfo);
            str = ((mSt_SInfo.Mode == 0) ? "P2P" : "Relay") + ", NAT=type" + IOTC_Get_Nat_Type();
            if (sid < 0) {
                switch (sid) {
                    case IOTC_ER_NOT_INITIALIZED:
                        str = String.format("Don't call IOTC_Initialize() when connecting.(%d)", 10);
                        break;

                    case IOTC_ER_CONNECT_IS_CALLING:
                        str = String.format("IOTC_Connect_ByXX() is calling when connecting.(%d)", sid);
                        break;

                    case IOTC_ER_FAIL_RESOLVE_HOSTNAME:
                        str = String.format("Can't resolved server's Domain name when connecting.(%d)", sid);
                        break;

                    case IOTC_ER_SERVER_NOT_RESPONSE:
                        str = String.format("Server not response when connecting.(%d)", sid);
                        break;

                    case IOTC_ER_FAIL_GET_LOCAL_IP:
                        str = String.format("Can't Get local IP when connecting.(%d)", sid);
                        break;

                    case IOTC_ER_UNKNOWN_DEVICE:
                        str = String.format("Wrong UID when connecting.(%d)", sid);
                        break;

                    case IOTC_ER_UNLICENSE:
                        str = String.format("UID is not registered when connecting.(%d)", sid);
                        break;

                    case IOTC_ER_CAN_NOT_FIND_DEVICE:
                        str = String.format("Device is NOT online when connecting.(%d)", sid);
                        break;

                    case IOTC_ER_EXCEED_MAX_SESSION:
                        str = String.format("Exceed the max session number when connecting.(%d)", sid);
                        break;

                    case IOTC_ER_TIMEOUT:
                        str = String.format("Timeout when connecting.(%d)", sid);
                        break;

                    case IOTC_ER_DEVICE_NOT_LISTENING:
                        str = String.format("The device is not on listening when connecting.(%d)", sid);
                        break;
                                /*case  IOTC_ER_DEVICE_EXCEED_MAX_SESSION:{
                                    return mSID;
                                }*/
                    default:
                        str = String.format("Failed to connect device when connecting.(%d)", sid);
                }
                Log.d(TAG, str);
                return false;
            }
            if (sid >= 0) {
                TUTKSession session = new TUTKSession(sid);
                onLine = true;
                sessionList.add(session);
                MsgThread msgThread = new MsgThread(this);
                msgThread.start();
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean startIpcamStream(int avIndex){
        synchronized (mLock) {
            AVAPIs av = new AVAPIs();
            int ret = av.avSendIOCtrl(avIndex, AVAPIs.IOTYPE_INNER_SND_DATA_DELAY,
                    new byte[2], 2);
            Log.d(TAG, String.format("IOTYPE_INNER_SND_DATA_DELAY:ret[%d],avIndex[%d]", ret, avIndex));
            if (ret < 0) {
                System.out.printf("start_ipcam_stream failed[%d]\n", ret);
                return false;
            }
            ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_START,
                    new byte[8], 8);
            Log.d(TAG, String.format("IOTYPE_USER_IPCAM_START:ret [%d] avIndex[%d]", ret, avIndex));
            if (ret < 0) {
                Log.d(TAG, String.format("start_ipcam_stream failed[%d]", ret));
               // System.out.printf("start_ipcam_stream failed[%d]\n", ret);
                return false;
            }
            return true;
        }
    }

    public void startAudio(final Handler handler, final Runnable success, final Runnable faild) {
        Thread thread =new Thread(new Runnable() {
            @Override
            public void run() {
                int ret = AVAPIs.avSendIOCtrl(getSession().getAVIndex(), IOTYPE_USER_IPCAM_AUDIOSTART,
                        new byte[8], 8);
                Log.d(TAG,String.format("IOTYPE_USER_IPCAM_AUDIOSTART [%d]",ret));
                if (ret < 0) {
                    System.out.printf("start_audio_stream failed[%d]\n", ret);
                    if (faild!=null&& handler!=null){
                        handler.post(faild);
                    }
                } else {
                    if (success!=null&& handler!=null){
                        handler.post(success);
                    }
                }
            }
        });
        thread.start();
    }

    public int getSpeakSession() {

        //int i =
        speakSession = IOTC_Connect_ByUID_Parallel(mUID, mSID);
        if (speakSession < 0) {

        }
        return speakSession;
    }

    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    public void close() {
            IOTC_Connect_Stop_BySID(mSID);
            for (TUTKSession session : sessionList) {
                session.close();
            }
            sessionList.clear();
    }

    class MsgThread extends Thread {
        TUTKDevice mDevice;
        private int MAX_BUF_SIZE = 1024;

        public MsgThread(TUTKDevice device) {
            mDevice = device;
        }

        @Override
        public void run() {
            super.run();

                int avIndex = mDevice.getSession().getAVIndex();
                if (avIndex < 0) {
                    return;
                }

                int sid = mDevice.getSession().getSID();
                byte[] ioCtrlBuf = new byte[MAX_BUF_SIZE];
                int[] ioType = new int[1];
                int rc = -1;
                int RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ = 0x1200;
            synchronized (mLock) {
                SettingsCommand settingsCommand = new SettingsCommand();
                byte[] buff = settingsCommand.Json().getBytes();
                int i = AVAPIs.avSendIOCtrl(avIndex, RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, buff, buff.length);
                Log.d("SettingsCommand", "SettingsCommand =　" + i);

                GetEncoderParameters getEncoderParameters = new GetEncoderParameters();
                byte[] bytes = getEncoderParameters.Json().getBytes();
                i = AVAPIs.avSendIOCtrl(mDevice.getSession().getAVIndex(), RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, bytes, bytes.length);
                Log.d("GetEncoderParameters", "GetEncoderParameters =　" + i);

            }
            while (true) {
                //Log.d(TAG," receive ioctrl:"+Thread.currentThread().getId());
                rc = AVAPIs.avRecvIOCtrl(avIndex, ioType, ioCtrlBuf, MAX_BUF_SIZE, 1000);
                if (rc >= 0) {
                    Log.d(TAG, "avRecvIOCtrl(), rc=[" + rc + "]" + "content:" + new String(ioCtrlBuf, 0, rc));
                    Handle_IOCTRL_Cmd(sid, avIndex, ioCtrlBuf, ioType[0], rc);
                } else if (rc == AVAPIs.AV_ER_NOT_INITIALIZED) {
                    break;
                } else if (rc == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
                    notifyTutkError(rc);
                    break;
                } else if (rc == AVAPIs.AV_ER_INVALID_SID) {
                    notifyTutkError(rc);
                    break;
                } else if (rc == AVAPIs.AV_ER_IOTC_SESSION_CLOSED) {
                    notifyTutkError(rc);
                    break;
                } else if (rc != AVAPIs.AV_ER_TIMEOUT) {
                    Log.d(TAG, "avRecvIOCtrl(), rc=[" + rc + "]");
                    continue;
                }
            }
        }

        private void Handle_IOCTRL_Cmd(int sid, int avIndex, byte[] buf, int type, int size) {
            Log.d(TAG, "Handle_IOCTRL_Cmd:");
            switch (type) {
                case AVIOCTRLDEFs.RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_RESP: {
                    String msg = new String(buf, 0, size);
                    if(processPlayConfig(msg)){
                        break;
                    } else if (processSdcardInfo(msg)){
                        break;
                    } else if (processDelFile(msg)){
                        break;
                    } else if (processServerRecord(msg)) {
                        break;
                    } else if (processSettingsUpdate(msg)) {
                        break;
                    } else if (processEncoderSettingsUpdate(msg)) {
                        break;
                    } else if (processEvents(msg)) {
                        break;
                    } else if (processDeviceInfo(msg)) {
                        break;
                    } else {
                        Log.e(TAG, String.format("Unknow Json:", msg));
                    }
                    break;
                }
            }  // end of switch
        }

        private boolean processDeviceInfo(String json) {
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(json);
                JSONArray item_list = jsonObject.getJSONArray("item_list");
                if (mDeviceInfo.initDeviceInfo(item_list)) {
                    notifyDeviceInfoChange(mDeviceInfo, "");
                }
                //check sdcard error
                if (mDeviceInfo.isSdcardFault()) {
                    mDeviceInfo.setSdcardFault(false);
                    SdcardModel model = new SdcardModel();
                    model.setName("sdcard fault");
                    model.setDesc("");
                    model.setUid(mDevice.getUID());
                    model.setNeedFormat(true);
                    handSdcardAlarm(model);
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }

    private boolean processSettingsUpdate(String json) {
        try {
            JSONArray jsonArray = new JSONArray(json);
            String name = jsonArray.getJSONObject(0).getString("name");
            Boolean result = jsonArray.getJSONObject(0).getBoolean("result");
            if (DeviceInfo.Moving.equals(name) && result) {
                mDeviceInfo.setmMoving(jsonArray.getJSONObject(0).getBoolean("value"));
            } else if (DeviceInfo.ThreeDNR.equals(name) && result) {
                mDeviceInfo.set3DNR(jsonArray.getJSONObject(0).getBoolean("value"));
            } else if (DeviceInfo.WaterMark.equals(name) && result) {
                mDeviceInfo.setmWaterMark(jsonArray.getJSONObject(0).getBoolean("value"));
            } else if (DeviceInfo.IDC.equals(name) && result) {
                mDeviceInfo.setIDC(jsonArray.getJSONObject(0).getBoolean("value"));
            } else if (DeviceInfo.Sound.equals(name) && result) {
                mDeviceInfo.setSound(jsonArray.getJSONObject(0).getBoolean("value"));
            } else if (DeviceInfo.Exposure.equals(name) && result) {
                mDeviceInfo.setmExposure(jsonArray.getJSONObject(0).getInt("value"));
            } else if (DeviceInfo.RecordTime.equals(name) && result) {
                mDeviceInfo.setmRecordTime(jsonArray.getJSONObject(0).getInt("value"));
            } else if (DeviceInfo.Resolutionl.equals(name) && result) {
                mDeviceInfo.setmResolution(jsonArray.getJSONObject(0).getString("value"));
            } else if (DeviceInfo.WhiteBalance.equals(name) && result) {
                mDeviceInfo.setmWhiteBalance(jsonArray.getJSONObject(0).getString("value"));
            } else if (DeviceInfo.MovingSensitivity.equals(name) && result) {
                mDeviceInfo.setMoveSensitivity(jsonArray.getJSONObject(0).getInt("value"));
            } else if (DeviceInfo.ThreeDNRLevel.equals(name) && result) {
                mDeviceInfo.set3DNRLevel(jsonArray.getJSONObject(0).getInt("value"));
            } else if (DeviceInfo.ODT.equals(name) && result) {
                boolean value = jsonArray.getJSONObject(0).getBoolean("value");
                mDeviceInfo.setOdt(value);
                if(value){
                    mDeviceInfo.setOdtLevel(DeviceInfo.ODT_DEFAULT_LEVEL-DeviceInfo.ODT_LEVEL_STANDARD);
                }else{
                    mDeviceInfo.setOdtLevel(-1);
                }
            } else if (DeviceInfo.ODTLevel.equals(name) && result) {
                mDeviceInfo.setOdtLevel(jsonArray.getJSONObject(0).getInt("value") - 9);
            } else if (DeviceInfo.Format.equals(name)){
                notifySdcardFormat(result);
                return true;
            }
            notifyDeviceInfoChange(mDeviceInfo, name);
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean processSdcardInfo(String json){
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("name")
                    && "GetSdcardInfo".equals(jsonObject.getString("name"))){
                mDeviceInfo.getSdcardInfo().setFreeSize(jsonObject.getLong("free"));
                mDeviceInfo.getSdcardInfo().setTotalSize(jsonObject.getLong("total"));
                mDeviceInfo.getSdcardInfo().setExist(jsonObject.getInt("exist") == 1);
                notifyDeviceInfoChange(mDeviceInfo, "GetSdcardInfo");
                return true;
            }
        }catch (JSONException e){
        }
        return false;
    }

    private boolean processServerRecord(String json){
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("name")
                    && "GetRecordList".equals(jsonObject.getString("name"))){
                handRecordListLoaded(json);
                return true;
            }
        }catch (JSONException e){
        }
        return false;
    }

    private boolean processDelFile(String json){
        try {
            JSONArray jsonArray = new JSONArray(json);
            if (jsonArray.getJSONObject(0).has("name")
                    && "DelFile".equals(jsonArray.getJSONObject(0).getString("name"))){
                handDelFile(jsonArray.getJSONObject(0).toString());
                return true;
            }
        }catch (JSONException e){
        }
        return false;
    }

    private boolean processPlayConfig(String json){
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.has("name") && PlayConfig.GetPlayConfig.equals(jsonObject.getString("name"))){
                if (mDeviceInfo.getPlayConfig().initPlayConfigInfo(json)) {
                    handPlayConfigLoaded(MsgDatas.MSG_GET_PLAY_CONFIG_TIMEOUT, true);
                }
                return true;
            }
        }catch (JSONException e){
        }
        try{
            JSONArray jsonArray = new JSONArray(json);
            if(!jsonArray.getJSONObject(0).has("name")){
                return false;
            }
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            String name = jsonObject.getString("name");
            boolean result = jsonObject.getBoolean("result");
            if(PlayConfig.PlayBrightness.equals(name)){
                if(result){
                    mDeviceInfo.getPlayConfig().setPlayBrightness(jsonObject.getInt("value"));
                }
                handPlayConfigLoaded(MsgDatas.MSG_SET_PLAY_BRIGHTNESS_TIMEOUT, result);
                return true;
            }else if(PlayConfig.PlayVolume.equals(name)){
                if(result){
                    mDeviceInfo.getPlayConfig().setPlayVolume(jsonObject.getInt("value"));
                }
                handPlayConfigLoaded(MsgDatas.MSG_SET_PLAY_VOLUME_TIMEOUT, result);
                return true;
            }else if(PlayConfig.PlayContrast.equals(name)){
                if(result){
                    mDeviceInfo.getPlayConfig().setPlayContrast(jsonObject.getInt("value"));
                }
                handPlayConfigLoaded(MsgDatas.MSG_SET_PLAY_CONTRAST_TIMEOUT, result);
                return true;
            }else if(PlayConfig.PlaySaturation.equals(name)){
                if(result){
                    mDeviceInfo.getPlayConfig().setPlaySaturation(jsonObject.getInt("value"));
                }
                handPlayConfigLoaded(MsgDatas.MSG_SET_PLAY_SATURATION_TIMEOUT, result);
                return true;
            }else if(PlayConfig.PlaySharpness.equals(name)){
                if(result){
                    mDeviceInfo.getPlayConfig().setPlaySharpness(jsonObject.getInt("value"));
                }
                handPlayConfigLoaded(MsgDatas.MSG_SET_PLAY_SHARPNESS_TIMEOUT, result);
                return true;
            }
        } catch (JSONException e) {
        }
        return false;
    }

    private boolean processEvents(String json) {
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(json);
            String name = jsonArray.getJSONObject(0).getString("name");
            if ("AlarmNotify".equals(name)) {
                MdNotifyInfo info = new MdNotifyInfo();
                info.setAlarmTime(jsonArray.getJSONObject(0).getString("AlarmTime"));
                if(jsonArray.getJSONObject(0).has("desc")){
                    info.setDesc(jsonArray.getJSONObject(0).getString("desc"));
                }
                /*if("stop".equals(info.getDesc())){
                    info.setDesc("/mnt/sdcard/.MISC/.THUMB-LOCK-FRONT/20160121_085053_A.jpg");
                }else{
                    return true;
                }*/
                if("stop".equals(info.getDesc())){
                    //handAlarmNotify(info);
                } else {
                    if(null != mThumbThread){
                        mThumbThread.setCancel(true);
                    }
                    mThumbThread = new ThumbnailsThread(info);
                    mThumbThread.start();
                }
                return true;
            } else if ("SdcardNotify".equals(name)) {
                SdcardModel model = new SdcardModel();
                model.setName(name);
                model.setDesc(jsonArray.getJSONObject(0).getString("desc"));
                model.setUid(jsonArray.getJSONObject(0).getString("uid"));
                model.setNeedFormat(jsonArray.getJSONObject(0).getInt("needFormat") > 0);
                handSdcardAlarm(model);
                return true;
            } else if ("TakePictureLocation".equals(name) && jsonArray.getJSONObject(0).getBoolean("result")) {
                String alarmTime = jsonArray.getJSONObject(0).getString("value");
                handPicturePatch(alarmTime);
                return true;
            } else {
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public interface DeviceInfoChangeCallback {
        public void onDeviceInfoChange(DeviceInfo deviceInfo, String name);
        public void onSdcardFormat(boolean result);
    }

    private final ArrayList<DeviceInfoChangeCallback> deviceInfoChangeCallbacks = new ArrayList<DeviceInfoChangeCallback>();

    public void addDeviceInfoChangeCallback(DeviceInfoChangeCallback listener) {
        Log.d(TAG, "----addDeviceInfoChangeCallback----" + listener);
        synchronized (deviceInfoChangeCallbacks) {
            if (!deviceInfoChangeCallbacks.contains(listener)) {
                deviceInfoChangeCallbacks.add(listener);
            }
        }
    }

    public void removeDeviceInfoChangeCallback(DeviceInfoChangeCallback listener) {
        Log.d(TAG, "----removeDeviceInfoChangeCallback----" + listener);
        synchronized (deviceInfoChangeCallbacks) {
            deviceInfoChangeCallbacks.remove(listener);
        }
    }

    private void notifyDeviceInfoChange(DeviceInfo deviceInfo, String name) {
        Log.d(TAG, "----notifyDeviceInfoChange----");
        synchronized (deviceInfoChangeCallbacks) {
            for (DeviceInfoChangeCallback callback : deviceInfoChangeCallbacks) {
                try {
                    callback.onDeviceInfoChange(deviceInfo, name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void notifySdcardFormat(boolean result) {
        Log.d(TAG, "----notifySdcardFormat----");
        synchronized (deviceInfoChangeCallbacks) {
            for (DeviceInfoChangeCallback callback : deviceInfoChangeCallbacks) {
                try {
                    callback.onSdcardFormat(result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public interface OnTutkError {
        public void onError(int code);
    }

    private final ArrayList<OnTutkError> tutkErrorsListener = new ArrayList<OnTutkError>();

    public void addOnTutkErrorListener(OnTutkError listener) {
        Log.d(TAG, "----addOnTutkErrorListener----" + listener);
        synchronized (tutkErrorsListener) {
            if (!tutkErrorsListener.contains(listener)) {
                tutkErrorsListener.add(listener);
            }
        }
    }

    public void removeOnTutkErrorListener(OnTutkError listener) {
        Log.d(TAG, "----removeOnTutkErrorListener----" + listener);
        synchronized (tutkErrorsListener) {
            tutkErrorsListener.remove(listener);
        }
    }

    private void notifyTutkError(int code) {
        synchronized (tutkErrorsListener) {
            for (OnTutkError callback : tutkErrorsListener) {
                try {
                    callback.onError(code);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean processEncoderSettingsUpdate(String msg) {
        try {
            JSONObject jsonObject = new JSONObject(msg);
            if ("GetEncoderParameters".equals(jsonObject.getString("name"))) {
                JSONArray item_list = jsonObject.getJSONArray("item_list");
                if (item_list != null) {
                    mDeviceInfo.encoderParameters.clear();
                    for (int i = 0; i < item_list.length(); i++) {
                        JSONObject item_listJSONObject = item_list.getJSONObject(i);
                        EncoderParameter encoderParameter = new EncoderParameter();
                        encoderParameter.setFrame_rate(String.valueOf(item_listJSONObject.getInt("frame_rate")));
                        encoderParameter.setLevel(String.valueOf(item_listJSONObject.getInt("level")));
                        encoderParameter.setGop_size(String.valueOf(item_listJSONObject.getInt("gop_size")));
                        encoderParameter.setProfile(String.valueOf(item_listJSONObject.getInt("profile")));
                        encoderParameter.setQuality(String.valueOf(item_listJSONObject.getInt("quality")));
                        encoderParameter.setQp_init(String.valueOf(item_listJSONObject.getInt("qp_init")));
                        encoderParameter.setQp_min(String.valueOf(item_listJSONObject.getInt("qp_min")));
                        encoderParameter.setQp_max(String.valueOf(item_listJSONObject.getInt("qp_max")));
                        encoderParameter.setQp_step(String.valueOf(item_listJSONObject.getInt("qp_step")));
                        encoderParameter.setRc_mode(String.valueOf(item_listJSONObject.getInt("rc_mode")));
                        encoderParameter.setWidth(String.valueOf(item_listJSONObject.getInt("width")));
                        encoderParameter.setHeight(String.valueOf(item_listJSONObject.getInt("height")));
                        encoderParameter.setBit_rate(String.valueOf(item_listJSONObject.getInt("bit_rate")));
                        encoderParameter.setChannel(String.valueOf(item_listJSONObject.getInt("value")));
                        mDeviceInfo.encoderParameters.add(encoderParameter);
                        if (encoderParameter.getChannel().equals("1")) {
                            mDeviceInfo.setPlayResolution(encoderParameter.getHeight());
                            mDeviceInfo.setPlayFps(Integer.parseInt(encoderParameter.getFrame_rate()));
                            notifyEncoderChange(encoderParameter);
                        }else if(encoderParameter.getChannel().equals("0")){
                            mDeviceInfo.setmResolution(encoderParameter.getHeight());
                            notifyEncoderChange(encoderParameter);
                        }


                    }

                }
                return true;
            } else if ("SetEncoderParameters".equals(jsonObject.getString("name")) && jsonObject.getBoolean("result")) {

                EncoderParameter encoderParameter = null;

                for (int i = 0; i < mDeviceInfo.encoderParameters.size(); i++) {
                    if (mDeviceInfo.encoderParameters.get(i).getChannel().equals(jsonObject.getString("value"))) {
                        encoderParameter = mDeviceInfo.encoderParameters.get(i);
                    }
                }

                if (encoderParameter == null) {
                    encoderParameter = new EncoderParameter();
                    mDeviceInfo.encoderParameters.add(encoderParameter);
                }

                encoderParameter.setFrame_rate(String.valueOf(jsonObject.getInt("frame_rate")));
                encoderParameter.setLevel(String.valueOf(jsonObject.getInt("level")));
                encoderParameter.setGop_size(String.valueOf(jsonObject.getInt("gop_size")));
                encoderParameter.setProfile(String.valueOf(jsonObject.getInt("profile")));
                encoderParameter.setQuality(String.valueOf(jsonObject.getInt("quality")));
                encoderParameter.setQp_init(String.valueOf(jsonObject.getInt("qp_init")));
                encoderParameter.setQp_min(String.valueOf(jsonObject.getInt("qp_min")));
                encoderParameter.setQp_max(String.valueOf(jsonObject.getInt("qp_max")));
                encoderParameter.setQp_step(String.valueOf(jsonObject.getInt("qp_step")));
                encoderParameter.setRc_mode(String.valueOf(jsonObject.getInt("rc_mode")));
                encoderParameter.setWidth(String.valueOf(jsonObject.getInt("width")));
                encoderParameter.setHeight(String.valueOf(jsonObject.getInt("height")));
                encoderParameter.setBit_rate(String.valueOf(jsonObject.getInt("bit_rate")));
                encoderParameter.setChannel(String.valueOf(jsonObject.getInt("value")));
                notifyEncoderChange(encoderParameter);
                return true;
            } else {
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public interface EncoderChangeCallback {
        public void onEncoderChange(EncoderParameter parameter);
    }

    public void addEncoderChangeCallbacks(EncoderChangeCallback listener) {
        Log.d(TAG, "----addEncoderChangeCallbacks----" + listener);
        synchronized (mEncoderChangeCallbacks) {
            if (!mEncoderChangeCallbacks.contains(listener)) {
                mEncoderChangeCallbacks.add(listener);
            }
        }
    }

    public void removeEncoderChangeCallbacks(EncoderChangeCallback listener) {
        Log.d(TAG, "----removeEncoderChangeCallbacks----" + listener);
        synchronized (mEncoderChangeCallbacks) {
            mEncoderChangeCallbacks.remove(listener);
        }
    }

    private final ArrayList<EncoderChangeCallback> mEncoderChangeCallbacks = new ArrayList<EncoderChangeCallback>();

    private void notifyEncoderChange(EncoderParameter parameter) {
        synchronized (mEncoderChangeCallbacks) {
            for (EncoderChangeCallback callback : mEncoderChangeCallbacks) {
                try {
                    callback.onEncoderChange(parameter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handAlarmNotify(MdNotifyInfo notifyInfo) {
        Log.v(TAG, "handAlarmNotify");
        synchronized (mListeners) {
            for (NotifyCallback callback : mListeners) {
                try {
                    callback.onEvent(notifyInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handPicturePatch(String path) {
        synchronized (mListeners) {
            for (NotifyCallback callback : mListeners) {
                try {
                    callback.onPictureTaked(path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handSdcardAlarm(SdcardModel model) {
        synchronized (mListeners) {
            for (NotifyCallback callback : mListeners) {
                try {
                    callback.onSdcardAlarm(model);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handRecordListLoaded(String json) {
        synchronized (mListeners) {
            for (NotifyCallback callback : mListeners) {
                try {
                    callback.onRecordListLoaded(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handDelFile(String json){
        synchronized (mListeners) {
            for (NotifyCallback callback : mListeners) {
                try {
                    callback.onDelFile(json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handPlayConfigLoaded(int msgWhat, boolean result) {
        synchronized (mListeners) {
            for (NotifyCallback callback : mListeners) {
                try {
                    callback.onPlayConfigLoaded(msgWhat, result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private final ArrayList<NotifyCallback> mListeners = new ArrayList<NotifyCallback>();

    public interface NotifyCallback {
        /**
         * Called when a new device has been added
         */
        public void onEvent(MdNotifyInfo notifyInfo);

        public void onPictureTaked(String patch);

        public void onSdcardAlarm(SdcardModel model);

        public void onPlayConfigLoaded(int msgWhat, boolean result);

        void onRecordListLoaded(String json);

        void onDelFile(String json);
    }

    public void addListener(NotifyCallback listener) {
        Log.d(TAG, "----addListener----" + listener);
        synchronized (mListeners) {
            if (!mListeners.contains(listener)) {
                mListeners.add(listener);
            }
        }
    }

    public void removeListener(NotifyCallback listener) {
        Log.d(TAG, "----removeListener----" + listener);
        synchronized (mListeners) {
            mListeners.remove(listener);
        }
    }

    public void setCurrenUser(UserInfo userInfo){
        synchronized (this){
            mUser=userInfo;
        }
    }

    public UserInfo getCurrentUser(){
        synchronized (this){
            return mUser;
        }
    }

    private class ThumbnailsThread extends Thread {
        private boolean cancel;
        private MdNotifyInfo notifyInfo;
        private int rdtId = -1;

        public void setCancel(boolean cancel) {
            this.cancel = cancel;
        }

        public ThumbnailsThread(MdNotifyInfo notifyInfo) {
            this.notifyInfo = notifyInfo;
        }

        @Override
        public void run() {
            Log.v(TAG, "begin to download thumbnail");
            int countNo = 0;
            while (!cancel){
                if(RDTChannelUsedStatus.CHANNEL_FREE != getRdtChannelUsedStatus()){
                    try {
                        Log.v(TAG, "rdt channel is used " + getRdtChannelUsedStatus()+", sleep time:" + countNo);
                        countNo++;
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(countNo > 15){
                        cancel = true;
                    }
                    continue;
                }
                setRdtChannelUsed(RDTChannelUsedStatus.CHANNEL_NOTIFY);
                int ret = cancel? -1 : downLoad(notifyInfo.getDesc());
                if (rdtId > -1) {
                    int retDestory = RDTAPIs.RDT_Abort(rdtId);
                    Log.v(TAG, "abort rdtid:" + rdtId + " after doing ret=" + retDestory);
                    if (retDestory > -1 || retDestory == RDTAPIs.RDT_ER_INVALID_RDT_ID
                            || retDestory == RDTAPIs.RDT_ER_REMOTE_ABORT) {
                        getSession().setRdtId(-1);
                    }
                    rdtId = -1;
                }
                setRdtChannelUsed(RDTChannelUsedStatus.CHANNEL_FREE);
                if (ret > 0) {
                    Bitmap thumbnails = BitmapFactory.decodeFile(RecordUtils.getThumbnailsPath());
                    if(null != thumbnails){
                        Log.v(TAG, "notify with bitmap");
                        notifyInfo.setBitmap(thumbnails);
                    }
                }
                handAlarmNotify(notifyInfo);
                break;
            }
        }

        private int downLoad(String pathRemote) {
            int sid = getSession().getSID();
            if (sid < 0) {
                return -1;
            }
            rdtId = getSession().getRdtId();
            if (rdtId >= 0) {
                int ret = RDTAPIs.RDT_Abort(rdtId);
                Log.w(TAG, "RDT_Abort old rdtid:" + rdtId + ", ret=" + ret);
                if (ret > -1 || ret == RDTAPIs.RDT_ER_INVALID_RDT_ID
                        || ret == RDTAPIs.RDT_ER_REMOTE_ABORT) {
                    getSession().setRdtId(-1);
                }
            }
            rdtId = RDTAPIs.RDT_Create(sid, 5000, 1);
            if (rdtId < 0) {
                Log.e(TAG, "failed sid=" + sid + " ,mRdtId=" + rdtId);
                return -1;
            }
            getSession().setRdtId(rdtId);
            if(cancel){
                Log.e(TAG, "return become cancel");
                return -1;
            }
            Log.v(TAG, "downloadFile retId=" + rdtId + ", pathRemote=" + pathRemote);
            FileOutputStream fos = null;
            try {
                int fileSize = 128*1024;
                byte[] buff = new byte[fileSize];
                RecordUtils.mkdirPath(RecordUtils.getDownloadPath());
                File file = new File(RecordUtils.getThumbnailsPath());
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                StartFileTransferCommand startFileTransferCommand = new StartFileTransferCommand(pathRemote);
                String commad = startFileTransferCommand.Json();
                int write = RDTAPIs.RDT_Write(rdtId, commad.getBytes(), commad.getBytes().length);
                if (!cancel && write < 0) {
                    Log.e(TAG, "cancel" + cancel + ", StartFileTransfer timeout");
                    return write;
                }

                int receive = 0;
                fos = new FileOutputStream(file);
                while (!cancel && receive < fileSize) {
                    int i = RDTAPIs.RDT_Read(rdtId, buff, buff.length, 2000);
                    if (i <= 0) {
                        Log.e(TAG, "break receive");
                        return -1;
                    }
                    fos.write(buff, 0, i);
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
                    if (null != fos) {
                        fos.flush();
                        fos.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
    }

    public synchronized RDTChannelUsedStatus getRdtChannelUsedStatus() {
        return mRdtChannelUsedStatus;
    }

    public synchronized void setRdtChannelUsed(RDTChannelUsedStatus rdtChannelUsed) {
        this.mRdtChannelUsedStatus = rdtChannelUsed;
    }

    public void cancelThumbsThread(){
        if(null != mThumbThread){
            mThumbThread.setCancel(true);
        }
        setRdtChannelUsed(RDTChannelUsedStatus.CHANNEL_FREE);
    }
}
