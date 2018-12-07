package com.qw.qw_ad;

import android.app.IntentService;
import android.app.Notification;
import android.content.Intent;
import android.os.Environment;
import android.support.annotation.Nullable;

import com.qw.qw_ad.workers.AppUpdateUtil;

import java.io.File;

/**
 * APP版本更新
 * http://www.cnblogs.com/bugzone/p/strictMode.html
 */
public class AppUpdateService extends IntentService {

    public AppUpdateService() {
        super("AppUpdateService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(SchedulerJobs.JOB_ID_APP_UPDATE, new Notification());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        new AppUpdateUtil(getApplicationContext()).updateApp();
    }


}
