package com.bee7.gamewall.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bee7.gamewall.GameWallImpl;
import com.bee7.gamewall.GameWallView;
import com.bee7.gamewall.R;
import com.bee7.gamewall.assets.AssetsManager;
import com.bee7.gamewall.assets.AssetsManagerSetBitmapTask;
import com.bee7.gamewall.assets.UnscaledBitmapLoader;
import com.bee7.gamewall.dialogs.DialogNoInternet;
import com.bee7.gamewall.interfaces.OnOfferClickListener;
import com.bee7.gamewall.interfaces.OnVideoRewardGeneratedListener;
import com.bee7.gamewall.views.Bee7ImageView;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.common.util.SharedPreferencesRewardsHelper;
import com.bee7.sdk.publisher.Publisher;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResult;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

public class VideoDialog extends RelativeLayout {

    private static final String TAG = VideoDialog.class.toString();

    private AppOffer appOffer;
    private AppOfferWithResult appOfferWithResult;
    private Publisher publisher;
    private OnVideoRewardGeneratedListener onVideoRewardGeneratedListener;
    private OnClickListener onCloseClickListener;
    private OnOfferClickListener onOfferClickListener;

    private Bee7ImageView icon;
    private TextView title;
    private TextView description;
    private ProgressBar spinner;
    private LinearLayout ratingsLayout;
    private Bee7ImageView replayIcon;
    private Bee7ImageView closeIcon;
    private Bee7ImageView videoOfferButton;
    private LinearLayout titleLayout;

    private VideoComponent videoComponent;

    private boolean isVisible;

    public VideoDialog(Context context, AppOffer appOffer, AppOfferWithResult appOfferWithResult,
                       long currentProgress, boolean videoMuted,
                       AppOffersModel.VideoPrequalType videoPrequalType, Publisher publisher,
                       OnVideoRewardGeneratedListener onVideoRewardGeneratedListener,
                       OnClickListener onCloseClickListener, OnOfferClickListener onOfferClickListener) {
        super(context);

        this.appOffer = appOffer;
        this.appOfferWithResult = appOfferWithResult;
        this.publisher = publisher;
        this.onVideoRewardGeneratedListener = onVideoRewardGeneratedListener;
        this.onCloseClickListener = onCloseClickListener;
        this.onOfferClickListener = onOfferClickListener;

        init();
    }

    private void init() {
        inflate(getContext(), R.layout.gamewall_video_dialog, this);
        //inflate(getContext(), R.layout.gamewall_video_dialog_fullscreen, this);
        //isFullscreen = true;

        isVisible = true;

        icon = (Bee7ImageView) findViewById(R.id.gamewallGamesListItemIcon);
        title = (TextView) findViewById(R.id.gamewallGamesListItemTitle);
        description = (TextView) findViewById(R.id.gamewallGamesListItemDescription);
        ratingsLayout = (LinearLayout) findViewById(R.id.gamewallGamesListItemRatingLayout);
        spinner = (ProgressBar) findViewById(R.id.gamewallGamesListItemSpinner);
        replayIcon = (Bee7ImageView) findViewById(R.id.replay_icon);
        closeIcon = (Bee7ImageView) findViewById(R.id.close_icon);
        videoOfferButton = (Bee7ImageView) findViewById(R.id.video_offer_button);
        titleLayout = (LinearLayout)findViewById(R.id.gamewallGamesListItemTitleLayout);

        videoComponent = (VideoComponent)findViewById(R.id.video_component);

        videoComponent.setup(appOffer, publisher, onOfferClickListener, onVideoRewardGeneratedListener,
                appOfferWithResult, new VideoComponent.VideoComponentCallbacks() {
                    @Override
                    public void onVideoEnd() {
                        replayIcon.setVisibility(VISIBLE);
                    }

                    @Override
                    public void onVideoStart() {
                        replayIcon.setVisibility(GONE);
                    }

                    @Override
                    public void onHide(View v) {
                        hide(false);
                    }
                });
        videoComponent.setCloseButton(false);

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

        if (appOffer.showUserRatings()) {
            description.setVisibility(GONE);
            ratingsLayout.removeAllViews();
            ratingsLayout.setVisibility(VISIBLE);

            double num = appOffer.getUserRating();
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

        if (title == null) {
            throw new IllegalStateException("GameWallUnit title view must not be null!");
        }

        title.setText(appOffer.getLocalizedName());

        setAppOfferIcon();

        replayIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!com.bee7.sdk.common.util.Utils.isOnline(getContext())) {
                    new DialogNoInternet(getContext()).show();
                } else {
                    if (videoComponent.replayVideo()) {
                        publisher.onVideoReplayEvent(appOffer.getId());
                    }
                }
            }
        });
        closeIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                hide(false);
            }
        });

        videoOfferButton.setOnClickListener(new OnClickListener() {
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

                        onOfferClickListener.onOfferClick(appOffer, appOfferWithResult, true, Publisher.AppOfferStartOrigin.DIALOG_VIDEO_BTN);
                    }
                }
            }
        });
        icon.setOnClickListener(new OnClickListener() {
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

                        onOfferClickListener.onOfferClick(appOffer, appOfferWithResult, true, Publisher.AppOfferStartOrigin.OFFER_ICON);
                    }
                }
            }
        });
        titleLayout.setOnClickListener(new OnClickListener() {
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

                        onOfferClickListener.onOfferClick(appOffer, appOfferWithResult, true, Publisher.AppOfferStartOrigin.OFFER_TEXT);
                    }
                }
            }
        });
        titleLayout.setOnTouchListener(new OnTouchListener() {
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
    }



    private void setAppOfferIcon() {
        if (icon == null || spinner == null) {
            throw new IllegalStateException("GameWallUnit icon view or spinner view must not be null!");
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isVisible = false;
        appOffer = null;
        publisher = null;
        System.gc();
    }

    /**
     * Called from activity onPause
     */
    public void onPause() {
        if (videoComponent != null) {
            videoComponent.onPause();
        }
    }

    /**
     * Called from activity onResume
     */
    public void onResume() {
        if (videoComponent != null) {
            videoComponent.onResume();
        }
    }

    /**
     * Checks status and calls onCloseClickListener.onClick when appropriate
     * @param forceHide if dialog should be closed without checks
     */
    public void hide(boolean forceHide) {
        if (forceHide) {
            if(onCloseClickListener != null) {
                if (videoComponent.isVideoPlaying()) {
                    videoComponent.reportVideoWatchedEvent();
                }
                onCloseClickListener.onClick(this);
            }
        } else {
            if (videoComponent.isCloseNoticeShown()) {
                if(onCloseClickListener != null) {
                    videoComponent.reportVideoWatchedEvent();
                    onCloseClickListener.onClick(this);
                }
            } else {
                boolean rewardAlreadyGiven =
                        new SharedPreferencesRewardsHelper(getContext(), publisher.getAppOffersModel().getVideoPrequalGlobalConfig().getMaxDailyRewardFreq())
                                .hasBeenRewardAlreadyGiven(appOffer.getId(), appOffer.getCampaignId());

                if ((publisher.getAppOffersModel().getVideoPrequaificationlType() == AppOffersModel.VideoPrequalType.INLINE_REWARD ||
                        publisher.getAppOffersModel().getVideoPrequaificationlType() == AppOffersModel.VideoPrequalType.FULLSCREEN_REWARD) &&
                        !rewardAlreadyGiven &&
                        !isCtaShowing()) {
                    videoComponent.showCloseNotice();
                } else {
                    if(onCloseClickListener != null) {
                        if (videoComponent.isVideoPlaying()) {
                            videoComponent.reportVideoWatchedEvent();
                        }
                        onCloseClickListener.onClick(this);
                    }
                }
            }
        }
    }


    @Override
    public boolean isShown() {
        return isVisible;
    }

    public boolean isCtaShowing() {
        return videoComponent.isCtaShowing();
    }

    public boolean isCloseNoticeShown() {
        return videoComponent.isCloseNoticeShown();
    }
}
