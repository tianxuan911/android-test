package com.qw.qw_ad.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.qw.qw_ad.MainActivity;
import com.qw.qw_ad.utils.ApiConsts;
import com.qw.qw_ad.utils.ApiUtils;
import com.qw.qw_ad.utils.AppUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class AppUpdateUtil {
    public static final String TAG = "AppUpdateUtil";
    //任务执行周期/毫秒
    public static final long PERIODIC = 60 * 1000L;
    private static boolean isDownloading = false;
    private File downloadDir;
    private String apkName;
    private String pkgName;
    private Context context;

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

                installAppSlientByRoot();

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
        } catch (Exception e) {
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
        String cmd = getInstallCommand(apkFile.getAbsolutePath());
        try {
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec(cmd);
            process.waitFor();
            InputStream errorInput = process.getErrorStream();
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String error = "";
            String result = "";
            String line = "";

            while ((line = bufferedReader.readLine()) != null) {
                result += line;
                Log.i(TAG,"result "+line);
            }
            bufferedReader = new BufferedReader(new InputStreamReader(errorInput));
            while ((line = bufferedReader.readLine()) != null) {
                error += line;
                Log.i(TAG,"error  "+line);
            }
            if(result.equals("Success")){
                Log.i(TAG, "install: Success");
            }else{
                Log.i(TAG, "install: error"+error);
            }
        } catch (IOException e) {
            Log.i(TAG, e.getLocalizedMessage(),e);
        } catch (InterruptedException e) {
            Log.i(TAG, e.getLocalizedMessage(),e);
        }
    }


    /**
     * 执行具体的静默安装逻辑，需要手机ROOT。
     * @return 安装成功返回true，安装失败返回false。
     */
    private boolean installAppSlientByRoot() {
        String apkPath = new File(downloadDir, apkName).getAbsolutePath();
        boolean result = false;
        DataOutputStream dataOutputStream = null;
        BufferedReader errorStream = null;
        try {
            Charset charSet = Charset.forName("utf-8");
            // 申请su权限
            Process process = Runtime.getRuntime().exec("su");

            dataOutputStream = new DataOutputStream(process.getOutputStream());


            //安裝APK
            String install = getInstallCommand(apkPath)+"\n";
            Log.i(TAG,install);
            //启动APP
            String startActivity = String.format("am start %s/%s \n",AppUtils.getPackageName(context),MainActivity.class.getName());
            Log.i(TAG,startActivity);

            dataOutputStream.write((install+startActivity).getBytes(charSet));

//            dataOutputStream.flush();
//            dataOutputStream.writeBytes("exit\n");
            dataOutputStream.flush();

            process.waitFor();
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String msg = "";
            String line;
            // 读取命令的执行结果
            while ((line = errorStream.readLine()) != null) {
                msg += line;
            }
            Log.d(TAG, "install msg is " + msg);
            // 如果执行结果中包含Failure字样就认为是安装失败，否则就认为安装成功
            if (!msg.contains("Failure")) {
                result = true;
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage(), e);
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        return result;
    }

    /**
     * 获取PM安装命令
     * @param apkFilePath
     * @return
     */

    private String getInstallCommand(String apkFilePath) {
        String cmd;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT_WATCH) {
            cmd = String.format("pm install -r -d %s",apkFilePath);
        } else {
            cmd = String.format("pm install -r -d -i %s --user 0 %s",pkgName,apkFilePath);
        }
        return cmd;
    }

}
