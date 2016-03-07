package com.bee7.gamewall;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bee7.gamewall.assets.AnimFactory;
import com.bee7.gamewall.interfaces.OnOfferClickListener;
import com.bee7.gamewall.interfaces.OnVideoClickListener;
import com.bee7.gamewall.interfaces.OnVideoRewardGeneratedListener;
import com.bee7.gamewall.interfaces.OnVideoWithRewardPlayingListener;
import com.bee7.gamewall.video.VideoComponent;
import com.bee7.gamewall.views.Bee7ImageView;
import com.bee7.sdk.common.util.*;
import com.bee7.sdk.publisher.GameWallConfiguration;
import com.bee7.sdk.publisher.Publisher;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

/**
 * Offer Banner GameWall Unit
 */
public class GameWallUnitOfferBanner extends GameWallUnitOffer {

    private final static String TAG = GameWallUnitOfferBanner.class.toString();

    private TextView description;
    private LinearLayout ratingsLayout;
    private RelativeLayout buttonRoot;
    private Bee7ImageView button;
    private RelativeLayout buttonVideoLayout;
    private RelativeLayout buttonVideoLayoutLeft;
    private Bee7ImageView videoButton;
    private TextView videoRewardText;
    private ImageView videoRewardIcon;
    private FrameLayout videoPlaceholder;
    private LinearLayout titleLayout;

    private Animation videoViewExpansionAnim;
    private Animation videoViewCollapseAnim;
    private boolean wasVideoStarted = false;
    private float exchangeRate;

    //private InWallVideoView inWallVideo;
    private VideoComponent videoComponent;

    public GameWallUnitOfferBanner(Context context, AppOffer _appOffer, OnOfferClickListener _onOfferClickListener,
                                   OnVideoClickListener _onVideoClickListener,
                                   AppOffersModel.VideoButtonPosition _videoButtonPosition,
                                   AppOffersModel.VideoPrequalType _videoPrequaificationlType,
                                   int maxDailyRewardFreq, GameWallConfiguration.UnitType _unitType,
                                   int _index, int _indexV, int _column, float exchangeRate) {
        super(context, _appOffer, maxDailyRewardFreq, _onOfferClickListener, _onVideoClickListener,
                _videoPrequaificationlType, _unitType, _videoButtonPosition, _index, _indexV, _column);

        inflate(getContext(), R.layout.gamewall_unit_offer_banner, this);

        icon = (Bee7ImageView) findViewById(R.id.gamewallGamesListItemIcon);
        title = (TextView) findViewById(R.id.gamewallGamesListItemTitle);
        description = (TextView) findViewById(R.id.gamewallGamesListItemDescription);
        ratingsLayout = (LinearLayout) findViewById(R.id.gamewallGamesListItemRatingLayout);
        titleLayout = (LinearLayout) findViewById(R.id.gamewallGamesListItemTitleLayout);
        buttonRoot = (RelativeLayout) findViewById(R.id.gamewallGamesListItemButtonsHolder);
        button = (Bee7ImageView) findViewById(R.id.gamewallGamesListItemButton);
        buttonVideoLayout = (RelativeLayout) findViewById(R.id.gamewallGamesListItemButtonVideoLayout);
        buttonVideoLayoutLeft = (RelativeLayout) findViewById(R.id.gamewallGamesListItemButtonVideoLayoutLeft);
        spinner = (ProgressBar) findViewById(R.id.gamewallGamesListItemSpinner);
        videoPlaceholder = (FrameLayout) findViewById(R.id.gamewallGamesListItemVideoPlaceholder);

        this.exchangeRate = exchangeRate;

        try {
            String fontFile = getContext().getResources().getString(R.string.bee7_font_file);

            if (com.bee7.sdk.common.util.Utils.hasText(fontFile)) {

                Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontFile);

                title.setTypeface(typeface);
                description.setTypeface(typeface);
            }
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to load font");
        }

        update(_appOffer);
    }

    @Override
    public void update(AppOffer _appOffer) {
        Logger.debug("GameWallUnitOfferBanner", "update banner " + appOffer.getLocalizedName() + " " + appOffer.getId());
        super.update(_appOffer);

        if (videoPrequaificationlType == AppOffersModel.VideoPrequalType.NO_VIDEO ||
                !appOffer.showVideoButton() ||
                appOffer.getState() == AppOffer.State.CONNECTED) {
            buttonVideoLayout.setVisibility(GONE);
            buttonVideoLayoutLeft.setVisibility(GONE);
        } else {
            if (videoButtonPosition == AppOffersModel.VideoButtonPosition.RIGHT) {
                buttonVideoLayout.setVisibility(VISIBLE);
                buttonVideoLayoutLeft.setVisibility(GONE);

                videoButton = (Bee7ImageView) findViewById(R.id.gamewallGamesListItemButtonVideo);
                videoRewardText = (TextView) findViewById(R.id.gamewallGamesListItemVideoRewardText);
                videoRewardIcon = (ImageView) findViewById(R.id.gamewallGamesListItemVideoRewardIcon);
            } else {
                buttonVideoLayout.setVisibility(GONE);
                buttonVideoLayoutLeft.setVisibility(VISIBLE);

                videoButton = (Bee7ImageView) findViewById(R.id.gamewallGamesListItemButtonVideoLeft);
                videoRewardText = (TextView) findViewById(R.id.gamewallGamesListItemVideoRewardTextLeft);
                videoRewardIcon = (ImageView) findViewById(R.id.gamewallGamesListItemVideoRewardIconLeft);
                buttonVideoLayout = buttonVideoLayoutLeft;
            }
        }

        //we need to check android api level and if offer supports video
        if (canVideoBePlayed() &&
                videoButton != null) {

            // offer with video
            appOfferWithResult.setVideoOffered(true);

            videoButton.setOnClickListener(onVideoBtnClickListener);
            icon.setOnClickListener(onVideoBtnClickListener);
            titleLayout.setOnClickListener(onVideoBtnClickListener);

            videoButton.setOnBackgroundPaddingChangedListener(new Bee7ImageView.OnBackgroundPaddingChangedListener() {
                @Override
                public void onPaddingSet(boolean set) {
                    icon.setOnTouchPaddingChange(set);
                }
            });
            icon.setOnBackgroundPaddingChangedListener(new Bee7ImageView.OnBackgroundPaddingChangedListener() {
                @Override
                public void onPaddingSet(boolean set) {
                    videoButton.setOnTouchPaddingChange(set);
                }
            });
            titleLayout.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        icon.setOnTouchPaddingChange(true);
                        videoButton.setOnTouchPaddingChange(true);
                    } else if (event.getAction() == MotionEvent.ACTION_UP ||
                            event.getAction() == MotionEvent.ACTION_CANCEL) {
                        icon.setOnTouchPaddingChange(false);
                        videoButton.setOnTouchPaddingChange(false);
                    }

                    return false;
                }
            });

            if ((videoPrequaificationlType == AppOffersModel.VideoPrequalType.INLINE_REWARD ||
                    videoPrequaificationlType == AppOffersModel.VideoPrequalType.FULLSCREEN_REWARD) &&
                    !rewardAlreadyGiven) {

                if (appOffer.getVideoReward() > 0) {
                    videoRewardText.setVisibility(VISIBLE);
                    videoRewardIcon.setVisibility(VISIBLE);

                    videoRewardText.setText("+" + (int)(appOffer.getVideoReward() * exchangeRate));
                }  else {
                    videoRewardText.setVisibility(GONE);
                    videoRewardIcon.setVisibility(GONE);
                }
            } else {
                videoRewardText.setVisibility(GONE);
                videoRewardIcon.setVisibility(GONE);
            }

        } else {
            buttonVideoLayout.setVisibility(GONE);

            // set regular click listener on all non video units
            icon.setOnClickListener(onClickListener);
            titleLayout.setOnClickListener(onClickListener);

            button.setOnBackgroundPaddingChangedListener(new Bee7ImageView.OnBackgroundPaddingChangedListener() {
                @Override
                public void onPaddingSet(boolean set) {
                    icon.setOnTouchPaddingChange(set);
                }
            });
            icon.setOnBackgroundPaddingChangedListener(new Bee7ImageView.OnBackgroundPaddingChangedListener() {
                @Override
                public void onPaddingSet(boolean set) {
                    button.setOnTouchPaddingChange(set);
                }
            });
            titleLayout.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        icon.setOnTouchPaddingChange(true);
                        button.setOnTouchPaddingChange(true);
                    } else if (event.getAction() == MotionEvent.ACTION_UP ||
                            event.getAction() == MotionEvent.ACTION_CANCEL) {
                        icon.setOnTouchPaddingChange(false);
                        button.setOnTouchPaddingChange(false);
                    }

                    return false;
                }
            });
        }

        if (appOffer.showUserRatings()) {
            description.setVisibility(GONE);
            ratingsLayout.removeAllViews();
            ratingsLayout.setVisibility(VISIBLE);

            double num = appOffer.getUserRating();
            if (num > 0) {
                int numberOfFullStars = (int) num;
                double fractionalPart = num - numberOfFullStars;

                for (int i = 0; i < 5; i++) {
                    ImageView imageView = new ImageView(getContext());
                    if (i < numberOfFullStars) { //add full star
                        imageView.setImageDrawable(getResources().getDrawable(R.drawable.bee7_star_full));
                    } else if (i == numberOfFullStars && fractionalPart > 0) { //add half star
                        imageView.setImageDrawable(getResources().getDrawable(R.drawable.bee7_star_half));
                    } else { //add empty star
                        imageView.setImageDrawable(getResources().getDrawable(R.drawable.bee7_star_empty));
                    }
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.rightMargin = getResources().getDimensionPixelSize(R.dimen.bee7_offer_banner_rating_spacing);
                    imageView.setLayoutParams(params);
                    ratingsLayout.addView(imageView);
                }
            } else {
                ratingsLayout.setVisibility(GONE);
                description.setVisibility(VISIBLE);
                description.setText(appOffer.getLocalizedDescription());
            }
        } else {
            ratingsLayout.setVisibility(GONE);
            description.setVisibility(VISIBLE);
            description.setText(appOffer.getLocalizedDescription());
        }

        if (appOffer.getState() == AppOffer.State.CONNECTED) {
            button.setImageDrawable(getResources().getDrawable(R.drawable.bee7_btn_game));

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) button.getLayoutParams();
            params.width = getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_button_game_width);
            params.height = getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_button_height);
            button.setLayoutParams(params);

            description.setVisibility(GONE);
            ratingsLayout.setVisibility(GONE);
        } else {
            button.setImageDrawable(getResources().getDrawable(R.drawable.bee7_btn_dl));

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) button.getLayoutParams();
            params.width = getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_button_dl_width);
            params.height = getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_button_height);
            button.setLayoutParams(params);
        }

        button.setOnClickListener(onClickListener);

        setAppOfferIcon();
    }

    private OnClickListener onVideoBtnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (canVideoBePlayed() &&
                    videoButton != null)
            {
                synchronized (GameWallView.lastClickSync) {
                    // mis-clicking prevention, using threshold of 1000 ms
                    if ((SystemClock.elapsedRealtime() - GameWallView.lastClickTimestamp) < 1000) {
                        GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();
                        return;
                    }

                    GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();

                    wasVideoStarted = true;
                    onVideoClickListener.onVideoClick(appOffer, appOfferWithResult);
                }
            }
        }
    };

    private OnClickListener onClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (onOfferClickListener != null) {
                synchronized (GameWallView.lastClickSync) {
                    // mis-clicking prevention, using threshold of 1000 ms
                    if ((SystemClock.elapsedRealtime() - GameWallView.lastClickTimestamp) < 1000) {
                        GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();
                        return;
                    }

                    GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();

                    Publisher.AppOfferStartOrigin origin = Publisher.AppOfferStartOrigin.DEFAULT_BTN;
                    if (wasVideoStarted) {
                        origin = Publisher.AppOfferStartOrigin.DEFAULT_VIDEO_BTN;
                    }
                    onOfferClickListener.onOfferClick(appOffer, appOfferWithResult, wasVideoStarted, origin);
                }
            }
        }
    };

    public void addVideoView(final Publisher publisher, OnVideoRewardGeneratedListener onVideoRewardGeneratedListener) {
        videoComponent = new VideoComponent(getContext());
        videoComponent.setup(appOffer, publisher, onOfferClickListener, onVideoRewardGeneratedListener, appOfferWithResult, new VideoComponent.VideoComponentCallbacks() {
            @Override
            public void onVideoEnd() {

            }

            @Override
            public void onVideoStart() {

            }

            @Override
            public void onHide(View v) {
                synchronized (GameWallView.lastClickSync) {
                    synchronized (GameWallView.lastClickSync) {
                        // mis-clicking prevention, using threshold of 1000 ms
                        if ((SystemClock.elapsedRealtime() - GameWallView.lastClickTimestamp) < 1000) {
                            GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();
                            return;
                        }

                        GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();

                        boolean rewardAlreadyGiven =
                                new SharedPreferencesRewardsHelper(getContext(), publisher.getAppOffersModel().getVideoPrequalGlobalConfig().getMaxDailyRewardFreq())
                                        .hasBeenRewardAlreadyGiven(appOffer.getId(), appOffer.getCampaignId());

                        if (publisher.getAppOffersModel().getVideoPrequaificationlType() == AppOffersModel.VideoPrequalType.INLINE_REWARD && !rewardAlreadyGiven) {
                            showCloseNotice();
                        } else {
                            removeVideoView(null, true, null, false);
                        }
                    }
                }
            }
        });
        videoComponent.setTag("video");


        if (videoViewExpansionAnim == null || videoViewExpansionAnim.hasEnded()
                || videoViewCollapseAnim == null || videoViewCollapseAnim.hasEnded()) {

            videoPlaceholder.addView(videoComponent, ViewGroup.LayoutParams.MATCH_PARENT, 1);

            videoViewExpansionAnim = AnimFactory.createTransformExpansionVideo(videoComponent, videoPlaceholder);
            videoViewExpansionAnim.setDuration(AnimFactory.ANIMATION_DURATION);

            videoViewExpansionAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if (button != null) {
                        button.setEnabled(false);
                    }
                    if (videoButton != null)  {
                        videoButton.setEnabled(false);
                    }

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (button != null) {
                                button.setEnabled(true);
                            }
                            if (videoButton != null)  {
                                videoButton.setEnabled(true);
                            }
                        }
                    }, AnimFactory.ANIMATION_DURATION);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (button != null) {
                        button.setEnabled(true);
                    }
                    if (videoButton != null)  {
                        videoButton.setEnabled(true);
                    }
                    videoComponent.showVideo();
                    wasVideoStarted = true;
                }

                @Override
                public void onAnimationRepeat(Animation animation) { }
            });

            videoComponent.startAnimation(videoViewExpansionAnim);

        }
    }

    public boolean removeVideoView(final Animation.AnimationListener animationListener, boolean animate,
                                   OnVideoWithRewardPlayingListener onVideoWithRewardPlayingListener,
                                   boolean showCloseNotice) {
        if (videoPlaceholder.getChildCount() > 0) {
            rewardAlreadyGiven = sharedPreferencesRewardsHelper.hasBeenRewardAlreadyGiven(appOffer.getId(), appOffer.getCampaignId());

            if (videoComponent.isVideoPlaying() &&
                    videoPrequaificationlType == AppOffersModel.VideoPrequalType.INLINE_REWARD &&
                    !videoComponent.isCloseNoticeShown() &&
                    !rewardAlreadyGiven &&
                    showCloseNotice &&
                    !videoComponent.isCtaShowing()) {

                if (onVideoWithRewardPlayingListener != null) {
                    onVideoWithRewardPlayingListener.onVideoWithRewardPlaying(column, index, GameWallUnitOfferBanner.this);
                    return false;
                }
            }

            if (videoComponent.isVideoPlaying() || videoComponent.isCloseNoticeShown()) {
                videoComponent.reportVideoWatchedEvent();
            }

            if (animate && (videoViewCollapseAnim == null || videoViewCollapseAnim.hasEnded()
                    || videoViewExpansionAnim == null || videoViewExpansionAnim.hasEnded()) ) {
                View view = videoPlaceholder.getChildAt(0);

                videoViewCollapseAnim = AnimFactory.createTransformCollapseVideo(view, videoPlaceholder);
                videoViewCollapseAnim.setDuration(AnimFactory.ANIMATION_DURATION);

                videoViewCollapseAnim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        if (videoPlaceholder != null && videoComponent != null && !videoComponent.isCtaShowing()) {
                            videoComponent.onPause();
                        }
                        videoComponent.remove();
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        videoPlaceholder.removeAllViews();
                        if (animationListener != null) {
                            animationListener.onAnimationEnd(animation);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) { }
                });

                view.startAnimation(videoViewCollapseAnim);
                return true;
            } else {
                videoPlaceholder.removeAllViews();
                return true;
            }
        }

        return false;
    }

    public boolean isVideoPlaying() {
        if (videoComponent != null) {
            return videoComponent.isVideoPlaying();
        }
        return false;
    }

    public void replayVideo() {
        if (videoComponent != null) {
            videoComponent.replayVideo();
        }
    }

    public boolean isVideoViewShown() {
        return videoPlaceholder.getChildCount() > 0;
    }

    public boolean isCloseNoticeShowing() {
        if (videoComponent != null) {
            return videoComponent.isCloseNoticeShown();
        }
        return false;
    }

    public void showCloseNotice() {
        if (videoComponent != null) {
            rewardAlreadyGiven = sharedPreferencesRewardsHelper.hasBeenRewardAlreadyGiven(appOffer.getId(), appOffer.getCampaignId());
            if (videoComponent.isVideoPlaying() &&
                    videoPrequaificationlType == AppOffersModel.VideoPrequalType.INLINE_REWARD &&
                    !rewardAlreadyGiven) {
                videoComponent.showCloseNotice();
            }
        }
    }

    @Override
    public AppOffer getAppOffer(String ignored) {
        return appOffer;
    }

    /**
     *
     * @return videoComponent
     */
    public VideoComponent getVideoComponent() {
        return videoComponent;
    }

    public boolean icCtaShowing() {
        return videoComponent.isCtaShowing();
    }
}
