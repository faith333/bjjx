package com.police.bjxj.service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tsz.afinal.FinalDb;
import net.tsz.afinal.FinalHttp;
import net.tsz.afinal.http.AjaxCallBack;
import net.tsz.afinal.http.HttpHandler;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.widget.Toast;

import com.police.bjxj.R;
import com.police.bjxj.activity.MainActivity;
import com.police.bjxj.bean.Book;
import com.police.bjxj.bean.DownloadBook;
import com.police.bjxj.config.Setting;
import com.police.bjxj.util.L;

public class DownloadService extends Service {
	private static final String DOWNLOAD_DB_NAME = Setting.GLOBAL_DB_NAME + "Download";

	private static final String TAG = "DownloadService";

	private String mDownloadDir;
	private FinalDb mDownloadDb;
	private List<DownloadTask> mDownloadList;

	// Binder given to clients
	private final IBinder mBinder = new DownloadBinder();

	/**
	 * Class used for the client Binder. Because we know this service always runs in the same process as its clients, we
	 * don't need to deal with IPC.
	 */
	public class DownloadBinder extends Binder {
		public DownloadService getService() {
			//			 Return this instance of LocalService so clients can call public methods
			return DownloadService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		L.d(TAG, "onBind.. " + dumpObjectInfo());
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		L.d(TAG, "onUnbind. " + dumpObjectInfo());
		if (mDownloadList.isEmpty()) {
			L.d(TAG, "no download task exist, stop self.");
			stopSelf();
		}
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		L.d(TAG, "onDestroy.. cancel all download task.. " + dumpObjectInfo());
		for (DownloadTask task : mDownloadList) {
			task.book.setState(DownloadBook.STATE_PAUSE);
			mDownloadDb.update(task.book);
			task.handler.cancel(true);
		}
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		L.d(TAG, "onStartCommand.");
		return START_STICKY;
	}

	@Override
	public void onCreate() {
		L.d(TAG, "onCreate... " + dumpObjectInfo());
		super.onCreate();

		String state = Environment.getExternalStorageState();
		if (!state.equals(Environment.MEDIA_MOUNTED)) {
			L.d(TAG, "sdcard Unavailable, state=" + state);
			Toast.makeText(this, R.string.download_error_nosdcard, Toast.LENGTH_SHORT).show();
			// stopSelf();
		} else {
			File sdcard = Environment.getExternalStorageDirectory();
			mDownloadDir = sdcard.getAbsolutePath() + Setting.DOWNLOAD_DIR;

			mDownloadDb = FinalDb.create(this, DOWNLOAD_DB_NAME);
			List<DownloadBook> downloadList = mDownloadDb.findAll(DownloadBook.class);

			mDownloadList = new ArrayList<DownloadTask>();
			for (DownloadBook book : downloadList) {
				// if download success, but file is deleted, remove it.
				if (book.state == DownloadBook.STATE_SUCCESS) {
					if (!new File(book.getFilePath()).exists()) {
						mDownloadDb.delete(book);
						continue;
					}
				}

				if (book.state != DownloadBook.STATE_UNSTART) {
					long curr = new File(book.getFilePath()).length();
					long count = book.getFileLength();
					book.setProgress((int) (curr * 1f / count * 100f));
				}
				
				//如果服务被意外杀死, 导致下载中的图书中段, 我们需要修正其状态为暂停.
				if(book.state == DownloadBook.STATE_DOWNLOADING){
					book.state = DownloadBook.STATE_PAUSE;
				}
				
				mDownloadList.add(new DownloadTask(book));
			}

			L.d(TAG, "load dowload list size=" + mDownloadList.size() + ", uncomplete size=" + getDownloadingSize());

		}
	}

	/**
	 * 开始下载. 最好在开始之前调用regDownloadListener注册下载监听
	 * 
	 * @param book
	 * @return
	 */
	public boolean startDownload(Book book) {
		DownloadTask holder = getDownloadTask(book);
		boolean isResume = false;

		// no download task exist
		if (holder == null) {
			// wrap the download object.
			DownloadBook downloadBook = new DownloadBook(book);
			downloadBook.setStartTime(SystemClock.currentThreadTimeMillis());
			downloadBook.setFilePath(getBookPath(book));

			// save to db
			mDownloadDb.save(downloadBook);

			// create download holder
			holder = new DownloadTask(downloadBook);
			mDownloadList.add(holder);

			L.d(TAG, "pending new download >> book=" + book);
		} else {
			isResume = true;
			L.d(TAG, "resume download >> book=" + book);
		}

		switch (holder.book.getState()) {
		case DownloadBook.STATE_DOWNLOADING:
			L.d(TAG, "book is in downloading >> book=" + book);
			return false;
		case DownloadBook.STATE_SUCCESS:
			L.d(TAG, "book is download sucess >> book=" + book);
			return false;
		default:
			break;
		}

		// start download
		FinalHttp http = new FinalHttp();
		HttpHandler handler = http.download(holder.book.getDownloadUrl(), holder.book.getFilePath(), isResume, holder);
		holder.book.state = DownloadBook.STATE_DOWNLOADING;
		holder.handler = handler;

		updateNotifacation();
		return true;
	}

	/**
	 * 注册下载监听. 同样的监听(means: listener1.equals(listener2) )不会重复注册.
	 * 
	 * @param book
	 * @param listener
	 */
	public void regDownloadListener(Book book, AjaxCallBack listener) {
		DownloadTask dl = getDownloadTask(book);
		if (dl != null) {
			L.d(TAG, "regDownloadListener for book=" + book + ", exist size=" + dl.callbackSet.size() + ", this="
					+ this.hashCode());
			dl.callbackSet.add(listener);
			L.d(TAG, "regDownloadListener[" + listener.hashCode() + "] to task[" + dl.callbackSet.hashCode()
					+ "] for book=" + book + ", current Size=" + dl.callbackSet.size());
		}
	}

	/**
	 * 移除下载监听
	 * 
	 * @param book
	 * @param listener
	 */
	public void unRegDownloadListener(Book book, AjaxCallBack listener) {
		DownloadTask dl = getDownloadTask(book);
		if (dl != null) {
			dl.callbackSet.remove(listener);
			L.d(TAG, "unRegDownloadListener[" + listener.hashCode() + "] to task[" + dl.callbackSet.hashCode()
					+ "] for book=" + book + ", current Size=" + dl.callbackSet.size());
		}
	}

	public List<DownloadBook> getDownloadList() {
		ArrayList<DownloadBook> list = new ArrayList<DownloadBook>();

		for (DownloadTask l : mDownloadList) {
			list.add(l.book);
		}

		return list;
	}

	public void cancel(Book book) {
		L.d(TAG, "cancel download >> book=" + book);

		DownloadTask listener = getDownloadTask(book);
		if (listener == null) return;

		if (listener.handler != null) {
			L.d(TAG, "cancel download task.");
			// listener.handler.stop();
			listener.handler.cancel(true);
		}
		listener.callbackSet.clear();

		new File(getBookPath(book)).delete();

		mDownloadDb.delete(book);
		mDownloadList.remove(listener);
		updateNotifacation();
	}

	/**
	 * 方便的书籍文件查询
	 * 
	 * @param context
	 * @param book
	 * @return
	 */
	public static File getDownloadBookFile(Context context, Book book) {
		FinalDb db = FinalDb.create(context, DOWNLOAD_DB_NAME);
		DownloadBook downloadBook = db.findById(book.getId(), DownloadBook.class);
		if (downloadBook != null && downloadBook.canRead()) {
			File file = new File(downloadBook.filePath);
			if (file.exists()) return file;
		}
		return null;
	}

	private void updateNotifacation() {
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		int count = getDownloadingSize();

		if (count == 0) {
			nm.cancel(0);
			return;
		}

		String text = getString(R.string.notification_format, count);
		Notification notification = new Notification(R.drawable.logo, text, System.currentTimeMillis());

		// set click intent.
		Intent intent = new Intent(this, MainActivity.class);
		intent.putExtra(MainActivity.EXTRA_TAB_INDEX, MainActivity.TAB_DOWNLOAD);
		PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		;
		notification.setLatestEventInfo(this, getString(R.string.notification_downloading), text, pi);

		nm.notify(0, notification);

	}

	private int getDownloadingSize() {
		int count = 0;
		for (DownloadTask task : mDownloadList) {
			if (task.book.isDownloading()) {
				count++;
			}
		}
		return count;
	}

	private DownloadTask getDownloadTask(Book book) {
		for (DownloadTask l : mDownloadList) {
			if (l.book.equals(book)) {
				return l;
			}
		}
		return null;
	}

	private String getBookPath(Book book) {
		return mDownloadDir + File.separator + book.getName() + ".pdf";
	}

	private class DownloadTask extends AjaxCallBack {
		private boolean saved;
		DownloadBook book;
		Set<AjaxCallBack> callbackSet;
		HttpHandler handler;

		public DownloadTask(DownloadBook book) {
			this.book = book;
			callbackSet = new HashSet<AjaxCallBack>();
		}

		@Override
		public void onStart() {
			L.d(TAG, "download start, book=" + book);
			book.setState(DownloadBook.STATE_DOWNLOADING);
			mDownloadDb.update(book);

			for (AjaxCallBack callback : callbackSet) {
				callback.onStart();
			}
		}

		@Override
		public void onFinish() {
			L.d(TAG, "download finish, book=" + book);
			mDownloadDb.update(book);
			updateNotifacation();

			for (AjaxCallBack callback : callbackSet) {
				callback.onFinish();
			}
		}

		@Override
		public void onLoading(long count, long current) {
			book.setProgress((int) ((float) current / count * 100f));
			book.setFileLength(count);

			if (!saved) {
				mDownloadDb.update(book);
				saved = true;
			}

			for (AjaxCallBack callback : callbackSet) {
				callback.onLoading(count, current);
			}
		}

		@Override
		public void onSuccess(Object obj) {
			L.d(TAG, "download sucess, book=" + book);
			book.setState(DownloadBook.STATE_SUCCESS);

			for (AjaxCallBack callback : callbackSet) {
				callback.onSuccess(obj);
			}

			//
			callbackSet.clear();
		}

		@Override
		public void onFailure(Exception t, Object attach) {
			L.d(TAG, "download failed, book=" + book);
			book.setState(DownloadBook.STATE_FAILURE);

			if (callbackSet == null) return;
			for (AjaxCallBack callback : callbackSet) {
				callback.onFailure(t, attach);
			}
		}
	}

	private String dumpObjectInfo() {
		return "pid|tid|hash=" + Process.myPid() + "|" + Process.myTid() + "|" + this.hashCode();
	}
}
