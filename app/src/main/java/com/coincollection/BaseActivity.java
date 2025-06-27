/*
 * Coin Collection, an Android app that helps users track the coins that they've collected
 * Copyright (C) 2010-2016 Andrew Williams
 *
 * This file is part of Coin Collection.
 *
 * Coin Collection is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Coin Collection is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Coin Collection.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.coincollection;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.SQLException;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.coincollection.helper.NonLeakingAlertDialogBuilder;
import com.spencerpages.BuildConfig;
import com.spencerpages.MainApplication;
import com.spencerpages.R;

/**
 * Base activity containing shared functions and resources between the activities
 */
public class BaseActivity extends AppCompatActivity implements AsyncProgressInterface {

    // Common intent variables
    public static final String UNIT_TEST_USE_ASYNC_TASKS = "unit-test-use-async-tasks";
    protected boolean mUseAsyncTasks = true;

    // Async Task info - Modern async handling
    protected AsyncOperationViewModel mAsyncViewModel; // Lifecycle-aware async handling
    public static final int TASK_OPEN_DATABASE = 0;
    public static final int TASK_IMPORT_COLLECTIONS = 1;
    public static final int TASK_CREATE_UPDATE_COLLECTION = 2;
    public static final int TASK_EXPORT_COLLECTIONS = 3;

    // Common activity variables
    protected final Context mContext = this;
    protected ProgressDialog mProgressDialog;
    public Resources mRes;
    protected Intent mCallingIntent;
    public DatabaseAdapter mDbAdapter = null;
    protected boolean mOpenDbAdapterInOnCreate = true;
    protected ActionBar mActionBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Add a manual inset handler
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        if (BuildConfig.DEBUG) {
            // Set StrictMode policies to help debug potential issues
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .permitDiskReads() // TODO - Fix these and remove
                    .permitDiskWrites() // TODO - Fix these and remove
                    .penaltyLog()
                    //.penaltyDeath() // TODO - Uncomment once fixed
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    //.penaltyDeath() // TODO - Uncomment once fixed
                    .build());
        }

        // Setup variables used across all activities
        mRes = getResources();
        mCallingIntent = getIntent();
        mUseAsyncTasks = mCallingIntent.getBooleanExtra(UNIT_TEST_USE_ASYNC_TASKS, true);
        mActionBar = getSupportActionBar();

        // Initialize the new async operation ViewModel
        mAsyncViewModel = new ViewModelProvider(this).get(AsyncOperationViewModel.class);
        
        // Set synchronous mode for unit tests
        if (!mUseAsyncTasks) {
            mAsyncViewModel.setSynchronousMode(true);
        }
        
        // Observe async operation state
        mAsyncViewModel.getIsTaskRunning().observe(this, isRunning -> {
            // This will be used by subclasses to update UI state
        });
        
        mAsyncViewModel.getTaskResult().observe(this, result -> {
            if (result != null && !result.isEmpty()) {
                // Handle the result - subclasses can override this behavior
                asyncProgressOnPostExecute(result);
            }
        });

        // In most cases we want to open the database adapter right away, but in MainActivity
        // we do this on the async task since the upgrade may take a while
        if (mOpenDbAdapterInOnCreate) {
            openDbAdapterForUIThread();
        }
    }

    /**
     * This method should be called when mDbAdapter can be opened on the UI thread
     */
    public void openDbAdapterForUIThread() {
        try {
            mDbAdapter = ((MainApplication) getApplication()).getDbAdapter();
            mDbAdapter.open();
        } catch (SQLException e) {
            showCancelableAlert(mRes.getString(R.string.error_opening_database));
            finish();
        }
    }

    /**
     * This method should be called when mDbAdapter can be opened on the UI thread
     *
     * @return An error message if the open failed, otherwise -1
     */
    public String openDbAdapterForAsyncThread() {
        try {
            mDbAdapter = ((MainApplication) getApplication()).getDbAdapter();
            mDbAdapter.open();
        } catch (SQLException e) {
            return mRes.getString(R.string.error_opening_database);
        }
        return "";
    }

    /**
     * This should be overridden by Activities that use the AsyncTask
     * - This is method contains the work that needs to be performed on the async task
     *
     * @return a string result to display, or "" if no result
     */
    @Override
    public String asyncProgressDoInBackground() {
        return "";
    }

    /**
     * This should be overridden by Activities that use the AsyncTask
     * - This is method is called on the UI thread ahead of executing DoInBackground
     */
    @Override
    public void asyncProgressOnPreExecute() {
    }

    /**
     * This should be overridden by Activities that use the AsyncTask
     * - This is method is called on the UI thread after executing DoInBackground
     * - Activities should call super.asyncProgressOnPostExecute to display the error
     *
     * @param resultStr a string result to display, or "" if no result
     */
    @Override
    public void asyncProgressOnPostExecute(String resultStr) {
        if (!resultStr.isEmpty()) {
            showCancelableAlert(resultStr);
        }
    }

    /**
     * Activities that make use of the async task should call this once their UI state
     * is ready for an already running async task to call back
     */
    protected void setActivityReadyForAsyncCallbacks() {
        // New async system is automatically lifecycle-aware, no manual setup needed
    }

    /**
     * Displays a message to the user
     *
     * @param text The text to be displayed
     */
    public void showCancelableAlert(String text) {
        showAlert(newBuilder().setMessage(text).setCancelable(true));
    }

    // https://raw.github.com/commonsguy/cw-android/master/Rotation/RotationAsync/src/com/commonsware/android/rotation/async/RotationAsync.java
    // TODO Consider only using one of onSaveInstanceState and onRetainNonConfigurationInstanceState
    // TODO Also, read the notes on this better and make sure we are using it correctly
    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // With lifecycle-aware ViewModels, we no longer need to manually retain async tasks
        // The ViewModel automatically survives configuration changes
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            dismissProgressDialog();
        }
        return null;
    }

    @Override
    public void onPause() {
        // Dismiss any open alerts to prevent memory leaks
        dismissAllAlerts();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        // The new ViewModel will automatically clean up when the activity is destroyed
        // due to its lifecycle-aware nature
        
        super.onDestroy();
    }

    /**
     * Create a new progress dialog
     */
    protected void createProgressDialog(String message) {
        dismissProgressDialog();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setMessage(message);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setProgress(0);
        mProgressDialog.show();
    }

    /**
     * Hides the progress dialog
     */
    protected void dismissProgressDialog() {
        if (mProgressDialog != null) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            mProgressDialog = null;
        }
    }

    /**
     * Hide the dialog and finish the activity
     */
    protected void completeProgressDialogAndFinishActivity() {
        dismissProgressDialog();
        this.finish();
    }

    /**
     * Builds the list element for displaying collections
     *
     * @param item Collection list info item
     * @param view view that needs to be populated
     * @param res  Used to access project string values
     */
    public static void buildListElement(CollectionListInfo item, View view, Resources res) {

        String tableName = item.getName();

        int total = item.getCollected();
        if (tableName != null) {

            ImageView image = view.findViewById(R.id.coinImageView);
            if (image != null) {
                image.setBackgroundResource(item.getCoinImageIdentifier());
            }

            TextView nameTextView = view.findViewById(R.id.collectionNameTextView);
            if (nameTextView != null) {
                nameTextView.setText(tableName);
            }

            TextView progressTextView = view.findViewById(R.id.progressTextView);
            if (progressTextView != null) {
                progressTextView.setText(res.getString(R.string.collection_completion_template, total, item.getMax()));
            }

            TextView completionTextView = view.findViewById(R.id.completeTextView);
            if (total >= item.getMax()) {
                // The collection is complete
                if (completionTextView != null) {
                    completionTextView.setText(res.getString(R.string.collection_complete));
                }
            } else {
                completionTextView.setText("");
            }
        }
    }

    /**
     * Create a help dialog to show the user how to do something
     *
     * @param helpStrKey key uniquely identifying this boolean key
     * @param helpStrId  Help message to display
     * @return true if the help dialog was displayed, otherwise false
     */
    public boolean createAndShowHelpDialog(final String helpStrKey, int helpStrId) {
        final SharedPreferences mainPreferences = this.getSharedPreferences(MainApplication.PREFS, MODE_PRIVATE);
        final Resources res = this.getResources();
        if (mainPreferences.getBoolean(helpStrKey, true)) {
            showAlert(newBuilder()
                    .setMessage(res.getString(helpStrId))
                    .setCancelable(false)
                    .setPositiveButton(res.getString(R.string.okay_exp), (dialog, id) -> {
                        dialog.dismiss();
                        SharedPreferences.Editor editor = mainPreferences.edit();
                        editor.putBoolean(helpStrKey, false);
                        editor.apply();
                    }));
            return true;
        }
        return false;
    }

    /**
     * Creates a new alerter builder and cleans up any previous builders,
     * to prevent memory leaks
     *
     * @return new builder object
     */
    protected NonLeakingAlertDialogBuilder newBuilder() {
        return new NonLeakingAlertDialogBuilder(this);
    }

    /**
     * Uses builder to create and show an alert
     *
     * @param builder to use to create alert
     */
    protected void showAlert(NonLeakingAlertDialogBuilder builder) {
        AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Cleans up any notifications currently shown to users
     */
    protected void dismissAllAlerts() {
        dismissProgressDialog();
    }

    /**
     * Create and kick-off an async task to finish long-running tasks
     * Uses modern async handling with lifecycle management
     *
     * @param taskId type of task
     */
    public void kickOffAsyncProgressTask(int taskId) {
        // Use new async system for better lifecycle management
        mAsyncViewModel.executeAsyncOperation(
            taskId,
            this::asyncProgressDoInBackground, // Background work
            this::asyncProgressOnPreExecute,   // Pre-execute work
            result -> asyncProgressOnPostExecute(result)   // Post-execute work
        );
    }

    /**
     * Applies window insets to the view, setting padding based on system bars
     *
     * @param view The view to apply insets to
     */
    void applyWindowInsets(View view) {
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
            );
            return insets;
        });
    }
}
