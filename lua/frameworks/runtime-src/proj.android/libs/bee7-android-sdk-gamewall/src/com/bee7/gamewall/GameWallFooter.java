package com.bee7.gamewall;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bee7.sdk.common.util.Logger;

/**
 * Footer of GameWall
 */
public class GameWallFooter extends LinearLayout {
    private static final String TAG = GameWallFooter.class.getName();

    public GameWallFooter(Context context) {
        super(context);
    }

    public GameWallFooter(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GameWallFooter(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
    }
}
