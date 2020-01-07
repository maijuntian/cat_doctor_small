package com.mai.cat_door_small.bean;

/**
 * Created by mai on 2017/11/20.
 */
public class CDRespone<T> {

    private int code;
    private String msg;
    private T data;
    private boolean succeed;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSucceed() {
        return succeed;
    }

    public void setSucceed(boolean succeed) {
        this.succeed = succeed;
    }

}
