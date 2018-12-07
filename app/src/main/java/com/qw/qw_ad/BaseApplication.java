package com.qw.qw_ad;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.qw.qw_ad.workers.AppUpdateUtil;
import com.qw.qw_ad.workers.AppUpdateWorker;
import com.qw.qw_ad.workers.WatchDogUtil;
import com.qw.qw_ad.workers.WatchDogWorker;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class BaseApplication extends Application {
    public static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();

        //[21,23]版本，使用JobService管理，任务执行周期没有最小时间限制，延迟小，功能正常；
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT <= Build.VERSION_CODES.M){
            runAppDeamon();
        //其余版本使用通用解决方案WorkManager，任务执行周期最小为15分钟，延迟较大，功能正常；
        }else{
            runWorkers();
        }

        //程序崩溃异常捕获并自动重启
        Thread.setDefaultUncaughtExceptionHandler(restartHandler);
    }

    private void runWorkers() {
        //PeriodicWorkRequest 最小重复周期为15分钟
        //APP退出重启
        PeriodicWorkRequest.Builder watchDogBuilder =
                new PeriodicWorkRequest.Builder(WatchDogWorker.class, WatchDogUtil.PERIODIC,
                        TimeUnit.MILLISECONDS);
        PeriodicWorkRequest watchDogWork = watchDogBuilder.build();

        //APP自动更新
        PeriodicWorkRequest.Builder appUpdateBuilder =
                new PeriodicWorkRequest.Builder(AppUpdateWorker.class, AppUpdateUtil.PERIODIC,
                        TimeUnit.MILLISECONDS);
//        Constraints appUpdateConstraints = new Constraints.Builder()
//                .build();
        PeriodicWorkRequest appUpdateWork = appUpdateBuilder.build();

        List<PeriodicWorkRequest> requests = new ArrayList<>();
        requests.add(watchDogWork);
        requests.add(appUpdateWork);

        // Then enqueue the recurring task:
        WorkManager.getInstance().enqueue(requests);


    }

    /**
     * 启动应用守护进程
     */
    private void runAppDeamon() {
        //启动应用守护进程
        Intent watchDog = new Intent(mContext, WatchDogService.class);
        //启动版本更新计划任务
        Intent appUpdate = new Intent(mContext, AppUpdateSchedulerService.class);
        //8.0及以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mContext.startForegroundService(watchDog);
            mContext.startForegroundService(appUpdate);
            //[5.0,8.0)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            startService(watchDog);
            startService(appUpdate);
        }
    }

    private Thread.UncaughtExceptionHandler restartHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.e("error", "未处理的异常", e);
            Intent intent = new Intent(mContext, QWADWebView.class);
            //重启应用
            PendingIntent restartIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            //2秒钟后重启应用
            AlarmManager mAlarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            mAlarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + 2000,
                    restartIntent);
            //退出程序
            android.os.Process.killProcess(android.os.Process.myPid());  //结束当前进程
        }
    };
}
