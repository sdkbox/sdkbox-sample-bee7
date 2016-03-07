package com.bee7.gamewall.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bee7.gamewall.R;
import com.bee7.sdk.common.util.Logger;

public class DialogReward extends Bee7Dialog {

    private final static String TAG = DialogReward.class.getName();

    private TextView textRewardAmount;
    private ImageView iconReward;
    private ImageView iconAdvertiser;
    private ImageView iconVirtualCurrency;
    private ImageView iconPublisher;
    private TextView buttonOk;
    private LinearLayout dialogContainer;

    private TextView textPart1;
    private TextView textPart2;
    private TextView textPart3;
    private TextView textPart4;
    private TextView textPart5;
    private TextView textPart6;

    private SharedPreferences sharedPreferences;

    public DialogReward(Context context, boolean videoReward) {
        super(context);

        sharedPreferences = context.getSharedPreferences(PREF_DIALOG_CONF, Context.MODE_PRIVATE);

        if (sharedPreferences.getBoolean(PREF_DIALOG_REWARD_TUTORIAL_SHOWN, false) || videoReward) {
            setContentView(R.layout.gamewall_dialog_reward);

            iconAdvertiser = (ImageView)findViewById(R.id.bee7_dialog_reward_icon_advertiser);
            textPart1 = (TextView)findViewById(R.id.bee7_dialog_reward_text_part1);
            textRewardAmount = (TextView)findViewById(R.id.bee7_dialog_reward_text_amount);
            iconReward = (ImageView)findViewById(R.id.bee7_dialog_reward_icon_amount_virtualcurrency);
            buttonOk = (TextView)findViewById(R.id.bee7_dialog_reward_text_button);
            dialogContainer = (LinearLayout)findViewById(R.id.dialog_container);

            try {
                String fontFile = getContext().getResources().getString(R.string.bee7_font_file);
                if (com.bee7.sdk.common.util.Utils.hasText(fontFile)) {
                    Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontFile);

                    textPart1.setTypeface(typeface);
                    textRewardAmount.setTypeface(typeface);
                    if (buttonOk != null) {
                        buttonOk.setTypeface(typeface);
                    }
                }
            } catch (Exception ex) {
                Logger.debug(TAG, ex, "Failed to load font");
            }

        } else {
            setContentView(R.layout.gamewall_dialog_reward_tutorial);

            textPart1 = (TextView)findViewById(R.id.bee7_dialog_reward_text_part1);
            textRewardAmount = (TextView)findViewById(R.id.bee7_dialog_reward_text_amount);
            iconReward = (ImageView)findViewById(R.id.bee7_dialog_reward_icon_amount_virtualcurrency);
            textPart2 = (TextView)findViewById(R.id.bee7_dialog_reward_text_part2);
            iconAdvertiser = (ImageView)findViewById(R.id.bee7_dialog_reward_icon_advertiser);
            textPart4 = (TextView)findViewById(R.id.bee7_dialog_reward_text_part4);
            iconVirtualCurrency = (ImageView)findViewById(R.id.bee7_dialog_reward_icon_virtualcurrency);
            textPart5 = (TextView)findViewById(R.id.bee7_dialog_reward_text_part5);
            iconPublisher = (ImageView)findViewById(R.id.bee7_dialog_reward_icon_publisher);
            buttonOk = (TextView)findViewById(R.id.bee7_dialog_reward_text_button);
            textPart3 = (TextView)findViewById(R.id.bee7_dialog_reward_text_part3);
            textPart6 = (TextView)findViewById(R.id.bee7_dialog_reward_text_part6);
            dialogContainer = (LinearLayout)findViewById(R.id.dialog_container);

            try {
                String fontFile = getContext().getResources().getString(R.string.bee7_font_file);
                if (com.bee7.sdk.common.util.Utils.hasText(fontFile)) {
                    Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontFile);

                    textPart1.setTypeface(typeface);
                    textRewardAmount.setTypeface(typeface);
                    textPart2.setTypeface(typeface);
                    textPart3.setTypeface(typeface);
                    textPart4.setTypeface(typeface);
                    if (buttonOk != null) {
                        buttonOk.setTypeface(typeface);
                    }
                    textPart5.setTypeface(typeface);
                    textPart6.setTypeface(typeface);
                }
            } catch (Exception ex) {
                Logger.debug(TAG, ex, "Failed to load font");
            }

            sharedPreferences.edit().putBoolean(PREF_DIALOG_REWARD_TUTORIAL_SHOWN, true).apply();
        }

        if (buttonOk != null){
            buttonOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
        if (dialogContainer != null) {
            dialogContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }

    }


    public void show(String rewardAmount, Bitmap advertiserIcon, Drawable vcIcon, Drawable publisherIcon) {
        if (!TextUtils.isEmpty(rewardAmount)) {
            textRewardAmount.setText(rewardAmount);
        }

        if (advertiserIcon != null) {
            iconAdvertiser.setImageBitmap(advertiserIcon);
        }
        if (vcIcon != null) {
            iconReward.setImageDrawable(vcIcon);
        }

        try {
            if (vcIcon != null) {
                iconVirtualCurrency.setImageDrawable(vcIcon);
            }
            if (publisherIcon != null) {
                iconPublisher.setImageDrawable(publisherIcon);
            }
        } catch (Exception ignore) {}

        show();
    }
}