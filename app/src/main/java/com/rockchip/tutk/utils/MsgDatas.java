package com.rockchip.tutk.utils;

/**
 * Created by waha on 2017/3/6.
 */

public class MsgDatas {
    public static final int TYPE_ALL = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_NOMAL = 0;
    public static final int TYPE_MD = 1;
    public static final int NETWORK_TIMEOUT = 10000;
    public static final int FORMAT_TIMEOUT = 240000;
    public static final int TAKEPIC_TIMEOUT = 7000;
    public static final int TAKEPIC_TRY_TIME = 2000;
    public static final int DEFAULT_FPS = 25;

    //activity requestcode
    public static final int REQUEST_RECORD_PLAY = 0;

    //录制文件查看
    public static final int MSG_SCAN_RECORD_START = 0x100001;
    public static final int MSG_SCAN_RECORD_FINISH = 0x100002;
    public static final int MSG_DEL_RECORD_SUCCESS = 0x100003;
    public static final int MSG_DEL_RECORD_FAILED = 0x100004;

    //sdcard格式化
    public static final int MSG_FORMAT_TIMEOUT = 0x100005;

    //拍照
    public static final int MSG_TAKE_PIC_TIMEOUT = 0x100006;
    public static final int MSG_TAKE_PIC_FINISH = 0x100007;

    //获取播放设置
    public static final int MSG_GET_PLAY_CONFIG_START = 0x100008;
    public static final int MSG_GET_PLAY_CONFIG_TIMEOUT = 0x100009;
    public static final int MSG_SET_PLAY_BRIGHTNESS_TIMEOUT = 0x100010;
    public static final int MSG_SET_PLAY_VOLUME_TIMEOUT = 0x100011;
    public static final int MSG_SET_PLAY_CONTRAST_TIMEOUT = 0x100012;
    public static final int MSG_SET_PLAY_SATURATION_TIMEOUT = 0x100013;
    public static final int MSG_SET_PLAY_SHARPNESS_TIMEOUT = 0x100014;
    public static final int MSG_GET_SETTINGS_TIMEOUT = 0x100010;

    //服务器录像文件
    public static final int MSG_SERVER_RECORD_START = 0x100011;
    public static final int MSG_SERVER_RECORD_TIMEOUT = 0x100012;
    public static final int MSG_SERVER_RECORD_SUCCESS = 0x100013;
    public static final int MSG_SERVER_RECORD_FAILED = 0x100014;
    public static final int MSG_DEL_FILE_TIMEOUT = 0x100015;
    public static final int MSG_DEL_FILE_SUCCESS = 0x100016;
    public static final int MSG_DEL_FILE_FAILED = 0x100017;

    //设置
    public static final int MSG_UPDATE_SETTINGS = 0x100018;
    public static final int MSG_FORMAT_SUCCESS = 0x100019;
    public static final int MSG_FORMAT_FAILED = 0x100020;
    public static final int MSG_RECOVERY_TIMEOUT = 0x100021;
    public static final int MSG_DOWNLOAD_FINISH = 0x100022;

    //播放
    public static final int MSG_PLAY_PREPARED = 0x100023;
    public static final int MSG_PLAY_FINISH = 0x100024;
    public static final int MSG_PLAY_CHECK = 0x100025;
    public static final int MSG_PLAY_TIMEOUT = 0x100026;
    public static final int MSG_PLAY_START = 0x100027;
    public static final int  MSG_CONNECT_ERROR = 0x100028;

    public static final int MSG_GET_SDCARDINFO_TIMEOUT = 0x100029;
}
