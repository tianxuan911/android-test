package com.qw.qw_ad;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.qw.qw_ad.workers.WatchDogUtil;

/**
 * 守护服务
 * 监控到APP进程退出,则重启APP
 * https://www.jianshu.com/p/1da4541b70ad
 * https://blog.csdn.net/aqi00/article/details/71638721
 */
public class WatchDogService extends JobService {

    private static final String TAG = "WatchDogService";

    @Override
    public boolean onStartJob(JobParameters params) {
        try{
            new WatchDogUtil(getApplicationContext()).runApp();
        }catch (Exception e){
            Log.e(TAG,"APP重启出现错误",e);
        }finally {
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                startSchedule(getApplicationContext());
            }
            //Call Job Finished
            jobFinished(params, false );
        }
        return true;

    }

    public static void startSchedule(Context context) {
        Log.i(TAG,"启动守护服务计划任务");
        try {
            JobInfo.Builder builder = new JobInfo.Builder(SchedulerJobs.JOB_ID_WATCH_DOG, new ComponentName(context.getPackageName(), WatchDogService.class.getName()));
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                builder.setMinimumLatency(WatchDogUtil.PERIODIC).setOverrideDeadline(WatchDogUtil.PERIODIC)
                        .setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR);

            }else{
                builder.setPeriodic(WatchDogUtil.PERIODIC);
            }

            builder.setPersisted(true);  // 设置设备重启时，执行该任务

            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(builder.build());
        } catch (Exception ex) {
            Log.e(TAG,"启动应用重启计划任务出现错误",ex);
        }

    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG,"运行结束");
        return false;
    }


}
