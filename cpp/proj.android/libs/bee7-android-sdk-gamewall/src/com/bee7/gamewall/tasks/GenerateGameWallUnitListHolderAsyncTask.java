package com.bee7.gamewall.tasks;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.bee7.gamewall.GameWallUnitOfferList;
import com.bee7.gamewall.GameWallView;
import com.bee7.gamewall.interfaces.OnOfferClickListener;
import com.bee7.gamewall.interfaces.OnVideoClickListener;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.GameWallConfiguration;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

import java.util.List;

/**
 * AsyncTask that puts generating of gamewall unit on another thread.
 */
public class GenerateGameWallUnitListHolderAsyncTask {

    private static final String TAG = GenerateGameWallUnitListHolderAsyncTask.class.getName();

    public void removeCallback() {
        onGameWallUnitListHolderGeneratedListener = null;
    }

    public interface OnGameWallUnitListHolderGeneratedListener {
        void OnGameWallUnitListHolderGenerated(View gameWallUnitListHolder, LinearLayout.LayoutParams layoutParams, int layoutIndex, int column);
    }

    private OnGameWallUnitListHolderGeneratedListener onGameWallUnitListHolderGeneratedListener;

    private Context context;
    private List<GameWallUnitOfferList.OfferTypePair> appOffers;
    private OnOfferClickListener onOfferClickListener;
    private OnVideoClickListener onVideoClickListener;
    private AppOffersModel.VideoButtonPosition videoButtonPosition;
    private AppOffersModel.VideoPrequalType videoPrequaificationlType;
    private int maxDailyRewardFreq;
    private GameWallConfiguration.UnitType unitType;
    private int index;
    private int column;
    private boolean firstInColumnGroup;
    private float exchangeRate;
    private GameWallConfiguration.LayoutType layoutType;
    private boolean immersiveMode = false;

    public GenerateGameWallUnitListHolderAsyncTask(Context context, List<GameWallUnitOfferList.OfferTypePair> appOffers, OnOfferClickListener onOfferClickListener,
                                                   OnVideoClickListener onVideoClickListener, AppOffersModel.VideoButtonPosition videoButtonPosition,
                                                   AppOffersModel.VideoPrequalType videoPrequaificationlType, int maxDailyRewardFreq,
                                                   GameWallConfiguration.UnitType unitType, int index, int column, boolean firstInColumnGroup,
                                                   float exchangeRate, GameWallConfiguration.LayoutType layoutType, boolean immersiveMode)
    {
        this.context = context;
        this.appOffers = appOffers;
        this.onOfferClickListener = onOfferClickListener;
        this.onVideoClickListener = onVideoClickListener;
        this.videoButtonPosition = videoButtonPosition;
        this.videoPrequaificationlType = videoPrequaificationlType;
        this.maxDailyRewardFreq = maxDailyRewardFreq;
        this.unitType = unitType;
        this.index = index;
        this.column = column;
        this.firstInColumnGroup = firstInColumnGroup;
        this.exchangeRate = exchangeRate;
        this.layoutType = layoutType;
        this.immersiveMode = immersiveMode;
    }

    public void setOnGameWallUnitListHolderGeneratedListener(OnGameWallUnitListHolderGeneratedListener onGameWallUnitListHolderGeneratedListener) {
        this.onGameWallUnitListHolderGeneratedListener = onGameWallUnitListHolderGeneratedListener;
    }

    protected View doInBackground() {
        Logger.debug(TAG, "GameWallUnitOfferList doInBackground");
        GameWallUnitOfferList gwUnitOfferListHolder = new GameWallUnitOfferList(
                context,
                appOffers,
                onOfferClickListener,
                onVideoClickListener,
                videoButtonPosition,
                videoPrequaificationlType,
                maxDailyRewardFreq,
                index,
                column,
                firstInColumnGroup,
                exchangeRate,
                layoutType,
                immersiveMode);
        return gwUnitOfferListHolder;
    }

    protected void onPostExecute(View view) {
        Logger.debug(TAG, "onPostExecute()");
        if (onGameWallUnitListHolderGeneratedListener != null && view != null) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);

            Logger.debug(TAG, "onGameWallUnitListHolderGeneratedListener.OnGameWallUnitListHolderGenerated(view, layoutParams, index: " + index + ", column: " + column + ")");
            onGameWallUnitListHolderGeneratedListener.OnGameWallUnitListHolderGenerated(view, layoutParams, index, column);
        } else {
            Logger.debug(TAG, "onGameWallUnitListHolderGeneratedListener == null or view == null");
        }
    }
}
