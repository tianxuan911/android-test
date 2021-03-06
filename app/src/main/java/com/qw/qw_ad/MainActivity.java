package com.qw.qw_ad;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;


import java.io.File;
import java.io.IOException;

import fi.iki.elonen.SimpleWebServer;

/**
 * 官方教程
 * https://developer.android.com/guide/webapps/webview
 * 第三方教程
 * https://www.jianshu.com/p/3c94ae673e2a
 * https://www.jianshu.com/p/2857d55e2f6e
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static String H5_URI="file:///android_asset/404.html";
    private static String H5_EXTERNAL_DIR="/qiwei/h5_latest/";
    private static String SERVER_ROOT="";
    private static int SERVER_PORT=9010;
    WebView myWebView;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //取得sdcard文件路径
        File path =new File("/mnt/external_sd");
//        File path1 =Environment.getExternalStorageDirectory();
        if(path.exists()){
            File h5path = new File(path,H5_EXTERNAL_DIR);
            if(h5path.exists()){
                SERVER_ROOT = path+H5_EXTERNAL_DIR;
                H5_URI=String.format("http://localhost:%d/%s",SERVER_PORT,"index.html");
                SimpleWebServer simpleWebServer = new SimpleWebServer(null,SERVER_PORT,
                        new File(SERVER_ROOT),true,"*");
                try {
                    if(!simpleWebServer.isAlive()){
                        simpleWebServer.start();
                    }
                } catch (IOException e) {
                    Log.e(TAG,e.getLocalizedMessage(),e);
                }
            }
        }

        //在共享系统（sharedUserId="android.uid.system）空间时，webview实例化可能会出现错误

        try{
//            AppUtils.hookWebView();
            myWebView = new WebView(getApplicationContext());
        }catch (Exception e){
            Log.e(TAG,"当前系统设置不支持webView组件在系统控件中实例化",e);
        }
        if(myWebView != null){
            setContentView(myWebView);

            customyWebViewSettings(myWebView);
            customyWebViewClient(myWebView);
            injectJSInterface(myWebView);

            loadData(myWebView);
        }


    }

    /**
     * 加载数据
     * @param myWebView
     */
    private void loadData(WebView myWebView) {
        myWebView.loadUrl(H5_URI);
    }

    /**
     * 注入JS接口
     * * @param myWebView
     */
    private void injectJSInterface(WebView webView) {
        webView.addJavascriptInterface(new WebAppInterface(this),"Android");
    }

    /**
     * 自定义webview客户端
     * @param myWebView
     */
    private void customyWebViewClient(WebView myWebView) {
        myWebView.setWebViewClient(new MyWebViewClient());
    }

    /**
     * 自定义webview设置
     * @param webView
     */
    private void customyWebViewSettings(WebView webView) {
        //声明WebSettings子类
        WebSettings webSettings = webView.getSettings();

        //如果访问的页面中要与Javascript交互，则webview必须设置支持Javascript
        webSettings.setJavaScriptEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        // 若加载的 html 里有JS 在执行动画等操作，会造成资源浪费（CPU、电量）
        // 在 onStop 和 onResume 里分别把 setJavaScriptEnabled() 给设置成 false 和 true 即可

        //设置自适应屏幕，两者合用
        webSettings.setUseWideViewPort(true); //将图片调整到适合webview的大小
        webSettings.setLoadWithOverviewMode(true); // 缩放至屏幕的大小

        //缩放操作
        webSettings.setSupportZoom(true); //支持缩放，默认为true。是下面那个的前提。
        webSettings.setBuiltInZoomControls(true); //设置内置的缩放控件。若为false，则该WebView不可缩放
        webSettings.setDisplayZoomControls(false); //隐藏原生的缩放控件

        //其他细节操作
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); //关闭webview中缓存
        webSettings.setAllowFileAccess(true); //设置可以访问文件
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true); //支持通过JS打开新窗口
        webSettings.setLoadsImagesAutomatically(true); //支持自动加载图片
        webSettings.setDefaultTextEncodingName("utf-8");//设置编码格式

    }

    /**
     * 实现导航功能
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && myWebView.canGoBack() && myWebView != null) {
            myWebView.goBack();
            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (myWebView != null) {
            myWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            myWebView.clearHistory();

            ((ViewGroup) myWebView.getParent()).removeView(myWebView);
            myWebView.destroy();
            myWebView = null;
        }
        super.onDestroy();

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(myWebView != null){
            myWebView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(myWebView != null){
            myWebView.onResume();
        }
    }
}
