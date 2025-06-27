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

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Background task executor that replaces deprecated AsyncTask
 * See: http://stackoverflow.com/questions/6450275/android-how-to-work-with-asynctasks-progressdialog
 */
// TODO For passing tasks between Activity instances, see this post:
// http://www.androiddesignpatterns.com/2013/04/retaining-objects-across-config-changes.html
// Our method is subject to the race conditions described therein :O
class AsyncProgressTask {
    AsyncProgressInterface mListener;
    int mAsyncTaskId = 0;
    private final static int NUM_DELAY_HALF_SECONDS = 10;
    String mResultString;
    
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    AsyncProgressTask(AsyncProgressInterface listener) {
        this.mListener = listener;
    }

    /**
     * Execute the task (replaces AsyncTask.execute())
     */
    public void execute() {
        // Execute pre-task on UI thread
        executeOnPreExecute();
        
        // Execute background task
        backgroundExecutor.execute(() -> {
            executeDoInBackground();
            
            // Execute post-task on UI thread
            mainHandler.post(this::executeOnPostExecute);
        });
    }

    /**
     * Cancel the task and clean up resources
     */
    public void cancel() {
        mListener = null;
        // Note: We don't shut down the executor here to avoid potential issues
        // with ongoing tasks. The executor will be garbage collected when this 
        // instance is no longer referenced.
    }

    private void executeDoInBackground() {
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
    }

    private void executeOnPreExecute() {
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

    private void executeOnPostExecute() {
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