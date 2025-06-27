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

import android.os.AsyncTask;

/**
 * sub-class of AsyncTask
 * See: http://stackoverflow.com/questions/6450275/android-how-to-work-with-asynctasks-progressdialog
 * 
 * @deprecated This class is deprecated as of API level 30. Use AsyncTaskManager with ExecutorService instead.
 * This class is maintained for backward compatibility during the transition period.
 */
// TODO For passing the AsyncTask between Activity instances, see this post:
// http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html
// Our method is subject to the race conditions described therein :O
@Deprecated
class AsyncProgressTask extends AsyncTask<Void, Void, Void> {
    AsyncProgressInterface mListener;
    int mAsyncTaskId = 0;
    private final static int NUM_DELAY_HALF_SECONDS = 10;
    String mResultString;

    /**
     * @deprecated Constructor for deprecated AsyncProgressTask. Use AsyncOperationViewModel instead.
     */
    @Deprecated
    AsyncProgressTask(AsyncProgressInterface listener) {
        this.mListener = listener;
    }

    /**
     * @deprecated This method is deprecated. Background work is now handled by AsyncTaskManager.
     */
    @Deprecated
    @Override
    protected Void doInBackground(Void... params) {
        for (int i = 0; i < NUM_DELAY_HALF_SECONDS; i++) {
            if (mListener != null) {
                mResultString = mListener.asyncProgressDoInBackground();
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }
        return null;
    }

    /**
     * @deprecated This method is deprecated. Pre-execute work is now handled by AsyncTaskManager.
     */
    @Deprecated
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        for (int i = 0; i < NUM_DELAY_HALF_SECONDS; i++) {
            if (mListener != null) {
                mListener.asyncProgressOnPreExecute();
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * @deprecated This method is deprecated. Post-execute work is now handled by AsyncTaskManager.
     */
    @Deprecated
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
        for (int i = 0; i < NUM_DELAY_HALF_SECONDS; i++) {
            if (mListener != null) {
                mListener.asyncProgressOnPostExecute(mResultString);
                break;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}