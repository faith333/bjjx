package com.police.bjxj.activity;

import java.util.ArrayList;
import java.util.List;

import net.tsz.afinal.FinalBitmap;
import net.tsz.afinal.FinalDb;
import net.tsz.afinal.core.AsyncTask;
import net.tsz.afinal.http.AjaxCallBack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.police.bjxj.R;
import com.police.bjxj.accessor.Accessor;
import com.police.bjxj.bean.Book;
import com.police.bjxj.bean.DownloadBook;
import com.police.bjxj.config.Setting;
import com.police.bjxj.service.DownloadService;
import com.police.bjxj.service.DownloadService.DownloadBinder;
import com.police.bjxj.util.L;

public class BookFragment extends Fragment implements IFragment {
	protected static final String TAG = "BookFragment";
	public static final String PARAMS_ALBUM_ID = "albumid";
	public static final String PARAMS_FROM_TAB = "from_tab";

	@SuppressWarnings("rawtypes")
	private AsyncTask task;
	private FinalDb mDB;
	private List<Book> mFavoriteList;

	private FinalBitmap mImageLoader;
	private DownloadService mDownloadService;
	private List<DownloadBook> mDownloadList;

	private BookAdapter mBookAdapter;
	private List<Book> mBookArray;
	private View vLoadFail;

	boolean mBound = false;
	private Bundle mArgs;

	@Override
	public void setParams(Bundle b) {
		mArgs = b;
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			L.d(TAG, "unbind download service");
			mBound = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			L.d(TAG, "bind download service");
			mDownloadService = ((DownloadBinder) service).getService();
			mBound = true;

		}
	};
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mImageLoader = FinalBitmap.create(activity);
		mImageLoader.configDiskCachePath(Setting.IMG_CACHE_DIR).configLoadingImage(R.drawable.cover);

		Intent service = new Intent(activity, DownloadService.class);
		activity.startService(service);
		activity.bindService(service, mConnection, Service.BIND_AUTO_CREATE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		L.lo(TAG, "onCreate");
		mBookArray = new ArrayList<Book>();
		mFavoriteList = new ArrayList<Book>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		L.d(TAG, "onCreateView");
		View root = inflater.inflate(R.layout.book_frg, container, false);

		setTitle(root);

		ListView vBookList = (ListView) root.findViewById(R.id.book_list);
		mBookAdapter = new BookAdapter(getActivity(), R.layout.book_list_item, R.id.book_name, mBookArray);
		vBookList.setAdapter(mBookAdapter);

		View emptyView = root.findViewById(R.id.shared_loading_parent);
		vBookList.setEmptyView(emptyView);

		return root;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		L.d(TAG, "onActivityCreated");
		
		loadData();
	}

	@Override
	public void onStart() {
		super.onStart();
		L.lo(TAG, "onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
		L.d(TAG, "onResume");
	}

	@Override
	public void onStop() {
		L.d(TAG, "onStop");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		L.d(TAG, "onDestroy");
		mImageLoader.onDestroy();
		task.cancel(true);

		if (mBound) {
			getActivity().unbindService(mConnection);
			mBound = false;
		}
		super.onDestroy();
	}

	@Override
	public void reloadData() {
		if (task != null) task.cancel(true);

		if (vLoadFail != null) {
			vLoadFail.setVisibility(View.GONE);
		}

		mBookArray.clear();
		mBookAdapter.notifyDataSetInvalidated();

		loadData();
	}

	@Override
	public boolean onBackPress() {
		if (mArgs == null) return false;

		int fromTab = mArgs.getInt(PARAMS_FROM_TAB);

		L.d(TAG, "onBackPress, fromtab=" + fromTab);
		((MainActivity) getActivity()).changedTabTo(fromTab, false);

		return true;
	}

	private void loadData() {
		if (mArgs == null) showErrorView(null);

		int albumId = mArgs.getInt(PARAMS_ALBUM_ID);

		task = new Accessor(new AjaxCallBack() {
			@Override
			public void onSuccess(Object obj) {
				try {
					JSONObject json = new JSONObject((String) obj);
					JSONArray array = json.optJSONArray("books");
					for (int i = 0; i < array.length(); i++) {
						Book book = Book.parseFrom(array.getJSONObject(i));
						mBookArray.add(book);
					}
					if (mBookArray.isEmpty()) {
						showErrorView(new Exception(getString(R.string.error_no_data)));
					} else {
						loadFavorites();
						loadDownloadList();
						mBookAdapter.notifyDataSetChanged();
					}
				} catch (JSONException e) {
					e.printStackTrace();
					onFailure(e, null);
				}
			}

			@Override
			public void onFailure(Exception t, Object attach) {
				showErrorView(t);
			}
		}).getBookList(albumId);

	}

	private void loadDownloadList() {
		if (mDownloadService != null) {
			mDownloadList = mDownloadService.getDownloadList();
		}
	}

	private void setTitle(View root) {
		TextView vTitle = (TextView) root.findViewById(R.id.title_name);
		vTitle.setText(R.string.album_title);

		root.findViewById(R.id.title_logo).setVisibility(View.GONE);
		root.findViewById(R.id.title_back).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPress();
			}
		});
	}

	private void showErrorView(Exception ex) {
		if (vLoadFail == null) {
			ViewStub stub = (ViewStub) getView().findViewById(R.id.shared_loading_failed_stub);
			vLoadFail = stub.inflate();
		}

		TextView vFailText = (TextView) vLoadFail.findViewById(R.id.shared_fail_text);
		vFailText.setText(ex.getMessage());

		vLoadFail.setVisibility(View.VISIBLE);
		vLoadFail.findViewById(R.id.shared_fail_retrybtn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				vLoadFail.setVisibility(View.GONE);
				loadData();
			}
		});
	}

	private void loadFavorites() {
		if (mDB == null) {
			mDB = FinalDb.create(getActivity(), Setting.GLOBAL_DB_NAME);
		}
		mFavoriteList = mDB.findAll(Book.class);
		L.d(TAG, "favorite list=" + mFavoriteList);
	}

	private void download(Book book) {
		boolean start = mDownloadService.startDownload(book);
		if (start) {
			((MainActivity) getActivity()).reloadFrgmentContent(MainActivity.TAB_DOWNLOAD);
		}
	}

	private void favoriteBook(final Book book) {
		if (mFavoriteList.contains(book)) {
			return;
		} else {
			mDB.save(book);
			mFavoriteList.add(book);
			((MainActivity) getActivity()).reloadFrgmentContent(MainActivity.TAB_FAVORITE);
		}
	}

	private class BookAdapter extends ArrayAdapter<Book> {
		public BookAdapter(Context context, int resource, int textViewResourceId, List<Book> objects) {
			super(context, resource, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);

			final Book book = getItem(position);

			TextView vName = (TextView) view.findViewById(R.id.book_name);
			vName.setText(book.getName());

			TextView vAlbumName = (TextView) view.findViewById(R.id.book_album_name);
			vAlbumName.setText(book.getAlbumName());

			ImageView vCover = (ImageView) view.findViewById(R.id.book_cover);
			mImageLoader.display(vCover, book.getCover());

			TextView vDownload = (TextView) view.findViewById(R.id.book_download);
			vDownload.setClickable(false);
			int state = getDownloadState(book.getId());
			switch (state) {
			case DownloadBook.STATE_SUCCESS:
				vDownload.setText(R.string.book_download_success);
				break;
			case DownloadBook.STATE_FAILURE:
				vDownload.setText(R.string.book_download_failure);
				break;
			case DownloadBook.STATE_DOWNLOADING:
				vDownload.setText(R.string.book_downloading);
				break;
			default:
				vDownload.setClickable(true);
				vDownload.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						((TextView) v).setText(R.string.book_downloading);
						download(book);
					}
				});
				break;
			}

			TextView vFavorite = (TextView) view.findViewById(R.id.book_favorite);
			vFavorite.setClickable(false);
			if (mFavoriteList.contains(book)) {
				vFavorite.setText(R.string.book_favorited);
			} else {
				vFavorite.setClickable(true);
				vFavorite.setText(R.string.book_favorite);
				vFavorite.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						((TextView) v).setText(R.string.book_favorited);
						favoriteBook(book);
					}

				});
			}

			return view;
		}
	}

	public int getDownloadState(long id) {
		if (mDownloadList != null) {
			for (DownloadBook downloadBook : mDownloadList) {
				if (downloadBook.getId() == id) {
					return downloadBook.getState();
				}
			}
		}
		return DownloadBook.STATE_UNSTART;
	}
}
