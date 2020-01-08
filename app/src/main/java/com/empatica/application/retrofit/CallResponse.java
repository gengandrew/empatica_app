package com.empatica.application.retrofit;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CallResponse {
    @SerializedName("SessionID")
    @Expose
    private Integer sessionID;

    public Integer getSessionID() {
        return sessionID;
    }
}
