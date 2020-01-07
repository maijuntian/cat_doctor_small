package com.mai.cat_door_small.base;

import com.mai.xmai_fast_lib.mvvm.presenter.ActivityPresenter;
import com.mai.xmai_fast_lib.mvvm.view.IDelegate;

/**
 * Created by mai on 2017/11/10.
 */
public class BaseActivity<T extends IDelegate> extends ActivityPresenter<T> {

    public void serialPortSmallCallBack(String msg) {  //小屏串口通信的回调
    }

}
