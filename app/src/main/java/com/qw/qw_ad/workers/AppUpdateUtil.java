package com.qw.qw_ad.workers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.qw.qw_ad.ApiConsts;
import com.qw.qw_ad.ApiUtils;
import com.qw.qw_ad.AppUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class AppUpdateUtil {
    public static final String TAG = "AppUpdateUtil";
    //任务执行周期/毫秒
    public static final long PERIODIC = 5 * 1000L;
    private static boolean isDownloading = false;
    private File downloadDir = null;
    private String apkName = null;
    private String pkgName = null;
    private Context context = null;

    public AppUpdateUtil(Context context) {
        this.context = context;
        downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        apkName = AppUtils.getAppName(context) + ".apk";
        pkgName = AppUtils.getPackageName(context);
    }

    public void updateApp() {
        //如果服务器版本号与当前APP版本号不同,下载apk->安装->重启app
        int latestedVersionCode = getAppVersionCode();
        Log.i(TAG, String.format("对比版本号,当前版本号[%d],最新版本号[%d]", AppUtils.getVersionCode(context), latestedVersionCode));
        if (isDownloading == true || latestedVersionCode == -1 || AppUtils.getVersionCode(context) == latestedVersionCode) {
            return;
        }
        ApiUtils.getClient().newCall(new Request.Builder().url(ApiConsts.URL_APK_DOWNLOAD).build()).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "APK下载失败", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i(TAG, String.format("启动最新版本APK下载,文件目录[%s]", downloadDir.getAbsolutePath()));

                if (response.code() != HttpURLConnection.HTTP_OK) {
                    Log.i(TAG, String.format("最新版本APK失败[HTTPCODE:%d]", response.code()));
                    return;
                }

                downloadApk(response);

                installAppBySlef();

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
//                        Log.d(TAG, String.format("本次下载进度%dbyte", size));
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
                Log.i(TAG, String.format("最新版本APK下载成功,文件目录[%s]", downloadDir.getAbsolutePath()));
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
            Log.d(TAG, String.format("最新版本号[%s]", versionCode));
            return Integer.parseInt(versionCode);
        } catch (IOException e) {
            Log.e(TAG, "版本号获取错误", e);
        }
        return -1;
    }

    /**
     *
     * 调用系统原生组件安装。调用系统相关服务，自动点击完成安装，需要系统默认开启相关服务
     * https://www.jianshu.com/p/719bd48fec11
     *
     */
    private void installAppBySlef() {
        Log.i(this.getClass().getName(), String.format("启动最新版本APK安装,文件目录[%s]", downloadDir.getAbsolutePath()));

        Intent intent = new Intent(Intent.ACTION_VIEW);
        //版本在7.0以上不能直接通过uri访问的
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 由于没有在Activity环境下启动Activity,设置下面的标签
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            //参数1:上下文, 参数2:Provider主机地址 和配置文件中保持一致,参数3:共享的文件
            Uri contentUri = FileProvider.getUriForFile(context, AppUtils.getPackageName(context) + ".fileprovider", new File(downloadDir, apkName));//注意修改
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
        } else {
            intent.setDataAndType(Uri.fromFile(new File(downloadDir, apkName)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }


    /**
     *
     * 静默安装参考，需要共享系统空间，APK加入系统签名后可用
     * https://blog.csdn.net/to_perfect/article/details/81809644
     * android源码查看网站
     * http://androidxref.com/
     *
     */
    private void installAppSlient() {

        Log.i(TAG, String.format("启动最新版本APK安装,文件目录[%s]", downloadDir.getAbsolutePath()));

        File apkFile = new File(downloadDir, apkName);
        String cmd = "";
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH) {
            cmd = String.format("pm install -r -d ",apkFile.getAbsolutePath());
        } else {
            cmd = String.format("pm install -r -d -i %s --user 0 %s",pkgName,apkFile.getAbsolutePath());
        }
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(cmd);
            InputStream errorInput = process.getErrorStream();
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String error = "";
            String result = "";
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                result += line;
            }
            bufferedReader = new BufferedReader(new InputStreamReader(errorInput));
            while ((line = bufferedReader.readLine()) != null) {
                error += line;
            }
            if(result.equals("Success")){
                Log.i(TAG, "install: Success");
            }else{
                Log.i(TAG, "install: error"+error);
            }
        } catch (IOException e) {
            Log.i(TAG, "install: error",e);
        }
    }

}
