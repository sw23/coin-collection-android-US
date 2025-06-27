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

import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Modern replacement for deprecated AsyncTask using ExecutorService and Handler
 * Provides lifecycle-aware async task execution with progress updates
 */
public class AsyncTaskManager {
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private Future<?> currentTask;
    
    // LiveData for progress tracking
    public final MutableLiveData<Boolean> isRunning = new MutableLiveData<>(false);
    public final MutableLiveData<String> result = new MutableLiveData<>();
    
    public AsyncTaskManager() {
        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Execute an async task with lifecycle-aware callbacks
     * 
     * @param taskId identifier for the task type
     * @param backgroundTask the task to execute on background thread
     * @param preExecuteTask task to run on UI thread before background task (optional)
     * @param postExecuteTask task to run on UI thread after background task (optional)
     */
    public void executeTask(int taskId, 
                           BackgroundTask backgroundTask,
                           Runnable preExecuteTask,
                           PostExecuteTask postExecuteTask) {
        
        // Cancel any running task
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
        
        // Run pre-execute on UI thread
        if (preExecuteTask != null) {
            mainHandler.post(preExecuteTask);
        }
        
        // Set running state
        isRunning.postValue(true);
        
        // Execute background task
        currentTask = executorService.submit(() -> {
            String taskResult = "";
            try {
                if (backgroundTask != null) {
                    taskResult = backgroundTask.doInBackground();
                }
            } catch (Exception e) {
                taskResult = "Error: " + e.getMessage();
            }
            
            // Post result to LiveData
            result.postValue(taskResult);
            
            // Run post-execute on UI thread
            final String finalResult = taskResult;
            mainHandler.post(() -> {
                isRunning.setValue(false);
                if (postExecuteTask != null) {
                    postExecuteTask.onPostExecute(finalResult);
                }
            });
        });
    }
    
    /**
     * Cancel the current running task
     */
    public void cancel() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
            isRunning.postValue(false);
        }
    }
    
    /**
     * Check if a task is currently running
     */
    public boolean isTaskRunning() {
        return currentTask != null && !currentTask.isDone();
    }
    
    /**
     * Clean up resources
     */
    public void shutdown() {
        cancel();
        executorService.shutdown();
    }
    
    /**
     * Interface for background tasks
     */
    public interface BackgroundTask {
        String doInBackground();
    }
    
    /**
     * Interface for post-execute callback
     */
    public interface PostExecuteTask {
        void onPostExecute(String result);
    }
}