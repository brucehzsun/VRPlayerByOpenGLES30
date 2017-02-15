package bruce.sun.vr.ui;

import android.opengl.GLSurfaceView;

/**
 * Update by sunhongzhi on 2017/2/14.
 */

public interface IVRPlayPresenter {
    void onDestory();

    boolean isPlaying();

    void seekTo(int progress);

    void pause();

    void startGlassesMode();

    void onResume();

    void onPause();

    void setGLSurfaceView(GLSurfaceView mySurfaceView);
}
