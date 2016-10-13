package com.bee7.gamewall.views;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import com.bee7.gamewall.interfaces.GamewallHeaderCallbackInterface;
import com.bee7.gamewall.interfaces.OnLayout;
import com.bee7.sdk.common.util.Logger;

public class Bee7FrameLayout extends FrameLayout {

    private OnLayout onLayout;

    public Bee7FrameLayout(Context context) {
        super(context);
        init();
    }

    public Bee7FrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Bee7FrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (getMeasuredHeight() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }

                    if (onLayout != null) {
                        onLayout.onHeight(getMeasuredHeight());
                        onLayout.onWidth(getMeasuredWidth());
                    }
                }
            }
        });
    }

    public void setOnLayout(OnLayout onLayout) {
        this.onLayout = onLayout;
    }

    @Override
    public void addView(View child) {
        super.addView(child);
    }

}
