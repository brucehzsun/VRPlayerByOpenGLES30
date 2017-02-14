
package bruce.sun.vr.ui;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;


public class PlayerProgressCtrl {

    public interface PlayerProgressListener {
        public int getCurrentPosition();

        public void onPlayerProgressShow();

        public void onPlayerProgressHide();
    }

    private String TAG;

    private boolean traceMode;

    // 设置一个阈值，用于判断播放器已经开始播放，从而需要隐藏ProgressBar
    private static final int PLAYER_POSITION_DIFF_WHEN_PLAYING = 1000;

    private static final int PLAYER_POSITION_CHECK_DELAY = 200;

    private Handler handler;

    private View mProgressBarParent;

    private PlayerProgressListener listener;

    // 显示ProgressBar的累计次数(参照引用计数的概念)
    private int showProgressRef;

    // 显示ProgressBar时，播放器的当前播放位置(如尚未开始播放,则可为记忆点值)
    private int playerPositionWhenProgressShown;

    private Runnable fixNeverToComeHideProgressRunnable = new Runnable() {
        @Override
        public void run() {
            fixNeverToComeHideProgress();
        }
    };

    public PlayerProgressCtrl(View progressBarParent, PlayerProgressListener listener,
                              Handler handler) {
        this.mProgressBarParent = progressBarParent;
        this.handler = handler;
        this.listener = listener;
        this.TAG = getClass().getSimpleName() + "_" + listener.getClass().getSimpleName();
    }

    public void enableTraceMode(boolean enable) {
        this.traceMode = enable;
    }

    public void showProgress(final boolean isShow) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showProgressInMainThread(isShow);
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    showProgressInMainThread(isShow);
                }
            });
        }
    }

    // 由于某些特殊原因，ProgressBar隐藏消息可能一直不来，这时应通过播放器是否已经开始播放进行处理
    private void fixNeverToComeHideProgress() {
        handler.removeCallbacks(fixNeverToComeHideProgressRunnable);

        if (isPlayerPlayingWithoutHideProgress()) {
            if (getShowProgressRef() > 0) {
                Log.e(TAG, "oops!!! the Player is Playing Without Hiding ProgressBar!!!");

                setShowProgressRef(0);
                setProgressBarVisibility(View.GONE);
                return;
            }
        }

        Log.i(TAG, "fixNeverToComeHideProgress waiting...");
        handler.postDelayed(fixNeverToComeHideProgressRunnable, PLAYER_POSITION_CHECK_DELAY);
    }

    protected boolean isPlayerPlayingWithoutHideProgress() {
        int playerPosition = listener.getCurrentPosition();
        int diff = Math.abs(playerPosition - playerPositionWhenProgressShown);

        if (diff >= PLAYER_POSITION_DIFF_WHEN_PLAYING) {
            return true;
        } else {
            return false;
        }
    }

    private void setProgressBarVisibility(int visibility) {
        mProgressBarParent.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            listener.onPlayerProgressShow();
        } else {
            listener.onPlayerProgressHide();
        }
        Log.w(TAG, "setProgressBarVisibility:" + visibility);
    }

    private void trace() {
        if (!traceMode) {
            return;
        }
        try {
            throw new Exception("setShowProgressRef,ThreadId:" + Thread.currentThread().getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setShowProgressRef(int ref) {
        trace();
        showProgressRef = ref;
        Log.i(TAG, "setShowProgressRef:" + ref);
    }

    private int getShowProgressRef() {
        return this.showProgressRef;
    }

    private void showProgressInMainThread(boolean isShow) {
        if (isShow) {
            // 引用计数加1
            setShowProgressRef(showProgressRef + 1);
        } else {
            // 引用计数减1
            setShowProgressRef(showProgressRef - 1);
        }
        if (getShowProgressRef() > 0) { // 引用计数大于0，保持显示
            if (getShowProgressRef() == 1) {
                playerPositionWhenProgressShown = listener.getCurrentPosition();

                setProgressBarVisibility(View.VISIBLE);

                // 万一隐藏消息一直不来
                fixNeverToComeHideProgress();
            }
        } else { // 否则隐藏

            if (getShowProgressRef() < 0) {
                Log.e(TAG, "oops!! you must have done sth wrong, go check it, now!!!");
                setShowProgressRef(0);
            }

            setProgressBarVisibility(View.GONE);
            handler.removeCallbacks(fixNeverToComeHideProgressRunnable);
        }
    }
}
