package com.mai.cat_door_small;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.mai.xmai_fast_lib.mvvm.view.AppDelegate;
import com.wonderkiln.camerakit.CameraKitEventCallback;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraView;

import butterknife.Bind;

/**
 * Created by maijuntian on 2018/7/13.
 */
public class MainDelegate extends AppDelegate {
    @Bind(R.id.iv_eyes)
    ImageView ivEyes;
    @Bind(R.id.cv_camera)
    CameraView cvCamera;
    @Bind(R.id.iv_bg)
    ImageView ivBg;
    @Bind(R.id.tv_tip)
    TextView tvTip;
    @Bind(R.id.rl_camera)
    RelativeLayout rlCamera;

    long DELAY = 2000;

    CameraKitEventCallback<CameraKitImage> captureCallback;
    @Bind(R.id.iv_example)
    ImageView ivExample;

    public void setCaptureCallback(CameraKitEventCallback<CameraKitImage> captureCallback) {
        this.captureCallback = captureCallback;
    }

    public void showExample(Bitmap bmp) {
        ivExample.setVisibility(View.VISIBLE);
        ivExample.setImageBitmap(bmp);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            String tip = tvTip.getText().toString().trim();
            switch (tip) {
                case "准备":
                    tvTip.setText("3");
                    handler.sendEmptyMessageDelayed(0, DELAY);
                    break;
                case "3":
                    tvTip.setText("2");
                    handler.sendEmptyMessageDelayed(0, DELAY);
                    break;
                case "2":
                    tvTip.setText("1");
                    handler.sendEmptyMessageDelayed(0, DELAY);
                    break;
                case "1":
                    tvTip.setText("");
                    cvCamera.captureImage(captureCallback);
                    break;
            }
        }
    };

    @Override
    public int getRootLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    public void initWidget() {
        Glide.with(mContext).load(R.mipmap.eyes2).into(ivEyes);
    }

    @Override
    public void onDestory() {
        super.onDestory();
    }

    public void startFaceCamera() {

        ivExample.setVisibility(View.GONE);
        ivBg.setVisibility(View.VISIBLE);
        rlCamera.setVisibility(View.VISIBLE);
        ivBg.setImageResource(R.mipmap.face_bg);
        ivEyes.setVisibility(View.GONE);
        try {
            cvCamera.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        tvTip.setText("准备");

        handler.sendEmptyMessageDelayed(0, DELAY);

    }

    public void startTonCamera() {

        ivExample.setVisibility(View.GONE);
        rlCamera.setVisibility(View.VISIBLE);
        ivBg.setVisibility(View.VISIBLE);
        ivBg.setImageResource(R.mipmap.ton_bg);
        ivEyes.setVisibility(View.GONE);
        try {
            cvCamera.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        tvTip.setText("准备");
        handler.sendEmptyMessageDelayed(0, 1000);
    }

    public void stopCamera() {

        rlCamera.setVisibility(View.GONE);
        ivBg.setVisibility(View.GONE);
        handler.removeMessages(0);
        ivEyes.setVisibility(View.VISIBLE);
        try {
            cvCamera.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
