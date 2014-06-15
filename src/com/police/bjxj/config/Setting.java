package com.police.bjxj.config;

/** 应用的全局设置 */
public class Setting {

	/* ==== host server, 不要修改产量的命名, 会影响到打包时候的取值替换 ==== */
	public static final String HOST_RELEASE = "http://112.126.66.117:8080/pap/";// release
	public static final String HOST_HTTPS = "https://112.126.66.117:8080/pap/";// release
	public static String HOST = HOST_RELEASE;// staging

	/* */
	public static final String GLOBAL_DB_NAME = "bjjx";

	/* ==== 是否开启自检测升级功能,默认不开启,由MovieApplication初始化时设置==== */
	public static boolean NEEDUPDATE = false;

	/* ==== 接口共有参数 ====*/
	public static final String CLIENT = "android";// 终端类型
	public static final String APIVER = "1.0.0";// 接口版本
	public static final String APP = "bjjx";// 应用名称
	public static String DEVICE_ID;// 设备唯一id,由MovieApplication初始化时设置
	public static String CHANNEL;// 渠道名,由MovieApplication初始化时设置
	public static String VERSION_NAME;// 客户端版本,由MovieApplication初始化时设置
	public static int SCREEN_WIDTH;// 屏幕宽度,由MovieApplication初始化时设置
	public static int SCREEN_HEIGHT;// 屏幕高度,由MovieApplication初始化时设置
	public static double DENSITY = 1;

	/** 升级下载保存目录 */
	public static final String DOWNLOAD_DIR = "/BJXJDownload";
	
	/** 图片sd卡缓存目录 */
	public static final String IMG_CACHE_DIR = DOWNLOAD_DIR + "/cover";

	/** 清除缓存的时间间隔，15天 */
	public final static long CLEAR_CACHE_SPAN = 15 * 24 * 3600 * 1000;

	/** 数据上报开关 */
	public static final boolean REPORT_ENABLE = false;
	
	/** log开关 */
	public static final boolean LOG_ENABLE = true;
}