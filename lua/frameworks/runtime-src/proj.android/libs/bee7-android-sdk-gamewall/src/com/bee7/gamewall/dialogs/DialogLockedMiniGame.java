package com.bee7.gamewall.dialogs;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bee7.gamewall.R;
import com.bee7.sdk.common.util.Logger;

public class DialogLockedMiniGame extends Bee7Dialog {

    private final static String TAG = DialogLockedMiniGame.class.getName();

    private ImageView iconMiniGame;
    private TextView buttonOk;
    private LinearLayout dialogContainer;
    private RelativeLayout backgroundLayout;

    private TextView dialogText;

    public DialogLockedMiniGame(Context context, boolean immersiveMode) {
        super(context, immersiveMode);

        setContentView(R.layout.gamewall_dialog_locked_minigame);

        iconMiniGame = (ImageView)findViewById(R.id.bee7_dialog_locked_minigame_icon);
        buttonOk = (TextView)findViewById(R.id.bee7_dialog_reward_text_button);
        dialogText = (TextView)findViewById(R.id.bee7_dialog_locked_minigame_text_part1);
        dialogContainer = (LinearLayout)findViewById(R.id.dialog_container);
        backgroundLayout = (RelativeLayout)findViewById(R.id.dialog_locked_minigame_background);

        try {
            String fontFile = getContext().getResources().getString(R.string.bee7_font_file);
            if (com.bee7.sdk.common.util.Utils.hasText(fontFile)) {
                Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontFile);

                dialogText.setTypeface(typeface);
                if (buttonOk != null) {
                    buttonOk.setTypeface(typeface);
                }
            }
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to load font");
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
        if (backgroundLayout != null) {
            backgroundLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
        }
    }

    public void show(int level, Drawable miniGameIcon) {

        String string = String.format(getContext().getResources().getString(R.string.bee7_dialog_locked_minigame_text), level);
        dialogText.setText(string);

        if (iconMiniGame != null) {
            iconMiniGame.setImageDrawable(miniGameIcon);
        }

        show();
    }
}