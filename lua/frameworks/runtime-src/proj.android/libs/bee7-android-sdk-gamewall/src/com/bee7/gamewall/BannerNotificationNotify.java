package com.bee7.gamewall;

import android.content.Context;
import android.os.Handler;
import android.view.View;

import com.bee7.gamewall.dialogs.Bee7PopupManager;
import com.bee7.gamewall.enums.BannerNotificationPosition;
import com.bee7.gamewall.interfaces.Bee7GameWallManagerV2;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.DefaultPublisher;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Banner notification wrapper, controls its visibility
 */
public class BannerNotificationNotify {

    public static final String TAG = BannerNotificationNotify.class.getName();

    public BannerNotificationQueue messageQueue;
    public Context context;
    public boolean queueOnStoppedQueue;
    public long executeTime;

    protected boolean dontRun;
    protected Lock msgLock = new ReentrantLock();

    private BannerNotification bannerNotification;
    private Bee7PopupManager bee7PopupManager;
    private long notificationBannerDisplayTime = 3;

    private long now;
    private Handler handler;

    public BannerNotificationNotify(Context context, BannerNotification bannerNotification) {
        this.context = context;
        this.bannerNotification = bannerNotification;
    }

    public synchronized boolean exec(View anchorView,
                                     BannerNotificationPosition bannerNotificationPosition,
                                     DefaultPublisher mPublisher,
                                     Bee7GameWallManagerV2 gameWallManagerV2,
                                     GameWallView gameWallView,
                                     boolean moreThanOneInQueue) {
        if (context == null) {
            return false;
        }

        dontRun = false;
        now = System.currentTimeMillis();
        executeTime = now;

        handler = new Handler();

        bee7PopupManager = new Bee7PopupManager(context, anchorView, bannerNotificationPosition, bannerNotification,
                mPublisher, gameWallManagerV2, gameWallView, this);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                msgLock.lock();
                try {
                    if (dontRun) {
                        return;
                    }

                    messageQueue.addToSessionBannerNotificationCount();
                    messageQueue.reportPopup(bee7PopupManager);
                    bee7PopupManager.show();

                    if (getBannerNotification() instanceof BannerNotificationWithReward) {
                        ((BannerNotificationWithReward) getBannerNotification()).setReward(null);
                    }
                } finally {
                    msgLock.unlock();
                }
            }
        });

        if(mPublisher != null && mPublisher.getBannerNotificationConfig() != null) {
            notificationBannerDisplayTime = mPublisher.getBannerNotificationConfig().getDisplayTime();
            if (moreThanOneInQueue) {
                notificationBannerDisplayTime = mPublisher.getBannerNotificationConfig().getDisplayTimeSecsMultiple();
            }
        }

        handler.postDelayed(runnable, notificationBannerDisplayTime);

        return true;
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            removeBubble(true, now);
        }
    };

    public synchronized void removeBubble(final boolean runNextMessage, long tm) {
        if (tm != executeTime && tm != 0) {
            return;
        }
        msgLock.lock();

        try {
            dontRun = true;
        } finally {
            msgLock.unlock();
        }

        if (bee7PopupManager == null) {
            return;
        }

        if (bee7PopupManager.isShowing()) {
            try {
                bee7PopupManager.dismiss(false);
            } catch (Exception e) {
                Logger.warn(TAG, e, "Failed to dismiss reward dialog, already removed.");
            }
        }

        // timeout, always drop message
        messageQueue.dropMessage();

        if (runNextMessage) {
            messageQueue.runNextMessage();
        }
    }

    public synchronized void cancel() {
        try {
            handler.removeCallbacks(runnable);
        } catch (Exception e) {}
    }

    public synchronized void dismiss() {
        try {
            messageQueue.dropMessage();
            messageQueue.runNextMessage();
        } catch (Exception e) {}
    }

    public BannerNotification getBannerNotification() {
        return bannerNotification;
    }
}
