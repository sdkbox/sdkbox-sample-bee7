package com.bee7.gamewall;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

import com.bee7.gamewall.assets.AnimFactory;
import com.bee7.gamewall.assets.AssetsManager;
import com.bee7.gamewall.assets.UnscaledBitmapLoader;
import com.bee7.gamewall.dialogs.DialogNoInternet;
import com.bee7.gamewall.dialogs.DialogRedirecting;
import com.bee7.gamewall.interfaces.Bee7GameWallManager;
import com.bee7.gamewall.interfaces.OnVideoRewardGeneratedListener;
import com.bee7.sdk.common.OnEnableChangeListener;
import com.bee7.sdk.common.OnReportingIdChangeListener;
import com.bee7.sdk.common.Reward;
import com.bee7.sdk.common.RewardCollection;
import com.bee7.sdk.common.task.TaskFeedback;
import com.bee7.sdk.common.util.*;
import com.bee7.sdk.publisher.DefaultPublisher;
import com.bee7.sdk.publisher.GameWallConfiguration;
import com.bee7.sdk.publisher.Publisher;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResult;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;
import com.bee7.sdk.publisher.appoffer.AppOffersModelEvent;
import com.bee7.sdk.publisher.appoffer.AppOffersModelListener;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameWallImpl  implements AppOffersModelListener, NotifyReward.DisplayedChangeListener,
        OnVideoRewardGeneratedListener {
    static final String TAG = GameWallImpl.class.getSimpleName();

    private static String BEE7_API_KEY;
    private static String BEE7_VENDOR_ID;

    private static boolean minigameStarted = false;

    private Context context;
    private Bee7GameWallManager manager;

    protected DefaultPublisher mPublisher;
    protected RewardQueue mRewardQueue;
    private GameWallView mGameWallView = null;
    private static Activity mActivity;
    private Uri mClaimData;

    private boolean mShown;
    private boolean mPendingGWBtnImpression = false;
    private Reward fullScreenVideoWatchedReward;
    private SharedPreferencesRewardsHelper sharedPreferencesRewardsHelper;
    private List<AppOffer> innerApps;
    private Object innerAppsLock = new Object();

    boolean animate = false; //TODO make configurable

    public GameWallImpl(Context ctx, final Bee7GameWallManager manager, List<AppOffer> miniGames)
    {
        this.init(ctx, manager, null, null, miniGames);
    }

	public GameWallImpl(Context ctx, final Bee7GameWallManager manager, String apiKey) {
        this.init(ctx, manager, apiKey, null, null);
    }
    
    public GameWallImpl(Context ctx, final Bee7GameWallManager manager, String apiKey, String vendorId, List<AppOffer> miniGames) {
        this.init(ctx, manager, apiKey, vendorId, miniGames);
    }

    private void init(Context ctx, final Bee7GameWallManager manager, String apiKey, String vendorId, List<AppOffer> miniGames)
    {
        this.context = ctx;
        this.manager = manager;

        BEE7_API_KEY = apiKey;

        BEE7_VENDOR_ID = vendorId;

        innerApps = new ArrayList<AppOffer>();

        if (miniGames != null && !miniGames.isEmpty()) {
            innerApps.addAll(miniGames);
        }

        mRewardQueue = new RewardQueue();

        mPublisher = new DefaultPublisher();

        mPublisher.disableProgressIndicator();

        mPublisher.setContext(context);
        mPublisher.setApiKey(BEE7_API_KEY);
        mPublisher.setTestVendorId(BEE7_VENDOR_ID);
        mPublisher.setProxyEnabled(true);

        mPublisher.setOnEnableChangeListener(new OnEnableChangeListener() {
            @Override
            public void onEnableChange(boolean enabled) {
                if (mPendingGWBtnImpression) {
                    mPendingGWBtnImpression = false;

                    if (enabled) {
                        mPublisher.onGameWallButtonImpression();
                    }
                }

                if (manager != null) {
                    manager.onAvailableChange(enabled && mPublisher.getAppOffersModel().hasAnyAppOffers());
                }
            }
        });

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
                        manager.onAvailableChange(false);
                    }
                }

                @Override
                public void onResults(Boolean result) {

                }

                @Override
                public void onFinish(Boolean result) {
                    Logger.debug(TAG, "Started - enabled=" + result);

                    if (manager != null && !result) {
                        manager.onAvailableChange(false);
                    }

                    int maxDailyRewardFreq = 1;

                    if (mPublisher.isEnabled()) {
                        maxDailyRewardFreq = mPublisher.getAppOffersModel().getVideoPrequalGlobalConfig().getMaxDailyRewardFreq();
                    }

                    sharedPreferencesRewardsHelper = new SharedPreferencesRewardsHelper(context, maxDailyRewardFreq);

                    tryClaim();
                }

                @Override
                public void onError(Exception e) {
                    Logger.debug(TAG, "Error starting: {0}", e.toString());

                    if (manager != null) {
                        manager.onAvailableChange(false);
                    }
                }
            });
        }
    }

    public void setAgeGate(boolean hasPassed) {
        if (mPublisher != null) {
            mPublisher.setAgeGate(hasPassed);
        }
    }

    public void setTestVariant(String testId) {
        if (mPublisher != null) {
            mPublisher.setTestVariant(testId);
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
                mPublisher.getAppOffersModel().getGameWallConfiguration().getRedirectingTimeout());
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

                for (Reward reward : result) {
                    if (addReward(reward)) {
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
    public void show(final Activity activity) {
        Logger.info(TAG, "show()");
        mActivity = activity;

        if (mGameWallView == null) {
            mGameWallView = (GameWallView) activity.getLayoutInflater().inflate(R.layout.gamewall_view, null);

            //we intercept any touch events so they do not get send to the underneath view
            mGameWallView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Logger.debug(TAG, "onTouch");
                    return true;
                }
            });

            mGameWallView.findViewById(R.id.gamewallHeaderButtonClose).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

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
            });

            mPublisher.getAppOffersModel().addAppOffersModelListener(this);
        }

        try {
            if (minigameStarted) {
                minigameStarted = false;

                mGameWallView.setVisibility(View.VISIBLE);
            } else {
                mGameWallView.init(mPublisher, this);

                mGameWallView.getGamesScrollView().fullScroll(View.FOCUS_UP);

                ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
                        animate) {
                    mGameWallView.setVisibility(View.INVISIBLE);
                    activity.getWindow().addContentView(mGameWallView, lp);

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
                    activity.getWindow().addContentView(mGameWallView, lp);
                    mGameWallView.viewShown();
                    checkForOffers();
                }
            }

            mShown = true;

            mPublisher.onGameWallImpression();

            if (manager != null) {
            	manager.onVisibleChange(true, true);
            }
        } catch (Exception e) {
            Logger.error(TAG, e, "{0}", e.getMessage());
        }
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
        if (reward.getVirtualCurrencyAmount() > 0) {

            NotifyReward msg = new NotifyReward(activity, this);

            String amountStr = String.format("%+,d", reward.getVirtualCurrencyAmount());

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

            Drawable bitmapPublisherIcon = null;
            try {
                bitmapPublisherIcon = activity.getPackageManager().getApplicationIcon(activity.getApplicationInfo());
            } catch (Exception ignore) { }

            msg.addMsg(amountStr, bm,
                    activity.getResources().getDrawable(R.drawable.bee7_icon_reward),
                    bitmapPublisherIcon, reward.isVideoReward());

            msg.queueOnStoppedQueue = true;

            mRewardQueue.addMessage(msg);

            mRewardQueue.startProcessing(activity);

            return true;
        } else {
            Logger.debug(TAG, "Reward with low VC amount: {0}", reward.toString());

            return false;
        }
    }

    public void onGameWallButtonImpression() {
        if (mPublisher != null && mPublisher.isEnabled()) {
            mPublisher.onGameWallButtonImpression();
        } else {
            mPendingGWBtnImpression = true;
        }
    }

    private static void showNoConnectionDialog(Context context) {
        new DialogNoInternet(context).show();
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
        }

        // Claim reward if available
        tryClaim();

        if (mGameWallView != null && mShown) {
            mGameWallView.onResume();

            //we check if any video is active
            if (mGameWallView.findViewWithVideoView() == null &&
                    mGameWallView.getVideoViewDialog() == null)
            checkForOffers();
        }
    }

    /**
     * Called from activity onPause
     */
    public void pause() {
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
        if (mGameWallView != null && mShown) {
            mGameWallView.onDestroy();
        }
    }

    /**
     * Called from the onConfigurationChanged event in order to update view in case orientation
     * was changed
     */
    public void updateView() {
        Logger.info(TAG, "updateView()");
        if (mShown) {
            checkForOffers();
        }
    }

    /**
     * Called from the onBackPressed event, in order to dismiss the game wall view
     */
    public void hide() {
        if (mGameWallView != null) {
            mGameWallView.disableClickEvents = false;
        }

        try {
            if (mPublisher != null && mPublisher.getAppOffersModel() != null) {
                mPublisher.getAppOffersModel().removeAppOffersModelListener(this);
            }

            mShown = false;

            if (mActivity != null) {
                if (mPublisher != null) {
                    mPublisher.onGameWallCloseImpression();
                }

                if (minigameStarted && mGameWallView != null) {
                    mGameWallView.setVisibility(View.GONE);
                } else {
                    final ViewGroup rootView = (ViewGroup) mActivity.findViewById(android.R.id.content);

                    if (rootView != null && mGameWallView != null) {
                        if (mGameWallView.getVideoViewDialog() != null) {
                            mGameWallView.getVideoViewDialog().hide(true);
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
                            }
                        }
                    }
                }

                mActivity = null;
                
                if (manager != null) {
                	manager.onVisibleChange(false, true);
                }
            }
        } catch (Exception e) {
            Logger.error(TAG, e, "hide failed");
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
            } else if (mShown && mGameWallView.getVideoViewDialog() != null) {
                mGameWallView.getVideoViewDialog().hide(false);
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
        Logger.info(TAG, "onAppOffersChange() ");

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

    /**
     * refreshes offers and updates ListView and GridView accordingly
     */
    public void checkForOffers() {
        Logger.info(TAG, "checkForOffers()");
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
        if (sharedPreferencesRewardsHelper != null && !sharedPreferencesRewardsHelper.hasBeenRewardAlreadyGiven(appOffer.getId(), appOffer.getCampaignId())) {
            Reward reward = mPublisher.generateVideoReward(appOffer);
            if (manager != null && reward != null) {
                sharedPreferencesRewardsHelper.saveGivenRewardKey(appOffer.getId(), appOffer.getCampaignId());
                manager.onGiveReward(reward);
            }
        }

        mGameWallView.updateGameWallUnit(appOffer);
    }
}
