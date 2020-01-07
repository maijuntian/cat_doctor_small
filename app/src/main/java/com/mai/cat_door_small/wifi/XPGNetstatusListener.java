/**
 * 
 */
package com.mai.cat_door_small.wifi;

import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;

/**
 * 
 * @author Derek
 *
 */
public interface XPGNetstatusListener {
	
	/**
	 * 联网形式变化
	 */
	void netStatusChange(NetworkInfo netInfo, WifiInfo currentWifi);
	
	/**
	 * 网络状况
	 */
	void conntivityStatusChange(boolean isConnectivity);
	
	void wifiStatus(int status);
}
