package com.bee7.gamewall;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import com.bee7.gamewall.assets.AnimFactory;
import com.bee7.gamewall.dialogs.DialogDebug;
import com.bee7.gamewall.dialogs.DialogNoInternet;
import com.bee7.gamewall.dialogs.DialogTutorial;
import com.bee7.gamewall.interfaces.BannerNotificationGameWallInterface;
import com.bee7.gamewall.interfaces.Bee7GameWallManager;
import com.bee7.gamewall.interfaces.OnLayout;
import com.bee7.gamewall.interfaces.OnOfferClickListener;
import com.bee7.gamewall.interfaces.OnVideoClickListener;
import com.bee7.gamewall.interfaces.OnVideoRewardGeneratedListener;
import com.bee7.gamewall.interfaces.OnVideoWithRewardPlayingListener;
import com.bee7.gamewall.tasks.GameWallTaskWorker;
import com.bee7.gamewall.tasks.GenerateGameWallUnitAsyncTask;
import com.bee7.gamewall.tasks.GenerateGameWallUnitListHolderAsyncTask;
import com.bee7.gamewall.video.VideoComponent;
import com.bee7.gamewall.video.VideoDialog;
import com.bee7.sdk.adunit.exoplayer.ExoVideoPlayer;
import com.bee7.gamewall.views.Bee7FrameLayout;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.common.util.SharedPreferencesHistoryHelper;
import com.bee7.sdk.common.util.SharedPreferencesRewardsHelper;
import com.bee7.sdk.publisher.GameWallConfiguration;
import com.bee7.sdk.publisher.Publisher;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResult;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


public class GameWallView extends RelativeLayout implements OnVideoWithRewardPlayingListener {

    public static Object lastClickSync = new Object();
    public static long lastClickTimestamp = 0;

    private static final String TAG = GameWallView.class.getName();

    private ScrollView gamesScrollView;
    private LinearLayout gamesColumn1;
    private Bee7FrameLayout gameWallHeaderFrameLayout;
    private Button debugButton;

    private int column1 = 1;

    private Handler handler;
    private Publisher publisher;
    private GameWallConfiguration gameWallConfiguration;
    private List<String> impressionOffersAlreadySent;
    private OnVideoRewardGeneratedListener onVideoRewardGeneratedListener;
    private BannerNotificationGameWallInterface bannerNotificationInterface;
    private DialogDebug.DialogDebugInterface dialogDebugInterface;
    private Bee7GameWallManager manager;

    private VideoDialog videoDialog;
    private DialogTutorial dialogTutorial;

    private GameWallTaskWorker worker;

    public boolean disableClickEvents = false; //TODO temporary fix for BSDK-282: Dialogs displaying in mini games
    private boolean immersiveMode = false;

    /**
     * With this flags we indicate if a offer list is first of a group in column
     */
    private boolean firstOfferListInColumn1Group;

    public GameWallView(Context context) {
        super(context);
    }

    public GameWallView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void init(Publisher publisher, OnVideoRewardGeneratedListener onVideoRewardGeneratedListener,
                     boolean immersiveMode, BannerNotificationGameWallInterface bannerNotificationInterface,
                     Bee7GameWallManager manager,
                     DialogDebug.DialogDebugInterface dialogDebugInterface) {
        this.publisher = publisher;
        this.gameWallConfiguration = publisher.getAppOffersModel().getGameWallConfiguration();
        this.onVideoRewardGeneratedListener = onVideoRewardGeneratedListener;
        this.bannerNotificationInterface = bannerNotificationInterface;
        this.immersiveMode = immersiveMode;
        this.dialogDebugInterface = dialogDebugInterface;
        this.manager = manager;

        impressionOffersAlreadySent = new ArrayList<String>();

        this.worker = GameWallTaskWorker.getInstance();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        handler = new Handler();

        gamesScrollView = (ScrollView) findViewById(R.id.gamewallScrollView);
        gamesColumn1 = (LinearLayout) findViewById(R.id.gamewallLinearLayout);
        debugButton = (Button) findViewById(R.id.debugButton);

        gameWallHeaderFrameLayout = (Bee7FrameLayout)findViewById(R.id.gamewall_header);
        gameWallHeaderFrameLayout.setOnLayout(new OnLayout() {
            @Override
            public void onHeight(int height) {
                Logger.debug(TAG, "GameWallHeaderFrameLayout onHeight {0}", height);
                onHeaderHeightChanged(height);
            }

            @Override
            public void onWidth(int width) {
                Logger.debug(TAG, "GameWallHeaderFrameLayout onWidth {0}", width);
            }
        });

        gamesScrollView.post(new Runnable() {
            @Override
            public void run() {
                //calculateShownOffers();

                if (gameWallConfiguration.isShowHeaderVC()) {
                    findViewById(R.id.gamewall_header).setVisibility(VISIBLE);
                } else {
                    findViewById(R.id.gamewall_header).setVisibility(GONE);
                }
                if (gameWallConfiguration.isShowFooter()) {
                    View footer = findViewById(R.id.gamewall_footer);
                    footer.setVisibility(VISIBLE);

                    ImageView logo = (ImageView) footer.findViewById(R.id.bee7_gamewall_footer_logo_icon);
                    logo.setColorFilter(getResources().getColor(R.color.bee7_footer_text_color), PorterDuff.Mode.SRC_ATOP);
                } else {
                    findViewById(R.id.gamewall_footer).setVisibility(GONE);
                }

                if (gameWallConfiguration.isTutorialEnabled()) {
                    dialogTutorial = new DialogTutorial(getContext(), immersiveMode);
                    dialogTutorial.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(DialogInterface dialog) {
                            if (bannerNotificationInterface != null) {
                                bannerNotificationInterface.toggleNotificationShowingOnGameWall(false);
                            }
                            disableClickEvents = true;
                        }
                    });
                    dialogTutorial.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if (bannerNotificationInterface != null) {
                                bannerNotificationInterface.toggleNotificationShowingOnGameWall(true);
                            }
                            disableClickEvents = false;
                        }
                    });
                    if (dialogTutorial.isDialogAllowedToShow()) {
                        dialogTutorial.show();
                        disableClickEvents = true;
                    } else {
                        disableClickEvents = false;
                    }
                } else {
                    disableClickEvents = false;
                }
            }
        });

        gamesScrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                calculateShownOffers();
            }
        });

        if (com.bee7.sdk.common.util.Utils.isDevBackendEnabled(getContext())) {
            debugButton.setVisibility(VISIBLE);
            debugButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    new DialogDebug(getContext(), publisher, onVideoRewardGeneratedListener, manager,
                            dialogDebugInterface).show();
                }
            });
        } else {
            debugButton.setVisibility(GONE);
        }
    }

    private int offersPosition;
    private int connectedPosition;
    private int tempUnitViewsCount1;
    private int appsCount;

    /**
     *  This method takes offers, installed/connected offers, layout configuration map and generate
     *  appropriate views and adds them to layout.
     *
     * @param appsNotInstalled: Apps that are not installed (offers).
     * @param appsInstalled: Apps that are installed/connected and provided minigames.
     * @param layoutTypeListMap: Map of lists of game wall unit items.
     */
    public void updateGameWallView(
            List<AppOffer> appsNotInstalled,
            List<AppOffer> appsInstalled,
            Map<GameWallConfiguration.LayoutType, List<GameWallConfiguration.UnitType>> layoutTypeListMap) {
        Logger.debug(TAG, "Updating Game Wall view: appsNotInstalled: " + Utils.convertToSimpleNamesListAppOffer(appsNotInstalled) + ", appsInstalled: " + Utils.convertToSimpleNamesListAppOffer(appsInstalled));
        //TODO implement some sort of view recycling

        worker.removeAllCallbacks();
        if (gamesColumn1 != null) {
            removeOfferViews();
        }

        int numberOfItemsInGwUnitListHolder = Utils.getNumberOfItemsInGwUnitListHolder(getContext());
        Logger.debug(TAG, "Number of items in gw unit list holder: " + numberOfItemsInGwUnitListHolder);

        // this flags indicates if we shoud display shadow on top of list unit
        firstOfferListInColumn1Group = true;

        // Sorting based on play history
        SharedPreferencesHistoryHelper.clearHistory(getContext(), appsInstalled);
        Collections.sort(appsInstalled, new Comparator<AppOffer>() {
            @Override
            public int compare(AppOffer lhs, AppOffer rhs) {
                if (lhs.getLastPlayedTimestamp(getContext()) > rhs.getLastPlayedTimestamp(getContext())) {
                    return -1;
                } else if (lhs.getLastPlayedTimestamp(getContext()) < rhs.getLastPlayedTimestamp(getContext())) {
                    return 1;
                }
                return 0;
            }
        });

        // we reset global trackers
        // so we can keep track of indexes
        offersPosition = 0;
        connectedPosition = 0;
        tempUnitViewsCount1 = 0;
        appsCount = 0;

        //portrait
        if (Utils.isPortrate(getContext()) && layoutTypeListMap != null) {
            List<GameWallConfiguration.UnitType> unitTypes = layoutTypeListMap.get(GameWallConfiguration.LayoutType.PORTRAIT);
            Logger.debug(TAG, "Loop trough all unitTypes: " + Utils.convertToSimpleNamesListUnitType(unitTypes) );
            if (unitTypes != null) {
                for (GameWallConfiguration.UnitType offerType : unitTypes) {
                    insertGameWallUnit(offerType, appsNotInstalled, appsInstalled, column1, GameWallConfiguration.LayoutType.PORTRAIT, numberOfItemsInGwUnitListHolder);
                }
            }
        //landscape
        } else if (!Utils.isPortrate(getContext()) &&
                layoutTypeListMap != null &&
                layoutTypeListMap.containsKey(GameWallConfiguration.LayoutType.LANDSCAPE_LEFT)) {

            List<GameWallConfiguration.UnitType> unitTypesLeft = layoutTypeListMap.get(GameWallConfiguration.LayoutType.LANDSCAPE_LEFT);

            int unitListMaxLength = unitTypesLeft.size();
            for (int f = 0; f < unitListMaxLength; f++) {
                //take one from left
                if (f < unitTypesLeft.size()) {
                    insertGameWallUnit(unitTypesLeft.get(f), appsNotInstalled, appsInstalled, column1, GameWallConfiguration.LayoutType.LANDSCAPE_LEFT, numberOfItemsInGwUnitListHolder);
                }
            }
        }
        //If we have some connected games left we add it at the bottom
        addRemaningConnectedOffers(appsInstalled, appsNotInstalled, numberOfItemsInGwUnitListHolder);
    }

    /**
     * Insert GameWall unit based on what type it is, in which column should be added and what offers should it contains
     */
    private void insertGameWallUnit(GameWallConfiguration.UnitType offerType,
                                    List<AppOffer> appsNotInstalled,
                                    List<AppOffer> appsInstalled,
                                    int column,
                                    GameWallConfiguration.LayoutType layoutType,
                                    int numberOfItemsInGwUnitListHolder)
    {
        Logger.debug(TAG, "Inserting game wall unit: offerType " + offerType + ", column " + column+ ", layoutType " + layoutType);

        if (offerType == GameWallConfiguration.UnitType.OFFER_BANNER) {
            if (offersPosition < appsNotInstalled.size()) {
                if (column == column1) {
                    createOfferBanner(appsNotInstalled.get(offersPosition), offerType, tempUnitViewsCount1, column1, layoutType);
                    firstOfferListInColumn1Group = true;
                    tempUnitViewsCount1++;
                    offersPosition++;
                }
            } else {
                Logger.debug(TAG, "We ran out of offers for banner.");
            }
        } else if (offerType == GameWallConfiguration.UnitType.CONNECTED_BANNER) {
            if (connectedPosition < appsInstalled.size()) {
                if (column == column1) {
                    createOfferBanner(appsInstalled.get(connectedPosition), offerType, -1, column1, layoutType);
                    firstOfferListInColumn1Group = true;
                    tempUnitViewsCount1++;
                    connectedPosition++;
                }
            } else {
                Logger.debug(TAG, "We ran out of connected offers for banner.");
            }
        } else if (offerType == GameWallConfiguration.UnitType.OFFER_LIST) {

            if (offersPosition < appsNotInstalled.size()) {

                //we create a list of numberOfItemsInGwUnitListHolder offers
                List<GameWallUnitOfferList.OfferTypePair> appOffers = new ArrayList<GameWallUnitOfferList.OfferTypePair>();
                for (int k = offersPosition + numberOfItemsInGwUnitListHolder ; offersPosition < k ; offersPosition++) {
                    if (offersPosition < appsNotInstalled.size()) {
                        appOffers.add(new GameWallUnitOfferList.OfferTypePair(appsNotInstalled.get(offersPosition), GameWallConfiguration.UnitType.OFFER_LIST));
                    } else {
                        break;
                    }
                }

                //if we have less than numberOfItemsInGwUnitListHolder items we try to add next connected games/mini games
                if (appOffers.size() < numberOfItemsInGwUnitListHolder) {
                    int emptySlots = numberOfItemsInGwUnitListHolder - appOffers.size();
                    int maxConnectedPosition = connectedPosition + emptySlots;
                    for (int k = connectedPosition ; k < maxConnectedPosition ; k++) {
                        if (appsInstalled.size() > connectedPosition) {
                            appOffers.add(new GameWallUnitOfferList.OfferTypePair(appsInstalled.get(connectedPosition), GameWallConfiguration.UnitType.CONNECTED_LIST));
                            connectedPosition++;
                        } else {
                            break;
                        }
                    }
                }

                //Then we add them to the correct column
                if (column == column1) {
                    createOfferList(appOffers, offerType, tempUnitViewsCount1, column1, firstOfferListInColumn1Group, layoutType);
                    firstOfferListInColumn1Group = false;
                    tempUnitViewsCount1++;
                }
            } else {
                Logger.debug(TAG, "We ran out of offers for list.");
            }

        } else if (offerType == GameWallConfiguration.UnitType.CONNECTED_LIST) {

            if (connectedPosition < appsInstalled.size()) {

                //we create a list of numberOfItemsInGwUnitListHolder connected offers/mini games
                List<GameWallUnitOfferList.OfferTypePair> appOffers = new ArrayList<GameWallUnitOfferList.OfferTypePair>();
                for (int k = connectedPosition + numberOfItemsInGwUnitListHolder ; connectedPosition < k ; connectedPosition++) {
                    if (connectedPosition < appsInstalled.size()) {
                        appOffers.add(new GameWallUnitOfferList.OfferTypePair(appsInstalled.get(connectedPosition), GameWallConfiguration.UnitType.CONNECTED_LIST));
                    } else {
                        break;
                    }
                }

                //if we have less than numberOfItemsInGwUnitListHolder items we try to add next offer
                if (appOffers.size() < numberOfItemsInGwUnitListHolder) {
                    int emptySlots = numberOfItemsInGwUnitListHolder - appOffers.size();
                    int maxOffersPosition = offersPosition + emptySlots;
                    for (int k = offersPosition ; k < maxOffersPosition ; k++) {
                        if (appsNotInstalled.size() > offersPosition) {
                            appOffers.add(new GameWallUnitOfferList.OfferTypePair(appsNotInstalled.get(offersPosition), GameWallConfiguration.UnitType.OFFER_LIST));
                            offersPosition++;
                        } else {
                            break;
                        }
                    }
                }

                //Then we add them to the correct column
                if (column == column1) {
                    createOfferList(appOffers, offerType, tempUnitViewsCount1, column1, firstOfferListInColumn1Group, layoutType);
                    firstOfferListInColumn1Group = false;
                    tempUnitViewsCount1++;
                }
            } else {
                Logger.debug(TAG, "We ran out of connected offers for list.");
            }

        }
    }

    /**
     * This method adds remaining connected offers and minigames to the end of list.
     *
     * @param appsInstalled
     */
    private void addRemaningConnectedOffers(List<AppOffer> appsInstalled, List<AppOffer> appsNotInstalled,
                                            int numberOfItemsInGwUnitListHolder) {
        Logger.debug(TAG, "Add Remaining Connected Offers");
        //if we don't get layout configuration
        if (Utils.isPortrate(getContext())) {
            //fill installed games for portrait
            for (int i = connectedPosition ; i < appsInstalled.size() ; i++) {
                List<GameWallUnitOfferList.OfferTypePair> appOffers = new ArrayList<GameWallUnitOfferList.OfferTypePair>();
                int temp = i + numberOfItemsInGwUnitListHolder;
                for (int k = i ; k < temp ; k++) {
                    if (k < appsInstalled.size()) {
                        appOffers.add(new GameWallUnitOfferList.OfferTypePair(appsInstalled.get(k), GameWallConfiguration.UnitType.CONNECTED_LIST));
                    }
                }
                i += numberOfItemsInGwUnitListHolder - 1;

                //if we have less than numberOfItemsInGwUnitListHolder items we try to add next offer
                if (appOffers.size() < numberOfItemsInGwUnitListHolder) {
                    int emptySlots = numberOfItemsInGwUnitListHolder - appOffers.size();
                    int maxOffersPosition = offersPosition + emptySlots;
                    for (int k = offersPosition ; k < maxOffersPosition ; k++) {
                        if (appsNotInstalled.size() > offersPosition) {
                            appOffers.add(new GameWallUnitOfferList.OfferTypePair(appsNotInstalled.get(offersPosition), GameWallConfiguration.UnitType.OFFER_LIST));
                            offersPosition++;
                        } else {
                            break;
                        }
                    }
                }

                createOfferList(appOffers, GameWallConfiguration.UnitType.CONNECTED_LIST, tempUnitViewsCount1, column1, firstOfferListInColumn1Group, GameWallConfiguration.LayoutType.PORTRAIT);
                firstOfferListInColumn1Group = false;
                tempUnitViewsCount1++;
            }
        } else {
            //right column is bigger
            for (int i = connectedPosition ; i < appsInstalled.size() ; i++) {
                List<GameWallUnitOfferList.OfferTypePair> appOffers = new ArrayList<GameWallUnitOfferList.OfferTypePair>();
                int temp = i + numberOfItemsInGwUnitListHolder;
                for (int k = i; k < temp; k++) {
                    if (k < appsInstalled.size()) {
                        appOffers.add(new GameWallUnitOfferList.OfferTypePair(appsInstalled.get(k), GameWallConfiguration.UnitType.CONNECTED_LIST));
                    }
                }
                i += numberOfItemsInGwUnitListHolder - 1;

                //if we have less than numberOfItemsInGwUnitListHolder items we try to add next offer
                if (appOffers.size() < numberOfItemsInGwUnitListHolder) {
                    int emptySlots = numberOfItemsInGwUnitListHolder - appOffers.size();
                    int maxOffersPosition = offersPosition + emptySlots;
                    for (int k = offersPosition ; k < maxOffersPosition ; k++) {
                        if (appsNotInstalled.size() > offersPosition) {
                            appOffers.add(new GameWallUnitOfferList.OfferTypePair(appsNotInstalled.get(offersPosition), GameWallConfiguration.UnitType.OFFER_LIST));
                            offersPosition++;
                        } else {
                            break;
                        }
                    }
                }

                createOfferList(appOffers, GameWallConfiguration.UnitType.CONNECTED_LIST, tempUnitViewsCount1, column1, firstOfferListInColumn1Group, GameWallConfiguration.LayoutType.LANDSCAPE_RIGHT);
                firstOfferListInColumn1Group = false;
                connectedPosition += numberOfItemsInGwUnitListHolder;
                tempUnitViewsCount1++;
            }

            //finish filling remaining installed offers
            for (int i = connectedPosition ; i < appsInstalled.size() ; i++) {
                Logger.debug(TAG, "Finish with adding Remaining Installed Offers");

                List<GameWallUnitOfferList.OfferTypePair> appOffers = new ArrayList<GameWallUnitOfferList.OfferTypePair>();
                int temp = i + numberOfItemsInGwUnitListHolder;
                for (int k = i ; k < temp ; k++) {
                    if (k < appsInstalled.size()) {
                        appOffers.add(new GameWallUnitOfferList.OfferTypePair(appsInstalled.get(k), GameWallConfiguration.UnitType.CONNECTED_LIST));
                    }
                }
                i += numberOfItemsInGwUnitListHolder + 2;
                createOfferList(appOffers, GameWallConfiguration.UnitType.CONNECTED_LIST, tempUnitViewsCount1, column1, firstOfferListInColumn1Group, GameWallConfiguration.LayoutType.LANDSCAPE_LEFT);
                firstOfferListInColumn1Group = false;
                tempUnitViewsCount1++;
            }
        }
    }

    /**
     * Creates and asynchronously add offer list to correct column
     * @param appOffers
     * @param offerType
     * @param index
     * @param column
     */
    private void createOfferList(final List<GameWallUnitOfferList.OfferTypePair> appOffers,
                                 GameWallConfiguration.UnitType offerType, int index, int column,
                                 boolean firstInColumnGroup, GameWallConfiguration.LayoutType layoutType)
    {
        Logger.debug(TAG, "Creating Offer List offerType: " + offerType + ", row: " + index + ", column " + column + ", layoutType: " + layoutType + ", " + Utils.convertToSimpleNames(appOffers));
        final int offersCnt = appOffers.size();

        GenerateGameWallUnitListHolderAsyncTask generateGameWallUnitListHolderAsyncTask = new GenerateGameWallUnitListHolderAsyncTask(
                getContext(),
                appOffers,
                new OnOfferClickListener(){
                    @Override
                    public void onOfferClick(AppOffer appOffer, AppOfferWithResult appOfferWithResult, boolean afterVideo, Publisher.AppOfferStartOrigin origin) {
                        if (GameWallView.this.isShown() && !disableClickEvents) {
                            // for mini games disable all future clicks
                            // clicks will be enabled again when show is called
                            if (appOffer.isInnerApp()) {
                                disableClickEvents = true;
                            }

                            onPause();
                            GameWallImpl.startAppOffer(appOffer, appOfferWithResult, getContext(), publisher, origin);
                        }
                    }
                },
                new OnVideoClickListener() {
                    @Override
                    public void onVideoClick(AppOffer appOffer, AppOfferWithResult appOfferWithResult) {
                        if (GameWallView.this.isShown() && !disableClickEvents) {
                            if (!com.bee7.sdk.common.util.Utils.isOnline(getContext())) {
                                new DialogNoInternet(getContext(), immersiveMode).show();
                                return;
                            }

                            GameWallUnitOfferBanner gwUnitOfferBanner = findViewWithVideoView();
                            if (gwUnitOfferBanner != null) {
                                gwUnitOfferBanner.removeVideoView(null, false, null, false);
                            }
                            AppOffersModel.VideoPrequalType videoType = publisher.getAppOffersModel().getVideoPrequaificationlType();
                            showVideoDialog(appOffer, appOfferWithResult, 0, false, videoType);
                        }
                    }
                },
                publisher.getAppOffersModel().getVideoButtonPosition(),
                publisher.getAppOffersModel().getVideoPrequaificationlType(),
                publisher.getAppOffersModel().getVideoPrequalGlobalConfig().getMaxDailyRewardFreq(),
                offerType,
                index,
                column,
                firstInColumnGroup,
                publisher.getExchangeRate(),
                layoutType,
                immersiveMode);

        generateGameWallUnitListHolderAsyncTask.setOnGameWallUnitListHolderGeneratedListener(new GenerateGameWallUnitListHolderAsyncTask.OnGameWallUnitListHolderGeneratedListener() {
            @Override
            public void OnGameWallUnitListHolderGenerated(View gameWallUnitListHolder, LinearLayout.LayoutParams layoutParams, int layoutIndex, int column) {
                Logger.debug(TAG, "Adding View To Column: layoutParams: " + layoutParams.width + "," + layoutParams.height + ", layoutIndex " + layoutIndex + ", column: " + column + Utils.convertToSimpleNames(appOffers));
                addViewToColumn(gameWallUnitListHolder, layoutParams, layoutIndex, column);

                appsCount += offersCnt;

                //-1 due to the zero based offers/connected positions
                if (appsCount >= (offersPosition + connectedPosition - 1)) {
                    calculateShownOffers();
                }
            }
        });

        if (this.worker != null) {
            Logger.debug(TAG, "this.worker.postGenerateUnitList(generateGameWallUnitListHolderAsyncTask)");
            this.worker.postGenerateUnitList(generateGameWallUnitListHolderAsyncTask);
        }
    }

    /**
     * Creates and asynchronously add offer banner to correct column.
     * @param appOffer
     * @param unitType
     * @param index
     * @param column
     */
    private void createOfferBanner(final AppOffer appOffer, final GameWallConfiguration.UnitType unitType,
                                   final int index, int column, final GameWallConfiguration.LayoutType layoutType)
    {
        Logger.debug(TAG, "Creating Offer List offerType: " + unitType + ", row: " + index + ", column " + column + ", layoutType: " + layoutType + ", " + appOffer.getLocalizedName());
        GenerateGameWallUnitAsyncTask generateGameWallUnitAsyncTask = new GenerateGameWallUnitAsyncTask(
                getContext(),
                appOffer,
                new OnOfferClickListener(){
                    @Override
                    public void onOfferClick(AppOffer appOffer, AppOfferWithResult appOfferWithResult, boolean afterVideo, Publisher.AppOfferStartOrigin origin) {
                        if (GameWallView.this.isShown() && !disableClickEvents) {
                            // for mini games disable all future clicks
                            // clicks will be enabled again when show is called
                            if (appOffer.isInnerApp()) {
                                disableClickEvents = true;
                            }
                            
                            onPause();
                            GameWallImpl.startAppOffer(appOffer, appOfferWithResult, getContext(), publisher, origin);
                        }
                    }
                },
                new OnVideoClickListener() {
                    @Override
                    public void onVideoClick(AppOffer appOffer, AppOfferWithResult appOfferWithResult) {
                        if (GameWallView.this.isShown() && !disableClickEvents) {
                            GameWallUnitOfferBanner gwUnitOfferBannerWithVideo = findViewWithVideoView();

                            if (gwUnitOfferBannerWithVideo != null) {
                                if (gwUnitOfferBannerWithVideo.getAppOffer(null).getId().equalsIgnoreCase(appOffer.getId())) { //its for the same offer
                                    if (gwUnitOfferBannerWithVideo.icCtaShowing() && !gwUnitOfferBannerWithVideo.isCloseNoticeShowing()) { //if its not playing, restart
                                        gwUnitOfferBannerWithVideo.replayVideo();
                                    }
                                } else { //its for different offer
                                    GameWallUnitOfferBanner gwUnitOfferBanner = findGwUnitOfferBanner(appOffer.getId());
                                    if (gwUnitOfferBanner != null) {
                                        if (gwUnitOfferBannerWithVideo.removeVideoView(
                                                null, true, null, false)) {
                                            showVideoView(appOffer, appOfferWithResult);
                                        }
                                    }
                                }
                            } else { //no other video view is active :)
                                showVideoView(appOffer, appOfferWithResult);
                            }
                        }
                    }
                },
                publisher.getAppOffersModel().getVideoButtonPosition(),
                publisher.getAppOffersModel().getVideoPrequaificationlType(),
                publisher.getAppOffersModel().getVideoPrequalGlobalConfig().getMaxDailyRewardFreq(),
                unitType, index, column,
                publisher.getExchangeRate());

        generateGameWallUnitAsyncTask.setOnGameWallUnitGeneratedListener(new GenerateGameWallUnitAsyncTask.OnGameWallUnitGeneratedListener() {
            @Override
            public void OnGameWallUnitGenerated(GameWallUnitOffer gameWallUnitOffer, LinearLayout.LayoutParams layoutParams, int layoutIndex, int column) {
                // update unit result
                if (gameWallUnitOffer.getAppOfferWithResult(null) != null) {
                    gameWallUnitOffer.getAppOfferWithResult(null).setLayoutType(layoutType);
                }

                Logger.debug(TAG, "Adding View To Column: layoutParams: " + layoutParams.width + "," + layoutParams.height + ", layoutIndex " + layoutIndex + ", column: " + column + ", " + appOffer.getLocalizedName());
                addViewToColumn(gameWallUnitOffer, layoutParams, layoutIndex, column);

                appsCount++;

                //-1 due to the zero based offers/connected positions
                if (appsCount >= (offersPosition + connectedPosition - 1)) {
                    calculateShownOffers();
                }
            }
        });

        if (this.worker != null) {
            this.worker.postGenerateUnit(generateGameWallUnitAsyncTask);
        }
    }

    /**
     * This method adds view to correct column at correct index (or if index to large, at the end of
     * column) using provided layout params.
     *
     * @param view
     * @param layoutParams
     * @param layoutIndex
     * @param column
     */
    private void addViewToColumn(View view, LinearLayout.LayoutParams layoutParams, int layoutIndex, int column) {
        Logger.debug(TAG, "addViewToColumn layoutIndex: " + layoutIndex  + ", column: " + column);
        if (column == 1 && gamesColumn1 != null) {
            if (gamesColumn1.getChildCount() < layoutIndex) {
                layoutIndex = gamesColumn1.getChildCount();
                Logger.debug(TAG, "addViewToColumn layoutIndex is bigger than gamesColumn1 child count, new index " + layoutIndex);
            }
            //disabled animation due to the problems on some devices
            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                Animation animShow = AnimFactory.createAlphaShow(view, true);
                if (animShow != null) {
                    Logger.debug(TAG, "addViewToColumn animation will be used");

                    view.setAlpha(0f);
                    Logger.debug(TAG, "Adding view to gamesColumn1: layoutIndex " + layoutIndex + ", layoutParams " + layoutParams.width + "," + layoutParams.height);
                    gamesColumn1.addView(view, layoutIndex, layoutParams);

                    animShow.setDuration(AnimFactory.ANIMATION_DURATION_SHORT);
                    final long animStart = System.currentTimeMillis();
                    animShow.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            Logger.debug(TAG, "Adding view to gamesColumn1 start animation");
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if ((System.currentTimeMillis() - animStart) > (AnimFactory.ANIMATION_DURATION_SHORT + 150)) {
                                AnimFactory.disableAnimations(getContext());
                            }
                            Logger.debug(TAG, "Adding view to gamesColumn1 end animation");
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) { }
                    });
                    view.startAnimation(animShow);
                } else {
                    Logger.debug(TAG, "Adding view to gamesColumn1: layoutIndex " + layoutIndex + ", layoutParams " + layoutParams.width + "," + layoutParams.height);
                    gamesColumn1.addView(view, layoutIndex, layoutParams);
                }
            } else {
            */
                Logger.debug(TAG, "Adding view to gamesColumn1: layoutIndex " + layoutIndex + ", layoutParams " + layoutParams.width + "," + layoutParams.height);
                gamesColumn1.addView(view, layoutIndex, layoutParams);
            //}
        } else {
            Logger.debug(TAG, "column != 1 or gamesColumn1 == null, no view will be added");
        }
    }

    /**
     * Shows video view in corresponding gamewall unit
     * @param appOffer
     */
    private void showVideoView(AppOffer appOffer, AppOfferWithResult appOfferWithResult) {
        //check if we have internet connection
        if(!com.bee7.sdk.common.util.Utils.isOnline(getContext())) {
            new DialogNoInternet(getContext(), immersiveMode).show();
            return;
        }

        GameWallUnitOfferBanner gwUnitOfferBanner = findGwUnitOfferBanner(appOffer.getId());

        if (gwUnitOfferBanner != null) {
            final AppOffersModel.VideoPrequalType videoType = publisher.getAppOffersModel().getVideoPrequaificationlType();
            if (videoType == AppOffersModel.VideoPrequalType.INLINE_REWARD) {

                gwUnitOfferBanner.addVideoView(publisher, onVideoRewardGeneratedListener);
                smoothScrollToListItem(gwUnitOfferBanner.getColumn(), gwUnitOfferBanner.getIndex());

            } else if (videoType == AppOffersModel.VideoPrequalType.INLINE_NO_REWARD) {

                gwUnitOfferBanner.addVideoView(publisher, onVideoRewardGeneratedListener);
                smoothScrollToListItem(gwUnitOfferBanner.getColumn(), gwUnitOfferBanner.getIndex());

            } else if (videoType == AppOffersModel.VideoPrequalType.FULLSCREEN_REWARD ||
                    videoType == AppOffersModel.VideoPrequalType.FULLSCREEN_NO_REWARD) {
                showVideoDialog(appOffer, appOfferWithResult, 0, false, videoType);
            }
        }
    }

    /**
     * Creates smooth animation to appropriate column index position.
     *
     * @param column
     * @param index
     */
    private void smoothScrollToListItem(int column, int index) {
        if (column < 0 || index < 0) {
            return;
        }
        //calculate height of video view
        int videoViewHeight = AnimFactory.getVideoViewHeight(gamesColumn1, 0);

        //calculate offset for positioning to center
        int centerOffset = (gamesScrollView.getMeasuredHeight()
                - getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_item_banner_height)
                - videoViewHeight) / 2;

        if (Utils.isPortrate(getContext())) {
            centerOffset -= getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_item_banner_height);
        }

        int scrollPosition = 0;
        GameWallUnitOfferBanner gwUnitOfferBanner = findViewWithVideoView();
        if (column == column1) {
            scrollPosition = gamesColumn1.getChildAt(index).getTop() - centerOffset;
            if (gameWallHeaderFrameLayout != null) {
                scrollPosition = scrollPosition - gameWallHeaderFrameLayout.getHeight();
            }

            //If there is any video view shown above view that we are scrolling to, we need to take this into our scroll position
            if (gwUnitOfferBanner != null && gwUnitOfferBanner.getColumn() == column1 && gamesColumn1.indexOfChild(gwUnitOfferBanner) < index ) {
                scrollPosition -= videoViewHeight;
            }
        }

        //animate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            ObjectAnimator animator = ObjectAnimator.ofInt(gamesScrollView, "scrollY", scrollPosition);
            animator.setDuration(AnimFactory.ANIMATION_DURATION_LONG);
            animator.start();
        } else {
            gamesScrollView.smoothScrollTo(0, scrollPosition);
        }

    }

    /**
     * @param view to be inspected
     * @return true if view is 3/4 visible, false otherwise.
     */
    private boolean isViewVisible(View view) {
        Rect scrollBounds = new Rect();
        gamesScrollView.getDrawingRect(scrollBounds);

        float top = view.getTop();
        float bottom = top + view.getHeight();

        //If user sees 3/4 of offer we count is as viewed
        float quarterViewHeight = (bottom - top) / 4;
        top = top + quarterViewHeight;
        bottom = bottom - quarterViewHeight;

        if (scrollBounds.bottom > 0 && scrollBounds.top <= top && scrollBounds.bottom >= bottom) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isVideoViewVisible(View view, float topOffset, float bottomOffest) {
        Rect scrollBounds = new Rect();
        gamesScrollView.getDrawingRect(scrollBounds);

        float top = view.getTop() + topOffset;
        float bottom = top + view.getHeight() - bottomOffest;

        //If user sees 3/4 of offer we count is as viewed
        float quarterViewHeight = (bottom - top) / 4;
        top = top + quarterViewHeight;
        bottom = bottom - quarterViewHeight;

        if (scrollBounds.bottom > 0 && scrollBounds.top <= top && scrollBounds.bottom >= bottom) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Calculates visible offers and reports them to publisher.
     */
    private void calculateShownOffers() {
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //calculate if video view is on screen
                GameWallUnitOfferBanner gameWallUnitOfferBanner = findViewWithVideoView();
                if (gameWallUnitOfferBanner != null && gameWallUnitOfferBanner.getVideoComponent() != null) {

                    VideoComponent videoComponent = gameWallUnitOfferBanner.getVideoComponent();
                    if (isVideoViewVisible(gameWallUnitOfferBanner,
                            getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_item_banner_height),
                            getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_item_banner_height))) { //InWallVideoView is visible
                        if (com.bee7.sdk.common.util.Utils.isOnline(getContext()) &&
                                !videoComponent.isCloseNoticeShown() &&
                                !videoComponent.isCtaShowing()) { //InWallVideoView video not playing
                            videoComponent.onResume();
                        }
                    } else { //InWallVideoView is not visible
                        if (videoComponent.isVideoPlaying()) { //InWallVideoView video playing
                            videoComponent.onPause();
                        }
                    }
                }

                //calculate visible offers
                List<AppOfferWithResult> eventOffers = new ArrayList<AppOfferWithResult>();

                for (int i = 0; i < gamesColumn1.getChildCount(); i++) {
                    if (isViewVisible(gamesColumn1.getChildAt(i))) {
                        View view = gamesColumn1.getChildAt(i);

                        if (view instanceof GameWallUnitOfferBanner) {
                            GameWallUnitOfferBanner gwUnitOfferBanner = (GameWallUnitOfferBanner)view;
                            if (gwUnitOfferBanner.getUnitType() == GameWallConfiguration.UnitType.OFFER_BANNER) {
                                if (!impressionOffersAlreadySent.contains(gwUnitOfferBanner.getAppOffer(null).getId())) {
                                    eventOffers.add(gwUnitOfferBanner.getAppOfferWithResult(null));
                                    impressionOffersAlreadySent.add(gwUnitOfferBanner.getAppOffer(null).getId());
                                    Logger.debug(TAG, "gamesColumn1 calculateShownOffers " + gwUnitOfferBanner.getAppOffer(null).getId());
                                }
                            }
                        } else if (view instanceof GameWallUnitOfferList) {
                            GameWallUnitOfferList gwUnitOfferList = (GameWallUnitOfferList)view;
                            for (AppOffer appOffer : gwUnitOfferList.getAppOffers(GameWallConfiguration.UnitType.OFFER_LIST)) {
                                if (!impressionOffersAlreadySent.contains(appOffer.getId())) {
                                    eventOffers.add(gwUnitOfferList.getAppOfferWithResult(appOffer.getId()));
                                    impressionOffersAlreadySent.add(appOffer.getId());
                                    Logger.debug(TAG, "gamesColumn1 calculateShownOffers " + appOffer.getId());
                                }
                            }
                        }
                    }
                }

                if (eventOffers.size() > 0) {
                    //report all impressed offers
                    publisher.onAppOffersImpression(eventOffers);
                }
            }
        }, 45);
    }

    /**
     * @return gamesScrollView
     */
    public ScrollView getGamesScrollView() {
        return gamesScrollView;
    }

    /**
     * This method creates intent with given parameters and launches video activity
     *
     * @param appOffer
     * @param currentProgress
     * @param videoMuted
     * @param videoPrequalType
     */
    private void showVideoDialog(AppOffer appOffer, AppOfferWithResult appOfferWithResult, long currentProgress, final boolean videoMuted, AppOffersModel.VideoPrequalType videoPrequalType) {
        if (videoDialog != null && videoDialog.isShowing()) {
            return;
        }

        videoDialog = new VideoDialog(getContext(), immersiveMode);

        videoDialog.setup(appOffer, appOfferWithResult, currentProgress, videoMuted, videoPrequalType, publisher, onVideoRewardGeneratedListener, new OnOfferClickListener() {
            @Override
            public void onOfferClick(AppOffer appOffer, AppOfferWithResult appOfferWithResult, boolean afterVideo, Publisher.AppOfferStartOrigin origin) {
                onPause();
                GameWallImpl.startAppOffer(appOffer, appOfferWithResult, getContext(), publisher, origin);
                if (videoDialog != null && videoDialog.isShowing()) {
                    videoDialog.dismiss();
                }
            }
        }, new ExoVideoPlayer.GameWallCallback() {
            @Override
            public void refreshGameWall() {
                updateGameWallUnits();
            }
        });

        videoDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                if (bannerNotificationInterface != null) {
                    bannerNotificationInterface.toggleNotificationShowingOnGameWall(false);
                }
            }
        });
        videoDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (bannerNotificationInterface != null) {
                    bannerNotificationInterface.toggleNotificationShowingOnGameWall(true);
                }
            }
        });

        videoDialog.show();
    }

    /**
     * Called from activity onDestroy
     */
    public void onDestroy() {
        disableClickEvents = false;

        if (this.worker != null) {
            this.worker.stop();
        }
    }

    /**
     * Called from activity onPause
     */
    public void onPause() {

        GameWallUnitOfferBanner offerBanner = findViewWithVideoView();
        if (offerBanner != null && offerBanner.getVideoComponent() != null) {
            offerBanner.removeVideoView(null, false, null, false);
        }

        if (videoDialog != null && videoDialog.isShowing()) {
            videoDialog.hide(true);
        }

        if (dialogTutorial != null && dialogTutorial.isShowing()) {
            dialogTutorial.dismiss();
        }
    }

    /**
     * Called from activity onResume
     */
    public void onResume() {
        GameWallUnitOfferBanner offerBanner = findViewWithVideoView();
        if (offerBanner != null &&
                offerBanner.getVideoComponent() != null &&
                isViewVisible(offerBanner) &&
                com.bee7.sdk.common.util.Utils.isOnline(getContext()) &&
                !offerBanner.isCloseNoticeShowing() &&
                !offerBanner.icCtaShowing()) {
            offerBanner.getVideoComponent().onResume();
        }
    }

    /**
     * @return true if video view was closed, false otherwise.
     */
    public boolean closeVideo(boolean showCloseNotice) {
        GameWallUnitOfferBanner gwUnitOfferBanner = findViewWithVideoView();
        if (gwUnitOfferBanner != null) {
            gwUnitOfferBanner.removeVideoView(null, true, this, showCloseNotice);
            return true;
        }
        return false;
    }

    /**
     * @param appOfferId to be used for searching AppOffer.
     * @return found AppOffer, false otherwise.
     */
    public AppOffer getAppOffer(String appOfferId) {
        for (int i = 0 ; i < gamesColumn1.getChildCount() ; i++) {
            GameWallUnit gameWallUnit = (GameWallUnit)gamesColumn1.getChildAt(i);
            AppOffer appOffer = gameWallUnit.getAppOffer(appOfferId);

            if (appOffer != null && appOffer.getId().equalsIgnoreCase(appOfferId)) {
                return appOffer;
            }
        }
        return null;
    }

    /**
     * @param appOffer to be used for updating matching game wall unit.
     */
    public void updateGameWallUnit(AppOffer appOffer) {
        for (int i = 0 ; i < gamesColumn1.getChildCount() ; i++) {
            GameWallUnit gameWallUnit = (GameWallUnit)gamesColumn1.getChildAt(i);

            AppOffer offer = gameWallUnit.getAppOffer(appOffer.getId());
            if (offer != null && offer.getId().equalsIgnoreCase(appOffer.getId())) {
                Logger.debug(TAG, "updateGameWallUnit " + appOffer.getLocalizedName() + " " + appOffer.getId());
                gameWallUnit.update(appOffer);
            }
        }
    }

    public void updateGameWallUnits() {
        for (int i = 0 ; i < gamesColumn1.getChildCount() ; i++) {
            GameWallUnit gameWallUnit = (GameWallUnit)gamesColumn1.getChildAt(i);
            gameWallUnit.update();
        }
    }

    /**
     * @param appOfferId to be used for search.
     * @return GameWallUnitOfferBanner that has correct appOffer, null otherwise.
     */
    public GameWallUnitOfferBanner findGwUnitOfferBanner(String appOfferId) {
        for (int i = 0 ; i < gamesColumn1.getChildCount() ; i++) {
            View view = gamesColumn1.getChildAt(i);
            if (view.getTag() != null &&
                    view.getTag().equals(appOfferId) &&
                    view instanceof GameWallUnitOfferBanner) {
                return (GameWallUnitOfferBanner) view;
            }
        }
        return null;
    }

    /**
     * @return GameWallUnitOfferBanner that has currently open video view
     */
    public GameWallUnitOfferBanner findViewWithVideoView() {
        for (int i = 0 ; i < gamesColumn1.getChildCount() ; i++) {
            View view = gamesColumn1.getChildAt(i);
            if (view instanceof GameWallUnitOfferBanner) {
                if (((GameWallUnitOfferBanner) view).isVideoViewShown()) {
                    return ((GameWallUnitOfferBanner) view);
                }
            }
        }
        return null;
    }

    /**
     * @return true if there is a video with reward playing, false otherwise.
     */
    public boolean isVideoWithRewardPlaying() {
        if (publisher.getAppOffersModel().getVideoPrequaificationlType() == AppOffersModel.VideoPrequalType.INLINE_REWARD) {
            GameWallUnitOfferBanner gwUnitOfferBanner = findViewWithVideoView();
            if (gwUnitOfferBanner != null && gwUnitOfferBanner.isVideoPlaying()) {
                AppOffer offer = gwUnitOfferBanner.getAppOffer(null);
                return !new SharedPreferencesRewardsHelper(getContext(),
                        publisher.getAppOffersModel().getVideoPrequalGlobalConfig().getMaxDailyRewardFreq())
                        .hasBeenRewardAlreadyGiven(offer.getId(), offer.getCampaignId());
            }
        }
        return false;
    }

    /**
     * @return true if there is a video view with close notice showing, false otherwise.
     */
    public boolean isVideoCloseNoticeShowing() {
        if (publisher.getAppOffersModel().getVideoPrequaificationlType() == AppOffersModel.VideoPrequalType.INLINE_REWARD) {
            GameWallUnitOfferBanner gwUnitOfferBanner = findViewWithVideoView();
            if (gwUnitOfferBanner != null && gwUnitOfferBanner.isCloseNoticeShowing()) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method is called when view was animated to the final position
     * or it was added without animation.
     */
    public void viewShown() {
    }

    /**
     * Removes all views from columns.
     */
    public void removeOfferViews() {
        Logger.debug(TAG, "Clearing gamesColumn1");
        gamesColumn1.removeAllViews();
    }

    @Override
    public void onVideoWithRewardPlaying(int column, int index, GameWallUnitOfferBanner gwUnitOfferBanner) {
        smoothScrollToListItem(column, index);
        gwUnitOfferBanner.showCloseNotice();
    }

    public VideoDialog getVideoDialog() {
        return videoDialog;
    }

    public void setHeader(View header) {
        gameWallHeaderFrameLayout.removeAllViews();
        gameWallHeaderFrameLayout.addView(header);
    }

    public void onHeaderHeightChanged(int viewHeight) {
        if (gamesColumn1 != null) {
            gamesColumn1.setPadding(0, viewHeight, 0, 0);
        }
    }

    public View getAnchorView() {
        if (videoDialog != null && videoDialog.isShowing()) {
            return videoDialog.getRootView();
        }
        return this;
    }
}
