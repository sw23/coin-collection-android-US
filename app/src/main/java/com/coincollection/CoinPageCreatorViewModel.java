package com.coincollection;

import android.app.Application;
import android.database.SQLException;
import android.util.Log;

import com.spencerpages.BuildConfig;
import com.spencerpages.MainApplication; // For APP_NAME
import com.spencerpages.R; // For string resources

import java.util.ArrayList;

// Assuming MainViewModel.TaskProgressCallback is general enough to be reused.
// If not, a specific one for CoinPageCreator could be defined.
// For now, we'll plan to use MainViewModel.TaskProgressCallback.

public class CoinPageCreatorViewModel extends BaseViewModel {

    private static final String APP_NAME = MainApplication.APP_NAME;

    public CoinPageCreatorViewModel() {
        super();
    }

    public void saveCollection(
            Application application,
            DatabaseAdapter dbAdapter,
            CollectionListInfo collectionListInfo,
            ArrayList<CoinSlot> coinList,
            int displayOrder, // Only used for new collections
            boolean isExistingCollection, // To differentiate between create and update
            CollectionListInfo existingCollectionInfo, // Used for update
            MainViewModel.TaskProgressCallback callback) {

        if (dbAdapter == null || !dbAdapter.isOpen()) {
            callback.onTaskCompleted(application.getResources().getString(R.string.error_database_not_open), false);
            return;
        }

        callback.onTaskStarted(application.getResources().getString(R.string.creating_collection));

        getExecutorService().execute(() -> {
            String resultMessage = "";
            boolean success = false;
            try {
                if (!isExistingCollection) {
                    dbAdapter.createAndPopulateNewTable(collectionListInfo, displayOrder, coinList);
                } else {
                    // existingCollectionInfo should not be null if isExistingCollection is true
                    if (existingCollectionInfo != null) {
                        String oldTableName = existingCollectionInfo.getName();
                        dbAdapter.updateExistingCollection(oldTableName, collectionListInfo, coinList);
                    } else {
                        // This case should ideally not happen if logic in Activity is correct
                        throw new IllegalArgumentException("ExistingCollectionInfo cannot be null when updating an existing collection.");
                    }
                }
                success = true;
                // No specific success message needed, empty string implies success to callback
            } catch (SQLException e) {
                Log.e(APP_NAME, "Error saving collection", e);
                resultMessage = application.getResources().getString(R.string.error_creating_database);
            } catch (IllegalArgumentException e) { // Catching the specific exception for existingCollectionInfo
                Log.e(APP_NAME, "Error saving collection: " + e.getMessage(), e);
                resultMessage = e.getMessage(); // Or a generic error message
            }

            final String finalResultMessage = resultMessage;
            // UI refresh is not directly handled by CoinPageCreator finishing, but MainActivity will refresh.
            // Pass false for requiresUiRefresh as this activity typically finishes.
            getMainThreadHandler().post(() -> callback.onTaskCompleted(finalResultMessage, false));
        });
    }
}
