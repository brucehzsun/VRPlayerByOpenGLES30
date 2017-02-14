
package bruce.sun.vr.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
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
import com.baofeng.mojing.input.base.MojingKeyCode;

import java.lang.reflect.Field;

import bruce.sun.vr.R;
import bruce.sun.vr.db.VrPreferences;
import bruce.sun.vr.mojing.ManufacturerList;
import bruce.sun.vr.render.BaseRenderer;
import bruce.sun.vr.render.MyRenderer;
import bruce.sun.vr.render.RendererListener;
import bruce.sun.vr.render.VrRender;


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class VRActivity extends Activity implements OnClickListener,
        SeekBar.OnSeekBarChangeListener, OnTouchListener, IVRPlayView {

    private static final String TAG = "VRActivity";


    private static final int MSG_REQUEST_CODE = 1000;


    private static final int SHOW_TIME = 5000;// 3000;

    private static final String PARAM_TITLE = "title";

    private static final String PARAM_URL = "url";

    private static final String PARAM_FROM = "from";

    private static final String PARAM_LIVE = "live";

    private static final String PARAM_MOJING_INFO = "mojing_info";

    private String curCachingP2PPath;


    ViewFlipper flipper;


    private float touchPrevX;

    private float touchPrevY;

    private float touchDeltaX;

    private float touchDeltaY;


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


    int touchEventMode;

    private static final int TOUCH_EVENT_MODE_NORMAL = 1;

    private static final int TOUCH_EVENT_MODE_ZOOM = 2;

    private float zoomStartDistance;

    private String videoUrl;

    private PlayerProgressCtrl playerProgressCtrl;


    private String mFullVideoTitle;

    private boolean isLive;

    private boolean isShowMojingBtn = true;

    private int taskId;

    private final int liveCacheSize = 20 * 1024 * 1024;

    private int MIN_MOVE_SPAN; // 区分滑屏和点击的距离阈值


    private String palyUrl;
    private IVRPlayPresenter presenter;
    private MojingSurfaceView mojingSurfaceView;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_vr);
        presenter = new VRPlayPresenter(this);


        Log.d(TAG, "onCreate");
        BaseRenderer.reset();
        MIN_MOVE_SPAN = Dp2Px(this, 20);
        initView();
        initData();
    }

    private void initData() {
        palyUrl = getIntent().getStringExtra("PlayUrl");
    }

    private boolean isP2PLive() {
        return isLive;
    }

    private void initView() {
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
        playerProgressCtrl = new PlayerProgressCtrl(mProgressBarLayout, null, null);
        playerProgressCtrl.enableTraceMode(false);

        flipper = (ViewFlipper) findViewById(R.id.flipper);
        mojingSurfaceView = (MojingSurfaceView) flipper.getChildAt(Constant.ID_MOJING);
        GLSurfaceView mySurfaceView = (GLSurfaceView) flipper.getChildAt(Constant.ID_MY);
        mySurfaceView.setEGLContextClientVersion(2);
        mojingSurfaceView.setOnTouchListener(this);
        mySurfaceView.setOnTouchListener(this);

        MojingSDK.Init(this);
//        selectSurfaceView(ID_MY, true);
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
        presenter.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        isOnResume = false;
        Log.d(TAG, "onPause");
        presenter.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestory();
    }


    public boolean isMojing() {
        int curId = flipper.getDisplayedChild();
        if (curId == Constant.ID_MOJING) {
            return true;
        } else {
            return false;
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.videoPlayer_ctrlbar_btn_reset:
                resetTouchPosition();
//                getRenderer().resetScale();
                break;
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
                onBackEvent();
                break;
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (MSG_REQUEST_CODE == requestCode) {
//            startGlassesMode();
        }
    }


    private void showTouchTips() {
        mShowTipRoot.setVisibility(View.VISIBLE);
    }

    @Override
    public void showGoryTips() {
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
        if (presenter.isPlaying()) {
            presenter.pause();
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
                presenter.startGlassesMode();
            }
        };
        customDialog1.setCancelable(false);
        customDialog1.show();
    }


    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mCurrTime.setText(VrUtils.getStringTime(progress));
        mTotalTime.setText(VrUtils.getStringTime(seekBar.getMax()));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
//        handler.removeMessages(MSG_UPDATE_SEEK_BAR);
        cancleAuToHide();
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        presenter.seekTo(seekBar.getProgress());
//        handler.sendEmptyMessage(MSG_UPDATE_SEEK_BAR);
//        handler.sendEmptyMessage(MSG_DISMISS_CONTROLLER_BAR);
        auToHide();
    }


    private void showContorlView() {
        cancleAuToHide();
//        if (!isGlassesMode) {
//            mResetBtn.setVisibility(View.VISIBLE);
//        }
        mTopControlView.setVisibility(View.VISIBLE);
        mBottomControlView.setVisibility(View.VISIBLE);
//        handler.sendEmptyMessage(MSG_UPDATE_SEEK_BAR);

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
//        handler.removeMessages(MSG_UPDATE_SEEK_BAR);

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
//        handler.removeMessages(MSG_DISMISS_CONTROLLER_BAR);
//        handler.sendEmptyMessageDelayed(MSG_DISMISS_CONTROLLER_BAR, SHOW_TIME);
    }

    /**
     * 3秒后自动隐藏提示
     */
    @Override
    public void auToHideTip() {
//        handler.removeMessages(MSG_DISMISS_TIP_LAYER);
//        handler.sendEmptyMessageDelayed(MSG_DISMISS_TIP_LAYER, SHOW_TIME);
    }

    /**
     * 取消自动隐藏
     */
    private void cancleAuToHide() {
//        handler.removeMessages(MSG_DISMISS_CONTROLLER_BAR);
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
                    float zoomEndDistance = getDistance(event);
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


    // 归零
    private void resetTouchPosition() {
        touchDeltaX = 0;
        touchDeltaY = 0;
        touchPrevX = 0;
        touchPrevY = 0;
        touchEventMode = 0;
//        getRenderer().setTouchData(0, 0, false);
    }


    @Override
    public void showProgress(boolean isShow) {
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
}
