package com.bee7.gamewall.interfaces;

import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResult;

public interface OnVideoClickListener {
    /**
     * Called when user performs a click on video button of offer in game wall.
     *
     * @param appOffer For what offer click happened.
     * @param appOfferWithResult Offer's data
     */
    void onVideoClick(AppOffer appOffer, AppOfferWithResult appOfferWithResult);
}
