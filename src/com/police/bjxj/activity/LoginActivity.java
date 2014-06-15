package com.police.bjxj.activity;

import net.tsz.afinal.core.AsyncTask;
import net.tsz.afinal.http.AjaxCallBack;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.police.bjxj.R;
import com.police.bjxj.accessor.Accessor;
import com.police.bjxj.accessor.BjjxException;

public class LoginActivity extends Activity {
	@SuppressWarnings("rawtypes")
	private AsyncTask task;

	private ProgressDialog mLoginDialog;
	private EditText vUserName;
	private EditText vPwd;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

		vUserName = (EditText) findViewById(R.id.login_edit_username);
		vPwd = (EditText) findViewById(R.id.login_edit_pwd);
		vPwd.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					doLogin();
					return true;
				}
				return false;
			}
		});

		findViewById(R.id.login_login).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doLogin();
			}
		});
	}

	@Override
	protected void onDestroy() {
		if (task != null) task.cancel(true);
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		if (task != null) task.cancel(true);
		super.onBackPressed();
	}

	private void doLogin() {
		String username = vUserName.getText().toString();
		String pwd = vPwd.getText().toString();

		if (TextUtils.isEmpty(username)) {
			Toast.makeText(this, R.string.login_username_empty, Toast.LENGTH_SHORT).show();
			return;
		}

		if (TextUtils.isEmpty(pwd)) {
			Toast.makeText(this, R.string.login_pwd_empty, Toast.LENGTH_SHORT).show();
			return;
		}

		task = new Accessor(new AjaxCallBack() {

			@Override
			public void onStart() {
				showLoginDialog();
			}

			@Override
			public void onFinish() {
				dismissLoginDialog();

				onSuccess(null);
			}

			@Override
			public void onSuccess(Object obj) {
				Intent intent = new Intent(LoginActivity.this, MainActivity.class);
				startActivity(intent);
				finish();
			}

			@Override
			public void onFailure(Exception t, Object attach) {
				String msg = getString(R.string.error_no_network);
				if (t instanceof BjjxException) {
					msg = t.getMessage();
				}
				Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
			}
		}).login(username, pwd);
	}

	private void dismissLoginDialog() {
		if (mLoginDialog != null) {
			mLoginDialog.dismiss();
		}
	}

	private void showLoginDialog() {
		mLoginDialog = new ProgressDialog(this);
		mLoginDialog.setCanceledOnTouchOutside(false);
		mLoginDialog.setCancelable(true);
		mLoginDialog.setMessage(getString(R.string.login_logining));
		mLoginDialog.show();
	}
}
