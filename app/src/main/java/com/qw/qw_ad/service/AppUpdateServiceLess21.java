package com.qw.qw_ad.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.qw.qw_ad.utils.AppUpdateUtil;

public class AppUpdateServiceLess21 extends Service {
    private static boolean IS_APP_UPDATE_RUNNING = false;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        exeAppUpdate();
        return result;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        exeAppUpdate();
        return null;
    }

    private void exeAppUpdate() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if(IS_APP_UPDATE_RUNNING){
                    return;
                }
                IS_APP_UPDATE_RUNNING = true;
                new AppUpdateUtil(getBaseContext()).updateApp();
                IS_APP_UPDATE_RUNNING = false;
            }
        });
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                IS_APP_UPDATE_RUNNING =false;
            }
        });
        thread.start();
    }
}
