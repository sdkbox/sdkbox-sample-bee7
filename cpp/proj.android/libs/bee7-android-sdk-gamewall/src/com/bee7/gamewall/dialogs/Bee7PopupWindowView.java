package com.bee7.gamewall.dialogs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bee7.gamewall.BannerNotification;
import com.bee7.gamewall.BannerNotificationWithReward;
import com.bee7.gamewall.GameWallImpl;
import com.bee7.gamewall.R;
import com.bee7.gamewall.assets.AssetsManager;
import com.bee7.gamewall.assets.AssetsManagerSetBitmapTask;
import com.bee7.gamewall.assets.UnscaledBitmapLoader;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.common.util.SharedPreferencesNotificationsHelper;
import com.bee7.sdk.common.util.SharedPreferencesRewardsHelper;
import com.bee7.sdk.common.util.Utils;
import com.bee7.sdk.publisher.DefaultPublisher;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

import java.util.List;

public class Bee7PopupWindowView {

    private final static String TAG = Bee7PopupWindowView.class.getName();
    private final static int CLICK_ANIM_INTERVAL = 120;
    private final static int CLICK_ANIM_START = 2000;
    private final static int CLICK_ANIM_CLICKS = 4;

    private Bee7PopupManager.GamewallBannerInterface gamewallBannerInterface;

    private Handler mHandler;
    private Runnable mStatusChecker;
    private boolean animFirst = true;
    private int animClicks= 0;
    private boolean animFirstRun = false;

    private int layoutType;
    private View contentView;

    //views
    LinearLayout bannerLayout;
    ImageView closeIcon;

    public Bee7PopupWindowView(Context context,
                               BannerNotification bannerNotification,
                               DefaultPublisher mPublisher, Bee7PopupManager.GamewallBannerInterface gamewallBannerInterface) {

        this.gamewallBannerInterface = gamewallBannerInterface;
        SharedPreferencesNotificationsHelper sharedPreferencesNotificationsHelper = new SharedPreferencesNotificationsHelper(context, mPublisher);

        layoutType = 0;
        int layout = R.layout.gamewall_banner_notification_0;

        int numberOfOffersInBannerNotification = 0;
        boolean videoBannerNotification = false;
        if (bannerNotification.getBannerNotificationType() == BannerNotification.BannerNotificationType.REWARD) {
            //get layout for reward notification
            switch (sharedPreferencesNotificationsHelper.getNextRewardNotificationLayout(BannerNotification.NUMBER_OF_REWARD_BANNER_NOTIFICATIONS_LAYOUTS)) {
                case 0:
                case 1:
                case 2:
                case 3:
                default:
                    layoutType = 3;
                    layout = R.layout.gamewall_banner_notification_reward_0;
                    numberOfOffersInBannerNotification = 1;
            }

        } else if (bannerNotification.getBannerNotificationType() == BannerNotification.BannerNotificationType.LOW_CURRENCY ||
                bannerNotification.getBannerNotificationType() == BannerNotification.BannerNotificationType.REMINDER) {
            //get layout for other notification
            switch (sharedPreferencesNotificationsHelper.getNextNotificationLayout(BannerNotification.NUMBER_OF_BANNER_NOTIFICATIONS_LAYOUTS)) {
                case 0:
                    layoutType = 0;
                    layout = R.layout.gamewall_banner_notification_0;
                    numberOfOffersInBannerNotification = 1;
                    break;
                case 1:
                    layoutType = 1;
                    layout = R.layout.gamewall_banner_notification_1;
                    numberOfOffersInBannerNotification = 1;
                    //if we can play video we use case 1: layout, if not, we skip to case 2:
                    if (Utils.isHardwareVideoCapable()) {
                        videoBannerNotification = true;
                        break;
                    }
                case 2:
                    layoutType = 2;
                    layout = R.layout.gamewall_banner_notification_2;
                    numberOfOffersInBannerNotification = 3;
                    break;
                default:
                    layoutType = 0;
                    layout = R.layout.gamewall_banner_notification_0;
                    numberOfOffersInBannerNotification = 1;
            }

        }

        //we get all offers that we can install
        List<AppOffer> offers = mPublisher.getNextOffersForBannerNotification(
                numberOfOffersInBannerNotification,
                videoBannerNotification,
                com.bee7.gamewall.Utils.getNumberOfItemsInGwUnitListHolder(context),
                com.bee7.gamewall.Utils.isPortrate(context));
        Logger.debug(TAG, "numberOfShownOffers offers " + offers.size());

        //remove video offers that allready have video reward
        if (videoBannerNotification) {
            for (int i = 0; i < offers.size(); i++ ) {

                AppOffer appOffer = offers.get(i);
                int maxDailyRewardFreq = mPublisher.getAppOffersModel().getVideoPrequalGlobalConfig().getMaxDailyRewardFreq();
                AppOffersModel.VideoPrequalType videoPrequaificationlType = mPublisher.getAppOffersModel().getVideoPrequaificationlType();
                SharedPreferencesRewardsHelper rewardsHelper = new SharedPreferencesRewardsHelper(context, maxDailyRewardFreq);
                boolean rewardAlreadyGiven = rewardsHelper.hasBeenRewardAlreadyGiven(appOffer.getId(), appOffer.getCampaignId());

                if (appOffer.getVideoReward() > 0 && !rewardAlreadyGiven && //remove video offers that user wont get any reward
                        (videoPrequaificationlType == AppOffersModel.VideoPrequalType.NO_VIDEO ||
                                videoPrequaificationlType == AppOffersModel.VideoPrequalType.FULLSCREEN_NO_REWARD ||
                                videoPrequaificationlType == AppOffersModel.VideoPrequalType.INLINE_NO_REWARD )) {
                    offers.remove(i);
                }
            }
        }

        if (offers.isEmpty() && videoBannerNotification) { //todo why check if its empty ? its too late now
            videoBannerNotification = false;
            layoutType = 0;
            layout = R.layout.gamewall_banner_notification_0;
            numberOfOffersInBannerNotification = 1;
            offers = mPublisher.getNextOffersForBannerNotification(numberOfOffersInBannerNotification, videoBannerNotification,
                    com.bee7.gamewall.Utils.getNumberOfItemsInGwUnitListHolder(context),
                    com.bee7.gamewall.Utils.isPortrate(context)); //Try with non video offers
        }

        if (layoutType == 2 && offers.size() < 3) {
            videoBannerNotification = false;
            layoutType = 0;
            layout = R.layout.gamewall_banner_notification_0;
            numberOfOffersInBannerNotification = 1;
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        this.contentView = inflater.inflate(layout, null);


        sharedPreferencesNotificationsHelper.saveUsedOffers(offers);

        if (offers.isEmpty()) { //if we still don't get offers //TODO should not happen
            Logger.debug(TAG, "Offers list is empty");
            numberOfOffersInBannerNotification = 0; //show banner with no offers
        }

        //--- find views
        bannerLayout = (LinearLayout) contentView.findViewById(R.id.banner_notification_layout);
        closeIcon = (ImageView) contentView.findViewById(R.id.banner_notification_close);

        setClickListeners();

        makeshiftGif(contentView.findViewById(R.id.bee7_banner_notification_button),
                context.getResources().getDrawable(R.drawable.bee7_banner_btn),
                context.getResources().getDrawable(R.drawable.bee7_banner_btn_pressed));

        if (bannerNotification.getBannerNotificationType() == BannerNotification.BannerNotificationType.REWARD) {
            if (Utils.isDevBackendEnabled(context)) {
                TextView debugText = new TextView(context);
                debugText.setText("REWARD");
                debugText.setBackgroundColor(Color.parseColor("#96FFFFFF"));
                RelativeLayout parent = (RelativeLayout)contentView.findViewById(R.id.banner_notification_parent_layout);
                parent.addView(debugText);
            }

            ImageView offerIcon = (ImageView)contentView.findViewById(R.id.banner_notification_offer_icon);
            TextView textReward = (TextView)contentView.findViewById(R.id.banner_notification_reward);

            BannerNotificationWithReward bannerNotificationWithReward = (BannerNotificationWithReward) bannerNotification;
            if (offerIcon != null && bannerNotificationWithReward != null) {
                if (bannerNotificationWithReward.getAppIcon() != null) {
                    offerIcon.setImageBitmap(bannerNotificationWithReward.getAppIcon());
                } else {
                    offerIcon.setImageDrawable(offerIcon.getResources().getDrawable(R.drawable.default_game_icon));
                }

                //Checking if reward is hidden and hiding offer icon
                if (bannerNotificationWithReward.getReward() != null
                        &&
                        bannerNotificationWithReward.getReward().isHidden()) {

                    offerIcon.setVisibility(View.INVISIBLE);
                    //we set width for padding
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) offerIcon.getLayoutParams();
                    params.width = getView().getContext().getResources().getDimensionPixelSize(R.dimen.bee7_banner_notification_icon_margin);
                    params.setMargins(0,0,0,0);
                    offerIcon.setLayoutParams(params);
                }
            }

            if (textReward != null) {
                textReward.setText(bannerNotificationWithReward.getRewardText());
            }
        } else {
            if (Utils.isDevBackendEnabled(context)) {
                TextView debugText = new TextView(context);
                if (bannerNotification.getBannerNotificationType() == BannerNotification.BannerNotificationType.REMINDER) {
                    debugText.setText("REMINDER");
                } else if (bannerNotification.getBannerNotificationType() == BannerNotification.BannerNotificationType.LOW_CURRENCY) {
                    debugText.setText("LOW_CURRENCY");
                }
                debugText.setBackgroundColor(Color.parseColor("#96FFFFFF"));
                RelativeLayout parent = (RelativeLayout)contentView.findViewById(R.id.banner_notification_parent_layout);
                parent.addView(debugText);
            }

            ImageView offerIcon = (ImageView)contentView.findViewById(R.id.banner_notification_offer_icon);
            ImageView offerIcon1 = (ImageView)contentView.findViewById(R.id.banner_notification_offer_icon_2);
            ImageView offerIcon2 = (ImageView)contentView.findViewById(R.id.banner_notification_offer_icon_3);

            TextView banner_notification_reward = (TextView)contentView.findViewById(R.id.banner_notification_reward);

            if (videoBannerNotification && banner_notification_reward != null) {
                banner_notification_reward.setText(contentView.getContext().getString(R.string.bee7_banner_notification_earn)
                        + " " + (int)(offers.get(0).getVideoReward() * mPublisher.getExchangeRate()));
            }

            for (int i=0;i< numberOfOffersInBannerNotification;i++) {
                switch (i) {
                    case 0:
                        if (offers.size() >= 1) {
                            AppOffer appOffer = offers.get(i);
                            setIcon(offerIcon, appOffer);
                        }
                        break;
                    case 1:
                        if (offers.size() >= 2) {
                            AppOffer appOffer2 = offers.get(i);
                            setIcon(offerIcon1, appOffer2);
                        }
                        break;
                    case 2:
                        if (offers.size() >= 3) {
                            AppOffer appOffer3 = offers.get(i);
                            setIcon(offerIcon2, appOffer3);
                        }
                        break;
                }
            }
        }
    }

    /**
     *
     * @return generated view
     */
    public View getView() {
        return contentView;
    }

    /**
     * Sets offer icon
     */
    private void setIcon(ImageView offerIcon, AppOffer appOffer) {
        Logger.debug(TAG, "setting icon for " + appOffer.getLocalizedName());
        AppOffer.IconUrlSize iconUrlSize = GameWallImpl.getAppOfIconUrlSize(offerIcon.getContext().getResources());
        UnscaledBitmapLoader.ScreenDPI screenDPI = UnscaledBitmapLoader.ScreenDPI.parseDensity(offerIcon.getContext().getResources()
                .getString(R.string.bee7_gamewallSourceIconDPI));

        AssetsManagerSetBitmapTask task = new AssetsManagerSetBitmapTask(appOffer.getIconUrl(iconUrlSize), offerIcon.getContext()) {
            @Override
            public void bitmapLoadedPost(Bitmap bitmap) {
                ImageView offerImageView = (ImageView) getParams();

                if (offerImageView == null) {
                    Logger.warn(TAG, "icon == null");
                    return;
                }

                offerImageView.setImageBitmap(bitmap);
            }
        };

        task.setParams(offerIcon);
        task.setSourceImageDPI(screenDPI);

        AssetsManager.getInstance().runIconTask(task);
    }

    private void setClickListeners() {
        if (bannerLayout != null) {
            bannerLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (gamewallBannerInterface != null) {
                        gamewallBannerInterface.OnBannerClick();
                    }
                }
            });
        }
        if (closeIcon != null) {
            closeIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (gamewallBannerInterface != null) {
                        gamewallBannerInterface.OnCloseClick();
                    }
                }
            });
        }
    }

    /**
     * Creates simple click animation by switching backgrounds
     */
    private void makeshiftGif(final View view, final Drawable drawable, final Drawable drawablePressed) {
        if (view == null) {
            return;
        }
        mHandler = new Handler();

        mStatusChecker = new Runnable() {
            @Override
            public void run() {
                if (view != null) {
                    if (animFirst) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            view.setBackground(drawable);
                        } else {
                            view.setBackgroundDrawable(drawable);
                        }
                        animFirst = false;
                    } else {
                        animClicks++;
                        if (animClicks <= CLICK_ANIM_CLICKS) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                view.setBackground(drawablePressed);
                            } else {
                                view.setBackgroundDrawable(drawablePressed);
                            }
                        }
                        animFirst = true;
                    }
                    view.setPadding(
                            view.getContext().getResources().getDimensionPixelSize(R.dimen.bee7_banner_notification_button_padding_horizontal),
                            view.getContext().getResources().getDimensionPixelSize(R.dimen.bee7_banner_notification_button_padding_vertical),
                            view.getContext().getResources().getDimensionPixelSize(R.dimen.bee7_banner_notification_button_padding_horizontal),
                            view.getContext().getResources().getDimensionPixelSize(R.dimen.bee7_banner_notification_button_padding_vertical));

                    if (animFirstRun) {
                        mHandler.postDelayed(mStatusChecker, CLICK_ANIM_INTERVAL);
                    } else {
                        mHandler.postDelayed(mStatusChecker, CLICK_ANIM_START);
                        animFirstRun = true;
                    }
                }
            }
        };

        mStatusChecker.run();
    }

    public int getLayoutType() {
        return layoutType;
    }
}
