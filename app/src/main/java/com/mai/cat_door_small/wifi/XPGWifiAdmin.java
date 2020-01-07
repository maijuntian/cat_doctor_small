package com.mai.cat_door_small.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class XPGWifiAdmin {
    public static final int ACTION_CONNECT_TARGET_SSID_SCAN_OVER_TIME = 10001;
    public static final int ACTION_CONNECT_TARGET_SSID_Connect_Success = 10003;
    public static final int ACTION_CONNECT_TARGET_SSID_Connect_Faild = 10004;

    private static XPGWifiAdmin instance;
    private static int Scan_Over_Timer = 1000 * 10 * 3;
    private static final int Scan_Wifi_End = 101;
    // 定义一个WifiManager对象
    private WifiManager mWifiManager;
    // 定义一个WifiInfo对象
    private WifiInfo mWifiInfo;
    // 网络连接列表
    private WifiLock mWifiLock;
    private XPGNetStatusReceiver xpgNetStatusReceiver;
    private XPGNetstatusListener xpgNetStatusListener;
    private XPGWifiScanListener xpgScanListener;
    private boolean isConnectTargetSSID = false;
    private String targetSSID;
    private String targetPassword;
    private Timer timer;
    private Handler uiHandler;
    private TimerTask scanTimerTask;
    private WifiConfiguration lastWifiConfiguration;

    private int wifiState = -1;

    //扫描出的网络连接列表
    private List<ScanResult> mWifiList;
    //网络连接列表  
    private List<WifiConfiguration> mWifiConfigurations;

    private Handler mHandler = new Handler() {

        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {
                case Scan_Wifi_End:
                    List<ScanResult> scanResult = getScanResult();
                    for (ScanResult tempResult : scanResult) {
                        if (tempResult.SSID.equals(targetSSID)) {
                            cancleConnectTargetSSID();
                            boolean connectWifi = connectWifi(tempResult, targetPassword);
                            int result = ACTION_CONNECT_TARGET_SSID_Connect_Success;
                            if (!connectWifi) {
                                result = ACTION_CONNECT_TARGET_SSID_Connect_Faild;
                            }
                            postUIEvent(result);
                            break;
                        }
                    }
            }
        }

    };

    public static XPGWifiAdmin getInstance(Context context) {
        if (instance == null) {
            instance = new XPGWifiAdmin(context);
        }
        return instance;
    }


    private XPGWifiAdmin(Context context) {
        // 取得WifiManager对象
        mWifiManager = (WifiManager) context.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
        // 取得WifiInfo对象
        mWifiInfo = mWifiManager.getConnectionInfo();

        timer = new Timer();

    }

    // 打开wifi
    public void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    // 关闭wifi
    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    //wifi开关状态
    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled() || wifiState == WifiManager.WIFI_STATE_ENABLING || wifiState == WifiManager.WIFI_STATE_ENABLED;
    }

    public void startScan() {
        mWifiManager.startScan();
        //得到扫描结果  
        mWifiList = mWifiManager.getScanResults();

        mWifiInfo = mWifiManager.getConnectionInfo();
        //得到配置好的网络连接  
        mWifiConfigurations = mWifiManager.getConfiguredNetworks();

        if(mWifiConfigurations != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < mWifiConfigurations.size(); i++) {
                WifiConfiguration mScanResult = mWifiConfigurations.get(i);
//        	mWifiManager.removeNetwork(mScanResult.networkId);
                sb = sb.append(mScanResult.BSSID + "  ").append(mScanResult.SSID + "   ")
                        .append(mScanResult.networkId + "   ").append(mScanResult.hiddenSSID + "   ")
                        .append(mScanResult.preSharedKey + "\n\n");
            }
            Log.i("528", "sb:" + sb.toString());
        }
    }

    //得到网络列表  
    public List<ScanResult> getWifiList() {
        return mWifiList;
    }

    public void connectTargetSSID(String ssid, String password) {
        this.targetSSID = ssid;
        this.targetPassword = password;
        isConnectTargetSSID = true;

        startScanTimer();
        startScan();
    }

    private void startScanTimer() {
        if (scanTimerTask == null) {
            scanTimerTask = new TimerTask() {

                @Override
                public void run() {
                    isConnectTargetSSID = false;
                    postUIEvent(ACTION_CONNECT_TARGET_SSID_SCAN_OVER_TIME);
                }
            };

            timer.schedule(scanTimerTask, Scan_Over_Timer);
        }
    }

    private void stopScanTimer() {
        if (scanTimerTask != null) {
            scanTimerTask.cancel();
            scanTimerTask = null;
        }
    }


    public void cancleConnectTargetSSID() {
        isConnectTargetSSID = false;
        stopScanTimer();
    }

    /**
     * 连接到指定wifi
     */
    public boolean connectWifi(ScanResult target, String password) {
        String ssid = target.SSID;
        WifiCipherType cipherType = getWifiCipherType(target.capabilities);
        return connectProcess(ssid, password, cipherType, NetMode.NET_MODE_WIFI);
    }

    /**
     * 连接到指定wifi
     */
    public boolean connectWifi(String ssid, String password) {
        WifiCipherType cipherType = WifiCipherType.Wifi_Cipher_WPA;
        return connectProcess(ssid, password, cipherType, NetMode.NET_MODE_WIFI);
    }

    /**
     * 连接到指定wifi
     */
    public boolean connectWifi(boolean newConnect, ScanResult target, String password) {
        String ssid = target.SSID;
        WifiCipherType cipherType = getWifiCipherType(target.capabilities);
        return connectProcess(newConnect, ssid, password, cipherType, NetMode.NET_MODE_WIFI);
    }

    /**
     * @param ssid
     * @param password
     * @param cipherType 密码类型
     */
    public boolean connectWifi(String ssid, String password, WifiCipherType cipherType) {
        return connectProcess(ssid, password, cipherType, NetMode.NET_MODE_WIFI);
    }

    /**
     * 判断密码类型
     *
     * @param cipherString
     * @return
     */
    private WifiCipherType getWifiCipherType(String cipherString) {
//		WifiCipherType result = WifiCipherType.Wifi_Cipher_Norm ;
        WifiCipherType result = WifiCipherType.Wifi_Cipher_Null;
        cipherString = cipherString.toUpperCase();
        if (cipherString.contains("WPA") || cipherString.contains("WPA2")) {
            result = WifiCipherType.Wifi_Cipher_WPA;
        } else if (cipherString.contains("NULL")) {
            result = WifiCipherType.Wifi_Cipher_Null;
        }
        return result;
    }

    /**
     * 连接过程  改变参数
     *
     * @param ssid
     * @param password
     * @param cipherType
     * @param mode
     */
    private boolean connectProcess(String ssid, String password, WifiCipherType cipherType, NetMode mode) {
        return connectProcess(true, ssid, password, cipherType, mode);
    }

    /**
     * 连接过程
     *
     * @param ssid
     * @param password
     * @param cipherType
     * @param mode
     */
    private boolean connectProcess(boolean newConnect, String ssid, String password, WifiCipherType cipherType, NetMode mode) {

        // 记录上一次的wifiConfigulator
        WifiConfiguration targetWifi = null;
        lastWifiConfiguration = getCurrentWifiConfiguration();

        if (newConnect) {
            targetWifi = createWifiInfo(ssid, password, cipherType, mode);
        } else {
            targetWifi = isExsits(ssid);
            if (targetWifi == null) {
                targetWifi = createWifiInfo(ssid, password, cipherType, mode);
            }
        }

        return addNetWork(targetWifi);
    }

    /**
     * 是否存在网络信息
     *
     * @param str 热点名称
     * @return
     */
    public WifiConfiguration isExsits(String str) {
        Iterator localIterator = this.mWifiManager.getConfiguredNetworks().iterator();
        WifiConfiguration localWifiConfiguration;
        do {
            if (!localIterator.hasNext()) return null;
            localWifiConfiguration = (WifiConfiguration) localIterator.next();
        } while (!localWifiConfiguration.SSID.equals("\"" + str + "\""));
        return localWifiConfiguration;
    }

    /**
     * 获取热点名
     **/
    public String getApSSID() {
        try {
            Method localMethod = this.mWifiManager.getClass().getDeclaredMethod("getWifiApConfiguration", new Class[0]);
            if (localMethod == null) return null;
            Object localObject1 = localMethod.invoke(this.mWifiManager, new Object[0]);
            if (localObject1 == null) return null;
            WifiConfiguration localWifiConfiguration = (WifiConfiguration) localObject1;
            if (localWifiConfiguration.SSID != null) return localWifiConfiguration.SSID;
            Field localField1 = WifiConfiguration.class.getDeclaredField("mWifiApProfile");
            if (localField1 == null) return null;
            localField1.setAccessible(true);
            Object localObject2 = localField1.get(localWifiConfiguration);
            localField1.setAccessible(false);
            if (localObject2 == null) return null;
            Field localField2 = localObject2.getClass().getDeclaredField("SSID");
            localField2.setAccessible(true);
            Object localObject3 = localField2.get(localObject2);
            if (localObject3 == null) return null;
            localField2.setAccessible(false);
            String str = (String) localObject3;
            return str;
        } catch (Exception localException) {
        }
        return null;
    }

    /**
     * 根据wifi信息创建或关闭一个热点
     *
     * @param paramWifiConfiguration
     * @param paramBoolean           关闭标志
     */
    public void createWifiAP(WifiConfiguration paramWifiConfiguration, boolean paramBoolean) {
        try {
            Class localClass = this.mWifiManager.getClass();
            Class[] arrayOfClass = new Class[2];
            arrayOfClass[0] = WifiConfiguration.class;
            arrayOfClass[1] = Boolean.TYPE;
            Method localMethod = localClass.getMethod("setWifiApEnabled", arrayOfClass);
            WifiManager localWifiManager = this.mWifiManager;
            Object[] arrayOfObject = new Object[2];
            arrayOfObject[0] = paramWifiConfiguration;
            arrayOfObject[1] = Boolean.valueOf(paramBoolean);
            localMethod.invoke(localWifiManager, arrayOfObject);
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 创建一个wifi信息
     *
     * @param ssid     名称
     * @param passawrd 密码
     * @param type     是"ap"还是"wifi"
     * @return
     */
    private WifiConfiguration createWifiInfo(String ssid, String passawrd, WifiCipherType cipherType, NetMode type) {
        //配置网络信息类
        WifiConfiguration localWifiConfiguration1 = new WifiConfiguration();
        //设置配置网络属性
        localWifiConfiguration1.allowedAuthAlgorithms.clear();
        localWifiConfiguration1.allowedGroupCiphers.clear();
        localWifiConfiguration1.allowedKeyManagement.clear();
        localWifiConfiguration1.allowedPairwiseCiphers.clear();
        localWifiConfiguration1.allowedProtocols.clear();

        Log.i("528 ", "createWifiInfo:" + ssid);

        if (type == NetMode.NET_MODE_WIFI) { //wifi连接
            localWifiConfiguration1.SSID = "\"" + ssid + "\"";
            WifiConfiguration localWifiConfiguration2 = isExsits(ssid);
            if (localWifiConfiguration2 != null) {
                mWifiManager.removeNetwork(localWifiConfiguration2.networkId); //从列表中删除指定的网络配置网络
            }
            if (cipherType == WifiCipherType.Wifi_Cipher_Null) { //没有密码
                Log.i("528 ", "Wifi_Cipher_Null");
//                localWifiConfiguration1.wepKeys[0] = "";
                localWifiConfiguration1.wepKeys[0] = "\"" + "\"";
                localWifiConfiguration1.allowedKeyManagement.set(0);
                localWifiConfiguration1.wepTxKeyIndex = 0;
            } else if (cipherType == WifiCipherType.Wifi_Cipher_Norm) { //简单密码
                localWifiConfiguration1.hiddenSSID = true;
                localWifiConfiguration1.wepKeys[0] = ("\"" + passawrd + "\"");
            } else { //wap加密
                localWifiConfiguration1.preSharedKey = ("\"" + passawrd + "\"");
                localWifiConfiguration1.hiddenSSID = true;
                localWifiConfiguration1.allowedAuthAlgorithms.set(0);
                localWifiConfiguration1.allowedGroupCiphers.set(2);
                localWifiConfiguration1.allowedKeyManagement.set(1);
                localWifiConfiguration1.allowedPairwiseCiphers.set(1);
                localWifiConfiguration1.allowedGroupCiphers.set(3);
                localWifiConfiguration1.allowedPairwiseCiphers.set(2);
            }
        } else {//"ap" wifi热点
            localWifiConfiguration1.SSID = ssid;
            localWifiConfiguration1.allowedAuthAlgorithms.set(1);
            localWifiConfiguration1.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            localWifiConfiguration1.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            localWifiConfiguration1.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            localWifiConfiguration1.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            localWifiConfiguration1.allowedKeyManagement.set(0);
            localWifiConfiguration1.wepTxKeyIndex = 0;
            if (cipherType == WifiCipherType.Wifi_Cipher_Null) {  //没有密码
                localWifiConfiguration1.wepKeys[0] = "";
                localWifiConfiguration1.allowedKeyManagement.set(0);
                localWifiConfiguration1.wepTxKeyIndex = 0;
            } else if (cipherType == WifiCipherType.Wifi_Cipher_Norm) { //简单密码
                localWifiConfiguration1.hiddenSSID = true;//网络上不广播ssid
                localWifiConfiguration1.wepKeys[0] = passawrd;
            } else if (cipherType == WifiCipherType.Wifi_Cipher_WPA) {//wap加密
                localWifiConfiguration1.preSharedKey = passawrd;
                localWifiConfiguration1.allowedAuthAlgorithms.set(0);
                localWifiConfiguration1.allowedProtocols.set(1);
                localWifiConfiguration1.allowedProtocols.set(0);
                localWifiConfiguration1.allowedKeyManagement.set(1);
                localWifiConfiguration1.allowedPairwiseCiphers.set(2);
                localWifiConfiguration1.allowedPairwiseCiphers.set(1);
            }
        }

        return localWifiConfiguration1;
    }

    /**
     * 检查当前wifi状态
     *
     * @return
     */
    public String checkState() {
        String temp = "";
        switch (mWifiManager.getWifiState()) {
            case 0:
                temp = "WIFI网卡正在关闭";
                break;
            case 1:
                temp = "WIFI网卡不可用";
                break;
            case 2:
                temp = "WIFI网正在打开";
                break;
            case 3:
                temp = "WIFI网卡可用";
                break;
            case 4:
                temp = "未知网卡状态";
                break;

            default:
                break;
        }

        return temp;
    }

    // 锁定wifiLock
    public void acquireWifiLock() {
        mWifiLock.acquire();
    }

    // 解锁wifiLock
    public void releaseWifiLock() {
        // 判断是否锁定
        if (mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    // 创建一个wifiLock
    public void createWifiLock() {
        mWifiLock = mWifiManager.createWifiLock("test");
    }

    // 得到配置好的网络
    public List<WifiConfiguration> getConfiguration() {
        return mWifiManager.getConfiguredNetworks();
    }


    // 指定配置好的网络进行连接
    public void connetionConfiguration(int index) {
        // 连接配置好指定ID的网络
        mWifiManager.enableNetwork(mWifiManager.getConfiguredNetworks().get(index).networkId, true);
    }

    public WifiConfiguration findConnectionConfiguration(int netId) {
        for (WifiConfiguration temp : mWifiManager.getConfiguredNetworks()) {
            if (temp.networkId == netId) {
                return temp;
            }
        }
        return null;
    }

    public WifiConfiguration findConnectionConfiguration(String ssid) {
        for (WifiConfiguration temp : mWifiManager.getConfiguredNetworks()) {
            if (temp.SSID.equals(ssid)) {
                return temp;
            }
        }
        return null;
    }

    public ScanResult findScanResult(String ssid) {
        ScanResult result = null;
        List<ScanResult> scanResults = mWifiManager.getScanResults();
        for (ScanResult scanResult : scanResults) {
            if (ssid.equals(scanResult.SSID)) {
                result = scanResult;
                break;
            }
        }
        return result;
    }


    // 得到网络列表
    public List<ScanResult> getScanResult() {
        return mWifiManager.getScanResults();
    }

    public String getMacAddress() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress();
    }

    public String getBSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID();
    }

    public String getSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getSSID().replace("\"", "");
    }

    public int getIpAddress() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress();
    }

    // 得到连接的ID
    public int getNetWordId() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
    }

    // 得到wifiInfo的所有信息
    public String getWifiInfo() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();
    }

    public WifiInfo getCurrentWifiInfo() {
        return mWifiManager.getConnectionInfo();
    }

    public WifiConfiguration getCurrentWifiConfiguration() {
        WifiInfo currentWifiInfo = getCurrentWifiInfo();
        List<WifiConfiguration> configuration = getConfiguration();

        for (WifiConfiguration wifiConfiguration : configuration) {
            if (wifiConfiguration.SSID.equals(currentWifiInfo.getSSID())) {
                return wifiConfiguration;
            }
        }
        return null;
    }

    // 添加一个网络并连接
    public boolean addNetWork(WifiConfiguration configuration) {
        int wcgId = mWifiManager.addNetwork(configuration);
        Log.i("528", "看看是不是-1：" + wcgId);
        return mWifiManager.enableNetwork(wcgId, true);
    }

    // 断开指定ID的网络
    public void disConnectionWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
    }

    /**
     * 断开当前网络
     */
    public void disConnectionCurrWifi() {
        mWifiManager.disconnect();
    }

    /**
     * 断开当前,并连接到上一次的网络
     */
    public void disConnectionReconnectPrevious() {
        disConnectionCurrWifi();

        if (lastWifiConfiguration != null) {
            addNetWork(lastWifiConfiguration);
        }
    }


    /**
     * 查询是否联网
     *
     * @return
     */
    public boolean checkNetStatus(Context context) {
        if (isMobileConnected(context) || isWifiConnected(context)) {
            return true;
        }
        return false;
    }

    /**
     * 判断wifi是否有连接
     *
     * @param context
     * @return
     */
    public boolean isWifiConnected(Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);


        // wifi
        State wifi = conMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                .getState();

        if (wifi == State.CONNECTED) {
            return true;
        }

        return false;
    }

    /**
     * 判断GPRS 或 3g 是否有开启
     *
     * @param context
     * @return
     */
    public boolean isMobileConnected(Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (conMan == null) return false;
        //mobile 3G Data Network
        NetworkInfo networkInfo = conMan.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (networkInfo == null) return false;
        State mobile = networkInfo.getState();


        //如果3G网络和wifi网络都未连接，且不是处于正在连接状态 则进入Network Setting界面 由用户配置网络连接
        if (mobile == State.CONNECTED) {
            return true;
        }

        return false;
    }


    /**
     * 打开系统无线配置页面
     *
     * @param context
     */
    public void openOSWifiSetting(Context context) {
//		context.startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));//进入无线网络配置
        context.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));  //进入手机中的wifi网络设置界面
    }

    /**
     * 注册广播
     *
     * @param context
     * @param listener
     */
    public void registerXPGNetStatusListener(Context context,
                                             XPGNetstatusListener listener) {
        this.xpgNetStatusListener = listener;
        if (xpgNetStatusReceiver == null) {
            xpgNetStatusReceiver = new XPGNetStatusReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
//		intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
//		intentFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        context.registerReceiver(xpgNetStatusReceiver, intentFilter);
    }

    public void unregisterXPGNetStatusListener(Context context) {
        context.unregisterReceiver(xpgNetStatusReceiver);
        this.xpgNetStatusListener = null;
    }


    /**
     * 网络状态广播
     *
     * @author Derek
     */
    public class XPGNetStatusReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("WifiAction", action);

            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) { // 搜索结果

                if (!isConnectTargetSSID && xpgScanListener != null) {
                    List<ScanResult> scanResults = getmWifiManager().getScanResults();
                    xpgScanListener.wifiScanEnd(scanResults);
                } else {
                    mHandler.obtainMessage(Scan_Wifi_End).sendToTarget();
                }

            } else if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

                boolean isFailOver = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
                boolean noConnectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

                if (xpgNetStatusListener != null) {
                    if (isFailOver || noConnectivity) {
                        xpgNetStatusListener.conntivityStatusChange(false);
                    } else if (!isFailOver && !noConnectivity) {
                        xpgNetStatusListener.conntivityStatusChange(true);
                    }
                }

            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {

                NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

                if (xpgNetStatusListener != null) {
                    xpgNetStatusListener.netStatusChange(netInfo, wifiInfo);
                }

            } else if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) { // wifi 开关状态
                int currWifiStatus = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);

                Log.i("WifiAction", "currWifiStatus:" + currWifiStatus);
                wifiState = currWifiStatus;
                if (xpgNetStatusListener != null) {
                    xpgNetStatusListener.wifiStatus(currWifiStatus);
                }
            } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
                int rssi = intent.getIntExtra(WifiManager.RSSI_CHANGED_ACTION, -1);
            } else if (action.equals(WifiManager.NETWORK_IDS_CHANGED_ACTION)) {

            }
        }
    }

    public void l(String content) {
        Log.i("WifiBox", content);
    }

    public static int getScanOverTimer() {
        return Scan_Over_Timer;
    }

    public static void setScanOverTimer(int scan_Over_Timer) {
        Scan_Over_Timer = scan_Over_Timer;
    }

    public Handler getUiHandler() {
        return uiHandler;
    }

    public void setUiHandler(Handler uiHandler) {
        this.uiHandler = uiHandler;
    }

    private void postUIEvent(int action) {
        if (uiHandler != null) {
            uiHandler.obtainMessage(action).sendToTarget();
        }
    }


    public WifiManager getmWifiManager() {
        return mWifiManager;
    }


    public void setmWifiManager(WifiManager mWifiManager) {
        this.mWifiManager = mWifiManager;
    }


    public XPGWifiScanListener getXpgScanListener() {
        return xpgScanListener;
    }


    public void setXpgScanListener(XPGWifiScanListener xpgScanListener) {
        this.xpgScanListener = xpgScanListener;
    }

}
