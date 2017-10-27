package com.rockchip.tutk;

import android.os.AsyncTask;
import android.util.Log;

import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.AVIOCTRLDEFs;
import com.tutk.IOTC.IOTCAPIs;
import com.tutk.IOTC.Packet;
import com.tutk.IOTC.RDTAPIs;
import com.tutk.IOTC.St_SInfo;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.tutk.IOTC.AVIOCTRLDEFs.RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Get_Nat_Type;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Session_Check;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Session_Get_Free_Channel;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Session_Read;
import static com.tutk.IOTC.IOTCAPIs.IOTC_Session_Write;

/**
 * Created by qiujian on 2017/1/3.
 */

public class TUTKSession {
    int mSID = -1;
    static int mAVUID = -1;
    private String TAG = "TUTKSession";

    final int MAXSIZE_RECVBUF = 1400;

    St_SInfo mSt_SInfo = new St_SInfo();

    int avIndex = -1;

    int rdtIndex = -1;

    int speakIndex = -1;


    public TUTKSession(int sid) {
        this.mSID = sid;
        IOTC_Session_Check(mSID, mSt_SInfo);
        String str = "TUTKSession:  " + ((mSt_SInfo.Mode == 0) ? "P2P" : "Relay") + ", NAT=type" + IOTC_Get_Nat_Type();
        Log.d(TAG, str);
    }

    public int write(String msg) {
        synchronized (mSt_SInfo) {
            Log.d(TAG, msg);
            final byte[] bytes = msg.getBytes();
            try {
                Object write = new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] objects) {
                        int write = AVAPIs.avSendIOCtrl(avIndex, RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, bytes, bytes.length);
                        return write;
                    }
                }.execute().get(2000, TimeUnit.MILLISECONDS);

                if (write != null) {
                    return (int) write;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }

            return -1;
        }
    }

    public String read() {
        synchronized (mSt_SInfo) {
            Log.d(TAG, "read");
            String result = "";
            byte[] readBuf = new byte[MAXSIZE_RECVBUF];
            int read = IOTC_Session_Read(mSID, readBuf, MAXSIZE_RECVBUF, 5000, 0);
        /*while (read < 0) {
            read = IOTC_Session_Read(mSID, readBuf, MAXSIZE_RECVBUF, 500, 0);
        }*/
            result = new String(readBuf, 0, read);
            Log.d(TAG, "readed:" + read + ":" + result);
            return result;
        }
    }

    public St_SInfo getSessionInfo() {
        IOTC_Session_Check(mSID, mSt_SInfo);
        String str = "getSessionInfo:  " + ((mSt_SInfo.Mode == 0) ? "P2P" : "Relay") + ", NAT=type" + IOTC_Get_Nat_Type();
        Log.d(TAG, str);
        return mSt_SInfo;
    }

    public String getIP() {
        St_SInfo sessionInfo = getSessionInfo();

        try {
            String s = new String(sessionInfo.RemoteIP, 0, sessionInfo.RemoteIP.length, "utf-8");
            s = s.replaceAll(new String(new byte[]{0}, "utf-8"), "");
            return s;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }

    }

    public int getPort() {
        St_SInfo sessionInfo = getSessionInfo();
        return sessionInfo.RemotePort;
    }

    public String getMode() {
        St_SInfo sessionInfo = getSessionInfo();
        String mode = "";
        switch (sessionInfo.Mode) {
            case 0:
                mode = "P2P";
                break;
            case 1:
                mode = "Relay";
                break;
            case 2:
                mode = "Lan";
                break;
        }
        return mode;
    }

    public String getUID() {
        St_SInfo sessionInfo = getSessionInfo();

        try {
            String s = new String(sessionInfo.UID, 0, 20, "utf-8");
            return s;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }

    }

    public int getSID() {
        return mSID;
    }

    public void setSID(int sid) {
        mSID = sid;
    }

    public int getAVIndex() {
        synchronized (mSt_SInfo) {
            final int[] srvType = new int[1];
            if (avIndex < 0) {
                try {
                    Object index = new AsyncTask() {
                        @Override
                        protected Object doInBackground(Object[] objects) {

                            int channelId = AVAPIs.avClientStart2(mSID, "admin", "888888", 20000, srvType, 0,srvType);
                           // int channelId = AVAPIs.avClientStart(mSID, "admin", "888888", 20000, srvType, 0);
                            Log.d(TAG, String.format("AVAPIs.avClientStart:session[%d],channelId[%d]", mSID,channelId) );
                            return channelId;
                        }
                    }.execute().get(2000,TimeUnit.MILLISECONDS);

                    if (index != null) {
                        avIndex = (int) index;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (TimeoutException e) {
                e.printStackTrace();
            }
            }
        /*avIndex = AVAPIs.avClientStart(mSID, "admin", "888888", 20000, srvType, 0);*/
            return avIndex;
        }
    }
    public void setAvIndex(int index){
        synchronized (mSt_SInfo){
            this.avIndex=index;
        }
    }
    public int login(String user,String pwd){
        final int[] srvType = new int[1];
        int ret = -1;
        ret = AVAPIs.avClientStart2(mSID, user, pwd, 20000,srvType,0,srvType);
        Log.d(TAG, String.format("AVAPIs.avClientStart:mSID[%d],ret=[%d]", mSID, ret));
        return ret;
    }

    public int getRDTIndex() {
        if (rdtIndex < 0) {
            try {
                Object index = new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] objects) {
                        int channelId = RDTAPIs.RDT_Create(mSID, -1, 1);
                        Log.d(TAG, "RDTAPIs.RDT_Create("+mSID+", -1, 1):" + channelId);
                        return channelId;
                    }
                }.execute().get(2000, TimeUnit.MILLISECONDS);

                if (index != null) {
                    rdtIndex = (int) index;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }
        return rdtIndex;
    }

    public void setRdtId(int rdtId){
        rdtIndex = rdtId;
    }

    public int getRdtId(){
        return rdtIndex;
    }

    int channel;

    public int startSpeakServer() {
        AVAPIs av = new AVAPIs();
        int[] bResend = new int[1];
        int SERVTYPE_STREAM_SERVER = 16;
        if (speakIndex >= 0) {
            Log.d(TAG, "====startSpeakServer started ===");
            Log.d(TAG, "SpeakChannel=" + channel);
            byte[] info = new byte[8];
            byte[] bytes = Packet.intToByteArray_Little(channel);
            for (int i = 0; i < bytes.length; i++) {
                info[i] = bytes[i];
            }
            int ret = av.avSendIOCtrl(avIndex, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SPEAKERSTART, info, info.length);
            Log.d(TAG, "IOTYPE_USER_IPCAM_SPEAKERSTART=" + ret);
            return speakIndex;
        }

        Log.d(TAG, "====startSpeakServer===");
        channel = IOTC_Session_Get_Free_Channel(mSID);
        Log.d(TAG, "SpeakChannel=" + channel);
        byte[] info = new byte[8];
        byte[] bytes = Packet.intToByteArray_Little(channel);
        for (int i = 0; i < bytes.length; i++) {
            info[i] = bytes[i];
        }
        int ret = av.avSendIOCtrl(avIndex, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SPEAKERSTART, info, info.length);
        Log.d(TAG, "IOTYPE_USER_IPCAM_SPEAKERSTART=" + ret);
        speakIndex = AVAPIs.avServStart3(mSID, null, null, -1, SERVTYPE_STREAM_SERVER, channel, bResend);
        Log.d(TAG, "avServStart3(), avIndex=[" + speakIndex + "], sid=[" + mSID + "], bResend=[" + bResend[0] + "]");
        if (speakIndex < 0) {
            Log.d(TAG, "startSpeakServer Fail...!");
            return -1;
        }
        return speakIndex;
    }

    public void stopSpeakServer() {
        AVAPIs.avServExit(mSID, channel);
        AVAPIs.avServStop(speakIndex);
        speakIndex = -1;
        Log.d(TAG, "===stopSpeakServer===");
    }

    public void close() {
        Log.d(TAG, "close()");
        try {
            new AsyncTask() {

                @Override
                protected Object doInBackground(Object[] objects) {
                    if(rdtIndex > -1){
                        RDTAPIs.RDT_Destroy(rdtIndex);
                        Log.d(TAG, String.format("RDTAPIs.RDT_Destroy RDTID[%d]", rdtIndex));
                        rdtIndex = -1;
                    }
                    AVAPIs.avClientExit(mSID, avIndex);
                    Log.d(TAG, String.format("AVAPIs.avClientExit SID[%d] avIndex[%d]", mSID, avIndex));
                    AVAPIs.avClientStop(avIndex);
                    Log.d(TAG, String.format("AVAPIs.avClientStop  avIndex[%d]", avIndex));
                    IOTCAPIs.IOTC_Session_Close(mSID);
                    Log.d(TAG, String.format("IOTCAPIs.IOTC_Session_Close   SID[%d]", mSID));
                    return null;
                }
            }.execute().get(2000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        mSID = -1;
        avIndex = -1;
    }
}
