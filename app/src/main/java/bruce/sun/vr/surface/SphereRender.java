
package bruce.sun.vr.surface;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Surface;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import bruce.sun.vr.domain.SemiSphereModel;
import bruce.sun.vr.domain.SphereModel;
import bruce.sun.vr.utils.MatrixState;
import bruce.sun.vr.utils.ShaderUtil;

public class SphereRender implements GLSurfaceView.Renderer, OnFrameAvailableListener {

    private static final String TAG = "SphereRender";
    private final GLSurfaceView glSurfaceView;

    private int[] textures;

    private SurfaceTexture surfaceTexture;

    private Surface surface;

    private int[] lock = new int[1];

    private boolean isFrameAvailable = false;

    private Context context;

    private MatrixState matrixState = new MatrixState();

    private SphereModel sphere;

    private float ratio;

    private boolean isRenderWhenDirty;

    private boolean isTmpRenderContinuously;

    private static final int SWITCH_MODE_END_FRAMES = 5; // 切换模式后，渲染多少个视频帧才算切换完成.

    private static final int SPHERE_RADIUS = 100;

    private int frames;

    private int draws;

    private int drawsPrev;

    private long fpsTimePrev;

    private static boolean isSemiSphere;

    private static boolean isGyroTrackEnabled;

    private static float touchX;

    private static float touchY;

    private static float touchLookDeltaX; // 由于LookAt方向的差别导致的x轴的旋转角度偏移


    private float[] fM;

    private float screenSizeRatio;

    private float scaleRatio;

    private float scaleRatioPrev;

    // 双指缩放时，终止指间距/起始指间距 的最大值
    private static final float zoom_range = 3f;

    // 双指缩放时，终止指间距/起始指间距 的最小值
    private static final float rev_zoom_range = 1f / zoom_range;

    // 画面最大放大倍数
    private static final float max_scale_ratio = 5f;

    private static final long BACK_TO_RENDER_WHEN_DIRTY_DELAY = 10 * 1000;

    private static final long BACK_TO_RENDER_WHEN_DIRTY_DELAY_REDO = BACK_TO_RENDER_WHEN_DIRTY_DELAY / 2;

    private long backToRenderWhenDirtyDelayStart;

    private float lastRatio;

    public SphereRender(GLSurfaceView glSurfaceView) {
        this.context = glSurfaceView.getContext().getApplicationContext();
        this.glSurfaceView = glSurfaceView;
        createTexture();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        reset();

        if (glSurfaceView.getRenderMode() == GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
            isRenderWhenDirty = true;
            if (isFrameAvailable) {
                glSurfaceView.requestRender();
            }
        } else {
            isRenderWhenDirty = false;
        }

//                    listener.onRendererInit();
//                    listener.onSwitchModeStart();

        if (isSemiSphere) {
            sphere = new SemiSphereModel(matrixState, SPHERE_RADIUS);
        } else {
            sphere = new SphereModel(matrixState, SPHERE_RADIUS);
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged,w:" + width + ",h:" + height + "," + this);
        GLES20.glViewport(0, 0, width, height);
        screenSizeRatio = Math.max(width, height) * 1.0f / Math.min(width, height);
        matrixState.setProjectFrustum(-ratio * screenSizeRatio, ratio * screenSizeRatio, -ratio,
                ratio, 1.0f, 800);
        resetLookAt();
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        matrixState.setInitStack();
//                    listener.onRendererReady();
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        boolean isSwitchModeEnd = false;
        synchronized (lock) {
            if (isFrameAvailable) {
                surfaceTexture.updateTexImage();
                isFrameAvailable = false;
//                Log.i(TAG, "onDrawFrame,frameAvailable:" + isFrameAvailable);
                if (frames < SWITCH_MODE_END_FRAMES) {
                    frames += 1;
                    isSwitchModeEnd = (frames == SWITCH_MODE_END_FRAMES);
                }
            }
            draws++;
        }
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        matrixState.pushMatrix();
//        if (isGyroTrackEnabled) {
//            if (fM == null) {
//                fM = new float[16];
//            }
//            com.baofeng.mojing.MojingSDK.getLastHeadView(fM);
//            matrixState.setViewMatrix(fM);
//        }
        rotateByTouch();
        sphere.drawSelf(textures[0]);
        matrixState.popMatrix();
        if (isSwitchModeEnd) {
            onSwitchModeEnd();
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (lock) {
//            Log.d(TAG, ">> nFrameAvailable lock, isRenderWhenDirty = " + isRenderWhenDirty + ",isTmpRenderContinuously = " + isTmpRenderContinuously);
            isFrameAvailable = true;
            if (isRenderWhenDirty && !isTmpRenderContinuously) {
                Log.d(TAG, ">> >> nFrameAvailable requestRender");
                glSurfaceView.requestRender();
            }
        }
    }

    private void createTexture() {
        textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        ShaderUtil.checkGlError("Texture bind");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        surfaceTexture = new SurfaceTexture(textures[0]);
        surfaceTexture.setOnFrameAvailableListener(this);
        surface = new Surface(surfaceTexture);
    }

    private void rotateByTouch() {
        if (!isLookLeft()) {
            // 朝后看
            if (applyTouchY() && touchY != 0) {
                matrixState.rotate(-touchY, 1.0f, 0.0f, 0.0f);
            }
            if (touchX != 0) {
                matrixState.rotate(touchX, 0.0f, 1.0f, 0.0f);
            }
        } else {
            // 朝左看
            if (applyTouchY() && touchY != 0) {
                matrixState.rotate(touchY, 0.0f, 0.0f, 1.0f);
            }
            if (touchX != 0) {
                matrixState.rotate(touchX, 0.0f, 1.0f, 0.0f);
            }
        }
    }

    private void reset() {
        isFrameAvailable = false;
        frames = 0;
        draws = 0;
        drawsPrev = 0;
        fpsTimePrev = System.currentTimeMillis();
        ratio = 1f;
        lastRatio = 1f;
        scaleRatio = 1f;
        scaleRatioPrev = 0f;
        isTmpRenderContinuously = false;
        backToRenderWhenDirtyDelayStart = 0;
        isGyroTrackEnabled = false;
        touchX = 0;
        touchY = 0;
        touchLookDeltaX = 0;
    }

    private boolean applyTouchY() {
        return true;
    }

    private boolean isLookLeft() {
        if (!isGyroTrackEnabled && !isSemiSphere) {
            return true;
        }
        return false;
    }


    private void resetLookAt() {
        if (isLookLeft()) {
            lookLeft();
        } else {
            lookBack();
        }
    }

    private void lookBack() {
        matrixState.setCamera(0, 0, 0, 0f, 0.0f, -0.1f, 0f, 1.0f, 0.0f);
    }

    private void lookLeft() {
        matrixState.setCamera(0, 0, 0, -0.1f, 0f, 0, 0f, 1.0f, 0.0f);
    }

    public void setTouchData(float touchX, float touchY, boolean isTouch) {
        synchronized (lock) {
            touchX += touchLookDeltaX;
            // 用户并未改变滑屏数据(当用户仅仅是单击一次GLSurfaceView，不改变渲染帧率)
            if (this.touchX == touchX && this.touchY == touchY) {
                return;
            }
            this.touchX = touchX;
            this.touchY = touchY;
            if (isTouch) {
                tmpRenderContinuously();
            }
        }
    }

    private void tmpRenderContinuously() {
        synchronized (lock) {
            if (!isRenderWhenDirty) {
                return;
            }
            if (backToRenderWhenDirtyDelayStart > 0) {// 延时任务已开启
                long now = System.currentTimeMillis();
                if (now - backToRenderWhenDirtyDelayStart >= BACK_TO_RENDER_WHEN_DIRTY_DELAY_REDO) {
//                    handler.removeCallbacks(backToRenderWhenDirty);
                    backToRenderWhenDirtyDelayStart = now;
//                    handler.postDelayed(backToRenderWhenDirty, BACK_TO_RENDER_WHEN_DIRTY_DELAY);
                    Log.i("xx", "redo backToRenderWhenDirty");
                }
            } else { // 延时任务未开启
                isTmpRenderContinuously = true;
                glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                backToRenderWhenDirtyDelayStart = System.currentTimeMillis();
//                handler.postDelayed(backToRenderWhenDirty, BACK_TO_RENDER_WHEN_DIRTY_DELAY);
                Log.i("xx", "do backToRenderWhenDirty");
            }
        }
    }


    //--------------------------------------------------------------------------------------------------------------

    public void setSemiSphere(boolean isSemiSphere) {
        SphereRender.isSemiSphere = isSemiSphere;
    }


    public float getFPS() {
        float fps = 0;
        long now = System.currentTimeMillis();
        long span = now - fpsTimePrev;
        if (fpsTimePrev > 0 && span >= 50) { // 时间间隔过短会导致fps值不准,因此设置时间间隔阈值
            fps = 1000.0f * (draws - drawsPrev) / span;
        }
        drawsPrev = draws;
        fpsTimePrev = now;
        return fps;
    }


    protected void onSwitchModeEnd() {
//        handler.post(new Runnable() {
//            @Override
//            public void run() {
//                if (listener != null) {
//                    listener.onSwitchModeEnd();
//                }
//            }
//        });
    }


    public void setDefaultBufferSize(int width, int height) {
        if (surfaceTexture == null) {
            return;
        }
        surfaceTexture.setDefaultBufferSize(width, height);
    }

    public Surface getSurface() {
        return this.surface;
    }

    public MatrixState getMatrixState() {
        return matrixState;
    }


    public boolean isGyroTrackEnabled() {
        return isGyroTrackEnabled;
    }


//    private Runnable backToRenderWhenDirty = new Runnable() {
//        @Override
//        public void run() {
//            synchronized (lock) {
//                initRenderMode();
//
//                if (isRenderWhenDirty) { // 切换陀螺仪模式时，会修改isRenderWhenDirty为false，因此需要判断延时过程中是否发生切换
//                    glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//                    if (isFrameAvailable) {
//                        glView.requestRender();
//                    }
//                }
//
//                Log.i("xx", "done backToRenderWhenDirty");
//            }
//        }
//    };



    private float format(float r) {
        if (r > zoom_range) {
            r = zoom_range;
        } else if (r < rev_zoom_range) {
            r = rev_zoom_range;
        }
        if (r < 1) {
            r = -1f / r + 1;
        } else {
            r = r - 1;
        }
        return r;
    }

    private void setScale(float scaleRatio, boolean isOnTouch) {
        float fScaleRatio = format(scaleRatio);
        float ratio = 1f;
        float scale = 1f;
        scale = fScaleRatio + scaleRatioPrev;
        if (scale < 0) {
            scale = 0;
        } else if (scale >= zoom_range - 1) {
            scale = zoom_range - 1;
        }
        this.scaleRatio = scale;
        ratio = 1 - scale / (zoom_range - 1) * (1f - 1f / max_scale_ratio);
        // Log.i("xxx", "r:" + scale + ",ratio:" + ratio + ",scaleRatio:"
        // + scaleRatio
        // + ",scaleRatioPrev:" + scaleRatioPrev);

        // 用户并未改变缩放比例(当用户仅仅是把两个手指放在GLSurfaceView上，但是并没有进行指间移动)
        if (ratio == lastRatio) {
            return;
        }
        lastRatio = ratio;
//        tmpRenderContinuously();
        matrixState.setProjectFrustum(-ratio * screenSizeRatio, ratio * screenSizeRatio, -ratio,
                ratio, 1.0f, 800);
    }

    public void scaleEnd() {
        scaleRatioPrev = scaleRatio;
    }

//    public void resetScale() {
//        scaleRatioPrev = 0f;
//        setScale(1f, false);
//    }


//    public void setGyroTrackEnabled(boolean enable) {
//        synchronized (lock) {
//            isGyroTrackEnabled = enable;
//            resetLookAt();
//            if (!isSemiSphere) {
//                touchX -= touchLookDeltaX;
//                if (!isGyroTrackEnabled) {
//                    touchLookDeltaX = 0f;
//                } else {
//                    touchLookDeltaX = -90f;
//                }
//                touchX += touchLookDeltaX;
//            }
//            initRenderMode();
//            handler.removeCallbacks(backToRenderWhenDirty);
//        }
//    }


}
