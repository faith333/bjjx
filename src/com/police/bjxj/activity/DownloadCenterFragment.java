package com.police.bjxj.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.tsz.afinal.FinalBitmap;
import net.tsz.afinal.http.AjaxCallBack;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.artifex.mupdf.MuPDFActivity;
import com.police.bjxj.R;
import com.police.bjxj.bean.DownloadBook;
import com.police.bjxj.service.DownloadService;
import com.police.bjxj.service.DownloadService.DownloadBinder;
import com.police.bjxj.util.L;

public class DownloadCenterFragment extends Fragment implements IFragment {
	protected static final String TAG = "DownloadCenterFragment";

	private DownloadAdapter mDownloadAdapter;
	private FinalBitmap mImageLoader;
	private DownloadService mDownloadService;

	private List<DownloadBook> mDownloadArray;

	/**
	 * dkdkd
	 * 
	 * @aaaa
	 */
	@Override
	public void setParams(Bundle b) {
		// do nothing..

		/*
		 * DDDDDD
		 */
	}

	boolean mBound = false;
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			L.d(TAG, "unbind download service");
			mBound = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			L.d(TAG, "bind download service success, IBinder=" + service);
			mDownloadService = ((DownloadBinder) service).getService();
			mBound = true;
			loadData();

		}
	};

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mImageLoader = FinalBitmap.create(activity);

		Intent service = new Intent(activity, DownloadService.class);
		activity.startService(service);
		activity.bindService(service, mConnection, Service.BIND_AUTO_CREATE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		L.lo(TAG, "onCreate");
		mDownloadArray = new ArrayList<DownloadBook>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		L.lo(TAG, "onCreateView");
		View root = inflater.inflate(R.layout.download_frg, container, false);

		setTitle(root);

		ListView vDownloadList = (ListView) root.findViewById(R.id.download_list);
		mDownloadAdapter = new DownloadAdapter(getActivity(), R.layout.download_list_item, R.id.download_book_name,
				mDownloadArray);
		vDownloadList.setAdapter(mDownloadAdapter);
		vDownloadList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				readBook(mDownloadAdapter.getItem(position));
			}
		});
		vDownloadList.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				//				cancelDownload(mDownloadAdapter.getItem(position));
				//				return true;
				return false;
			}
		});

		View emptyView = root.findViewById(R.id.shared_loading_parent);
		vDownloadList.setEmptyView(emptyView);
		return root;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		L.lo(TAG, "onActivityCreated");
	}

	@Override
	public void onStart() {
		super.onStart();
		L.lo(TAG, "onStart");
	}

	@Override
	public void onResume() {
		super.onResume();
		L.lo(TAG, "onResume");
	}

	@Override
	public void onStop() {
		L.lo(TAG, "onStop");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		L.d(TAG, "onDestroy");
		mImageLoader.onDestroy();

		if (mBound) {
			getActivity().unbindService(mConnection);
			mBound = false;
		}
		super.onDestroy();
	}

	@Override
	public void reloadData() {
		mDownloadArray.clear();
		mDownloadAdapter.notifyDataSetInvalidated();
		loadData();
	}

	@Override
	public boolean onBackPress() {
		// TODO Auto-generated method stub
		return false;
	}

	private void loadData() {
		L.d(TAG, "loadData");
		mDownloadArray.addAll(mDownloadService.getDownloadList());
		mDownloadAdapter.notifyDataSetChanged();
	}

	private void setTitle(View root) {
		TextView vTitle = (TextView) root.findViewById(R.id.title_name);
		vTitle.setText(R.string.album_title);

		root.findViewById(R.id.title_back).setVisibility(View.GONE);
	}

	private void readBook(DownloadBook book) {
		if (!book.canRead()) return;

		Intent intent = new Intent(getActivity(), MuPDFActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setType("application/pdf");
		intent.setData(Uri.fromFile(new File(book.getFilePath())));
		startActivity(intent);
	}

	private void showErrorView(Exception ex) {
		View root = getView();

		root.findViewById(R.id.shared_loading_progressbar).setVisibility(View.GONE);

		TextView vTx = (TextView) root.findViewById(R.id.shared_loading_text);
		vTx.setText(R.string.download_book_empty);
	}

	private class DownloadAdapter extends ArrayAdapter<DownloadBook> {
		private SparseArray<ProgressHolder> mProgressHolders;

		public DownloadAdapter(Context context, int resource, int textViewResourceId, List<DownloadBook> objects) {
			super(context, resource, textViewResourceId, objects);
			mProgressHolders = new SparseArray<ProgressHolder>();
		}

		@Override
		public void notifyDataSetChanged() {
			super.notifyDataSetChanged();
			if (mDownloadArray.isEmpty()) {
				showErrorView(null);
			}
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View view = super.getView(position, convertView, parent);
			view.setBackgroundColor(Color.TRANSPARENT);

			final DownloadBook book = getItem(position);
			L.d(TAG,
					"getview, book " + book.getName() + " at position " + position + ", progress is "
							+ book.getProgress() + ", state=" + book.getState());

			TextView vName = (TextView) view.findViewById(R.id.download_book_name);
			vName.setText(book.getName());

			TextView vAlbumName = (TextView) view.findViewById(R.id.download_book_album_name);
			vAlbumName.setText(book.getAlbumName());

			ImageView vCover = (ImageView) view.findViewById(R.id.download_book_cover);
			mImageLoader.display(vCover, book.getCover());

			/* sync view statue. -------------------------------------- */
			switch (book.getState()) {
			case DownloadBook.STATE_SUCCESS:
			case DownloadBook.STATE_FAILURE:
				bindDownloadCompleteViews(view, book);
				break;
			default:
				bindDownloadingViews(view, book);
				mDownloadService.startDownload(book);
				break;
			}

			return view;
		}

		private void bindDownloadingViews(View root, final DownloadBook book) {
			L.d(TAG, "bindDownloadingViews at book=" + book + ", view=" + root);

			root.findViewById(R.id.download_book_complete_parent).setVisibility(View.INVISIBLE);
			root.findViewById(R.id.download_book_downloading_parent).setVisibility(View.VISIBLE);

			final ProgressBar vProgress = (ProgressBar) root.findViewById(R.id.download_book_progress);
			vProgress.setProgress(book.getProgress());

			final TextView vProgressTx = (TextView) root.findViewById(R.id.download_book_progress_tx);
			vProgressTx.setText(book.getProgress() + "%");

			final TextView vOp = (TextView) root.findViewById(R.id.download_book_cancel);
			vOp.setText(R.string.download_book_cancel);
			vOp.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mProgressHolders.remove((int) book.getId());
					cancelDownload(book);
				}
			});

			/* listening progress -------------------------------------- */
			ProgressHolder progressHolder = mProgressHolders.get((int) book.getId());
			if (progressHolder == null) {
				progressHolder = new ProgressHolder(vProgress, vProgressTx, root, book) {
					@Override
					public void onLoading(long count, long current) {
						L.d(TAG, "onLoading");
						int progress = book.getProgress();
						bar.setProgress(progress);
						txt.setText(progress + "%");
					}

					@Override
					public void onSuccess(Object obj) {
						L.d(TAG, "onSuccess");
						bindDownloadCompleteViews(itemView, book);
						((View) txt.getParent()).setBackgroundResource(R.drawable.bg_list_item_selector);
					}

					@Override
					public void onFailure(Exception t, Object attach) {
						L.d(TAG, "onFailure");
						bindDownloadCompleteViews(itemView, book);
					}
				};
				mProgressHolders.put((int) book.getId(), progressHolder);
				mDownloadService.regDownloadListener(book, progressHolder);
			} else {
				progressHolder.bar = vProgress;
				progressHolder.txt = vProgressTx;
				progressHolder.itemView = root;
				progressHolder.book = book;
			}
		}

		private void bindDownloadCompleteViews(final View root, final DownloadBook book) {
			L.d(TAG, "bindDownloadCompleteViews at book=" + book + ", view=" + root);

			root.findViewById(R.id.download_book_downloading_parent).setVisibility(View.INVISIBLE);
			root.findViewById(R.id.download_book_complete_parent).setVisibility(View.VISIBLE);

			final TextView vFailedTx = (TextView) root.findViewById(R.id.download_book_failed);
			final TextView vRetry = (TextView) root.findViewById(R.id.download_book_retry);
			vRetry.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mDownloadService.startDownload(book);
					bindDownloadingViews(root, book);
				}
			});

			final TextView vDelete = (TextView) root.findViewById(R.id.download_book_delete);
			vDelete.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mProgressHolders.remove((int) book.getId());
					new AlertDialog.Builder(getActivity())//
							.setIcon(android.R.drawable.ic_dialog_alert)//
							.setCancelable(false)//
							.setMessage(R.string.download_book_delete_wanning)//
							.setNegativeButton(android.R.string.ok, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									cancelDownload(book);
								}
							})//
							.setPositiveButton(android.R.string.cancel, new OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							})//
							.create().show();

				}
			});

			switch (book.getState()) {
			case DownloadBook.STATE_SUCCESS:
				vFailedTx.setVisibility(View.INVISIBLE);
				vRetry.setVisibility(View.INVISIBLE);
				break;
			default:
				vFailedTx.setVisibility(View.VISIBLE);
				vRetry.setVisibility(View.VISIBLE);
				break;
			}
		}

		private void cancelDownload(final DownloadBook book) {
			mDownloadService.cancel(book);
			mDownloadArray.remove(book);
			mDownloadAdapter.notifyDataSetChanged();
			mProgressHolders.remove((int) book.getId());
		}

		private class ProgressHolder extends AjaxCallBack {
			ProgressBar bar;
			TextView txt;
			View itemView;
			DownloadBook book;

			public ProgressHolder(ProgressBar bar, TextView txt, View root, DownloadBook book) {
				this.bar = bar;
				this.txt = txt;
				this.itemView = root;
				this.book = book;
			}
		}
	}
}
