package bruce.sun.vr.surface;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Surface;

public class VRGLSurfaceView extends GLSurfaceView {

    private SphereRender mRenderer;

    public VRGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public VRGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        this.setEGLContextClientVersion(3); //设置使用OPENGL ES3.0
        mRenderer = new SphereRender(this);
        setRenderer(mRenderer);                //设置渲染器
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);//设置渲染模式为主动渲染
    }

    public Surface getSurface() {
        return mRenderer.getSurface();
    }
}
