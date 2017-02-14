
package bruce.sun.vr.render;

import android.content.Context;
import android.opengl.GLES20;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.baofeng.mojing.MojingSurfaceView;

import java.nio.IntBuffer;

public class VrRender extends BaseRenderer {

    private int g_frameBufferObject;

    private float fFOV = 96; // 因为这一版暂时未做魔镜型号选择功能，因此默认设置为魔镜4的FOV

    private long detectTimePrev;

    private int detectDrawsPrev;

    private Runnable detectRunnable;

    private boolean detectTimeWarpAndMultiThread;

    private static final int MIN_TIMEWARP_AND_MULTITHREAD_FPS = 35;

    public VrRender(Context context, Handler handler) {
        super(context, handler);
    }

    public void setDetectTimeWarpAndMultiThread(boolean isDetect) {
        detectTimeWarpAndMultiThread = isDetect;
    }

    @Override
    protected void onSurfaceChanged(int width, int height) {
        com.baofeng.mojing.MojingSDK.SetCenterLine(0, 255, 255, 255, 255);
        // com.baofeng.mojing.MojingSDK.LogTrace("EnterMojingWorld");
        // fFOV = com.baofeng.mojing.MojingSDK.GetMojingWorldFOV();
        MojingSurfaceView.OnSurfaceChanged(width, height);
        ratio = (float) Math.tan(Math.toRadians(fFOV / 2)) * 1.0f;
        Log.i(TAG, String.format("ratio is %f", ratio));
        matrixState.setProjectFrustum(-ratio, ratio, -ratio, ratio, 1.0f, 800);
        lookBack();
        com.baofeng.mojing.MojingSDK.GetGlassesSeparationInPix();
    }

    @Override
    protected void onSurfaceCreated() {
        g_frameBufferObject = generateFrameBufferObject();
        com.baofeng.mojing.MojingSDK.SetCenterLine(0, 255, 255, 255, 255);
        // com.baofeng.mojing.MojingSDK.LogTrace("EnterMojingWorld");
    }

    private static int generateFrameBufferObject() {
        IntBuffer framebuffer = IntBuffer.allocate(1);
        GLES20.glGenFramebuffers(1, framebuffer);
        return framebuffer.get(0);
    }

    @Override
    protected void renderToTexture() {
        int EyeTex[] = {
                0, 0
        };
        float Camera[] = {
                -0.1f, 0.1f
        };
        for (int iEye = 0; iEye < 2; iEye++) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, g_frameBufferObject);
            com.baofeng.mojing.EyeTextureParameter EyeTexture = com.baofeng.mojing.MojingSDK
                    .GetEyeTextureParameter(iEye + 1);
            EyeTex[iEye] = EyeTexture.m_EyeTexID;
            if (EyeTex[iEye] != 0 && GLES20.glIsTexture(EyeTex[iEye])) {
                GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                        GLES20.GL_TEXTURE_2D, EyeTexture.m_EyeTexID, 0);
                GLES20.glClearColor(0, 0, 0, 1);
                GLES20.glViewport(0, 0, EyeTexture.m_Width, EyeTexture.m_Height);
                GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
                int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
                if (status == GLES20.GL_FRAMEBUFFER_COMPLETE) {
                    GLES20.glClearColor(0, 0, 0, 1);
                    GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

                    matrixState.pushMatrix();
                    matrixState.setCamera(Camera[iEye], 0, 0, 0f, 0.0f, -0.1f, 0f, 1.0f, 0.0f);
                    float[] fM = new float[16];
                    com.baofeng.mojing.MojingSDK.getLastHeadView(fM);
                    matrixState.setViewMatrix(fM);
                    rotateByTouch();
                    drawSphere();
                    matrixState.popMatrix();
                }
            }
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, 0, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
        com.baofeng.mojing.MojingSDK.DrawTexture(EyeTex[0], EyeTex[1]);
    }

    @Override
    protected void onSwitchModeEnd() {
        super.onSwitchModeEnd();
        if (!detectTimeWarpAndMultiThread) {
            return;
        }
        // 避免重复检测
        detectTimeWarpAndMultiThread = false;
        getFPS();
        if (detectRunnable == null) {
            detectRunnable = new Runnable() {
                @Override
                public void run() {
                    if (listener == null) {
                        return;
                    }
                    long now = System.currentTimeMillis();
                    long span = now - detectTimePrev;
                    float fps = 1000.0f * (draws - detectDrawsPrev) / span;
                    if (fps >= MIN_TIMEWARP_AND_MULTITHREAD_FPS) {
                        listener.onDetectTimeWarpAndMultiThread(true);
                    } else {
                        listener.onDetectTimeWarpAndMultiThread(false);
                    }
                }
            };
        }
        handler.removeCallbacks(detectRunnable);
        detectTimePrev = System.currentTimeMillis();
        detectDrawsPrev = draws;
        handler.postDelayed(detectRunnable, 5000);
    }

    @Override
    public void onPause() {
        if (detectRunnable != null) {
            handler.removeCallbacks(detectRunnable);
        }
    }

    @Override
    protected boolean applyTouchY() {
        return false;
    }
}
