package com.bee7.gamewall.interfaces;

import com.bee7.sdk.publisher.appoffer.AppOffer;

public interface OnVideoRewardGeneratedListener {
    /**
     * Called when reward should be generated for an offer video.
     *
     * @param appOffer to be used for checking and generating the reward.
     */
    void onVideoRewardGenerated(AppOffer appOffer);
}
