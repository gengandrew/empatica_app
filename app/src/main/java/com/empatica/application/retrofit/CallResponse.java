package com.empatica.application.retrofit;

import com.google.gson.annotations.SerializedName;

public class CallResponse {
    @SerializedName("sessionID")
    private int sessionID;

    public int getSessionID() {
        return sessionID;
    }
}
