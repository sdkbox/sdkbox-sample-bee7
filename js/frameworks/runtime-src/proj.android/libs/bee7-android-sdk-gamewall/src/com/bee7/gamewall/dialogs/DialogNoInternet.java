package com.bee7.gamewall.dialogs;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.widget.TextView;

import com.bee7.gamewall.R;
import com.bee7.sdk.common.util.Logger;

public class DialogNoInternet extends Bee7Dialog {

    private final static String TAG = DialogNoInternet.class.getName();

    private TextView textNoInternet;
    private TextView buttonOK;

    public DialogNoInternet(Context context, boolean immersiveMode) {
        super(context, immersiveMode);

        setContentView(R.layout.gamewall_dialog_no_internet);

        textNoInternet = (TextView)findViewById(R.id.bee7_dialog_internet_message);
        buttonOK = (TextView)findViewById(R.id.bee7_dialog_internet_ok);

        try {
            String fontFile = getContext().getResources().getString(R.string.bee7_font_file);
            if (com.bee7.sdk.common.util.Utils.hasText(fontFile)) {
                Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontFile);

                textNoInternet.setTypeface(typeface);
                buttonOK.setTypeface(typeface);
            }
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to load font");
        }

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

}
