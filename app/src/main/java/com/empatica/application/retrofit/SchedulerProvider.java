package com.empatica.application.retrofit;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SchedulerProvider {
    // User interface thread
    public static Scheduler UIThread() {
        return AndroidSchedulers.mainThread();
    }

    // Input output thread
    public static Scheduler IOThread() {
        return Schedulers.io();
    }

    // Computation thread
    public static Scheduler computationThread() {
        return Schedulers.computation();
    }
}
