package com.bee7.gamewall.interfaces;


import com.bee7.sdk.common.Reward;

public interface Bee7GameWallManager {

    /**
     * Callback from the SDK when a reward was claimed.
     */
    void onGiveReward(Reward reward);

    /**
     * Called when availability of game wall changes (due to configuration change).
     */
    void onAvailableChange(boolean available);

    /**
     * Callback from the game-wall, when the view is displayed or hidden
     */
    void onVisibleChange(boolean visible, boolean isGameWall);

    /**
     * Callback from the game-wall, when the view is about to close (close button pressed, ...)
     * @return Manager should return true if game-wall can be closed immediately , or false if
     * manager will close
     */
    boolean onGameWallWillClose();

    /**
     * Callback from the game-wall when reportingId is sent.
     * @param reportingId String id
     * @param reportingIdTs long time stamp
     */
    void onReportingId(String reportingId, long reportingIdTs);
}
