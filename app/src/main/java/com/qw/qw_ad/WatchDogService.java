package com.qw.qw_ad;

import android.annotation.TargetApi;
import android.app.Notification;
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
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class WatchDogService extends JobService {

    @Override
    public void onCreate() {
        super.onCreate();

        startForeground(SchedulerJobs.JOB_ID_WATCH_DOG,new Notification());

        Log.d(WatchDogService.class.getName(),String.format("[%s]守护服务创建",AppUtils.getAppName(getApplicationContext())));
        startJobSheduler();
    }

    public void startJobSheduler() {
        try {
            JobInfo.Builder builder = new JobInfo.Builder(SchedulerJobs.JOB_ID_WATCH_DOG, new ComponentName(getPackageName(), WatchDogService.class.getName()));
            //7.0及以上版本
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setMinimumLatency(WatchDogUtil.PERIODIC); //执行的最小延迟时间
                builder.setOverrideDeadline(WatchDogUtil.PERIODIC);  //执行的最长延时时间
                builder.setBackoffCriteria(JobInfo.DEFAULT_INITIAL_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR);//线性重试方案
            } else {
                builder.setPeriodic(WatchDogUtil.PERIODIC);
            }
            builder.setPersisted(true);  // 设置设备重启时，执行该任务
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

            JobScheduler jobScheduler = (JobScheduler) this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            jobScheduler.schedule(builder.build());
        } catch (Exception ex) {
            Log.e(WatchDogService.class.getName(),"启动应用重启计划任务出现错误",ex);
        }
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        new WatchDogUtil(getApplicationContext()).runApp();
        return false;

    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(WatchDogService.class.getName(),"运行结束");
        return false;
    }


}
