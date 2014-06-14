package com.police.bjxj.activity;

import net.tsz.afinal.FinalBitmap;
import net.tsz.afinal.core.AsyncTask;
import net.tsz.afinal.http.AjaxCallBack;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TabWidget;
import android.widget.TextView;

import com.police.bjxj.R;
import com.police.bjxj.accessor.Accessor;
import com.police.bjxj.config.Setting;
import com.police.bjxj.util.DeviceUtil;
import com.police.bjxj.util.L;

public class MainActivity extends FragmentActivity {
	private static final String TAG = "MainActivity";

	public static final String EXTRA_TAB_INDEX = "tab_index";
	public static final String EXTRA_TAB_RELOAD = "tab_reload";

	public static final int TAB_Album = 0;
	public static final int TAB_FAVORITE = 1;
	public static final int TAB_DOWNLOAD = 2;
	public static final int TAB_SEARCH = 3;
	public static final int TAB_SETTING = 4;
	public static final int TAB_BOOK = 5;

	private TabWidget mTabWidget;

	private int mCurrentTab = -1;

	@SuppressWarnings("rawtypes")
	private AsyncTask task;

	public void onCreate(Bundle paramBundle) {
		super.onCreate(paramBundle);
		setContentView(R.layout.main);

		L.lo(TAG, "onCreate");

		// 初始化全局设置
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		Setting.SCREEN_WIDTH = metrics.widthPixels;
		Setting.SCREEN_HEIGHT = metrics.heightPixels;
		Setting.DENSITY = metrics.density;
		Setting.VERSION_NAME = DeviceUtil.getVersionName(this);
		Setting.DEVICE_ID = DeviceUtil.getDevId(this);
		Setting.CHANNEL = DeviceUtil.getChannel(this);

		L.lo(TAG, "density=" + Setting.DENSITY + ", w|h=" + Setting.SCREEN_WIDTH + "|" + Setting.SCREEN_HEIGHT);

		// init tab
		initTabs();

		// 由后台重新回到前台运行时
		int tabIndex;
		if (paramBundle != null) {
			tabIndex = paramBundle.getInt(EXTRA_TAB_INDEX);
		} else {
			tabIndex = getIntent().getIntExtra(EXTRA_TAB_INDEX, 0);
		}

		boolean tabReload = getIntent().getBooleanExtra(EXTRA_TAB_RELOAD, false);

		L.lo(TAG, "onCreate >>>> tabindex=" + tabIndex + ", reload=" + tabReload);

		changedTabTo(tabIndex, tabReload);

		loadAdvertise();
	}

	@Override
	public void onResume() {
		super.onResume();
		L.lo(TAG, "onResume");
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		int tabIndex = intent.getIntExtra(EXTRA_TAB_INDEX, mCurrentTab);
		boolean tabReload = intent.getBooleanExtra(EXTRA_TAB_RELOAD, false);
		changedTabTo(tabIndex, tabReload);
	}

	@Override
	protected void onStop() {
		super.onStop();
		L.lo(TAG, "onStop");
	}

	@Override
	protected void onDestroy() {
		L.lo(TAG, "onDestroy");
		if (task != null) task.cancel(true);
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		L.lo(TAG, "onSaveInstanceState");
		outState.putInt(EXTRA_TAB_INDEX, mCurrentTab);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void reloadFrgmentContent(int... tabs) {
		// notify all fragment on screen, neither hidden or showing.
		FragmentManager fm = getSupportFragmentManager();
		for (int i = 0; i < tabs.length; i++) {
			Fragment fragment = fm.findFragmentByTag(tabs[i] + "");
			if (fragment != null && fragment instanceof IFragment) {
				((IFragment) fragment).reloadData();
			}
		}
	}

	/** initialize tab control. */
	private void initTabs() {
		mTabWidget = (TabWidget) findViewById(android.R.id.tabs);

		LinearLayout.LayoutParams lp = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
		mTabWidget.addView(getTabView(R.string.main_tab_0, R.drawable.ic_main_tab0), lp);
		mTabWidget.addView(getTabView(R.string.main_tab_1, R.drawable.ic_main_tab1), lp);
		mTabWidget.addView(getTabView(R.string.main_tab_2, R.drawable.ic_main_tab2), lp);
		mTabWidget.addView(getTabView(R.string.main_tab_3, R.drawable.ic_main_tab3), lp);
		mTabWidget.addView(getTabView(R.string.main_tab_4, R.drawable.ic_main_tab4), lp);

		for (int i = 0; i < mTabWidget.getChildCount(); i++) {
			final int index = i;
			View v = mTabWidget.getChildAt(i);
			v.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					changedTabTo(index, false);
				}
			});
		}
	}

	private View getTabView(int textId, int ic) {
		TextView v = (TextView) getLayoutInflater().inflate(R.layout.main_tab_item, mTabWidget, false);
		v.setCompoundDrawablesWithIntrinsicBounds(0, ic, 0, 0);
		v.setText(textId);
		return v;
	}

	public void changedTabTo(int index, boolean reloadData) {
		changedTabTo(index, reloadData, null);
	}

	public void changedTabTo(int index, boolean reloadData, Bundle args) {
		if (mCurrentTab == index && !reloadData) return;

		L.lo(TAG, "changedTabTo index=" + index);

		View container = findViewById(R.id.main_content);
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction transaction = fm.beginTransaction();

		// 下面6行诡异的代码是为了解决一个bug
		// 当app推到后台,不久进程被系统kill,用户重新启动app,此时会重新走oncreate, 但是fm里面仍然存在三个tab的fragment的实例
		// 导致现象是界面出现3个tab层叠在一起, 而我们想要的是当前tab显示,其他tab隐藏
		// 所以此处强制做了一个hide的调用.
		// 真心不明白为什么进程都kill掉了,SupportFragmentManager中依然还有实例保存着.
		/* ==================================================== */
		/*
		 * Fragment f0 = fm.findFragmentByTag(TAB_FAVORITE + ""); Fragment f1 = fm.findFragmentByTag(TAB_DOWNLOAD + "");
		 * Fragment f2 = fm.findFragmentByTag(TAB_Album + ""); if (f0 != null) transaction.hide(f0); if (f1 != null)
		 * transaction.hide(f1); if (f2 != null) transaction.hide(f2);
		 */
		/* ====================================================== */

		Fragment currFragment = fm.findFragmentByTag(mCurrentTab + "");
		L.lo(TAG, "hide currtab[" + mCurrentTab + "] currFramgnet=" + currFragment);
		if (currFragment != null) {
			currFragment.setMenuVisibility(false);
			currFragment.setUserVisibleHint(false);
			transaction.hide(currFragment);
		}

		boolean isNew = false;
		Fragment target = fm.findFragmentByTag(index + "");
		if (target == null) {
			target = newTabFragment(index);
			transaction.add(container.getId(), target, index + "");
			isNew = true;
			L.lo(TAG, "add fragment=" + target);
		} else {
			L.lo(TAG, "show framgnet=" + target);
			transaction.show(target);
		}

		target.setMenuVisibility(true);
		target.setUserVisibleHint(true);

		transaction.commitAllowingStateLoss();

		if (target instanceof IFragment) {
			((IFragment) target).setParams(args);

			if (reloadData && !isNew) {
				((IFragment) target).reloadData();
			}
		}

		if (mCurrentTab != -1) {
			View tab = mTabWidget.getChildAt(mCurrentTab);
			if (tab != null) tab.setSelected(false);
		}

		mCurrentTab = index;
		View tab = mTabWidget.getChildAt(mCurrentTab);
		if (tab != null) tab.setSelected(true);
	}

	private Fragment newTabFragment(int index) {
		Fragment target = null;
		switch (index) {
		case TAB_Album:
			target = new AlbumFragment();
			break;
		case TAB_FAVORITE:
			target = new FavoriteFragment();
			break;
		case TAB_DOWNLOAD:
			target = new DownloadCenterFragment();
			break;
		case TAB_SEARCH:
			target = new SearchFragment();
			break;
		case TAB_SETTING:
			target = new SettingFragment();
			break;
		case TAB_BOOK:
			target = new BookFragment();
			break;
		}
		return target;
	}

	private void loadAdvertise() {
		task = new Accessor(new AjaxCallBack() {
			@Override
			public void onSuccess(Object obj) {
				try {
					JSONObject json = new JSONObject((String) obj);
					String imgUrl = json.getString("img");
					String clickUrl = json.getString("url");
					setAdvertise(imgUrl, clickUrl);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onFailure(Exception t, Object attach) {
				// TODO Auto-generated method stub
				super.onFailure(t, attach);
			}
		}).getAdvertise();
	}

	private void setAdvertise(String imgUrl, String clickUrl) {
		if (imgUrl == null || imgUrl.isEmpty()) return;
		ImageView vAdvertise = (ImageView) findViewById(R.id.main_advertise);
		vAdvertise.setVisibility(View.VISIBLE);
		vAdvertise.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
			}
		});

		FinalBitmap imageLoader = FinalBitmap.create(MainActivity.this);
		imageLoader.configDiskCachePath(Setting.IMG_CACHE_DIR).configLoadingImage(null);
		imageLoader.display(vAdvertise, imgUrl);
	}

	@Override
	public void onBackPressed() {
		boolean intercept = false;
		L.d(TAG, "onBackPressed...");

		FragmentManager fm = getSupportFragmentManager();
		Fragment currFragment = fm.findFragmentByTag(mCurrentTab + "");
		if (currFragment instanceof IFragment) {
			intercept = ((IFragment) currFragment).onBackPress();
		}

		if (intercept) return;

		if (mCurrentTab != 0) {
			changedTabTo(0, false);
			intercept = true;
		} else {
			AlertDialog dialog = new AlertDialog.Builder(this).setTitle("提示").setMessage(R.string.main_exit)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
							exit();
						}
					}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					}).create();
			dialog.show();
			intercept = true;
		}

		if (!intercept) super.onBackPressed();
	}

	private void exit() {
		finish();
	}
}
