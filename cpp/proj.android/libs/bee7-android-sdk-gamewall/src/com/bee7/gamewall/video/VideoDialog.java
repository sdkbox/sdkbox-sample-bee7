package com.bee7.gamewall.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bee7.gamewall.GameWallImpl;
import com.bee7.gamewall.GameWallView;
import com.bee7.gamewall.R;
import com.bee7.gamewall.assets.AssetsManager;
import com.bee7.gamewall.assets.AssetsManagerSetBitmapTask;
import com.bee7.gamewall.assets.UnscaledBitmapLoader;
import com.bee7.gamewall.dialogs.Bee7Dialog;
import com.bee7.gamewall.dialogs.DialogNoInternet;
import com.bee7.gamewall.interfaces.OnOfferClickListener;
import com.bee7.gamewall.interfaces.OnVideoRewardGeneratedListener;
import com.bee7.sdk.adunit.exoplayer.ExoVideoPlayer;
import com.bee7.gamewall.views.Bee7ImageView;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.common.util.SharedPreferencesRewardsHelper;
import com.bee7.sdk.publisher.DefaultPublisher;
import com.bee7.sdk.publisher.Publisher;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResult;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

public class VideoDialog extends Bee7Dialog {

    private static final String TAG = VideoDialog.class.toString();

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

    private AppOffer appOffer;
    private AppOfferWithResult appOfferWithResult;
    private Publisher publisher;
    private OnVideoRewardGeneratedListener onVideoRewardGeneratedListener;
    private OnOfferClickListener onOfferClickListener;
    private ExoVideoPlayer.GameWallCallback gameWallCallback;
    private boolean immersiveMode = false;

    public VideoDialog(Context context, boolean immersiveMode) {
        super(context, immersiveMode);
        this.immersiveMode = immersiveMode;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        setContentView(R.layout.gamewall_video_dialog);

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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    }

    public void setup(AppOffer _appOffer, AppOfferWithResult _appOfferWithResult,
                      long currentProgress, boolean videoMuted,
                      AppOffersModel.VideoPrequalType videoPrequalType, Publisher _publisher,
                      OnVideoRewardGeneratedListener onVideoRewardGeneratedListener,
                      OnOfferClickListener _onOfferClickListener,
                      ExoVideoPlayer.GameWallCallback gameWallCallback) {

        this.appOffer = _appOffer;
        this.appOfferWithResult = _appOfferWithResult;
        this.publisher = _publisher;
        this.onVideoRewardGeneratedListener = onVideoRewardGeneratedListener;
        //this.onCloseClickListener = onCloseClickListener;
        this.onOfferClickListener = _onOfferClickListener;
        this.gameWallCallback = gameWallCallback;

        videoComponent.setup(appOffer, onOfferClickListener, onVideoRewardGeneratedListener,
                appOfferWithResult, new VideoComponent.VideoComponentCallbacks() {
                    @Override
                    public void onVideoEnd() {
                        replayIcon.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onVideoStart() {
                        replayIcon.setVisibility(View.GONE);
                    }

                    @Override
                    public void onHide(View v) {
                        hide(false);
                    }

                    @Override
                    public void onVideoFailedEvent(String appId, String error, boolean isVideoDisabled) {
                        publisher.onVideoFailedEvent(appId, error, isVideoDisabled);
                    }

                    @Override
                    public void onVideoMuteEvent(String appId, boolean mute) {
                        publisher.onVideoMuteEvent(appId, mute);
                    }

                    @Override
                    public void onVideoPrequalificationWatched(String appId, int watchedProgress, long rewardGiven) {
                        publisher.onVideoPrequalificationWatched(appId, watchedProgress, rewardGiven, DefaultPublisher.gwUnitId);
                    }

                    @Override
                    public void onVideoPrequalificationEnd(String appId, int watchedProgress, long rewardGiven) {
                        publisher.onVideoPrequalificationWatched(appId, 100, rewardGiven, DefaultPublisher.gwUnitId);
                    }

                    @Override
                    public void onVideoStartEvent(String appId) {
                        publisher.onVideoStartEvent(appId);
                    }

                    @Override
                    public AppOffersModel getAppOffersModel() {
                        return publisher.getAppOffersModel();
                    }
                }, gameWallCallback, immersiveMode, false);
        videoComponent.showCloseButton(false);

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
            description.setVisibility(View.GONE);
            ratingsLayout.removeAllViews();
            ratingsLayout.setVisibility(View.VISIBLE);

            double num = appOffer.getUserRating();
            int numberOfFullStars = (int) num;
            double fractionalPart = num - numberOfFullStars;

            for (int i = 0; i < 5; i++) {
                ImageView imageView = new ImageView(getContext());
                if (i < numberOfFullStars) { //add full star
                    imageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.bee7_star_full));
                } else if (i == numberOfFullStars && fractionalPart > 0) { //add half star
                    imageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.bee7_star_half));
                } else { //add empty star
                    imageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.bee7_star_empty));
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.rightMargin = getContext().getResources().getDimensionPixelSize(R.dimen.bee7_offer_banner_rating_spacing);
                imageView.setLayoutParams(params);
                ratingsLayout.addView(imageView);
            }
        } else {
            ratingsLayout.setVisibility(View.GONE);
            description.setVisibility(View.VISIBLE);
            description.setText(appOffer.getLocalizedDescription());
        }

        if (title == null) {
            throw new IllegalStateException("GameWallUnit title view must not be null!");
        }

        title.setText(appOffer.getLocalizedName());

        setAppOfferIcon();

        replayIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!com.bee7.sdk.common.util.Utils.isOnline(getContext())) {
                    new DialogNoInternet(getContext(), immersiveMode).show();
                } else {
                    if (videoComponent.replayVideo()) {
                        publisher.onVideoReplayEvent(appOffer.getId());
                    }
                }
            }
        });
        closeIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                hide(false);
            }
        });

        videoOfferButton.setOnClickListener(new View.OnClickListener() {
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
        icon.setOnClickListener(new View.OnClickListener() {
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
        titleLayout.setOnClickListener(new View.OnClickListener() {
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
        titleLayout.setOnTouchListener(new View.OnTouchListener() {
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

        AppOffer.IconUrlSize iconUrlSize = GameWallImpl.getAppOfIconUrlSize(getContext().getResources());
        UnscaledBitmapLoader.ScreenDPI screenDPI = UnscaledBitmapLoader.ScreenDPI.parseDensity(getContext().getResources()
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
                        spinner.setVisibility(View.VISIBLE);
                    } else {
                        spinner.setVisibility(View.GONE);
                    }
                } else {
                    spinner.setVisibility(View.GONE);
                }
            }
        };

        task.setParams(appOffer);
        task.setSourceImageDPI(screenDPI);

        AssetsManager.getInstance().runIconTask(task);
    }

    public void hide(boolean forceHide) {
        if (forceHide) {
            //if(onCloseClickListener != null) {
                if (videoComponent.isVideoPlaying()) {
                    videoComponent.reportVideoWatchedEvent();
                }
            dismiss();
                //onCloseClickListener.onClick(this);
            //}
        } else {
            if (videoComponent.isCloseNoticeShown()) {
                //if(onCloseClickListener != null) {
                    videoComponent.reportVideoWatchedEvent();
                //    onCloseClickListener.onClick(this);
                //}
                dismiss();
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
                    //if(onCloseClickListener != null) {
                        if (videoComponent.isVideoPlaying()) {
                            videoComponent.reportVideoWatchedEvent();
                        }
                        //onCloseClickListener.onClick(this);
                    dismiss();
                    //}
                }
            }
        }
    }

    public boolean isCtaShowing() {
        return videoComponent.isCtaShowing();
    }

    public boolean isCloseNoticeShown() {
        return videoComponent.isCloseNoticeShown();
    }

    @Override
    public void onBackPressed() {
        hide(false);
    }

    public View getRootView() {
        return videoComponent;
    }
}
