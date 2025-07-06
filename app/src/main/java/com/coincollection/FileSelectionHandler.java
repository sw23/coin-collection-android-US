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

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;

/**
 * Handles file selection operations for import and export functionality.
 * This class encapsulates the file selection logic to make it testable and
 * separate from the activity lifecycle.
 */
public class FileSelectionHandler {
    
    /**
     * Interface for handling file selection results
     */
    public interface FileSelectionCallback {
        void onFileSelected(Uri fileUri);
        void onSelectionCancelled();
        void onError(String errorMessage);
    }

    /**
     * Creates an intent for selecting import files
     * 
     * @return Intent configured for import file selection
     */
    public Intent createImportFileIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"text/comma-separated-values", "text/csv", "application/json"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The files should preferably be placed in the downloads folder
            Uri pickerInitialUri = Uri.parse(Environment.DIRECTORY_DOWNLOADS);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        }
        
        return intent;
    }

    /**
     * Creates an intent for selecting export file location
     * 
     * @param isExportSingleFileCsv true if exporting as CSV, false for JSON
     * @param dateString date string for default filename
     * @return Intent configured for export file selection
     */
    public Intent createExportFileIntent(boolean isExportSingleFileCsv, String dateString) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        
        if (isExportSingleFileCsv) {
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_TITLE, "coin-collection-" + dateString + ".csv");
        } else {
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "coin-collection-" + dateString + ".json");
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The files should preferably be placed in the downloads folder
            Uri pickerInitialUri = Uri.parse(Environment.DIRECTORY_DOWNLOADS);
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri);
        }
        
        return intent;
    }

    /**
     * Processes the result from file selection activity
     * 
     * @param resultData Intent containing the selected file URI
     * @param callback Callback to handle the result
     */
    public void handleFileSelectionResult(Intent resultData, FileSelectionCallback callback) {
        if (resultData != null && resultData.getData() != null) {
            callback.onFileSelected(resultData.getData());
        } else {
            callback.onSelectionCancelled();
        }
    }
}