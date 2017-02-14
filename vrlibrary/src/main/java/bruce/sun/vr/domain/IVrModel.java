package bruce.sun.vr.domain;

public interface IVrModel {

    public void drawSelf(int texId);

    public interface IVrMode {
        int MODE_NONE = 0;

        int MODE_TOUCH = 1;

        int MODE_GYRO = 2;

        int MODE_GLASSES = 3;
    }

    public interface IVrModelType {
        int TYPE_NONE = 0;

        int TYPE_SPHERE = 1; // 球(默认值)

        int TYPE_SEMI_SPHERE = 2; // 半球

        int TYPE_SKYBOX = 3; // 六面体
    }

}
