package com.bee7.gamewall;


import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Pair;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bee7.gamewall.assets.AssetsManager;
import com.bee7.gamewall.assets.AssetsManagerSetBitmapTask;
import com.bee7.gamewall.assets.UnscaledBitmapLoader;
import com.bee7.gamewall.interfaces.OnOfferClickListener;
import com.bee7.gamewall.interfaces.OnVideoClickListener;
import com.bee7.gamewall.views.Bee7ImageView;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.common.util.SharedPreferencesRewardsHelper;
import com.bee7.sdk.publisher.GameWallConfiguration;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResult;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResultImpl;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

/**
 * Base abstract class for gamewall unit offers
 */
public abstract class GameWallUnitOffer extends GameWallUnit {

    private String Tag = GameWallUnitOffer.class.getName();

    protected AppOffer appOffer;
    protected AppOfferWithResult appOfferWithResult;
    protected SharedPreferencesRewardsHelper sharedPreferencesRewardsHelper;
    protected OnOfferClickListener onOfferClickListener;
    protected OnVideoClickListener onVideoClickListener;
    protected AppOffersModel.VideoPrequalType videoPrequaificationlType;
    protected AppOffersModel.VideoButtonPosition videoButtonPosition;
    protected GameWallConfiguration.UnitType unitType;

    protected boolean rewardAlreadyGiven = false;

    /**
     * Views
     */
    protected Bee7ImageView icon;
    protected ProgressBar spinner;
    protected TextView title;

    public GameWallUnitOffer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GameWallUnitOffer(Context context, AppOffer _appOffer, int maxDailyRewardFreq, OnOfferClickListener _onOfferClickListener,
                             OnVideoClickListener _onVideoClickListener, AppOffersModel.VideoPrequalType _videoPrequaificationlType,
                             GameWallConfiguration.UnitType _unitType, AppOffersModel.VideoButtonPosition _videoButtonPosition,
                             int index, int indexV, int column) {
        super(context, index, indexV, column);

        this.appOffer = _appOffer;
        this.videoButtonPosition = _videoButtonPosition;
        this.sharedPreferencesRewardsHelper = new SharedPreferencesRewardsHelper(context, maxDailyRewardFreq);
        this.onOfferClickListener = _onOfferClickListener;
        this.onVideoClickListener = _onVideoClickListener;
        this.videoPrequaificationlType = _videoPrequaificationlType;
        this.unitType = _unitType;

        this.appOfferWithResult = new AppOfferWithResultImpl(this.appOffer);

        this.appOfferWithResult.setGameWallPosition(new Pair<Integer, Integer>(index, indexV));
        this.appOfferWithResult.setVideoOffered(false);
        this.appOfferWithResult.setUnitType(unitType);
    }

    /**
     * Update Game Wall Unit Offer
     */
    public void update(AppOffer _appOffer, int maxDailyRewardFreq, OnOfferClickListener _onOfferClickListener,
                       OnVideoClickListener _onVideoClickListener, AppOffersModel.VideoPrequalType _videoPrequaificationlType,
                       GameWallConfiguration.UnitType _unitType, AppOffersModel.VideoButtonPosition _videoButtonPosition,
                       int index, int indexV, int column) {
        update(index, indexV, column);

        this.appOffer = _appOffer;
        this.videoButtonPosition = _videoButtonPosition;
        this.sharedPreferencesRewardsHelper = new SharedPreferencesRewardsHelper(getContext(), maxDailyRewardFreq);
        this.onOfferClickListener = _onOfferClickListener;
        this.onVideoClickListener = _onVideoClickListener;
        this.videoPrequaificationlType = _videoPrequaificationlType;
        this.unitType = _unitType;

        this.appOfferWithResult = new AppOfferWithResultImpl(this.appOffer);

        this.appOfferWithResult.setGameWallPosition(new Pair<Integer, Integer>(index, indexV));
        this.appOfferWithResult.setVideoOffered(false);
        this.appOfferWithResult.setUnitType(unitType);
    }

    @Override
    public AppOfferWithResult getAppOfferWithResult(String appOfferId) {
        return appOfferWithResult;
    }

    /**
     * @return unitType
     */
    public GameWallConfiguration.UnitType getUnitType() {
        return unitType;
    }

    public void update(AppOffer appOffer) {
        this.appOffer = appOffer;
        this.appOfferWithResult.setAppOffer(this.appOffer);

        rewardAlreadyGiven = sharedPreferencesRewardsHelper.hasBeenRewardAlreadyGiven(appOffer.getId(), appOffer.getCampaignId()    );

        if (title == null) {
            throw new IllegalStateException("GameWallUnit title view must not be null!");
        }

        title.setText(appOffer.getLocalizedName());

        setAppOfferIcon();
    }

    protected void setAppOfferIcon() {
        if (icon == null || spinner == null) {
            throw new IllegalStateException("GameWallUnit icon view or spinner view must not be null!");
        }

        if (appOffer != null && appOffer.isInnerApp()) {
            icon.setImageDrawable(appOffer.getIconDrawable());
            return;
        }

        AppOffer.IconUrlSize iconUrlSize = GameWallImpl.getAppOfIconUrlSize(getResources());
        UnscaledBitmapLoader.ScreenDPI screenDPI = UnscaledBitmapLoader.ScreenDPI.parseDensity(getResources()
                .getString(R.string.bee7_gamewallSourceIconDPI));

        /**
         * This is an example of how we can get default offer icon if for some reason appOffer icon
         *  url is null or malformed.
         */
        /*
        appOffer.getDefaultIconBitmap(getContext(), iconUrlSize, new AppOfferDefaultIconListener(){
            @Override
            public void onDefaultIcon(Bitmap bitmap) {
                icon.setImageBitmap(bitmap);
            }
        });
        */
        AssetsManagerSetBitmapTask task = new AssetsManagerSetBitmapTask(appOffer.getIconUrl(iconUrlSize), getContext()) {
            @Override
            public void bitmapLoadedPost(Bitmap bitmap) {
                if (getParams() != appOffer) {
                    Logger.warn("", "View already changed: old = {0}, new = {1}", getParams(), appOffer);
                    return;
                }

                if (icon == null || spinner == null) {
                    Logger.warn("", "icon or spinner == null");
                    return;
                }

                icon.setImageBitmap(bitmap);

                if (bitmap == null) {
                    if (com.bee7.sdk.common.util.Utils.isOnline(getContext())) {
                        spinner.setVisibility(VISIBLE);
                    } else {
                        spinner.setVisibility(GONE);
                    }
                } else {
                    spinner.setVisibility(GONE);
                }
            }
        };

        task.setParams(appOffer);
        task.setSourceImageDPI(screenDPI);

        AssetsManager.getInstance().runIconTask(task);
    }

    protected boolean canVideoBePlayed() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                appOffer != null &&
                appOffer.showVideoButton() &&
                videoPrequaificationlType != AppOffersModel.VideoPrequalType.NO_VIDEO &&
                (appOffer.getState() == AppOffer.State.NOT_CONNECTED || appOffer.getState() == AppOffer.State.NOT_CONNECTED_PENDING_INSTALL) &&
                appOffer.getVideoUrl() != null &&
                com.bee7.sdk.common.util.Utils.isHardwareVideoCapable();
    }
}
