package com.mai.cat_door_small.wifi;

import android.net.wifi.ScanResult;

import java.util.List;

public interface XPGWifiScanListener {
    /**
     * 扫描成功
     */
    void wifiScanEnd(List<ScanResult> scanResults);
}
