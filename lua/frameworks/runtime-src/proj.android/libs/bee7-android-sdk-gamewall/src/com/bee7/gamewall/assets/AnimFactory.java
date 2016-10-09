package com.bee7.gamewall.assets;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.bee7.gamewall.video.VideoComponent;

/**
 * This class is responsible for creating animations.
 */
public class AnimFactory {
    private final static String TAG = AnimFactory.class.toString();

    public static final long ANIMATION_DURATION_SHORT = 200;
    public static final long ANIMATION_DURATION = 350;
    public static final long ANIMATION_DURATION_LONG = 550;

    private static final String PREF_ANIM_CONF = "pref_anim_conf";
    private static final String ANIM_KEY = "anim_key";

    public static Animation createScaleExpansion(View view) {
        return new SizedHeightScaleAnimation(view, 0, 1,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f);
    }

    public static Animation createScaleCollapse(View view) {
        return new SizedHeightScaleAnimation(view, 1, 0,
                Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f);
    }

    public static Animation createTransformExpansion(View view) {
        return new SizeHeightTransformAnimation(view, 0, 1);
    }

    public static Animation createTransformCollapseVideo(View view, LinearLayout parent) {
        return new SizeHeightTransformAnimationVideo(view, 1, 0, parent);
    }

    public static Animation createTransformExpansionVideo(VideoComponent videoComponent, LinearLayout videoPlaceholder) {
        return new SizeHeightTransformAnimationVideo(videoComponent, 0, 1, videoPlaceholder);
    }

    public static Animation createTransformExpansion(LinearLayout view, int animateFrom, int animateTo) {
        return new SizeHeightTransformAnimationLayout(view, animateFrom, animateTo);
    }

    public static Animation createAlphaShow(View view, boolean checkAvailability) {
        if (isAnimationsEnabled(view.getContext()) || !checkAvailability) {
            return new AlphaTransformAnimation(view, 0, 1);
        }
        return null;
    }

    public static Animation createAlphaHide(View view) {
        return new AlphaTransformAnimation(view, 1, 0);
    }

    public static Animation createSlideInFromBottom(View view) {
        return new SlideVertically(view, view.getHeight(), 0);
    }

    public static Animation createSlideOutFromTop(View view) {
        return new SlideVertically(view, 0, view.getHeight());
    }

    /**
     * Returns width of video view.
     * @param height to be used in calculation
     * @return width of video view
     */
    public static int getVideoViewWidth(int height) {
        return (int) ((height / 9f) * 16f);
    }

    /**
     * Returns height of video view.
     * Must be called after view has been measured (added to hierarchy).
     *
     * @param parent to be used for width in calculation
     * @return height of video view
     */
    public static int getVideoViewHeight(View parent, int offset) {
        //int margin = parent.getContext().getResources().getDimensionPixelSize(R.dimen.bee7_ingamewall_video_margin_vertical);
        return (int) (((parent.getMeasuredWidth()) / 16f) * 9f) + offset;
    }

    /**
     *
     * @param width of available space
     * @return calculated height
     */
    public static int getVideoViewHeight(int width) {
        return (int) ((width / 16f) * 9f);
    }

    public static void disableAnimations(Context context) {
        context.getSharedPreferences(PREF_ANIM_CONF, Context.MODE_PRIVATE).edit().putBoolean(ANIM_KEY, false).commit();
    }

    public static boolean isAnimationsEnabled(Context context) {
        return context.getSharedPreferences(PREF_ANIM_CONF, Context.MODE_PRIVATE).getBoolean(ANIM_KEY, true);
    }

    /**
     * Like {@link android.view.animation.ScaleAnimation} for height scaling that also resizes the view accordingly,
     * so it takes the right amount of space while scaling.
     */
    public static class SizedHeightScaleAnimation extends ScaleAnimation {

        private float fromHeight;
        private float toHeight;
        private View viewToScale;

        public SizedHeightScaleAnimation(View viewToScale, float fromY, float toY) {
            super(1, 1, fromY, toY);
            init(viewToScale, fromY, toY);
        }

        public SizedHeightScaleAnimation(View viewToScale, float fromY, float toY, float pivotX, float pivotY) {
            super(1, 1, fromY, toY, pivotX, pivotY);
            init(viewToScale, fromY, toY);
        }

        public SizedHeightScaleAnimation(View viewToScale, float fromY, float toY, int pivotXType, float pivotXValue, int pivotYType, float pivotYValue) {
            super(1, 1, fromY, toY, pivotXType, pivotXValue, pivotYType, pivotYValue);
            init(viewToScale, fromY, toY);
        }

        private void init(View viewToScale, float fromY, float toY) {

            this.viewToScale = viewToScale;
            viewToScale.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            fromHeight = viewToScale.getMeasuredHeight() * fromY;
            toHeight = viewToScale.getMeasuredHeight() * toY;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);

            int newHeight = (int) (fromHeight * (1 - interpolatedTime) + toHeight * interpolatedTime);

            viewToScale.getLayoutParams().height = newHeight;
            viewToScale.requestLayout();
            if (viewToScale.getParent() != null) {
                viewToScale.getParent().requestLayout();
            }
        }
    }

    public static class SizeHeightTransformAnimation extends Animation {

        private View view;
        private float fromHeight;
        private float toHeight;

        public SizeHeightTransformAnimation(View view, float fromY, float toY) {
            this.view = view;

            view.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            this.fromHeight = view.getMeasuredHeight() * fromY;
            this.toHeight = view.getMeasuredHeight() * toY;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);

            int newHeight = (int) (fromHeight * (1 - interpolatedTime) + toHeight * interpolatedTime);

            view.getLayoutParams().height = newHeight;
            view.requestLayout();
        }

        @Override
        public boolean willChangeTransformationMatrix() {
            return true;
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }

    public static class SizeHeightTransformAnimationVideo extends Animation {

        private View view;
        private float fromHeight;
        private float toHeight;

        public SizeHeightTransformAnimationVideo(View view, float fromY, float toY, LinearLayout parent) {
            this.view = view;

            view.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            this.fromHeight = parent.getMeasuredHeight() * fromY;
            //this.toHeight = ((parent.getMeasuredWidth() / 16) * 9) * toY;
            this.toHeight = (float) getVideoViewHeight(parent, 0) * toY;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);

            view.getLayoutParams().height = (int) (fromHeight * (1 - interpolatedTime) + toHeight * interpolatedTime);
            view.requestLayout();
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }

    }

    public static class SizeHeightTransformAnimationLayout extends Animation {

        private View view;
        private float fromHeight;
        private float toHeight;

        public SizeHeightTransformAnimationLayout(View view, float fromHeight, float toHeight) {
            this.view = view;
            this.fromHeight = fromHeight;
            this.toHeight = toHeight;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);

            view.getLayoutParams().height = (int) (fromHeight * (1 - interpolatedTime) + toHeight * interpolatedTime);
            view.requestLayout();
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }

    }

    public static class AlphaTransformAnimation extends Animation {
        private View view;
        private float fromAlpha;
        private float toAlpha;

        public AlphaTransformAnimation(View view, float fromAlpha, float toAlpha) {
            this.view = view;
            this.fromAlpha = fromAlpha;
            this.toAlpha = toAlpha;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            //some hack
            if (interpolatedTime > 1) {
                interpolatedTime = 0;
            }
            float alpha = fromAlpha + ((toAlpha - fromAlpha) * interpolatedTime);
            view.setAlpha(alpha);
        }
    }

    public static class SlideVertically extends Animation {

        private View view;
        private int from;
        private int to;

        public SlideVertically(View view, int from, int to) {
            this.view = view;
            this.from = from;
            this.to = to;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            view.setY(from * (1 - interpolatedTime) + to * interpolatedTime);
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }
}
