package bruce.sun.vr.utils;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Created by mazejia on 2016/3/11.
 */
public class VrUtils {

    public static String getStringTime(long position) {
        SimpleDateFormat fmPlayTime;
        if (position <= 0) {
            return "00:00";
        }

        long lCurrentPosition = position / 1000;
        long lHours = lCurrentPosition / 3600;

        if (lHours > 0)
            fmPlayTime = new SimpleDateFormat("HH:mm:ss");
        else
            fmPlayTime = new SimpleDateFormat("mm:ss");

        fmPlayTime.setTimeZone(TimeZone.getTimeZone("GMT"));
        return fmPlayTime.format(position);
    }
    
    public static void setEnabledAll(View v, boolean enabled) {
        v.setEnabled(enabled);
        v.setFocusable(enabled);

        if(v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++)
                setEnabledAll(vg.getChildAt(i), enabled);
        }
    }

    /**
     * 根据手机的分辨率从px(像素) 的单位 转成为dp
     */
    public static int dp2Px(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static float getDistance(MotionEvent event) {
        float dx = event.getX(1) - event.getX(0);
        float dy = event.getY(1) - event.getY(0);
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance;
    }
}
