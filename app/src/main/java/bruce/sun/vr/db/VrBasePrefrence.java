
package bruce.sun.vr.db;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

public abstract class VrBasePrefrence {

    private SharedPreferences mPrefs;

    protected Context mContext;

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    protected VrBasePrefrence(Context context, String prefrenceName) {
        mContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mPrefs = mContext.getSharedPreferences(prefrenceName, Context.MODE_MULTI_PROCESS);
        } else {
            mPrefs = mContext.getSharedPreferences(prefrenceName, Context.MODE_WORLD_WRITEABLE);
        }
    }

    protected SharedPreferences getPrefs() {
        return mPrefs;
    }

    public int getInt(String key) {
        return getPrefs().getInt(key, 0);
    }

    public int getInt(String key, int defaultValue) {
        return getPrefs().getInt(key, defaultValue);
    }

    public void setInt(String key, int value) {
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public String getString(String key) {
        return getPrefs().getString(key, "");
    }

    protected String getString(String key, String defaultValue) {
        return getPrefs().getString(key, defaultValue);
    }

    public void setString(String key, String value) {
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putString(key, value);
        editor.commit();
    }

    public boolean getBoolean(String key) {
        return getPrefs().getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean value) {
        return getPrefs().getBoolean(key, value);
    }

    public void setBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    protected long getLong(String key) {
        return getPrefs().getLong(key, 0);
    }

    protected long getLong(String key, long value) {
        return getPrefs().getLong(key, value);
    }

    protected float getFloat(String key) {
        return getPrefs().getFloat(key, 0);
    }

    protected float getFloat(String key, float value) {
        return getPrefs().getFloat(key, value);
    }

    protected void setFloat(String key, float value) {
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    protected void setLong(String key, long value) {
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putLong(key, value);
        editor.commit();
    }
}
