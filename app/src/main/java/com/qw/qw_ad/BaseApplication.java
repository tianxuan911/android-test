package com.qw.qw_ad;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import com.qw.qw_ad.service.AppUpdateSchedulerService;
import com.qw.qw_ad.service.AppUpdateServiceLess21;
import com.qw.qw_ad.service.WatchDogService;
import com.qw.qw_ad.service.WatchDogServiceLess21;
import com.qw.qw_ad.utils.AppUpdateUtil;
import com.qw.qw_ad.utils.SchedulerJobs;
import com.qw.qw_ad.utils.WatchDogUtil;

public class BaseApplication extends Application {

    private static final String TAG = "BaseApplication";

    public static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        //启动计划任务
        runAppDeamon();
        //程序崩溃异常捕获并自动重启
        Thread.setDefaultUncaughtExceptionHandler(restartHandler);
    }

    /**
     * 启动计划任务
     */
    private void runAppDeamon() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WatchDogService.startSchedule(getApplicationContext());
            AppUpdateSchedulerService.startScheduler(getApplicationContext());
        }else{

            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);

            Intent appUpdateIntent = new Intent(mContext,AppUpdateServiceLess21.class);
            PendingIntent appUpdatePi = PendingIntent.getService(mContext,SchedulerJobs.JOB_ID_APP_UPDATE,appUpdateIntent,PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, AppUpdateUtil.PERIODIC + SystemClock.elapsedRealtime(), AppUpdateUtil.PERIODIC, appUpdatePi);


            Intent watchDogIntent = new Intent(mContext,WatchDogServiceLess21.class);
            PendingIntent watchDogPi = PendingIntent.getService(mContext,SchedulerJobs.JOB_ID_WATCH_DOG,watchDogIntent,PendingIntent.FLAG_CANCEL_CURRENT);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, WatchDogUtil.PERIODIC + SystemClock.elapsedRealtime(), WatchDogUtil.PERIODIC, watchDogPi);

        }
    }

    private Thread.UncaughtExceptionHandler restartHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.e("error", "未处理的异常", e);
            Intent intent = new Intent(mContext, MainActivity.class);
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
