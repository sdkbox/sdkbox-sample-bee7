package com.bee7.gamewall;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;

import com.bee7.gamewall.dialogs.Bee7GameWallDialog;
import com.bee7.gamewall.enums.BannerNotificationPosition;
import com.bee7.gamewall.interfaces.Bee7BannerNotificationInterface;
import com.bee7.gamewall.interfaces.Bee7GameWallManager;
import com.bee7.gamewall.interfaces.Bee7GameWallViewsInterface;
import com.bee7.sdk.common.Reward;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.OnOfferListener;
import com.bee7.sdk.publisher.Publisher;
import com.bee7.sdk.publisher.appoffer.AppOffer;

import java.util.List;

public class GameWallDialogImpl implements Bee7BannerNotificationInterface {
    static final String TAG = GameWallDialogImpl.class.getSimpleName();

    private GameWallImpl mGameWall;
    private Bee7GameWallDialog dialog;
    private static boolean immersiveMode = false;

    public GameWallDialogImpl(Activity activity, Bee7GameWallManager manager, List<AppOffer> miniGames) {
        this(activity, manager, null, null, miniGames, true);
    }

    public GameWallDialogImpl(Activity activity, Bee7GameWallManager manager, List<AppOffer> miniGames, boolean showNotifications) {
        this(activity, manager, null, null, miniGames, showNotifications);
    }

    public GameWallDialogImpl(Activity activity, Bee7GameWallManager manager, String apiKey, String vendorId, List<AppOffer> miniGames) {
        this(activity, manager, apiKey, vendorId, miniGames, true);
    }

    public GameWallDialogImpl(Activity activity, Bee7GameWallManager manager, String apiKey, String vendorId, List<AppOffer> miniGames, boolean showNotifications) {
        // check if already initialized
        if (mGameWall == null) {
            try {
                mGameWall = new GameWallImpl(activity, manager, apiKey, vendorId, miniGames, showNotifications);
                mGameWall.checkForClaimData(activity.getIntent());
            } catch (Exception ex) {
                Logger.debug(TAG, ex, "Failed to init game wall");
            }
        }
    }

    public void resume() {
        Logger.debug(TAG, "resume()");
        if (mGameWall != null) {
            Logger.debug(TAG, "GW resumed");
            mGameWall.resume();
        }
    }

    public void pause() {
        Logger.debug(TAG, "pause()");
        // do not pause if game wall activity is being set up
        if (mGameWall != null) { //!settingUpGameWall &&
            Logger.debug(TAG, "GW paused");
            mGameWall.pause();
        }
    }

    public void destroy() {
        Logger.debug(TAG, "destroy()");
        if (mGameWall != null) {
            Logger.debug(TAG, "GW destroyed");
            mGameWall.destroy();
            mGameWall = null;
        }
    }

    public void checkForClaimData(Intent intent) {
        Logger.debug(TAG, "checkForClaimData()");
        if (mGameWall != null && intent != null) {
            mGameWall.checkForClaimData(intent);
        }
    }

    public void setAgeGate(boolean hasPassed) {
        Logger.debug(TAG, "setAgeGate()");
        if (mGameWall != null) {
            mGameWall.setAgeGate(hasPassed);
        }
    }

    public void setImmersiveMode(boolean _immersiveMode) {
        Logger.debug(TAG, "setImmersiveMode()");
        if (mGameWall != null) {
            mGameWall.setAgeGate(_immersiveMode);
        }
    }

    public void show(Activity activity) {
        Logger.debug(TAG, "show()");

        try {
            if (dialog == null) {
                if (activity != null) {
                    dialog = new Bee7GameWallDialog(activity, immersiveMode);
                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            if (mGameWall != null) {
                                mGameWall.hide();
                            }
                        }
                    });
                    dialog.setOnBackPressedListener(new Bee7GameWallDialog.OnBackPressedListener() {
                        @Override
                        public void onBackPressed() {
                            if (mGameWall != null) {
                                mGameWall.onBackPressed();
                            } else {
                                dialog.dismiss();
                            }
                        }
                    });
                    mGameWall.show(dialog);

                    Logger.debug(TAG, "GW starting dialog");
                }
            } else {
                if (mGameWall != null) {
                    mGameWall.show(dialog);

                    Logger.debug(TAG, "GW showed");
                }
            }
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to show game wall");
        }
    }

    /**
     *
     * @return true if banner notification can be shown, false otherwise
     */
    public boolean canShowReward(Reward reward, Activity activity) {
        Logger.debug(TAG, "canShowReward()");
        if (mGameWall != null) {
            return mGameWall.canShowReward(reward, activity);
        }
        return false;
    }

    public boolean showReward(Reward reward, Activity activity) {
        Logger.debug(TAG, "showReward()");
        if (mGameWall != null) {
            Logger.debug(TAG, "GW show reward on main activity");
            return mGameWall.showReward(reward, activity);
        }
        return false;
    }

    public void onGameWallButtonImpression() {
        Logger.debug(TAG, "onGameWallButtonImpression()");
        if (mGameWall != null) {
            mGameWall.onGameWallButtonImpression();
        }
    }

    public void updateView() {
        Logger.debug(TAG, "updateView()");
        if (mGameWall != null) {
            mGameWall.updateView();
        }
    }

    public void hide() {
        Logger.debug(TAG, "hide()");
        if (mGameWall != null) {
            mGameWall.hide();
        }
    }

    public void setTestVariant(String testId) {
        Logger.debug(TAG, "setTestVariant " + testId);
        if (mGameWall != null) {
            mGameWall.setTestVariant(testId);
        }
    }

    public void requestAppOffersOfType(Publisher.AppOffersType type) {
        Logger.debug(TAG, "requestAppOffersOfType " + type);
        if (mGameWall != null) {
            mGameWall.requestAppOffersOfType(type);
        }
    }

    public void updateMiniGames(List<AppOffer> miniGames) {
        Logger.debug(TAG, "updateMiniGames");
        if (mGameWall != null) {
            mGameWall.updateMiniGames(miniGames);
        }
    }

    public void setBee7GameWallViewsInterface(Bee7GameWallViewsInterface bee7GameWallViewsInterface) {
        Logger.debug(TAG, "setBee7GameWallViewsInterface");
        if (bee7GameWallViewsInterface != null) {
            mGameWall.setBee7GameWallViewsInterface(bee7GameWallViewsInterface);
        }
    }

    public void setOnOfferListener(OnOfferListener onOfferListener) {
        Logger.debug(TAG, "setOnOfferListener");
        if (mGameWall != null) {
            mGameWall.setOnOfferListener(onOfferListener);
        }
    }

    @Override
    public void toggleNotificationShowing(boolean enableNotifications) {
        Logger.debug(TAG, "toggleNotificationShowing " + enableNotifications);
        if (mGameWall != null) {
            mGameWall.toggleNotificationShowing(enableNotifications);
        }
    }

    @Override
    public void setVirtualCurrencyState(boolean lowCurrency) {
        Logger.debug(TAG, "setVirtualCurrencyState " + lowCurrency);
        if (mGameWall != null) {
            mGameWall.setVirtualCurrencyState(lowCurrency);
        }
    }

    @Override
    public boolean isBannerNotificationShown() {
        Logger.debug(TAG, "isBannerNotificationShown");
        if (mGameWall != null) {
            return mGameWall.isBannerNotificationShown();
        }
        return false;
    }

    @Override
    public Reward showBannerNotification(View anchorView, BannerNotificationPosition bannerNotificationPosition) {
        Logger.debug(TAG, "showBannerNotification");
        if (mGameWall != null) {
            return mGameWall.showBannerNotification(anchorView, bannerNotificationPosition);
        }
        return null;
    }

    @Override
    public void closeBannerNotification() {
        Logger.debug(TAG, "closeBannerNotification");
        if (mGameWall != null) {
            mGameWall.closeBannerNotification();
        }
    }
}
