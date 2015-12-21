package de.gebatzens.sia;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

public class LifecycleHandler implements Application.ActivityLifecycleCallbacks {
    private int resumed;
    private int paused;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
        resumed++;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        paused++;
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    public boolean isAppInForeground() {
        return resumed > paused;
    }
}