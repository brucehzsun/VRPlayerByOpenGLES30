package bruce.sun.vr.ui;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

import bruce.sun.vr.surface.VRGLSurfaceView;

/**
 * Update by sunhongzhi on 2017/2/14.
 */

public class VRPlayPresenter implements IVRPlayPresenter, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnErrorListener, MediaPlayer.OnVideoSizeChangedListener, MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnCompletionListener {
    private static final String TAG = "VRPlayPresenter";

    private final Activity context;

    private MediaPlayer mMediaPlayer;

    private IVRPlayView ivrPlayView;

    private VRGLSurfaceView glSurfaceView;

    private boolean isBuffering;

    public VRPlayPresenter(IVRPlayView ivrPlayView) {
        context = (Activity) ivrPlayView;
        this.ivrPlayView = ivrPlayView;


    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
    }


    @Override
    public void onResume() {
        glSurfaceView.onResume();
//        startGyroTracking();
//        if (ivrPlayView.isMojing()) {
//            onResumeMojing();
//        } else {
//            onResumeMy();
//        }
    }

    @Override
    public void onPause() {
        glSurfaceView.onPause();
//        stopGyroTracking();
//        if (ivrPlayView.isMojing()) {
//            onPauseMojing();
//        } else {
//            onPauseMy();
//        }
    }

    @Override
    public void setGLSurfaceView(VRGLSurfaceView mySurfaceView) {
        this.glSurfaceView = mySurfaceView;

    }

    @Override
    public void doPlay(String playUrl) {
        // flipper发生切换时，薪的glsurface会重新创建,导致再次调用play方法
        if (mMediaPlayer != null) {
            return;
        }
        Log.d(TAG, "doPlay");
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setSurface(glSurfaceView.getSurface());
            mMediaPlayer.setDataSource(playUrl);
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnVideoSizeChangedListener(this);
            mMediaPlayer.setOnInfoListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setTouchData(int touchDeltaX, int touchDeltaY, boolean b) {
        glSurfaceView.setTouchData(touchDeltaX, touchDeltaY, b);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, " mMediaPlayer.onError();");
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
//        getRenderer().setDefaultBufferSize(mMediaPlayer.getVideoWidth(),
//                mMediaPlayer.getVideoHeight());
//                    if (VrPreferences.getInstance(VRActivity.this).isFirstTouchMode()) {
//                        showTouchTips();
//                        auToHideTip();
//                        VrPreferences.getInstance(VRActivity.this).setFirstTouchMode(false);
//                    }
//                    auToHide();
        Log.d(TAG, " onPrepared");
        mp.start();
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.d(TAG, "onVideoSizeChanged width = " + width + ",height = " + height);
        //                            if (checkStart()) {
//                                handler.sendEmptyMessageDelayed(MSG_UPDATE_SEEK_BAR, 1000);
//                            }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion");
        try {
            mMediaPlayer.stop();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        mMediaPlayer.release();
        mMediaPlayer = null;
//                    finish();
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        Log.d(TAG, "onInfo what = " + what);
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

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.d(TAG, "onSeekComplete");
        // 没调用seekTo方法，居然也有可能收到onSeekComplete,无语中
//                    if (isSeeking) {
//                        isSeeking = false;
//                        showProgress(false);
//                    }
    }


}
