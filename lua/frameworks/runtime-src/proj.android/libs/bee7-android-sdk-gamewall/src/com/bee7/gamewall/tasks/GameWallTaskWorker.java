package com.bee7.gamewall.tasks;

import android.os.*;
import android.view.View;

import com.bee7.gamewall.GameWallUnitOffer;
import com.bee7.sdk.common.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Bee7 on 15/06/15.
 */
public class GameWallTaskWorker {

    public static final String TAG = GameWallTaskWorker.class.getName();
    private static GameWallTaskWorker singletonInstance = null;

    public static GameWallTaskWorker getInstance() {
        if (singletonInstance == null) {
            singletonInstance = new GameWallTaskWorker();
        }
        return singletonInstance;
    }

    private ExecutorService executor;
    private List<GenerateGameWallUnitListHolderAsyncTask> gameWallUnitListTasks;
    private List<GenerateGameWallUnitAsyncTask> gameWallUnitTasks;

    public GameWallTaskWorker() {
        executor = Executors.newSingleThreadExecutor();
        gameWallUnitListTasks = new ArrayList<GenerateGameWallUnitListHolderAsyncTask>();
        gameWallUnitTasks = new ArrayList<GenerateGameWallUnitAsyncTask>();
    }

    public void stop() {
        executor.shutdownNow();
    }

    // post generate unit
    public void postGenerateUnit(final GenerateGameWallUnitAsyncTask task) {
        if (task == null) {
            return;
        }

        gameWallUnitTasks.add(task);
        final Handler main = new Handler();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final GameWallUnitOffer offer = task.doInBackground();

                    // return in main thread
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            task.onPostExecute(offer);
                        }
                    });
                } catch (Exception e) {
                    Logger.debug("GameWallTaskWorker", e, "Failed to generate unit");
                }
            }
        });
    }

    // post generate unit list
    public void postGenerateUnitList(final GenerateGameWallUnitListHolderAsyncTask task) {
        if (task == null) {
            Logger.debug(TAG, "postGenerateUnitList task == null");
            return;
        }

        gameWallUnitListTasks.add(task);
        final Handler main = new Handler();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final View listView = task.doInBackground();

                    // return in main thread
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            Logger.debug(TAG, " task.onPostExecute(listView)");
                            task.onPostExecute(listView);
                        }
                    });
                } catch (Exception e) {
                    Logger.debug("GameWallTaskWorker", e, "Failed to generate unit list");
                }
            }
        });
    }

    public void removeAllCallbacks() {
        for (GenerateGameWallUnitListHolderAsyncTask task : gameWallUnitListTasks) {
            task.removeCallback();
        }
        for (GenerateGameWallUnitAsyncTask task : gameWallUnitTasks) {
            task.removeCallback();
        }

        executor.shutdownNow();
        executor = Executors.newSingleThreadExecutor();

        gameWallUnitListTasks.clear();
        gameWallUnitTasks.clear();
    }
}
