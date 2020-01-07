package com.mai.cat_door_small.http;


import com.mai.cat_door_small.bean.CDRespone;
import com.mai.cat_door_small.bean.PicUrl;

import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PartMap;
import rx.Observable;

/**
 * Created by mai on 2017/11/20.
 */
public interface CatDoctorService {

    @Multipart
    @POST("doctorMall/report/testUpload")
    Observable<CDRespone<PicUrl>> faceTonUpload(@PartMap Map<String, RequestBody> params);

    @Multipart
    @POST("uploadImg")
    Observable<PicUrl> faceTonUpload2(@PartMap Map<String, RequestBody> params);

}

