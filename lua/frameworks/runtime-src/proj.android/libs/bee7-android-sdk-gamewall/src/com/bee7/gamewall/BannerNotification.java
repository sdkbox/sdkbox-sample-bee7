package com.bee7.gamewall;

import com.bee7.sdk.common.NonObfuscatable;

public class BannerNotification implements NonObfuscatable {

    public enum BannerNotificationType implements NonObfuscatable {
        REWARD("REWARD"),
        REMINDER("REMINDER"),
        LOW_CURRENCY("LOW_CURRENCY");

        private final String type;

        BannerNotificationType(String s) {
            type = s;
        }

        public String toString() {
            return this.type;
        }
    }

    public static int NUMBER_OF_REWARD_BANNER_NOTIFICATIONS_LAYOUTS = 1;
    public static int NUMBER_OF_BANNER_NOTIFICATIONS_LAYOUTS = 3;
    protected BannerNotificationType bannerNotificationType;

    public BannerNotification(BannerNotificationType bannerNotificationType) {
        this.bannerNotificationType = bannerNotificationType;
    }

    public BannerNotificationType getBannerNotificationType() {
        return bannerNotificationType;
    }

}
