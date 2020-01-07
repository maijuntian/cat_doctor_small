package com.mai.cat_door_small.serialport;

import com.mai.cat_door_small.Constant;
import com.mai.cat_door_small.MyApplication;
import com.mai.cat_door_small.utils.Mlog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android_serialport_api.SerialPort;

/**
 * Created by mai on 2017/12/1.
 */
public class SerialPortEngineSmall {

    static SerialPortEngineSmall instance;

    public static SerialPortEngineSmall getInstance() {
        if (instance == null) {
            instance = new SerialPortEngineSmall();
        }
        return instance;
    }

    protected SerialPort mSerialPort;
    protected OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;

    public static final String SUFFIX = "*";

    private StringBuilder sbMsg = new StringBuilder();

    /**
     * 读取硬件上报数据线程
     */
    private class ReadThread extends Thread {

        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                int size;
                try {
                    byte[] buffer = new byte[8];
                    if (mInputStream == null) return;
                    size = mInputStream.read(buffer);
                    if (size > 0) {

                        String result = new String(buffer, 0, size);
                        sbMsg.append(result);
                        Mlog.log("收到回调,大小" + size + "--->" + result + "   结束没：" + result.endsWith(SUFFIX));
                        if (result.endsWith(SUFFIX)) {
                            String finalResult = sbMsg.toString().replace(SUFFIX,"").trim();
                            sbMsg.delete(0, sbMsg.length());
                            Mlog.log("最后结果--->" + finalResult.length() + "--->" + finalResult);
                            MyApplication.get().serialPortSmallCallBack(finalResult);

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    SerialPortEngineSmall() {
        try {
            mSerialPort = new SerialPort(new File(Constant.SERIAL_PATH_SMALL), Constant.BAUDRATE, 0);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

			/* Create a receiving thread */
            mReadThread = new ReadThread();
            mReadThread.start();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        if (mOutputStream == null)
            return;
        try {
            Mlog.log("发送信息--->" + msg);
            msg += SUFFIX;
            mOutputStream.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
