package com.bee7.gamewall;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bee7.gamewall.interfaces.GamewallHeaderCallbackInterface;
import com.bee7.gamewall.views.AutoResizeSingleLineTextView;
import com.bee7.gamewall.views.Bee7ImageView;
import com.bee7.sdk.common.util.Logger;

/**
 * Header of GameWall
 */
public class GameWallHeader extends RelativeLayout {
    private static final String TAG = GameWallHeader.class.getName();

    private AutoResizeSingleLineTextView mTitle;
    private LinearLayout titleLayout;
    private Bee7ImageView closeIcon;
    protected boolean resizeTextViewOnConfigurationChanged;
    private GamewallHeaderCallbackInterface gamewallHeaderCallbackInterface;

    public GameWallHeader(Context context, GamewallHeaderCallbackInterface gamewallHeaderCallbackInterface) {
        super(context);
        this.gamewallHeaderCallbackInterface = gamewallHeaderCallbackInterface;
        resizeTextViewOnConfigurationChanged = true;
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.gamewall_header, this);

        mTitle = (AutoResizeSingleLineTextView)findViewById(R.id.gamewallHeaderTitleView);
        mTitle.setText(mTitle.getText().toString().toUpperCase());
        titleLayout = (LinearLayout)findViewById(R.id.layout1);
        closeIcon = (Bee7ImageView)findViewById(R.id.gamewallHeaderButtonClose);

        try {
            String fontFile = getContext().getResources().getString(R.string.bee7_title_font_file);
            if (com.bee7.sdk.common.util.Utils.hasText(fontFile)) {
                Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontFile);
                mTitle.setTypeface(typeface);
                mTitle.setIncludeFontPadding(false);
                mTitle.invalidate();
            }
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to load font");
        }

        calculateTextSize();

        closeIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gamewallHeaderCallbackInterface != null) {
                    gamewallHeaderCallbackInterface.onClose();
                }
            }
        });
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        calculateTextSize();
    }

    private void calculateTextSize() {
        if (resizeTextViewOnConfigurationChanged) {

            int offset = getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_header_textandicon_offset);// how far should text be from right edge

            mTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_header_text_size));
            mTitle.setIncludeFontPadding(false);

            DisplayMetrics displaymetrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(displaymetrics);
            int textViewWidth = displaymetrics.widthPixels - offset;
            LinearLayout.LayoutParams params3 = (LinearLayout.LayoutParams) mTitle.getLayoutParams();
            params3.width = textViewWidth;
            mTitle.setLayoutParams(params3);

            mTitle.post(new Runnable() {
                @Override
                public void run() {
                    titleLayout.setVisibility(VISIBLE);
                }
            });
        }
    }
}
