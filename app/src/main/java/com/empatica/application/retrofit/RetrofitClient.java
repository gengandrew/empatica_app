package com.empatica.application.retrofit;

import android.util.Log;
import android.os.StrictMode;

import java.net.Inet6Address;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.DatagramSocket;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private Retrofit instance;
    //private InetAddress inetAddress;
    private String ip;
    private static String baseUrl;
    private static int port = 5000;

    public RetrofitClient() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            ip = socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            Log.d("CustomDebug", "Local host is unable to be obtained");
            Log.d("CustomDebug", e.toString());
        }

//        try {
//            inetAddress = InetAddress.getLocalHost();
//        } catch (Exception e) {
//            Log.d("CustomDebug", "Local host is unable to be obtained");
//            Log.d("CustomDebug", e.toString());
//        }

        Log.d("CustomDebug", "Obtained localhost of " + ip);
        baseUrl =  "http://" + ip + ":" + port + "/api/";
        Log.d("CustomDebug", "Obtained a BaseUrl of " + baseUrl);

        // Virtual Machine ip
        // baseUrl = "http://10.0.2.2:5000/api/";

        instance = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public Retrofit getInstance() {
        return instance;
    }

    public IBackend getService() {
        return instance.create(IBackend.class);
    }
}
