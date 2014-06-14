package com.police.bjxj.accessor;

import net.tsz.afinal.core.AsyncTask;

@SuppressWarnings("rawtypes")
public interface IAccessor {
	public static final int ERROR_LOGIN_EXPIRED = 0;
	public static final int ERROR_DATA_ERROR = 1;
	
	AsyncTask getAlbumList();
	
	AsyncTask getBookList(long albumId);
	
	AsyncTask search(String key);
	
	AsyncTask getAdvertise();

	AsyncTask login(String username, String pwd);

}
