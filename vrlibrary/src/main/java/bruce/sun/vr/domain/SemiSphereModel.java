
package bruce.sun.vr.domain;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import bruce.sun.vr.utils.MatrixState;

public class SemiSphereModel extends SphereModel {

    public SemiSphereModel(MatrixState matrixState, float radius) {
        super(matrixState, radius);
    }

    public boolean isSemiSphere() {
        return true;
    }

    public void creatSphere(float radius) {
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

            for (int j = 0; j <= hozSegmentCount / 2; j++) {
                float s = (float) (hozSegmentCount - j) / (hozSegmentCount);
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
            for (int col = 0; col < hozSegmentCount / 2; col++) {
                int N10 = (int) ((row + 1) * (hozSegmentCount / 2 + 1) + col);
                int N00 = (int) (row * (hozSegmentCount / 2 + 1) + col);

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

}
