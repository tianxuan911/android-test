package com.qw.qw_ad.workers;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Result;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class AppUpdateWorker extends Worker {

    public AppUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

    }

    @NonNull
    @Override
    public Result doWork() {
        new AppUpdateUtil(getApplicationContext()).updateApp();
        return Result.success();
    }



}
