package com.mai.cat_door_small.bean;

/**
 * Created by maijuntian on 2018/7/14.
 */
public class Message {
    String type;
    String mallId;
    String deviceId;

    public Message(String type, String mallId, String deviceId) {
        this.type = type;
        this.mallId = mallId;
        this.deviceId = deviceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMallId() {
        return mallId;
    }

    public void setMallId(String mallId) {
        this.mallId = mallId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
