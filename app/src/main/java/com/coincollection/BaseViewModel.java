package com.coincollection;

import androidx.lifecycle.ViewModel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class BaseViewModel extends ViewModel {

    private final ExecutorService executorService;
    private final Handler mainThreadHandler;

    public BaseViewModel() {
        executorService = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    protected ExecutorService getExecutorService() {
        return executorService;
    }

    protected Handler getMainThreadHandler() {
        return mainThreadHandler;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Attempt to gracefully shut down the executor service
        // You might want to use shutdownNow() or awaitTermination() depending on requirements
        executorService.shutdown(); 
    }
}
