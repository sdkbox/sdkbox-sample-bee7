package com.bee7.gamewall;

import android.content.Context;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bee7.gamewall.interfaces.OnOfferClickListener;
import com.bee7.gamewall.interfaces.OnVideoClickListener;
import com.bee7.gamewall.views.Bee7ImageView;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.GameWallConfiguration;
import com.bee7.sdk.publisher.Publisher;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

/**
 * Offer List item in GameWall Offer List Holder
 */
public class GameWallUnitOfferListItem extends GameWallUnitOffer {

    private static final String TAG = GameWallUnitOfferListItem.class.toString();
    private Bee7ImageView videoButton;
    private RelativeLayout buttonVideoLayout;
    private TextView videoRewardText;
    private ImageView videoRewardIcon;
    private float exchangeRate;

    public GameWallUnitOfferListItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        icon = (Bee7ImageView) findViewById(R.id.gamewallGamesListItemIcon);
        title = (TextView) findViewById(R.id.bee7_gamewallGamesListItemTitle);
        buttonVideoLayout = (RelativeLayout) findViewById(R.id.gamewallGamesListItemButtonVideoLayout);
        videoButton = (Bee7ImageView) findViewById(R.id.gamewallGamesListItemButtonVideo);
        videoRewardText = (TextView) findViewById(R.id.gamewallGamesListItemVideoRewardText);
        videoRewardIcon = (ImageView) findViewById(R.id.gamewallGamesListItemVideoRewardIcon);
        spinner = (ProgressBar) findViewById(R.id.gamewallGamesListItemSpinner);
    }

    public void update(AppOffer _appOffer, OnOfferClickListener _onOfferClickListener, OnVideoClickListener _onVideoClickListener,
                       AppOffersModel.VideoButtonPosition _videoButtonPosition, AppOffersModel.VideoPrequalType _videoPrequaificationlType,
                       int maxDailyRewardFreq, GameWallConfiguration.UnitType _unitType, int index, int indexV, float exchangeRate) {

        update(_appOffer, maxDailyRewardFreq, _onOfferClickListener, _onVideoClickListener, _videoPrequaificationlType,
                _unitType, _videoButtonPosition, index, indexV, 1);

        this.exchangeRate = exchangeRate;

        try {
            String fontFile = getContext().getResources().getString(R.string.bee7_font_file);
            if (com.bee7.sdk.common.util.Utils.hasText(fontFile)) {
                Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontFile);
                title.setTypeface(typeface);
            }
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to load font");
        }

        icon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    if (videoButton != null) {
                        videoButton.setOnTouchPaddingChange(true);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL)
                {
                    if (videoButton != null) {
                        videoButton.setOnTouchPaddingChange(false);
                    }
                }

                return false;
            }
        });

        videoButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                {
                    if (icon != null) {
                        icon.setOnTouchPaddingChange(true);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL)
                {
                    if (icon != null) {
                        icon.setOnTouchPaddingChange(false);
                    }
                }

                return false;
            }
        });

        update(_appOffer);
    }

    @Override
    public void update(AppOffer _appOffer) {
        super.update(_appOffer);

        icon.setOnClickListener(onClickListener);
        title.setOnClickListener(onClickListener);
        videoButton.setOnClickListener(onClickListener);

        //Check what kind of item is this: NOT_CONNECTED, NOT_CONNECTED_PENDING_INSTALL, CONNECTED
        if (appOffer.getState() == AppOffer.State.NOT_CONNECTED
                || appOffer.getState() == AppOffer.State.NOT_CONNECTED_PENDING_INSTALL) {
            //show download or video icon
            if (canVideoBePlayed()) {

                // offer with video
                appOfferWithResult.setVideoOffered(true);

                if ((videoPrequaificationlType == AppOffersModel.VideoPrequalType.INLINE_REWARD ||
                        videoPrequaificationlType == AppOffersModel.VideoPrequalType.FULLSCREEN_REWARD) &&
                        !rewardAlreadyGiven) {
                    videoButton.setImageDrawable(getResources().getDrawable(R.drawable.bee7_btn_play_mini));
                    if (appOffer.getVideoReward() > 0) {
                        videoRewardText.setVisibility(VISIBLE);
                        videoRewardIcon.setVisibility(VISIBLE);
                        videoRewardText.setText("+" + (int)(appOffer.getVideoReward() * exchangeRate));
                    }  else {
                        videoRewardText.setVisibility(GONE);
                        videoRewardIcon.setVisibility(GONE);
                    }
                } else {
                    videoButton.setImageDrawable(getResources().getDrawable(R.drawable.bee7_btn_play_mini));
                    videoRewardText.setVisibility(GONE);
                    videoRewardIcon.setVisibility(GONE);
                }

            } else {
                videoRewardText.setVisibility(GONE);
                videoRewardIcon.setVisibility(GONE);
                videoButton.setImageDrawable(getResources().getDrawable(R.drawable.bee7_btn_dl_mini));
            }
        } else if (appOffer.getState() == AppOffer.State.CONNECTED) {
            //show no icon
            buttonVideoLayout.setVisibility(GONE);
        }
    }

    private OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            synchronized (GameWallView.lastClickSync) {
                // mis-clicking prevention, using threshold of 1000 ms
                if ((SystemClock.elapsedRealtime() - GameWallView.lastClickTimestamp) < 1000) {
                    GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();
                    return;
                }
                GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();

                if (appOffer.getState() == AppOffer.State.NOT_CONNECTED
                        || appOffer.getState() == AppOffer.State.NOT_CONNECTED_PENDING_INSTALL) {
                    //show download or video icon
                    if (canVideoBePlayed()) {

                        if (onVideoClickListener != null) {
                            onVideoClickListener.onVideoClick(appOffer, appOfferWithResult);
                        }

                    } else {
                        if (onOfferClickListener != null) {
                            onOfferClickListener.onOfferClick(appOffer, appOfferWithResult, false, Publisher.AppOfferStartOrigin.DEFAULT_BTN);
                        }
                    }
                } else if (appOffer.getState() == AppOffer.State.CONNECTED) {
                    if (onOfferClickListener != null) {
                        onOfferClickListener.onOfferClick(appOffer, appOfferWithResult, false, Publisher.AppOfferStartOrigin.DEFAULT_BTN);
                    }
                }
            }
        }
    };

    @Override
    public AppOffer getAppOffer(String appOfferId) {
        return appOffer;
    }
}
