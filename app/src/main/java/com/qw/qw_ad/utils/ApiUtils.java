package com.qw.qw_ad.utils;

import okhttp3.OkHttpClient;

public class ApiUtils {

    private static final OkHttpClient client = new OkHttpClient();

    public static OkHttpClient getClient() {
        return client;
    }


}
