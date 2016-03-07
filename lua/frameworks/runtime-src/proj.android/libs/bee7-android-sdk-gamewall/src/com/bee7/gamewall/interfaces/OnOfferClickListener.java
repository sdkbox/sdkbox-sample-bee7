package com.bee7.gamewall.interfaces;

import com.bee7.sdk.publisher.Publisher;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResult;

public interface OnOfferClickListener {
    /**
     * Called when user performs a click on offer in game wall.
     *
     * @param appOffer For what offer click happened.
     * @param appOfferWithResult For what offer click happened and offer GW details.
     * @param afterVideo If click happened after video was started.
     * @param origin of the click
     */
    void onOfferClick(AppOffer appOffer, AppOfferWithResult appOfferWithResult, boolean afterVideo, Publisher.AppOfferStartOrigin origin);
}
