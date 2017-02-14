
package bruce.sun.vr.db;

import android.content.Context;

public class VrPreferences extends VrBasePrefrence {
    private static final String Preference_Storm = "AndroidVrPrefs";

    private static VrPreferences m_stInstance;

    public static synchronized VrPreferences getInstance(Context context) {
        if (m_stInstance == null)
            m_stInstance = new VrPreferences(context, Preference_Storm);

        return m_stInstance;
    }

    private VrPreferences(Context context, String name) {
        super(context, name);
    }

    public boolean isFirstTouchMode() {
        return getBoolean("first_touch_mode", true);
    }

    public void setFirstTouchMode(boolean flag) {
        setBoolean("first_touch_mode", flag);
    }

    public boolean isFirstGoryMode() {
        return getBoolean("first_gory_mode", true);
    }

    public void setFirstGoryMode(boolean flag) {
        setBoolean("first_gory_mode", flag);
    }

    public String getUuid() {
        return getString("uuid", "");
    }

    public void setUuid(String uuid) {
        setString("uuid", uuid);
    }

    public String getGuid() {
        return getString("guid", "");
    }

    public void setGuid(String guid) {
        setString("guid", guid);
    }

    public String getOgid() {
        return getString("ogid", "");
    }

    public void setOgid(String ogid) {
        setString("ogid", ogid);
    }

    public String getSupportTimeWarpAndMultiThread() {
        return getString("time_warp_and_multi_thread", "");
    }

    public void setSupportTimeWarpAndMultiThread(String support) {
        setString("time_warp_and_multi_thread", support);
    }
}
