package com.qw.qw_ad;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

/**
 * APP版本更新
 * http://www.cnblogs.com/bugzone/p/strictMode.html
 */
public class AppUpdateService extends IntentService {

    private static boolean isDownloading = false;
    private File downloadDir = null;
    private String apkName = null;

    public AppUpdateService() {
        super("AppUpdateService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        downloadDir = getApplicationContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        apkName = AppUtils.getAppName(getApplicationContext()) + ".apk";

    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        //如果服务器版本号与当前APP版本号不同,下载apk->安装->重启app
        int latestedVersionCode = getAppVersionCode();
        Log.i(AppUpdateService.class.getName(), String.format("对比版本号,当前版本号[%d],最新版本号[%d]", AppUtils.getVersionCode(getApplicationContext()), latestedVersionCode));
        if (isDownloading == true || latestedVersionCode == -1 || AppUtils.getVersionCode(getApplicationContext()) == latestedVersionCode) {
            return;
        }
        ApiUtils.getClient().newCall(new Request.Builder().url(ApiConsts.URL_APK_DOWNLOAD).build()).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(AppUpdateService.class.getName(), "apk update failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i(AppUpdateService.class.getName(), String.format("启动最新版本APK下载,文件目录[%s]", downloadDir.getAbsolutePath()));

                if (response.code() != HttpURLConnection.HTTP_OK) {
                    Log.i(AppUpdateService.class.getName(), String.format("最新版本APK失败[HTTPCODE:%d]", response.code()));
                    return;
                }

                downloadApk(response);

                installApp();

            }

            private void downloadApk(Response response) throws IOException {
                isDownloading = true;
                InputStream inputStream = null;
                FileOutputStream outputStream = null;
                try {
                    inputStream = response.body().byteStream();
                    File file = new File(downloadDir, apkName);
                    outputStream = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int size;
                    while ((size = inputStream.read(buffer)) > 0) {
//                        Log.d(AppUpdateService.class.getName(), String.format("本次下载进度%dbyte", size));
                        outputStream.write(buffer, 0, size);
                    }
                } finally {
                    isDownloading = false;
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
                Log.i(AppUpdateService.class.getName(), String.format("最新版本APK下载成功,文件目录[%s]", downloadDir.getAbsolutePath()));
            }
        });

    }

    /**
     * 获取版本号
     *
     * @return
     */
    private int getAppVersionCode() {

        Request request = new Request.Builder().url(ApiConsts.URL_VERSION_CODE).build();
        try {
            Response response = ApiUtils.getClient().newCall(request).execute();
            String versionCode = response.body().string();
            response.close();
            Log.d(AppUpdateService.class.getName(), String.format("最新版本号[%s]", versionCode));
            return Integer.parseInt(versionCode);
        } catch (IOException e) {
            Log.e(AppUpdateService.class.getName(), "版本号获取错误", e);
        }
        return -1;
    }

    private void installApp() {
        Log.i(AppUpdateService.class.getName(), String.format("启动最新版本APK安装,文件目录[%s]", downloadDir.getAbsolutePath()));

        Intent intent = new Intent(Intent.ACTION_VIEW);
        //版本在7.0以上不能直接通过uri访问的
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 由于没有在Activity环境下启动Activity,设置下面的标签
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //参数1:上下文, 参数2:Provider主机地址 和配置文件中保持一致,参数3:共享的文件
            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), AppUtils.getPackageName(getApplicationContext()) + ".fileprovider", new File(downloadDir, apkName));//注意修改
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(new File(downloadDir, apkName)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
    }

}
