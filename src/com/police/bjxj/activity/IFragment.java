package com.police.bjxj.activity;

import android.os.Bundle;

/**
 * activity和Fragment通信的接口定义
 * @author RR
 *
 */
public interface IFragment {
	/**
	 * 重新加载fragment的数据
	 * ps: 例如当用户登陆成功后,跳转回到包含fragment的activity时.
	 */
	void reloadData();
	
	boolean onBackPress();
	
	/**
	 * 设置参数. 取代fragment默认的setArguments.
	 * @param b
	 */
	void setParams(Bundle b);
}
