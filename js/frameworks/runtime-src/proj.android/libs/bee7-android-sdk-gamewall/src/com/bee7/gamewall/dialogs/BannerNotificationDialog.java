package com.bee7.gamewall.dialogs;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bee7.gamewall.GameWallImpl;
import com.bee7.gamewall.R;
import com.bee7.gamewall.assets.AssetsManager;
import com.bee7.gamewall.assets.AssetsManagerSetBitmapTask;
import com.bee7.gamewall.assets.UnscaledBitmapLoader;
import com.bee7.gamewall.enums.BannerNotificationPosition;
import com.bee7.gamewall.BannerNotification;
import com.bee7.gamewall.BannerNotification.BannerNotificationType;
import com.bee7.gamewall.BannerNotificationWithReward;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.common.util.SharedPreferencesNotificationsHelper;
import com.bee7.sdk.common.util.SharedPreferencesRewardsHelper;
import com.bee7.sdk.common.util.Utils;
import com.bee7.sdk.publisher.DefaultPublisher;
import com.bee7.sdk.publisher.PublisherConfiguration;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

import java.util.List;

@Deprecated
public class BannerNotificationDialog extends Bee7Dialog {

    private final static String TAG = BannerNotificationDialog.class.getName();

    private BannerNotificationPosition bannerNotificationPosition;
    //private GamewallBannerNotificationDialogInterface gamewallBannerNotificationDialogInterface;
    private BannerNotification bannerNotification;
    private SharedPreferencesNotificationsHelper sharedPreferencesNotificationsHelper;
    private DefaultPublisher mPublisher;

    private int numberOfOffersInBannerNotification = 0;
    private boolean videoBannerNotification = false;

    private Handler mHandler;
    private boolean first = true;
    private Runnable mStatusChecker;

    //views
    LinearLayout bannerLayout;
    ImageView closeIcon;
    RelativeLayout emptySpace;

    public BannerNotificationDialog(Context context,
                                    BannerNotificationPosition _bannerNotificationPosition,
                                    final BannerNotification _bannerNotification,
                                    DefaultPublisher mPublisher) {
        super(context, false);

        this.bannerNotificationPosition = _bannerNotificationPosition;
        this.bannerNotification = _bannerNotification;
        //this.gamewallBannerNotificationDialogInterface = _bannerNotification.getGamewallBannerNotificationDialogInterface();
        this.sharedPreferencesNotificationsHelper = new SharedPreferencesNotificationsHelper(context, mPublisher);
        this.mPublisher = mPublisher;

        if (bannerNotification.getBannerNotificationType() == BannerNotificationType.REWARD) {
            switch (sharedPreferencesNotificationsHelper.getNextRewardNotificationLayout(BannerNotification.NUMBER_OF_REWARD_BANNER_NOTIFICATIONS_LAYOUTS)) {
                case 0:
                case 1:
                case 2:
                case 3:
                default:
                    setContentView(R.layout.gamewall_banner_notification_reward_0);
                    numberOfOffersInBannerNotification = 1;
            }

        } else if (bannerNotification.getBannerNotificationType() == BannerNotificationType.LOW_CURRENCY ||
                bannerNotification.getBannerNotificationType() == BannerNotificationType.REMINDER) {

            switch (sharedPreferencesNotificationsHelper.getNextNotificationLayout(BannerNotification.NUMBER_OF_BANNER_NOTIFICATIONS_LAYOUTS)) {
                case 0:
                    setContentView(R.layout.gamewall_banner_notification_0);
                    numberOfOffersInBannerNotification = 1;
                    break;
                case 1:
                    setContentView(R.layout.gamewall_banner_notification_1);
                    numberOfOffersInBannerNotification = 1;
                    if (Utils.isHardwareVideoCapable()) {
                        videoBannerNotification = true;
                        break;
                    }
                case 2:
                    setContentView(R.layout.gamewall_banner_notification_2);
                    numberOfOffersInBannerNotification = 3;
                    break;
                default:
                    setContentView(R.layout.gamewall_banner_notification_0);
                    numberOfOffersInBannerNotification = 1;
            }

        }

        List<AppOffer> offers = mPublisher.getNextOffersForBannerNotification(numberOfOffersInBannerNotification, videoBannerNotification, 0, false);

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

        if (offers.isEmpty() && videoBannerNotification) {
            videoBannerNotification = false;
            setContentView(R.layout.gamewall_banner_notification_0);
            numberOfOffersInBannerNotification = 1;
            offers = mPublisher.getNextOffersForBannerNotification(numberOfOffersInBannerNotification, videoBannerNotification, 0, false); //Try with non video offers
        }

        sharedPreferencesNotificationsHelper.saveUsedOffers(offers);

        if (offers.isEmpty()) { //if we still don't get offers
            numberOfOffersInBannerNotification = 0; //show banner with no offers
        }

        //--- find views
        bannerLayout = (LinearLayout)findViewById(R.id.banner_notification_layout);
        closeIcon = (ImageView) findViewById(R.id.banner_notification_close);
        emptySpace = (RelativeLayout)findViewById(R.id.banner_notification_parent_layout);

        setAnimations();
        setClickListeners();

        makeshiftGif(findViewById(R.id.bee7_banner_notification_button),
                getContext().getResources().getDrawable(R.drawable.bee7_banner_btn),
                getContext().getResources().getDrawable(R.drawable.bee7_banner_btn_pressed));

        if (bannerNotification.getBannerNotificationType() == BannerNotificationType.REWARD) {
            ImageView offerIcon = (ImageView)findViewById(R.id.banner_notification_offer_icon);
            TextView textReward = (TextView)findViewById(R.id.banner_notification_reward);

            BannerNotificationWithReward bannerNotificationWithReward = (BannerNotificationWithReward) bannerNotification;
            if (offerIcon !=null) {
                offerIcon.setImageBitmap(bannerNotificationWithReward.getAppIcon());
            }

            if (textReward != null) {
                textReward.setText(bannerNotificationWithReward.getRewardText());
            }
        } else {
            ImageView offerIcon = (ImageView)findViewById(R.id.banner_notification_offer_icon);
            ImageView offerIcon1 = (ImageView)findViewById(R.id.banner_notification_offer_icon_2);
            ImageView offerIcon2 = (ImageView)findViewById(R.id.banner_notification_offer_icon_3);

            TextView banner_notification_reward = (TextView)findViewById(R.id.banner_notification_reward);

            if (videoBannerNotification && banner_notification_reward != null) {
                banner_notification_reward.setText("Earn " + (int)(offers.get(0).getVideoReward() * mPublisher.getExchangeRate()));
            }

            for (int i=0;i<numberOfOffersInBannerNotification;i++) {
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


        setCanceledOnTouchOutside(false);
    }

    private void setIcon(ImageView offerIcon, AppOffer appOffer) {
        AppOffer.IconUrlSize iconUrlSize = GameWallImpl.getAppOfIconUrlSize(offerIcon.getContext().getResources());
        UnscaledBitmapLoader.ScreenDPI screenDPI = UnscaledBitmapLoader.ScreenDPI.parseDensity(offerIcon.getContext().getResources()
                .getString(R.string.bee7_gamewallSourceIconDPI));

        AssetsManagerSetBitmapTask task = new AssetsManagerSetBitmapTask(appOffer.getIconUrl(iconUrlSize), getContext()) {
            @Override
            public void bitmapLoadedPost(Bitmap bitmap) {
                ImageView offerImageView = (ImageView) getParams();

                if (offerImageView == null) {
                    Logger.warn("", "icon == null");
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
                    dismiss();
                    //if (gamewallBannerNotificationDialogInterface != null) {
                    //    gamewallBannerNotificationDialogInterface.OnBannerNotificationClick();
                    //}
                }
            });
        }
        if (closeIcon != null) {
            closeIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
        if (emptySpace != null) {
            emptySpace.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }

    private void setAnimations() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) bannerLayout.getLayoutParams();
        if (bannerNotificationPosition == BannerNotificationPosition.TOP_DOWN) {
            getWindow().setWindowAnimations(R.style.Bee7BannerNotificationAnimationTopDown);
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        } else if (bannerNotificationPosition == BannerNotificationPosition.BOTTOM_UP) {
            getWindow().setWindowAnimations(R.style.Bee7BannerNotificationAnimationBottomUp);
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        }
        bannerLayout.setLayoutParams(params);
    }

    private void makeshiftGif(final View view, final Drawable drawable, final Drawable drawable1) {
        if (view == null) {
            return;
        }
        mHandler = new Handler();

        mStatusChecker = new Runnable() {
            @Override
            public void run() {
                if (view != null) {
                    if (first) {
                        view.setBackgroundDrawable(drawable1);
                        first = false;
                    } else {
                        view.setBackgroundDrawable(drawable);
                        first = true;
                    }
                    view.setPadding(
                            getContext().getResources().getDimensionPixelSize(R.dimen.bee7_banner_notification_button_padding_horizontal),
                            getContext().getResources().getDimensionPixelSize(R.dimen.bee7_banner_notification_button_padding_vertical),
                            getContext().getResources().getDimensionPixelSize(R.dimen.bee7_banner_notification_button_padding_horizontal),
                            getContext().getResources().getDimensionPixelSize(R.dimen.bee7_banner_notification_button_padding_vertical));

                    mHandler.postDelayed(mStatusChecker, 450);
                }
            }
        };

        mStatusChecker.run();

        final Runnable finalMStatusChecker = mStatusChecker;
        setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mHandler.removeCallbacks(finalMStatusChecker);
            }
        });
    }

}
