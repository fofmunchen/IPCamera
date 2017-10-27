package com.rockchip.tutk.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rockchip.media.Audio;
import com.rockchip.tutk.EncoderParameter;
import com.rockchip.tutk.PlayConfig;
import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKManager;
import com.rockchip.tutk.TUTKSession;
import com.rockchip.tutk.command.FileTransferCommand;
import com.rockchip.tutk.command.StartFileTransferCommand;
import com.rockchip.tutk.media.AacCodec;
import com.rockchip.tutk.model.MdNotifyInfo;
import com.rockchip.tutk.model.SdcardModel;
import com.rockchip.tutk.utils.LooperExecutor;
import com.rockchip.tutk.utils.MsgDatas;
import com.rockchip.tutk.utils.RecordUtils;
import com.rockchip.tutk.view.ConfigDialog;
import com.rockchip.tutk.view.PlayResolutionPop;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.AVFrame;
import com.tutk.IOTC.AVIOCTRLDEFs;
import com.tutk.IOTC.RDTAPIs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import static com.rockchip.tutk.media.AacCodec.SAMPLE_RATE;
import static com.tutk.IOTC.AVFrame.MEDIA_CODEC_VIDEO_H264;
import static com.tutk.IOTC.AVIOCTRLDEFs.AVIOCTRL_PTZ_DOWN;
import static com.tutk.IOTC.AVIOCTRLDEFs.AVIOCTRL_PTZ_LEFT;
import static com.tutk.IOTC.AVIOCTRLDEFs.AVIOCTRL_PTZ_LEFT_DOWN;
import static com.tutk.IOTC.AVIOCTRLDEFs.AVIOCTRL_PTZ_LEFT_UP;
import static com.tutk.IOTC.AVIOCTRLDEFs.AVIOCTRL_PTZ_RIGHT;
import static com.tutk.IOTC.AVIOCTRLDEFs.AVIOCTRL_PTZ_RIGHT_DOWN;
import static com.tutk.IOTC.AVIOCTRLDEFs.AVIOCTRL_PTZ_RIGHT_UP;
import static com.tutk.IOTC.AVIOCTRLDEFs.AVIOCTRL_PTZ_UP;
import static com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_AUDIOSTOP;
import static com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_PTZ_COMMAND;
import static com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_STOP;
import static com.tutk.IOTC.AVIOCTRLDEFs.RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ;

public class CameraActivity extends Activity implements View.OnClickListener,
		SurfaceHolder.Callback,TUTKDevice.NotifyCallback ,
		ConfigDialog.OnConfigDialogListener,
		PlayResolutionPop.OnPlayResolutionPopListener, TUTKDevice.OnTutkError, TUTKDevice.EncoderChangeCallback {
	@Override
	public void onEncoderChange(EncoderParameter parameter) {
		Log.d(TAG,"onEncoderChange");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if(null != mTUTKDevice && null != mTUTKDevice.getDeviceInfo()){
					mFps = mTUTKDevice.getDeviceInfo().getPlayFps();
				}
				reflushPlayResolution();
			}
		});
	}

	public enum RecordVideoStep {
		NOT_STATUS,//not record
		READY,//start record but not init
		INIT,//already init,but not idr
		IDR_GETTING,//get idr
		IDR_SAVED,//already save idr
		START_SPS//start saving sps
	}

	private static int mAudioSource = MediaRecorder.AudioSource.MIC;    /* 录音源 */

    private static int mAudioChannel = AudioFormat.CHANNEL_IN_MONO;    /* 录音的声道，单声道 */
	private static int mAudioFormat = AudioFormat.ENCODING_PCM_16BIT;  /* 量化的深度 */
    private static int mBufferSize = Audio.getBufferSize(mAudioChannel,mAudioFormat);    /* 缓存的大小 */
	static TUTKDevice mTUTKDevice;
	SurfaceView mSurfaceView;

//	View play;
	ImageButton speakButton;
	ImageView screenScale;
	private ImageButton btnVolume;
	private ImageButton btnConfig;
	private ImageButton btnRecordVideo;
	private TextView txtResolution;
	private LinearLayout layout_return;
	private boolean mIsScreenFull = true;
	private int mScreenWidth = 0;
	private int mScreenHeight = 0;
	private int mVideoWidth = 0;
	private int mVideoHeight = 0;
	int avIndex = -1;
	int MAX_BUF_SIZE = 1024;
	private Queue<byte[]> dataQueue = new ArrayDeque<>();
	private String TAG = "wz";
	private long recRate = 0;
	private long lastTime = 0;
	private TextView recRateText;
	private ConfigDialog mConfigDialog;
	private PlayResolutionPop mResolutionPop;
	private byte[] mCurrentSpsPps;
	private byte[] mCurrentIdrFrame;
	private String mAudioRecordName;
	private long mLastAudioBuffTime;

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MsgDatas.MSG_TAKE_PIC_TIMEOUT:
					Log.v(TAG, "save pic timeout");
					mIsRecordPic = false;
					Toast.makeText(CameraActivity.this, R.string.take_pic_failed,
							Toast.LENGTH_SHORT).show();
					break;
				case MsgDatas.MSG_TAKE_PIC_FINISH:
					Toast.makeText(CameraActivity.this, msg.obj.toString(),
							Toast.LENGTH_SHORT).show();
					break;
				case MsgDatas.MSG_GET_PLAY_CONFIG_START:
					try {
						JSONObject jsonObject = new JSONObject();
						jsonObject.put("name", PlayConfig.GetPlayConfig);
						sendPlayConfigMsg(jsonObject.toString(),
								MsgDatas.MSG_GET_PLAY_CONFIG_TIMEOUT);
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
				case MsgDatas.MSG_GET_PLAY_CONFIG_TIMEOUT:
					Toast.makeText(CameraActivity.this, R.string.txt_get_info_failed,
							Toast.LENGTH_SHORT).show();
					if (null != mConfigDialog && mConfigDialog.isShowing()) {
						mConfigDialog.cancel();
						mConfigDialog = null;
					}
					break;
				case MsgDatas.MSG_SET_PLAY_BRIGHTNESS_TIMEOUT:
					if (null != mConfigDialog && mConfigDialog.isShowing()) {
						mConfigDialog.refreshBright();
						Toast.makeText(CameraActivity.this, R.string.txt_play_bright_failed,
								Toast.LENGTH_SHORT).show();
					}
					break;
				case MsgDatas.MSG_SET_PLAY_VOLUME_TIMEOUT:
					if (null != mConfigDialog && mConfigDialog.isShowing()) {
						mConfigDialog.refreshVolume();
						Toast.makeText(CameraActivity.this, R.string.txt_play_volume_failed,
								Toast.LENGTH_SHORT).show();
					}
					break;
				case MsgDatas.MSG_SET_PLAY_CONTRAST_TIMEOUT:
					if (null != mConfigDialog && mConfigDialog.isShowing()) {
						mConfigDialog.refreshContrast();
						Toast.makeText(CameraActivity.this, R.string.txt_play_contrast_failed,
								Toast.LENGTH_SHORT).show();
					}
					break;
				case MsgDatas.MSG_SET_PLAY_SATURATION_TIMEOUT:
					if (null != mConfigDialog && mConfigDialog.isShowing()) {
						mConfigDialog.refreshSaturation();
						Toast.makeText(CameraActivity.this, R.string.txt_play_saturation_failed,
								Toast.LENGTH_SHORT).show();
					}
					break;
				case MsgDatas.MSG_SET_PLAY_SHARPNESS_TIMEOUT:
					if (null != mConfigDialog && mConfigDialog.isShowing()) {
						mConfigDialog.refreshSharpness();
						Toast.makeText(CameraActivity.this, R.string.txt_play_sharpness_failed,
								Toast.LENGTH_SHORT).show();
					}
					break;
			}
		}
	};
	LooperExecutor looperExecutor = new LooperExecutor();
	LooperExecutor musicExecutor = new LooperExecutor();
	AVAPIs av = new AVAPIs();
	private ByteBuffer[] decodeInputBuffers;
	private ByteBuffer[] decodeOutputBuffers;
	private RecordVideoStep mRecordVideoStep = RecordVideoStep.NOT_STATUS;
	private Object mRecordLock = new Object();
	private boolean mIsRecordPic = false;
    private long mPressPicButtonTime;
	private boolean mIsAlreadGetPic = false;
	private boolean isSpeak = false;
	private File takePicturefile;
	private String mRecodeName = "";
	private final boolean SAVE_WITH_h264 = false;
    private int mFps = MsgDatas.DEFAULT_FPS;
	AudioRecord mAudioRecord;
	private boolean mIsBackpress = false;
	private VideoThread mVideoThread;
	private boolean mNeedSendStart = true;
	private boolean mInitFfmpeg;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_camera2);
		init();
		initUI();
		initData();
		looperExecutor.requestStart();
		lastTime = SystemClock.elapsedRealtime();
		//int minBufSize =Audio.getBufferSize(mAudioChannel,mAudioFormat);//AudioRecord.getMinBufferSize(SAMPLE_RATE, mAudioChannel, mAudioFormat);

		Audio.init(mBufferSize);
	}

	private void initAudioTrack() {
		if (mAudioTrack == null) {
			// 获得构建对象的最小缓冲区大小
			//int minBufSize = Audio.getBufferSize(mAudioChannel,mAudioFormat);
			mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
					SAMPLE_RATE,
					AudioFormat.CHANNEL_CONFIGURATION_MONO,
					AudioFormat.ENCODING_PCM_16BIT,
					mBufferSize,
					AudioTrack.MODE_STREAM);
			mAudioTrack.play();
		}
	}

	@Override
	protected void onResume() {
		if (mTUTKDevice != null) {
			mTUTKDevice.addOnTutkErrorListener(this);
			mTUTKDevice.addEncoderChangeCallbacks(this);
		}
		mDisplay = true;
		mSurfaceView.refreshDrawableState();
		if (mPlayAudio){
			btnVolume.setBackground(getResources().getDrawable(R.drawable.volume_normal));
		}else{
			btnVolume.setBackground(getResources().getDrawable(R.drawable.mute_normal));
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (mTUTKDevice != null) {
			mTUTKDevice.removeOnTutkErrorListener(this);
			mTUTKDevice.removeEncoderChangeCallbacks(this);
		}
		if (!mIsBackpress) {
            /*if (null != mVideoThread) {
                mVideoThread.setCancel(true);
            }*/
			mPlayAudio = false;
			isSpeak = false;
			speakButton.setBackground(getResources().getDrawable(R.drawable.icon_lan_video_sound_off_pressed));
			mVideoWidth = 0;
			mVideoHeight = 0;
			//mPlay = false;
            /*if (null != looperExecutor) {
                looperExecutor.requestStop();
            }
            if (null != musicExecutor) {
                musicExecutor.requestStop();
            }*/
		}
		mHandler.removeMessages(MsgDatas.MSG_TAKE_PIC_TIMEOUT);
		mHandler.removeMessages(MsgDatas.MSG_GET_PLAY_CONFIG_TIMEOUT);
		mHandler.removeMessages(MsgDatas.MSG_GET_PLAY_CONFIG_START);
		if (null != mConfigDialog && mConfigDialog.isShowing()) {
			mConfigDialog.cancel();
			mConfigDialog = null;
		}
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		mPlayAudio = false;
		synchronized (dataQueue) {
			dataQueue.clear();
		}

		mIsBackpress = true;
		super.onBackPressed();
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@Override
	protected void onDestroy() {
		mPlayAudio = false;
		isSpeak = false;

		new Thread(new Runnable() {
			@Override
			public void run() {
				int ret;
				ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_AUDIOSTOP,
						new byte[8], 8);
				Log.d(TAG, "IOTYPE_USER_IPCAM_AUDIOSTOP:" + ret);
			}
		}).start();
		//AVAPIs.avClientStop(avIndex);
		stopCamera();
		if (mInitFfmpeg) {
			RecordUtils.DecodeRelease();
		}
		endRecordVideo(false);
		RecordUtils.mp4release();//also release even already endRecordVideo
		if(null != mTUTKDevice){
			mTUTKDevice.cancelThumbsThread();
		}
		mTUTKDevice.close();
		Audio.destory();
		super.onDestroy();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mDisplay = false;
	}

	private void stopCamera() {
		if (mPlay) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					int ret;
					ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_STOP,
							new byte[8], 8);
					Log.d(TAG, "IOTYPE_USER_IPCAM_STOP:" + ret);
				}
			}).start();

			mPlay = false;
			looperExecutor.requestStop();
			musicExecutor.requestStop();
		}
	}

	private void init() {
		String uid = getIntent().getStringExtra("UID");
		if (uid != null){// && uid.length() == 20) {
			mTUTKDevice = TUTKManager.getByUID(uid);
		}
	}

	private void initUI() {

		mSurfaceView = (SurfaceView) findViewById(R.id.surfaceView);

	//	mSurfaceView.setOnClickListener(sufaceClickListener);
		mSurfaceView.getHolder().addCallback(this);

	/*	play = findViewById(R.id.control_play);
		play.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mTUTKDevice != null) {
					play();
					//mTUTKDevice.getSession().write(new SettingsCommand().Json());
				}
			}
		});*/

	/*	findViewById(R.id.control_left_up).setOnClickListener(this);
		findViewById(R.id.control_up).setOnClickListener(this);
		findViewById(R.id.control_up_right).setOnClickListener(this);
		findViewById(R.id.control_left).setOnClickListener(this);
		findViewById(R.id.control_right).setOnClickListener(this);
		findViewById(R.id.control_left_down).setOnClickListener(this);
		findViewById(R.id.control_down).setOnClickListener(this);
		findViewById(R.id.control_right_down).setOnClickListener(this);*/
		screenScale = (ImageView) findViewById(R.id.btn_camera_fullscreen);
		recRateText = (TextView) findViewById(R.id.txt_camera_speed);
		screenScale.setOnClickListener(this);


		findViewById(R.id.btn_camera_photo).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
                /*TakePicture takePicture = new TakePicture();
                String json = takePicture.Json();
                mTUTKDevice.getSession().write(json);*/
				startTakePic();
			}
		});

		btnVolume = (ImageButton) findViewById(R.id.btn_camera_mute);
		btnVolume.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				if (mPlayAudio == false) {
					mTUTKDevice.startAudio(mHandler, new Runnable() {
						@Override
						public void run() {
							mPlayAudio = true;
							Thread audioThread = new Thread(new AudioThread(avIndex), "Audio Thread");
							audioThread.start();
							btnVolume.setBackground(getResources().getDrawable(R.drawable.volume_normal));
						}
					},null);
				} else {
					mPlayAudio = false;
					btnVolume.setBackground(getResources().getDrawable(R.drawable.mute_normal));
				}
			}
		});

		btnRecordVideo = (ImageButton) findViewById(R.id.btn_camera_videotape);
		btnRecordVideo.setOnClickListener(this);

		speakButton = (ImageButton) findViewById(R.id.btn_camera_intercom);
		speakButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				final ImageButton btn = (ImageButton) view;
				if (!isSpeak) {

					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								AacCodec.getInstance().setEncodeType(MediaFormat.MIMETYPE_AUDIO_AAC);
								AacCodec.getInstance().setDevice(mTUTKDevice);
								AacCodec.getInstance().prepare();
								mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, mAudioChannel, mAudioFormat, mBufferSize);
								short[] buffer = new short[mBufferSize / 2];
								mAudioRecord.startRecording();
								isSpeak = true;

								AacCodec.getInstance().startAsync();
								mHandler.post(new Runnable() {
									@Override
									public void run() {
										btn.setBackground(getResources().getDrawable(R.drawable.intercom_foucs));
									}
								});
								if (isSpeak){
									Audio.begin_tx();
								}
								while (isSpeak) {
									int bufferReadResult = mAudioRecord.read(buffer, 0, mBufferSize / 2);
									byte bData[] = short2byte(buffer);
									if (bufferReadResult > 0) {
										byte destData[] = new byte[bData.length*2];
										byte txOut[] = new byte[bData.length];
										int audio_size= Audio.audio_tx(bData, bData.length, destData, destData.length,txOut);
										if (mAudioTrack != null&& audio_size>0) {
											synchronized (mAudioTrack) {
												mAudioTrack.write(txOut, 0, audio_size);
											}
										}

										AacCodec.getInstance().putPCMData(destData);

									}
								}
								try {
									Audio.end_tx();
									mAudioRecord.stop();
									mAudioRecord.release();
									mAudioRecord = null;
								} catch (Exception e) {
									e.printStackTrace();
								}
								AacCodec.getInstance().stopAsync();
							}catch (Exception e){
								e.printStackTrace();
							}
						}
					}).start();
				} else {
					btn.setBackground(getResources().getDrawable(R.drawable.intercom_normal));
					isSpeak = false;
				}
			}
		});
		layout_return = (LinearLayout)findViewById(R.id.btn_title_return) ;
		layout_return.setOnClickListener(this);
		btnConfig = (ImageButton) findViewById(R.id.btn_camera_setting);
		btnConfig.setOnClickListener(this);
		txtResolution = (TextView) findViewById(R.id.txt_camera_resolution);
		txtResolution.setOnClickListener(this);
		reflushPlayResolution();
	}

	/**
	 * get current resolution
	 */
	private String getCurrentResolution() {

		String[] titleArray = this.getResources().getStringArray(R.array.pref_play_resolution_list_titles);
		String[] valueArray = this.getResources().getStringArray(R.array.pref_play_resolution_list_values);
		String mStr = null;
		for (int i = 0; i < titleArray.length; i++) {
			Log.d("abc", "----------------valueArray[i]=" + valueArray[i]);
            if (mTUTKDevice != null && mTUTKDevice.getDeviceInfo() != null && valueArray[i].equals(mTUTKDevice.getDeviceInfo().getPlayResolution())) {
				mStr = titleArray[i];
			}
		}
		return mStr;
	}


	MediaPlayer shootMediaPlayer;

	private void reflushPlayResolution(){
		Log.d("wz", "1==========" + getCurrentResolution());

        if (mTUTKDevice != null && mTUTKDevice.getDeviceInfo() != null && mTUTKDevice.getDeviceInfo().getPlayResolution() != null) {
			txtResolution.setText(getCurrentResolution());
		} else {
			txtResolution.setText(this.getResources().getStringArray(R.array.pref_play_resolution_list_titles)[0]);
		}
	}

	public void shootSound() {
		AudioManager meng = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
		int volume = meng.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

		if (volume != 0) {
			if (shootMediaPlayer == null)
				shootMediaPlayer = MediaPlayer.create(this, Uri.parse("file:///system/media/audio/ui/camera_click.ogg"));
			if (shootMediaPlayer != null)
				shootMediaPlayer.start();
		}
	}

	private byte[] short2byte(short[] sData) {
		int shortArrsize = sData.length;
		byte[] bytes = new byte[shortArrsize * 2];
		for (int i = 0; i < shortArrsize; i++) {
			bytes[i * 2] = (byte) (sData[i] & 0x00FF);
			bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
			sData[i] = 0;
		}
		return bytes;

	}


	private void stop() {
		stopCamera();
		clearDraw();
		mSurfaceView.setVisibility(ViewStub.GONE);
		mSurfaceView.setVisibility(ViewStub.VISIBLE);
	}


	private void play() {
		if (avIndex < 0) {
			avIndex = mTUTKDevice.getSession().getAVIndex();
		}
		if (mNeedSendStart) {
			if (mTUTKDevice.startIpcamStream(avIndex)){
				Log.v(TAG, "play()3.1");
				mNeedSendStart = false;
				if (looperExecutor.getState() == Thread.State.TERMINATED) {
					Log.v(TAG, "play()3.2");
					looperExecutor = new LooperExecutor();
					looperExecutor.requestStart();
					Log.v(TAG, "looperExecutor.requestStart()");
				}
				if (musicExecutor.getState() == Thread.State.TERMINATED) {
					musicExecutor = new LooperExecutor();
					musicExecutor.requestStart();
					Log.v(TAG, "musicExecutor.requestStart()");
				}
				if (null != mVideoThread) {
					mVideoThread.setCancel(true);
				}

				mPlay = true;
				mVideoThread = new VideoThread(avIndex);
				looperExecutor.execute(mVideoThread);
				Log.v(TAG, "send idr beforeplay");
				sendForceIdr();
			}
            /*try {
                Object o = new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] objects) {
                        int ret;
                        do {
                            AVAPIs.avSendIOCtrlExit(avIndex);
                            ret = av.avSendIOCtrl(avIndex, AVAPIs.IOTYPE_INNER_SND_DATA_DELAY, new byte[2], 2);
                            Log.d(TAG, String.format("IOTYPE_INNER_SND_DATA_DELAY:ret[%d],avIndex[%d]", ret, avIndex));
                            ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_START, new byte[8], 8);
                            Log.d(TAG, String.format("IOTYPE_USER_IPCAM_START:ret [%d] avIndex[%d]", ret, avIndex));

                            AVAPIs.avSendIOCtrlExit(avIndex);
                        }while (ret ==AVAPIs.AV_ER_SENDIOCTRL_ALREADY_CALLED);
                        return null;
                    }
                }.execute().get(2000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return;
            } catch (TimeoutException e) {
                e.printStackTrace();
                Toast.makeText(getApplication(), "连接超时", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }*/
		}

	}

	private void sendPTZ(final int ptz) {

		final ProcessThread processThread=new ProcessThread(new Runnable() {
			@Override
			public void run() {
				int ret;
				byte[] bytes = new byte[1];
				bytes[0] = (byte) ptz;
				ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_PTZ_COMMAND, bytes, 1);
				Log.d(TAG, "IOTYPE_INNER_SND_DATA_DELAY:" + ret);

			}
		});
		processThread.start();

		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				processThread.setCancle(true);
			}
		}, 3000);
	}

	private void initData() {
		TUTKSession session = null;
		if (mTUTKDevice != null ) {
			session = mTUTKDevice.getSession();
			if (session != null){
				String uid = mTUTKDevice.getSession().getUID();
				if (uid != null) {
					setTitle(uid);
					mTUTKDevice.addListener(this);
				}
			}
		}
		if ( session == null ||  mTUTKDevice == null) {
			Log.e(TAG, "finish cameraactivity becase null");
			Toast.makeText(this, R.string.device_view_status, Toast.LENGTH_SHORT).show();
			finish();
		}
	}

	boolean mPlay = false;
	boolean mPlayAudio = false;
	boolean mDisplay = false;

	public void clearDraw() {
		SurfaceHolder surfaceHolder = mSurfaceView.getHolder();
		Canvas canvas = null;//new Canvas(Bitmap.createBitmap(mSurfaceView.getWidth(),mSurfaceView.getHeight(),Bitmap.Config.ARGB_8888));
		try {
			canvas = surfaceHolder.lockCanvas(null);
			canvas.drawColor(Color.WHITE);
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);

		} catch (Exception e) {
			e.printStackTrace();

		} finally {

			if (canvas != null) {

				surfaceHolder.unlockCanvasAndPost(canvas);
			}
		}
	}

	public static boolean startIpcamStream(int avIndex) {
		AVAPIs av = new AVAPIs();
		int ret = av.avSendIOCtrl(avIndex, AVAPIs.IOTYPE_INNER_SND_DATA_DELAY,
				new byte[2], 2);
		if (ret < 0) {
			System.out.printf("start_ipcam_stream failed[%d]\n", ret);
			return false;
		}
		// This IOTYPE constant and its corrsponsing data structure is defined in
		// Sample/Linux/Sample_AVAPIs/AVIOCTRLDEFs.h
		//
		int IOTYPE_USER_IPCAM_START = 0x1FF;
		ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_START,
				new byte[8], 8);
		if (ret < 0) {
			System.out.printf("start_ipcam_stream failed[%d]\n", ret);
			return false;
		}
/*
        int IOTYPE_USER_IPCAM_AUDIOSTART = 0x300;
        ret = av.avSendIOCtrl(avIndex, IOTYPE_USER_IPCAM_AUDIOSTART,
                new byte[8], 8);
        if (ret < 0) {
            System.out.printf("start_ipcam_stream failed[%d]\n", ret);
            return false;
        }
*/
		return true;
	}

	int rdt_id = -1;

	private void sendFile() {
		if (rdt_id < 0) {
			rdt_id = mTUTKDevice.getSession().getRDTIndex();
		}
		Log.d(TAG, "rdt_id=" + rdt_id);
		FileTransferCommand fileTransferCommand = new FileTransferCommand("/etc/profile");
		String json = fileTransferCommand.Json();
		int write = RDTAPIs.RDT_Write(rdt_id, json.getBytes(), json.getBytes().length);
		Log.d(TAG, "write:" + write);
		byte[] buff = new byte[1024];
		int rdt_read = RDTAPIs.RDT_Read(rdt_id, buff, 1024, 3000);
		String fileInfo = new String(buff, 0, rdt_read);
		try {
			JSONObject jsonObject = new JSONObject(fileInfo);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		StartFileTransferCommand startFileTransferCommand = new StartFileTransferCommand("/etc/profile");
		String commad = startFileTransferCommand.Json();
		write = RDTAPIs.RDT_Write(rdt_id, commad.getBytes(), commad.getBytes().length);
		Log.d(TAG, "write:" + write);

		while (true) {
			try {
				int i = RDTAPIs.RDT_Read(rdt_id, buff, buff.length, 3000);
				String msg = new String(buff, 0, i);
				Log.d(TAG, "pppppppppp:" + msg);
				if (i <= 0 || i < buff.length) {
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
		Log.d(TAG, "end");
	}

	@Override
	public void onClick(View view) {
		int id = view.getId();
		switch (id) {
	/*		case R.id.control_left_up: {
				sendPTZ(AVIOCTRL_PTZ_LEFT_UP);
				break;
			}
			case R.id.control_up: {
				sendPTZ(AVIOCTRL_PTZ_UP);
				break;
			}
			case R.id.control_up_right: {
				sendPTZ(AVIOCTRL_PTZ_RIGHT_UP);
				break;
			}
			case R.id.control_left: {
				sendPTZ(AVIOCTRL_PTZ_LEFT);
				break;
			}
			case R.id.control_right: {
				sendPTZ(AVIOCTRL_PTZ_RIGHT);
				break;
			}
			case R.id.control_left_down: {
				sendPTZ(AVIOCTRL_PTZ_LEFT_DOWN);
				break;
			}
			case R.id.control_down: {
				sendPTZ(AVIOCTRL_PTZ_DOWN);
				break;
			}
			case R.id.control_right_down: {
				sendPTZ(AVIOCTRL_PTZ_RIGHT_DOWN);
				break;
			}*/
			case R.id.btn_camera_fullscreen:{
					scaleSurface();
			}
			case R.id.btn_camera_setting: {
				if (null != mTUTKDevice) {
					if (null != mConfigDialog) {
						mConfigDialog.cancel();
					}
					mConfigDialog = new ConfigDialog(this, mTUTKDevice.getDeviceInfo(), this);
					mConfigDialog.show();
					mHandler.sendEmptyMessageDelayed(MsgDatas.MSG_GET_PLAY_CONFIG_START, 1000);
				}
				break;
			}
			case R.id.txt_camera_resolution: {
				if (null == mResolutionPop) {
					mResolutionPop = new PlayResolutionPop(this, this, mTUTKDevice);
					mResolutionPop.showPop(view);
				}
				break;
			}
			case R.id.btn_camera_videotape: {
				if (RecordVideoStep.NOT_STATUS == mRecordVideoStep) {
					startRecordVideo();
				} else {
					endRecordVideo(true );
				}
				break;
			}
			case R.id.btn_title_return:
			{
				mPlayAudio = false;
				synchronized (dataQueue) {
					dataQueue.clear();
				}

				mIsBackpress = true;
				CameraActivity.this.finish();
				break;
			}
			default:
				break;
		}
	}



	private void scaleSurface() {
		if (mVideoWidth < 1 || mVideoHeight < 1) {
			Log.w(TAG, "video not play");
			return;
		}
		if (0 == mScreenWidth || 0 == mScreenHeight) {
			mScreenWidth = mSurfaceView.getWidth();
			mScreenHeight = mSurfaceView.getHeight();
		}
		float scaleWidth = (float) mVideoWidth / mScreenWidth;
		float scaleHeight = (float) mVideoHeight / mScreenHeight;
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		if (mIsScreenFull) {
			screenScale.setImageLevel(1);
			if (scaleWidth > scaleHeight) {
				int h = (int) (mScreenHeight - (mVideoHeight / scaleWidth)) / 2;
				lp.setMargins(0, h, 0, h);
			} else {
				int w = (int) (mScreenWidth - (mVideoWidth / scaleHeight)) / 2;
				lp.setMargins(w, 0, w, 0);
			}
		} else {
			screenScale.setImageLevel(0);
		}
		zoomMin = 0;
		moveHorStep = 0;
		moveVerStep = 0;
		mSurfaceView.setLayoutParams(lp);
		mIsScreenFull = !mIsScreenFull;
	}

	int leftMargin = 0;
	int rightMargin = 0;
	int topMargin = 0;
	int bottomMargin = 0;
	private void scaleVedioSurface(float zoomLevel,int horStep,int verSetp) {
		if (mVideoWidth < 1 || mVideoHeight < 1) {
			Log.w(TAG, "video not play");
			return;
		}
		if (0 == mScreenWidth || 0 == mScreenHeight) {
			mScreenWidth = mSurfaceView.getWidth();
			mScreenHeight = mSurfaceView.getHeight();
		}
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		if(horStep==0 && verSetp==0){
			leftMargin = 0;
			topMargin = 0;
			moveHorStep = 0;
			moveVerStep = 0;
			rightMargin = -(int)(mScreenWidth*zoomLevel/2);
			bottomMargin = -(int)(mScreenHeight*zoomLevel/2);
//            lp.setMargins(0,0, -(int)(mScreenWidth*zoomLevel/2),-(int)(mScreenHeight*zoomLevel/2));
			lp.setMargins(leftMargin,topMargin, rightMargin,bottomMargin);
		}else{
			leftMargin = -horStep;
			topMargin = -verSetp;
			rightMargin = -(int)(mScreenWidth*zoomLevel/2) + horStep;
			bottomMargin = -(int)(mScreenHeight*zoomLevel/2) + verSetp;
			if(leftMargin>=0){
				leftMargin =0;
				rightMargin = -(int)(mScreenWidth*zoomLevel/2);
				moveHorStep = 0;
			}
			if(topMargin>=0){
				topMargin = 0;
				bottomMargin  = -(int)(mScreenHeight*zoomLevel/2);
				moveVerStep = 0;
			}
			if(rightMargin>=0){
				rightMargin = 0;
				leftMargin = -(int)(mScreenWidth*zoomLevel/2);
			}
			if(bottomMargin>=0){
				topMargin = -(int)(mScreenHeight*zoomLevel/2);
				bottomMargin = 0;
			}
			lp.setMargins(leftMargin,topMargin, rightMargin,bottomMargin);

		}

		mSurfaceView.setLayoutParams(lp);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(TAG, "surfaceCreated");
		play();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	@Override
	public void onEvent(MdNotifyInfo notifyInfo) {

	}

	@Override
	public void onPictureTaked(String patch) {
		if (rdt_id < 0) {
			rdt_id = mTUTKDevice.getSession().getRDTIndex();
		}
		Log.d(TAG, "rdt_id=" + rdt_id);
		FileTransferCommand fileTransferCommand = new FileTransferCommand(patch);
		String json = fileTransferCommand.Json();
		Log.d(TAG, "send:" + json);
		int write = RDTAPIs.RDT_Write(rdt_id, json.getBytes(), json.getBytes().length);
		Log.d(TAG, "write:" + write);
		byte[] buff = new byte[1024 * 200];
		int rdt_read = RDTAPIs.RDT_Read(rdt_id, buff, buff.length, 3000);
		long fileSize = 0;
		String fileInfo = new String(buff, 0, rdt_read);

		try {
			JSONArray jsonArray = new JSONArray(fileInfo);
			JSONObject jsonObject = jsonArray.getJSONObject(0);
			fileSize = jsonObject.getLong("FileSize");
			String value = jsonObject.getString("value");
			takePicturefile = new File(value);
			String name = takePicturefile.getName();
			String dir = "/sdcard/IPC/Picture/";
			new File(dir).mkdirs();
			takePicturefile = new File(dir + name);
			if (takePicturefile.exists()) {
				takePicturefile.delete();
			}
			try {
				takePicturefile.createNewFile();
				Log.d(TAG, takePicturefile.getAbsolutePath() + " create success!");
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}

		} catch (JSONException e) {
			e.printStackTrace();
			return;
		}

		StartFileTransferCommand startFileTransferCommand = new StartFileTransferCommand(patch);
		String commad = startFileTransferCommand.Json();
		write = RDTAPIs.RDT_Write(rdt_id, commad.getBytes(), commad.getBytes().length);
		Log.d(TAG, "write:" + write);
		long receive = 0;
		OutputStream outputStream = null;
		BufferedOutputStream bufferedOutputStream = null;
		while (receive < fileSize) {
			try {
				int i = RDTAPIs.RDT_Read(rdt_id, buff, buff.length, 3000);
				if (outputStream == null) {
					outputStream = new FileOutputStream(takePicturefile, true);
					bufferedOutputStream = new BufferedOutputStream(outputStream);
				}
				Log.d(TAG, "receive:" + i);
				if (i <= 0) {
					Log.d(TAG, "break receive");
					break;
				}
				bufferedOutputStream.write(buff, 0, i);
				receive += i;
				Log.d(TAG, "fileSize:" + fileSize + " received=" + receive);
				if (receive == fileSize) {
					Log.d(TAG, "receive end");
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}
		}
		try {
			bufferedOutputStream.flush();
			bufferedOutputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Log.d(TAG, "end receive");
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(CameraActivity.this, takePicturefile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
			}
		});
	}


	@Override
	public void onSdcardAlarm(SdcardModel model) {

	}

	@Override
	public void onPlayConfigLoaded(int msgWhat, boolean result) {
		if (null != mConfigDialog && mConfigDialog.isShowing()) {
			mHandler.removeMessages(msgWhat);
			if (MsgDatas.MSG_GET_PLAY_CONFIG_TIMEOUT == msgWhat) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mConfigDialog.loadFinish();
					}
				});
			}else{
				if(!result){
					mHandler.sendEmptyMessage(msgWhat);
				}
			}
		}
	}

	@Override
	public void onRecordListLoaded(String json) {

	}

	@Override
	public void onDelFile(String json) {

	}

	private void sendPlayConfigMsg(final String msg, final int msgWhat){
		new Thread(){
			@Override
			public void run() {
				int write = -1;
				mHandler.sendEmptyMessageDelayed(msgWhat, MsgDatas.NETWORK_TIMEOUT);
				try {
					byte[] bytes = msg.getBytes();
					write = AVAPIs.avSendIOCtrl(avIndex,
							RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, bytes, bytes.length);
				} catch (Exception e) {
					e.printStackTrace();
				}
				Log.v(TAG, write + ", send playmsg= " + msg);
				if (write != 0) {
					mHandler.removeMessages(msgWhat);
					mHandler.sendEmptyMessage(msgWhat);
				}
			}
		}.start();
	}


	@Override
	public void brightChange(int value) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", PlayConfig.PlayBrightness);
			jsonObject.put("value", Integer.valueOf(value));
			sendPlayConfigMsg(jsonObject.toString(),
					MsgDatas.MSG_SET_PLAY_BRIGHTNESS_TIMEOUT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void volumeChange(int value) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", PlayConfig.PlayVolume);
			jsonObject.put("value", Integer.valueOf(value));
			sendPlayConfigMsg(jsonObject.toString(),
					MsgDatas.MSG_SET_PLAY_VOLUME_TIMEOUT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void contrastChange(int value) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", PlayConfig.PlayContrast);
			jsonObject.put("value", Integer.valueOf(value));
			sendPlayConfigMsg(jsonObject.toString(),
					MsgDatas.MSG_SET_PLAY_CONTRAST_TIMEOUT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void saturationChange(int value) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", PlayConfig.PlaySaturation);
			jsonObject.put("value", Integer.valueOf(value));
			sendPlayConfigMsg(jsonObject.toString(),
					MsgDatas.MSG_SET_PLAY_SATURATION_TIMEOUT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sharpnessChange(int value) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("name", PlayConfig.PlaySharpness);
			jsonObject.put("value", Integer.valueOf(value));
			sendPlayConfigMsg(jsonObject.toString(),
					MsgDatas.MSG_SET_PLAY_SHARPNESS_TIMEOUT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean playResolutionClick(String title, String val) {
		String[] data = getResources().getStringArray(R.array.pref_play_resolution_list_titles);
        /*CommandBase command=new ExposureCommand(String.valueOf(val));
        String json = command.Json();
        int write = mTUTKDevice.getSession().write(json);
        if (write != 0){
            Toast.makeText(this, R.string.txt_bright_failed, Toast.LENGTH_SHORT).show();
            return false;
        }
        mTUTKDevice.getDeviceInfo().setmResolution(val);*/
		boolean ret = true;
		if (ret) {
			txtResolution.setText(title);
			mResolutionPop.hidePop();
			mResolutionPop = null;
		}
		return ret;
	}

	@Override
	public void onError(final int code) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(CameraActivity.this, String.format("与设备断开连接 [%d]", code), Toast.LENGTH_SHORT).show();
			}
		});
		finish();
	}

	public class VideoThread implements Runnable {
		static final int VIDEO_BUF_SIZE = 1000000;
		static final int FRAME_INFO_SIZE = 24;

		private int avIndex;
		private String TAG = "VideoThread";
		private boolean cancel;

		public VideoThread(int avIndex) {
			this.avIndex = avIndex;
		}

		public void setCancel(boolean cancel) {
			this.cancel = cancel;
		}

		@Override
		public void run() {
			int ret = -1;
			System.out.printf("[%s] Start\n",
					Thread.currentThread().getName());
			long errorTime = 0;
			AVAPIs av = new AVAPIs();
			byte[] frameInfo = new byte[FRAME_INFO_SIZE];
			byte[] videoBuffer = new byte[VIDEO_BUF_SIZE];
			int[] outBufSize = new int[1];
			int[] outFrameSize = new int[1];
			int[] outFrmInfoBufSize = new int[1];
			while (mPlay && !cancel) {
				int[] frameNumber = new int[1];
				ret = av.avRecvFrameData2(avIndex, videoBuffer,
						VIDEO_BUF_SIZE, outBufSize, outFrameSize,
						frameInfo, FRAME_INFO_SIZE,
						outFrmInfoBufSize, frameNumber);
				if (ret == AVAPIs.AV_ER_DATA_NOREADY) {
					if (errorTime == 0) {
						errorTime = SystemClock.elapsedRealtime();
					}
					if (SystemClock.elapsedRealtime() - errorTime > 5000) {
						Log.v(TAG, "=====not ready time out =====");
						onError(ret);
						break;
					}
					try {
						Thread.sleep(30);
					} catch (InterruptedException e) {
						System.out.println(e.getMessage());
					}
					continue;
				} else if (ret == AVAPIs.AV_ER_INVALID_ARG) {
					Log.v(TAG, "ret==AV_ER_INVALID_ARG");
					onError(ret);
					break;
				} else if (ret == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
					System.out.printf("VideoThread[%s] Lost video frame number[%d]\n",
							Thread.currentThread().getName(), frameNumber[0]);
					continue;
				} else if (ret == AVAPIs.AV_ER_INCOMPLETE_FRAME) {
					System.out.printf("VideoThread[%s] Incomplete video frame number[%d]\n",
							Thread.currentThread().getName(), frameNumber[0]);
					continue;
				} else if (ret == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
					System.out.printf("VideoThread[%s] AV_ER_SESSION_CLOSE_BY_REMOTE\n",
							Thread.currentThread().getName());
					Toast.makeText(CameraActivity.this, "AV_ER_SESSION_CLOSE_BY_REMOTE", Toast.LENGTH_SHORT).show();
					break;
				} else if (ret == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
					System.out.printf("VideoThread[%s] AV_ER_REMOTE_TIMEOUT_DISCONNECT\n",
							Thread.currentThread().getName());
					Toast.makeText(CameraActivity.this, "AV_ER_REMOTE_TIMEOUT_DISCONNECT", Toast.LENGTH_SHORT).show();
					break;
				} else if (ret == AVAPIs.AV_ER_INVALID_SID) {
					System.out.printf("VideoThread[%s] Session cant be used anymore\n",
							Thread.currentThread().getName());
					Toast.makeText(CameraActivity.this, "AV_ER_INVALID_SID", Toast.LENGTH_SHORT).show();
					break;
				} else if (ret < 0) {
					Log.d("VideoThread", "ret ==" + ret);

				}
				errorTime = 0;
				if (ret > 0) {
					recRate = recRate + ret;
					long elapsedRealtime = SystemClock.elapsedRealtime();
					if (elapsedRealtime - lastTime >= 1000) {
						mHandler.post(new Runnable() {
							@Override
							public void run() {
								recRateText.setText(String.valueOf(recRate / 1024) + "KB ");
								recRate = 0;
							}
						});
						lastTime = elapsedRealtime;
					}
				} else {
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							recRateText.setText("0KB");
							recRate = 0;
						}
					});
				}
				if (mDisplay) {
					Log.i(TAG, "ready in videoBuffer");
					AVFrame avFrame = new AVFrame(frameNumber[0], (byte) 0, frameInfo, videoBuffer, outBufSize[0]);
					if (mCodec == null && avFrame.getCodecId() == MEDIA_CODEC_VIDEO_H264) {
						mVideoWidth = avFrame.getVideoWidth();
						mVideoHeight = avFrame.getVideoHeight();

						try {
							initDecoder("video/avc", mVideoWidth, mVideoHeight);
						} catch (Exception e) {
							e.printStackTrace();
							mCodec.stop();
							mCodec.release();
							mCodec = null;
						}
					}

					int videoWidth = avFrame.getVideoWidth();
					int videoHeight = avFrame.getVideoHeight();
					if(mVideoWidth != videoWidth || mVideoHeight != videoHeight){
						if(mRecordVideoStep != RecordVideoStep.NOT_STATUS){
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									btnRecordVideo.performClick();
								}
							});
						}
						mVideoWidth = videoWidth;
						mVideoHeight = videoHeight;
					}

					if (mCodec != null && mPlay) {
						try {
							//save video
							synchronized (mRecordLock) {
								pakingVideo(avFrame);
							}

							//save pic
							if (mIsRecordPic && !mIsAlreadGetPic && isIdrFrame(avFrame)) {
								//mIsRecordPic = false;
								Log.v(TAG, "get idr to save pic");
								mIsAlreadGetPic = true;
								int len = avFrame.getFrmSize();
								byte[] data = new byte[len];
								System.arraycopy(avFrame.frmData, 0, data, 0, len);
								saveData2Pic(data);
							}

							//Log.d(TAG,"getFrmNo-:"+avFrame.getFrmNo());
							long startTime = System.currentTimeMillis();
							int count = 0;
							while(ret >= 0 && onFrame(avFrame) == 0){
								if(System.currentTimeMillis() - startTime > 100){
									break;
								}else{
									count++;
									Log.e(TAG, "try onFrame "+count);
									Thread.sleep(10);
								}
							}
							//Log.d(TAG, "onFrame:" + b+"getFrmNo:"+avFrame.getFrmNo());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			if (mCodec != null) {
				try {
					mCodec.stop();
				} catch (Exception e) {
					Log.e(TAG, "mCodec.stop error");
					e.printStackTrace();
				}
				mCodec.release();
				mCodec = null;
			}

			System.out.printf("Video [%s] Exit ret= [%d]\n",
					Thread.currentThread().getName(), ret);
		}
	}

	private void sendForceIdr() {
		//send force Idr
		new Thread() {
			@Override
			public void run() {
				JSONObject jsonObject = new JSONObject();
				try {
					jsonObject.put("name", "ForceIdr");
					jsonObject.put("type", true);
					byte[] bytes = jsonObject.toString().getBytes();
					for (int i = 0; i < 2; i++) {
						int write = AVAPIs.avSendIOCtrl(avIndex,
								RK_IOTC_IOTYPE_USER_RK_CONTROL_MSG_REQ, bytes, bytes.length);
						if (write == 0) {
							Log.v(TAG, "send force idr success");
							break;
						} else {
							Log.v(TAG, "send force idr failed");
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	private boolean isIdrFrame(AVFrame avFrame){
		if(null != avFrame && null != avFrame.frmData && avFrame.frmData.length > 20
				&& avFrame.frmData[0] == 0 && avFrame.frmData[1] == 0 && avFrame.frmData[2] == 1
				&& avFrame.frmData[3] == 103 && avFrame.frmData[4] == 100){
			return true;
		}
		if(avFrame.isIFrame()){
			Log.e(TAG, "not idr but flag is");
			//return true;
		}
		return false;
	}

	private void startRecordVideo() {
		mRecordVideoStep = RecordVideoStep.READY;
		btnRecordVideo.setBackground(getResources().getDrawable(R.drawable.videotape_foucs));
		mRecodeName = RecordUtils.getRecordSaveName(SAVE_WITH_h264 ? ".h264" : ".mp4");
		boolean ret = RecordUtils.mkdirPath(RecordUtils.getRecordPath());
		if (!ret) {
			Toast.makeText(CameraActivity.this, R.string.crete_file_error,
					Toast.LENGTH_SHORT).show();
			mRecordVideoStep = RecordVideoStep.NOT_STATUS;
			return;
		}
		File file = new File(mRecodeName);
		if (file.exists()) {
			file.delete();
		}
		if (SAVE_WITH_h264) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mRecordVideoStep = RecordVideoStep.INIT;
		} else {
			for (int i = 0; i < 2; i++) {
				sendForceIdr();
			}
			mRecordVideoStep = RecordVideoStep.INIT;
		}
	}

	private void pakingVideo(AVFrame avFrame) {
		if (RecordVideoStep.INIT == mRecordVideoStep) {
			boolean isIdrFrame = isIdrFrame(avFrame);
			if(SAVE_WITH_h264 && isIdrFrame){
				mRecordVideoStep = RecordVideoStep.IDR_GETTING;
			}else if (isIdrFrame) {
				int len = 50;
				mCurrentIdrFrame = new byte[len];
				System.arraycopy(avFrame.frmData, 0, mCurrentIdrFrame, 0, len);
				if (mVideoWidth > 0 && mVideoHeight > 0 /*&& null != mFirstFlame*/) {
					mCurrentSpsPps = RecordUtils.mp4init(mRecodeName, 1, mVideoWidth, mVideoHeight, mFps, mCurrentIdrFrame);
					if (null != mCurrentSpsPps) {
						sendForceIdr();
						mRecordVideoStep = RecordVideoStep.IDR_GETTING;
					}
				}
				if(RecordVideoStep.INIT == mRecordVideoStep){
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							btnRecordVideo.setBackground(getResources().getDrawable(R.drawable.videotape_normal));
							Toast.makeText(CameraActivity.this, R.string.record_init_failed,
									Toast.LENGTH_SHORT).show();
						}
					});
					mRecordVideoStep = RecordVideoStep.NOT_STATUS;
					return;
				}
			}
		}

		if (RecordVideoStep.IDR_GETTING == mRecordVideoStep
				|| RecordVideoStep.IDR_SAVED == mRecordVideoStep) {
			if (SAVE_WITH_h264) {
				try {
					OutputStream outputStream = new FileOutputStream(mRecodeName, true);
					BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
					bufferedOutputStream.write(avFrame.frmData, 0, avFrame.getFrmSize());
					bufferedOutputStream.flush();
					bufferedOutputStream.close();
					if (RecordVideoStep.IDR_GETTING == mRecordVideoStep) {
						mRecordVideoStep = RecordVideoStep.IDR_SAVED;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				try {
					int len = avFrame.getFrmSize();
					byte[] data = null;
					if(isIdrFrame(avFrame)){
						data = new byte[len];
						System.arraycopy(avFrame.frmData, 0, data, 0, len);
					} else {
						data = new byte[len + mCurrentSpsPps.length + 6];
						System.arraycopy(mCurrentIdrFrame, 0, data, 0, mCurrentSpsPps.length + 6);
						System.arraycopy(avFrame.frmData, 0, data, mCurrentSpsPps.length + 6, len);
					}
					boolean retPack = RecordUtils.mp4packVideo(data, data.length, 1/*ret>10000?1:0*/);
					//RecordUtils.mp4packVideo(avFrame.frmData, avFrame.getFrmSize(), 1);
					if (RecordVideoStep.IDR_GETTING == mRecordVideoStep) {
						mLastAudioBuffTime = System.currentTimeMillis();
						mRecordVideoStep = retPack ? RecordVideoStep.IDR_SAVED : RecordVideoStep.INIT;
					}
				} catch (Exception e) {
					Log.v(TAG, "happen error when packvideo to mp4");
					e.printStackTrace();
				}
			}
		}
	}

	private void endRecordVideo(boolean withToast) {
		synchronized (mRecordLock) {
			if (TextUtils.isEmpty(mRecodeName)) {
				mRecordVideoStep = RecordVideoStep.NOT_STATUS;
				btnRecordVideo.setBackground(getResources().getDrawable(R.drawable.videotape_normal));
				return;
			}
			if (RecordVideoStep.IDR_SAVED == mRecordVideoStep) {
				if(!SAVE_WITH_h264){
					mRecordVideoStep = RecordVideoStep.START_SPS;
					RecordUtils.mp4savesps();
					RecordUtils.mp4close();
					RecordUtils.mp4release();
					Log.v(TAG, "close mp4");
				}
				if(withToast){
					Toast.makeText(CameraActivity.this, mRecodeName, Toast.LENGTH_SHORT).show();
				}else{
					Log.v(TAG, "endRecordVideo not toast, maybe finish");
				}
			} else if(RecordVideoStep.NOT_STATUS != mRecordVideoStep){
				Log.v(TAG, "record video failed current state=" + mRecordVideoStep);
				File file = new File(mRecodeName);
				if (file.exists()) {
					file.delete();
				}
				if(withToast){
					Toast.makeText(CameraActivity.this, R.string.take_record_failed, Toast.LENGTH_SHORT).show();
				}else{
					Log.v(TAG, "endRecordVideo not toast, maybe finish");
				}
			}
			mRecodeName = "";
			mRecordVideoStep = RecordVideoStep.NOT_STATUS;
			btnRecordVideo.setBackground(getResources().getDrawable(R.drawable.videotape_normal));
			mLastAudioBuffTime = 0;
		}
	}

	private boolean pakingAudio(AVFrame avFrame){
		if(RecordVideoStep.IDR_SAVED == mRecordVideoStep){
			OutputStream os = null;
			BufferedOutputStream bos = null;
			try{
				if(null == mAudioRecordName && !TextUtils.isEmpty(mRecodeName)){
					RecordUtils.mkdirPath(RecordUtils.getRecordAudioPath());
					mAudioRecordName = RecordUtils.getRecordAudioSaveName(mRecodeName, ".rkaac");
					File file = new File(mAudioRecordName);
					if(file.exists()){
						file.delete();
					}
					file.createNewFile();
				}
				if(null != mAudioRecordName){
					os = new FileOutputStream(mAudioRecordName, true);
					bos = new BufferedOutputStream(os);
					int timeDuration = 0;
					long curTime = System.currentTimeMillis();
					if(mLastAudioBuffTime != 0){
						timeDuration = (int)(curTime - mLastAudioBuffTime);
					}
					mLastAudioBuffTime = curTime;
					int len = avFrame.getFrmSize();
					byte[] byteDataLen = RecordUtils.intToBytes(len);
					byte[] byteInterval = RecordUtils.intToBytes(timeDuration);
					bos.write(byteDataLen);
					bos.write(byteInterval);
					bos.write(avFrame.frmData, 0, len);
					bos.flush();
					return true;
				}
			} catch (Exception e){
				Log.e(TAG, "happen error when paking audio");
				e.printStackTrace();
			} finally {
				if(null != os){
					try {
						os.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					os = null;
				}
				if(null != bos){
					try {
						bos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					bos = null;
				}
			}
		}
		return false;
	}

	private void startTakePic() {
		if (!mIsRecordPic) {
			mPressPicButtonTime = System.currentTimeMillis();
			shootSound();
			mIsRecordPic = true;
			mIsAlreadGetPic = false;
			Log.v(TAG, "press takePic button");
			if (!RecordUtils.mkdirPath(RecordUtils.getRecordPath())) {
				mIsRecordPic = false;
				Toast.makeText(CameraActivity.this, R.string.crete_file_error, Toast.LENGTH_SHORT).show();
				return;
			} else if (!mInitFfmpeg) {
				if (mVideoWidth < 1 || mVideoHeight < 1
						|| 1 != RecordUtils.DecodeInit(mVideoWidth, mVideoHeight)) {
					mIsRecordPic = false;
					Toast.makeText(CameraActivity.this, R.string.crete_file_error, Toast.LENGTH_SHORT).show();
					return;
				} else {
					mInitFfmpeg = true;
				}
			}
			//send force Idr
			sendForceIdr();
			mHandler.sendEmptyMessageDelayed(MsgDatas.MSG_TAKE_PIC_TIMEOUT, MsgDatas.TAKEPIC_TIMEOUT);
		}
	}

	private void saveData2Pic(final byte[] data) {
		new Thread() {
			@Override
			public void run() {
				String savePath = RecordUtils.takePic(data, mVideoWidth, mVideoHeight);
				if (null != savePath) {
					mHandler.removeMessages(MsgDatas.MSG_TAKE_PIC_TIMEOUT);
					Message msg = new Message();
					msg.what = MsgDatas.MSG_TAKE_PIC_FINISH;
					msg.obj = savePath;
					mHandler.sendMessage(msg);
					mIsRecordPic = false;
				} else if(System.currentTimeMillis() - mPressPicButtonTime < MsgDatas.TAKEPIC_TRY_TIME){
					Log.w(TAG, "take pic failed and try again");
					mPressPicButtonTime = 0;
					mIsAlreadGetPic = false;
					sendForceIdr();
				} else if(mIsRecordPic){
					mHandler.removeMessages(MsgDatas.MSG_TAKE_PIC_TIMEOUT);
					Message msg = new Message();
					msg.what = MsgDatas.MSG_TAKE_PIC_FINISH;
					msg.obj = getString(R.string.take_pic_failed);
					mHandler.sendMessage(msg);
					mIsRecordPic = false;
				}
			}
		}.start();
	}

	/*
	View.OnClickListener sufaceClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View view) {
			if (controlPanel.getVisibility() == View.VISIBLE) {
				controlPanel.setVisibility(View.INVISIBLE);
				screenScale.setVisibility(View.INVISIBLE);
				if (null != mResolutionPop) {
					mResolutionPop.hidePop();
					mResolutionPop = null;
				}
				txtResolution.setVisibility(View.INVISIBLE);
			} else {
				controlPanel.setVisibility(View.VISIBLE);
				screenScale.setVisibility(View.VISIBLE);
				txtResolution.setVisibility(View.VISIBLE);
			}
		}
	};
*/
	private void Handle_IOCTRL_Cmd(int sid, int avIndex, byte[] buf, int type, int size) {
		Log.d(TAG, "Handle_IOCTRL_Cmd:");
		ByteArrayInputStream in = null;
		switch (type) {
			case AVIOCTRLDEFs.IOTYPE_USER_IPCAM_DEVINFO_RESP: {
				Log.d(TAG, "buf:" + new String(buf, 0, size));
				break;
			}
		}  // end of switch
	}

	// Video Constants
	private final static String MIME_TYPE = "video/avc"; // H.264 Advanced Video

	private final static int TIME_INTERNAL = 30;
	MediaCodec mCodec = null;

	public void initDecoder(String mime, int width, int height) throws IOException {
		Log.v(TAG, "initDecoder");
		mCodec = MediaCodec.createDecoderByType(mime);
		MediaFormat mediaFormat = MediaFormat.createVideoFormat(mime,
				width, height);

		mCodec.configure(mediaFormat, mSurfaceView.getHolder().getSurface(),
				null, 0);
		mCodec.start();

	}

	ByteBuffer[] inputBuffers;
	int inputBufferIndex;
	MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();

	public int onFrame(AVFrame avFrame) {
		if (mCodec == null) {
			return -1;
		}
		try {
			inputBufferIndex = mCodec.dequeueInputBuffer(100/*-1*/);
			if (inputBufferIndex >= 0) {
				inputBuffers = mCodec.getInputBuffers();
				ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
				inputBuffer.clear();
				inputBuffer.put(avFrame.frmData, 0, avFrame.getFrmSize());
				try {
					mCodec.queueInputBuffer(inputBufferIndex, 0, avFrame.getFrmSize(), avFrame.getTimeStamp()
							* TIME_INTERNAL, 0);
				}catch (Exception e){
					Log.d(TAG,"queueInputBuffer err");
					//e.printStackTrace();
				}

			} else {
				Log.v(TAG, "inputBufferIndex "+inputBufferIndex);
				try {
					int outputBufferIndex = mCodec.dequeueOutputBuffer(videoBufferInfo, 0);
					Log.v(TAG, "outputBufferIndex "+outputBufferIndex);
					while (outputBufferIndex >= 0) {
						/**true or false?*/
						mCodec.releaseOutputBuffer(outputBufferIndex, true);
						outputBufferIndex = mCodec.dequeueOutputBuffer(videoBufferInfo, 0);
					}
				} catch (Exception e) {
					Log.v(TAG, "recycle output error ");
					e.printStackTrace();
				}
				return 0;
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				mCodec.stop();
				mCodec.release();

			}catch (Exception ee){
				ee.printStackTrace();
			}
			mCodec = null;
			Log.d(TAG, "ResetCodec");
			Toast.makeText(CameraActivity.this, "ResetCodec", Toast.LENGTH_SHORT).show();
			return -1;
		}
		try {
			int outputBufferIndex = mCodec.dequeueOutputBuffer(videoBufferInfo, 100);
			while (outputBufferIndex >= 0) {
				mCodec.releaseOutputBuffer(outputBufferIndex, true);
				outputBufferIndex = mCodec.dequeueOutputBuffer(videoBufferInfo, 0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//Log.i("Video", "onFrame end");
		return 1;
	}

	MediaCodec mAudioCodec = null;
	MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
	static byte[] chunkPCM = new byte[4096];

	public boolean onAudioFrame(AVFrame avFrame) {
		if (mAudioCodec == null) {
			return false;
		}
		try {
			inputBufferIndex = mAudioCodec.dequeueInputBuffer(-1);
			if (inputBufferIndex >= 0) {
				inputBuffers = mAudioCodec.getInputBuffers();
				ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
				inputBuffer.clear();

				inputBuffer.put(avFrame.frmData, 7, avFrame.getFrmSize() - 7);
				mAudioCodec.queueInputBuffer(inputBufferIndex, 7, avFrame.getFrmSize() - 7, 0, 0);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		int outputBufferIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, 10000);
		ByteBuffer outputBuffer;
		if (outputBufferIndex >= 0) {
			decodeOutputBuffers = mAudioCodec.getOutputBuffers();
			outputBuffer = decodeOutputBuffers[outputBufferIndex];//拿到用于存放PCM数据的Buffer
			if (chunkPCM == null || chunkPCM.length < bufferInfo.size) {
				chunkPCM = new byte[bufferInfo.size];//BufferInfo内定义了此数据块的大小
			}
			outputBuffer.get(chunkPCM);//将Buffer内的数据取出到字节数组中
			outputBuffer.clear();//数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buff

			//playAudio(chunkPCM);
			receivePCM(chunkPCM);
            /*
            try {

                OutputStream outputStream=new FileOutputStream(pcmfile,true);
                BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(outputStream);
                bufferedOutputStream.write(chunkPCM);
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
*/
			//Log.d(TAG,"mAudioCodec.releaseOutputBuffer(outputBufferIndex, false);");
			mAudioCodec.releaseOutputBuffer(outputBufferIndex, false);
			//Log.d(TAG,"mAudioCodec.releaseOutputBuffer(outputBufferIndex, false);ddd");
			//outputBufferIndex = mAudioCodec.dequeueOutputBuffer(bufferInfo, 10000);
		} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
			Log.e(TAG, "OutputBuffer INFO_OUTPUT_BUFFERS_CHANGED");
			decodeOutputBuffers = mAudioCodec.getOutputBuffers();
		} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
			Log.e(TAG, "OutputBuffer INFO_OUTPUT_FORMAT_CHANGED");
			// Subsequent data will conform to new format.
			MediaFormat format = mAudioCodec.getOutputFormat();
		} else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
			Log.e(TAG, "dequeueOutputBuffer timed out!");
		}
// All decoded frames have been rendered, we can stop playing now
		if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
			Log.e(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
			//break;
		}

		//Log.i("onAudioFrame", "onFrame end");
		return true;
	}

	File pcmfile;
	AudioTrack mAudioTrack;
	static final int AUDIO_BUF_SIZE = 1024;
	static final int FRAME_INFO_SIZE = 24;
	static byte[] frameInfo = new byte[FRAME_INFO_SIZE];
	static byte[] audioBuffer = new byte[AUDIO_BUF_SIZE];

	public class AudioThread implements Runnable {
		final String TAG = "AudioThread";
		private int avIndex;

		public AudioThread(int avIndex) {
			this.avIndex = avIndex;
		}

		@Override
		public void run() {
			boolean isEOF = false;
			System.out.printf("[%s] Start\n",
					Thread.currentThread().getName());

            /*pcmfile = new File("/sdcard/pcm.raw");
            if (pcmfile.exists()) {
                pcmfile.delete();
            }
            try {
                pcmfile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }*/

			AVAPIs av = new AVAPIs();
			int ret = -1;
			while (mPlayAudio) {
				ret = av.avCheckAudioBuf(avIndex);
				//Log.d(TAG,"audio buff="+ret);
				if (ret < 0) {
					// Same error codes as below
					System.out.printf("[%s] avCheckAudioBuf() failed: %d\n",
							Thread.currentThread().getName(), ret);
					isEOF = true;
					//break;
				} else if (ret < 3) {
					try {
						Thread.sleep(120);
						continue;
					} catch (InterruptedException e) {
						System.out.println(e.getMessage());
						isEOF = true;
						//break;
					}
				}

				int[] frameNumber = new int[1];
				ret = av.avRecvAudioData(avIndex, audioBuffer,
						AUDIO_BUF_SIZE, frameInfo, FRAME_INFO_SIZE,
						frameNumber);

				if (ret == AVAPIs.AV_ER_SESSION_CLOSE_BY_REMOTE) {
					System.out.printf("[%s] AV_ER_SESSION_CLOSE_BY_REMOTE\n",
							Thread.currentThread().getName());
					isEOF = true;
					//break;
				} else if (ret == AVAPIs.AV_ER_REMOTE_TIMEOUT_DISCONNECT) {
					System.out.printf("[%s] AV_ER_REMOTE_TIMEOUT_DISCONNECT\n",
							Thread.currentThread().getName());
					isEOF = true;
					//break;
				} else if (ret == AVAPIs.AV_ER_INVALID_SID) {
					System.out.printf("[%s] Session cant be used anymore\n",
							Thread.currentThread().getName());
					isEOF = true;
					//break;
				} else if (ret == AVAPIs.AV_ER_LOSED_THIS_FRAME) {
					//System.out.printf("[%s] Audio frame losed\n",
					//        Thread.currentThread().getName());
					continue;
				} else if (ret < 0) {
					Log.d(TAG, "ret========" + ret);
					continue;
				}
				if (ret > 0) {
					recRate = recRate + ret;
				}
				if (mDisplay) {
					AVFrame avFrame = new AVFrame(frameNumber[0], (byte) 0, frameInfo, audioBuffer, ret);
					if (mAudioCodec == null) {
						initAACMediaEncode();
					}
					if (mAudioTrack == null) {
						initAudioTrack();
					}

					//save audio to file
					if(ret > 0){
						pakingAudio(avFrame);
					}

					try {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
							synchronized (dataQueue) {
								byte[] bytes = Arrays.copyOf(audioBuffer, ret);
								dataQueue.add(bytes);
								if (dataQueue.size() > 40) {
									dataQueue.clear();
								}
								Log.d(TAG, String.format("byte=  %d  dataQueue= %d", ret, dataQueue.size()));
							}
						} else {
							onAudioFrame(avFrame);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			try {
				mAudioCodec.stop();
				mAudioCodec.release();
				mAudioCodec = null;
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (mAudioTrack != null) {
				try{
					mAudioTrack.stop();
					mAudioTrack.release();
				}catch (Exception e){
					e.printStackTrace();
				}


				mAudioTrack = null;
			}
			System.out.printf("[%s] Exit ret[%d]\n",
					Thread.currentThread().getName(),ret);

			ret = av.avSendIOCtrl(avIndex, AVIOCTRLDEFs.IOTYPE_USER_IPCAM_AUDIOSTOP, new byte[8], 8);
			Log.d(TAG, "IOTYPE_USER_IPCAM_AUDIOSTOP==" + ret);
			av.avClientCleanAudioBuf(avIndex);

		}
	}


	/**
	 * 初始化AAC编码器
	 */
	private void initAACMediaEncode() {
		try {
			MediaFormat audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, 2);//参数对应-> mime type、采样率、声道数
			ByteBuffer wrap = null;
			if (SAMPLE_RATE == 44100) {
				wrap = ByteBuffer.wrap(new byte[]{0x12, 0x10});
			} else if (SAMPLE_RATE == 16000) {
				wrap = ByteBuffer.wrap(new byte[]{0x14, 0x10});
			}
			audioFormat.setByteBuffer("csd-0", wrap);
			audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64000);//比特率
			audioFormat.setInteger(MediaFormat.KEY_IS_ADTS, 0);
			audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
			audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);//作用于inputBuffer的大小
			mAudioCodec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				mAudioCodec.setCallback(new MediaCodec.Callback() {
					public String TAG = "MediaCodec.Callback";

					@TargetApi(Build.VERSION_CODES.LOLLIPOP)
					@Override
					public void onInputBufferAvailable(MediaCodec mediaCodec, int i) {
						//Log.e(TAG,"onInputBufferAvailable");
						synchronized (dataQueue) {
							try {
								byte[] poll = null;//

								try {
									poll = dataQueue.poll();
								} catch (Exception e) {
									e.printStackTrace();
								}
								if (poll != null) {
									Log.e(TAG, "mediaCodec.queueInputBuffer poll.length=" + poll.length);
									ByteBuffer inputBuffer = mediaCodec.getInputBuffer(i);
									inputBuffer.clear();
									inputBuffer.put(poll, 7, poll.length - 7);
									mediaCodec.queueInputBuffer(i, 7, poll.length - 7, 0, 0);
									Log.e(TAG, "mediaCodec.queueInputBuffer(i,7,poll.length-7,0,0);");
								} else {
									mediaCodec.queueInputBuffer(i, 0, 0, 0, 0);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}

					@TargetApi(Build.VERSION_CODES.LOLLIPOP)
					@Override
					public void onOutputBufferAvailable(MediaCodec mediaCodec, int i, MediaCodec.BufferInfo bufferInfo) {
						Log.e(TAG, "onOutputBufferAvailable");
						try {
							ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(i);
							MediaFormat bufferFormat = mediaCodec.getOutputFormat(i); // option A
							outputBuffer.get(chunkPCM);
							outputBuffer.clear();
							//playAudio(chunkPCM);
							receivePCM(chunkPCM);
							mediaCodec.releaseOutputBuffer(i, false);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onError(MediaCodec mediaCodec, MediaCodec.CodecException e) {
						Log.e(TAG, "onError");
					}

					@Override
					public void onOutputFormatChanged(MediaCodec mediaCodec, MediaFormat mediaFormat) {
						Log.e(TAG, "onOutputFormatChanged");
					}
				});
			}
			mAudioCodec.configure(audioFormat, null, null, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (mAudioCodec == null) {
			Log.e(TAG, "create mediaEncode failed");
			return;
		}
		mAudioCodec.start();
	}
	float mPosX = 0;
	float mPosY = 0;
	float mCurPosX = 0;
	float mCurPosY = 0;
	float x1 = 0;
	float x2 = 0;
	float y1 = 0;
	float y2 = 0;
	boolean doubleFlag = false;
	double nLenStart = 0;
	float  zoomMin = 0;
	float  zoomMax =(float) 0.8;
	int moveHorStep = 0,moveVerStep = 0;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//继承了Activity的onTouchEvent方法，直接监听点击事件
		switch (event.getAction()) {

			case MotionEvent.ACTION_DOWN:
				mPosX = event.getX();
				mPosY = event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				mCurPosX = event.getX();
				mCurPosY = event.getY();
				if(mPosY - mCurPosY > 10) {
					sendPTZ(AVIOCTRL_PTZ_UP);
					Log.i("wz","向上滑");
//					Toast.makeText(CameraActivity.this, "向上滑", Toast.LENGTH_SHORT).show();
				} else if(mCurPosY - mPosY > 10) {
					sendPTZ(AVIOCTRL_PTZ_DOWN);
					Log.i("wz","向下滑");
	//				Toast.makeText(CameraActivity.this, "向下滑", Toast.LENGTH_SHORT).show();
				} else if(mPosX - mCurPosX > 10) {
					sendPTZ(AVIOCTRL_PTZ_LEFT);
					Log.i("wz","向左滑");
	//				Toast.makeText(CameraActivity.this, "向左滑", Toast.LENGTH_SHORT).show();
				} else if(mCurPosX - mPosX > 10) {
					sendPTZ(AVIOCTRL_PTZ_RIGHT);
					Log.i("wz","向右滑");
	//				Toast.makeText(CameraActivity.this, "向右滑", Toast.LENGTH_SHORT).show();
				}
				mPosX = mCurPosY;
				mPosY = mCurPosY;
				break;
			case MotionEvent.ACTION_UP:
				//当手指离开的时候
				mCurPosX = event.getX();
				mCurPosY = event.getY();


				break;
		}


		//touch hide resolutionpop
		if(mResolutionPop!=null){
			mResolutionPop.hidePop();
		}
		int nCnt = event.getPointerCount();
		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			x1 = event.getX();
			y1 = event.getY();
			doubleFlag = false;
		}
		//------------zoom start -----------------------------------
		if( (event.getAction()&MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN && 2 == nCnt)
		{
			doubleFlag=true;
			for(int i=0; i< nCnt; i++) {
				float x = event.getX(i);
				float y = event.getY(i);
				Point pt = new Point((int)x, (int)y);
			}

			int xlen = Math.abs((int)event.getX(0) - (int)event.getX(1));
			int ylen = Math.abs((int)event.getY(0) - (int)event.getY(1));

			nLenStart = Math.sqrt((double)xlen*xlen + (double)ylen * ylen);
		}else if( (event.getAction()&MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_UP  && 2 == nCnt){
			for(int i=0; i< nCnt; i++){
				float x = event.getX(i);
				float y = event.getY(i);
				Point pt = new Point((int)x, (int)y);
			}
			int xlen = Math.abs((int)event.getX(0) - (int)event.getX(1));
			int ylen = Math.abs((int)event.getY(0) - (int)event.getY(1));

			double nLenEnd = Math.sqrt((double)xlen*xlen + (double)ylen * ylen);

			if(nLenEnd > nLenStart){
				zoomMin = zoomMin+(float)0.2;
				if(zoomMin>=zoomMax){
					zoomMin = zoomMax;
				}
				scaleVedioSurface((float) zoomMin,0,0);
			}else{
				zoomMin = zoomMin-(float)0.2;
				if(zoomMin<=0){
					zoomMin = 0;
				}
				scaleVedioSurface((float)zoomMin,0,0);
			}
		}
		//------------zoom end -----------------------------------


		//------------touch control start-----------------------------------
		if(event.getAction() == MotionEvent.ACTION_UP && !doubleFlag && zoomMin==0) {
			x2 = event.getX();
			y2 = event.getY();
			if(y1 - y2 > 50) {
				if(x2 - x1 > 50){
					//turn left down
					sendPTZ(AVIOCTRL_PTZ_LEFT_DOWN);
				}else if(x1 - x2 > 50){
					//turn right down
					sendPTZ(AVIOCTRL_PTZ_RIGHT_DOWN);
				}else{
					//turn down
					sendPTZ(AVIOCTRL_PTZ_DOWN);
				}
			} else if(y2 - y1 > 50) {
				if(x2 - x1 > 50){
					//turn left up
					sendPTZ(AVIOCTRL_PTZ_LEFT_UP);
				}else if(x1 - x2 > 50){
					//turn right up
					sendPTZ(AVIOCTRL_PTZ_RIGHT_UP);
				}else{
					//turn up
					sendPTZ(AVIOCTRL_PTZ_UP);
				}
			} else if(x1 - x2 > 50) {
				//turn right
				sendPTZ(AVIOCTRL_PTZ_RIGHT);
			} else if(x2 - x1 > 50) {
				//turn left
				sendPTZ(AVIOCTRL_PTZ_LEFT);
			}

//			if(x1==x2 && y1==y2){
//				hideAndShowControlView();
//			}
		}

		/**
		 * Move the view after zoom
		 */
		if(event.getAction() == MotionEvent.ACTION_UP && zoomMin>0 && !doubleFlag) {
			x2 = event.getX();
			y2 = event.getY();
			if(x1 - x2 > 50 && rightMargin<0) {
				moveHorStep = moveHorStep +(int)Math.abs(x1-x2)/2;
			} else if(x2 - x1 > 50 && leftMargin <0) {
				moveHorStep = moveHorStep - (int)Math.abs(x1-x2)/2;
			} else if(y1-y2>50 && bottomMargin < 0){
				moveVerStep = moveVerStep + (int)Math.abs(y1-y2)/2;
			}else if(y2-y1>50 && topMargin<0){
				moveVerStep = moveVerStep - (int)Math.abs(y1-y2)/2;
			}
			scaleVedioSurface((float)zoomMin,moveHorStep,moveVerStep);
		}
		return super.onTouchEvent(event);
	}

	void receivePCM(byte[] pcm) {
		byte destData[] = new byte[pcm.length];
		int audio_size = Audio.audio_rx(pcm, pcm.length, destData, destData.length);
		if (audio_size > 0) {
			synchronized (mAudioTrack) {
				if (mAudioTrack != null) {
					mAudioTrack.write(destData, 0, audio_size);
				}
			}
		}
	}

	class  ProcessThread extends Thread{
		boolean cancle;

		public ProcessThread(Runnable target) {
			super(target);
		}

		public boolean isCancle() {
			return cancle;
		}

		public void setCancle(boolean cancle) {
			this.cancle = cancle;
		}
	}

}
