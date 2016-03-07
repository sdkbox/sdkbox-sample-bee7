package com.bee7.gamewall.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bee7.gamewall.R;
import com.bee7.sdk.common.util.Logger;

/**
 * This tutorial shows once when user first opens gamewall.
 */
public class DialogTutorial extends Bee7Dialog {

    private final static String TAG = DialogTutorial.class.getName();

    private TextView textPlaygames;
    private ImageView iconVirtualCurrency;
    private ImageView iconPublisher;
    private TextView buttonOk;
    private TextView textPart1;
    private TextView textPart2;
    private TextView textPart3;

    private SharedPreferences sharedPreferences;

    public DialogTutorial(Context context) {
        super(context);
        setContentView(R.layout.gamewall_dialog_tutorial);

        textPlaygames = (TextView)findViewById(R.id.bee7_dialog_tutorial_text_playgames);
        textPart1 = (TextView)findViewById(R.id.bee7_dialog_tutorial_part1);
        iconVirtualCurrency = (ImageView)findViewById(R.id.bee7_dialog_tutorial_icon_virtualcurrency);
        textPart2 = (TextView)findViewById(R.id.bee7_dialog_tutorial_part2);
        iconPublisher = (ImageView)findViewById(R.id.bee7_dialog_tutorial_icon_publisher);
        textPart3 = (TextView)findViewById(R.id.bee7_dialog_tutorial_part3);
        buttonOk = (TextView)findViewById(R.id.bee7_dialog_tutorial_text_button);

        sharedPreferences = context.getSharedPreferences(PREF_DIALOG_CONF, Context.MODE_PRIVATE);

        try {
            String fontFile = getContext().getResources().getString(R.string.bee7_font_file);
            if (com.bee7.sdk.common.util.Utils.hasText(fontFile)) {
                Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontFile);

                textPlaygames.setTypeface(typeface);
                textPart1.setTypeface(typeface);
                textPart2.setTypeface(typeface);
                textPart3.setTypeface(typeface);
                buttonOk.setTypeface(typeface);
            }
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to load font");
        }

        try {
            iconPublisher.setImageDrawable(getContext().getPackageManager().getApplicationIcon(getContext().getApplicationInfo()));
        } catch (Exception ignore) { }

        iconVirtualCurrency.setImageDrawable(getContext().getResources().getDrawable(R.drawable.bee7_icon_reward));

        buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharedPreferences.edit().putBoolean(PREF_DIALOG_TUTORIAL_SHOWN, true).apply();
                DialogTutorial.this.dismiss();
            }
        });
    }

    @Override
    public void show() {
        if (!sharedPreferences.getBoolean(PREF_DIALOG_TUTORIAL_SHOWN, false)) {
            super.show();
        }
    }
}
