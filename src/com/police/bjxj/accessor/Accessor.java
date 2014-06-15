package com.police.bjxj.accessor;

import net.tsz.afinal.FinalHttp;
import net.tsz.afinal.core.AsyncTask;
import net.tsz.afinal.http.AjaxCallBack;
import net.tsz.afinal.http.AjaxParams;

import org.json.JSONException;
import org.json.JSONObject;

import com.police.bjxj.config.Setting;
import com.police.bjxj.util.L;
import com.police.bjxj.util.SercurityUtil;

@SuppressWarnings("rawtypes")
public class Accessor implements IAccessor {
	private static final String TAG = "Accessor";

	/** 客户端：symbian/android/iphone/ipad/wp7/win8 */
	protected static final String PARAMS_REQUEST_CLIENT = "client";

	/** 客户端API版本号：1.0.0 */
	protected static final String PARAMS_REQUEST_APIVER = "apiver";

	/** 客户端的版本号：2.0.0 */
	protected static final String PARAMS_REQUEST_VERSION = "version";
	/** 用户的设备的udid(unique device identifier) */
	protected static final String PARAMS_REQUEST_UDID = "udid";

	private static final String PARAMS_RESULT_SUCC = "succ";
	private static final String PARAMS_RESULT_ERROR_CODE = "errorcode";
	private static final String PARAMS_RESULT_ERROR_DESC = "desc";

	private AjaxCallBack uiCallback;

	public Accessor(AjaxCallBack callback) {
		this.uiCallback = callback;
	}

	/** 正常callback的包装, 为了统一处理请求发送成功, 但服务端返回错误的情况. 这类情况一般由于登陆过期,参数错误等. */
	private AjaxCallBack wrapperCallBack = new AjaxCallBack() {

		/** 将服务端主动返回的错误情况重新回调到onFailure上 */
		@Override
		public void onSuccess(Object content) {
			if (content instanceof String) {
				try {
					JSONObject jsonObject = new JSONObject((String) content);

					boolean succ = jsonObject.optBoolean(PARAMS_RESULT_SUCC, true);
					if (!succ) {
						String errorDesc = jsonObject.optString(PARAMS_RESULT_ERROR_DESC);
						int errorCode = jsonObject.optInt(PARAMS_RESULT_ERROR_CODE);
						BjjxException exception = new BjjxException(errorCode, errorDesc);
						onFailure(exception, jsonObject);
						return;
					}
				} catch (JSONException e) {
					e.printStackTrace();
					BjjxException exception = new BjjxException(ERROR_DATA_ERROR, "response is no json object");
					onFailure(exception, content);
					return;
				}
			}

			L.d(TAG, "onSucess, content=" + content);
			if (uiCallback != null) {
				uiCallback.onSuccess(content);
			}
		}

		@Override
		public void onStart() {
			if (uiCallback != null) {
				uiCallback.onStart();
			}
		}

		@Override
		public void onFinish() {
			if (uiCallback != null) {
				uiCallback.onFinish();
			}
		}

		@Override
		public void onFailure(Exception ex, Object attach) {
			L.d(TAG, "onFailure, content=" + attach);
			if (uiCallback != null) {
				uiCallback.onFailure(ex, attach);
			}
		}

		@Override
		public void onLoading(long count, long current) {
			if (uiCallback != null) {
				uiCallback.onLoading(count, current);
			}
		}
	};

	/** 获取网络请求参数（添加公共请求参数）
	 * 
	 * @return */
	private void addCommonParams(AjaxParams parmas) {
		parmas.put(PARAMS_REQUEST_CLIENT, Setting.CLIENT);
		parmas.put(PARAMS_REQUEST_APIVER, Setting.APIVER);
		parmas.put(PARAMS_REQUEST_VERSION, Setting.VERSION_NAME);
		parmas.put(PARAMS_REQUEST_UDID, Setting.DEVICE_ID);
	}

	/** 获取分类专辑列表 */
	@Override
	public AsyncTask getAlbumList() {
		String url = Setting.HOST_RELEASE + "lib/LibraryMess.do?method=appQueryList";
		FinalHttp http = new FinalHttp();
		AjaxParams params = new AjaxParams();
		addCommonParams(params);

		return http.get(url, params, wrapperCallBack);
	}

	@Override
	public AsyncTask getBookList(long albumId) {
		String url = Setting.HOST_RELEASE + "lib/LibraryMess.do?method=SearchLibrarySortBook";

		FinalHttp http = new FinalHttp();
		AjaxParams params = new AjaxParams("LibrarySortId", albumId);
		addCommonParams(params);

		return http.get(url, params, wrapperCallBack);
	}

	@Override
	public AsyncTask search(String key) {
		String url = Setting.HOST_RELEASE + "lib/LibraryMess.do?method=SearchLibraryBook";

		FinalHttp http = new FinalHttp();
		AjaxParams params = new AjaxParams("LibraryName", key);
		addCommonParams(params);

		return http.get(url, params, wrapperCallBack);
	}

	@Override
	public AsyncTask getAdvertise() {
		String url = Setting.HOST_RELEASE + "lib/LibraryMess.do?method=advertise";

		FinalHttp http = new FinalHttp();
		AjaxParams params = new AjaxParams();
		addCommonParams(params);

		return http.get(url, params, wrapperCallBack);
	}

	@Override
	public AsyncTask login(String username, String pwd) {
		String url = Setting.HOST_RELEASE + "readUser/readUserMess.do?method=readerUserLogin";

		FinalHttp http = new FinalHttp();
		AjaxParams params = new AjaxParams();
		params.put("username", username);
		params.put("password", SercurityUtil.encryptToMD5(pwd));
		addCommonParams(params);

		return http.post(url, params, wrapperCallBack);
	}
}
