package com.police.bjxj.util;

import java.util.UUID;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Base64;

public class DeviceUtil {
	/**
	 * 获取mac地址
	 * 
	 * @param context
	 * @return
	 */
	public static String getMac(Context context) {
		if (context == null) {
			return "";
		}

		String mac = null;
		try {
			WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			WifiInfo info = wifi.getConnectionInfo();
			if (null != info) {
				mac = info.getMacAddress();
			}
		} catch (Exception e) {
		}

		return null != mac ? Base64.encodeToString(mac.getBytes(), Base64.DEFAULT) : "";
	}

	/**
	 * 获取设备唯一标识
	 * 
	 * @param context
	 * @return
	 */
	public synchronized static String getUUID(Context context) {
		if (context == null) {
			return "";
		}

		final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		final String tmDevice, tmSerial, androidId;
		tmDevice = "" + tm.getDeviceId();
		tmSerial = "" + tm.getSimSerialNumber();
		androidId = ""
				+ android.provider.Settings.Secure.getString(context.getContentResolver(),
						android.provider.Settings.Secure.ANDROID_ID);
		UUID uuid = new UUID(androidId.hashCode(), ((long) tmDevice.hashCode() << 32) | tmSerial.hashCode());
		return uuid.toString();
	}

	/**
	 * 获取设备ID,友盟统计分析使用
	 * 
	 * @param context
	 * @return
	 */
	public static String getDevId(Context context) {
		final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		final String tmDevice = tm.getDeviceId();
		return tmDevice;
	}

	/**
	 * 渠道号
	 * 
	 * @return
	 */
	public static String getChannel(Context context) {
		String metaData = null;
		try {
			ApplicationInfo info = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			if(info!=null && info.metaData!=null){
				metaData = info.metaData.getString("UMENG_CHANNEL");
			}else{
				metaData = "unknow";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return metaData;
	}

	/**
	 * Return true if sdcard is available.
	 * 
	 * @return
	 */
	public static boolean isSdCardExist() {
		return android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
	}

	/**
	 * get application version name defined in manifest
	 * @param context
	 * @return
	 */
	public static String getVersionName(Context context) {
		PackageManager pm = context.getPackageManager();
		String verName = null;
		try {
			PackageInfo pInfo;
			pInfo = pm.getPackageInfo(context.getPackageName(), 0);
			verName = pInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return verName;
	}
	
	/** * 根据手机的分辨率从 dp 的单位 转成为 px(像素) */
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	/** * 根据手机的分辨率从 px(像素) 的单位 转成为 dp */
	public static int px2dip(Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}
}
