
package bruce.sun.vr.domain;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES30;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import bruce.sun.vr.utils.MatrixState;
import bruce.sun.vr.utils.ShaderUtil;

public class SphereModel implements IVrModel {
    private int mProgram;

    private int muMVPMatrixHandle;

    private int maPositionHandle;

    private int maTexCoorHandle;

    FloatBuffer mVertexBuffer;

    FloatBuffer mTexCoorBuffer;

    ShortBuffer indexBuffer;

    static double NV_PI = 3.14159265358979323846;

    int iCount = 0;

    private int sTextureHandle;

    private MatrixState matrixState;

    public SphereModel(Context context, MatrixState matrixState, float radius) {
        this.matrixState = matrixState;
        creatSphere(radius);

        String vertexShader =
                "#version 300 es\n" +
                        "uniform mat4 uMVPMatrix;" +
                        "in vec3 aPosition;" +
                        "in vec2 aTexCoor;" +
                        "out vec2 vTextureCoord;" +
                        "void main() {" +
                        "   gl_Position = uMVPMatrix * vec4(aPosition,1);" +
                        "   vTextureCoord = aTexCoor;" +
                        "}";

        String fragmentShader =
                "#version 300 es\n" +
                        "#extension GL_OES_EGL_image_external_essl3 : require\n" +
                        "precision mediump float;" +
                        "uniform samplerExternalOES sTexture;" +
                        "in vec2 vTextureCoord;" +
                        "out vec4 fragColor;" +
                        "void main() {" +
                        "   vec4 finalColor=texture(sTexture, vTextureCoord);" +
                        "   fragColor = finalColor;" +
                        "}";

        initShader(vertexShader, fragmentShader);
    }

    public boolean isSemiSphere() {
        return false;
    }

    private void creatSphere(float radius) {
        int segmentCount = 15;
        int hozSegmentCount = segmentCount * 4;
        int verSegmentCount = segmentCount * 2;

        ArrayList<Float> cosTheta = new ArrayList<Float>();
        ArrayList<Float> sinTheta = new ArrayList<Float>();

        double theta = NV_PI / 2;
        double thetaStep = NV_PI / (segmentCount * 2);
        for (int i = 0; i < hozSegmentCount; i++, theta += thetaStep) {
            cosTheta.add((float) Math.cos(theta));
            sinTheta.add((float) Math.sin(theta));
        }
        cosTheta.add(cosTheta.get(0));
        sinTheta.add(sinTheta.get(0));

        double angle = (NV_PI / 2);
        double angleStep = NV_PI / verSegmentCount;

        ArrayList<Float> vertexPos = new ArrayList<Float>();
        ArrayList<Float> vertexTexCoord = new ArrayList<Float>();
        // ArrayList<Float> vertexNormal = new ArrayList<Float>();

        for (int i = 0; i <= verSegmentCount; i++, angle -= angleStep) {
            float t = (float) i / verSegmentCount;
            double radiusInCrossSection;
            float y;

            if (i == 0) {
                radiusInCrossSection = 0;
                y = (float) radius;
            } else if (i == verSegmentCount) {
                radiusInCrossSection = 0;
                y = (float) -radius;
            } else {
                radiusInCrossSection = radius * Math.cos(angle);
                y = (float) (radius * Math.sin(angle));
            }

            for (int j = 0; j <= hozSegmentCount; j++) {
                float s = (float) (hozSegmentCount - j) / hozSegmentCount;
                vertexPos.add((float) (radiusInCrossSection * sinTheta.get(j)));
                vertexPos.add(y);
                vertexPos.add((float) (radiusInCrossSection * cosTheta.get(j)));

                vertexTexCoord.add(s);
                vertexTexCoord.add(t);
            }
        }

        float vertices[] = new float[vertexPos.size()];
        for (int i = 0; i < vertexPos.size(); i++) {
            vertices[i] = vertexPos.get(i);
        }

        float textures[] = new float[vertexTexCoord.size()];
        for (int i = 0; i < vertexTexCoord.size(); i++) {
            textures[i] = vertexTexCoord.get(i);
        }

        ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length * 4);
        vbb.order(ByteOrder.nativeOrder());
        mVertexBuffer = vbb.asFloatBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);

        ByteBuffer tbb = ByteBuffer.allocateDirect(textures.length * 4);
        tbb.order(ByteOrder.nativeOrder());
        mTexCoorBuffer = tbb.asFloatBuffer();
        mTexCoorBuffer.put(textures);
        mTexCoorBuffer.position(0);

        ArrayList<Integer> alIndex = new ArrayList<Integer>();

        for (int row = 0; row < verSegmentCount; row++) {
            for (int col = 0; col < hozSegmentCount; col++) {
                int N10 = (int) ((row + 1) * (hozSegmentCount + 1) + col);
                int N00 = (int) (row * (hozSegmentCount + 1) + col);

                alIndex.add(N00);
                alIndex.add(N10 + 1);
                alIndex.add(N10);

                alIndex.add(N00);
                alIndex.add(N00 + 1);
                alIndex.add(N10 + 1);
            }
        }

        iCount = alIndex.size();
        short indices[] = new short[iCount];
        for (int i = 0; i < iCount; i++) {
            indices[i] = alIndex.get(i).shortValue();
        }

        ByteBuffer ibb = ByteBuffer.allocateDirect(iCount * 2);

        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(indices);
        indexBuffer.position(0);
    }

    private void initShader(String vertexShader, String fragmentShader) {
        mProgram = ShaderUtil.createProgram(vertexShader, fragmentShader);
        maPositionHandle = GLES30.glGetAttribLocation(mProgram, "aPosition");
        maTexCoorHandle = GLES30.glGetAttribLocation(mProgram, "aTexCoor");
        muMVPMatrixHandle = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix");
        sTextureHandle = GLES30.glGetUniformLocation(mProgram, "sTexture");
    }

    @SuppressLint("InlinedApi")
    @Override
    public void drawSelf(int texId) {
        GLES30.glUseProgram(mProgram);
        GLES30.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, matrixState.getFinalMatrix(), 0);
        GLES30.glVertexAttribPointer(maPositionHandle, 3, GLES30.GL_FLOAT, false, 3 * 4,
                mVertexBuffer);
        GLES30.glVertexAttribPointer(maTexCoorHandle, 2, GLES30.GL_FLOAT, false, 2 * 4,
                mTexCoorBuffer);
        GLES30.glEnableVertexAttribArray(maPositionHandle);
        GLES30.glEnableVertexAttribArray(maTexCoorHandle);

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId);
        GLES30.glUniform1i(sTextureHandle, 0);

        GLES30.glDrawElements(GLES30.GL_TRIANGLES, iCount, GLES30.GL_UNSIGNED_SHORT, indexBuffer);
    }
}
