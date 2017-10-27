package com.rockchip.tutk.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKManager;
import com.rockchip.tutk.command.DevicePassword;
import com.rockchip.tutk.db.AuthorisationOperator;
import com.rockchip.tutk.db.UserInfo;
import com.tutk.IOTC.AVAPIs;

import static com.tutk.IOTC.AVIOCTRLDEFs.IOTYPE_USER_IPCAM_SETPASSWORD_REQ;

public class PasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private String mUID;
    private TUTKDevice mTUTKDevice;
    private String mDeviceName;
    private ActionBar mActionBar;
    private EditText mPasswordView,mPassword2View;
    private View mProgressView;
    private View mPasswordFormView;
    private String TAG = "PasswordActivity";
    private Handler mHandler =new Handler();
    AuthorisationOperator authorisationOperator;
    Button mSubmit;
    private boolean mIsResetPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password);
        authorisationOperator=new AuthorisationOperator(this);
        mPasswordFormView = findViewById(R.id.password_form);
        mProgressView = findViewById(R.id.password_progress);

        Intent intent = getIntent();
        if (intent == null) return;

        mUID = intent.getStringExtra(TUTKManager.TUTK_UID);
        if (mUID != null){//} && mUID.length() == 20) {
            setTitle(mUID);
        }

        if (mUID != null){// && mUID.length() == 20) {
            mTUTKDevice = TUTKManager.getByUID(mUID);
        }

        mDeviceName = intent.getStringExtra(TUTKManager.TUTK_DEVICE_NAME);
        if (mDeviceName != null && mDeviceName.length() > 0) {
            setTitle(mDeviceName);
        }

        mActionBar = getSupportActionBar();
        mActionBar.setHomeButtonEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        mPasswordView =(EditText) findViewById(R.id.password);

        mPassword2View = (EditText) findViewById(R.id.password2);
        mPassword2View.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.submit || id == EditorInfo.IME_NULL) {
                    attemptSubmit();
                    return true;
                }
                return false;
            }
        });

        mSubmit= (Button) findViewById(R.id.submit_button);
        mSubmit.setOnClickListener(this);
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }

    private void attemptSubmit() {

        // Reset errors.
        mPasswordView.setError(null);
        mPassword2View.setError(null);

        // Store values at the time of the login attempt.

        final String password = mPasswordView.getText().toString();
        String password2 = mPassword2View.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(password)){
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }else if (TextUtils.isEmpty(password2)) {
            mPassword2View.setError(getString(R.string.error_field_required));
            focusView = mPassword2View;
            cancel = true;
        } else if (!isPasswordValid(password2)) {
            mPassword2View.setError(getString(R.string.error_invalid_password));
            focusView = mPassword2View;
            cancel = true;
        }else if (!password2.equals(password)){
            mPassword2View.setError(getString(R.string.error_invalid_password_equal));
            focusView = mPassword2View;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            final ProcessThread processThread=new ProcessThread(new Runnable() {
                @Override
                public void run() {
                    int ret;
                    UserInfo currentUser = mTUTKDevice.getCurrentUser();
                    DevicePassword devicePassword =new DevicePassword(currentUser.getPassword(),password);
                    String json = devicePassword.Json();
                    byte[] bytes = json.getBytes();
                    ret = AVAPIs.avSendIOCtrl(mTUTKDevice.getSession().getAVIndex(), IOTYPE_USER_IPCAM_SETPASSWORD_REQ, bytes, bytes.length);
                    Log.d(TAG, String.format("[ret=%d] IOTYPE_USER_IPCAM_SETPASSWORD_REQ:%s" ,ret,json));
                    if (ret>=0){
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                showProgress(false);
                                Toast.makeText(getApplicationContext(),"修改成功",Toast.LENGTH_SHORT).show();
                            }
                        });
                        currentUser.setPassword(password);
                        authorisationOperator.updateRecord(currentUser,currentUser.getUid(),currentUser.getAccount());
                        mIsResetPwd = true;
                    }else{
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                showProgress(false);
                                Toast.makeText(getApplicationContext(),"修改失败",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    finish();
                }
            });
            processThread.start();

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    processThread.setCancle(true);
                }
            }, 3000);
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mPasswordFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mPasswordFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mPasswordFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mPasswordFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onClick(View view) {
        attemptSubmit();
    }

    class  ProcessThread extends Thread{
        boolean cancle;

        public ProcessThread(Runnable target) {
            super(target);
        }

        public boolean isCancle() {
            return cancle;
        }

        public void setCancle(boolean cancle) {
            this.cancle = cancle;
        }
    }

    @Override
    public void finish() {
        if (mIsResetPwd) {
            setResult(Activity.RESULT_OK);
        }
        super.finish();
    }
}
