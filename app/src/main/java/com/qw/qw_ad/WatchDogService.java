package com.qw.qw_ad;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.List;

/**
 * 守护服务
 * 监控到APP进程退出,则重启APP
 * https://www.jianshu.com/p/1da4541b70ad
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class WatchDogService extends JobService {

    //任务执行周期5秒钟
    private static long PERIODIC = 5 * 1000L;
    private static int JOB_ID = 1010;
    private String pkgName = "";
    private String appName = "";
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        pkgName = AppUtils.getPackageName(getApplicationContext());
        appName = AppUtils.getAppName(getApplicationContext());
        Log.d("watch-dog",String.format("[%s]守护服务创建",appName));
        startJobSheduler();
    }

    public void startJobSheduler() {
        try {
            JobInfo.Builder builder = new JobInfo.Builder(JOB_ID, new ComponentName(getPackageName(), WatchDogService.class.getName()));
            //7.0及以上版本
            if (Build.VERSION.SDK_INT >= 24) {
                builder.setMinimumLatency(PERIODIC); //执行的最小延迟时间
                builder.setOverrideDeadline(PERIODIC);  //执行的最长延时时间
                builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR);//线性重试方案
            } else {
                builder.setPeriodic(PERIODIC);
            }
            builder.setPersisted(true);  // 设置设备重启时，执行该任务
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            builder.setRequiresCharging(true); // 当插入充电器，执行该任务

            JobScheduler jobScheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(builder.build());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        if(!isRun(getApplicationContext())){
            Log.d("watch-dog",String.format("[%s]应用启动",appName));
            AppUtils.startMainActive(getApplicationContext());
        }
        return false;

    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d("watch-dog","运行结束");
        return false;
    }

    /**
     * 判断应用是否在运行
     * @param context
     * @return
     */
    public boolean isRun(Context context){
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(100);
        boolean isAppRunning = false;

        //100表示取的最大的任务数，info.topActivity表示当前正在运行的Activity，info.baseActivity表系统后台有此进程在运行
        for (ActivityManager.RunningTaskInfo info : list) {
            if (info.topActivity.getPackageName().equals(pkgName) || info.baseActivity.getPackageName().equals(pkgName)) {
                isAppRunning = true;
                Log.d("ActivityService isRun()",info.topActivity.getPackageName() + " info.baseActivity.getPackageName()="+info.baseActivity.getPackageName());
                break;
            }
        }

        Log.d("ActivityService isRun()", "com.ad 程序   ...isAppRunning......"+isAppRunning);
        return isAppRunning;
    }
}