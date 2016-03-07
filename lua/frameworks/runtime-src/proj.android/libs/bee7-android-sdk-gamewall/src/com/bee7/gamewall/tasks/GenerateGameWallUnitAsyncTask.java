package com.bee7.gamewall.tasks;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.bee7.gamewall.GameWallUnitOffer;
import com.bee7.gamewall.GameWallUnitOfferBanner;
import com.bee7.gamewall.GameWallView;
import com.bee7.gamewall.interfaces.OnOfferClickListener;
import com.bee7.gamewall.interfaces.OnVideoClickListener;
import com.bee7.sdk.publisher.GameWallConfiguration;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

/**
 * AsyncTask that puts generating of gamewall unit on another thread.
 */
public class GenerateGameWallUnitAsyncTask {

    private static final String TAG = GameWallView.class.getName();

    public interface OnGameWallUnitGeneratedListener {
        void OnGameWallUnitGenerated(GameWallUnitOffer gameWallUnitOffer, LinearLayout.LayoutParams layoutParams, int layoutIndex, int column);
    }

    private OnGameWallUnitGeneratedListener onGameWallUnitGeneratedListener;

    private Context context;
    private AppOffer appOffer;
    private OnOfferClickListener onOfferClickListener;
    private OnVideoClickListener onVideoClickListener;
    private AppOffersModel.VideoButtonPosition videoButtonPosition;
    private AppOffersModel.VideoPrequalType videoPrequaificationlType;
    private int maxDailyRewardFreq;
    private GameWallConfiguration.UnitType unitType;
    private int index;
    private int column;
    private float exchangeRate;

    public GenerateGameWallUnitAsyncTask(Context context, AppOffer appOffer, OnOfferClickListener onOfferClickListener,
                                         OnVideoClickListener onVideoClickListener, AppOffersModel.VideoButtonPosition videoButtonPosition,
                                         AppOffersModel.VideoPrequalType videoPrequaificationlType, int maxDailyRewardFreq,
                                         GameWallConfiguration.UnitType unitType, int index, int column, float exchangeRate) {
        this.context = context;
        this.appOffer = appOffer;
        this.onOfferClickListener = onOfferClickListener;
        this.onVideoClickListener = onVideoClickListener;
        this.videoButtonPosition = videoButtonPosition;
        this.videoPrequaificationlType = videoPrequaificationlType;
        this.maxDailyRewardFreq = maxDailyRewardFreq;
        this.unitType = unitType;
        this.index = index;
        this.column = column;
        this.exchangeRate = exchangeRate;
    }

    public void setOnGameWallUnitGeneratedListener(OnGameWallUnitGeneratedListener onGameWallUnitGeneratedListener) {
        this.onGameWallUnitGeneratedListener = onGameWallUnitGeneratedListener;
    }

    protected GameWallUnitOffer doInBackground() {
        GameWallUnitOfferBanner gwUnitOfferBanner = new GameWallUnitOfferBanner(
                context,
                appOffer,
                onOfferClickListener,
                onVideoClickListener,
                videoButtonPosition,
                videoPrequaificationlType,
                maxDailyRewardFreq,
                unitType,
                index,
                0,
                column,
                exchangeRate);

        gwUnitOfferBanner.setTag(appOffer.getId());
        return gwUnitOfferBanner;
    }

    protected void onPostExecute(GameWallUnitOffer gameWallUnitOffer) {
        if (onGameWallUnitGeneratedListener != null && gameWallUnitOffer != null) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

            onGameWallUnitGeneratedListener.OnGameWallUnitGenerated(gameWallUnitOffer, layoutParams, index, column);
        }
    }
}
