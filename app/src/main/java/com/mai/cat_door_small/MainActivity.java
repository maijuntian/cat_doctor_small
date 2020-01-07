package com.mai.cat_door_small;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.mai.cat_door_small.base.BaseActivity;
import com.mai.cat_door_small.bean.Message;
import com.mai.cat_door_small.bean.PicUrl;
import com.mai.cat_door_small.bean.ResponeMsg;
import com.mai.cat_door_small.http.CatDoctorApi;
import com.mai.cat_door_small.serialport.SerialPortEngineSmall;
import com.mai.cat_door_small.utils.BitmapUtils;
import com.mai.cat_door_small.wifi.XPGWifiAdmin;
import com.mai.xmai_fast_lib.basehttp.UploadListener;
import com.tencent.cos.xml.CosXmlService;
import com.tencent.cos.xml.CosXmlServiceConfig;
import com.tencent.cos.xml.exception.CosXmlClientException;
import com.tencent.cos.xml.exception.CosXmlServiceException;
import com.tencent.cos.xml.listener.CosXmlProgressListener;
import com.tencent.cos.xml.listener.CosXmlResultListener;
import com.tencent.cos.xml.model.CosXmlRequest;
import com.tencent.cos.xml.model.CosXmlResult;
import com.tencent.cos.xml.model.bucket.PutBucketRequest;
import com.tencent.cos.xml.model.bucket.PutBucketResult;
import com.tencent.cos.xml.transfer.COSXMLUploadTask;
import com.tencent.cos.xml.transfer.TransferConfig;
import com.tencent.cos.xml.transfer.TransferManager;
import com.tencent.qcloud.core.auth.QCloudCredentialProvider;
import com.tencent.qcloud.core.auth.ShortTimeCredentialProvider;
import com.wonderkiln.camerakit.CameraKitEventCallback;
import com.wonderkiln.camerakit.CameraKitImage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import rx.functions.Action1;

public class MainActivity extends BaseActivity<MainDelegate> {

    XPGWifiAdmin xpgWifiAdmin;

    private final String FACE = "FACE";
    private final String TON = "TON";
    private final String FACE_TON = "FACE_TON";
    private final String STP = "STP";
    private final String WIFI = "WIFI";

    private int currCaptureType = -1; // 1 面部图片， 2舌头图片

    private Message getMsg;

    private List<ResponeMsg> responeMsgs = new ArrayList<>();

    CosXmlService cosXmlService;


    String bucket = "panda-1257270219"; //格式：BucketName-APPID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                }
            }
        }

        xpgWifiAdmin = XPGWifiAdmin.getInstance(this);
        xpgWifiAdmin.openWifi();

        initBucket();


        viewDelegate.setCaptureCallback(new CameraKitEventCallback<CameraKitImage>() {
            @Override
            public void callback(final CameraKitImage cameraKitImage) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap bmp = cameraKitImage.getBitmap();
                        viewDelegate.showExample(bmp);

                        String path;
                        if (currCaptureType == 1) {
                            path = CatDoctorApi.getFaceFilePath();
                        } else {
                            path = CatDoctorApi.getTonFilePath();
                        }

                        BitmapUtils.compressJpgBitmap(bmp, path);

                        upload(path, new Action1<String>() {
                            @Override
                            public void call(final String url) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (currCaptureType == 1 && getMsg.getType().equals(FACE_TON)) { //继续拍摄舌相
                                            responeMsgs.add(new ResponeMsg(FACE, url));
                                            currCaptureType = 2;
                                            viewDelegate.startTonCamera();
                                        } else { //拍摄完成
                                            responeMsgs.add(new ResponeMsg(currCaptureType == 1 ? FACE : TON, url));
                                            SerialPortEngineSmall.getInstance().sendMsg(new Gson().toJson(responeMsgs));
                                            viewDelegate.stopCamera();
                                        }
                                    }
                                });
                            }
                        });
                    }
                });

            }
        });
        hideStatusNavigationBar();
        /*new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currCaptureType = 1;
                        getMsg = new Message(FACE_TON, "feifej", "321321");
                        getMsg.setUserType(1);
                        viewDelegate.startFaceCamera();
                    }
                });
            }
        }, 3000);*/
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
            case WIFI:
                connectWifi(getMsg.getSsid(), getMsg.getPwd());
                break;
        }
    }

    private void connectWifi(String ssid, String pwd) {
        xpgWifiAdmin.connectWifi(ssid, pwd);
    }

    private void hideStatusNavigationBar() {
        int uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN //hide statusBar
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION; //hide navigationBar
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
    }


    private void initBucket() {

        String region = "ap-guangzhou";

        //创建 CosXmlServiceConfig 对象，根据需要修改默认的配置参数
        CosXmlServiceConfig serviceConfig = new CosXmlServiceConfig.Builder()
                .setRegion(region)
                .isHttps(true) // 使用 https 请求, 默认 http 请求
                .setDebuggable(true)
                .builder();

        String secretId = "AKIDjFVUEmFUTVNK3Y9nX2HK8g4GJBoB5BwT"; //永久密钥 secretId
        String secretKey = "RcXDzYrulOmwrX1FDwQhFZgdOzN5brzs"; //永久密钥 secretKey

        /**
         * 初始化 {@link QCloudCredentialProvider} 对象，来给 SDK 提供临时密钥。
         * @parma secretId 永久密钥 secretId
         * @param secretKey 永久密钥 secretKey
         * @param keyDuration 密钥有效期,单位为秒
         */
        QCloudCredentialProvider credentialProvider = new ShortTimeCredentialProvider(secretId,
                secretKey, Integer.MAX_VALUE);

        cosXmlService = new CosXmlService(this, serviceConfig, credentialProvider);

        PutBucketRequest putBucketRequest = new PutBucketRequest(bucket);
        //发送请求
        cosXmlService.putBucketAsync(putBucketRequest, new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest request, CosXmlResult result) {
                // todo Put Bucket success
                PutBucketResult putBucketResult = (PutBucketResult) result;
                log(putBucketResult.printResult());
            }

            @Override
            public void onFail(CosXmlRequest cosXmlRequest, CosXmlClientException clientException, CosXmlServiceException serviceException) {
                // todo Put Bucket failed because of CosXmlClientException or CosXmlServiceException...
            }
        });
    }


    private void upload(String srcPath, final Action1<String> action) {
        // 初始化 TransferConfig
        TransferConfig transferConfig = new TransferConfig.Builder().build();

        //初始化 TransferManager
        TransferManager transferManager = new TransferManager(cosXmlService, transferConfig);
//        微信用户舌诊：wxuser-[id]/tongueimg/filename.png,示例：
//        wxuser-1/tongueimg/wxf6b91ba0966277bd.o6zAJs0Y7qMlYcOF7fTkmchaCqK4.z5gIhzaZA1tG754336c35612032f06385abb48fa0a4a.png
//        微信用户面诊：wxuser-[id]/faceimg/filename.png,示例：
//        wxuser-1/faceimg/wxf6b91ba0966277bd.o6zAJs0Y7qMlYcOF7fTkmchaCqK4.z5gIhzaZA1tG754336c35612032f06385abb48fa0a4a.png
//
//        临时用户舌诊：relative-[id]/ tongueimg /filename.png
//        示例：
//        relative-1/tongueimg/wxf6b91ba0966277bd.o6zAJs0Y7qMlYcOF7fTkmchaCqK4.z5gIhzaZA1tG754336c35612032f06385abb48fa0a4a.png
//        临时用户面诊：relative-[id]/ faceimg /filename.png
//        示例：
//        relative-1/faceimg/wxf6b91ba0966277bd.o6zAJs0Y7qMlYcOF7fTkmchaCqK4.z5gIhzaZA1tG754336c35612032f06385abb48fa0a4a.png

        String cosPath = (getMsg.getUserType() == 1 ? "wxuser" : "relative") + "-" + getMsg.getMallId() + "/tongueimg/" + getRandomString(20) + ".png"; //即对象到 COS 上的绝对路径, 格式如 cosPath = "text.txt";
        String uploadId = null; //若存在初始化分片上传的 UploadId，则赋值对应 uploadId 值用于续传，否则，赋值 null。
//上传对象
        COSXMLUploadTask cosxmlUploadTask = transferManager.upload(bucket, cosPath, srcPath, uploadId);

        cosxmlUploadTask.setCosXmlResultListener(new CosXmlResultListener() {
            @Override
            public void onSuccess(CosXmlRequest request, CosXmlResult result) {
                COSXMLUploadTask.COSXMLUploadTaskResult cOSXMLUploadTaskResult = (COSXMLUploadTask.COSXMLUploadTaskResult) result;
                log("Success: " + cOSXMLUploadTaskResult.printResult() + "   " + cOSXMLUploadTaskResult.accessUrl);

                action.call(cOSXMLUploadTaskResult.accessUrl);

            }

            @Override
            public void onFail(CosXmlRequest request, CosXmlClientException exception, CosXmlServiceException serviceException) {
                log("Failed: " + (exception == null ? serviceException.getMessage() : exception.toString()));
                showToast("上传出错...");
            }
        });
    }

    public String getRandomString(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(62);
            sb.append(str.charAt(number));
        }
        return sb.toString();
    }
}
