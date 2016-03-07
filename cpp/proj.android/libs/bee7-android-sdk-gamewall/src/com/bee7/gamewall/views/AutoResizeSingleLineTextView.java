package com.bee7.gamewall.views;

import android.content.Context;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * Different to AutoResizeEdit. Makes any text scale down to fit the predefined height and width of this view. Left and
 * right padding set the padding. The largest text size is defined with the text size attribute.
 * 
 * @author Mihec
 * 
 */
public class AutoResizeSingleLineTextView extends TextView implements TextWatcher {

    //    private static final float THRESHOLD = 0.5f;
    //    private static final int MAX_ITERATIONS = 30;

    private Paint mTestPaint = new Paint();
    //    private float max = -1.0f;
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

    private void calculateTextSize(int viewWidth) {
        if (!needResize) {
            // don't resize if nothing changed
            return;
        }
        needResize = false;

        float width = viewWidth - (getPaddingLeft() + getPaddingRight());
        //        mTestPaint = new Paint();
        mTestPaint.set(this.getPaint());

        float baseTextWidth = mTestPaint.measureText(getText().toString());
        if (baseTextWidth <= width) {
            // if current text size isn't too big for it's view, no resizing needed, return
            return;
        }

//        float newSize = width / baseTextWidth * getTextSize();
        int newSize = (int) (width / baseTextWidth * getTextSize() * 0.99f);
//        int newSize = (int) (width / baseTextWidth * getTextSize());
        this.setTextSize(TypedValue.COMPLEX_UNIT_PX, newSize);

        //        float min = 0;
        //        max = getTextSize();
        //
        //        /*
        //         * Start at half the current text size and work your way up. Not really a good approach.
        //         */
        //        int i = 0;
        //        while ((max - min) > THRESHOLD && i < MAX_ITERATIONS) {
        //            float size = (max + min) / 2;
        //            mTestPaint.setTextSize(size);
        //            float currentTextWidth = mTestPaint.measureText(getText().toString());
        //            if (currentTextWidth >= width)
        //                max = size;
        //            else
        //                min = size;
        //            i++;
        //        }
        //
        //        if ((max - min) <= THRESHOLD && i >= MAX_ITERATIONS) {
        //            //error simply reduce the text size by half
        //            Assert.fail("Text resizing failed in : " + MAX_ITERATIONS + " iterations");
        //            //            min = min / 2.0f;
        //        }
        //
        //        this.setTextSize(TypedValue.COMPLEX_UNIT_PX, min);
    }

    /**
     * If the text view size changed, set the force resize flag to true.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w != oldw || h != oldh) {
            needResize = true;
        }
    }

    @Override
    public void afterTextChanged(Editable s) {}

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        needResize = true;
    }
}
