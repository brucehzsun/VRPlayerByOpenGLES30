package bruce.sun.vr.surface;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;

public class VRGLSurfaceView extends GLSurfaceView implements SensorEventListener {
    private static final String TAG = "VRGLSurfaceView";

    private SphereRender mRenderer;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

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

        mSensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);


    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[SensorManager.DATA_X];
        float y = event.values[SensorManager.DATA_Y];
        float z = event.values[SensorManager.DATA_Z];

        float upx = -y;
        float upy = x;
        float upz = z;
        Log.d(TAG, "onSensorChanged x=" + upx + "," + "y=" + upy + "," + "z=" + upz);
//        mRenderer.setSensorChanged(upx, upy);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public Surface getSurface() {
        return mRenderer.getSurface();
    }

    public void setTouchData(int touchDeltaX, int touchDeltaY, boolean b) {
        mRenderer.setTouchData(touchDeltaX, touchDeltaY, b);
    }
}
