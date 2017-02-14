package bruce.sun.vr.ui;

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
}
