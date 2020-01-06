package com.empatica.application.retrofit;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.GET;

public interface IBackend {

    @POST("InsertAssociation")
    @FormUrlEncoded
    Observable<String> InsertAssociation(@Field("participantID") int participantID);

    @POST("InsertData")
    @FormUrlEncoded
    Observable<String> InsertData(@Field("sessionID") int sessionID,
                                  @Field("e4Time") double e4Time,
                                  @Field("bvp") float bvp,
                                  @Field("eda") float eda,
                                  @Field("ibi") float ibi,
                                  @Field("heartRate") float heartRate,
                                  @Field("temperature") float temperature);

    @POST("InsertAcceleration")
    @FormUrlEncoded
    Observable<String> InsertAcceleration(@Field("sessionID") int sessionID,
                                          @Field("e4Time") double e4Time,
                                          @Field("accelX") float accelX,
                                          @Field("accelY") float accelY,
                                          @Field("accelZ") float accelZ);
}
