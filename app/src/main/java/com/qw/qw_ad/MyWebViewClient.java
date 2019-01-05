package com.qw.qw_ad;

import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class MyWebViewClient extends WebViewClient {
    private static final String TAG="MyWebViewClient";
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
//        if (Uri.parse(url).getHost().equals("https://www.example.com")) {
//            // This is my website, so do not override; let my WebView load the page
//        return false;
//        }
//        // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
//        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
//        startActivity(intent);
//        return true;
        //不打开外部浏览器，当前webview加载所有页面
        view.loadUrl(url);
        return true;
    }
    @Override
    public void onPageFinished(WebView view, String url) {
        // TODO Auto-generated method stub
        super.onPageFinished(view, url);
        //自动执行名为runByAndroidAfterLoaded的无参方法
        Log.d(TAG,String.format("加载URL[%s],尝试执行javascript:runByAndroidAfterLoaded()",url));
        view.loadUrl("javascript:runByAndroidAfterLoaded()");
    }

}
