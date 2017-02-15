
package bruce.sun.vr.render;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;

import bruce.sun.vr.domain.SemiSphereModel;
import bruce.sun.vr.domain.SphereModel;
import bruce.sun.vr.utils.MatrixState;
import bruce.sun.vr.utils.ShaderUtil;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
public abstract class BaseRenderer implements GLSurfaceView.Renderer, OnFrameAvailableListener {

    protected static String TAG;

    protected int[] textures;

    protected SurfaceTexture surfaceTexture;

    private Surface surface;

    protected int[] lock = new int[1];

    protected boolean isFrameAvailable = false;

    protected Context context;

    protected Handler handler;

    protected RendererListener listener;

    protected MatrixState matrixState = new MatrixState();

    protected SphereModel sphere;

    protected GLSurfaceView glView;

    protected float ratio;

    protected boolean isRenderWhenDirty;

    protected boolean isTmpRenderContinuously;

    protected int glRenderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY;

    protected static final int SWITCH_MODE_END_FRAMES = 5; // 切换模式后，渲染多少个视频帧才算切换完成.

    private static final int SPHERE_RADIUS = 100;

    private int frames;

    protected int draws;

    private int drawsPrev;

    private long fpsTimePrev;

    protected static boolean isSemiSphere;

    protected static boolean isGyroTrackEnabled;

    protected static float touchX;

    protected static float touchY;

    protected static float touchLookDeltaX; // 由于LookAt方向的差别导致的x轴的旋转角度偏移

    public BaseRenderer(Context context, Handler handler) {
        this.context = context.getApplicationContext();
        this.handler = handler;
        TAG = getClass().getSimpleName();
    }

    public void setListener(RendererListener listener) {
        this.listener = listener;
    }

    public void setGlSurfaceView(GLSurfaceView glView, int glRenderMode) {

    }

    public void reset() {
        isGyroTrackEnabled = false;
        touchX = 0;
        touchY = 0;
        touchLookDeltaX = 0;
    }

    public void setSemiSphere(boolean isSemiSphere) {
        BaseRenderer.isSemiSphere = isSemiSphere;
    }

    private void createSphere() {
        matrixState.setInitStack();
        if (isSemiSphere) {
            sphere = new SemiSphereModel(matrixState, SPHERE_RADIUS);
        } else {
            sphere = new SphereModel(matrixState, SPHERE_RADIUS);
        }
    }

    private void init() {
        initTexture();
        isFrameAvailable = false;
        frames = 0;
        draws = 0;
        drawsPrev = 0;
        fpsTimePrev = System.currentTimeMillis();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");
        init();
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onRendererInit();
                    listener.onSwitchModeStart();
                }
            });
        }
        createSphere();
        onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged,w:" + width + ",h:" + height + "," + this);
        onSurfaceChanged(width, height);
        if (listener != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onRendererReady();
                }
            });
        }
    }

    protected abstract void onSurfaceCreated();

    protected abstract void onSurfaceChanged(int width, int height);

    protected abstract void renderToTexture();

    protected void lookBack() {
        matrixState.setCamera(0, 0, 0, 0f, 0.0f, -0.1f, 0f, 1.0f, 0.0f);
    }

    protected void lookLeft() {
        matrixState.setCamera(0, 0, 0, -0.1f, 0f, 0, 0f, 1.0f, 0.0f);
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

    @Override
    public void onDrawFrame(GL10 gl) {
        // Log.i(TAG, "onDrawFrame,frameAvailable:" + frameAvailable +
        // ",this:" + this);
        boolean isSwitchModeEnd = false;
        synchronized (lock) {
            if (isFrameAvailable) {
                surfaceTexture.updateTexImage();
                isFrameAvailable = false;
                if (frames < SWITCH_MODE_END_FRAMES) {
                    frames += 1;
                    isSwitchModeEnd = (frames == SWITCH_MODE_END_FRAMES);
                }
            }
            draws++;
        }
        renderToTexture();
        if (isSwitchModeEnd) {
            onSwitchModeEnd();
        }
    }

    protected void onSwitchModeEnd() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onSwitchModeEnd();
                }
            }
        });
    }

    private void initTexture() {
        textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        ShaderUtil.checkGlError("Texture bind");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        synchronized (lock) {
            surfaceTexture = new SurfaceTexture(textures[0]);
            surfaceTexture.setOnFrameAvailableListener(this);
            surface = new Surface(surfaceTexture);
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (lock) {
            isFrameAvailable = true;
            if (isRenderWhenDirty && !isTmpRenderContinuously) {
                glView.requestRender();
            }
        }
    }

    protected void drawSphere() {
        sphere.drawSelf(textures[0]);
    }

    public void setTouchData(float touchX, float touchY, boolean isOnTouch) {

    }

    public void setScale(float scaleRatio, boolean isOnTouch) {

    }

    public void scaleEnd() {

    }

    public void resetScale() {

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

    public void setGyroTrackEnabled(boolean enable) {
        synchronized (lock) {
            isGyroTrackEnabled = enable;
            resetLookAt();
            if (!isSemiSphere) {
                touchX -= touchLookDeltaX;
                if (!isGyroTrackEnabled) {
                    touchLookDeltaX = 0f;
                } else {
                    touchLookDeltaX = -90f;
                }
                touchX += touchLookDeltaX;
            }
        }
    }

    public boolean isGyroTrackEnabled() {
        return isGyroTrackEnabled;
    }

    protected boolean isLookLeft() {
        if (!isGyroTrackEnabled && !isSemiSphere) {
            return true;
        }
        return false;
    }

    protected boolean applyTouchY() {
        return true;
    }

    protected void rotateByTouch() {
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

    protected void resetLookAt() {
        if (isLookLeft()) {
            lookLeft();
        } else {
            lookBack();
        }
    }

    public void onPause() {

    }

    public void onResume() {

    }
}
