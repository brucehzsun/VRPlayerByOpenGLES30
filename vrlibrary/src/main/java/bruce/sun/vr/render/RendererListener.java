
package bruce.sun.vr.render;

public interface RendererListener {
    public void onRendererInit();

    public void onRendererReady();

    public void onSwitchModeStart();

    public void onSwitchModeEnd();

    public void onDetectTimeWarpAndMultiThread(boolean isSupported);
}
