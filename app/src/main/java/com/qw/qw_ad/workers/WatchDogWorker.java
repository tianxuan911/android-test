package com.qw.qw_ad.workers;

import android.content.Context;
import android.support.annotation.NonNull;

import androidx.work.Result;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class WatchDogWorker extends Worker {

    public WatchDogWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

    }

    @NonNull
    @Override
    public Result doWork() {
        new WatchDogUtil(getApplicationContext()).runApp();
        return Result.success();
    }


}
