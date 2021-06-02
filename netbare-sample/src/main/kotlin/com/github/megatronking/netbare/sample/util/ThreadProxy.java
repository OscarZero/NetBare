package com.github.megatronking.netbare.sample.util;

import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


public class ThreadProxy {

    private final Executor executor;

    static class InnerClass {
        static ThreadProxy instance = new ThreadProxy();
    }

    private ThreadProxy() {

        executor = new ThreadPoolExecutor(1, 4,
                10L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread thread = new Thread(r);
                thread.setName("ThreadProxy");
                return thread;
            }
        });
    }
    public void execute(Runnable run){
        executor.execute(run);
    }
    public static ThreadProxy getInstance(){
        return InnerClass.instance;
    }

}
