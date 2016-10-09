package com.bee7.gamewall;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.bee7.gamewall.enums.BannerNotificationPosition;
import com.bee7.gamewall.interfaces.Bee7BannerNotificationInterface;
import com.bee7.gamewall.interfaces.Bee7GameWallManager;
import com.bee7.gamewall.interfaces.Bee7GameWallManagerV2;
import com.bee7.gamewall.interfaces.Bee7GameWallViewsInterface;
import com.bee7.sdk.common.Reward;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.OnOfferListener;
import com.bee7.sdk.publisher.Publisher;
import com.bee7.sdk.publisher.appoffer.AppOffer;

import java.util.List;

public class GameWallActivityImpl implements Bee7BannerNotificationInterface {
    static final String TAG = GameWallActivityImpl.class.getSimpleName();

    private static GameWallActivityImpl instance;

    private Activity activity;
    private GameWallActivity gameWallActivityActivity;

    private GameWallImpl gameWallImpl;
    private Bee7GameWallManager bee7GameWallManager;
    private Bee7GameWallManager manager;

    private boolean settingUpGameWall;
    private static boolean immersiveMode = false;

    public static GameWallActivityImpl sharedInstance() {
        if (instance == null) {
            instance = new GameWallActivityImpl();
        }

        return instance;
    }

    private GameWallActivityImpl() {}

    public void init(Activity activity, Bee7GameWallManager bee7GameWallManager, String apiKey) {
        init(activity, bee7GameWallManager, apiKey, "", null, false);
    }

    public void init(Activity activity, Bee7GameWallManager bee7GameWallManager, String apiKey, String vendorId) {
        init(activity, bee7GameWallManager, apiKey, vendorId, null, false);
    }

    public void init(Activity activity, Bee7GameWallManager bee7GameWallManager, String apiKey, String vendorId, boolean showNotifications) {
        init(activity, bee7GameWallManager, apiKey, vendorId, null, showNotifications);
    }

    public void init(Activity activity, Bee7GameWallManager bee7GameWallManager, String apiKey, String vendorId, List<AppOffer> miniGames) {
        init(activity, bee7GameWallManager, apiKey, vendorId, miniGames, false);
    }

    public void init(Activity activity, Bee7GameWallManager _bee7GameWallManager, String apiKey, String vendorId, List<AppOffer> miniGames, boolean showNotifications) {
        // check if already initialized
        if (gameWallImpl == null) {
            try {
                this.bee7GameWallManager = _bee7GameWallManager;

                if (bee7GameWallManager instanceof Bee7GameWallManagerV2) {
                    manager = new Bee7GameWallManagerV2() {
                        @Override
                        public void onGameWallShowRequest() {
                            if (bee7GameWallManager != null) {
                                ((Bee7GameWallManagerV2) bee7GameWallManager).onGameWallShowRequest();
                            }
                        }

                        @Override
                        public void onBannerNotificationShowRequest() {
                            if (bee7GameWallManager != null) {
                                ((Bee7GameWallManagerV2) bee7GameWallManager).onBannerNotificationShowRequest();
                            }
                        }

                        @Override
                        public void onBannerNotificationClick() {
                            if (bee7GameWallManager != null) {
                                ((Bee7GameWallManagerV2) bee7GameWallManager).onBannerNotificationClick();
                            }
                        }

                        @Override
                        public void onBannerNotificationVisibilityChanged(boolean visible) {
                            if (bee7GameWallManager != null) {
                                ((Bee7GameWallManagerV2) bee7GameWallManager).onBannerNotificationVisibilityChanged(visible);
                            }
                        }

                        @Override
                        public void onGiveReward(Reward reward) {
                            if (bee7GameWallManager != null) {
                                bee7GameWallManager.onGiveReward(reward);
                            }
                        }

                        @Override
                        public void onAvailableChange(boolean available) {
                            if (bee7GameWallManager != null) {
                                bee7GameWallManager.onAvailableChange(available);
                            }
                        }

                        @Override
                        public void onVisibleChange(boolean visible, boolean isGameWall) {
                            if (!visible && isGameWall) {
                                if (gameWallActivityActivity != null) {
                                    gameWallActivityActivity.finish();

                                    gameWallActivityActivity = null;
                                }
                            }

                            if (bee7GameWallManager != null) {
                                bee7GameWallManager.onVisibleChange(visible, isGameWall);
                            }
                        }

                        @Override
                        public boolean onGameWallWillClose() {
                            if (bee7GameWallManager != null) {
                                return bee7GameWallManager.onGameWallWillClose();
                            }
                            return false;
                        }

                        @Override
                        public void onReportingId(String reportingId, long reportingIdTs) {
                            if (bee7GameWallManager != null) {
                                bee7GameWallManager.onReportingId(reportingId, reportingIdTs);
                            }
                        }
                    };
                } else {
                    manager = new Bee7GameWallManager() {
                        @Override
                        public void onGiveReward(Reward reward) {
                            if (bee7GameWallManager != null) {
                                bee7GameWallManager.onGiveReward(reward);
                            }
                        }

                        @Override
                        public void onAvailableChange(boolean available) {
                            if (bee7GameWallManager != null) {
                                bee7GameWallManager.onAvailableChange(available);
                            }
                        }

                        @Override
                        public void onVisibleChange(boolean visible, boolean isGameWall) {
                            if (!visible && isGameWall) {
                                if (gameWallActivityActivity != null) {
                                    gameWallActivityActivity.finish();

                                    gameWallActivityActivity = null;
                                }
                            }

                            if (bee7GameWallManager != null) {
                                bee7GameWallManager.onVisibleChange(visible, isGameWall);
                            }
                        }

                        @Override
                        public boolean onGameWallWillClose() {
                            if (bee7GameWallManager != null) {
                                return bee7GameWallManager.onGameWallWillClose();
                            }
                            return false;
                        }

                        @Override
                        public void onReportingId(String reportingId, long reportingIdTs) {
                            if (bee7GameWallManager != null) {
                                bee7GameWallManager.onReportingId(reportingId, reportingIdTs);
                            }
                        }
                    };
                }


                gameWallImpl = new GameWallImpl(activity, manager, apiKey, vendorId, miniGames, showNotifications);

                gameWallImpl.checkForClaimData(activity.getIntent());

                this.activity = activity;
                gameWallActivityActivity = null;

                settingUpGameWall = false;
            } catch (Exception ex) {
                Logger.debug(TAG, ex, "Failed to init game wall");
            }
        }
    }

    public void resume() {
        if (gameWallImpl != null) {
            Logger.debug(TAG, "GW resumed");

            gameWallImpl.resume();
        }
    }

    public void pause() {
        // do not pause if game wall activity is being set up
        if (!settingUpGameWall && gameWallImpl != null) {
            Logger.debug(TAG, "GW paused");

            gameWallImpl.pause();
        }
    }

    /**
     * Same as @destroy
     */
    public void hide() {
        destroy();
    }

    public void destroy() {
        // do not destroy if game wall activity is on top
        if (gameWallActivityActivity == null) {
            if (gameWallImpl != null) {
                Logger.debug(TAG, "GW destroyed");

                gameWallImpl.destroy();

                gameWallImpl = null;
            }
        }
    }

    public void checkForClaimData(Intent intent) {
        if (gameWallImpl != null && intent != null) {
            gameWallImpl.checkForClaimData(intent);
        }
    }

    public void setAgeGate(boolean hasPassed) {
        if (gameWallImpl != null) {
            gameWallImpl.setAgeGate(hasPassed);
        }
    }

    public void setImmersiveMode(boolean _immersiveMode) {
        Logger.debug(TAG, "setImmersiveMode()");
        if (gameWallImpl != null) {
            gameWallImpl.setImmersiveMode(_immersiveMode);
        }
    }

    public void show() {
        try {

            if (gameWallActivityActivity == null
                    || gameWallActivityActivity.isFinishing()) {
                if (activity != null) {
                    settingUpGameWall = true;

                    Intent intent = new Intent(activity, GameWallActivity.class);

                    intent.putExtra(GameWallActivity.IMMERSIVE_MODE_KEY, immersiveMode);

                    activity.startActivity(intent);

                    Logger.debug(TAG, "GW starting activity");
                }
            } else {
                if (gameWallImpl != null) {
                    Logger.debug(TAG, "GW showed");

                    gameWallImpl.show(gameWallActivityActivity);
                }
            }
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to show game wall");
        }
    }

    public void showReward(Reward reward) {
        if (gameWallImpl != null) {
            if (gameWallActivityActivity != null && gameWallActivityActivity.isVisible()) {
                Logger.debug(TAG, "GW show reward on GW activity");

                gameWallImpl.showReward(reward, gameWallActivityActivity);
            } else {
                Logger.debug(TAG, "GW show reward on main activity");

                gameWallImpl.showReward(reward, activity);
            }
        }
    }

    public void onGameWallButtonImpression() {
        if (gameWallImpl != null) {
            gameWallImpl.onGameWallButtonImpression();
        }
    }

    public void setTestVariant(String testId) {
        Logger.debug(TAG, "setTestVariant " + testId);
        if (gameWallImpl != null) {
            gameWallImpl.setTestVariant(testId);
        }
    }

    public void setBee7GameWallViewsInterface(Bee7GameWallViewsInterface bee7GameWallViewsInterface) {
        Logger.debug(TAG, "setBee7GameWallViewsInterface");
        if (bee7GameWallViewsInterface != null) {
            gameWallImpl.setBee7GameWallViewsInterface(bee7GameWallViewsInterface);
        }
    }

    public void setOnOfferListener(OnOfferListener onOfferListener) {
        Logger.debug(TAG, "setOnOfferListener");
        if (gameWallImpl != null) {
            gameWallImpl.setOnOfferListener(onOfferListener);
        }
    }

    public void requestAppOffersOfType(Publisher.AppOffersType type) {
        Logger.debug(TAG, "requestAppOffersOfType " + type);
        if (gameWallImpl != null) {
            gameWallImpl.requestAppOffersOfType(type);
        }
    }

    /**
     * @return true if banner notification can be shown, false otherwise
     */
    public boolean canShowReward(Reward reward, Activity activity) {
        Logger.debug(TAG, "canShowReward()");
        if (gameWallImpl != null) {
            return gameWallImpl.canShowReward(reward, activity);
        }
        return false;
    }

    /**************************************************
     * Activity calls
     **************************************************/
    public void addGameWallContent(GameWallActivity activity) {
        gameWallActivityActivity = activity;

        settingUpGameWall = true;

        if (gameWallImpl != null) {
            Logger.debug(TAG, "GW show on GW activity");

            gameWallImpl.show(gameWallActivityActivity);
        }
    }

    public void resumeGameWall() {
        // first resume after game wall activity was created
        if (settingUpGameWall) {
            settingUpGameWall = false;

            if (gameWallImpl != null) {
                gameWallImpl.saveAppStartTimestamp();
            }
        } else if (gameWallImpl != null) {
            // resume of game wall activity
            gameWallImpl.resume();

            Logger.debug(TAG, "GW resumed from GW activity");
        }
    }

    public void pauseGameWall() {
        // pausing with game wall activity on top
        if (gameWallImpl != null) {
            gameWallImpl.saveAppCloseTimestamp();
        }

        if (gameWallActivityActivity != null) {
            if (gameWallImpl != null) {
                gameWallImpl.pause();
                Logger.debug(TAG, "GW paused from GW activity");
            }
        }
    }

    public void destroyGameWall() {
        // hide game wall in case main activity was activated again
        if (gameWallImpl != null) {
            Logger.debug(TAG, "GW hide from GW activity");

            gameWallImpl.hide();
        }

        // reset activity reference
        gameWallActivityActivity = null;
    }

    public boolean onBackPressed() {
        if (gameWallImpl != null) {
            return gameWallImpl.onBackPressed();
        }

        return false;
    }

    public void updateView() {
        if (gameWallActivityActivity != null) {
            if (gameWallImpl != null) {
                Logger.debug(TAG, "GW updated from GW activity");

                gameWallImpl.updateView();
            }
        }
    }

    /***************************************************
     * Bee7BannerNotificationInterface impl
     ***************************************************/

    @Override
    public Reward showBannerNotification(View anchorView, BannerNotificationPosition bannerNotificationPosition) {
        Logger.debug(TAG, "showBannerNotification");
        if (gameWallImpl != null) {
            return gameWallImpl.showBannerNotification(anchorView, bannerNotificationPosition);
        }
        return null;
    }

    @Override
    public boolean isBannerNotificationShown() {
        Logger.debug(TAG, "isBannerNotificationShown");
        if (gameWallImpl != null) {
            return gameWallImpl.isBannerNotificationShown();
        }
        return false;
    }

    @Override
    public void closeBannerNotification() {
        Logger.debug(TAG, "closeBannerNotification");
        if (gameWallImpl != null) {
            gameWallImpl.closeBannerNotification();
        }
    }

    @Override
    public void setVirtualCurrencyState(boolean lowCurrency) {
        Logger.debug(TAG, "setVirtualCurrencyState " + lowCurrency);
        if (gameWallImpl != null) {
            gameWallImpl.setVirtualCurrencyState(lowCurrency);
        }
    }

    @Override
    public void toggleNotificationShowing(boolean notificationShowing) {
        Logger.debug(TAG, "toggleNotificationShowing " + notificationShowing);
        if (gameWallImpl != null) {
            gameWallImpl.toggleNotificationShowing(notificationShowing);
        }
    }
}
