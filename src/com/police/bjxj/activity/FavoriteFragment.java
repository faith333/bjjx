package com.police.bjxj.activity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.tsz.afinal.FinalBitmap;
import net.tsz.afinal.FinalDb;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.artifex.mupdf.MuPDFActivity;
import com.police.bjxj.R;
import com.police.bjxj.bean.Book;
import com.police.bjxj.config.Setting;
import com.police.bjxj.service.DownloadService;
import com.police.bjxj.util.L;

public class FavoriteFragment extends Fragment implements IFragment {
	protected static final String TAG = "FavoriteFragment";

	private FavoriteAdapter mFavoriteAdapter;
	private List<Book> mBooks;
	private List<Book> mDeleteBooks;

	private FinalBitmap mImageLoader;
	private GridView vFavoreteGrid;
	private View vDelTip;

	private boolean isInEditMode;
	private FinalDb mDB;


	@Override
	public void setParams(Bundle b) {
		//do nothing
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		L.lo(TAG, "onAttach");
		mImageLoader = FinalBitmap.create(activity);
		mImageLoader.configDiskCachePath(Setting.IMG_CACHE_DIR).configLoadingImage(R.drawable.cover);
		
		mDB = FinalDb.create(getActivity(), Setting.GLOBAL_DB_NAME);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		L.lo(TAG, "onCreate");
		mBooks = new ArrayList<Book>();
		mDeleteBooks = new ArrayList<Book>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		L.lo(TAG, "onCreateView");
		View root = inflater.inflate(R.layout.favorite_frg, container, false);

		setTitle(root);

		vDelTip = root.findViewById(R.id.favorite_del_tip_parent);
		vDelTip.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				exitEditMode();
				deleteBooks();
			}
		});
		vDelTip.setClickable(false);

		vFavoreteGrid = (GridView) root.findViewById(R.id.favorite_grid);
		mFavoriteAdapter = new FavoriteAdapter();
		vFavoreteGrid.setAdapter(mFavoriteAdapter);
		vFavoreteGrid.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long id) {
				enterEditMode(view);
				return true;
			}
		});

		vFavoreteGrid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int position, long arg3) {
				Book book = mFavoriteAdapter.getItem(position);
				if (isInEditMode) {
					CheckBox vCheckbox = (CheckBox) view.findViewById(R.id.favorite_book_check);
					vCheckbox.toggle();
					if (vCheckbox.isChecked()) {
						mDeleteBooks.add(book);
					} else {
						mDeleteBooks.remove(book);
					}
				} else {
					showBook(book);
				}
			}
		});

		View emptyView = root.findViewById(R.id.shared_loading_parent);
		vFavoreteGrid.setEmptyView(emptyView);

		return root;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		L.lo(TAG, "onActivityCreated");
		
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
		L.lo(TAG, "onResume");
	}

	@Override
	public void onStop() {
		L.lo(TAG, "onStop");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		L.lo(TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	public void reloadData() {
		L.d(TAG, "reloadData");

		getView().findViewById(R.id.shared_loading_progressbar).setVisibility(View.VISIBLE);

		mDeleteBooks.clear();
		mBooks.clear();
		mFavoriteAdapter.notifyDataSetInvalidated();

		loadData();
	}

	@Override
	public boolean onBackPress() {
		L.d(TAG, "onBackPress..");
		if (isInEditMode) {
			exitEditMode();
			return true;
		}
		return false;
	}

	private void loadData() {
		mBooks = mDB.findAll(Book.class, "id");
		L.d(TAG, "load favorites, books=" + mBooks);
		if (mBooks.isEmpty()) {
			showErrorView(null);
		} else {
			vDelTip.setVisibility(View.VISIBLE);
			mFavoriteAdapter.notifyDataSetChanged();
		}
	}

	private void setTitle(View root) {
		TextView vTitle = (TextView) root.findViewById(R.id.title_name);
		vTitle.setText(R.string.favorite_title);

		root.findViewById(R.id.title_back).setVisibility(View.GONE);
	}

	private void setViewEditMode(boolean enter, View view, boolean checked) {
		view.findViewById(R.id.favorite_book_check_bg).setVisibility(enter ? View.VISIBLE : View.INVISIBLE);
		CheckBox vCheckbox = (CheckBox) view.findViewById(R.id.favorite_book_check);
		vCheckbox.setVisibility(enter ? View.VISIBLE : View.INVISIBLE);
		vCheckbox.setChecked(checked);

		if (checked) {
			int position = vFavoreteGrid.getPositionForView(view);
			mDeleteBooks.add(mFavoriteAdapter.getItem(position));
		}
	}

	private void deleteBooks() {
		for (Book delBook : mDeleteBooks) {
			mDB.deleteById(Book.class, delBook.getId());
			mBooks.remove(delBook);
		}
		
		if(!mDeleteBooks.isEmpty()){
			mFavoriteAdapter.notifyDataSetChanged();
		}
		
		if(mBooks.isEmpty()){
			showErrorView(null);
		}
		
		mDeleteBooks.clear();
	}

	private void enterEditMode(View checkView) {
		if (isInEditMode) return;
		isInEditMode = true;

		vDelTip.setClickable(true);
		TextView vTipTxt = (TextView) vDelTip.findViewById(R.id.favorite_del_tip);
		vTipTxt.setText(R.string.favorite_del);
		vTipTxt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
		vFavoreteGrid.setSelector(new ColorDrawable(Color.TRANSPARENT));

		int count = vFavoreteGrid.getChildCount();
		for (int i = 0; i < count; i++) {
			View child = vFavoreteGrid.getChildAt(i);
			setViewEditMode(true, child, checkView.equals(child));
		}
	}

	private void exitEditMode() {
		if (!isInEditMode) return;
		isInEditMode = false;

		vDelTip.setClickable(false);
		TextView vTipTxt = (TextView) vDelTip.findViewById(R.id.favorite_del_tip);
		vTipTxt.setText(R.string.favorite_longclick2edit);
		vTipTxt.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		vFavoreteGrid.setSelector(getResources().getDrawable(R.drawable.bg_grid_item_selector));

		int count = vFavoreteGrid.getChildCount();
		for (int i = 0; i < count; i++) {
			View child = vFavoreteGrid.getChildAt(i);
			setViewEditMode(false, child, false);
		}
	}

	private void showBook(Book book) {
		File bookFile = DownloadService.getDownloadBookFile(getActivity(), book);
		if(bookFile!=null){
			readBook(bookFile);
			return;
		}
		
		Bundle args = new Bundle();
		args.putInt(BookFragment.PARAMS_ALBUM_ID, (int) book.getId());
		args.putInt(BookFragment.PARAMS_FROM_TAB, MainActivity.TAB_FAVORITE);
		((MainActivity) getActivity()).changedTabTo(MainActivity.TAB_BOOK, true, args);
	}

	private void readBook(File file) {
		Intent intent = new Intent(getActivity(), MuPDFActivity.class);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setType("application/pdf");
		intent.setData(Uri.fromFile(file));
		startActivity(intent);		
	}

	private void showErrorView(Exception ex) {
		View root = getView();

		root.findViewById(R.id.shared_loading_progressbar).setVisibility(View.GONE);

		TextView vTx = (TextView) root.findViewById(R.id.shared_loading_text);
		vTx.setText(R.string.favorite_empty);

		vDelTip.setVisibility(View.GONE);
	}

	private class FavoriteAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return mBooks.size();
		}

		@Override
		public Book getItem(int position) {
			return mBooks.get(position);
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view;
			final Book book = getItem(position);

			if (convertView == null) {
				view = LayoutInflater.from(getActivity()).inflate(R.layout.favorite_gird_item, parent, false);
				if (isInEditMode) {
					setViewEditMode(true, view, mDeleteBooks.contains(book));
				}
			} else {
				view = convertView;
			}
			
			ImageView vCover = (ImageView) view.findViewById(R.id.favorite_book_cover);
			mImageLoader.display(vCover, book.getCover());

			return view;
		}
	}
}
