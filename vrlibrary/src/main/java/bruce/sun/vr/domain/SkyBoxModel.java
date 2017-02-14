package bruce.sun.vr.domain;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import bruce.sun.vr.utils.MatrixState;
import bruce.sun.vr.utils.ShaderUtil;

public class SkyBoxModel implements IVrModel {
    int mProgram;            //Shader的运行脚本ID
    int muMVPMatrixHandle;    //Shader的参数，用于MVP矩阵
    int maPositionHandle;    //
    int maTexCoorHandle;

    FloatBuffer mVertexBuffer;
    FloatBuffer mTexCoorBuffer;
    ShortBuffer indexBuffer;

    int miIndexCount = 36;

    protected int sTextureHandle;
    protected MatrixState matrixState;

    public SkyBoxModel(MatrixState matrixState, float fBoxSize, float fSafeEdge) {
        this.matrixState = matrixState;
        creatSkyBox(fBoxSize, fSafeEdge);

        String vertexShader =
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec3 aPosition;" +
                        "attribute vec2 aTexCoor;" +
                        "varying vec2 vTextureCoord;" +
                        "void main()" +
                        "{" +
                        "   gl_Position = uMVPMatrix * vec4(aPosition,1);" +
                        "   vTextureCoord = aTexCoor;" +
                        "}";

        String fragmentShader =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;" +
                        "uniform samplerExternalOES sTexture;" +
                        "varying vec2 vTextureCoord;" +
                        "void main()" +
                        "{" +
                        "   vec4 finalColor=texture2D(sTexture, vTextureCoord);" +
                        "   gl_FragColor = finalColor;" +
                        "}";

        initShader(vertexShader, fragmentShader);
    }

    public void creatSkyBox(float fBoxSize, float fSafeEdge) {
        // 现在只考虑一张图帖6面的做法，暂时不考虑其他的
        /*----------------------------------------
         *    R    |     L    |      T
    	 * ---------------------------------------
    	 *    BT   |     F    |      BA  
    	 * ---------------------------------------
    	 */
        // 1 六面体空间坐标
        float fBoxHalfSize = fBoxSize / 2.0f;
        // 所有坐标都以位于(000),面朝平面方向为准,顺时针添加
        float[] VertexBase = {	/*右面*/
                +1, +1, -1,// 右前上0
                +1, +1, +1,// 右后上1
                +1, -1, +1,// 右后下2
                +1, -1, -1,// 右前下3
								/*左面*/
                -1, +1, +1,// 左后上 4
                -1, +1, -1,// 左前上5
                -1, -1, -1,// 左前下6
                -1, -1, +1,// 左后下7
								
								/*上面*/
                -1, +1, +1,//左后上8
                +1, +1, +1,//右后上9
                +1, +1, -1,//右前上10
                -1, +1, -1,//左前上 11
								/*下面*/
                -1, -1, -1,//左前下12
                +1, -1, -1,//右前下13
                +1, -1, +1,//右后下14
                -1, -1, +1,//左后下15
    							/*前面*/
                -1, 1, -1, // 左前上 - 16
                +1, 1, -1, // 右前上 - 17
                +1, -1, -1, // 右前下 - 18
                -1, -1, -1, // 左前下 - 19
    							/*后面*/
                +1, 1, +1, // 右后上 - 20
                -1, 1, +1, // 左后上 - 21
                -1, -1, +1,  // 左后下 - 22
                +1, -1, +1 // 右后下 - 23
        };
        float[] Vertex = new float[VertexBase.length];
        for (int iVertexIndex = 0; iVertexIndex < VertexBase.length; iVertexIndex++) {
            Vertex[iVertexIndex] = VertexBase[iVertexIndex] * fBoxHalfSize;
        }
        ByteBuffer vbb = ByteBuffer.allocateDirect(6 * 12 * 4);
        vbb.order(ByteOrder.nativeOrder());
        mVertexBuffer = vbb.asFloatBuffer();
        mVertexBuffer.put(Vertex);
        mVertexBuffer.position(0);

        float SubImageSizeWidth = 1.0f / 3.0f;
        float SubImageSizeHeight = 1.0f / 2.0f;
        // 先假定为3X2的坐标网格
        float UV[] = {
    			/*右面*/
                0, 0,
                1, 0,
                1, 1,
                0, 1,
				/*左面*/
                1, 0,
                2, 0,
                2, 1,
                1, 1,
				/*上面 */
                2, 0,
                3, 0,
                3, 1,
                2, 1,
				/*下面 */
                0, 1,
                1, 1,
                1, 2,
                0, 2,
				/*前面*/
                1, 1,
                2, 1,
                2, 2,
                1, 2,
				/*后面*/
                2, 1,
                3, 1,
                3, 2,
                2, 2
        };

        for (int Said = 0; Said < 6; Said++) {
            int iIndexBase = Said * 4 * 2;
            // 左上
            UV[iIndexBase + 0] = UV[iIndexBase + 0] * SubImageSizeWidth + fSafeEdge;
            UV[iIndexBase + 1] = UV[iIndexBase + 1] * SubImageSizeHeight + fSafeEdge;
            // 右上
            UV[iIndexBase + 2] = UV[iIndexBase + 2] * SubImageSizeWidth - fSafeEdge;
            UV[iIndexBase + 3] = UV[iIndexBase + 3] * SubImageSizeHeight + fSafeEdge;
            // 右下
            UV[iIndexBase + 4] = UV[iIndexBase + 4] * SubImageSizeWidth - fSafeEdge;
            UV[iIndexBase + 5] = UV[iIndexBase + 5] * SubImageSizeHeight - fSafeEdge;
            // 左下
            UV[iIndexBase + 6] = UV[iIndexBase + 6] * SubImageSizeWidth + fSafeEdge;
            UV[iIndexBase + 7] = UV[iIndexBase + 7] * SubImageSizeHeight - fSafeEdge;
        }
        ByteBuffer tbb = ByteBuffer.allocateDirect(48 * 4);
        tbb.order(ByteOrder.nativeOrder());
        mTexCoorBuffer = tbb.asFloatBuffer();
        mTexCoorBuffer.put(UV);
        mTexCoorBuffer.position(0);
        // 3 六面体索引
        short alIndex[] = {
                // 右面
                0, 2, 3,
                0, 1, 2,
                // 左面
                4, 6, 7,
                4, 5, 6,

                // 上面
                8, 10, 11,
                8, 9, 10,

                // 下面
                12, 14, 15,
                12, 13, 14,
                // 前面
                16, 18, 19,
                16, 17, 18,
                // 后面
                20, 22, 23,
                20, 21, 22
        };
        ByteBuffer ibb = ByteBuffer.allocateDirect(miIndexCount * 2);
        ibb.order(ByteOrder.nativeOrder());
        indexBuffer = ibb.asShortBuffer();
        indexBuffer.put(alIndex);
        indexBuffer.position(0);
    }

    public void initShader(String vertexShader, String fragmentShader) {
        mProgram = ShaderUtil.createProgram(vertexShader, fragmentShader);
        maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        maTexCoorHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoor");
        muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        sTextureHandle = GLES20.glGetUniformLocation(mProgram, "sTexture");
    }

    @Override
    public void drawSelf(int texId) {
        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, matrixState.getFinalMatrix(), 0);
        GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 3 * 4,
                mVertexBuffer);
        GLES20.glVertexAttribPointer(maTexCoorHandle, 2, GLES20.GL_FLOAT, false, 2 * 4,
                mTexCoorBuffer);

        GLES20.glEnableVertexAttribArray(maPositionHandle);
        GLES20.glEnableVertexAttribArray(maTexCoorHandle);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId);
        GLES20.glUniform1i(sTextureHandle, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, miIndexCount, GLES20.GL_UNSIGNED_SHORT, indexBuffer);
    }
}

