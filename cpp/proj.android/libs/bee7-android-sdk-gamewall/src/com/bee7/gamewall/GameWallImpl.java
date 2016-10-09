package com.bee7.gamewall;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

import com.bee7.gamewall.assets.AnimFactory;
import com.bee7.gamewall.assets.AssetsManager;
import com.bee7.gamewall.assets.UnscaledBitmapLoader;
import com.bee7.gamewall.dialogs.DialogDebug;
import com.bee7.gamewall.dialogs.DialogNoInternet;
import com.bee7.gamewall.dialogs.DialogRedirecting;
import com.bee7.gamewall.enums.BannerNotificationPosition;
import com.bee7.gamewall.interfaces.BannerNotificationGameWallInterface;
import com.bee7.gamewall.interfaces.Bee7BannerNotificationInterface;
import com.bee7.gamewall.interfaces.Bee7GameWallManager;
import com.bee7.gamewall.interfaces.Bee7GameWallManagerV2;
import com.bee7.gamewall.interfaces.Bee7GameWallViewsInterface;
import com.bee7.gamewall.interfaces.GamewallHeaderCallbackInterface;
import com.bee7.gamewall.interfaces.OnVideoRewardGeneratedListener;
import com.bee7.sdk.common.Bee7;
import com.bee7.sdk.common.ExternalDebugToolsDialog;
import com.bee7.sdk.common.OnEnableChangeListener;
import com.bee7.sdk.common.OnReportingIdChangeListener;
import com.bee7.sdk.common.Reward;
import com.bee7.sdk.common.RewardCollection;
import com.bee7.sdk.common.task.TaskFeedback;
import com.bee7.sdk.common.util.*;
import com.bee7.sdk.publisher.DefaultPublisher;
import com.bee7.sdk.publisher.GameWallConfiguration;
import com.bee7.sdk.publisher.OnOfferListener;
import com.bee7.sdk.publisher.Publisher;
import com.bee7.sdk.publisher.PublisherConfiguration;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResult;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;
import com.bee7.sdk.publisher.appoffer.AppOffersModelEvent;
import com.bee7.sdk.publisher.appoffer.AppOffersModelListener;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameWallImpl implements AppOffersModelListener, NotifyReward.DisplayedChangeListener,
        OnVideoRewardGeneratedListener, Bee7BannerNotificationInterface, BannerNotificationGameWallInterface{
    static final String TAG = GameWallImpl.class.getSimpleName();

    private static String BEE7_API_KEY;
    private static String BEE7_VENDOR_ID;
    private static String BEE7_GAMEWALL_VERSION = "2.11.3";

    private static boolean minigameStarted = false;

    private Context context;
    private Bee7GameWallManager manager;

    protected DefaultPublisher mPublisher;
    protected RewardQueue mRewardQueue;
    protected BannerNotificationQueue notificationQueue;
    private GameWallView mGameWallView = null;
    private static Activity mActivity;
    private static Dialog mDialog;
    private Uri mClaimData;

    private boolean mShown;
    private boolean mPendingGWBtnImpression = false;
    private Reward fullScreenVideoWatchedReward;
    private SharedPreferencesRewardsHelper sharedPreferencesRewardsHelper;
    private List<AppOffer> innerApps;
    private Object innerAppsLock = new Object();
    private static OnOfferListener onOfferListener;

    boolean animate = false; //TODO make configurable
    private static boolean immersiveMode = false;

    private Bee7GameWallViewsInterface bee7GameWallViewsInterface;

    public GameWallImpl(Context ctx, final Bee7GameWallManager manager, List<AppOffer> miniGames)
    {
        this.init(ctx, manager, null, null, miniGames, true);
    }

	public GameWallImpl(Context ctx, final Bee7GameWallManager manager, String apiKey) {
        this.init(ctx, manager, apiKey, null, null, false);
    }
    
    public GameWallImpl(Context ctx, final Bee7GameWallManager manager, String apiKey, String vendorId, List<AppOffer> miniGames) {
        this.init(ctx, manager, apiKey, vendorId, miniGames, true);
    }

    public GameWallImpl(Context ctx, final Bee7GameWallManager manager, String apiKey, String vendorId, List<AppOffer> miniGames, boolean showNotifications) {
        this.init(ctx, manager, apiKey, vendorId, miniGames, showNotifications);
    }

    private void init(Context ctx, final Bee7GameWallManager manager, String apiKey, String vendorId, final List<AppOffer> miniGames, boolean showNotifications)
    {
        Logger.debug(TAG, "Bee7 game wall version: " + BEE7_GAMEWALL_VERSION  + ", Bee7 SDK version " + Bee7.LIB_VERSION);
        this.context = ctx;
        this.manager = manager;

        BEE7_API_KEY = apiKey;

        BEE7_VENDOR_ID = vendorId;

        innerApps = new ArrayList<AppOffer>();

        if (miniGames != null && !miniGames.isEmpty()) {
            innerApps.addAll(miniGames);
        }

        if (manager instanceof Bee7GameWallManagerV2) {
            notificationQueue = new BannerNotificationQueue(context, (Bee7GameWallManagerV2)manager, showNotifications);
        } else {
            Logger.warn(TAG, "Bee7GameWallManagerV2 is not implemented, banner notifications will not work.");
        }

        mPublisher = DefaultPublisher.getInstance();

        mPublisher.disableProgressIndicator();

        mPublisher.setContext(context);
        mPublisher.setApiKey(BEE7_API_KEY);
        mPublisher.setTestVendorId(BEE7_VENDOR_ID);
        mPublisher.setProxyEnabled(true);

        mRewardQueue = new RewardQueue(mPublisher);

        mPublisher.setOnEnableChangeListener(new OnEnableChangeListener() {
            @Override
            public void onEnableChange(boolean enabled) {
                Logger.debug(TAG, "Publisher OnEnableChangeListener onEnableChange " + enabled);
                if (mPendingGWBtnImpression) {
                    mPendingGWBtnImpression = false;

                    if (enabled) {
                        mPublisher.onGameWallButtonImpression();
                    }
                }

                if (manager != null) {
                    manager.onAvailableChange(isAvailable(enabled));
                }

                if (enabled && mPublisher != null) {
                    int maxDailyRewardFreq = 1;

                    if (mPublisher.isEnabled()) {
                        maxDailyRewardFreq = mPublisher.getAppOffersModel().getVideoPrequalGlobalConfig().getMaxDailyRewardFreq();
                    }

                    sharedPreferencesRewardsHelper = new SharedPreferencesRewardsHelper(context, maxDailyRewardFreq);

                    if (notificationQueue != null) {
                        notificationQueue.setBannerNotificationConfig(mPublisher);
                    }

                    tryClaim();

                    saveAppStartAndTriggerNotifications();
                }

            }
        });

        mPublisher.getAppOffersModel().addAppOffersModelListener(this);

        mPublisher.setOnReportingIdChangeListener(new OnReportingIdChangeListener() {
            @Override
            public void onReportingIdChange(String reportingId, long reportingIdTs) {
                if (manager != null) {
                    manager.onReportingId(reportingId, reportingIdTs);
                }
            }
        });

        if (com.bee7.sdk.common.util.Utils.hasText(BEE7_API_KEY)) {
            mPublisher.start(new TaskFeedback<Boolean>() {
                @Override
                public void onStart() {
                    Logger.debug(TAG, "Starting...");
                }

                @Override
                public void onCancel() {
                    Logger.debug(TAG, "Canceled starting");

                    if (manager != null) {
                        manager.onAvailableChange(isAvailable(false));
                    }
                }

                @Override
                public void onResults(Boolean result) {

                }

                @Override
                public void onFinish(Boolean result) {
                    Logger.debug(TAG, "Started - enabled=" + result);

                    if (manager != null && !result) {
                        manager.onAvailableChange(isAvailable(false));
                    }

                    int maxDailyRewardFreq = 1;

                    if (mPublisher.isEnabled()) {
                        maxDailyRewardFreq = mPublisher.getAppOffersModel().getVideoPrequalGlobalConfig().getMaxDailyRewardFreq();
                    }

                    if (notificationQueue != null) {
                        notificationQueue.setBannerNotificationConfig(mPublisher);
                    }

                    sharedPreferencesRewardsHelper = new SharedPreferencesRewardsHelper(context, maxDailyRewardFreq);

                    //tryClaim();

                    //saveAppStartAndTriggerNotifications();
                }

                @Override
                public void onError(Exception e) {
                    Logger.debug(TAG, "Error starting: {0}", e.toString());

                    if (manager != null) {
                        manager.onAvailableChange(isAvailable(false));
                    }
                }
            });
        }
    }

    private void saveAppStartAndTriggerNotifications() {
        try {
            SharedPreferencesNotificationsHelper sharedPreferencesNotificationsHelper = new SharedPreferencesNotificationsHelper(context, mPublisher);

            if (mPublisher.saveAppStartTimestamp()) {
                sharedPreferencesNotificationsHelper.clearBannerNotificationCount();
            }

            //if we should remind player
            if (sharedPreferencesNotificationsHelper.shouldRemindPlayer(
                    com.bee7.gamewall.Utils.getNumberOfItemsInGwUnitListHolder(context),
                    com.bee7.gamewall.Utils.isPortrate(context))) {
                if (notificationQueue != null) {
                    notificationQueue.addReminderNotificationBannerToQueue();
                }
            }
            //if player is in low currency state
            if (sharedPreferencesNotificationsHelper.shouldShowLowCurrencyBanner(
                    com.bee7.gamewall.Utils.getNumberOfItemsInGwUnitListHolder(context),
                    com.bee7.gamewall.Utils.isPortrate(context))) {
                if (notificationQueue != null) {
                    notificationQueue.addLowCurrencyNotificationBannerToQueue();
                }
            }
        } catch (Exception e) {
            Logger.debug(TAG, e, "Failed save app start session: {0}", e.getMessage());
        }

    }

    public void setAgeGate(boolean hasPassed) {
        if (mPublisher != null) {
            mPublisher.setAgeGate(hasPassed);
        }
    }

    public void setImmersiveMode(boolean _immersiveMode) {
        immersiveMode = _immersiveMode;
    }

    public void setTestVariant(String testId) {
        if (mPublisher != null) {
            mPublisher.setTestVariant(testId);
        }
    }

    public void setStoreId(String storeId) {
        if (mPublisher != null) {
            mPublisher.setStoreId(storeId);

            DialogDebug.setStoreId(storeId);
        }
    }

    public void requestAppOffersOfType(Publisher.AppOffersType type) {
        if (mPublisher != null) {
            mPublisher.requestAppOffersOfType(type);
        }
    }

    public void updateMiniGames(List<AppOffer> miniGames) {
        if (miniGames != null && !miniGames.isEmpty()) {
            synchronized (innerAppsLock) {

                if (innerApps == null) {
                    innerApps = new ArrayList<AppOffer>();
                }

                innerApps.clear();

                innerApps.addAll(miniGames);
            }
        }
    }

    /**
     * When user clicks on an offer, Bee7 SDK will open the app locally or on store
     * @param appOffer Bee7 AppOffer reference
     */
    public static void startAppOffer(final AppOffer appOffer, final AppOfferWithResult appOfferWithResult, final Context context, final Publisher mPublisher, Publisher.AppOfferStartOrigin origin)
    {
        if (onOfferListener != null && appOffer.getState() == AppOffer.State.CONNECTED && !appOffer.isInnerApp()) {
            onOfferListener.onConnectedOfferClick(appOffer.getId());
        }

        // call provided method in order to start inner app
        if (appOffer.isInnerApp()) {
            try {
                minigameStarted = true;
                appOffer.updateLastPlayedTimestamp(context);
                appOffer.startInnerApp();
            } catch(Exception e) {
                Logger.error(TAG, e, "Failed to start inner app");
            }

            return;
        }

        if (appOfferWithResult != null) {
            appOfferWithResult.setClickOrigin(origin);
        }

        if (appOffer.getState() == AppOffer.State.CONNECTED) {
            appOffer.updateLastPlayedTimestamp(context);
        }

        // no need to try to open
        if (appOffer.getState() != AppOffer.State.CONNECTED && !com.bee7.sdk.common.util.Utils.isOnline(context)) {
            showNoConnectionDialog(context);
            return;
        }

        Logger.debug(TAG, "startAppOffer(appOffer={0})", appOffer);

        final DialogRedirecting dialogRedirecting = new DialogRedirecting(context, appOffer,
                mPublisher.getAppOffersModel().getGameWallConfiguration().isTutorialEnabledRedirecting(),
                mPublisher.getAppOffersModel().getGameWallConfiguration().getRedirectingTimeout(),
                immersiveMode);
        dialogRedirecting.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Logger.debug(TAG, "DialogRedirecting OnCancelListener");
                mPublisher.cancelAppOffer();
            }
        });

        mPublisher.startAppOffer(appOffer, appOfferWithResult, new TaskFeedback<Void>() {
            @Override
            public void onStart() {
                Logger.debug(TAG, "Opening app offer: " + appOffer.getId());
                try {
                    dialogRedirecting.show();
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onCancel() {
                Logger.debug(TAG, "Canceled opening app offer: " + appOffer.getId());
                try {
                    dialogRedirecting.dismiss();
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onResults(Void result) {

            }

            @Override
            public void onFinish(Void result) {
                Logger.debug(TAG, "Opened app offer: " + appOffer.getId());

                try {
                    dialogRedirecting.dismiss();
                } catch (Exception ignored) {
                }
            }

            @Override
            public void onError(Exception e) {
                Logger.error(TAG, "Error opening app offer: {0} \n {1}", appOffer.getId(), e.getMessage());
                e.printStackTrace();

                try {
                    dialogRedirecting.dismiss();
                } catch (Exception ignored) {
                }
            }
        }, System.currentTimeMillis());
    }

    /**
     * On app start, resume or when publisher is enabled, reward should be claimed
     */
    public void tryClaim() {
        Logger.debug(TAG, "tryClaim");

        if (!mPublisher.isEnabled()) {
            return;
        }

        mPublisher.claimReward(mClaimData, new TaskFeedback<RewardCollection>() {
            @Override
            public void onStart() {
                Logger.debug(TAG, "Claiming reward...");
            }

            @Override
            public void onCancel() {
                Logger.debug(TAG, "Canceled claiming");
            }

            @Override
            public void onResults(RewardCollection result) {
                Logger.debug(TAG, "claimReward onResults {0}", result.toJson().toString());
                for (Reward reward : result) {
                    if (addReward(reward)) {
                        addNotificationToQueue(reward);

                        if (manager != null) {
                            manager.onGiveReward(reward);
                        }
                    }
                }
            }

            @Override
            public void onFinish(RewardCollection result) {
                Logger.debug(TAG, "Number of rewards: " + result.size());
            }

            @Override
            public void onError(Exception e) {
                Logger.debug(TAG, "Error claiming: {0}", e.toString());
            }
        });

        mClaimData = null;
    }

    /**
     * Get custom reward data if available
     */
    public void checkForClaimData(Intent intent) {
        if (intent != null) {
            Uri data = intent.getData();

            if (data != null && "publisher".equals(data.getHost())) {
                mClaimData = data;
            }
        }
    }

    /**
     * Called from the activity in order to display the game wall
     */
    public void show(Activity _activity) {
        Logger.debug(TAG, "show()");

        //If we are returning from mini-game and nothing was cleared we just show it
        if (minigameStarted && mActivity != null && mGameWallView != null) {
            minigameStarted = false;
            mGameWallView.setVisibility(View.VISIBLE);
            markGameWallAsShown();
            mGameWallView.disableClickEvents = false;

        } else {

            mActivity = _activity;

            if (mGameWallView == null) {
                mGameWallView = (GameWallView) mActivity.getLayoutInflater().inflate(R.layout.gamewall_view, null);

                setHeader();

                //we intercept any touch events so they do not get send to the underneath view
                mGameWallView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        Logger.debug(TAG, "onTouch");
                        return true;
                    }
                });
            }

            if (mGameWallView != null) {
                mGameWallView.disableClickEvents = true;
            }

            try {
                mGameWallView.init(mPublisher, this, immersiveMode, this, manager, dialogDebugInterface);

                mGameWallView.getGamesScrollView().fullScroll(View.FOCUS_UP);

                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                        animate) {
                    mGameWallView.setVisibility(View.INVISIBLE);
                    mActivity.getWindow().addContentView(mGameWallView, lp);

                    mGameWallView.post(new Runnable() {
                        @Override
                        public void run() {
                            Animation anim = AnimFactory.createSlideInFromBottom(mGameWallView);
                            anim.setDuration(AnimFactory.ANIMATION_DURATION_LONG);
                            anim.setInterpolator(new DecelerateInterpolator(3f));
                            anim.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                    mGameWallView.setVisibility(View.VISIBLE);
                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    mGameWallView.viewShown();
                                    checkForOffers();
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {
                                }
                            });
                            mGameWallView.startAnimation(anim);
                        }
                    });
                } else {
                    mActivity.getWindow().addContentView(mGameWallView, lp);
                    mGameWallView.viewShown();
                    checkForOffers();
                }

                markGameWallAsShown();
            } catch (Exception e) {
                Logger.error(TAG, e, "{0}", e.getMessage());
            }
        }
    }

    public void show(Dialog dialog) {
        Logger.debug(TAG, "show()");

        //If we are returning from mini-game and nothing was cleared we just show it
        if (minigameStarted && mDialog != null && mGameWallView != null) {
            minigameStarted = false;
            mDialog.show();
            markGameWallAsShown();
            mGameWallView.disableClickEvents = false;
        } else {

            mDialog = dialog;

            if (immersiveMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                dialog.setCanceledOnTouchOutside(false);
                dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                com.bee7.sdk.common.util.Utils.setImmersiveModeFlags(dialog.getWindow().getDecorView());
            }

            if (mGameWallView == null) {
                mGameWallView = (GameWallView) dialog.getLayoutInflater().inflate(R.layout.gamewall_view, null);

                setHeader();

                //we intercept any touch events so they do not get send to the underneath view
                mGameWallView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        Logger.debug(TAG, "onTouch");
                        return true;
                    }
                });
            }

            try {
                mGameWallView.init(mPublisher, this, immersiveMode, this, manager, dialogDebugInterface);

                mGameWallView.getGamesScrollView().fullScroll(View.FOCUS_UP);

                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                try {
                    dialog.getWindow().addContentView(mGameWallView, lp);
                } catch (Exception e) {
                    Logger.error(TAG, e, "{0}", e.getMessage());
                }
                checkForOffers();

                dialog.show();

                if (immersiveMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                }

                markGameWallAsShown();
                mGameWallView.disableClickEvents = false;
            } catch (Exception e) {
                Logger.error(TAG, e, "{0}", e.getMessage());
            }
        }
    }

    DialogDebug.DialogDebugInterface dialogDebugInterface = new DialogDebug.DialogDebugInterface() {
        @Override
        public void onRewardGenerated(Reward reward) {
            if (addReward(reward)) {
                addNotificationToQueue(reward);
                manager.onGiveReward(reward);
            }
        }
    };

    private void markGameWallAsShown() {
        mShown = true;

        mGameWallView.viewShown();

        mPublisher.onGameWallImpression();
        mPublisher.saveGameWallOpenTimestamp();

        if (manager != null) {
            manager.onVisibleChange(true, true);
        }

        resumeShowingOfRewardBanners();

        if (notificationQueue != null) {
            closeBannerNotification(); //Stop showing banners
            notificationQueue.setGWVisibility(mShown);
        }
    }

    private void resumeShowingOfRewardBanners() {
        if (notificationQueue != null) {
            notificationQueue.setGWVisibility(mShown);
            notificationQueue.clearNonRewardBannersFromQueue();

            //delay so we don't do to many things at the same time
            mGameWallView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    notificationQueue.resumeNotificationShowingOnGameWall();
                }
            }, 500);
        }
    }


    private void setHeader() {
        View headerView = null;
        if (bee7GameWallViewsInterface != null) {
            headerView = bee7GameWallViewsInterface.provideHeaderView(gamewallHeaderCallbackInterface);
            Logger.debug(TAG, "headerView provideHeaderView " + ((headerView != null) ? "not null" : "null"));
        }

        if (headerView == null) {
            headerView = new GameWallHeader(context, gamewallHeaderCallbackInterface);
            Logger.debug(TAG, "headerView default header view");
        }

        mGameWallView.setHeader(headerView);
    }

    /**
     * @param reward to be checked.
     * @return true if virtual currency amount is larger than 0, false otherwise.
     */
    private boolean addReward(Reward reward) {
        return reward.getVirtualCurrencyAmount() > 0;
    }

    /**
     * Show reward notification bubble
     * @param reward Bee7 Reward reference
     * @return returns true if bubble was displayed or if notification bubbles are disabled
     */
    public boolean showReward(Reward reward, Activity activity) {
        Logger.debug(TAG, "showReward");
        if (reward.getVirtualCurrencyAmount() > 0) {

            mRewardQueue.setImmersiveMode(immersiveMode);

            if (mPublisher != null) {
                NotifyReward msg = new NotifyReward(this);

                msg.addMsg(
                        getRewardAmountString(reward),
                        getAppIcon(reward),
                        context.getResources().getDrawable(R.drawable.bee7_icon_reward),
                        getPublisherDrawable(),
                        reward.isVideoReward(),
                        reward.getPending());

                msg.queueOnStoppedQueue = true;
                mRewardQueue.addMessage(msg);
            }

            mRewardQueue.startProcessing(activity, immersiveMode);

            return true;
        } else {
            Logger.debug(TAG, "Reward with low VC amount: {0}", reward.toString());

            return false;
        }
    }

    /**
     *
     * @return true if banner notification can be shown, false otherwise
     */
    public boolean canShowReward(Reward reward, Activity activity) {
        Logger.debug(TAG, "canShowReward: {0}", reward.toJson().toString());
        if (mPublisher != null && !reward.isVideoReward()) { //check if should display banner notification instead of dialog bubble, or if it is video reward
            PublisherConfiguration.BannerNotificationConfig bannerNotification = mPublisher.getBannerNotificationConfig();
            if (bannerNotification != null && bannerNotification.isEnabled() && notificationQueue != null) {
                return true;
            } else {
                Logger.debug(TAG, "Cant show notification banner; bannerNotification is null or bannerNotification is not disabled or notificationQueue is null");
            }
        } else {
            if (mPublisher == null) {
                Logger.debug(TAG, "Cant show notification banner; mPublisher is null");
            }
            if (reward.isVideoReward()) {
                Logger.debug(TAG, "Cant show notification banner; reward.isVideoReward");
            }
        }
        return false;
    }

    private String getRewardAmountString(Reward reward) {
        return String.format("%+,d", reward.getVirtualCurrencyAmount());
    }

    private Bitmap getAppIcon(Reward reward) {
        Bitmap bm = null;
        if (reward.isHidden()) {
            bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.default_game_icon);
        } else {
            if (!reward.isVideoReward()) {
                byte[] ba = AssetsManager.getInstance().getCachedBitmap(context, reward.getIconUrl(getRewardIconUrlSize(context.getResources())));

                UnscaledBitmapLoader.ScreenDPI screenDPI = UnscaledBitmapLoader.ScreenDPI.parseDensity(context.getResources()
                        .getString(R.string.bee7_gamewallSourceIconDPI));

                bm = AssetsManager.getInstance().makeBitmap(ba, context, screenDPI.getDensity());
            } else {
                bm = AssetsManager.getInstance().getVideoRewardBitmap(context);
            }
        }
        return bm;
    }

    private Drawable getPublisherDrawable() {
        Drawable bitmapPublisherIcon = null;
        try {
            bitmapPublisherIcon = context.getPackageManager().getApplicationIcon(context.getApplicationInfo());
        } catch (Exception ignore) { }
        return bitmapPublisherIcon;
    }

    public void onGameWallButtonImpression() {
        if (mPublisher != null && mPublisher.isEnabled()) {
            mPublisher.onGameWallButtonImpression();
        } else {
            mPendingGWBtnImpression = true;
        }
    }

    private static void showNoConnectionDialog(Context context) {
        new DialogNoInternet(context, immersiveMode).show();
    }

    public static AppOffer.IconUrlSize getAppOfIconUrlSize(Resources resources) {
        AppOffer.IconUrlSize iconUrlSize = AppOffer.IconUrlSize.SMALL;

        if (resources.getString(R.string.bee7_gamewallIconSize).equalsIgnoreCase("large")) {
            iconUrlSize = AppOffer.IconUrlSize.LARGE;
        }

        return iconUrlSize;
    }

    public static Reward.IconUrlSize getRewardIconUrlSize(Resources resources) {
        Reward.IconUrlSize iconUrlSize = Reward.IconUrlSize.SMALL;

        if (resources.getString(R.string.bee7_gamewallIconSize).equalsIgnoreCase("large")) {
            iconUrlSize = Reward.IconUrlSize.LARGE;
        }

        return iconUrlSize;
    }

    public void resume() {
        mPublisher.resume();

        if (fullScreenVideoWatchedReward != null && manager != null) {
            manager.onGiveReward(fullScreenVideoWatchedReward);
            fullScreenVideoWatchedReward = null;
        }

        if (mShown) {
            mPublisher.onGameWallImpression();
            mPublisher.saveGameWallOpenTimestamp();
        }

        // Claim reward if available
        tryClaim();

        if (mGameWallView != null && mShown) {
            mGameWallView.onResume();

            //we check if any video is active
            if ((mGameWallView.findViewWithVideoView() == null && mGameWallView.getVideoDialog() == null) ||
                    (mGameWallView.getVideoDialog() != null && !mGameWallView.getVideoDialog().isShowing())) {
                checkForOffers();
            }
        }

        saveAppStartAndTriggerNotifications();
    }

    /**
     * Called from activity onPause
     */
    public void pause() {
        if(mShown) {
            mPublisher.saveGameWallCloseTimestamp();
        }
        mPublisher.pause();
        if (mGameWallView != null && mShown) {
            mGameWallView.onPause();
        }
    }

    /**
     * Called from activity onDestroy
     */
    public void destroy() {
        mPublisher.stop();
        closeBannerNotification();
        if (mGameWallView != null && mShown) {
            mGameWallView.onDestroy();
        }
    }

    /**
     * Called from the onConfigurationChanged event in order to update view in case orientation
     * was changed
     */
    public void updateView() {
        Logger.debug(TAG, "updateView()");
        if (mShown) {
            checkForOffers();
        }
    }

    /**
     * Called from the onBackPressed event, in order to dismiss the game wall view
     */
    public void hide() {
        if (mGameWallView != null) {
            mGameWallView.disableClickEvents = true;
        }

        try {
            mShown = false;
            if (notificationQueue != null) {
                notificationQueue.setGWVisibility(mShown);
            }

            if (mActivity != null) {
                if (mPublisher != null) {
                    mPublisher.onGameWallCloseImpression();
                }

                if (minigameStarted && mGameWallView != null) {
                    mGameWallView.setVisibility(View.GONE);
                } else {
                    if (mGameWallView != null && mGameWallView.getParent() != null) {
                        final ViewGroup rootView = (ViewGroup) mGameWallView.getParent();

                        if (rootView != null && mGameWallView != null) {
                            if (mGameWallView.getVideoDialog() != null) {
                                mGameWallView.getVideoDialog().hide(true);
                            }

                            for (int i = 0; i < rootView.getChildCount(); i++) {
                                if (rootView.getChildAt(i).getId() == mGameWallView.getId()) {

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                                            animate) {
                                        mGameWallView.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                Animation anim = AnimFactory.createSlideOutFromTop(mGameWallView);
                                                anim.setDuration(AnimFactory.ANIMATION_DURATION_LONG);
                                                anim.setInterpolator(new AccelerateInterpolator(3f));
                                                anim.setAnimationListener(new Animation.AnimationListener() {
                                                    @Override
                                                    public void onAnimationStart(Animation animation) {
                                                    }

                                                    @Override
                                                    public void onAnimationEnd(Animation animation) {
                                                        mGameWallView.removeOfferViews();
                                                        rootView.removeView(mGameWallView);
                                                    }

                                                    @Override
                                                    public void onAnimationRepeat(Animation animation) {
                                                    }
                                                });
                                                mGameWallView.startAnimation(anim);
                                            }
                                        });
                                    } else {
                                        mGameWallView.removeOfferViews();
                                        rootView.removeView(mGameWallView);
                                    }

                                    if (mDialog != null) {
                                        mDialog.dismiss();
                                    }
                                }
                            }
                        }
                    }

                    mActivity = null;
                    mGameWallView = null;
                }

                if (manager != null) {
                	manager.onVisibleChange(false, true);
                }

                if(mPublisher != null) {
                    mPublisher.saveGameWallCloseTimestamp();
                }

            } else if (mDialog != null) {

                if (mPublisher != null) {
                    mPublisher.onGameWallCloseImpression();
                }

                if (minigameStarted && mDialog != null) {
                    mDialog.dismiss();
                } else {
                    if (mGameWallView != null && mGameWallView.getParent() != null) {
                        ViewGroup rootView = (ViewGroup) mGameWallView.getParent();

                        if (mGameWallView.getVideoDialog() != null &&
                                mGameWallView.getVideoDialog().isShowing()) {
                            mGameWallView.getVideoDialog().hide(true);
                        }

                        for (int i = 0; i < rootView.getChildCount(); i++) {
                            if (rootView.getChildAt(i).getId() == mGameWallView.getId()) {

                                if (mGameWallView != null && mGameWallView.getParent() != null) {
                                    ((ViewGroup)mGameWallView.getParent()).removeView(mGameWallView);
                                }

                                if (mDialog != null) {
                                    mDialog.dismiss();
                                }
                            }
                        }
                    }

                    mGameWallView = null;
                    mDialog = null;
                }


                if (manager != null) {
                    manager.onVisibleChange(false, true);
                }

                if(mPublisher != null) {
                    mPublisher.saveGameWallCloseTimestamp();
                }


            }
        } catch (Exception e) {
            Logger.error(TAG, e, "hide failed {0}", e.getMessage());
        }

        // reset blocking for GW
        synchronized (GameWallView.lastClickSync) {
                GameWallView.lastClickTimestamp = 0;
        }
    }

    /**
     *
     * @return true if we consume event
     */
    public boolean onBackPressed() {
        boolean consume = mShown;

        synchronized (GameWallView.lastClickSync) {
            if ((SystemClock.elapsedRealtime() - GameWallView.lastClickTimestamp) < 500) {
                return consume;
            }

            GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();

            if (mShown &&
                    mGameWallView.isVideoWithRewardPlaying() &&
                    !mGameWallView.isVideoCloseNoticeShowing()) {
                mGameWallView.closeVideo(true);
            } else if (mShown && mGameWallView.findViewWithVideoView() != null) {
                mGameWallView.closeVideo(true);
            } else if (mShown && mGameWallView.getVideoDialog() != null &&
                    mGameWallView.getVideoDialog().isShowing()) {
                mGameWallView.getVideoDialog().hide(false);
            } else {
                if (mShown) {
                    if (manager != null) {
                        if (manager.onGameWallWillClose()) {
                            hide();
                        } else {
                            // pending hide call, extend the blocking
                            GameWallView.lastClickTimestamp += 3000;
                            if (mGameWallView != null) {
                                mGameWallView.disableClickEvents = true;
                            }
                        }
                    } else {
                        hide();
                    }
                }
            }
        }

        return consume;
    }

    /**
     * Implementation for the AppOffersModelListener interface.
     * Receives notification when offers were changed.
     * @param event Bee7 AppOffersModelEvent
     */
    @Override
    public void onAppOffersChange(AppOffersModelEvent event) {
        Logger.debug(TAG, "onAppOffersChange() ");

        if (manager != null) {
            boolean enabled = mPublisher != null && mPublisher.isEnabled();
            manager.onAvailableChange(isAvailable(enabled));
        }

        /*boolean available = mPublisher != null && mPublisher.isEnabled() && mPublisher.getAppOffersModel().hasAnyAppOffers();

        synchronized (innerAppsLock) {
            if (innerApps != null && innerApps.size() > 0) {
                available = true;
            }
        }

        if (manager != null) {
            manager.onAvailableChange(available);
        }

        if (!available) {
            if (mShown) {
                if (manager != null) {
                    if (manager.onGameWallWillClose()) {
                        hide();
                    }
                } else {
                    hide();
                }
            }
            return;
        }

        if (!event.getAddedAppOffers().isEmpty()) {
            Logger.debug(TAG, "App offers change: added");
        }
        if (!event.getRemovedAppOffers().isEmpty()) {
            Logger.debug(TAG, "App offers change: removed");
        }
        if (!event.getChangedAppOffers().isEmpty()) {
            Logger.debug(TAG, "App offers change: changed");
        }

        if (!mShown) {
            checkForOffers();
        }*/
    }

    private boolean isAvailable(boolean enabled) {
        //there are mini games
        if (innerApps != null && innerApps.size() > 0) {
            return true;
        }

        //publisher is enabled and there are connected apps
        if (enabled && mPublisher.getAppOffersModel().getCurrentOrderedAppOffers(
                AppOffersModel.AppOffersState.CONNECTED_AND_PENDING_INSTALL).size() > 0) {
            return true;
        }

        //publisher is enabled, there are offers and we have configured layout to show them
        if (enabled && mPublisher.getAppOffersModel().hasAnyAppOffers() &&
                mPublisher.getAppOffersModel().getGameWallConfiguration().getLayoutMap().size() > 0) {
            return true;
        }

        return false;
    }

    /**
     * refreshes offers and updates ListView and GridView accordingly
     */
    public void checkForOffers() {
        Logger.debug(TAG, "checkForOffers()");
        AppOffersModel appOffersModel = mPublisher.getAppOffersModel();

        appOffersModel.checkOffersState();

        //Fetch offers
        List<AppOffer> appsNotInstalled = appOffersModel
                .getCurrentOrderedAppOffers(AppOffersModel.AppOffersState.NOT_CONNECTED_AND_PENDING_INSTALL);

        //Fetch connected offers
        List<AppOffer> appsInstalled = appOffersModel
                .getCurrentOrderedAppOffers(AppOffersModel.AppOffersState.CONNECTED_ONLY);

        //Add inner apps
        synchronized (innerAppsLock) {
            if (!innerApps.isEmpty()) {
                if (appsInstalled == null || appsInstalled.isEmpty()) {
                    appsInstalled = new ArrayList<AppOffer>();
                }

                appsInstalled.addAll(innerApps);
            }
        }

        //Fetch Layout map
        Map<GameWallConfiguration.LayoutType, List<GameWallConfiguration.UnitType>> layoutTypeListMap
                = appOffersModel.getLayoutUnitTypeMap();

        mGameWallView.updateGameWallView(appsNotInstalled, appsInstalled, layoutTypeListMap);
    }
    
    @Override
    public void onDisplayedChange(boolean displayed) {
    	if (!mShown) {
    		if (manager != null) {
    			manager.onVisibleChange(displayed, false);
    		}
    	}
    }

    public Bitmap getAppOfferIcon(String appId) {
        if (mPublisher == null || !mPublisher.isEnabled() || !mPublisher.getAppOffersModel().hasAnyAppOffers()) {
            return null;
        }

        AppOffer offer = mPublisher.getAppOffersModel().getCurrentAppOffer(appId);
        if (offer == null) {
            return null;
        }

        URL iconUrl = offer.getIconUrl(getAppOfIconUrlSize(context.getResources()));
        if (iconUrl == null) {
            return null;
        }

        byte[] ba = AssetsManager.getInstance().getCachedBitmap(context, iconUrl);
        if (ba == null) {
            return null;
        }

        UnscaledBitmapLoader.ScreenDPI screenDPI = UnscaledBitmapLoader.ScreenDPI.parseDensity(context.getResources()
                .getString(R.string.bee7_gamewallSourceIconDPI));
        Bitmap bm = AssetsManager.getInstance().makeBitmap(ba, context, screenDPI.getDensity());

        return bm;
    }

    public Bitmap getAppOfferIcon(Reward reward) {
        if (reward == null) {
            return null;
        }

        if (reward.isVideoReward()) {
            return AssetsManager.getInstance().getVideoRewardBitmap(context);
        } else {
            return getAppOfferIcon(reward.getAppId());
        }
    }

    @Override
    public void onVideoRewardGenerated(AppOffer appOffer) {
        Logger.debug("GameWallImpl", "onVideoRewardGenerated " + appOffer.getLocalizedName() + " " + appOffer.getId());
        if ((sharedPreferencesRewardsHelper != null
                &&
                !sharedPreferencesRewardsHelper.hasBeenRewardAlreadyGiven(appOffer.getId(), appOffer.getCampaignId()))
                ||
                (sharedPreferencesRewardsHelper != null && com.bee7.sdk.common.util.Utils.isDevBackendEnabled(context))) {
            Reward reward = mPublisher.generateVideoReward(appOffer);
            if (manager != null && reward != null) {
                sharedPreferencesRewardsHelper.saveGivenRewardKey(appOffer.getId(), appOffer.getCampaignId());

                if (addReward(reward)) {
                    addNotificationToQueue(reward);
                    manager.onGiveReward(reward);
                }
            }
        }

        if (mGameWallView != null) {
            mGameWallView.updateGameWallUnit(appOffer);
        }
    }

    public void setOnOfferListener(OnOfferListener _onOfferListener) {
        onOfferListener = _onOfferListener;
        mPublisher.setOnOfferListener(_onOfferListener);
    }

    public void setBee7GameWallViewsInterface(Bee7GameWallViewsInterface bee7GameWallViewsInterface) {
        this.bee7GameWallViewsInterface = bee7GameWallViewsInterface;
    }

    private GamewallHeaderCallbackInterface gamewallHeaderCallbackInterface = new GamewallHeaderCallbackInterface() {
        @Override
        public void onClose() {
            synchronized (GameWallView.lastClickSync) {
                if ((SystemClock.elapsedRealtime() - GameWallView.lastClickTimestamp) < 500) {
                    return;
                }
                GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();

                if (mShown) {
                    mGameWallView.closeVideo(false);
                }

                if (manager != null) {
                    if (manager.onGameWallWillClose()) {
                        hide();
                    } else {
                        // pending hide call, extend the blocking
                        GameWallView.lastClickTimestamp += 3000;
                        if (mGameWallView != null) {
                            mGameWallView.disableClickEvents = true;
                        }
                    }
                } else {
                    hide();
                }
            }
        }
    };

    @Override
    public Reward showBannerNotification(View anchorView, BannerNotificationPosition bannerNotificationPosition) {
        Logger.debug(TAG, "showBannerNotification");
        if (notificationQueue != null && !notificationQueue.isEmpty()) {
            return notificationQueue.startProcessing(context, anchorView, bannerNotificationPosition, (Bee7GameWallManagerV2) manager, mGameWallView);
        }

        if (manager != null) {
            if (manager instanceof Bee7GameWallManagerV2) {
                ((Bee7GameWallManagerV2) manager).onBannerNotificationVisibilityChanged(false);
            }
        }

        return null;
    }

    /**
     * @return true if banner notification is shown, false otherwise
     */
    @Override
    public boolean isBannerNotificationShown() {
        if (notificationQueue != null) {
            return notificationQueue.isBannerNotificationShown();
        }
        return false;
    }

    /**
     * Closes any shown banner notification and dismisses ant pending ones
     */
    @Override
    public void closeBannerNotification() {
        if (notificationQueue != null) {
            notificationQueue.closeBannerNotification();
        }
    }

    /**
     * Call when players virtual currency amount is low.
     * Will trigger GamewallBannerNotificationInterface.OnPendingBannerNotification if
     */
    @Override
    public void setVirtualCurrencyState(boolean lowCurrency) {
        if (notificationQueue != null) {
            notificationQueue.setVirtualCurrencyState(lowCurrency);
        }
    }

    /**
     * If set to true this will enable bee7 notification banners and show any pending ones,
     * if set to false, notification banners will be disabled and any shown banners hidden.
     * @param enableNotifications
     */
    @Override
    public void toggleNotificationShowing(boolean enableNotifications) {
        if (notificationQueue != null) {
            notificationQueue.toggleNotificationShowing(enableNotifications);
        }
    }

    public boolean saveAppStartTimestamp() {
        if (mPublisher != null) {
            return mPublisher.saveAppStartTimestamp();
        }
        return false;
    }

    public void saveAppCloseTimestamp() {
        if (mPublisher != null) {
            mPublisher.saveAppCLoseTimestamp();
        }
    }

    @Override
    public void toggleNotificationShowingOnGameWall(boolean notificationShowing) {
        if (notificationQueue != null) {
            notificationQueue.toggleNotificationShowingOnGameWall(notificationShowing);
        }
    }

    private void addNotificationToQueue(Reward reward) {
        if (mPublisher != null && !reward.isVideoReward()) { //check if should display banner notification instead of dialog bubble, or if it is video reward
            PublisherConfiguration.BannerNotificationConfig bannerNotification = mPublisher.getBannerNotificationConfig();

            if (bannerNotification != null && bannerNotification.isEnabled() && notificationQueue != null) {
                notificationQueue.addRewardNotificationBannerToQueue(
                        getRewardAmountString(reward),
                        getAppIcon(reward),
                        getPublisherDrawable(),
                        reward);

                return;
            } else {
                Logger.debug(TAG, "Cant show notification banner; bannerNotification is null or bannerNotification is not disabled or notificationQueue is null");
            }
        }
    }

    public void showDebugTools() {
        if (com.bee7.sdk.common.util.Utils.isDevBackendEnabled(context)) {
            ExternalDebugToolsDialog toolsDialog = new ExternalDebugToolsDialog(context, mPublisher);
            toolsDialog.setTitle("Bee7 Debug tools");
            toolsDialog.show();
        }
    }
}
