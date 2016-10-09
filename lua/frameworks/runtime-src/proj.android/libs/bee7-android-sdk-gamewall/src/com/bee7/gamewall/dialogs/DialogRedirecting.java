package com.bee7.gamewall.dialogs;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bee7.gamewall.GameWallImpl;
import com.bee7.gamewall.R;
import com.bee7.gamewall.assets.AssetsManager;
import com.bee7.gamewall.assets.AssetsManagerSetBitmapTask;
import com.bee7.gamewall.assets.UnscaledBitmapLoader;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.appoffer.AppOffer;

import java.net.FileNameMap;

public class DialogRedirecting extends Bee7Dialog {

    private final static String TAG = DialogRedirecting.class.getName();

    private int numOfSeconds;

    private TextView textCountDown;
    private ImageView iconAdvertiser;
    private ImageView iconVirtualCurrency;
    private ImageView iconPublisher;
    private LinearLayout redirectLinerLayout;
    private LinearLayout redirectLinerLayout2;
    private LinearLayout redirectLinerLayout3temp;
    private ImageView redirectArrow;
    private LinearLayout redirectingLayout;

    private TextView textPart1;
    private TextView textPart2;
    private TextView textPart3;
    private TextView textPart4;
    private TextView textPart5;
    private TextView redirectText;

    private Handler handler = new Handler();

    public DialogRedirecting(Context context, AppOffer appOffer, boolean tutorialEnabled, int timeout) {
        super(context, false);
        init(context, appOffer, tutorialEnabled, timeout);
    }

    public DialogRedirecting(Context context, AppOffer appOffer, boolean tutorialEnabled, int timeout, boolean immersiveMode) {
        super(context, immersiveMode);
        init(context, appOffer, tutorialEnabled, timeout);
    }

    private void init(Context context, AppOffer appOffer, boolean tutorialEnabled, int timeout) {
        if (tutorialEnabled) {
            numOfSeconds = timeout / 1000;

            setContentView(R.layout.gamewall_dialog_redirect);

            textCountDown = (TextView)findViewById(R.id.bee7_dialog_redirect_text_countdown);
            iconAdvertiser = (ImageView)findViewById(R.id.bee7_dialog_redirect_icon_advertiser);
            iconVirtualCurrency = (ImageView)findViewById(R.id.bee7_dialog_redirect_icon_virtualcurrency);
            iconPublisher = (ImageView)findViewById(R.id.bee7_dialog_redirect_icon_publisher);
            textPart1 = (TextView)findViewById(R.id.bee7_dialog_redirect_part1);
            textPart2 = (TextView)findViewById(R.id.bee7_dialog_redirect_part2);
            textPart3 = (TextView)findViewById(R.id.bee7_dialog_redirect_part3);
            textPart4 = (TextView)findViewById(R.id.bee7_dialog_redirect_part4);
            textPart5 = (TextView)findViewById(R.id.bee7_dialog_redirect_part5);
            redirectLinerLayout = (LinearLayout)findViewById(R.id.bee7_dialog_redirect_layout);
            redirectLinerLayout2 = (LinearLayout)findViewById(R.id.bee7_dialog_redirect_layout2);
            redirectLinerLayout3temp = (LinearLayout)findViewById(R.id.bee7_dialog_redirect_layout3);
            redirectArrow = (ImageView)findViewById(R.id.bee7_dialog_redirect_arrow);
            redirectingLayout = (LinearLayout)findViewById(R.id.bee7_dialog_redirecting_redirecting_layout);
            redirectText = (TextView)findViewById(R.id.bee7_dialog_redirect_text);

            if (TextUtils.isEmpty(getContext().getResources().getString(R.string.bee7_dialog_redirecting_part1))) {
                textPart1.setVisibility(View.GONE);
            }
            if (TextUtils.isEmpty(getContext().getResources().getString(R.string.bee7_dialog_redirecting_part2))) {
                textPart2.setVisibility(View.GONE);
            }

            try {
                String fontFile = getContext().getResources().getString(R.string.bee7_font_file);
                if (com.bee7.sdk.common.util.Utils.hasText(fontFile)) {
                    Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontFile);

                    textCountDown.setTypeface(typeface);
                    textPart1.setTypeface(typeface);
                    textPart2.setTypeface(typeface);
                    textPart3.setTypeface(typeface);
                    textPart3.setTypeface(typeface);
                    textPart4.setTypeface(typeface);
                    textPart5.setTypeface(typeface);
                    redirectText.setTypeface(typeface);
                }
            } catch (Exception ex) {
                Logger.debug(TAG, ex, "Failed to load font");
            }

            setAppOfferIcon(appOffer);

            try {
                iconPublisher.setImageDrawable(getContext().getPackageManager().getApplicationIcon(getContext().getApplicationInfo()));
            } catch (Exception ignore) { }

            iconVirtualCurrency.setImageDrawable(getContext().getResources().getDrawable(R.drawable.bee7_icon_reward));

            startProgressUpdateReporting();

        } else {

            setContentView(R.layout.gamewall_dialog_redirect_spinner);
        }
    }

    private void startProgressUpdateReporting() {
        handler.postDelayed(new Runnable(){
            public void run() {
                if (numOfSeconds <= 0) {
                    textCountDown.setVisibility(View.INVISIBLE);
                    redirectLinerLayout.setVisibility(View.INVISIBLE);
                    redirectLinerLayout2.setVisibility(View.INVISIBLE);
                    if (redirectLinerLayout3temp != null) {
                        redirectLinerLayout3temp.setVisibility(View.INVISIBLE);
                    }
                    redirectArrow.setVisibility(View.INVISIBLE);
                    redirectingLayout.setVisibility(View.VISIBLE);

                    ImageView anim = (ImageView)findViewById(R.id.bee7_dialog_redirect_img_redirecting);
                    final AnimationDrawable myAnimationDrawable = (AnimationDrawable) anim.getDrawable();
                    anim.post(new Runnable() {
                        @Override
                        public void run() {
                            myAnimationDrawable.start();
                        }
                    });

                    stopProgressUpdateReporting();
                    return;
                }
                textCountDown.setText(String.valueOf(numOfSeconds));

                numOfSeconds--;
                handler.postDelayed(this, 1000);
            }
        }, 0);
    }

    private void stopProgressUpdateReporting() {
        handler.removeCallbacksAndMessages(null);
    }

    protected void setAppOfferIcon(AppOffer appOffer) {
        if (appOffer == null) {
            return;
        }

        AppOffer.IconUrlSize iconUrlSize = GameWallImpl.getAppOfIconUrlSize(getContext().getResources());
        UnscaledBitmapLoader.ScreenDPI screenDPI = UnscaledBitmapLoader.ScreenDPI.parseDensity(getContext().getResources()
                .getString(R.string.bee7_gamewallSourceIconDPI));

        AssetsManagerSetBitmapTask task = new AssetsManagerSetBitmapTask(appOffer.getIconUrl(iconUrlSize), getContext()) {
            @Override
            public void bitmapLoadedPost(Bitmap bitmap) {
                if (iconAdvertiser == null) {
                    Logger.warn("DialogRedirecting", "icon == null");
                    return;
                }
                iconAdvertiser.setImageBitmap(bitmap);
            }
        };

        task.setParams(appOffer);
        task.setSourceImageDPI(screenDPI);

        AssetsManager.getInstance().runIconTask(task);
    }

}