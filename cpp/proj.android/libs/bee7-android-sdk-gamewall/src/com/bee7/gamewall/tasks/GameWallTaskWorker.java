package com.bee7.gamewall.tasks;

import android.os.*;
import android.view.View;

import com.bee7.gamewall.GameWallUnitOffer;
import com.bee7.sdk.common.util.Logger;

/**
 * Created by Bee7 on 15/06/15.
 */
public class GameWallTaskWorker {
    protected Handler executor;

    public GameWallTaskWorker(String threadName) {
        HandlerThread handlerThread = new HandlerThread(threadName, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();

        Looper looper = handlerThread.getLooper(); // Blocks until handlerThread is fully init

        executor = new Handler(looper);
    }

    public void stop() {
        executor.getLooper().quit();
    }

    // post generate unit
    public void postGenerateUnit(final GenerateGameWallUnitAsyncTask task) {
        if (task == null) {
            return;
        }

        final Handler main = new Handler();

        executor.post(new Runnable() {
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
            return;
        }

        final Handler main = new Handler();

        executor.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final View listView = task.doInBackground();

                    // return in main thread
                    main.post(new Runnable() {
                        @Override
                        public void run() {
                            task.onPostExecute(listView);
                        }
                    });
                } catch (Exception e) {
                    Logger.debug("GameWallTaskWorker", e, "Failed to generate unit list");
                }
            }
        });
    }
}
