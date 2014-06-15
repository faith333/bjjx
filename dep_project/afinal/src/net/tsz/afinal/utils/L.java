package net.tsz.afinal.utils;

import android.util.Log;

public class L {
	private static final String TAG_GLOBAL = "aFianl";
	private static final boolean logOpen = true;
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
