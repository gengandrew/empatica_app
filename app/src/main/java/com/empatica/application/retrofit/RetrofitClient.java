package com.empatica.application.retrofit;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static String baseUrl = "http://192.168.8.105:5000/api/";

    private static Retrofit instance = new Retrofit.Builder()
            .baseUrl(baseUrl)
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build();

    private static IBackend backendService = instance.create(IBackend.class);

    public static Retrofit getInstance() {
        return instance;
    }

    public static IBackend getService() {
        return backendService;
    }
}
