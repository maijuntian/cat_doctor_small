package com.mai.cat_door_small.http;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;

import com.mai.cat_door_small.Constant;
import com.mai.cat_door_small.bean.CDRespone;
import com.mai.cat_door_small.bean.PicUrl;
import com.mai.cat_door_small.utils.BitmapUtils;
import com.mai.xmai_fast_lib.basehttp.BaseRetrofitService;
import com.mai.xmai_fast_lib.basehttp.MParams;
import com.mai.xmai_fast_lib.basehttp.UploadListener;
import com.mai.xmai_fast_lib.exception.ServerException;
import com.mai.xmai_fast_lib.utils.MLog;
import com.mai.xmai_fast_lib.utils.SharedPreferencesHelper;

import java.io.File;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Created by mai on 2017/11/20.
 */
public class CatDoctorApi extends BaseRetrofitService<CatDoctorService> {

    static CatDoctorApi instance;

    ProgressDialog pdLoading;

    public static CatDoctorApi getInstance() {
        if (instance == null) {
            synchronized (CatDoctorApi.class) {
                if (instance == null) {
                    instance = new CatDoctorApi();
                }
            }
        }
        return instance;
    }

    @Override
    protected String getBaseUrl() {
        return "http://apisail.healthmall.cn/api/";
//        return "http://dev-apisail.healthmall.cn/api/";
    }


    @Override
    protected void showDialog(Context context) {
    }

    protected void showDialog(Context context, String msg) {
        if (pdLoading != null && pdLoading.isShowing())
            pdLoading.dismiss();
        pdLoading = new ProgressDialog(context);
        pdLoading.setCancelable(false);
        pdLoading.setMessage(msg);
        pdLoading.show();
    }

    @Override
    protected void dismissDialog() {
    }

    /**
     * loading框
     *
     * @param observable
     * @param context
     */
    protected <M> Observable<M> showDialog(Observable<M> observable, final String msg, final Context context) {
        return observable.doOnSubscribe(new Action0() {
            @Override
            public void call() {
                showDialog(context, msg);
            }
        }).doOnCompleted(new Action0() {
            @Override
            public void call() {
                dismissDialog();
            }
        });
    }


    /**
     * 检查返回值
     *
     * @param observable
     * @param <M>
     */
    protected <M> Observable<M> checkResult2(Observable<CDRespone<M>> observable) {
        return observable.map(new Func1<CDRespone<M>, M>() {
            @Override
            public M call(CDRespone<M> response) {
                MLog.log("访问结果", response.toString());
                if (response.getCode() != 2000) {
                    ServerException serverException = new ServerException(response.getMsg());
                    serverException.setCode(response.getCode());
                    throw serverException;
                }
                return response.getData();
            }
        }).subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread());
    }


    protected <M> Observable<M> checkAll2(Observable<CDRespone<M>> observable, Context context) {
        return checkError(showDialog(checkNetWork(checkResult2(observable), context), context), context);
    }

    protected <M> Observable<M> checkNoDialog(Observable<CDRespone<M>> observable, Context context) {
        return checkError(checkNetWork(checkResult2(observable), context), context);
    }

    protected <M> Observable<M> checkNoResult(Observable<M> observable, Context context) {
        return checkError(showDialog(checkNetWork(observable.subscribeOn(Schedulers.newThread()).observeOn(AndroidSchedulers.mainThread()), context), context), context);
    }

    protected <M> Observable<M > checkNoDialog2(Observable<CDRespone<M>> observable, Context context) {
        return checkNetWork(checkResult2(observable), context);
    }

    //请求接口

    public Observable<PicUrl> faceTonUpload(final Bitmap bmp, final String deviceId, final String mallId, final int testType, final Context ctx) {
        return checkNoDialog(Observable.just(bmp).map(new Func1<Bitmap, File>() {
            @Override
            public File call(Bitmap bitmap) {
                String path;
                if (testType == 1) {
                    path = getFaceFilePath();
                } else {
                    path = getTonFilePath();
                }

                BitmapUtils.compressJpgBitmap(bmp, path);
                return new File(path);
            }
        }).flatMap(new Func1<File, Observable<CDRespone<PicUrl>>>() {
            @Override
            public Observable<CDRespone<PicUrl>> call(File file) {

                return mService.faceTonUpload(new MParams().add("img", file).add("pwd", "9daef6de79902510dbd1f7702b179d0d").add("testType", testType).add("source", 0).add("deviceId", deviceId).add("mallId", mallId).getFileParams());
            }
        }), ctx);
    }


    public static String getFaceFilePath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File file = new File(Constant.SDCARD_CACHE_IMG_PATH);
            if (!file.exists()) {
                file.mkdirs();
            }
            File cropFile = new File(file, "face.png");
            return cropFile.getAbsolutePath();
        } else {
            return null;
        }
    }

    public static String getTonFilePath() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File file = new File(Constant.SDCARD_CACHE_IMG_PATH);
            if (!file.exists()) {
                file.mkdirs();
            }
            File cropFile = new File(file, "ton.png");
            return cropFile.getAbsolutePath();
        } else {
            return null;
        }
    }
}
