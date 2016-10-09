package com.bee7.gamewall.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

/**
 * Different to AutoResizeEdit. Makes any text scale down to fit the predefined height and width of this view. Left and
 * right padding set the padding. The largest text size is defined with the text size attribute.
 *
 * @author Mihec
 *
 */
public class AutoResizeSingleLineTextView extends TextView implements TextWatcher {

    private Paint mTestPaint = new Paint();
    private boolean needResize = true;

    public AutoResizeSingleLineTextView(Context context) {
        super(context);
        addTextChangedListener(this);
    }

    public AutoResizeSingleLineTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        addTextChangedListener(this);
    }

    public AutoResizeSingleLineTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        addTextChangedListener(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        setSingleLine();
    }

    /**
     * Resizes text after measuring.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        calculateTextSize(getMeasuredWidth());
    }

    public void calculateTextSize(int viewWidth) {
        if (!needResize) {
            // don't resize if nothing changed
            return;
        }
        needResize = false;

        resizeText(viewWidth);
        int iconSpace = scaleCompoundDrawable();
        repositionCompoundDrawable(viewWidth, iconSpace);
    }

    private void repositionCompoundDrawable(int viewWidth, int iconSpace) {
        if (iconSpace == 0) {
            return;
        }
        Rect bounds = new Rect();
        getPaint().getTextBounds(getText().toString(), 0, getText().length(), bounds);

        int drawablePadding = (int) (viewWidth - getPaddingLeft() - (1.05f * bounds.width()) - getCompoundDrawablePadding() - iconSpace);
        setPadding(getPaddingLeft(), getPaddingTop(), drawablePadding, getPaddingBottom());
    }

    private int scaleCompoundDrawable() {
        if (getCompoundDrawables()[2] == null) {
            return 0;
        }
        BitmapDrawable drawable = (BitmapDrawable) getCompoundDrawables()[2];
        Bitmap original = drawable.getBitmap();
        float aspectRatio = original.getHeight() / (float) original.getWidth();

        int height = (int) getTextSize();
        int width = Math.round(height / aspectRatio);
        if (width > 0 && height > 0) {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(original, width, height, false);
            Drawable scaledDrawable = new BitmapDrawable(getResources(), scaledBitmap);
            setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
            setCompoundDrawablesWithIntrinsicBounds(null, null, scaledDrawable, null);
            return width;
        }
        return 0;
    }

    private void resizeText(int textSpace) {
        float width;
        if (getCompoundDrawables()[2] == null) {
            width = textSpace - (getPaddingLeft() + getPaddingRight());
        } else {
            width = (0.85f * textSpace) - (getPaddingLeft() + getPaddingRight());
        }

        mTestPaint.set(this.getPaint());

        float baseTextWidth = mTestPaint.measureText(getText().toString());
        if (baseTextWidth <= width) {
            // if current text size isn't too big for it's view, no resizing needed, return
            return;
        }

        int newSize = (int) (width / baseTextWidth * getTextSize() * 0.99f);
        super.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);
    }

    /**
     * If the text view size changed, set the force resize flag to true.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw || h != oldh) {
//            needResize = true;
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        needResize = true;
    }
}
