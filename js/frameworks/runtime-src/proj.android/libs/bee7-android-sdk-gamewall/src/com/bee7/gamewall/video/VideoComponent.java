package com.bee7.gamewall.video;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bee7.gamewall.GameWallImpl;
import com.bee7.gamewall.GameWallView;
import com.bee7.gamewall.R;
import com.bee7.gamewall.Utils;
import com.bee7.gamewall.assets.AnimFactory;
import com.bee7.gamewall.assets.AssetsManager;
import com.bee7.gamewall.assets.AssetsManagerSetBitmapTask;
import com.bee7.gamewall.assets.UnscaledBitmapLoader;
import com.bee7.gamewall.interfaces.OnOfferClickListener;
import com.bee7.gamewall.interfaces.OnVideoRewardGeneratedListener;
import com.bee7.gamewall.video.exoplayer.ExoVideoPlayer;
import com.bee7.gamewall.views.Bee7ImageView;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.Publisher;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResult;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

import java.net.URL;

public class VideoComponent extends RelativeLayout {

    public interface VideoComponentCallbacks {
        void onVideoEnd();
        void onVideoStart();
        void onHide(View v);
    }

    private static final String TAG = VideoComponent.class.toString();

    private Bee7ImageView ctaImage;
    private FrameLayout ingamewallVideoLayout;
    private ProgressBar progressBar;
    private RelativeLayout controlsLayout;
    private Bee7ImageView videoMute;
    private TextView circleCounter;
    private RelativeLayout closeNoticeLayout;
    private TextView closeNoticeMessage;
    private TextView closeNoticeContinueWatching;
    private RelativeLayout ingamewallCtaLayout;
    private Bee7ImageView videoClose;

    private VideoPlayerInterface videoPlayerInterface;
    private boolean ctaVisible = false;
    private boolean videoVisible = false;
    private boolean isCloseNoticeShown = false;
    private boolean watchedAlreadyReported = false;
    private boolean startAlreadyReported = false;
    private Handler controlsHandler;
    private boolean isFullscreen;

    private AppOffer appOffer;
    private Publisher publisher;
    private OnOfferClickListener onOfferClickListener;
    private OnVideoRewardGeneratedListener onVideoRewardGeneratedListener;
    private VideoComponentCallbacks videoComponentCallbacks;
    private AppOfferWithResult appOfferWithResult;

    public VideoComponent(Context context) {
        super(context);
        init(context);
    }

    public VideoComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoComponent(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public VideoComponent(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.gamewall_video_component, this, true);

        ctaImage = (Bee7ImageView) findViewById(R.id.gamewallGamesListCTAImage);
        ingamewallVideoLayout = (FrameLayout) findViewById(R.id.ingamewall_video_layout);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        controlsLayout = (RelativeLayout)findViewById(R.id.ingamewall_controls_layout);
        videoMute = (Bee7ImageView) findViewById(R.id.ingamewall_video_mute);
        circleCounter = (TextView) findViewById(R.id.ingamewall_video_counter);
        closeNoticeLayout = (RelativeLayout)findViewById(R.id.ingamewall_video_notice_layout);
        closeNoticeMessage = (TextView)findViewById(R.id.ingamewall_video_notice_message);
        closeNoticeContinueWatching = (TextView)findViewById(R.id.ingamewall_video_notice_text);
        ingamewallCtaLayout = (RelativeLayout) findViewById(R.id.ingamewall_cta_layout);
        videoClose = (Bee7ImageView) findViewById(R.id.ingamewall_video_close);

        try {
            String fontFile = getContext().getResources().getString(R.string.bee7_font_file);

            if (com.bee7.sdk.common.util.Utils.hasText(fontFile)) {
                Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontFile);
                closeNoticeMessage.setTypeface(typeface);
                closeNoticeContinueWatching.setTypeface(typeface);
            }
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to load font");
        }

        this.controlsHandler = new Handler();

        if(getContext().getSharedPreferences(AbstractVideoPlayer.PREF_COM_PLAYER, Context.MODE_PRIVATE).getBoolean(AbstractVideoPlayer.PREF_PLAYER_MUTE_CONF_KEY, false)) {
            videoMute.setImageDrawable(getResources().getDrawable(R.drawable.bee7_btn_sound_off));
        } else {
            videoMute.setImageDrawable(getResources().getDrawable(R.drawable.bee7_btn_sound_on));
        }
    }

    public void setup(AppOffer _appOffer, Publisher _publisher, OnOfferClickListener _onOfferClickListener,
                      OnVideoRewardGeneratedListener _onVideoRewardGeneratedListener, AppOfferWithResult _appOfferWithResult,
                      final VideoComponentCallbacks _videoComponentCallbacks) {
        //TODO fetch fullscreen flag from server
        isFullscreen = false;

        this.appOffer = _appOffer;
        this.publisher = _publisher;
        this.onOfferClickListener = _onOfferClickListener;
        this.onVideoRewardGeneratedListener = _onVideoRewardGeneratedListener;
        this.appOfferWithResult = _appOfferWithResult;
        this.videoComponentCallbacks = _videoComponentCallbacks;

        ViewGroup rootView = ((ViewGroup) ((Activity) getContext()).findViewById(android.R.id.content));
        if (isFullscreen) {
            //LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            //        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            //videoPartLayout.setLayoutParams(params);
        } else {
            if (Utils.isPortrate(getContext())) {
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        AnimFactory.getVideoViewHeight(rootView, -(getResources().getDimensionPixelSize(R.dimen.bee7_ingamewall_video_margin_vertical))));
                setLayoutParams(params);
            } else {
                int placeForVideo = 0;
                WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                Display display = wm.getDefaultDisplay();

                Logger.debug("placeForVideo", "height " + display.getHeight());

                placeForVideo = display.getHeight()
                        - getResources().getDimensionPixelSize(R.dimen.bee7_dialog_vertical_margin) //top margin
                        - getResources().getDimensionPixelSize(R.dimen.bee7_dialog_vertical_margin) //bottom margin
                        - getResources().getDimensionPixelSize(R.dimen.bee7_ingamewall_video_margin_vertical) //top padding
                        - getResources().getDimensionPixelSize(R.dimen.bee7_ingamewall_video_margin_vertical) //bottom padding
                        - getResources().getDimensionPixelSize(R.dimen.bee7_video_dialog_icon_size) //offer icon height
                        - getResources().getDimensionPixelSize(R.dimen.bee7_dialog_vertical_item_spacing) //spacing between items
                        - getResources().getDimensionPixelSize(R.dimen.bee7_dialog_dl_button_height) //download button height
                        - getResources().getDimensionPixelSize(R.dimen.bee7_dialog_vertical_item_spacing); //spacing between items

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        AnimFactory.getVideoViewWidth(placeForVideo),
                        placeForVideo);
                setLayoutParams(params);
            }
        }

        videoPlayerInterface = new ExoVideoPlayer(getContext(), appOffer.getVideoUrl(), 0, false, false, new VideoCallbackListener() {
            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
            @Override
            public ViewGroup onSurfaceView(TextureView surfaceView) {
                ingamewallVideoLayout.removeAllViews();
                ingamewallVideoLayout.setAlpha(0.0f);
                ingamewallVideoLayout.addView(surfaceView);
                return ingamewallVideoLayout;
            }

            @Override
            public void onError(String e) {
                Logger.debug("ExoVideoPlayer", "onError " + e);
                publisher.onVideoFailedEvent(appOffer.getId(), e);

                ctaImage.setVisibility(VISIBLE);
                ctaVisible = true;

                videoPlayerInterface = new NativeVideoPlayer(getContext(), appOffer.getVideoUrl(), 0, false, true, new VideoCallbackListener() {
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                    @Override
                    public ViewGroup onSurfaceView(TextureView surfaceView) {
                        ingamewallVideoLayout.removeAllViews();
                        ingamewallVideoLayout.setAlpha(0.0f);
                        ingamewallVideoLayout.addView(surfaceView);
                        return ingamewallVideoLayout;
                    }

                    @Override
                    public void onError(String e) {
                        publisher.onVideoFailedEvent(appOffer.getId(), e);
                        Logger.debug("NativeVideoPlayer", "onError " + e);
                        videoVisible = false;
                    }

                    @Override
                    public void onVideoEnd(int videoPlayed, boolean error) {
                        if (closeNoticeLayout.isShown()) {
                            closeNoticeLayout.setVisibility(GONE);
                            isCloseNoticeShown = false;
                        }

                        reportVideoWatchedEvent();

                        if (appOffer != null
                                && !error
                                && onVideoRewardGeneratedListener != null
                                && videoPlayed >= 98 //if video was watched more than 98%. I choose 98 instead of 100 because I don't trust player position reporting on all devices.
                                //&& !alreadyWatched
                                && (publisher.getAppOffersModel().getVideoPrequaificationlType() == AppOffersModel.VideoPrequalType.FULLSCREEN_REWARD
                                || publisher.getAppOffersModel().getVideoPrequaificationlType() == AppOffersModel.VideoPrequalType.INLINE_REWARD)) {

                            onVideoRewardGeneratedListener.onVideoRewardGenerated(appOffer);
                        }

                        controlsHandler.removeCallbacksAndMessages(null);

                        ctaImage.setVisibility(VISIBLE);
                        ctaVisible = true;

                        if (!error) {
                            if (isFullscreen) {
                                ingamewallCtaLayout.setVisibility(VISIBLE);
                            }
                            ctaImage.setVisibility(VISIBLE);
                            ingamewallVideoLayout.setVisibility(INVISIBLE);
                            controlsLayout.setVisibility(GONE);
                            ctaVisible = true;

                            if (videoComponentCallbacks != null) {
                                videoComponentCallbacks.onVideoEnd();
                            }

                            Animation videoViewHideAnim = AnimFactory.createAlphaHide(ingamewallVideoLayout);
                            videoViewHideAnim.setDuration(AnimFactory.ANIMATION_DURATION_LONG);

                            videoViewHideAnim.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    if (ingamewallVideoLayout != null) {
                                        ingamewallVideoLayout.setVisibility(INVISIBLE);
                                    }
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {
                                }
                            });
                            videoVisible = false;
                            ingamewallVideoLayout.startAnimation(videoViewHideAnim);
                        }

                        ((Activity)getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }

                    @Override
                    public void onVideoStart() {
                        Logger.debug(TAG, "onVideoStart");
                        showTimedControlsVisibility();

                        reportVideoStartEvent();

                        if (videoComponentCallbacks != null) {
                            videoComponentCallbacks.onVideoStart();
                        }

                        if (!videoVisible) {
                            Animation videoViewHideAnim = AnimFactory.createAlphaShow(ingamewallVideoLayout, false);
                            videoViewHideAnim.setDuration(AnimFactory.ANIMATION_DURATION_LONG);
                            videoViewHideAnim.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                    if (ingamewallVideoLayout != null) {
                                        ingamewallVideoLayout.setVisibility(VISIBLE);
                                    }
                                }

                                @Override
                                public void onAnimationEnd(Animation animation) { }

                                @Override
                                public void onAnimationRepeat(Animation animation) { }
                            });
                            videoVisible = true;
                            ingamewallVideoLayout.startAnimation(videoViewHideAnim);
                        }

                        ((Activity)getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }

                    @Override
                    public void onBuffer(boolean buffering) {
                        if (buffering) {
                            //show progress
                            progressBar.setVisibility(View.VISIBLE);
                        } else {
                            //hide progress
                            progressBar.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onTimeToEndUpdate(long progress) {
                        if(circleCounter != null) {
                            circleCounter.setText(String.valueOf(progress));
                        }
                    }
                });
            }

            @Override
            public void onVideoEnd(int videoPlayed, boolean error) {
                if (closeNoticeLayout.isShown()) {
                    closeNoticeLayout.setVisibility(GONE);
                    isCloseNoticeShown = false;
                }

                reportVideoWatchedEvent();

                if (appOffer != null
                        && !error
                        && onVideoRewardGeneratedListener != null
                        && videoPlayed >= 98 //if video was watched more than 98%. I choose 98 instead of 100 because I don't trust player position reporting on all devices.
                        //&& !alreadyWatched
                        && (publisher.getAppOffersModel().getVideoPrequaificationlType() == AppOffersModel.VideoPrequalType.FULLSCREEN_REWARD
                        || publisher.getAppOffersModel().getVideoPrequaificationlType() == AppOffersModel.VideoPrequalType.INLINE_REWARD)) {

                    onVideoRewardGeneratedListener.onVideoRewardGenerated(appOffer);
                }

                controlsHandler.removeCallbacksAndMessages(null);

                if (!error) {
                    if (isFullscreen) {
                        ingamewallCtaLayout.setVisibility(VISIBLE);
                    }
                    ctaImage.setVisibility(VISIBLE);
                    ingamewallVideoLayout.setVisibility(INVISIBLE);
                    controlsLayout.setVisibility(GONE);
                    ctaVisible = true;

                    if (videoComponentCallbacks != null) {
                        videoComponentCallbacks.onVideoEnd();
                    }

                    Animation videoViewHideAnim = AnimFactory.createAlphaHide(ingamewallVideoLayout);
                    videoViewHideAnim.setDuration(AnimFactory.ANIMATION_DURATION_LONG);

                    videoViewHideAnim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            if (ingamewallVideoLayout != null) {
                                ingamewallVideoLayout.setVisibility(INVISIBLE);
                            }
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                    videoVisible = false;
                    ingamewallVideoLayout.startAnimation(videoViewHideAnim);
                }

                ((Activity)getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

            @Override
            public void onVideoStart() {
                showTimedControlsVisibility();

                reportVideoStartEvent();
                if (videoComponentCallbacks != null) {
                    videoComponentCallbacks.onVideoStart();
                }

                if (ingamewallCtaLayout != null) {
                    ingamewallCtaLayout.setVisibility(View.INVISIBLE);
                }

                if (!videoVisible) {
                    Animation videoViewHideAnim = AnimFactory.createAlphaShow(ingamewallVideoLayout, false);
                    videoViewHideAnim.setDuration(AnimFactory.ANIMATION_DURATION_LONG);

                    videoViewHideAnim.setAnimationListener(new Animation.AnimationListener() {
                        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                        @Override
                        public void onAnimationStart(Animation animation) {
                            if (ingamewallVideoLayout != null) {
                                ingamewallVideoLayout.setVisibility(VISIBLE);
                            }
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {  }

                        @Override
                        public void onAnimationRepeat(Animation animation) { }
                    });
                    videoVisible = true;
                    ingamewallVideoLayout.startAnimation(videoViewHideAnim);
                }

                ((Activity)getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

            @Override
            public void onBuffer(boolean buffering) {
                if (buffering) {
                    //show progress
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    //hide progress
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onTimeToEndUpdate(long progress) {
                if(circleCounter != null) {
                    circleCounter.setText(String.valueOf(progress));
                }
            }
        }, publisher.getAppOffersModel().getVideoPrequalGlobalConfig());

        if(!TextUtils.isEmpty(appOffer.getCreativeUrl())) {
            try {
                URL cUrl = new URL(appOffer.getCreativeUrl());

                AssetsManagerSetBitmapTask task = new AssetsManagerSetBitmapTask(cUrl, getContext()) {
                    @Override
                    public void bitmapLoadedPost(Bitmap bitmap) {
                        if (bitmap != null && ctaImage != null) {
                            ctaImage.setImageBitmap(bitmap);
                        } else {
                            setOfferIconAsCreative(appOffer);
                        }
                    }
                };

                AssetsManager.getInstance().runIconTask(task);
            } catch (Exception ignored) {
                setOfferIconAsCreative(appOffer);
            }
        } else {
            setOfferIconAsCreative(appOffer);
        }

        ingamewallVideoLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                synchronized (GameWallView.lastClickSync) {
                    // mis-clicking prevention, using threshold of 1000 ms
                    if ((SystemClock.elapsedRealtime() - GameWallView.lastClickTimestamp) < 1000) {
                        GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();
                        return;
                    }

                    GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();

                    if (onOfferClickListener != null) {
                        onOfferClickListener.onOfferClick(appOffer, appOfferWithResult, true, Publisher.AppOfferStartOrigin.DIALOG_VIDEO);
                    }
                }
            }
        });
        closeNoticeLayout.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                closeNoticeLayout.setVisibility(GONE);
                isCloseNoticeShown = false;
                if (videoPlayerInterface != null) {
                    if (videoPlayerInterface.isVideoAtEnd()) {
                        videoPlayerInterface.seekToVideonEnd(3000);
                    } else {
                        videoPlayerInterface.resumeVideo();
                    }

                }
            }
        });
        closeNoticeContinueWatching.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                closeNoticeLayout.setVisibility(GONE);
                isCloseNoticeShown = false;
                if (videoPlayerInterface != null) {
                    if (videoPlayerInterface.isVideoAtEnd()) {
                        videoPlayerInterface.seekToVideonEnd(3000);
                    } else {
                        videoPlayerInterface.resumeVideo();
                    }

                }
            }
        });
        videoMute.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoPlayerInterface.toggleSound()) {
                    videoMute.setImageDrawable(getResources().getDrawable(R.drawable.bee7_btn_sound_on));
                    reportVideoMuteEvent(false);
                } else {
                    videoMute.setImageDrawable(getResources().getDrawable(R.drawable.bee7_btn_sound_off));
                    reportVideoMuteEvent(true);
                }
            }
        });
        ctaImage.setOnClickListener(new OnClickListener() {
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

                        onOfferClickListener.onOfferClick(appOffer, appOfferWithResult, true, Publisher.AppOfferStartOrigin.DIALOG_VIDEO_IMG_BTN);
                    }
                }
            }
        });
        if (videoClose != null ) {
            videoClose.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (videoComponentCallbacks != null) {
                        videoComponentCallbacks.onHide(videoClose);
                    }
                }
            });
        }
    }

    private void setOfferIconAsCreative(AppOffer appOffer) {
        if (appOffer == null) {
            return;
        }

        UnscaledBitmapLoader.ScreenDPI screenDPI = UnscaledBitmapLoader.ScreenDPI.DENSITY_XXXHDPI; //so we get big icon
        AppOffer.IconUrlSize iconUrlSize = GameWallImpl.getAppOfIconUrlSize(getResources());
        AssetsManagerSetBitmapTask task = new AssetsManagerSetBitmapTask(appOffer.getIconUrl(iconUrlSize), getContext()) {
            @Override
            public void bitmapLoadedPost(Bitmap bitmap) {
                if (ctaImage != null) {
                    ctaImage.setImageBitmap(bitmap);
                }
            }
        };
        task.setSourceImageDPI(screenDPI);
        AssetsManager.getInstance().runIconTask(task);
    }

    private void showTimedControlsVisibility() {
        controlsLayout.setVisibility(VISIBLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            controlsLayout.setAlpha(1f);
        }
        ctaVisible = false;
        videoPlayerInterface.showMediaController();

        controlsHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideControlsVisibility();
            }
        }, DemoUtil.TOGGLE_CONTROLS_TIME);
    }

    private void hideControlsVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            controlsLayout.setAlpha(0.75f);
        }
        ctaVisible = false;
        videoPlayerInterface.hideMediaController();
    }

    public void showCloseNotice() {
        closeNoticeLayout.setVisibility(VISIBLE);
        isCloseNoticeShown = true;
        if (videoPlayerInterface != null) {
            videoPlayerInterface.pauseVideo();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        videoPlayerInterface.onDestroy();
        videoPlayerInterface = null;
        controlsHandler.removeCallbacksAndMessages(null);
        controlsHandler = null;
        appOffer = null;
        publisher = null;
        ctaImage.setImageDrawable(null);
        ctaImage.destroyDrawingCache();
        ctaImage = null;
        ingamewallVideoLayout.removeAllViews();
        ingamewallVideoLayout = null;
        System.gc();
    }

    public void onPause() {
        if (videoPlayerInterface != null) {
            videoPlayerInterface.pauseVideo();
        }
    }

    public void onResume() {
        if (videoPlayerInterface != null) {
            videoPlayerInterface.resumeVideo();
        }
    }

    public boolean replayVideo() {
        watchedAlreadyReported = false;
        startAlreadyReported = false;
        return videoPlayerInterface != null && videoPlayerInterface.replayVideo();
    }

    public int getProgress() {
        if (videoPlayerInterface != null) {
            return videoPlayerInterface.getProgress();
        }
        return 0;
    }

    public boolean isVideoPlaying() {
        return videoPlayerInterface.isVideoPlaying();
    }

    public boolean isCtaShowing() {
        return ctaVisible;
    }

    public boolean isCloseNoticeShown() {
        return isCloseNoticeShown;
    }

    public void reportVideoMuteEvent(boolean mute) {
        publisher.onVideoMuteEvent(appOffer.getId(), mute);
    }

    /**
     * This method triggers video watched event
     */
    public void reportVideoWatchedEvent() {
        if (!watchedAlreadyReported) {
            watchedAlreadyReported = true;
            publisher.onVideoPrequalificationWatched(appOffer.getId(), getProgress(), appOffer.getVideoReward());
        }
    }

    public void reportVideoStartEvent() {
        if (videoPlayerInterface.isVideoPlaying() && !startAlreadyReported) {
            startAlreadyReported = true;
            publisher.onVideoStartEvent(appOffer.getId());
        }
    }

    public void setCloseButton(boolean show) {
        if (show) {
            videoClose.setVisibility(VISIBLE);
        } else {
            videoClose.setVisibility(GONE);
        }
    }

    public void showVideo() {
        if (videoPlayerInterface != null) {
            videoPlayerInterface.showVideo();
        }
    }

    public void remove() {
        if (videoPlayerInterface != null) {
            videoPlayerInterface.stopVideo();
        }
    }
}
