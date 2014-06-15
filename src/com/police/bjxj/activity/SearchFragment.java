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
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.police.bjxj.R;
import com.police.bjxj.accessor.Accessor;
import com.police.bjxj.bean.Album;
import com.police.bjxj.config.Setting;
import com.police.bjxj.util.L;

public class SearchFragment extends Fragment implements IFragment {
	protected static final String TAG = "SearchFragment";

	@SuppressWarnings("rawtypes")
	private AsyncTask task;

	private FinalBitmap mImageLoader;

	private EditText vEdit;
	private GridView vSearchGrid;

	private List<Album> mAlbumArray;
	private SearchAdapter mSearchAdapter;

	private View vLoading;
	private View vLoadFail;

	@Override
	public void setParams(Bundle b) {
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		L.lo(TAG, "onAttach");
		mImageLoader = FinalBitmap.create(activity);
		mImageLoader.configDiskCachePath(Setting.IMG_CACHE_DIR).configLoadingImage(R.drawable.cover);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		L.lo(TAG, "onCreate");
		mAlbumArray = new ArrayList<Album>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		L.lo(TAG, "onCreateView");
		View root = inflater.inflate(R.layout.search_frg, container, false);

		setTitle(root);

		vSearchGrid = (GridView) root.findViewById(R.id.search_grid);
		mSearchAdapter = new SearchAdapter();
		vSearchGrid.setAdapter(mSearchAdapter);

		vSearchGrid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				showAlbumBooks(mAlbumArray.get(position));
			}
		});

		vEdit = (EditText) root.findViewById(R.id.search_edit);
		View vSubmit = root.findViewById(R.id.search_submit);
		vSubmit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleInputMethod();
				reloadData();
			}
		});

		vLoading = root.findViewById(R.id.shared_loading_parent);
		vLoading.findViewById(R.id.shared_loading_progressbar).setVisibility(View.GONE);
		TextView vTx = (TextView) vLoading.findViewById(R.id.shared_loading_text);
		vTx.setText("");
		vSearchGrid.setEmptyView(vLoading);

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
		vEdit.requestFocus();
	}

	@Override
	public void onPause() {
		L.lo(TAG, "onPause");
		super.onPause();
	}

	@Override
	public void onStop() {
		L.lo(TAG, "onStop");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		L.lo(TAG, "onStop");
		super.onDestroy();
	}

	@Override
	public void reloadData() {
		String key = vEdit.getText().toString();
		if (key == null || key.isEmpty()) {
			Toast.makeText(getActivity(), R.string.search_key_empty, Toast.LENGTH_SHORT).show();
			return;
		}

		if (task != null) task.isCancelled();
		mAlbumArray.clear();

		if (vLoadFail != null) {
			vLoadFail.setVisibility(View.GONE);
		}
		vLoading.findViewById(R.id.shared_loading_progressbar).setVisibility(View.VISIBLE);
		TextView vTx = (TextView) vLoading.findViewById(R.id.shared_loading_text);
		vTx.setText(R.string.loading);

		mSearchAdapter.notifyDataSetInvalidated();

		loadData();
	}

	@Override
	public boolean onBackPress() {
		// TODO Auto-generated method stub
		return false;
	}

	private void loadData() {
		task = new Accessor(new AjaxCallBack() {
			@Override
			public void onSuccess(Object obj) {
				try {
					JSONObject json = new JSONObject((String) obj);
					JSONArray albums = json.getJSONArray("result");
					for (int i = 0; i < albums.length(); i++) {
						Album album = Album.parseFrom(albums.getJSONObject(i));
						mAlbumArray.add(album);
					}

					if (mAlbumArray.isEmpty()) {
						showEmptyResult();
					} else {
						mSearchAdapter.notifyDataSetChanged();
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
		}).search(vEdit.getText().toString());
	}

	private void setTitle(View root) {
		TextView vTitle = (TextView) root.findViewById(R.id.title_name);
		vTitle.setText(R.string.search);

		root.findViewById(R.id.title_back).setVisibility(View.GONE);
	}

	private void showAlbumBooks(Album album) {
		Bundle args = new Bundle();
		args.putInt(BookFragment.PARAMS_ALBUM_ID, (int) album.getId());
		args.putInt(BookFragment.PARAMS_FROM_TAB, MainActivity.TAB_SEARCH);
		((MainActivity) getActivity()).changedTabTo(MainActivity.TAB_BOOK, true, args);
	}

	private void showEmptyResult() {
		vLoading.setVisibility(View.VISIBLE);
		vLoading.findViewById(R.id.shared_loading_progressbar).setVisibility(View.GONE);
		TextView vTx = (TextView) vLoading.findViewById(R.id.shared_loading_text);
		vTx.setText(R.string.search_empty);
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

	public void toggleInputMethod() {
		InputMethodManager m = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
		m.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
	}

	private class SearchAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return mAlbumArray.size();
		}

		@Override
		public Album getItem(int position) {
			return mAlbumArray.get(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ImageView view;

			if (convertView == null) {
				view = (ImageView) LayoutInflater.from(getActivity()).inflate(R.layout.search_grid_item, parent, false);
			} else {
				view = (ImageView) convertView;
			}

			Album album = getItem(position);
			mImageLoader.display(view, album.getCover());

			return view;
		}
	}

}
