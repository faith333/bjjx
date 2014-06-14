package com.police.bjxj.activity;

import android.graphics.Movie;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.police.bjxj.util.L;

public class SettingFragment extends Fragment implements IFragment {
	protected static final String TAG = "MovieListFragment";

	private Bundle mArgs;

	@Override
	public void setParams(Bundle b) {
		mArgs = b;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		L.lo(TAG, "onCreate");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		L.lo(TAG, "onCreateView");
		View root = null;//inflater.inflate(R.layout.main_movie, container, false);

		initTabs(root);

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
	public void reloadData() {}

	@Override
	public boolean onBackPress() {
		// TODO Auto-generated method stub
		return false;
	}

	private void loadMovieData() {}

	private void initTabs(View root) {}

	private void changedTabTo(int index) {}

	private void showMovieDetail(Movie movie) {}
}
