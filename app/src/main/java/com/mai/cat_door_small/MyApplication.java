package com.mai.cat_door_small;

import android.app.Activity;
import android.app.Application;

import com.mai.cat_door_small.base.BaseActivity;
import com.mai.cat_door_small.serialport.SerialPortEngineSmall;
import com.mai.cat_door_small.utils.VoiceMamanger;
import com.mai.xmai_fast_lib.base.BaseApplication;
import com.mai.xmai_fast_lib.utils.XAppManager;

/**
 * Created by maijuntian on 2018/7/13.
 */
public class MyApplication extends BaseApplication{

    private static MyApplication instance;

    public static MyApplication get() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        VoiceMamanger.init(this);

        SerialPortEngineSmall.getInstance();
    }

    @Override
    public String getBuglyAppid() {
        return null;
    }

    public void serialPortSmallCallBack(final String msg) {
        XAppManager.getInstance().doInAllActivity(new XAppManager.DoAllActivityListener() {
            @Override
            public void doAll(Activity act) {
                ((BaseActivity) act).serialPortSmallCallBack(msg);
            }
        });
    }
}
