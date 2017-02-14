package com.baofeng.mojing;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.InputDevice;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MojingSDK {
    public static int SENSOR_ERROR_NOERROR = 0;

    public static int SENSOR_ERROR_NoMag = 1;

    public static int SENSOR_ERROR_NoGryo = 4;

    public static int SENSOR_ERROR_GryoTooSlow = 8;

    public static int SENSOR_ERROR_NoAccel = 16;

    public static int SENSOR_ERROR_AccelTooSlow = 32;

    public static int EYE_TEXTURE_TYPE_LEFT = 1;

    public static int EYE_TEXTURE_TYPE_RIGHT = 2;

    public static int EYE_TEXTURE_TYPE_BOTH = 3;

    private static Timer g_DeviceTimer;

    static {
        System.loadLibrary("mojing");
    }

    private static boolean m_inited = false;

    public static boolean Init(Context context) {
        if (!m_inited) {
            String path = exportFromAssetsFile(context);
            String appName = getApplicationName(context);
            String packageName = getAppPackageName(context);
            String userID = getUserID(context);
            String channelID = getCustomMetaData(context, "DEVELOPER_CHANNEL_ID");
            String appID = getCustomMetaData(context, "DEVELOPER_APP_ID");
            String appKey = getCustomMetaData(context, "DEVELOPER_APP_KEY");
            String merchantID = getCustomMetaData(context, "DEVELOPER_MERCHANT_ID");
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            m_inited = true;
            return Init(merchantID, appID, appKey, appName, packageName, userID, channelID,
                    dm.widthPixels, dm.heightPixels, dm.xdpi, dm.ydpi, path);
        }
        return true;
    }

    public static EyeTextureParameter GetEyeTextureParameter(int eyeTextureType) {
        EyeTextureParameter Ret = new EyeTextureParameter();
        int[] Parameter = {
                0, 0, 0
        };
        Ret.m_EyeTexID = GetEyeTexture(eyeTextureType, Parameter);
        Ret.m_EyeTexType = eyeTextureType;
        Ret.m_Width = Parameter[0];
        Ret.m_Height = Parameter[1];
        Ret.m_Format = Parameter[2];
        return Ret;
    }

    private static String exportFromAssetsFile(Context context) {
        String result = null;
        File CacheDir = context.getCacheDir();
        String cachePath = CacheDir.getPath();
        result = cachePath;
        File cachePathFile = new File(cachePath);
        if (cachePathFile != null) {
            if ((!cachePathFile.exists()) || (!cachePathFile.isDirectory())) {
                cachePathFile.mkdir();
            }
        }
        try {
            Resources r = context.getResources();
            String[] ProfileList = r.getAssets().list("MojingSDK");
            LogTrace("Find " + ProfileList.length + " file(s) in assets/MojingSDK");
            if (ProfileList.length > 0) {
                AssetManager assetManager = r.getAssets();
                for (String filename : ProfileList) {
                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = assetManager.open("MojingSDK/" + filename);
                        File outFile = new File(CacheDir, filename);
                        out = new FileOutputStream(outFile);
                        copyFile(in, out);
                        in.close();
                        in = null;
                        out.flush();
                        out.close();
                        out = null;
                        LogTrace("copy " + filename + " to CacheDir");
                    } catch (IOException e) {
                        LogError("Failed to copy asset file: " + filename + e.toString());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte['?'];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private static String getCPUSerial() {
        String str = "";
        String strCPU = "";
        String cpuAddress = "0000000000000000";
        try {
            Process pp = Runtime.getRuntime().exec("cat /proc/cpuinfo");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (int i = 1; i < 100; i++) {
                str = input.readLine();
                if (str == null) {
                    break;
                }
                if (str.indexOf("Serial") > -1) {
                    strCPU = str.substring(str.indexOf(":") + 1, str.length());
                    cpuAddress = strCPU.trim();
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return cpuAddress;
    }

    public static String getSerialNumber() {
        String serial = "0000000000000000";
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");

            Method get = c.getMethod("get", new Class[]{
                    String.class
            });

            serial = (String) get.invoke(c, new Object[]{
                    "ro.serialno"
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serial;
    }

    private static String getUserID(Context context) {
        String uniqueId = "UNKNOWN";
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            String tmDevice = "" + tm.getDeviceId();
            String androidId = ""
                    + Settings.Secure.getString(context.getContentResolver(), "android_id");

            String serial = getSerialNumber();
            UUID deviceUuid = new UUID(androidId.hashCode(), tmDevice.hashCode() << 32
                    | serial.hashCode());
            uniqueId = deviceUuid.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uniqueId;
    }

    public static String getCustomMetaData(Context context, String metadataKey) {
        String metadataValue = "UNKNOWN";
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            Object obj = applicationInfo.metaData.get(metadataKey);
            if (obj != null) {
                metadataValue = obj.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            metadataKey = "";
        }
        return metadataValue;
    }

    private static String getAppPackageName(Context context) {
        String packageName = "UNKNOWN";
        try {
            String pkName = context.getPackageName();

            packageName = pkName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return packageName;
    }

    private static String getApplicationName(Context context) {
        String applicationName = "UNKNOWN";
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = context.getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(
                    context.getApplicationInfo().packageName, 0);
            applicationName = (String) packageManager.getApplicationLabel(applicationInfo);

            PackageInfo packInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            String version = packInfo.versionName;
            int versionCode = packInfo.versionCode;
            if (version != null) {
                applicationName = applicationName + " " + version + "(" + versionCode + ")";
            } else {
                applicationName = applicationName + " (" + versionCode + ")";
            }
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        return applicationName;
    }

    public static void SetCenterLine(int iWidth) {
        SetCenterLine(iWidth, 255, 255, 255, 255);
    }

    public static void onNativeActivePause() {
        if (g_DeviceTimer != null) {
            g_DeviceTimer.cancel();
        }
        g_DeviceTimer = null;
    }

    private static Object g_DeviceTimerSync = new Object();

    public static void onNativeActiveResume() {
        if (g_DeviceTimer == null)
            g_DeviceTimer = new Timer();
        g_DeviceTimer.schedule(new TimerTask() {

            public void run() {
                synchronized (MojingSDK.g_DeviceTimerSync) {
                    if (MojingSDK.g_DeviceTimer != null) {
                        MojingSDK.NativeBeginUpdateDeviceMap();
                        int temp[] = InputDevice.getDeviceIds();
                        for (int i = 0; i < temp.length; i++) {
                            InputDevice inputDevice = InputDevice.getDevice(temp[i]);
                            int deviceID = inputDevice.getId();
                            String strDeviceName = inputDevice.getName();
                            MojingSDK.NativeAddDeviceToMap(deviceID, strDeviceName);
                        }

                        MojingSDK.NativeEndUpdateDeviceMap();
                    }
                }
            }

        }, 0L, 5000L);
    }

    private static void Log(int logLevel, String sInfo) {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();

        String tag = "[" + stacktrace[4].getMethodName() + "] " + sInfo;
        String log = "file:" + stacktrace[4].getFileName() + ",line:"
                + stacktrace[4].getLineNumber();
        switch (logLevel) {
            case 40000:
                Log.e(tag, log);
                break;

            case 30000:
                Log.w(tag, log);
                break;

            case 10000:
                Log.d(tag, log);
                break;

            case 0:
                Log.i(tag, log);
                break;
        }
    }

    public static void LogError(String sInfo) {
        Log(40000, sInfo);
    }

    public static void LogWarning(String sInfo) {
        Log(30000, sInfo);
    }

    public static void LogDebug(String sInfo) {
        Log(10000, sInfo);
    }

    public static void LogTrace(String sInfo) {
        Log(0, sInfo);
    }

    public static native int GetSystemIntProperty(String paramString, int paramInt);

    private static native boolean Init(String paramString1, String paramString2,
                                       String paramString3, String paramString4, String paramString5, String paramString6,
                                       String paramString7, int paramInt1, int paramInt2, float paramFloat1,
                                       float paramFloat2, String paramString8);

    public static native boolean AppInit(String paramString1, String paramString2);

    public static native void AppExit();

    public static native boolean AppResume(String paramString);

    public static native void AppPause();

    public static native void AppPageStart(String paramString);

    public static native void AppPageEnd(String paramString);

    public static native void AppSetEvent(String paramString1, String paramString2,
                                          String paramString3, float paramFloat1, String paramString4, float paramFloat2);

    public static native void ReportLog(int paramInt, String paramString1, String paramString2);

    public static native void AppSetContinueInterval(int paramInt);

    public static native void OnKeyEvent(String paramString, int paramInt, boolean paramBoolean);

    public static native void OnAxisEvent(String paramString, int paramInt, float paramFloat);

    public static native boolean StartTracker(int paramInt);

    public static native int CheckSensors();

    public static native void ResetSensorOrientation();

    public static native void ResetTracker();

    public static native float IsTrackerCalibrated();

    public static native boolean StartTrackerCalibration();

    public static native void getLastHeadView(float[] paramArrayOfFloat);

    public static native int getPredictionHeadView(float[] paramArrayOfFloat, double paramDouble);

    public static native void getLastHeadEulerAngles(float[] paramArrayOfFloat);

    public static native void getLastHeadQuarternion(float[] paramArrayOfFloat);

    public static native void StopTracker();

    public static native boolean DrawTexture(int paramInt1, int paramInt2);

    public static native boolean DrawTextureWithOverlay(int paramInt1, int paramInt2,
                                                        int paramInt3, int paramInt4);

    public static native void SetOverlayPosition(float paramFloat1, float paramFloat2,
                                                 float paramFloat3, float paramFloat4);

    public static native float GetMojingWorldFOV();

    private static native int GetEyeTexture(int paramInt, int[] paramArrayOfInt);

    public static native float GetGlassesSeparationInPix();

    public static native void SetImageYOffset(float paramFloat);

    public static native void SetCenterLine(int paramInt1, int paramInt2, int paramInt3,
                                            int paramInt4, int paramInt5);

    public static native String GetManufacturerList(String paramString);

    public static native String GetProductList(String paramString1, String paramString2);

    public static native String GetGlassList(String paramString1, String paramString2);

    public static native String GetGlassInfo(String paramString1, String paramString2);

    public static native String GenerationGlassKey(String paramString1, String paramString2);

    public static native boolean SetDefaultMojingWorld(String paramString);

    public static native String GetDefaultMojingWorld(String paramString);

    public static native String GetLastMojingWorld(String paramString);

    public static native String GetSDKVersion();

    public static native boolean GetInitSDK();

    public static native boolean GetStartTracker();

    public static native boolean GetInMojingWorld();

    public static native String GetGlasses();

    public static native float GetScreenPPI();

    public static native float GetDistortionR(String paramString);

    public static native String GetJoystickFileName();

    public static native void NativeSetMojing2Number(int paramInt);

    private static native void NativeCleanDeviceMap();

    private static native void NativeBeginUpdateDeviceMap();

    private static native void NativeEndUpdateDeviceMap();

    private static native void NativeAddDeviceToMap(int paramInt, String paramString);

    public static native void FuncTest();
}
