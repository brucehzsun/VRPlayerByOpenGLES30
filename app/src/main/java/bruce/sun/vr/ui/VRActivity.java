
package bruce.sun.vr.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.ViewFlipper;

import bruce.sun.vr.R;
import bruce.sun.vr.render.BaseRenderer;
import bruce.sun.vr.surface.VRGLSurfaceView;
import bruce.sun.vr.utils.VrUtils;


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class VRActivity extends Activity implements OnClickListener,
        SeekBar.OnSeekBarChangeListener, OnTouchListener, IVRPlayView {

    private static final String TAG = "VRActivity";

    ViewFlipper flipper;


    private float touchPrevX;

    private float touchPrevY;


    private static final float sDensity = Resources.getSystem().getDisplayMetrics().density;

    private static final float sDamping = 0.2f;


    int touchEventMode;

    private static final int TOUCH_EVENT_MODE_NORMAL = 1;

    private static final int TOUCH_EVENT_MODE_ZOOM = 2;

    private float zoomStartDistance;

    private String videoUrl;

//    private PlayerProgressCtrl playerProgressCtrl;


    private String mFullVideoTitle;

    private boolean isLive;

    private boolean isShowMojingBtn = true;

    private int taskId;

    private final int liveCacheSize = 20 * 1024 * 1024;

    private int MIN_MOVE_SPAN; // 区分滑屏和点击的距离阈值


    private String playUrl;
    private IVRPlayPresenter presenter;
    private VRGLSurfaceView mySurfaceView;
    private int touchDeltaX;
    private int touchDeltaY;

    float touchStartX = 0;

    float touchStartY = 0;
//    private MojingSurfaceView mojingSurfaceView;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_vr);
        presenter = new VRPlayPresenter(this);


        Log.d(TAG, "onCreate");
        BaseRenderer.reset();
        MIN_MOVE_SPAN = VrUtils.dp2Px(this, 20);
        initView();
        initData();
    }

    private void initData() {
        playUrl = getIntent().getStringExtra("PlayUrl");
        presenter.doPlay(playUrl);
    }

    private boolean isP2PLive() {
        return isLive;
    }

    private void initView() {
//        mBackBtn = findViewById(R.id.videoPlayer_ctrlbar_btn_back);
//        mShowTipRoot = findViewById(R.id.video_player_tip_root);
//        mShowTipImg = (ImageView) findViewById(R.id.video_player_dialog_tip_img);
//        mShowTipText = (TextView) findViewById(R.id.video_player_dialog_tip_text);
//        mCurrTime = (TextView) findViewById(R.id.videoPlayer_ctlbar_text_curtime);
//        mTotalTime = (TextView) findViewById(R.id.videoPlayer_ctrlbar_text_duration);
//        mTopControlView = findViewById(R.id.videoPlayer_controlBar_top_layout);
//        mBottomControlView = findViewById(R.id.videoPlayer_ctrlbar_bottom_layout);
//        mTitle = (TextView) findViewById(R.id.movie_ctrlbar_text_name);
//        mPauseBtn = (ImageView) findViewById(R.id.videoPlayer_ctrlbar_btn_playpause);
//        mResetBtn = (ImageView) findViewById(R.id.videoPlayer_ctrlbar_btn_reset);
//        mGyroModeBtn = (ImageView) findViewById(R.id.videoPlayer_ctrlbar_btn_gyro);
//        mGlassesModeBtn = (ImageView) findViewById(R.id.videoPlayer_ctrlbar_btn_glasses);
//        mSeekBar = (SeekBar) findViewById(R.id.videoPlayer_ctrlbar_seekbar);
//        mProgressBarLayout = findViewById(R.id.videoPlayer_seek_loadingLayout);
//        mTitle = (TextView) findViewById(R.id.movie_ctrlbar_text_name);
//
//        mBackBtn.setOnClickListener(this);
//        mPauseBtn.setOnClickListener(this);
//        mSeekBar.setOnSeekBarChangeListener(this);
//        mResetBtn.setOnClickListener(this);
//        mGyroModeBtn.setOnClickListener(this);
//        mGlassesModeBtn.setOnClickListener(this);

//        mBottomControlView.setOnTouchListener(blockTouchToGlSurface);
//        mTopControlView.setOnTouchListener(blockTouchToGlSurface);
//        playerProgressCtrl = new PlayerProgressCtrl(mProgressBarLayout, null, null);
//        playerProgressCtrl.enableTraceMode(false);

//        flipper = (ViewFlipper) findViewById(R.id.flipper);
//        mojingSurfaceView = (MojingSurfaceView) flipper.getChildAt(Constant.ID_MOJING);
        mySurfaceView = (VRGLSurfaceView) findViewById(R.id.mySurfaceView);//.getChildAt(Constant.ID_MY);
        mySurfaceView.setEGLContextClientVersion(2);
//        mojingSurfaceView.setOnTouchListener(this);
        mySurfaceView.setOnTouchListener(this);
        presenter.setGLSurfaceView(mySurfaceView);

//        MojingSDK.Init(this);
//        selectSurfaceView(ID_MY, true);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        presenter.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        presenter.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestory();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
//            case R.id.videoPlayer_ctrlbar_btn_reset:
//                resetTouchPosition();
////                getRenderer().resetScale();
//                break;
            case R.id.videoPlayer_ctrlbar_btn_gyro:
//                if (!isGyroMode) {
//                    startGyroMode();
//                } else {
//                    startTouchMode();
//                }
                break;

            case R.id.videoPlayer_ctrlbar_btn_glasses:
//                if (!isGlassesMode) {
//                    mResetBtn.setVisibility(View.GONE);
//                    showGlassModeDialog();
//                    FullVideoStatisticUtils.mojingCount(this,
//                            FullVideoStatisticUtils.MOJING_DISPLAY_CLICK,
//                            FullVideoStatisticUtils.STATUS_CLICK);
//                } else {
//                    startTouchMode();
//                }
                break;
            case R.id.videoPlayer_ctrlbar_btn_playpause:
//                pauseOrPlay();
                break;
            case R.id.videoPlayer_ctrlbar_btn_back:
//                onBackEvent();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//        mCurrTime.setText(VrUtils.getStringTime(progress));
//        mTotalTime.setText(VrUtils.getStringTime(seekBar.getMax()));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
//        handler.removeMessages(MSG_UPDATE_SEEK_BAR);
//        cancleAuToHide();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
//        presenter.seekTo(seekBar.getProgress());
//        handler.sendEmptyMessage(MSG_UPDATE_SEEK_BAR);
//        handler.sendEmptyMessage(MSG_DISMISS_CONTROLLER_BAR);
    }


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
//                getRenderer().scaleEnd();
                float xd = Math.abs(event.getX() - touchStartX);
                float yd = Math.abs(event.getY() - touchStartY);
                if (xd < MIN_MOVE_SPAN && yd < MIN_MOVE_SPAN) { // 滑动距离小于阈值,鉴定为单击事件
                    if (touchEventMode == TOUCH_EVENT_MODE_NORMAL) {
//                        handler.removeMessages(UPDATE_VIDEO_TITLEBAR_ISSHOW);
//                        handler.sendEmptyMessageDelayed(UPDATE_VIDEO_TITLEBAR_ISSHOW, 300);
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (touchEventMode == TOUCH_EVENT_MODE_ZOOM) {
                    if (event.getPointerCount() == 1) {
                        break;
                    }
                    float zoomEndDistance = VrUtils.getDistance(event);
                    if (zoomEndDistance > 10f) {
                        float scale = zoomEndDistance / zoomStartDistance;
//                        getRenderer().setScale(scale, true);
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
//                    getRenderer().setTouchData(touchDeltaX, touchDeltaY, true);
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                touchEventMode = TOUCH_EVENT_MODE_ZOOM;
                zoomStartDistance = VrUtils.getDistance(event);
                break;
        }
        touchPrevX = x;
        touchPrevY = y;
        return true;
    }


    // 归零
    private void resetTouchPosition() {
        touchDeltaX = 0;
        touchDeltaY = 0;
        touchPrevX = 0;
        touchPrevY = 0;
        touchEventMode = 0;
//        getRenderer().setTouchData(0, 0, false);
    }

}
