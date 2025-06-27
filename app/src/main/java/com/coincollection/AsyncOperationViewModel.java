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

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel for managing async operations in a lifecycle-aware manner
 * Replaces the deprecated AsyncTask system with modern architecture components
 */
public class AsyncOperationViewModel extends ViewModel {
    
    private final AsyncTaskManager asyncTaskManager;
    
    // Task state LiveData
    private final MutableLiveData<Boolean> isTaskRunning = new MutableLiveData<>(false);
    private final MutableLiveData<String> taskResult = new MutableLiveData<>();
    private final MutableLiveData<Integer> currentTaskId = new MutableLiveData<>();
    
    // Task IDs - maintaining compatibility with existing code
    public static final int TASK_OPEN_DATABASE = 0;
    public static final int TASK_IMPORT_COLLECTIONS = 1;
    public static final int TASK_CREATE_UPDATE_COLLECTION = 2;
    public static final int TASK_EXPORT_COLLECTIONS = 3;
    
    public AsyncOperationViewModel() {
        asyncTaskManager = new AsyncTaskManager();
        
        // Forward async manager state to our LiveData
        asyncTaskManager.isRunning.observeForever(isTaskRunning::setValue);
        asyncTaskManager.result.observeForever(taskResult::setValue);
    }
    
    /**
     * Execute an async operation
     * 
     * @param taskId the type of task to execute
     * @param backgroundWork the work to do in background
     * @param preExecuteWork work to do before background task (on UI thread)
     * @param postExecuteWork work to do after background task (on UI thread)
     */
    public void executeAsyncOperation(int taskId,
                                    AsyncTaskManager.BackgroundTask backgroundWork,
                                    Runnable preExecuteWork,
                                    AsyncTaskManager.PostExecuteTask postExecuteWork) {
        currentTaskId.setValue(taskId);
        asyncTaskManager.executeTask(taskId, backgroundWork, preExecuteWork, postExecuteWork);
    }
    
    /**
     * Set synchronous mode for unit testing
     */
    public void setSynchronousMode(boolean syncMode) {
        asyncTaskManager.setSynchronousMode(syncMode);
    }
    
    /**
     * Cancel the current operation
     */
    public void cancelCurrentOperation() {
        asyncTaskManager.cancel();
        currentTaskId.setValue(null);
    }
    
    // Getters for LiveData
    public LiveData<Boolean> getIsTaskRunning() {
        return isTaskRunning;
    }
    
    public LiveData<String> getTaskResult() {
        return taskResult;
    }
    
    public LiveData<Integer> getCurrentTaskId() {
        return currentTaskId;
    }
    
    public boolean isCurrentlyRunning() {
        return asyncTaskManager.isTaskRunning();
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        asyncTaskManager.shutdown();
    }
}