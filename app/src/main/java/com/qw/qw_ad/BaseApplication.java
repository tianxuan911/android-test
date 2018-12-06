package com.qw.qw_ad;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BaseApplication extends Application {
    public static Context mContext;

    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();

        runAppDeamon();

        //程序崩溃异常捕获并自动重启
        Thread.setDefaultUncaughtExceptionHandler(restartHandler);
    }

    /**
     * 启动应用守护进程
     */
    private void runAppDeamon() {
        //5.0及以上版本
        if (Build.VERSION.SDK_INT >= 21) {
            startService(new Intent(mContext, WatchDogService.class));
        }
    }

    private Thread.UncaughtExceptionHandler restartHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.e("error","未处理的异常",e);
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
