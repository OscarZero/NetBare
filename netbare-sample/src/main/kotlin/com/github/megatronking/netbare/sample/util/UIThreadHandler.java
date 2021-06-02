package com.github.megatronking.netbare.sample.util;

import android.os.Handler;
import android.os.Looper;

public class UIThreadHandler {
    private static Handler sUiHandler = new Handler(Looper.getMainLooper());

    private static Object sToken = new Object();

    public final static boolean post(Runnable r) {
        if (sUiHandler == null)
            return false;
        return sUiHandler.post(r);
    }

    public final static boolean postDelayed(Runnable r, long delayMillis) {
        if (sUiHandler == null)
            return false;

        return sUiHandler.postDelayed(r, delayMillis);
    }

    public final static Handler getsUiHandler() {
        return sUiHandler;
    }

    public final static boolean postOnceDelayed(Runnable r, long delayMillis) {
        if (sUiHandler == null)
            return false;
        sUiHandler.removeCallbacks(r);
        return sUiHandler.postDelayed(r, delayMillis);
    }

    public static void removeCallbacks(Runnable runnable) {
        if (sUiHandler == null)
            return;
        sUiHandler.removeCallbacks(runnable);
    }
}
