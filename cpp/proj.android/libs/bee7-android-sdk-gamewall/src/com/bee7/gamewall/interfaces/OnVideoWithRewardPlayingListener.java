package com.bee7.gamewall.interfaces;

import com.bee7.gamewall.GameWallUnitOfferBanner;

public interface OnVideoWithRewardPlayingListener {
    /**
     * Called when gamewall wants to close but one gamewall unit is still playing video.
     *
     * @param column in what column is this offer with video.
     * @param index in column of this offer with video.
     * @param gwUnitOfferBanner the view with video view,
     */
    void onVideoWithRewardPlaying(int column, int index, GameWallUnitOfferBanner gwUnitOfferBanner);
}
