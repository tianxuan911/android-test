package com.qw.qw_ad;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 *  开机自启动,如果需要在模拟器中验证，请使用冷启动(cold boot now)，默认是fastboot
 *  https://blog.csdn.net/qq_29586601/article/details/79935425
 *  http://www.trinea.cn/android/android-boot_completed-not-work/
 */
public class BootCompletedReceiver extends BroadcastReceiver {
    static final String action_boot ="android.intent.action.BOOT_COMPLETED";
    @Override
    public void onReceive (Context context, Intent intent) {
        Log.i("charge start", "启动完成");
        if (intent.getAction().equals(action_boot)){
            Intent mBootIntent = new Intent(context, QWADWebView.class);
            // 下面这句话必须加上才能开机自动运行app的界面
            mBootIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mBootIntent);
        }
    }
}
