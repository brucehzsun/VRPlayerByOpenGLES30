
package bruce.sun.vr.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.baofeng.mojing.MojingSDK;
import com.baofeng.mojing.MojingSurfaceView;
import com.baofeng.mojing.MojingVrLib;
import com.baofeng.mojing.input.base.MojingKeyCode;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

import bruce.sun.vr.R;
import bruce.sun.vr.db.VrPreferences;
import bruce.sun.vr.mojing.ManufacturerList;
import bruce.sun.vr.render.BaseRenderer;
import bruce.sun.vr.render.MyRenderer;
import bruce.sun.vr.render.RendererListener;
import bruce.sun.vr.render.VrRender;


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class VRActivity extends Activity implements RendererListener, OnClickListener,
        SeekBar.OnSeekBarChangeListener, OnTouchListener {

    private static final String TAG = "VRActivity";

    private static final int MSG_UPDATE_SEEK_BAR = 100;

    private static final int MSG_DISMISS_CONTROLLER_BAR = 1001;

    private static final int UPDATE_VIDEO_TITLEBAR_ISSHOW = 1002;

    private static final int MSG_REQUEST_CODE = 1000;

    private static final int MSG_DISMISS_TIP_LAYER = 1004;

    private static final int SHOW_TIME = 5000;// 3000;

    private static final String PARAM_TITLE = "title";

    private static final String PARAM_URL = "url";

    private static final String PARAM_FROM = "from";

    private static final String PARAM_LIVE = "live";

    private static final String PARAM_MOJING_INFO = "mojing_info";

    private String curCachingP2PPath;

    private static final int ID_MY = 0;

    private static final int ID_MOJING = 1;

    GLMsgHandler handler;

    MojingSurfaceView mojingSurfaceView;

    GLSurfaceView mySurfaceView;

    SurfaceView curSurfaceView;

    boolean isMyInited = false;

    boolean isMojingInited = false;

    private ManufacturerList m_ManufacturerList;

    ViewFlipper flipper;

    VrRender mojingRenderer;

    MyRenderer myRenderer;

    BaseRenderer curRenderer;

    SparseArray<String> keyStateMap = new SparseArray<String>();

    SparseArray<String> axisStateMap = new SparseArray<String>();

    private float touchPrevX;

    private float touchPrevY;

    private float touchDeltaX;

    private float touchDeltaY;

    private MediaPlayer mMediaPlayer;

    private boolean isSeeking;

    private boolean isPlayerPrepared;

    private boolean isRendererReady;

    private static final float sDensity = Resources.getSystem().getDisplayMetrics().density;

    private static final float sDamping = 0.2f;

    boolean isVideoSizeChanged = false;

    private View mProgressBarLayout;

    private ImageView mResetBtn;

    private ImageView mGyroModeBtn;

    private ImageView mGlassesModeBtn;

    private SeekBar mSeekBar;

    private boolean isTouchMode;

    private boolean isGyroMode;

    private boolean isGlassesMode;

    private ImageView mPauseBtn;

    private TextView mTitle;

    private View mTopControlView;

    private View mBottomControlView;

    private TextView mCurrTime;

    private TextView mTotalTime;

    private View mShowTipRoot;

    private View mBackBtn;

    private TextView mShowTipText;

    private ImageView mShowTipImg;

    private boolean isOnResume;

    private boolean isMojingSDKInited;

    int touchEventMode;

    private static final int TOUCH_EVENT_MODE_NORMAL = 1;

    private static final int TOUCH_EVENT_MODE_ZOOM = 2;

    private float zoomStartDistance;

    private boolean isRendererInited;

    private boolean isGetVideoUrlSuccess;

    private String videoUrl;

    private PlayerProgressCtrl playerProgressCtrl;

    private int playStartPosition; // 播放记忆点

    private String mBaseUrl;

    private String mVideoFrom;

    private String mFullVideoTitle;

    private boolean isLive;

    private boolean isShowMojingBtn = true;

    private int taskId;

    private final int liveCacheSize = 20 * 1024 * 1024;

    private int MIN_MOVE_SPAN; // 区分滑屏和点击的距离阈值

    private boolean isSemiSphere = false; // 2016-03-22
    // 是否180度全景视频(画面只有360全景视频的前半边,后半边为黑色背景)

    private View.OnTouchListener blockTouchToGlSurface = new View.OnTouchListener() {
        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (v.getId()) {
                case R.id.videoPlayer_controlBar_top_layout:
                case R.id.videoPlayer_ctrlbar_bottom_layout:
                    // 当用户本身已经在操作控制层时，控制层的隐藏时间应该重新计算
                    showContorlView();
                    break;
            }
            // 禁止控制层的触屏消息透传到GlSurfaceView引起画面旋转
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_vr);
        Log.d(TAG, "onCreate");
        BaseRenderer.reset();
        MIN_MOVE_SPAN = Dp2Px(this, 20);
        initView();
        initData();
    }

    private void initData() {
        mBaseUrl = "http://192.168.90.43/minfo.php?aid=9390299&seq=1&package=com.storm.smart&platf=android&mtype=normal&g=23&ver=6.0.01&td=0&s=F98478740A13589B0BB436500EDD927E5188E499";
        getIntentData();
        if (isShowMojingBtn) {
            mGlassesModeBtn.setVisibility(View.VISIBLE);
        }
        mTitle.setText(mFullVideoTitle);
        // 测试关联播放
        boolean test_assoc = true;
        isTouchMode = true;
        if (test_assoc) {
            Uri uri = getIntent().getData();
            // 关联播放只处理绝对路径的视频文件
            if (uri != null && "file".equals(uri.getScheme()) && uri.isAbsolute()) {
                isGetVideoUrlSuccess = true;
                videoUrl = uri.toString();
                return;
            }
        }
        if (!isLive) {
//            videoUrl = "http://127.0.0.1:" + P2P.P2P_LIVE_PLAY_PORT;
//            GetP2pStringTask task = new GetP2pStringTask(this, mBaseUrl, this);
//            task.execute();
        } else {
//            videoUrl = "http://127.0.0.1:" + P2P.P2P_LIVE_PLAY_PORT + "/live.m3u8";
//            p2pManager = P2P.getInstance(this);
//            p2pManager.init(getApplicationContext(),
//                    P2pUtils.getP2PDownloadInitPath(getApplicationContext()),
//                    P2pUtils.getP2pDownloadPath(getApplicationContext()));
//            startP2PLive(mBaseUrl);
            isGetVideoUrlSuccess = true;
        }
    }

    private boolean isP2PLive() {
        return isLive;
    }


    private void getIntentData() {
        Intent intent = getIntent();
        if (intent == null || intent.getExtras() == null) {
            return;
        }
        Bundle bundle = intent.getExtras();
        mBaseUrl = bundle.getString(PARAM_URL);
        mVideoFrom = bundle.getString(PARAM_FROM);
        mFullVideoTitle = bundle.getString(PARAM_TITLE);
        isLive = bundle.getBoolean(PARAM_LIVE, false);
        String[] mojingInfo = bundle.getStringArray(PARAM_MOJING_INFO);
        if (mojingInfo != null && mojingInfo.length != 0) {
            if (mojingInfo.length > 0) {
                final String SYN_SHOW = "1";
                String switchInfo = TextUtils.isEmpty(mojingInfo[0]) ? SYN_SHOW : mojingInfo[0];
                isShowMojingBtn = SYN_SHOW.equals(switchInfo) ? true : false;
            }
            if (mojingInfo.length > 1) {
//                mMojingBuyUrl = TextUtils.isEmpty(mojingInfo[1]) ? UrlHelper.MOJING_BUY_URL
//                        : mojingInfo[1];
            }
        }
    }

    private void initView() {
        handler = new GLMsgHandler(this);
        mBackBtn = findViewById(R.id.videoPlayer_ctrlbar_btn_back);
        mShowTipRoot = findViewById(R.id.video_player_tip_root);
        mShowTipImg = (ImageView) findViewById(R.id.video_player_dialog_tip_img);
        mShowTipText = (TextView) findViewById(R.id.video_player_dialog_tip_text);
        mCurrTime = (TextView) findViewById(R.id.videoPlayer_ctlbar_text_curtime);
        mTotalTime = (TextView) findViewById(R.id.videoPlayer_ctrlbar_text_duration);
        mTopControlView = findViewById(R.id.videoPlayer_controlBar_top_layout);
        mBottomControlView = findViewById(R.id.videoPlayer_ctrlbar_bottom_layout);
        mTitle = (TextView) findViewById(R.id.movie_ctrlbar_text_name);
        mPauseBtn = (ImageView) findViewById(R.id.videoPlayer_ctrlbar_btn_playpause);
        mResetBtn = (ImageView) findViewById(R.id.videoPlayer_ctrlbar_btn_reset);
        mGyroModeBtn = (ImageView) findViewById(R.id.videoPlayer_ctrlbar_btn_gyro);
        mGlassesModeBtn = (ImageView) findViewById(R.id.videoPlayer_ctrlbar_btn_glasses);
        mSeekBar = (SeekBar) findViewById(R.id.videoPlayer_ctrlbar_seekbar);
        mProgressBarLayout = findViewById(R.id.videoPlayer_seek_loadingLayout);
        mTitle = (TextView) findViewById(R.id.movie_ctrlbar_text_name);

        mBackBtn.setOnClickListener(this);
        mPauseBtn.setOnClickListener(this);
        mSeekBar.setOnSeekBarChangeListener(this);
        mResetBtn.setOnClickListener(this);
        mGyroModeBtn.setOnClickListener(this);
        mGlassesModeBtn.setOnClickListener(this);

        mBottomControlView.setOnTouchListener(blockTouchToGlSurface);
        mTopControlView.setOnTouchListener(blockTouchToGlSurface);
        playerProgressCtrl = new PlayerProgressCtrl(mProgressBarLayout, null, handler);
        playerProgressCtrl.enableTraceMode(false);

        flipper = (ViewFlipper) findViewById(R.id.flipper);
        mojingSurfaceView = (MojingSurfaceView) flipper.getChildAt(ID_MOJING);
        mySurfaceView = (GLSurfaceView) flipper.getChildAt(ID_MY);
        mySurfaceView.setEGLContextClientVersion(2);
        mojingSurfaceView.setOnTouchListener(this);
        mySurfaceView.setOnTouchListener(this);

        MojingSDK.Init(this);
        selectSurfaceView(ID_MY, true);
    }

    private void changeCtrlBarStatus() {
        if (mTopControlView.getVisibility() == View.VISIBLE) {
            hideContorlView();
        } else {
            showContorlView();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    @Override
    protected void onResume() {
        super.onResume();
        isOnResume = true;
        Log.d(TAG, "onResume");
        startGyroTracking();
        if (isMojing()) {
            onResumeMojing();
        } else {
            onResumeMy();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isOnResume = false;
        Log.d(TAG, "onPause");
        stopGyroTracking();
        if (isMojing()) {
            onPauseMojing();
        } else {
            onPauseMy();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    public void onChangeMojingWorld() {
        float fov = com.baofeng.mojing.MojingSDK.GetMojingWorldFOV() / 2.f;
        float ratio = (float) Math.tan(Math.toRadians(fov));
        mojingRenderer.getMatrixState().setProjectFrustum(-ratio, ratio, -ratio, ratio, 1.f, 800);
    }

    private boolean isMojing() {
        int curId = flipper.getDisplayedChild();
        if (curId == ID_MOJING) {
            return true;
        } else {
            return false;
        }
    }

    private SurfaceView getSurfaceView() {
        if (isMojing()) {
            return mojingSurfaceView;
        } else {
            return mySurfaceView;
        }
    }

    private BaseRenderer getRenderer() {
        if (isMojing()) {
            return mojingRenderer;
        } else {
            return myRenderer;
        }
    }

    private void initMojingSDK() {
        if (isMojingSDKInited) {
            return;
        }
        MojingSDK.Init(this);
        isMojingSDKInited = true;
    }

    private void selectSurfaceView(int id, boolean isOnCreate) {
        if (!isOnCreate && flipper.getDisplayedChild() == id) {
            return;
        }
        isRendererInited = false;
        isRendererReady = false;
        flipper.setDisplayedChild(id);
        if (isMojing()) {
            if (!isOnCreate) {
                myRenderer.onPause();
                mySurfaceView.onPause();
            }
            if (!isMojingInited) {
                initMojingSDK();
                mojingRenderer = new VrRender(this, handler);
                mojingRenderer.setListener(this);
                mojingSurfaceView.setRenderer(mojingRenderer);
                mojingSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                initKeyValues();
                // MUST INIT glass list first
                m_ManufacturerList = ManufacturerList.getInstance("zh");
                // Setup default glass mode
                mojingSurfaceView
                        .setGlassesKey(m_ManufacturerList.mManufaturerList.get(0).mProductList
                                .get(0).mGlassList.get(0).mKey);
                mojingSurfaceView.setMessageHandler(handler);
                String support = VrPreferences.getInstance(this).getSupportTimeWarpAndMultiThread();
                if (TextUtils.isEmpty(support) || support.equals("yes")) {
                    mojingSurfaceView.setTimeWarp(true);
                    mojingSurfaceView.setMultiThread(true);
                    if (TextUtils.isEmpty(support)) { // 未检测过，开启检测
                        mojingRenderer.setDetectTimeWarpAndMultiThread(true);
                    }
                }
                if (isOnCreate) {
                    startGyro();
                }
                isMojingInited = true;
            }
            if (!isOnCreate) {
                mojingRenderer.onResume();
                mojingSurfaceView.onResume();
            }
        } else {
            if (!isOnCreate) {
                mojingRenderer.onPause();
                mojingSurfaceView.onPause();
            }
            if (!isMyInited) {
                myRenderer = new MyRenderer(this, handler);
                myRenderer.setListener(this);
                mySurfaceView.setRenderer(myRenderer);
                myRenderer.setGlSurfaceView(mySurfaceView, GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                isMyInited = true;
            }
            if (!isOnCreate) {
                myRenderer.onResume();
                mySurfaceView.onResume();
            }
        }
        curSurfaceView = getSurfaceView();
        curRenderer = getRenderer();
        curSurfaceView.requestFocus();
        curRenderer.setSemiSphere(isSemiSphere);
    }

    private void initKeyValues() {
        try {
            Field fields[] = MojingKeyCode.class.getFields();
            for (int i = 0; i < fields.length; i++) {
                String fieldName = fields[i].getName();
                if (fieldName.startsWith("KEYCODE")) {
                    keyStateMap.put(fields[i].getInt(null), fieldName);
                } else if (fieldName.startsWith("AXIS")) {
                    axisStateMap.put(fields[i].getInt(null), fieldName);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    private void onPauseMojing() {
        if (!isMojingInited) {
            return;
        }
        MojingVrLib.stopVsync(this);
        checkPause();
    }

    private void onPauseMy() {
        if (!isMyInited) {
            return;
        }
        checkPause();
    }

    private void onResumeMojing() {
        if (!isMojingInited) {
            return;
        }
        MojingVrLib.startVsync(this);
        checkStart();
    }

    private void onResumeMy() {
        if (!isMyInited) {
            return;
        }
        checkStart();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.videoPlayer_ctrlbar_btn_reset:
                resetTouchPosition();
                getRenderer().resetScale();
                break;
            case R.id.videoPlayer_ctrlbar_btn_gyro:
                if (!isGyroMode) {
                    startGyroMode();
                } else {
                    startTouchMode();
                }
                break;

            case R.id.videoPlayer_ctrlbar_btn_glasses:
                if (!isGlassesMode) {
                    mResetBtn.setVisibility(View.GONE);
                    showGlassModeDialog();
//                    FullVideoStatisticUtils.mojingCount(this,
//                            FullVideoStatisticUtils.MOJING_DISPLAY_CLICK,
//                            FullVideoStatisticUtils.STATUS_CLICK);
                } else {
                    startTouchMode();
                }
                break;
            case R.id.videoPlayer_ctrlbar_btn_playpause:
                pauseOrPlay();
                break;
            case R.id.videoPlayer_ctrlbar_btn_back:
                onBackEvent();
                break;
        }
    }

    /**
     * 开启touch模式
     */
    private void startTouchMode() {
        if (!isTouchMode) {
            resetModeFlag();
            isTouchMode = true;
            mResetBtn.setVisibility(View.VISIBLE);
            myRenderer.setGlSurfaceView(mySurfaceView, GLSurfaceView.RENDERMODE_WHEN_DIRTY);
            if (isMojing()) {
                onPauseMojing();
                selectSurfaceView(ID_MY, false);
                onResumeMy();
            }
            stopGyro();
        }
    }

    /**
     * 开启Gyro模式
     */
    private void startGyroMode() {
        if (VrPreferences.getInstance(this).isFirstGoryMode()) {
            showGoryTips();
            auToHideTip();
            VrPreferences.getInstance(this).setFirstGoryMode(false);
        }
        mResetBtn.setVisibility(View.VISIBLE);
        resetModeFlag();
        mGyroModeBtn.setImageResource(R.drawable.movie_ctrlbar_btn_tly_down_selector);
        isGyroMode = true;
        myRenderer.setGlSurfaceView(mySurfaceView, GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        if (isMojing()) {
            onPauseMojing();
            selectSurfaceView(ID_MY, false);
            onResumeMy();
        }
        startGyro();
    }

    private void startGlassesMode() {
        resetModeFlag();
        mGlassesModeBtn.setImageResource(R.drawable.movie_ctrlbar_btn_mj_down_selector);
        isGlassesMode = true;
        onPauseMy();
        selectSurfaceView(ID_MOJING, false);
        onResumeMojing();
        startGyro();
    }

    private void resetModeFlag() {
        isTouchMode = false;
        isGyroMode = false;
        isGlassesMode = false;
        mGyroModeBtn.setImageResource(R.drawable.movie_ctrlbar_btn_tly_selector);
        mGlassesModeBtn.setImageResource(R.drawable.movie_ctrlbar_btn_mj_selector);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (MSG_REQUEST_CODE == requestCode) {
            startGlassesMode();
        }
    }

    private void startGyro() {
        if (curRenderer != null && !curRenderer.isGyroTrackEnabled()) {
            curRenderer.setGyroTrackEnabled(true);
            startGyroTracking();
            MojingSDK.ResetSensorOrientation();
        }
    }

    private void stopGyro() {
        if (curRenderer != null && curRenderer.isGyroTrackEnabled()) {
            stopGyroTracking();
            curRenderer.setGyroTrackEnabled(false);
        }
    }

    private void showTouchTips() {
        mShowTipRoot.setVisibility(View.VISIBLE);
    }

    private void showGoryTips() {
        mShowTipRoot.setVisibility(View.VISIBLE);
        mShowTipImg.setImageResource(R.drawable.fullvideo_gory_mode_img);
        mShowTipText.setText(getString(R.string.full_video_gory_str));
    }

    private void hideTips() {
        if (mShowTipRoot.getVisibility() == View.VISIBLE) {
            mShowTipRoot.setVisibility(View.GONE);
        }
    }

    /**
     * 显示3d眼睛模式对话框
     */
    private void showGlassModeDialog() {
        if (mMediaPlayer.isPlaying()) {
            pause();
        }
        GlassModeDialog customDialog1 = new GlassModeDialog(this) {
            @Override
            public void gotoBuyClick() {
//                onBuyMojingBtnClick();
//                umengCount(VRActivity.this, UmengConstants.FULL_VIDEO_GOTO_BUY);
//                FullVideoStatisticUtils.mojingCount(VRActivity.this,
//                        FullVideoStatisticUtils.MOJING_ONLY_CLICK,
//                        FullVideoStatisticUtils.STATUS_CLICK);
                this.dismiss();
            }

            @Override
            public void continuePlay() {
                this.dismiss();
                startGlassesMode();
            }
        };
        customDialog1.setCancelable(false);
        customDialog1.show();
    }

    private void pauseOrPlay() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                pause();
            } else {
                goToStart();
            }
        }
    }

    private void pause() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            Log.d(TAG, "pause() mMediaPlayer.pause();");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mPauseBtn.setImageResource(R.drawable.movie_ctrlbar_btn_play_selector);
                }
            });
            cancleAuToHide();
        }
    }

    private void goToStart() {
        if (mMediaPlayer != null && isOnResume) {
            mMediaPlayer.start();
            Log.d(TAG, "goToStart  mMediaPlayer.start();");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    mPauseBtn.setImageResource(R.drawable.movie_ctrlbar_btn_pause_selector);
                }
            });
            auToHide();
        }
    }

    private void startGyroTracking() {
        if (curRenderer != null && curRenderer.isGyroTrackEnabled()) {
            initMojingSDK();
            MojingSDK.StartTracker(100);
        }
    }

    private void stopGyroTracking() {
        if (curRenderer != null && curRenderer.isGyroTrackEnabled()) {
            initMojingSDK();
            MojingSDK.StopTracker();
        }
    }

    @Override
    public void onRendererInit() {
        isRendererInited = true;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onRendererReady() {
        isRendererReady = true;
        if (mMediaPlayer != null) {
            mMediaPlayer.setSurface(getRenderer().getSurface());
        }
        if (isReady()) {
            getRenderer().setDefaultBufferSize(mMediaPlayer.getVideoWidth(),
                    mMediaPlayer.getVideoHeight());
        }
        checkStart();
    }

    private boolean isReady() {
        if (mMediaPlayer != null && isPlayerPrepared && isVideoSizeChanged && isRendererReady) {
            return true;
        }
        return false;
    }

    private boolean checkStart() {
        if (isReady()) {
            goToStart();
            return true;
        }
        return false;
    }

    private boolean checkPause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            pause();
            return true;
        }
        return false;
    }


    protected void play() {
        // flipper发生切换时，薪的glsurface会重新创建,导致再次调用play方法
        if (mMediaPlayer != null) {
            return;
        }
        Log.d(TAG, "创建MediaPlayer");
        isVideoSizeChanged = false;
        isPlayerPrepared = false;
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setSurface(getRenderer().getSurface());
            mMediaPlayer.setDataSource(videoUrl);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    isPlayerPrepared = true;
                    getRenderer().setDefaultBufferSize(mMediaPlayer.getVideoWidth(),
                            mMediaPlayer.getVideoHeight());
                    if (checkStart()) {
                        handler.sendEmptyMessageDelayed(MSG_UPDATE_SEEK_BAR, 1000);
                    }
                    if (VrPreferences.getInstance(VRActivity.this).isFirstTouchMode()) {
                        showTouchTips();
                        auToHideTip();
                        VrPreferences.getInstance(VRActivity.this).setFirstTouchMode(false);
                    }
                    auToHide();
                    Log.d(TAG, " mMediaPlayer.start();");
                }
            });
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    Log.e(TAG, " mMediaPlayer.onError();");
                    return true;
                }
            });
            mMediaPlayer
                    .setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                        @Override
                        public void onVideoSizeChanged(MediaPlayer mediaPlayer, int i, int i1) {
                            isVideoSizeChanged = true;
                            if (checkStart()) {
                                handler.sendEmptyMessageDelayed(MSG_UPDATE_SEEK_BAR, 1000);
                            }
                        }
                    });
            mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                // buffer start和buffer end居然也可能不成对出现
                private boolean isBuffering;

                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    switch (what) {
                        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                            if (!isBuffering) {
                                isBuffering = true;
                                showProgress(true);
                            }
                            break;

                        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                            if (isBuffering) {
                                isBuffering = false;
                                showProgress(false);
                            }
                            break;

                        default:
                            break;
                    }
                    return true;
                }
            });
            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    // 没调用seekTo方法，居然也有可能收到onSeekComplete,无语中
                    if (isSeeking) {
                        isSeeking = false;
                        showProgress(false);
                    }
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    try {
                        mMediaPlayer.stop();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    mMediaPlayer.release();
                    mMediaPlayer = null;
                    finish();
                }
            });
            mMediaPlayer.prepareAsync();
            Log.d(TAG, "mMediaPlayer prepare");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateSeekBar() {
        if (curRenderer != null) {
            // mFps.setText(String.format("FPS: %02.1f", curRenderer.getFPS()));
        }

        handler.removeMessages(MSG_UPDATE_SEEK_BAR);
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            int curr = mMediaPlayer.getCurrentPosition();
            int total = mMediaPlayer.getDuration();
            mSeekBar.setProgress(curr);
            mSeekBar.setMax(total);
        }
        handler.sendEmptyMessageDelayed(MSG_UPDATE_SEEK_BAR, 1000);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mCurrTime.setText(VrUtils.getStringTime(progress));
        mTotalTime.setText(VrUtils.getStringTime(seekBar.getMax()));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        handler.removeMessages(MSG_UPDATE_SEEK_BAR);
        cancleAuToHide();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        seekTo(seekBar.getProgress());
        handler.sendEmptyMessage(MSG_UPDATE_SEEK_BAR);
        handler.sendEmptyMessage(MSG_DISMISS_CONTROLLER_BAR);
        auToHide();
    }

    private void seekTo(int msec) {
        if (mMediaPlayer == null) {
            return;
        }
        if (!isPlayerPrepared) {
            return;
        }
        isSeeking = true;
        showProgress(true);
        mMediaPlayer.seekTo(msec);
    }

    private void showContorlView() {
        cancleAuToHide();
        if (!isGlassesMode) {
            mResetBtn.setVisibility(View.VISIBLE);
        }
        mTopControlView.setVisibility(View.VISIBLE);
        mBottomControlView.setVisibility(View.VISIBLE);
        handler.sendEmptyMessage(MSG_UPDATE_SEEK_BAR);

        // if (Build.VERSION.SDK_INT < 14 ||
        // ViewConfiguration.get(this).hasPermanentMenuKey()) {
        // return;
        // }
        // getWindow().getDecorView().setSystemUiVisibility(
        // View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        // | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        // | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        auToHide();
    }

    private void hideContorlView() {
        cancleAuToHide();
        mResetBtn.setVisibility(View.GONE);
        mTopControlView.setVisibility(View.GONE);
        mBottomControlView.setVisibility(View.GONE);
        handler.removeMessages(MSG_UPDATE_SEEK_BAR);

        if (Build.VERSION.SDK_INT < 14 || ViewConfiguration.get(this).hasPermanentMenuKey()) {
            return;
        }
        // 获
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    /**
     * 3秒后自动隐藏控制层
     */
    private void auToHide() {
        handler.removeMessages(MSG_DISMISS_CONTROLLER_BAR);
        handler.sendEmptyMessageDelayed(MSG_DISMISS_CONTROLLER_BAR, SHOW_TIME);
    }

    /**
     * 3秒后自动隐藏提示
     */
    private void auToHideTip() {
        handler.removeMessages(MSG_DISMISS_TIP_LAYER);
        handler.sendEmptyMessageDelayed(MSG_DISMISS_TIP_LAYER, SHOW_TIME);
    }

    /**
     * 取消自动隐藏
     */
    private void cancleAuToHide() {
        handler.removeMessages(MSG_DISMISS_CONTROLLER_BAR);
    }

    private float getDistance(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance;
    }

    float touchStartX = 0;

    float touchStartY = 0;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                touchEventMode = TOUCH_EVENT_MODE_NORMAL;
                touchStartX = event.getX();
                touchStartY = event.getY();
                break;

            case MotionEvent.ACTION_UP:
                getRenderer().scaleEnd();
                float xd = Math.abs(event.getX() - touchStartX);
                float yd = Math.abs(event.getY() - touchStartY);
                if (xd < MIN_MOVE_SPAN && yd < MIN_MOVE_SPAN) { // 滑动距离小于阈值,鉴定为单击事件
                    if (touchEventMode == TOUCH_EVENT_MODE_NORMAL) {
                        handler.removeMessages(UPDATE_VIDEO_TITLEBAR_ISSHOW);
                        handler.sendEmptyMessageDelayed(UPDATE_VIDEO_TITLEBAR_ISSHOW, 300);
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (touchEventMode == TOUCH_EVENT_MODE_ZOOM) {
                    if (event.getPointerCount() == 1) {
                        break;
                    }
                    float zoomEndDistance = getDistance(event);
                    if (zoomEndDistance > 10f) {
                        float scale = zoomEndDistance / zoomStartDistance;
                        getRenderer().setScale(scale, true);
                    }
                } else {
                    float deltaX = x - touchPrevX;
                    float deltaY = y - touchPrevY;
                    // 为了避免滑屏导致头晕，每次只允许在一个方向上滑动，即：斜着滑屏时，取滑动距离最大的方向
                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
                        touchDeltaX += deltaX / sDensity * sDamping;
                    } else {
                        touchDeltaY += deltaY / sDensity * sDamping;
                    }
                    getRenderer().setTouchData(touchDeltaX, touchDeltaY, true);
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                touchEventMode = TOUCH_EVENT_MODE_ZOOM;
                zoomStartDistance = getDistance(event);
                break;
        }
        touchPrevX = x;
        touchPrevY = y;
        return true;
    }

    /**
     * 根据手机的分辨率从px(像素) 的单位 转成为dp
     */
    public int Dp2Px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }


    private static class GLMsgHandler extends Handler {
        private final WeakReference<VRActivity> mActivity;

        public GLMsgHandler(VRActivity activity) {
            mActivity = new WeakReference<VRActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mActivity == null) {
                return;
            }
            VRActivity activity = mActivity.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case MojingSurfaceView.ON_CHANGE_MOJING_WORLD:
                    activity.onChangeMojingWorld();
                    break;
                case MSG_UPDATE_SEEK_BAR:
                    activity.updateSeekBar();
                    break;
                case MSG_DISMISS_CONTROLLER_BAR:
                    activity.hideContorlView();
                    break;
                case UPDATE_VIDEO_TITLEBAR_ISSHOW:
                    activity.changeCtrlBarStatus();
                case MSG_DISMISS_TIP_LAYER:
                    activity.hideTips();
                    break;
            }
        }
    }

    // 归零
    private void resetTouchPosition() {
        touchDeltaX = 0;
        touchDeltaY = 0;
        touchPrevX = 0;
        touchPrevY = 0;
        touchEventMode = 0;
        getRenderer().setTouchData(0, 0, false);
    }

    @Override
    public void onSwitchModeStart() {
        // 切换模式开始
        showProgress(true);
    }

    @Override
    public void onSwitchModeEnd() {
        // 切换模式结束
        showProgress(false);
    }

    private void showProgress(boolean isShow) {
        playerProgressCtrl.showProgress(isShow);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        onBackEvent();
    }

    private void onBackEvent() {
        finish();
    }

    @Override
    public void onDetectTimeWarpAndMultiThread(boolean isSupported) {
        if (!isMojing()) {
            return;
        }
        // 本设备不支持TimeWarp和多线程反畸变
        if (isSupported) {
            VrPreferences.getInstance(this).setSupportTimeWarpAndMultiThread("yes");
            return;
        }
        VrPreferences.getInstance(this).setSupportTimeWarpAndMultiThread("no");
        flipper.setDisplayedChild(ID_MY);
        mojingSurfaceView.setTimeWarp(false);
        mojingSurfaceView.setMultiThread(false);
        flipper.setDisplayedChild(ID_MOJING);
    }
}
