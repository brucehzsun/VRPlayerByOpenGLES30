
package bruce.sun.vr.render;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.Log;

public class MyRenderer extends BaseRenderer {

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

    public MyRenderer(Context context, Handler handler) {
        super(context, handler);
        init();
    }

    private void init() {
        ratio = 1f;
        lastRatio = 1f;
        scaleRatio = 1f;
        scaleRatioPrev = 0f;
        initRenderMode();
    }

    private void initRenderMode() {
        isTmpRenderContinuously = false;
        backToRenderWhenDirtyDelayStart = 0;
    }

    private Runnable backToRenderWhenDirty = new Runnable() {
        @Override
        public void run() {
            synchronized (lock) {
                initRenderMode();

                if (isRenderWhenDirty) { // 切换陀螺仪模式时，会修改isRenderWhenDirty为false，因此需要判断延时过程中是否发生切换
                    glView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
                    if (isFrameAvailable) {
                        glView.requestRender();
                    }
                }

                Log.i("xx", "done backToRenderWhenDirty");
            }
        }
    };

    @Override
    public void setGlSurfaceView(GLSurfaceView glView, int glRenderMode) {
        synchronized (lock) {
            super.glView = glView;
            super.glRenderMode = glRenderMode;
            super.glView.setRenderMode(glRenderMode);
            if (glView != null && glRenderMode == GLSurfaceView.RENDERMODE_WHEN_DIRTY) {
                isRenderWhenDirty = true;
                if (isFrameAvailable) {
                    glView.requestRender();
                }
            } else {
                isRenderWhenDirty = false;
            }
        }
    }

    protected void renderToTexture() {
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
        matrixState.pushMatrix();
        if (isGyroTrackEnabled) {
            if (fM == null) {
                fM = new float[16];
            }
            com.baofeng.mojing.MojingSDK.getLastHeadView(fM);
            matrixState.setViewMatrix(fM);
        }
        rotateByTouch();
        drawSphere();
        matrixState.popMatrix();
    }

    @Override
    public void setTouchData(float touchX, float touchY, boolean isTouch) {
        synchronized (lock) {
            touchX += touchLookDeltaX;
            // 用户并未改变滑屏数据(当用户仅仅是单击一次GLSurfaceView，不改变渲染帧率)
            if (BaseRenderer.touchX == touchX && BaseRenderer.touchY == touchY) {
                return;
            }
            BaseRenderer.touchX = touchX;
            BaseRenderer.touchY = touchY;
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
                    handler.removeCallbacks(backToRenderWhenDirty);
                    backToRenderWhenDirtyDelayStart = now;
                    handler.postDelayed(backToRenderWhenDirty, BACK_TO_RENDER_WHEN_DIRTY_DELAY);
                    Log.i("xx", "redo backToRenderWhenDirty");
                }
            } else { // 延时任务未开启
                isTmpRenderContinuously = true;
                glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
                backToRenderWhenDirtyDelayStart = System.currentTimeMillis();
                handler.postDelayed(backToRenderWhenDirty, BACK_TO_RENDER_WHEN_DIRTY_DELAY);
                Log.i("xx", "do backToRenderWhenDirty");
            }
        }
    }

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

    @Override
    public void setScale(float scaleRatio, boolean isOnTouch) {
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
        tmpRenderContinuously();
        matrixState.setProjectFrustum(-ratio * screenSizeRatio, ratio * screenSizeRatio, -ratio,
                ratio, 1.0f, 800);
    }

    @Override
    public void scaleEnd() {
        scaleRatioPrev = scaleRatio;
    }

    @Override
    public void resetScale() {
        scaleRatioPrev = 0f;
        setScale(1f, false);
    }

    @Override
    public void setGyroTrackEnabled(boolean enable) {
        synchronized (lock) {
            super.setGyroTrackEnabled(enable);
            initRenderMode();
            handler.removeCallbacks(backToRenderWhenDirty);
        }
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        screenSizeRatio = Math.max(width, height) * 1.0f / Math.min(width, height);
        matrixState.setProjectFrustum(-ratio * screenSizeRatio, ratio * screenSizeRatio, -ratio,
                ratio, 1.0f, 800);
        resetLookAt();
        GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    @Override
    protected void onSurfaceCreated() {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
    }
}
