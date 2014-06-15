package com.police.bjxj.activity;

import java.util.ArrayList;
import java.util.List;

import net.tsz.afinal.FinalBitmap;
import net.tsz.afinal.core.AsyncTask;
import net.tsz.afinal.http.AjaxCallBack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.police.bjxj.R;
import com.police.bjxj.accessor.Accessor;
import com.police.bjxj.bean.Album;
import com.police.bjxj.config.Setting;
import com.police.bjxj.util.L;

public class AlbumFragment extends Fragment implements IFragment {
	protected static final String TAG = "AlbumFragment";

	@SuppressWarnings("rawtypes")
	private AsyncTask task;

	private SparseArray<String> mCategoryArray;
	private SparseArray<List<Album>> mAlbumArray;

	private FinalBitmap mImageLoader;
	private CategoryAdapter mCategoryAdapter;

	private ListView vAlbumList;
	private View vLoadFail;

	@Override
	public void setParams(Bundle b) {
		//do nothing.
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mImageLoader = FinalBitmap.create(activity);
		mImageLoader.configDiskCachePath(Setting.IMG_CACHE_DIR).configLoadingImage(R.drawable.cover);
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		L.lo(TAG, "onCreate");
		mCategoryArray = new SparseArray<String>();
		mAlbumArray = new SparseArray<List<Album>>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		L.lo(TAG, "onCreateView");
		View root = inflater.inflate(R.layout.album_frg, container, false);

		setTitle(root);

		vAlbumList = (ListView) root.findViewById(R.id.album_list);
		mCategoryAdapter = new CategoryAdapter();
		vAlbumList.setAdapter(mCategoryAdapter);

		View emptyView = root.findViewById(R.id.shared_loading_parent);
		vAlbumList.setEmptyView(emptyView);

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
		loadData();
	}

	@Override
	public void onResume() {
		super.onResume();
		//		mImageLoader.onResume();
		L.lo(TAG, "onResume");
	}

	@Override
	public void onPause() {
		L.lo(TAG, "onPause");
		//		mImageLoader.onPause();
		super.onPause();
	}

	@Override
	public void onStop() {
		L.lo(TAG, "onStop");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		L.lo(TAG, "onDestroy");
		mImageLoader.onDestroy();
		task.cancel(true);
		super.onDestroy();
	}

	@Override
	public void reloadData() {
		task.isCancelled();

		if (vLoadFail != null) {
			vLoadFail.setVisibility(View.GONE);
		}

		mCategoryArray.clear();
		mAlbumArray.clear();
		mCategoryAdapter.notifyDataSetInvalidated();

		loadData();
	}

	@Override
	public boolean onBackPress() {
		//do nothing..
		return false;
	}

	private void loadData() {
		task = new Accessor(new AjaxCallBack() {
			@Override
			public void onSuccess(Object obj) {
				try {
					JSONObject json = new JSONObject((String) obj);
					JSONArray array = json.optJSONArray("categories");
					for (int i = 0; i < array.length(); i++) {
						JSONObject category = array.getJSONObject(i);

						// put category
						int categoryId = category.optInt("id");
						String categoryName = category.optString("category");
						mCategoryArray.put(categoryId, categoryName);

						// put album
						JSONArray albums = category.getJSONArray("album");
						ArrayList<Album> albumArray = new ArrayList<Album>();
						for (int j = 0; j < albums.length(); j++) {
							Album album = Album.parseFrom(albums.getJSONObject(j));
							albumArray.add(album);
						}
						mAlbumArray.put(categoryId, albumArray);
					}

					L.d(TAG, "load data finish, categorySize=" + mCategoryArray.size());
					mCategoryAdapter.notifyDataSetChanged();
				} catch (JSONException e) {
					e.printStackTrace();
					onFailure(e, null);
				}
			}

			@Override
			public void onFailure(Exception t, Object attach) {
				showErrorView(t);
			}
		}).getAlbumList();
	}

	private void setTitle(View root) {
		TextView vTitle = (TextView) root.findViewById(R.id.title_name);
		vTitle.setText(R.string.album_title);

		root.findViewById(R.id.title_back).setVisibility(View.GONE);
	}

	private void showAlbumBooks(int albumId) {
		Bundle args = new Bundle();
		args.putInt(BookFragment.PARAMS_ALBUM_ID, albumId);
		args.putInt(BookFragment.PARAMS_FROM_TAB, MainActivity.TAB_Album);
		((MainActivity) getActivity()).changedTabTo(MainActivity.TAB_BOOK, true, args);
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

	private class CategoryAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return mCategoryArray.size();
		}

		@Override
		public String getItem(int position) {
			return mCategoryArray.valueAt(position);
		}

		@Override
		public long getItemId(int position) {
			return mCategoryArray.keyAt(position);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewGroup view;

			LayoutInflater inflater = LayoutInflater.from(getActivity());
			if (convertView == null) {
				view = (ViewGroup) inflater.inflate(R.layout.album_list_item, parent, false);
			} else {
				view = (ViewGroup) convertView;
			}

			String categoryName = (String) getItem(position);
			TextView vName = (TextView) view.findViewById(R.id.album_category_name);
			vName.setText(categoryName);

			ViewGroup rowParent = (ViewGroup) view.findViewById(R.id.album_book_parent);
			List<Album> albums = mAlbumArray.get((int) getItemId(position));
			int maxCell = albums.size();
			int maxRow = (int) Math.max(rowParent.getChildCount(), Math.ceil(maxCell / 4f));
			for (int i = 0; i < maxRow; i++) {
				ViewGroup row = (ViewGroup) rowParent.getChildAt(i);

				//if have row, but no item to display, remove row view and skip this loop.
				if (row != null && i * 4 < maxCell) {
					view.removeView(row);
					continue;
				}

				//if have row, but not created, create row view.
				if (row == null) {
					row = (ViewGroup) inflater.inflate(R.layout.album_grid_item, rowParent, false);
					rowParent.addView(row);
				}

				//if have row and have enough item to display, bind cell.
				for (int j = 0; j < 4; j++) {
					ImageView cell = (ImageView) row.getChildAt(j);
					int cellIndex = j + i * 4;
					if (cellIndex < maxCell) {
						final Album album = albums.get(cellIndex);
						mImageLoader.display(cell, album.getCover());
						cell.setVisibility(View.VISIBLE);
						cell.setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								showAlbumBooks((int) album.getId());
							}
						});
					} else {
						cell.setVisibility(View.INVISIBLE);
					}

				}

			}

			return view;
		}
	}
}
