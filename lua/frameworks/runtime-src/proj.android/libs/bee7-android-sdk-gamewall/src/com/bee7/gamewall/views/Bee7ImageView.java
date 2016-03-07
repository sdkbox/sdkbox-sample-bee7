package com.bee7.gamewall.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.bee7.gamewall.R;
import com.bee7.sdk.common.util.Logger;

public class Bee7ImageView extends ImageView {

    public interface OnBackgroundPaddingChangedListener {
        void onPaddingSet(boolean set);
    }

    private int initialPaddingLeft = 0;
    private int initialPaddingTop = 0;
    private int initialPaddingRight = 0;
    private int initialPaddingBottom = 0;
    private int offset = 0;
    private OnBackgroundPaddingChangedListener onBackgroundPaddingChangedListener;

    public Bee7ImageView(Context context) {
        super(context);
        offset = getResources().getDimensionPixelSize(R.dimen.bee7_touch_effect_offset);
    }

    public Bee7ImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        offset = getResources().getDimensionPixelSize(R.dimen.bee7_touch_effect_offset);
    }

    public Bee7ImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        offset = getResources().getDimensionPixelSize(R.dimen.bee7_touch_effect_offset);
    }

    public void setOnBackgroundPaddingChangedListener(OnBackgroundPaddingChangedListener onBackgroundPaddingChangedListener) {
        this.onBackgroundPaddingChangedListener = onBackgroundPaddingChangedListener;
    }

    public void setOnTouchPaddingChange(boolean set) {
        if (set) {
            initialPaddingLeft = getPaddingLeft();
            initialPaddingTop = getPaddingTop();
            initialPaddingRight = getPaddingRight();
            initialPaddingBottom = getPaddingBottom();
            
            setPadding(offset + initialPaddingLeft, offset + initialPaddingTop, initialPaddingRight, initialPaddingBottom);
            invalidate();
        } else {
            setPadding(initialPaddingLeft, initialPaddingTop, initialPaddingRight, initialPaddingBottom);
            invalidate();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            setOnTouchPaddingChange(true);

            if (onBackgroundPaddingChangedListener != null) {
                onBackgroundPaddingChangedListener.onPaddingSet(true);
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP ||
                event.getAction() == MotionEvent.ACTION_CANCEL) {
            setOnTouchPaddingChange(false);

            if (onBackgroundPaddingChangedListener != null) {
                onBackgroundPaddingChangedListener.onPaddingSet(false);
            }
        }

        return super.onTouchEvent(event);
    }
}
