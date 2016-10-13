package com.bee7.gamewall;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import com.bee7.sdk.common.NonObfuscatable;
import com.bee7.sdk.common.Reward;

public class BannerNotificationWithReward extends BannerNotification implements NonObfuscatable {

    private String text;
    private Bitmap appIcon;
    private Drawable vCIcon;
    private Drawable publisherIcon;
    private Reward reward;
    private String pendingId;

    public BannerNotificationWithReward(String text, Bitmap appIcon, Drawable vcIcon,
                                        Drawable publisherIcon, Reward reward) {
        super(BannerNotificationType.REWARD);
        this.text = text;
        this.appIcon = appIcon;
        this.vCIcon = vcIcon;
        this.publisherIcon = publisherIcon;
        this.reward = reward;
        this.pendingId = "";

        if (reward != null) {
            this.pendingId = reward.getPending();
        }
    }

    public String getRewardText() {
        return text;
    }

    public Bitmap getAppIcon() {
        return appIcon;
    }

    public Drawable getPublisherIcon() {
        return publisherIcon;
    }

    public Drawable getVCIcon() {
        return vCIcon;
    }

    public boolean isVideoReward() {
        return reward != null && reward.isVideoReward();
    }

    public Reward getReward() {
        return reward;
    }

    public void setReward(Reward reward) {
        this.reward = reward;
    }

    public String getPendingId() {
        return pendingId;
    }
}
