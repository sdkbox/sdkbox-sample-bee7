package com.bee7.gamewall;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import com.bee7.gamewall.dialogs.Bee7PopupManager;
import com.bee7.gamewall.enums.BannerNotificationPosition;
import com.bee7.gamewall.interfaces.Bee7GameWallManagerV2;
import com.bee7.sdk.common.Reward;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.common.util.SharedPreferencesNotificationsHelper;
import com.bee7.sdk.publisher.DefaultPublisher;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Banner notification queue implementation.
 * There can be multiple banner notification, and they should be displayed in sequence.
 */
public class BannerNotificationQueue {
    public static final String TAG = BannerNotificationQueue.class.getName();

    private Queue<BannerNotificationNotify> queue = new LinkedList<BannerNotificationNotify>();
    private boolean isProcessing;

    private Context context;
    private BannerNotificationPosition bannerNotificationPosition;
    private Bee7GameWallManagerV2 gameWallManagerV2;
    private View anchorView;
    private Bee7PopupManager tempBee7PopupManager;
    private DefaultPublisher mPublisher;
    public boolean isGWShown = false;
    private boolean showNotifications;
    private boolean showNotificationsOnGameWall = true;
    private GameWallView gameWallView;
    private boolean moreThanOneBannersInQueue = false;

    public BannerNotificationQueue(Context context, Bee7GameWallManagerV2 manager, boolean showNotifications) {
        this.context = context;
        this.gameWallManagerV2 = manager;
        this.showNotifications = showNotifications;
    }


    public synchronized Reward addMessage(BannerNotificationNotify msg) {
        if (!isProcessing && !msg.queueOnStoppedQueue) {
            return null;
        }

        queue.offer(msg);

        msg.messageQueue = this;
        msg.context = context;
        if (queue.size() == 1 && isProcessing) {
            Logger.debug(TAG, "addMessage exec queue.size() == 1 && isProcessing");

            if (anchorView == null || anchorView.getWindowToken() == null) {
                Logger.error(TAG, "anchorView is null, requesting onBannerNotificationShowRequest");

                isProcessing = false;
                gameWallManagerV2.onBannerNotificationShowRequest();
                return null;
            }
            msg.exec(anchorView, bannerNotificationPosition, mPublisher, gameWallManagerV2, gameWallView, false);
            if (msg.getBannerNotification() instanceof BannerNotificationWithReward) {
                Reward reward = ((BannerNotificationWithReward) msg.getBannerNotification()).getReward();
                return reward;
            }
        }
        return null;
    }

    public synchronized Reward runNextMessage() {
        if (!isProcessing) {
            return null;
        }

        if (queue.size() == 0) {
            isProcessing = false;
            moreThanOneBannersInQueue = false;
            return null;
        }

        isProcessing = false;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Logger.debug(TAG, "runNextMessage gameWallManagerV2.onBannerNotificationShowRequest();");
                gameWallManagerV2.onBannerNotificationShowRequest();
            }
        }, context.getResources().getInteger(R.integer.bee7_notification_banner_anim_speed));
        return null;
    }

    public synchronized BannerNotificationQueue dropMessage() {
        queue.poll();
        return this;
    }

    public synchronized Reward startProcessing(Context context, View anchorView, BannerNotificationPosition bannerNotificationPosition,
                                                                      Bee7GameWallManagerV2 gameWallManagerV2, GameWallView gameWallView) {
        Logger.debug(TAG, "startProcessing, enabled " + showNotifications);
        if(isProcessing) {
            return null;
        }

        this.context = context;
        this.bannerNotificationPosition = bannerNotificationPosition;
        this.gameWallManagerV2 = gameWallManagerV2;
        this.anchorView = anchorView;
        this.gameWallView = gameWallView;
        this.isProcessing = true;

        BannerNotificationNotify msg = queue.peek();

        if (msg == null) {
            return null;
        }

        if (queue.size() > 1) {
            moreThanOneBannersInQueue = true;
        }

        msg.context = context;
        Logger.debug(TAG, "startProcessing exec");
        msg.exec(anchorView, bannerNotificationPosition, mPublisher, gameWallManagerV2, gameWallView, moreThanOneBannersInQueue);
        if (msg.getBannerNotification() instanceof BannerNotificationWithReward) {
            Reward reward = ((BannerNotificationWithReward) msg.getBannerNotification()).getReward();

            mPublisher.removePendingReward(((BannerNotificationWithReward) msg.getBannerNotification()).getPendingId());

            return reward;
        }
        return null;
    }

    public synchronized boolean isEmpty() {
        if (queue != null && !queue.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * Remove non rewarding banner notification, dismiss shown banner and stop execution
     */
    public void stop() {
        if (tempBee7PopupManager != null && tempBee7PopupManager.isShowing()) {
            tempBee7PopupManager.dismiss(false);

            boolean dismissedPrematurely = tempBee7PopupManager.dismissedPrematurely();
            if (!dismissedPrematurely) {
                dropMessage();
                clearNonRewardBannersFromQueue();
            } else {
                triggerDelayedBannerRequest();
            }
        }

        isProcessing = false;
        moreThanOneBannersInQueue = queue.size() > 1;
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public void setBannerNotificationConfig(DefaultPublisher mPublisher) {
        this.mPublisher = mPublisher;
    }

    public void addToSessionBannerNotificationCount() {
        SharedPreferencesNotificationsHelper helper = new SharedPreferencesNotificationsHelper(
                context, mPublisher);
        helper.addToSessionBannerNotificationCount();
    }

    public boolean isBannerNotificationAllowedToShow(BannerNotification.BannerNotificationType bannerNotificationType) {
        SharedPreferencesNotificationsHelper helper = new SharedPreferencesNotificationsHelper(
                context, mPublisher);
        //If we have a reward banner and gw is shown, we ignore setting for enabled notifications
        if (bannerNotificationType != BannerNotification.BannerNotificationType.REWARD) {

            if (isGWShown) {
                return false;
            }

            if (queue.size() >=mPublisher.getBannerNotificationConfig().getBannersPerSession()) {
                return false;
            }

            if (!helper.isBannerNotificationAllowedToShow(bannerNotificationType.toString()) ) {
                return false;
            }

        }

        return true;
    }

    public void reportPopup(Bee7PopupManager bee7PopupManager) {
        tempBee7PopupManager = bee7PopupManager;

        SharedPreferencesNotificationsHelper helper = new SharedPreferencesNotificationsHelper(
                context, mPublisher);
        helper.saveUsedMessageConfig(bee7PopupManager.getBannerNotificationType().toString());
    }

    public boolean isBannerNotificationShown() {
        return tempBee7PopupManager != null && tempBee7PopupManager.isShowing();
    }

    public boolean areAnyBannerNotificationsPending() {
        return queue != null && queue.size() > 0;
    }

    public void setGWVisibility(boolean isGWShown) {
        this.isGWShown = isGWShown;
        this.showNotificationsOnGameWall = isGWShown;
    }

    public void setVirtualCurrencyState(boolean lowCurrency) {
        Logger.debug(TAG, "setVirtualCurrencyState " + lowCurrency);
        try {
            if (mPublisher != null && mPublisher.getBannerNotificationConfig() != null && mPublisher.getBannerNotificationConfig().isEnabled()) {

                SharedPreferencesNotificationsHelper helper = new SharedPreferencesNotificationsHelper(
                        context, mPublisher);
                helper.setVirtualCurrencyState(lowCurrency);

                if (lowCurrency && helper.shouldShowLowCurrencyBanner(
                        com.bee7.gamewall.Utils.getNumberOfItemsInGwUnitListHolder(context),
                        com.bee7.gamewall.Utils.isPortrate(context))) {
                    addLowCurrencyNotificationBannerToQueue();
                }
            }
        } catch (Exception e) {
            Logger.error(TAG, e, "reportLowVirtualCurrency {0}", e.getMessage());
        }
    }

    public void addLowCurrencyNotificationBannerToQueue() {
        Logger.debug(TAG, "addReminderNotificationBannerToQueue");
        if (mPublisher != null && mPublisher.getBannerNotificationConfig() != null) {
            BannerNotificationNotify bannerNotificationNotify = new BannerNotificationNotify(
                    context,
                    new BannerNotification(
                            BannerNotification.BannerNotificationType.LOW_CURRENCY)
            );
            bannerNotificationNotify.queueOnStoppedQueue = true;
            addNotificationBannerToQueue(bannerNotificationNotify);
        }
    }

    private void addNotificationBannerToQueue(BannerNotificationNotify bannerNotificationNotify) {
        if (isBannerNotificationAllowedToShow(bannerNotificationNotify.getBannerNotification().bannerNotificationType)) {
            Logger.debug(TAG, "addNotificationBannerToQueue");
            addMessage(bannerNotificationNotify);

            int delay = 0;
            if (queue.size() > 0) {
                delay = 1000;
            }
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {

                    triggerBannerRequest();

                }
            }, delay);

        }
    }

    public void toggleNotificationShowing(boolean enableNotifications) {
        Logger.debug(TAG, "gameWallManagerV2.enableNotificationShowing() " + enableNotifications);
        this.showNotifications = enableNotifications;

        if (showNotifications) {
            if (areAnyBannerNotificationsPending() && !isProcessing()) {
                Logger.debug(TAG, "gameWallManagerV2.onBannerNotificationShowRequest();");
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        gameWallManagerV2.onBannerNotificationShowRequest();
                    }
                });
            }
        } else {
            closeBannerNotification();
        }
    }

    public void toggleNotificationShowingOnGameWall(boolean enableNotifications) {
        Logger.debug(TAG, "gameWallManagerV2.toggleNotificationShowingOnGameWall() " + enableNotifications);
        this.showNotificationsOnGameWall = enableNotifications;

        if (showNotificationsOnGameWall) {
            if (areAnyBannerNotificationsPending() && !isProcessing()) {
                Logger.debug(TAG, "gameWallManagerV2.onBannerNotificationShowRequest();");
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        gameWallManagerV2.onBannerNotificationShowRequest();
                    }
                });
            }
        } else {
            closeBannerNotification();
        }
    }


    public void closeBannerNotification() {
        Logger.debug(TAG, "closeBannerNotification isProcessing: " + isProcessing());
        if (isProcessing()) {
            stop();
        }
    }

    public void addReminderNotificationBannerToQueue() {
        Logger.debug(TAG, "addReminderNotificationBannerToQueue");
        if (mPublisher != null && mPublisher.getBannerNotificationConfig() != null) {
            BannerNotificationNotify bannerNotificationNotify = new BannerNotificationNotify(
                    context,
                    new BannerNotification(
                            BannerNotification.BannerNotificationType.REMINDER)
            );
            bannerNotificationNotify.queueOnStoppedQueue = true;

            addNotificationBannerToQueue(bannerNotificationNotify);
        }
    }

    public void addRewardNotificationBannerToQueue(String amountStr, Bitmap appIcon, Drawable bitmapPublisherIcon, Reward reward) {
        Logger.debug(TAG, "addRewardNotificationBannerToQueue");
        if (mPublisher != null && mPublisher.getBannerNotificationConfig() != null) {
            BannerNotificationNotify bannerNotificationNotify = new BannerNotificationNotify(
                    context,
                    new BannerNotificationWithReward(
                            amountStr,
                            appIcon,
                            context.getResources().getDrawable(R.drawable.bee7_icon_reward),
                            bitmapPublisherIcon,
                            reward)

            );
            bannerNotificationNotify.queueOnStoppedQueue = true;

            clearNonRewardBannersFromQueue();

            addNotificationBannerToQueue(bannerNotificationNotify);
        }
    }

    /**
     * Recursively clear head of queue
     */
    public void clearNonRewardBannersFromQueue(){
        if (queue != null && queue.size() > 0) {
            BannerNotificationNotify bannerNotificationNotify1 = queue.peek();

            //Check if banner notification type is reward
            if (bannerNotificationNotify1.getBannerNotification().getBannerNotificationType() != BannerNotification.BannerNotificationType.REWARD) {
                queue.poll();

                clearNonRewardBannersFromQueue();
            }
        }
    }

    public void triggerDelayedBannerRequest() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                triggerBannerRequest();
            }
        }, 5000);
    }

    public void triggerBannerRequest() {
        if (isGWShown) {
            if (!isProcessing() && showNotificationsOnGameWall) {
                Logger.debug(TAG, "gameWallManagerV2.onBannerNotificationShowRequest();");
                gameWallManagerV2.onBannerNotificationShowRequest();
            }
        } else {
            if (!isProcessing() && showNotifications) {
                Logger.debug(TAG, "gameWallManagerV2.onBannerNotificationShowRequest();");
                gameWallManagerV2.onBannerNotificationShowRequest();
            }
        }
    }

    public void resumeNotificationShowingOnGameWall() {
        Logger.debug(TAG, "resumeNotificationShowingOnGameWall ");

        if (showNotificationsOnGameWall) {
            if (areAnyBannerNotificationsPending() && !isProcessing()) {
                Logger.debug(TAG, "gameWallManagerV2.onBannerNotificationShowRequest();");
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        gameWallManagerV2.onBannerNotificationShowRequest();
                    }
                });
            }
        }
    }
}
