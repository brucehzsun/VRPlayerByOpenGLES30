package bruce.sun.vr.ui;

import android.opengl.GLSurfaceView;

import bruce.sun.vr.surface.VRGLSurfaceView;

/**
 * Update by sunhongzhi on 2017/2/14.
 */

public interface IVRPlayPresenter {
    void onDestroy();

    void onResume();

    void onPause();

    void setGLSurfaceView(VRGLSurfaceView mySurfaceView);

    void doPlay(String playUrl);

    void setTouchData(int touchDeltaX, int touchDeltaY, boolean b);

    void onClickGyroButton();
}
