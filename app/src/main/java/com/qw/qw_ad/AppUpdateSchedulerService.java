package com.qw.qw_ad;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.qw.qw_ad.workers.AppUpdateUtil;

public class AppUpdateSchedulerService extends JobService {

    private static final String TAG = "AppUpdateSchedulerServ";

    @Override
    public boolean onStartJob(JobParameters params) {
        try {
            new AppUpdateUtil(getApplicationContext()).updateApp();
        }catch (Exception e){
            Log.e(TAG,"APP更新出现错误",e);
        }finally{
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                startScheduler(getApplicationContext());
            }
            jobFinished(params,false);
        }
        return true;
    }

    public static void startScheduler(Context context) {
        Log.i(TAG,"启动APP更新计划任务");
        try {
            JobInfo.Builder builder = new JobInfo.Builder(SchedulerJobs.JOB_ID_APP_UPDATE, new ComponentName(context.getPackageName(), AppUpdateSchedulerService.class.getName()));

            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                builder.setMinimumLatency(AppUpdateUtil.PERIODIC).setOverrideDeadline(AppUpdateUtil.PERIODIC)
                        .setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR);

            }else{
                builder.setPeriodic(AppUpdateUtil.PERIODIC);
            }

            builder.setPersisted(true);  // 设置设备重启时，执行该任务
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
            //builder.setRequiresCharging(true); // 当插入充电器，执行该任务

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(builder.build());
        } catch (Exception ex) {
            Log.e(TAG,"启动APP更新计划任务出现错误",ex);
        }
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }


}
