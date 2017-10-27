package com.rockchip.tutk.dialog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.rockchip.tutk.R;
import com.rockchip.tutk.TUTKDevice;
import com.rockchip.tutk.TUTKManager;
import com.rockchip.tutk.activity.CameraActivity;
import com.rockchip.tutk.activity.VideoSettingActivity;
import com.rockchip.tutk.constants.Constants;
import com.rockchip.tutk.db.AccountLoader;
import com.rockchip.tutk.db.AuthorisationOperator;
import com.rockchip.tutk.db.UserInfo;
import com.rockchip.tutk.utils.MsgManager;
import com.tutk.IOTC.AVAPIs;
import com.tutk.IOTC.IOTCAPIs;

import java.util.ArrayList;
import java.util.List;


/**
 * A login screen that offers login via email/password.
 */
public class DeviceLoginDialog extends Dialog implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener {


    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    public static final Uri uri = Uri.parse("content://com.rockchip.authorisation");

    // UI references.
    private AutoCompleteTextView mAccountView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private String mUID;
    private TUTKDevice mTUTKDevice;
    private String mDeviceName;
    ConnectThread connectThread;
    private Handler mHandler = new Handler();
    int loginType = -1;
    AuthorisationOperator authorisationOperator;
    private String TAG = "LoginActivity";
    private Activity mContext;
    private Intent mIntent;
    private String mExtra = null;
    Handler mUIHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case 0:
                    DeviceLoginDialog.this.showProgress(false);
                    return;
                case 1:
                    mAccountView.setError(mContext.getString(R.string.error_incorrect_password));
                    return;
            }
        }
    };

    public DeviceLoginDialog(Activity paramActivity, Intent paramIntent, String paramString)
    {
        super( paramActivity, R.style.AppTheme);
        this.mContext = paramActivity;
        this.mIntent = paramIntent;
        this.mExtra = paramString;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        authorisationOperator = new AuthorisationOperator(mContext);
        // Set up the login form.
        mAccountView = (AutoCompleteTextView) findViewById(R.id.account);
        //mAccountView.setOnItemSelectedListener(this);
        mAccountView.setOnItemClickListener(this);

        populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        if (mIntent == null)
            return;
        loginType = mIntent.getIntExtra(TUTKManager.TYPE, -1);
        mUID = mIntent.getStringExtra(TUTKManager.TUTK_UID);
        if (mUID != null ){// && mUID.length() == 20) {
            setTitle(mUID);
        }

        if (mUID != null ){// && mUID.length() == 20) {
            mTUTKDevice = TUTKManager.getByUID(mUID);
        }

        mDeviceName = mIntent.getStringExtra(TUTKManager.TUTK_DEVICE_NAME);
        if (mDeviceName != null && mDeviceName.length() > 0) {
            setTitle(mDeviceName);
        }

        //test
        mAccountView.setText("admin");
        mPasswordView.setText("888888");
        if (this.mExtra == null)
        {
            mEmailSignInButton.performClick();
        }
        else {
            requestPasswd();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                DeviceLoginDialog.this.dismiss();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void populateAutoComplete() {
        if (Build.VERSION.SDK_INT >= 14) {
            // Use ContactsContract.Profile (API 14+)
            mContext.getLoaderManager().initLoader(0, null, this);
        } else if (Build.VERSION.SDK_INT >= 8) {
            // Use AccountManager (API 8+)
            new SetupAccoountAutoCompleteTask().execute(null, null);
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mAccountView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String account = mAccountView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            mPasswordView.setError(mContext.getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(account)) {
            mAccountView.setError(mContext.getString(R.string.error_field_required));
            focusView = mAccountView;
            cancel = true;
        } else if (!isAccountValid(account)) {
            mAccountView.setError(mContext.getString(R.string.error_invalid_email));
            focusView = mAccountView;
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

            Intent intent;
            if (loginType == 0) {
                intent = new Intent(mContext, CameraActivity.class);
            } else {
                intent = new Intent(mContext, VideoSettingActivity.class);
            }
            intent.putExtra(TUTKManager.TUTK_UID, mUID);
            intent.putExtra(TUTKManager.TUTK_DEVICE_NAME, mTUTKDevice.getDeviceInfo().getDeviceName());
            connectThread = new ConnectThread(mTUTKDevice, intent, account, password);
            connectThread.start();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (connectThread.isCancle()) {
                        return;
                    }
                    connectThread.cancle();
                    showProgress(false);
                    Toast.makeText(mContext, R.string.device_list_fragment_connet_error, Toast.LENGTH_SHORT).show();
                }
            }, 2500);
        }
    }

    private boolean isAccountValid(String account) {
        //TODO: Replace this with your own logic
        return account.length() > 3;
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = mContext.getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }


    private void addAccountsToAutoComplete(List<String> accountCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(mContext,
                        android.R.layout.simple_dropdown_item_1line, accountCollection);

        mAccountView.setAdapter(adapter);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new AccountLoader(mContext);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        List<String> accounts = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            String account = cursor.getString(cursor.getColumnIndex("account"));
            if (!accounts.contains(account)){
                accounts.add(account);
            }
            cursor.moveToNext();
        }
        addAccountsToAutoComplete(accounts);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        String account = (String) adapterView.getSelectedItem();
        Log.d(TAG, String.format("account:%s", account));
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        String account = (String) adapterView.getItemAtPosition(i);
        Log.d(TAG, String.format("account:%s", account));
        List<UserInfo> byUid = authorisationOperator.findByUid(mUID);
        for (UserInfo userInfo : byUid) {
            if (userInfo.getUid().trim().equals(mUID) && userInfo.getAccount().trim().equals(account)) {
                mPasswordView.setText(userInfo.getPassword());
            }
        }
    }

    class SetupAccoountAutoCompleteTask extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... voids) {
            ArrayList<String> accountsCollection = new ArrayList<>();
            Cursor cursor = authorisationOperator.findRecord();
            while (cursor.moveToNext()) {
                String email = cursor.getString(cursor.getColumnIndex("account"));
                accountsCollection.add(email);
            }
            cursor.close();

            return accountsCollection;
        }

        @Override
        protected void onPostExecute(List<String> accountsCollection) {
            addAccountsToAutoComplete(accountsCollection);
        }
    }

    class ConnectThread extends Thread {
        TUTKDevice device;
        Intent successIntent;
        private boolean cancle = false;

        private final String mAccount;
        private final String mPassword;

        public ConnectThread(TUTKDevice device, Intent intent, String user, String pasword) {
            this.device = device;
            this.successIntent = intent;
            this.mAccount = user;
            this.mPassword = pasword;
        }

        @Override
        public void run() {
            super.run();
            final int ret = device.connect();

            if (ret >= 0 && !isCancle()) {
                int logon = -1;
                logon = device.login(mAccount, mPassword);
                if (logon >= 0) {
                    mContext.startActivity(successIntent);
                    mUIHandler.sendEmptyMessage(0);

                    List<UserInfo> byUid = authorisationOperator.findByUid(mUID);
                    boolean update = false;
                    if (byUid.size() > 0) {
                        for (UserInfo userInfo : byUid) {
                            if (userInfo.getAccount().trim().equals(mAccount)) {
                                userInfo.setPassword(mPassword);
                                authorisationOperator.updateRecord(userInfo, mUID, mAccount);
                                update = true;
                                mTUTKDevice.setCurrenUser(userInfo);
                            }
                        }
                    }
                    if (!update) {
                        UserInfo userInfo = new UserInfo();
                        userInfo.setUid(mUID);
                        userInfo.setAccount(mAccount);
                        userInfo.setPassword(mPassword);
                        long l = authorisationOperator.addRecord(userInfo);
                        if (l > 0) {
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mContext.getContentResolver().notifyChange(uri, null);
                                }
                            });
                        }
                        mTUTKDevice.setCurrenUser(userInfo);
                    }
                    cancle();
                } else {
                    device.disconnect();
                    final int finalLogon = logon;
                    mUIHandler.sendEmptyMessage(0);
                    if (finalLogon == AVAPIs.AV_ER_WRONG_VIEWACCorPWD) {
                        mUIHandler.sendEmptyMessage(1);
                        //Toast.makeText(LoginActivity.this, getString(R.string.error_incorrect_password), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext, String.format("error [login=%d]", finalLogon), Toast.LENGTH_SHORT).show();
                    }
                    cancle();
                }

                //startActivity(successIntent);
                return;
            }

            if (isCancle()) {
                return;
            }
            Message msg = new Message();
            msg.what = Constants.UserMSG.DEVICEERROR;
            if (ret == IOTCAPIs.IOTC_ER_DEVICE_EXCEED_MAX_SESSION) {
                //Toast.makeText(LoginActivity.this, getString(R.string.error_device_exceed_max_session), Toast.LENGTH_SHORT).show();
                msg.obj = mContext.getString(R.string.error_device_exceed_max_session);
            } else if (ret == IOTCAPIs.IOTC_ER_DEVICE_OFFLINE) {
//                        Toast.makeText(LoginActivity.this, getString(R.string.error_device_offline), Toast.LENGTH_SHORT).show();
                msg.obj = mContext.getString(R.string.error_device_offline);
            } else if (ret == IOTCAPIs.IOTC_ER_CAN_NOT_FIND_DEVICE) {
//                        Toast.makeText(LoginActivity.this, getString(R.string.error_can_not_find_device), Toast.LENGTH_SHORT).show();
                msg.obj = mContext.getString(R.string.error_can_not_find_device);
            } else {
//                        Toast.makeText(LoginActivity.this, String.format("error [ret=%d]", ret), Toast.LENGTH_SHORT).show();
                msg.obj = String.format("error [ret=%d]", ret);
            }
            MsgManager.sendToHandler("FragementCamera",msg);
            DeviceLoginDialog.this.dismiss();
        }

        public void cancle() {
            Log.d("ConnectThread", "cancle");
            synchronized (this) {
                cancle = true;
            }

        }

        public boolean isCancle() {
            synchronized (this) {
                return cancle;
            }
        }
    }

    private void requestPasswd(){
        try{
            List<UserInfo> byUid = authorisationOperator.findByUid(mUID);
            for (UserInfo userInfo : byUid) {
                if (userInfo.getUid().trim().equals(mUID)
                        && userInfo.getAccount().trim().equals(mAccountView.getText().toString())) {
                    mPasswordView.setText(userInfo.getPassword());
                    Log.v(TAG, "finish to reset pwd content");
                }
            }
        } catch (Exception e){
            Log.e(TAG, "happen error when reset passwd");
            e.printStackTrace();
        }
    }
}

