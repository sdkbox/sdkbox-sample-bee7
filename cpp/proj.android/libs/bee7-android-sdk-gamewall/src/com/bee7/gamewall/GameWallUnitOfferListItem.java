package com.bee7.gamewall;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bee7.gamewall.dialogs.DialogLockedMiniGame;
import com.bee7.gamewall.interfaces.Bee7InnerApp;
import com.bee7.gamewall.interfaces.OnOfferClickListener;
import com.bee7.gamewall.interfaces.OnVideoClickListener;
import com.bee7.gamewall.views.Bee7ImageView;
import com.bee7.sdk.common.util.*;
import com.bee7.sdk.common.util.Utils;
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
    private TextView lockedIconText;
    private boolean immersiveMode = false;

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
        lockedIconText = (TextView)findViewById(R.id.locked_icon);
    }

    public void update(AppOffer _appOffer, OnOfferClickListener _onOfferClickListener, OnVideoClickListener _onVideoClickListener,
                       AppOffersModel.VideoButtonPosition _videoButtonPosition, AppOffersModel.VideoPrequalType _videoPrequaificationlType,
                       int maxDailyRewardFreq, GameWallConfiguration.UnitType _unitType, int index, int indexV, float exchangeRate,
                       boolean immersiveMode) {

        update(_appOffer, maxDailyRewardFreq, _onOfferClickListener, _onVideoClickListener, _videoPrequaificationlType,
                _unitType, _videoButtonPosition, index, indexV, 1);

        this.immersiveMode = immersiveMode;
        this.exchangeRate = exchangeRate;

        try {
            String fontFile = getContext().getResources().getString(R.string.bee7_font_file);
            if (com.bee7.sdk.common.util.Utils.hasText(fontFile)) {
                Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontFile);
                title.setTypeface(typeface);
                if (lockedIconText != null) {
                    lockedIconText.setTypeface(typeface);
                }
            }
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to load font");
        }

        getViewTreeObserver().addOnGlobalLayoutListener(globalListener);

        icon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (videoButton != null) {
                        videoButton.setOnTouchPaddingChange(true);
                    } 
                    if (lockedIconText != null && lockedIconText.getVisibility() == VISIBLE) {
                        setMarginForLockIcon(true);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP ||
                        event.getAction() == MotionEvent.ACTION_CANCEL) {
                    if (videoButton != null) {
                        videoButton.setOnTouchPaddingChange(false);
                    }
                    if (lockedIconText != null && lockedIconText.getVisibility() == VISIBLE) {
                        setMarginForLockIcon(false);
                    }
                }

                return false;
            }
        });

        if (lockedIconText != null) {
            setMarginForLockIcon(false);
        }

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

        videoRewardIcon.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        update(_appOffer);
    }

    private void setMarginForLockIcon(boolean pressed) {
        if (pressed) {
            int totalSize = getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_list_item_icon_size);
            int availableSizeHeight = (totalSize - getResources().getDimensionPixelSize(R.dimen.bee7_locked_minigame_icon_height)) / 2;
            int availableSizeWidth = (totalSize - getResources().getDimensionPixelSize(R.dimen.bee7_locked_minigame_icon_width)) / 2;

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) lockedIconText.getLayoutParams();
            params.setMargins(availableSizeWidth + getResources().getDimensionPixelSize(R.dimen.bee7_touch_effect_offset),
                    availableSizeHeight + getResources().getDimensionPixelSize(R.dimen.bee7_touch_effect_offset), 0, 0);
            lockedIconText.setLayoutParams(params);
        } else {
            int totalSize = getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_list_item_icon_size);
            int availableSizeHeight = (totalSize - getResources().getDimensionPixelSize(R.dimen.bee7_locked_minigame_icon_height)) / 2;
            int availableSizeWidth = (totalSize - getResources().getDimensionPixelSize(R.dimen.bee7_locked_minigame_icon_width)) / 2;

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) lockedIconText.getLayoutParams();
            params.setMargins(availableSizeWidth, availableSizeHeight, 0, 0);
            lockedIconText.setLayoutParams(params);
        }
    }

    ViewTreeObserver.OnGlobalLayoutListener globalListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                getViewTreeObserver().removeOnGlobalLayoutListener(globalListener);
            } else {
                getViewTreeObserver().removeGlobalOnLayoutListener(globalListener);
            }

            int margin = getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_list_item_text_horizontal_padding);

            title.setVisibility(INVISIBLE);
            LayoutParams params = new LayoutParams(
                    title.getWidth() - (2 * margin),
                    title.getHeight());
            params.setMargins(margin, 0, margin, 0);

            title.setLayoutParams(params); //We set LayoutParams by hand so it knows its bounds when ellipsize happens

            Logger.debug(TAG, "onGlobalLayout title.setText " + appOffer.getLocalizedName());
            title.setText(appOffer.getLocalizedName());
            title.requestLayout();
            title.invalidate();

            title.post(new Runnable() { //we use runnable since textview it not finished with setting at this point
                @Override
                public void run() {
                    Logger.debug(TAG, "onGlobalLayout post run title.setText " + getEllipsisedText(title));
                    title.setText(getEllipsisedText(title)); //We set text again this time with knowing how much lines the text uses (1 or 2)
                    title.setVisibility(VISIBLE);
                }
            });
        }
    };

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
            if (com.bee7.sdk.common.util.Utils.canVideoBePlayed(getContext(), appOffer, videoPrequaificationlType)) {

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

        if (appOffer instanceof Bee7InnerApp) {
            final Bee7InnerApp innerApp = (Bee7InnerApp)appOffer;

            if (innerApp.isLocked()) {
                lockedIconText.setVisibility(VISIBLE);
                lockedIconText.setText(String.valueOf(innerApp.getLockLevel()));

                Drawable drawable = innerApp.getIcon().getConstantState().newDrawable();
                drawable.mutate();
                drawable.setColorFilter(getResources().getColor(R.color.bee7_locked_offer_overlay_color), PorterDuff.Mode.SRC_ATOP);

                icon.setImageDrawable(drawable);

                OnClickListener lockedMiniGameClickListener = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if ((SystemClock.elapsedRealtime() - GameWallView.lastClickTimestamp) < 1000) {
                            GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();
                            return;
                        }
                        GameWallView.lastClickTimestamp = SystemClock.elapsedRealtime();

                        DialogLockedMiniGame dialogLockedMiniGame = new DialogLockedMiniGame(getContext(), immersiveMode);
                        dialogLockedMiniGame.show(innerApp.getLockLevel(), innerApp.getIcon());
                        innerApp.lockedMiniGameClicked();
                    }
                };
                icon.setOnClickListener(lockedMiniGameClickListener);
                title.setOnClickListener(lockedMiniGameClickListener);
                videoButton.setOnClickListener(lockedMiniGameClickListener);
            } else {
                lockedIconText.setVisibility(GONE);
            }

        }
    }

    @Override
    public void update() {
        update(appOffer);
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
                    if (Utils.canVideoBePlayed(getContext(), appOffer, videoPrequaificationlType)) {

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

    public String getEllipsisedText(TextView textView) {
        String text = textView.getText().toString();
        Logger.debug(TAG, "getEllipsisedText " + text);

        int lines = textView.getLineCount();
        if (lines <= 1) {
            Logger.debug(TAG, "getEllipsisedText returning " + text);
            return text;
        }
        int width = textView.getWidth();
        int len = text.length();
        TextUtils.TruncateAt where = TextUtils.TruncateAt.END;
        TextPaint paint = textView.getPaint();

        StringBuffer result = new StringBuffer();

        int spos = 0, cnt, tmp, hasLines = 0;

        while(hasLines < lines - 1) {
            cnt = paint.breakText(text, spos, len, true, width, null);
            if(cnt >= len - spos) {
                result.append(text.substring(spos));
                break;
            }

            tmp = text.lastIndexOf('\n', spos + cnt - 1);

            if(tmp >= 0 && tmp < spos + cnt) {
                result.append(text.substring(spos, tmp + 1));
                spos += tmp + 1;
            }
            else {
                tmp = text.lastIndexOf(' ', spos + cnt - 1);
                if(tmp >= spos) {
                    result.append(text.substring(spos, tmp + 1));
                    spos += tmp + 1;
                }
                else {
                    result.append(text.substring(spos, cnt));
                    spos += cnt;
                }
            }

            hasLines++;
        }

        if(spos < len) {
            result.append(TextUtils.ellipsize(text.subSequence(spos, len), paint, (float)width, where));
        }

        Logger.debug(TAG, "getEllipsisedText returning " + result.toString());
        return result.toString();
    }
}
