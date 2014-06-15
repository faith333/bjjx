/**
 * Copyright (c) 2012-2013, Michael Yang 杨福海 (www.yangfuhai.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tsz.afinal;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import net.tsz.afinal.core.AsyncTask;
import net.tsz.afinal.http.AjaxCallBack;
import net.tsz.afinal.http.AjaxParams;
import net.tsz.afinal.http.HttpHandler;
import net.tsz.afinal.http.RetryHandler;
import net.tsz.afinal.http.SyncRequestHandler;
import net.tsz.afinal.utils.L;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpVersion;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;

import android.util.Log;

public class FinalHttp {
	private static final String TAG = "FinalHttp";

	private static final int DEFAULT_SOCKET_BUFFER_SIZE = 8 * 1024; // 8KB
	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";

	private static int maxConnections = 10; // http请求最大并发连接数
	private static int socketTimeout = 10 * 1000; // 超时时间，默认10秒
	private static int maxRetries = 3;// 错误尝试次数，错误异常表请在RetryHandler添加
	private static int httpThreadCount = 3;// http线程池数量
	private static int downloadThreadCount = 5;// 下载线程池数量

	private final DefaultHttpClient httpClient;
	private final HttpContext httpContext;
	private String charset = "utf-8";

	private final Map<String, String> clientHeaderMap;

	private static final ThreadFactory sThreadFactory = new ThreadFactory() {
		private final AtomicInteger mCount = new AtomicInteger(1);

		public Thread newThread(Runnable r) {
			Thread tread = new Thread(r, "FinalHttp #" + mCount.getAndIncrement());
			tread.setPriority(Thread.NORM_PRIORITY - 1);
			return tread;
		}
	};

	private static final Executor executor = Executors.newFixedThreadPool(httpThreadCount, sThreadFactory);
	private static final Executor downloadExecutor = Executors.newFixedThreadPool(downloadThreadCount, sThreadFactory);

	public FinalHttp() {
		BasicHttpParams httpParams = new BasicHttpParams();

		ConnManagerParams.setTimeout(httpParams, socketTimeout);
		ConnManagerParams.setMaxConnectionsPerRoute(httpParams, new ConnPerRouteBean(maxConnections));
		ConnManagerParams.setMaxTotalConnections(httpParams, 10);

		HttpConnectionParams.setSoTimeout(httpParams, socketTimeout);
		HttpConnectionParams.setConnectionTimeout(httpParams, socketTimeout);
		HttpConnectionParams.setTcpNoDelay(httpParams, true);
		HttpConnectionParams.setSocketBufferSize(httpParams, DEFAULT_SOCKET_BUFFER_SIZE);

		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		ThreadSafeClientConnManager cm = new ThreadSafeClientConnManager(httpParams, schemeRegistry);

		httpContext = new SyncBasicHttpContext(new BasicHttpContext());
		httpClient = new DefaultHttpClient(cm, httpParams);
		httpClient.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(HttpRequest request, HttpContext context) {
				if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
					request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
				}
				for (String header : clientHeaderMap.keySet()) {
					request.addHeader(header, clientHeaderMap.get(header));
				}
			}
		});

		httpClient.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(HttpResponse response, HttpContext context) {
				final HttpEntity entity = response.getEntity();
				if (entity == null) {
					return;
				}
				final Header encoding = entity.getContentEncoding();
				if (encoding != null) {
					for (HeaderElement element : encoding.getElements()) {
						if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
							response.setEntity(new InflatingEntity(response.getEntity()));
							break;
						}
					}
				}
			}
		});

		httpClient.setHttpRequestRetryHandler(new RetryHandler(maxRetries));

		clientHeaderMap = new HashMap<String, String>();

	}

	public HttpClient getHttpClient() {
		return this.httpClient;
	}

	public HttpContext getHttpContext() {
		return this.httpContext;
	}

	public void configCharset(String charSet) {
		if (charSet != null && charSet.trim().length() != 0) this.charset = charSet;
	}

	public void configCookieStore(CookieStore cookieStore) {
		httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
	}

	public void configUserAgent(String userAgent) {
		HttpProtocolParams.setUserAgent(this.httpClient.getParams(), userAgent);
	}

	/** 设置网络连接超时时间，默认为10秒钟
	 * 
	 * @param timeout */
	public void configTimeout(int timeout) {
		final HttpParams httpParams = this.httpClient.getParams();
		ConnManagerParams.setTimeout(httpParams, timeout);
		HttpConnectionParams.setSoTimeout(httpParams, timeout);
		HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
	}

	/** 设置https请求时 的 SSLSocketFactory
	 * 
	 * @param sslSocketFactory */
	public void configSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
		Scheme scheme = new Scheme("https", sslSocketFactory, 443);
		this.httpClient.getConnectionManager().getSchemeRegistry().register(scheme);
	}

	/** 配置错误重试次数
	 * 
	 * @param retry */
	public void configRequestExecutionRetryCount(int count) {
		this.httpClient.setHttpRequestRetryHandler(new RetryHandler(count));
	}

	/** 添加http请求头
	 * 
	 * @param header
	 * @param value */
	public void addHeader(String header, String value) {
		clientHeaderMap.put(header, value);
	}

	// ------------------get 请求-----------------------
	@SuppressWarnings("rawtypes")
	public AsyncTask get(String url, AjaxCallBack callBack) {
		return get(url, null, callBack);
	}

	@SuppressWarnings("rawtypes")
	public AsyncTask get(String url, AjaxParams params, AjaxCallBack callBack) {
		return sendRequest(httpClient, httpContext, new HttpGet(getUrlWithQueryString(url, params)), null, callBack);
	}

	@SuppressWarnings("rawtypes")
	public AsyncTask get(String url, Header[] headers, AjaxParams params, AjaxCallBack callBack) {
		HttpUriRequest request = new HttpGet(getUrlWithQueryString(url, params));
		if (headers != null) request.setHeaders(headers);
		return sendRequest(httpClient, httpContext, request, null, callBack);
	}

	public Object getSync(String url) {
		return getSync(url, null);
	}

	public Object getSync(String url, AjaxParams params) {
		HttpUriRequest request = new HttpGet(getUrlWithQueryString(url, params));
		return sendSyncRequest(httpClient, httpContext, request, null);
	}

	public Object getSync(String url, Header[] headers, AjaxParams params) {
		HttpUriRequest request = new HttpGet(getUrlWithQueryString(url, params));
		if (headers != null) request.setHeaders(headers);
		return sendSyncRequest(httpClient, httpContext, request, null);
	}

	// ------------------post 请求-----------------------
	@SuppressWarnings("rawtypes")
	public AsyncTask post(String url, AjaxCallBack callBack) {
		return post(url, null, callBack);
	}

	@SuppressWarnings("rawtypes")
	public AsyncTask post(String url, AjaxParams params, AjaxCallBack callBack) {
		return post(url, paramsToEntity(params), null, callBack);
	}

	@SuppressWarnings("rawtypes")
	public AsyncTask post(String url, HttpEntity entity, String contentType, AjaxCallBack callBack) {
		return sendRequest(httpClient, httpContext, addEntityToRequestBase(new HttpPost(url), entity), contentType,
				callBack);
	}

	@SuppressWarnings("rawtypes")
	public AsyncTask post(String url, Header[] headers, AjaxParams params, String contentType, AjaxCallBack callBack) {
		HttpEntityEnclosingRequestBase request = new HttpPost(url);
		if (params != null) request.setEntity(paramsToEntity(params));
		if (headers != null) request.setHeaders(headers);
		return sendRequest(httpClient, httpContext, request, contentType, callBack);
	}

	@SuppressWarnings("rawtypes")
	public AsyncTask post(String url, Header[] headers, HttpEntity entity, String contentType, AjaxCallBack callBack) {
		HttpEntityEnclosingRequestBase request = addEntityToRequestBase(new HttpPost(url), entity);
		if (headers != null) request.setHeaders(headers);
		return sendRequest(httpClient, httpContext, request, contentType, callBack);
	}

	public Object postSync(String url) {
		return postSync(url, null);
	}

	public Object postSync(String url, AjaxParams params) {
		return postSync(url, paramsToEntity(params), null);
	}

	public Object postSync(String url, HttpEntity entity, String contentType) {
		return sendSyncRequest(httpClient, httpContext, addEntityToRequestBase(new HttpPost(url), entity), contentType);
	}

	public Object postSync(String url, Header[] headers, AjaxParams params, String contentType) {
		HttpEntityEnclosingRequestBase request = new HttpPost(url);
		if (params != null) request.setEntity(paramsToEntity(params));
		if (headers != null) request.setHeaders(headers);
		return sendSyncRequest(httpClient, httpContext, request, contentType);
	}

	public Object postSync(String url, Header[] headers, HttpEntity entity, String contentType) {
		HttpEntityEnclosingRequestBase request = addEntityToRequestBase(new HttpPost(url), entity);
		if (headers != null) request.setHeaders(headers);
		return sendSyncRequest(httpClient, httpContext, request, contentType);
	}

	// ------------------put 请求-----------------------

	@SuppressWarnings("rawtypes")
	public AsyncTask put(String url, AjaxCallBack callBack) {
		return put(url, null, callBack);
	}

	@SuppressWarnings("rawtypes")
	public AsyncTask put(String url, AjaxParams params, AjaxCallBack callBack) {
		return put(url, paramsToEntity(params), null, callBack);
	}

	@SuppressWarnings("rawtypes")
	public AsyncTask put(String url, HttpEntity entity, String contentType, AjaxCallBack callBack) {
		return sendRequest(httpClient, httpContext, addEntityToRequestBase(new HttpPut(url), entity), contentType,
				callBack);
	}

	@SuppressWarnings("rawtypes")
	public AsyncTask put(String url, Header[] headers, HttpEntity entity, String contentType, AjaxCallBack callBack) {
		HttpEntityEnclosingRequestBase request = addEntityToRequestBase(new HttpPut(url), entity);
		if (headers != null) request.setHeaders(headers);
		return sendRequest(httpClient, httpContext, request, contentType, callBack);
	}

	public Object putSync(String url) {
		return putSync(url, null);
	}

	public Object putSync(String url, AjaxParams params) {
		return putSync(url, paramsToEntity(params), null);
	}

	public Object putSync(String url, HttpEntity entity, String contentType) {
		return putSync(url, null, entity, contentType);
	}

	public Object putSync(String url, Header[] headers, HttpEntity entity, String contentType) {
		HttpEntityEnclosingRequestBase request = addEntityToRequestBase(new HttpPut(url), entity);
		if (headers != null) request.setHeaders(headers);
		return sendSyncRequest(httpClient, httpContext, request, contentType);
	}

	// ------------------delete 请求-----------------------
	@SuppressWarnings("rawtypes")
	public AsyncTask delete(String url, AjaxCallBack callBack) {
		final HttpDelete delete = new HttpDelete(url);
		return sendRequest(httpClient, httpContext, delete, null, callBack);
	}

	@SuppressWarnings("rawtypes")
	public AsyncTask delete(String url, Header[] headers, AjaxCallBack callBack) {
		final HttpDelete delete = new HttpDelete(url);
		if (headers != null) delete.setHeaders(headers);
		return sendRequest(httpClient, httpContext, delete, null, callBack);
	}

	public Object deleteSync(String url) {
		return deleteSync(url, null);
	}

	public Object deleteSync(String url, Header[] headers) {
		final HttpDelete delete = new HttpDelete(url);
		if (headers != null) delete.setHeaders(headers);
		return sendSyncRequest(httpClient, httpContext, delete, null);
	}

	// ---------------------下载---------------------------------------
	public HttpHandler download(String url, String target, AjaxCallBack callback) {
		return download(url, null, target, false, callback);
	}

	public HttpHandler download(String url, String target, boolean isResume, AjaxCallBack callback) {
		return download(url, null, target, isResume, callback);
	}

	public HttpHandler download(String url, AjaxParams params, String target, AjaxCallBack callback) {
		return download(url, params, target, false, callback);
	}

	public HttpHandler download(String url, AjaxParams params, String target, boolean isResume, AjaxCallBack callback) {
		final HttpGet get = new HttpGet(getUrlWithQueryString(url, params));
		HttpHandler handler = new HttpHandler(httpClient, httpContext, callback, charset);

		Log.d(TAG, "send download request, url=" + get.getURI());

		handler.executeOnExecutor(downloadExecutor, get, target, isResume);
		return handler;
	}

	@SuppressWarnings("rawtypes")
	protected AsyncTask sendRequest(DefaultHttpClient client, HttpContext httpContext, HttpUriRequest uriRequest,
			String contentType, AjaxCallBack ajaxCallBack) {
		if (contentType != null) {
			uriRequest.addHeader("Content-Type", contentType);
		}

		Log.d(TAG, "send async request, url=" + uriRequest.getURI());

		return new HttpHandler(client, httpContext, ajaxCallBack, charset).executeOnExecutor(executor, uriRequest);
	}

	protected Object sendSyncRequest(DefaultHttpClient client, HttpContext httpContext, HttpUriRequest uriRequest,
			String contentType) {
		if (contentType != null) {
			uriRequest.addHeader("Content-Type", contentType);
		}

		Log.d(TAG, "send sync request, url=" + uriRequest.getURI());

		return new SyncRequestHandler(client, httpContext, charset).sendRequest(uriRequest);
	}

	public static String getUrlWithQueryString(String url, AjaxParams params) {
		if (params == null) return url;

		try {
			URL u = new URL(url);
			String symbol = u.getQuery() != null ? "&" : "?";
			String paramString = params.getParamString();
			url += symbol + paramString;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return url;
	}

	private HttpEntity paramsToEntity(AjaxParams params) {
		HttpEntity entity = null;

		if (params != null) {
			L.d(TAG, "put params=" + params.toString());
			entity = params.getEntity();
		}

		return entity;
	}

	private HttpEntityEnclosingRequestBase addEntityToRequestBase(HttpEntityEnclosingRequestBase requestBase,
			HttpEntity entity) {
		if (entity != null) {
			requestBase.setEntity(entity);
		}

		return requestBase;
	}

	private static class InflatingEntity extends HttpEntityWrapper {
		public InflatingEntity(HttpEntity wrapped) {
			super(wrapped);
		}

		@Override
		public InputStream getContent() throws IOException {
			return new GZIPInputStream(wrappedEntity.getContent());
		}
	}
}
