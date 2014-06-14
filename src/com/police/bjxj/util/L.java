package com.police.bjxj.util;

import android.util.Log;

import com.police.bjxj.config.Setting;

public class L {
	private static final String TAG_GLOBAL = "BJXJ";
	private static final boolean logOpen = Setting.LOG_ENABLE;
	private static long lastTime = 0;

	public static void d(String msg) {
		if (logOpen) {
			Log.d(TAG_GLOBAL, msg);
		}
	}

	public static void d(String tag, String msg) {
		if (logOpen) {
			Log.d(TAG_GLOBAL, tag + "| " + msg);
		}
	}

	public static void e(String msg) {
		if (logOpen) {
			Log.e(TAG_GLOBAL, msg);
		}
	}

	public static void e(String tag, String msg) {
		if (logOpen) {
			Log.e(TAG_GLOBAL, tag + "| " + msg);
		}
	}

	public static void lo(String tag, Object... value) {
		if (!logOpen) return;

		StringBuilder resStr = new StringBuilder();
		for (int i = 0; i < value.length; i++) {
			resStr.append(i != 0 ? " >> " : "" + value[i]);
		}
		Log.d(TAG_GLOBAL, tag + "| " + resStr.toString());
	}

	public static void timeSpan(String tag) {
		if (!logOpen) {
			return;
		}
		Log.i(TAG_GLOBAL, tag + ":" + (System.currentTimeMillis() - lastTime));
		lastTime = System.currentTimeMillis();
	}
}
