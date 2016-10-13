package com.bee7.gamewall.interfaces;

public interface Bee7GameWallManagerV2 extends Bee7GameWallManager {

    /**
     * Callback from SDK when GameWall should be shown.
     */
    void onGameWallShowRequest();

    /**
     *  Callback when banner notification/s are ready for display.
     */
    void onBannerNotificationShowRequest();

    /**
     * Callback when click happened on banner notification.
     * Just for information purposes, no special handling required.
     */
    void onBannerNotificationClick();

    /**
     * Callback when showing/hiding banner notification
     * @param visible true if showing, false if hiding
     */
    void onBannerNotificationVisibilityChanged(boolean visible);
}
