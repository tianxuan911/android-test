package com.qw.qw_ad.workers;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

import com.qw.qw_ad.AppUtils;

import java.util.List;

public class WatchDogUtil {
    //任务执行周期/毫秒
    public static final long PERIODIC = 5 *1000L;
    //应用包名称
    private String pkgName = "";
    //应用名称
    private String appName = "";
    Context context;

    public WatchDogUtil(Context context) {
        this.context = context;
        pkgName = AppUtils.getPackageName(context);
        appName = AppUtils.getAppName(context);
    }

    public void runApp(){
        if (!isRun()) {
            Log.d(this.getClass().getName(), String.format("[%s]启动应用", appName));
            AppUtils.startMainActive(context);
        }
    }

    /**
     * 判断应用是否在运行
     *
     * @return
     */
    private boolean isRun() {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
        boolean isAppRunning = false;

        //100表示取的最大的任务数，info.topActivity表示当前正在运行的Activity，info.baseActivity表系统后台有此进程在运行
        for (ActivityManager.RunningTaskInfo info : list) {
            if (info.topActivity.getPackageName().equals(pkgName) || info.baseActivity.getPackageName().equals(pkgName)) {
                isAppRunning = true;
//                Log.d(WatchDogService.class.getName(),info.topActivity.getPackageName() + " info.baseActivity.getPackageName()="+info.baseActivity.getPackageName());
                break;
            }
        }

        Log.d(this.getClass().getName(), String.format(isAppRunning ? "[%s]应用运行正常" : "[%s]应用已停止", appName));
        return isAppRunning;
    }
}
