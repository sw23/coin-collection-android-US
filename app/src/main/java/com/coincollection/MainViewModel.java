package com.coincollection;

import android.app.Application;
import android.database.SQLException;
import android.net.Uri;
import android.util.Log;

import com.spencerpages.BuildConfig;
import com.spencerpages.MainApplication;
import com.spencerpages.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainViewModel extends BaseViewModel {

    private static final String APP_NAME = MainApplication.APP_NAME; // Or get dynamically if needed

    // To hold the DatabaseAdapter instance after it's opened.
    // ViewModel should not hold a reference to Activity context, but Application context is fine.
    private DatabaseAdapter mDbAdapter = null;

    public interface DatabaseOpenCallback {
        void onDatabaseOpened(String errorMessage, DatabaseAdapter dbAdapter);
    }

    public interface TaskProgressCallback {
        void onTaskStarted(String progressMessage);
        void onTaskCompleted(String resultMessage, boolean requiresUiRefresh);
    }

    public MainViewModel() {
        super();
    }

    public void openDatabase(Application application, DatabaseOpenCallback callback) {
        getExecutorService().execute(() -> {
            String errorMessage = "";
            DatabaseAdapter localDbAdapter = null;
            try {
                // Simulate parts of BaseActivity's openDbAdapterForAsyncThread()
                // We need to get a new instance or ensure the application provides a shared one correctly.
                // For this pattern, ViewModel should own its resources or receive them appropriately.
                localDbAdapter = ((MainApplication) application).getDbAdapter();
                localDbAdapter.open();
                
                // Assign to ViewModel's instance *after* successful open
                mDbAdapter = localDbAdapter;

                if (BuildConfig.DEBUG) {
                    Log.d(APP_NAME, "Database opened successfully by MainViewModel.");
                }
            } catch (SQLException e) {
                if (BuildConfig.DEBUG) {
                    Log.e(APP_NAME, "Error opening database in MainViewModel", e);
                }
                errorMessage = application.getResources().getString(R.string.error_opening_database);
                // Ensure mDbAdapter is not assigned if open failed
                mDbAdapter = null; 
            } catch (Exception e) { // Catch any other unexpected errors
                 if (BuildConfig.DEBUG) {
                    Log.e(APP_NAME, "Unexpected error opening database in MainViewModel", e);
                }
                errorMessage = application.getResources().getString(R.string.error_opening_database) + ": " + e.getMessage();
                mDbAdapter = null;
            }

            final String finalErrorMessage = errorMessage;
            // The DatabaseAdapter instance is passed back for the Activity to use.
            // The Activity will be responsible for managing this adapter instance for its own use.
            final DatabaseAdapter finalDbAdapter = mDbAdapter; 
            
            getMainThreadHandler().post(() -> callback.onDatabaseOpened(finalErrorMessage, finalDbAdapter));
        });
    }
    
    // Method for MainActivity to get the adapter if it's already opened by this ViewModel
    // This is optional, MainActivity could also just store the adapter it receives in the callback.
    public DatabaseAdapter getDatabaseAdapter() {
        return mDbAdapter;
    }

    // Ensure the adapter is closed if the ViewModel is cleared and the adapter was opened.
    @Override
    protected void onCleared() {
        if (mDbAdapter != null) {
            // Check if it's open before closing, though DatabaseAdapter.close() might be null-safe
            // or handle its own state. Assuming it needs to be open to be closed.
            // For simplicity, just calling close. DatabaseAdapter should be robust to this.
            mDbAdapter.close();
            if (BuildConfig.DEBUG) {
                Log.d(APP_NAME, "Database closed by MainViewModel in onCleared.");
            }
            mDbAdapter = null;
        }
        super.onCleared(); // This calls executorService.shutdown()
    }

    public void importCollections(Application application, Uri fileUri, boolean isLegacyCsv, String legacyFolderName, TaskProgressCallback callback) {
        // Ensure mDbAdapter is available from openDatabase or passed differently
        if (mDbAdapter == null || !mDbAdapter.isOpen()) {
            callback.onTaskCompleted(application.getResources().getString(R.string.error_database_not_open), false);
            return;
        }

        callback.onTaskStarted(application.getResources().getString(R.string.importing_collections));

        getExecutorService().execute(() -> {
            ExportImportHelper helper = new ExportImportHelper(application.getResources(), mDbAdapter);
            String resultMessage;
            if (isLegacyCsv) {
                resultMessage = helper.importCollectionsFromLegacyCSV(legacyFolderName);
            } else {
                try (InputStream inputStream = application.getContentResolver().openInputStream(fileUri)) {
                    // Get file name (MainActivity's getFileNameFromUri can be utility or duplicated here if simple)
                    // For simplicity, we'll assume ExportImportHelper doesn't strictly need filename for import, or it's handled inside.
                    // String fileName = getFileNameFromUri(application, fileUri); // Assuming getFileNameFromUri is available or moved.
                    // Let's check ExportImportHelper: importCollectionsFromSingleCSV and importCollectionsFromJson don't use fileName.
                    
                    // A simplified way to check extension from URI path if needed, though helper methods might not require it.
                    String path = fileUri.getPath();
                    boolean isCsv = path != null && path.toLowerCase().endsWith(".csv");

                    if (isCsv) { // Crude check, proper MIME type check is better if available from URI
                        resultMessage = helper.importCollectionsFromSingleCSV(inputStream);
                    } else {
                        resultMessage = helper.importCollectionsFromJson(inputStream);
                    }
                } catch (IOException e) {
                    Log.e(APP_NAME, "Error importing collections", e);
                    resultMessage = application.getResources().getString(R.string.error_importing, e.getMessage());
                } catch (SecurityException e) {
                    Log.e(APP_NAME, "Security error importing collections", e);
                    resultMessage = application.getResources().getString(R.string.error_importing_permission, e.getMessage());
                }
            }

            final String finalResultMessage = resultMessage;
            getMainThreadHandler().post(() -> callback.onTaskCompleted(finalResultMessage, true)); // true because UI refresh is needed
        });
    }

    public void exportCollections(Application application, Uri fileUri, boolean isLegacyCsv, String legacyFolderName, boolean isSingleFileCsv, String outputFileName, TaskProgressCallback callback) {
        if (mDbAdapter == null || !mDbAdapter.isOpen()) {
            callback.onTaskCompleted(application.getResources().getString(R.string.error_database_not_open), false);
            return;
        }

        callback.onTaskStarted(application.getResources().getString(R.string.exporting_collections));

        getExecutorService().execute(() -> {
            ExportImportHelper helper = new ExportImportHelper(application.getResources(), mDbAdapter);
            String resultMessage;
            if (isLegacyCsv) {
                resultMessage = helper.exportCollectionsToLegacyCSV(legacyFolderName);
            } else {
                try (OutputStream outputStream = application.getContentResolver().openOutputStream(fileUri)) {
                    // outputFileName is passed for SAF, ExportImportHelper methods use it.
                    if (isSingleFileCsv) {
                        resultMessage = helper.exportCollectionsToSingleCSV(outputStream, outputFileName);
                    } else {
                        resultMessage = helper.exportCollectionsToJson(outputStream, outputFileName);
                    }
                } catch (IOException e) {
                    Log.e(APP_NAME, "Error exporting collections", e);
                    resultMessage = application.getResources().getString(R.string.error_exporting, e.getMessage());
                } catch (SecurityException e) {
                    Log.e(APP_NAME, "Security error exporting collections", e);
                    resultMessage = application.getResources().getString(R.string.error_exporting_permission, e.getMessage());
                }
            }

            final String finalResultMessage = resultMessage;
            getMainThreadHandler().post(() -> callback.onTaskCompleted(finalResultMessage, false)); // false as UI refresh might not be strictly needed for export
        });
    }
}
