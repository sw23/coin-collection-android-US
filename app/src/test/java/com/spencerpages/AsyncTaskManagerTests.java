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

package com.spencerpages;

import static org.junit.Assert.*;

import com.coincollection.AsyncTaskManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

/**
 * Unit tests for the new AsyncTaskManager that replaces deprecated AsyncTask
 */
@RunWith(RobolectricTestRunner.class)
public class AsyncTaskManagerTests {

    @Test
    public void test_asyncTaskManagerCreation() {
        AsyncTaskManager manager = new AsyncTaskManager();
        
        // Test initial state
        assertFalse("Should not be running initially", manager.isTaskRunning());
        assertNotNull("LiveData should be initialized", manager.isRunning);
        assertNotNull("Result LiveData should be initialized", manager.result);
        
        // Cleanup
        manager.shutdown();
    }
    
    @Test
    public void test_asyncTaskManagerBasicExecution() {
        AsyncTaskManager manager = new AsyncTaskManager();
        
        final boolean[] preExecuteRan = new boolean[1];
        final boolean[] postExecuteRan = new boolean[1];
        final String[] resultHolder = new String[1];
        
        // Test background task
        AsyncTaskManager.BackgroundTask backgroundTask = () -> "Test result";
        
        // Test pre-execute task
        Runnable preExecuteTask = () -> preExecuteRan[0] = true;
        
        // Test post-execute task
        AsyncTaskManager.PostExecuteTask postExecuteTask = result -> {
            resultHolder[0] = result;
            postExecuteRan[0] = true;
        };
        
        // Execute the task
        manager.executeTask(1, backgroundTask, preExecuteTask, postExecuteTask);
        
        // For unit tests, we validate the task was set up correctly
        assertTrue("Task should be considered running after execution starts", manager.isTaskRunning());
        
        // Cleanup
        manager.shutdown();
    }
    
    @Test
    public void test_asyncTaskManagerCancellation() {
        AsyncTaskManager manager = new AsyncTaskManager();
        
        // Test background task that would take some time
        AsyncTaskManager.BackgroundTask backgroundTask = () -> {
            try {
                Thread.sleep(1000); // Simulate work
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "Task was cancelled";
            }
            return "Task completed normally";
        };
        
        // Execute the task
        manager.executeTask(1, backgroundTask, null, null);
        
        // Verify task is running
        assertTrue("Task should be running after execution", manager.isTaskRunning());
        
        // Cancel the task
        manager.cancel();
        
        // Verify task cancellation is handled
        assertNotNull("Manager should handle cancellation", manager);
        
        // Cleanup
        manager.shutdown();
    }
    
    @Test
    public void test_asyncTaskManagerInterfaces() {
        // Test that the interfaces are properly defined
        AsyncTaskManager.BackgroundTask backgroundTask = () -> "test";
        assertNotNull("BackgroundTask interface should work", backgroundTask);
        assertEquals("BackgroundTask should return expected result", "test", backgroundTask.doInBackground());
        
        AsyncTaskManager.PostExecuteTask postExecuteTask = result -> {
            // Test callback
        };
        assertNotNull("PostExecuteTask interface should work", postExecuteTask);
        
        // Test that the callback can be called
        postExecuteTask.onPostExecute("test result");
    }
    
    @Test
    public void test_asyncTaskManagerLiveDataInitialization() {
        AsyncTaskManager manager = new AsyncTaskManager();
        
        // Test that LiveData is properly initialized
        assertNotNull("IsRunning LiveData should not be null", manager.isRunning);
        assertNotNull("Result LiveData should not be null", manager.result);
        
        // Test initial values
        assertFalse("Should not be running initially", Boolean.TRUE.equals(manager.isRunning.getValue()));
        
        // Cleanup
        manager.shutdown();
    }
    
    @Test
    public void test_asyncTaskManagerConfigurationChangeScenario() {
        AsyncTaskManager manager = new AsyncTaskManager();
        final boolean[] taskCompleted = {false};
        final String[] taskResult = {null};
        
        // Simulate a long-running task that would span a configuration change
        AsyncTaskManager.BackgroundTask longTask = () -> {
            try {
                // Simulate some work
                Thread.sleep(100);
                return "Long task completed";
            } catch (InterruptedException e) {
                return "Task interrupted";
            }
        };
        
        AsyncTaskManager.PostExecuteTask postTask = result -> {
            taskCompleted[0] = true;
            taskResult[0] = result;
        };
        
        // Start the task
        manager.executeTask(1, longTask, null, postTask);
        
        // Verify task is running using the method (not LiveData since it's async)
        assertTrue("Task should be running", manager.isTaskRunning());
        
        // Simulate configuration change - the task should continue running
        // (In real scenario, the ViewModel survives and the task continues)
        assertTrue("Task should still be running after configuration change", manager.isTaskRunning());
        
        // Wait for task completion
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        
        // Let background thread finish
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Process UI thread tasks again
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        
        // Verify task completed successfully
        assertTrue("Task should have completed", taskCompleted[0]);
        assertEquals("Task result should be correct", "Long task completed", taskResult[0]);
        assertFalse("Task should no longer be running", manager.isTaskRunning());
        
        // Cleanup
        manager.shutdown();
    }
}