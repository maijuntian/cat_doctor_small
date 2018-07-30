package com.mai.cat_door_small;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.mai.cat_door_small.base.BaseActivity;
import com.mai.cat_door_small.bean.Message;
import com.mai.cat_door_small.bean.PicUrl;
import com.mai.cat_door_small.bean.ResponeMsg;
import com.mai.cat_door_small.http.CatDoctorApi;
import com.mai.cat_door_small.serialport.SerialPortEngineSmall;
import com.mai.xmai_fast_lib.basehttp.UploadListener;
import com.wonderkiln.camerakit.CameraKitEventCallback;
import com.wonderkiln.camerakit.CameraKitImage;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import rx.functions.Action1;

public class MainActivity extends BaseActivity<MainDelegate> {

    private final String FACE = "FACE";
    private final String TON = "TON";
    private final String FACE_TON = "FACE_TON";
    private final String STP = "STP";

    private int currCaptureType = -1; // 1 面部图片， 2舌头图片

    private Message getMsg;

    private List<ResponeMsg> responeMsgs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewDelegate.setCaptureCallback(new CameraKitEventCallback<CameraKitImage>() {
            @Override
            public void callback(final CameraKitImage cameraKitImage) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bmp = cameraKitImage.getBitmap();
                        viewDelegate.showExample(bmp);
                        CatDoctorApi.getInstance().faceTonUpload(bmp, getMsg.getDeviceId(), getMsg.getMallId(), currCaptureType, MainActivity.this).subscribe(new Action1<PicUrl>() {
                            @Override
                            public void call(PicUrl picUrl) {
                                if (currCaptureType == 1 && getMsg.getType().equals(FACE_TON)) { //继续拍摄舌相
                                    responeMsgs.add(new ResponeMsg(FACE, picUrl.getUrl()));
                                    currCaptureType = 2;
                                    viewDelegate.startTonCamera();
                                } else { //拍摄完成
                                    responeMsgs.add(new ResponeMsg(currCaptureType == 1 ? FACE : TON, picUrl.getUrl()));
                                    SerialPortEngineSmall.getInstance().sendMsg(new Gson().toJson(responeMsgs));
                                    viewDelegate.stopCamera();
                                }
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                throwable.printStackTrace();
                                if (currCaptureType == 1) {
                                    viewDelegate.startFaceCamera();
                                } else {
                                    viewDelegate.startTonCamera();
                                }
                            }
                        });
                    }
                });

            }
        });
        hideStatusNavigationBar();
       /* new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        serialPortSmallCallBack(new Gson().toJson(new Message(FACE_TON, "2047279016", "BC002004")));
                    }
                });
            }
        }, 10000);*/
    }

    @Override
    public void serialPortSmallCallBack(String msg) {
        super.serialPortSmallCallBack(msg);

        getMsg = new Gson().fromJson(msg, Message.class);
        responeMsgs.clear();
        switch (getMsg.getType()) {
            case FACE:
                currCaptureType = 1;
                viewDelegate.startFaceCamera();
                break;
            case TON:
                currCaptureType = 2;
                viewDelegate.startTonCamera();
                break;
            case FACE_TON:
                currCaptureType = 1;
                viewDelegate.startFaceCamera();
                break;
            case STP:
                viewDelegate.stopCamera();
                break;
        }
    }

    private void hideStatusNavigationBar() {
        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN //hide statusBar
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION; //hide navigationBar
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
    }
}
