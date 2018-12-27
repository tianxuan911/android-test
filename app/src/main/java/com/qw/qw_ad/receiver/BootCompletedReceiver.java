package com.qw.qw_ad.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.qw.qw_ad.utils.AppUtils;

/**
 *  开机自启动,如果需要在模拟器中验证，请使用冷启动(cold boot now)，默认是fastboot
 *  https://blog.csdn.net/qq_29586601/article/details/79935425
 *  http://www.trinea.cn/android/android-boot_completed-not-work/
 *  https://www.jianshu.com/p/b16631a2fe3c
 *  https://www.jianshu.com/p/1da4541b70ad
 *
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    static final String action_boot ="android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive (Context context, Intent intent) {
        Log.i("charge start", "启动完成");
        if (intent.getAction().equals(action_boot)){
            AppUtils.startMainActive(context);
        }
    }
}
