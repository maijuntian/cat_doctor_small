package com.mai.cat_door_small.bean;

import com.google.gson.annotations.SerializedName;

/**
 * Created by maijuntian on 2018/7/14.
 */
public class PicUrl {


    private String url;

    public String getUrl() {
        return "http://193.112.79.134:8080/" + url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
