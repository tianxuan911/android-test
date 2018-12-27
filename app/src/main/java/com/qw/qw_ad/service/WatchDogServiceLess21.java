package com.qw.qw_ad.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.qw.qw_ad.utils.WatchDogUtil;

public class WatchDogServiceLess21 extends Service {
    private static boolean IS_WATCH_DOG_RUNNING = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        keepAppAlive();
        return result;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void keepAppAlive() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if(IS_WATCH_DOG_RUNNING){
                    return;
                }
                IS_WATCH_DOG_RUNNING = true;
                new WatchDogUtil(getApplicationContext()).runApp();
                IS_WATCH_DOG_RUNNING = false;
            }
        });

        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                IS_WATCH_DOG_RUNNING = false;
            }
        });

        thread.start();
    }
}
