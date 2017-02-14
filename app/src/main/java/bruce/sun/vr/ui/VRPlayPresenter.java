package bruce.sun.vr.ui;

import android.app.Activity;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.baofeng.mojing.MojingSDK;
import com.baofeng.mojing.MojingSurfaceView;
import com.baofeng.mojing.MojingVrLib;

import java.io.IOException;
import java.lang.ref.WeakReference;

import bruce.sun.vr.R;
import bruce.sun.vr.db.VrPreferences;
import bruce.sun.vr.mojing.ManufacturerList;
import bruce.sun.vr.render.BaseRenderer;
import bruce.sun.vr.render.MyRenderer;
import bruce.sun.vr.render.VrRender;

import static bruce.sun.vr.R.id.mojingSurfaceView;
import static bruce.sun.vr.R.id.mySurfaceView;

/**
 * Update by sunhongzhi on 2017/2/14.
 */

public class VRPlayPresenter implements IVRPlayPresenter {
    private static final String TAG = "VRPlayPresenter";
    private final Activity activity;

    private MediaPlayer mMediaPlayer;

    Handler handler;


    VrRender mojingRenderer;

    MyRenderer myRenderer;

    BaseRenderer curRenderer;

    public VRPlayPresenter(IVRPlayView ivrPlayView) {
        activity = (Activity) ivrPlayView;
        this.ivrPlayView = ivrPlayView;
    }

    private IVRPlayView ivrPlayView;

    protected void play(String videoUrl) {
        // flipper发生切换时，薪的glsurface会重新创建,导致再次调用play方法
        if (mMediaPlayer != null) {
            return;
        }
        Log.d(TAG, "创建MediaPlayer");
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setSurface(getRenderer().getSurface());
            mMediaPlayer.setDataSource(videoUrl);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    getRenderer().setDefaultBufferSize(mMediaPlayer.getVideoWidth(),
                            mMediaPlayer.getVideoHeight());
//                    if (VrPreferences.getInstance(VRActivity.this).isFirstTouchMode()) {
//                        showTouchTips();
//                        auToHideTip();
//                        VrPreferences.getInstance(VRActivity.this).setFirstTouchMode(false);
//                    }
//                    auToHide();
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
//                            if (checkStart()) {
//                                handler.sendEmptyMessageDelayed(MSG_UPDATE_SEEK_BAR, 1000);
//                            }
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
//                                showProgress(true);
                            }
                            break;

                        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                            if (isBuffering) {
                                isBuffering = false;
//                                showProgress(false);
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
//                    if (isSeeking) {
//                        isSeeking = false;
//                        showProgress(false);
//                    }
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
//                    finish();
                }
            });
            mMediaPlayer.prepareAsync();
            Log.d(TAG, "mMediaPlayer prepare");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
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

    @Override
    public void pause() {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            Log.d(TAG, "pause() mMediaPlayer.pause();");
//            cancleAuToHide();
        }
    }

    private void goToStart() {
        if (mMediaPlayer != null) {
            mMediaPlayer.start();
            Log.d(TAG, "goToStart  mMediaPlayer.start();");
//            auToHide();
        }
    }

    public void onRendererReady() {
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
//        if (mMediaPlayer != null && isPlayerPrepared && isVideoSizeChanged && isRendererReady) {
//            return true;
//        }
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

    @Override
    public void onDestory() {

    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public void seekTo(int msec) {
        if (mMediaPlayer == null) {
            return;
        }
//        if (!isPlayerPrepared) {
//            return;
//        }
//        isSeeking = true;
//        showProgress(true);
        mMediaPlayer.seekTo(msec);
    }

    private void selectSurfaceView(int id, boolean isOnCreate) {
        if (ivrPlayView.isMojing()) {
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

    @Override
    public void onResume() {
        startGyroTracking();
        if (isMojing()) {
            onResumeMojing();
        } else {
            onResumeMy();
        }
    }

    @Override
    public void onPause() {
        stopGyroTracking();
        if (isMojing()) {
            onPauseMojing();
        } else {
            onPauseMy();
        }
    }

    private void resetModeFlag() {
        isTouchMode = false;
        isGyroMode = false;
        isGlassesMode = false;
        mGyroModeBtn.setImageResource(R.drawable.movie_ctrlbar_btn_tly_selector);
        mGlassesModeBtn.setImageResource(R.drawable.movie_ctrlbar_btn_mj_selector);
    }

    public void onChangeMojingWorld() {
        float fov = com.baofeng.mojing.MojingSDK.GetMojingWorldFOV() / 2.f;
        float ratio = (float) Math.tan(Math.toRadians(fov));
        mojingRenderer.getMatrixState().setProjectFrustum(-ratio, ratio, -ratio, ratio, 1.f, 800);
    }

    private BaseRenderer getRenderer() {
        if (isMojing()) {
            return mojingRenderer;
        } else {
            return myRenderer;
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

    private static class MyHandler extends Handler {
        private final WeakReference<VRPlayPresenter> mActivity;

        public MyHandler(VRPlayPresenter activity) {
            mActivity = new WeakReference<VRPlayPresenter>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mActivity == null) {
                return;
            }
            VRPlayPresenter activity = mActivity.get();
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

}
