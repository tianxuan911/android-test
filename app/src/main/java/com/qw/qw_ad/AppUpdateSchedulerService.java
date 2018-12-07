package com.qw.qw_ad;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AppUpdateSchedulerService extends JobService {

    //任务执行周期60秒
    private static long PERIODIC = 60 * 1000L;

    @Override
    public void onCreate() {
        super.onCreate();
        startJobSheduler();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        startService(new Intent(getApplicationContext(),AppUpdateService.class));
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public void startJobSheduler() {
        Log.i(AppUpdateSchedulerService.class.getName(),"启动APP更新计划任务");
        try {
            JobInfo.Builder builder = new JobInfo.Builder(SchedulerJobs.JOB_ID_APP_UPDATE, new ComponentName(getPackageName(), AppUpdateSchedulerService.class.getName()));
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
            Log.e(AppUpdateSchedulerService.class.getName(),"启动APP更新计划任务出现错误",ex);
        }
    }
}
