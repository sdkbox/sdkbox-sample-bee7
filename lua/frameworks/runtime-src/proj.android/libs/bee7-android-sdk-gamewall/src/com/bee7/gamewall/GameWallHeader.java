package com.bee7.gamewall;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bee7.gamewall.views.AutoResizeSingleLineTextView;
import com.bee7.sdk.common.util.Logger;

/**
 * Header of GameWall
 */
public class GameWallHeader extends RelativeLayout {
    private static final String TAG = GameWallHeader.class.getName();

    private AutoResizeSingleLineTextView mTitle;
    private ImageView icon;
    private LinearLayout titleLayout;

    public GameWallHeader(Context context) {
        super(context);
    }

    public GameWallHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GameWallHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mTitle = (AutoResizeSingleLineTextView)findViewById(R.id.gamewallHeaderTitleView);
        mTitle.setText(mTitle.getText().toString().toUpperCase());
        icon = (ImageView)findViewById(R.id.gamewallHeaderIconReward);
        titleLayout = (LinearLayout)findViewById(R.id.layout1);

        mTitle.post(new Runnable() {
            @Override
            public void run() {
                int offset = getResources().getDimensionPixelSize(R.dimen.bee7_gamewall_header_textandicon_offset);

                int spaceAvailable = getWidth() - offset;

                if (mTitle.getWidth() > spaceAvailable) {
                    mTitle.setWidth(spaceAvailable);
                }

                Rect bounds = new Rect();
                mTitle.getPaint().getTextBounds(mTitle.getText().toString(), 0, mTitle.getText().toString().length(), bounds);
                Logger.debug(TAG, "icon " + bounds.width() + " " + bounds.height());

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) icon.getLayoutParams();
                params.height = bounds.height();
                params.width = bounds.height();
                icon.setLayoutParams(params);

                icon.post(new Runnable() {
                    @Override
                    public void run() {
                        mTitle.requestLayout();
                        titleLayout.setVisibility(VISIBLE);
                    }
                });
            }
        });

//        mTitle.getAutofitHelper().addOnTextSizeChangeListener(new AutofitHelper.OnTextSizeChangeListener() {
//            @Override
//            public void onTextSizeChange(float textSize, float oldTextSize) {
//                Rect bounds = new Rect();
//                mTitle.getPaint().getTextBounds(mTitle.getText().toString(), 0, mTitle.getText().toString().length(), bounds);
//                Logger.debug(TAG, "icon " + bounds.width() + " " + bounds.height());
//
//                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) icon.getLayoutParams();
//                params.height = bounds.height();
//                params.width = bounds.height();
//                icon.setLayoutParams(params);
//
//                icon.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        mTitle.requestLayout();
//                        titleLayout.setVisibility(VISIBLE);
//                    }
//                });
//            }
//        });

        try {
            String fontFile = getContext().getResources().getString(R.string.bee7_title_font_file);
            if (com.bee7.sdk.common.util.Utils.hasText(fontFile)) {
                Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/" + fontFile);
                mTitle.setTypeface(typeface);
                mTitle.setIncludeFontPadding(false);
            }
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to load font");
        }
    }
}
