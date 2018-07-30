package com.mai.cat_door_small.utils;

import android.util.Log;

/**
 * Created by maijuntian on 2018/7/13.
 */
public class Mlog {
    public static String TAG = "mai";
    private static boolean isLog = true;

    public static void log(String msg){
        if(isLog)
            Log.e("Mlog-->" + TAG, msg);
    }

    public static void log(String tag, String msg){
        if(isLog)
            Log.e("Mlog-->" + tag + "--->", msg);
    }


    public static void setIsLog(boolean isLog) {
        Mlog.isLog = isLog;
    }
}
