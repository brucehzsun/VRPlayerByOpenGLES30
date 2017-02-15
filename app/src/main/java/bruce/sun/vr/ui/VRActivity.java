
package bruce.sun.vr.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.ViewFlipper;

import bruce.sun.vr.R;
import bruce.sun.vr.render.BaseRenderer;
import bruce.sun.vr.surface.VRGLSurfaceView;
import bruce.sun.vr.utils.VrUtils;


@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class VRActivity extends Activity implements OnTouchListener, IVRPlayView {

    private static final String TAG = "VRActivity";

    private float touchPrevX;

    private float touchPrevY;


    private static final float sDensity = Resources.getSystem().getDisplayMetrics().density;

    private static final float sDamping = 0.2f;


    int touchEventMode;

    private static final int TOUCH_EVENT_MODE_NORMAL = 1;

    private static final int TOUCH_EVENT_MODE_ZOOM = 2;

    private float zoomStartDistance;

    private int MIN_MOVE_SPAN; // 区分滑屏和点击的距离阈值


    private String playUrl;
    private IVRPlayPresenter presenter;
    private VRGLSurfaceView mySurfaceView;
    private int touchDeltaX;
    private int touchDeltaY;

    float touchStartX = 0;

    float touchStartY = 0;
//    private MojingSurfaceView mojingSurfaceView;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.d(TAG, "onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_vr);
        presenter = new VRPlayPresenter(this);
        MIN_MOVE_SPAN = VrUtils.dp2Px(this, 20);
        initView();
        playUrl = getIntent().getStringExtra("PlayUrl");
        presenter.doPlay(playUrl);
    }

    private void initView() {
//        flipper = (ViewFlipper) findViewById(R.id.flipper);
//        mojingSurfaceView = (MojingSurfaceView) flipper.getChildAt(Constant.ID_MOJING);
        mySurfaceView = (VRGLSurfaceView) findViewById(R.id.mySurfaceView);//.getChildAt(Constant.ID_MY);
        mySurfaceView.setOnTouchListener(this);
        presenter.setGLSurfaceView(mySurfaceView);

//        MojingSDK.Init(this);
//        selectSurfaceView(ID_MY, true);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        presenter.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        presenter.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        presenter.onDestroy();
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                touchEventMode = TOUCH_EVENT_MODE_NORMAL;
                touchStartX = event.getX();
                touchStartY = event.getY();
                break;

            case MotionEvent.ACTION_UP:
//                getRenderer().scaleEnd();
                float xd = Math.abs(event.getX() - touchStartX);
                float yd = Math.abs(event.getY() - touchStartY);
                if (xd < MIN_MOVE_SPAN && yd < MIN_MOVE_SPAN) { // 滑动距离小于阈值,鉴定为单击事件
                    if (touchEventMode == TOUCH_EVENT_MODE_NORMAL) {
//                        handler.removeMessages(UPDATE_VIDEO_TITLEBAR_ISSHOW);
//                        handler.sendEmptyMessageDelayed(UPDATE_VIDEO_TITLEBAR_ISSHOW, 300);
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (touchEventMode == TOUCH_EVENT_MODE_ZOOM) {
                    if (event.getPointerCount() == 1) {
                        break;
                    }
                    float zoomEndDistance = VrUtils.getDistance(event);
                    if (zoomEndDistance > 10f) {
                        float scale = zoomEndDistance / zoomStartDistance;
//                        getRenderer().setScale(scale, true);
                    }
                } else {
                    float deltaX = x - touchPrevX;
                    float deltaY = y - touchPrevY;
                    // 为了避免滑屏导致头晕，每次只允许在一个方向上滑动，即：斜着滑屏时，取滑动距离最大的方向
                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
                        touchDeltaX += deltaX / sDensity * sDamping;
                    } else {
                        touchDeltaY += deltaY / sDensity * sDamping;
                    }
//                    getRenderer().setTouchData(touchDeltaX, touchDeltaY, true);
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                touchEventMode = TOUCH_EVENT_MODE_ZOOM;
                zoomStartDistance = VrUtils.getDistance(event);
                break;
        }
        touchPrevX = x;
        touchPrevY = y;
        return true;
    }


    // 归零
    private void resetTouchPosition() {
        touchDeltaX = 0;
        touchDeltaY = 0;
        touchPrevX = 0;
        touchPrevY = 0;
        touchEventMode = 0;
//        getRenderer().setTouchData(0, 0, false);
    }

}
