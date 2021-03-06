package com.empatica.application.retrofit;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    // Live Machine [http://10.134.71.198:8006/api/] || Virtual Machine [http://192.168.43.203:8006/api/]
//    private static String baseUrl = "http://192.168.43.203:8006/api/";
//
//    private static Retrofit instance = new Retrofit.Builder()
//            .baseUrl(baseUrl)
//            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
//            .addConverterFactory(GsonConverterFactory.create())
//            .build();
//
//    private static IBackend backendService = instance.create(IBackend.class);
//
//    public static Retrofit getInstance() {
//        return instance;
//    }

//    public static IBackend getService() {
//        return backendService;
//    }

    public static IBackend getService(String baseUrl) {
//        Gson gson = new GsonBuilder()
//                .setLenient()
//                .create();
        Retrofit instance = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        IBackend backendService = instance.create(IBackend.class);
        return backendService;
    }

}
