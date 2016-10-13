package com.bee7.gamewall.interfaces;

import android.view.View;

import com.bee7.gamewall.enums.BannerNotificationPosition;
import com.bee7.sdk.common.Reward;

public interface Bee7BannerNotificationInterface {

    /**
     * @param anchorView Display the content view in a popup window anchored to the top or bottom of the anchor view.
     * @param bannerNotificationPosition Position from where banner notification should be shown.
     */
    Reward showBannerNotification(View anchorView, BannerNotificationPosition bannerNotificationPosition);

    /**
     *
     * @return true if banner notification is shown, false otherwise.
     */
    boolean isBannerNotificationShown();

    /**
     * It closes any shown banner notification and stops execution of pending ones.
     */
    void closeBannerNotification();

    /**
     * Report that user has low currency.
     * @param lowCurrency true user is in low currency state, false otherwise.
     */
    void setVirtualCurrencyState(boolean lowCurrency);

    /**
     * Enables or disables showing of banner notification.
     * Setting to false will close any shown banner notification and stops execution of pending ones.
     * @param notificationShowing true to enable banner notifications, false to disable it.
     */
    void toggleNotificationShowing(boolean notificationShowing);
}
