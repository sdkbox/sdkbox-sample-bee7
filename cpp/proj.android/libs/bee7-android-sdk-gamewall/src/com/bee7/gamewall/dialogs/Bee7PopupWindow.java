package com.bee7.gamewall.dialogs;

import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupWindow;

import com.bee7.gamewall.GameWallView;
import com.bee7.gamewall.R;
import com.bee7.gamewall.enums.BannerNotificationPosition;
import com.bee7.sdk.common.util.Logger;

public class Bee7PopupWindow extends PopupWindow {

    private static final String TAG = Bee7PopupWindow.class.getName();

    private View anchor;
    private BannerNotificationPosition bannerNotificationPosition;
    private GameWallView gameWallView;
    private OnDismissListener onDismissListener;

    public Bee7PopupWindow(View view, int matchParent, int wrapContent, boolean focusable, BannerNotificationPosition bannerNotificationPosition,
                           GameWallView gameWallView, OnDismissListener onDismissListener) {
        super(view, matchParent, wrapContent, focusable);

        setBackgroundDrawable(new ColorDrawable());
        setOutsideTouchable(false);
        setTouchable(true);
        setFocusable(false);

        this.gameWallView = gameWallView;
        this.bannerNotificationPosition = bannerNotificationPosition;
        this.onDismissListener = onDismissListener;

        setOnDismissListener(onDismissListener);

        if (bannerNotificationPosition == BannerNotificationPosition.BOTTOM_UP) {
            setAnimationStyle(R.style.bee7_banner_notification_anim_up_from_bottom);
        } else {
            setAnimationStyle(R.style.bee7_banner_notification_anim_down_from_top);
        }
    }

    @Override
    public void showAsDropDown(View anchor) {
        this.anchor = anchor;
    }

    public void show() {
        Logger.info(TAG, "banner notification show");
        if (anchor != null && anchor.getWindowToken() != null) {

            //Due to the workings of PopupView (it does not create its oun window, but uses anchors window)
            // we must fetch top most view in order to get correct window
            if (bannerNotificationPosition == BannerNotificationPosition.BOTTOM_UP) {
                if (gameWallView != null && gameWallView.isShown()) {
                    View view = gameWallView.getAnchorView();
                    super.showAtLocation(view, Gravity.NO_GRAVITY, 0, -getContentView().getMeasuredHeight());
                } else {
                    super.showAtLocation(anchor, Gravity.NO_GRAVITY, 0, -getContentView().getMeasuredHeight());
                }
            } else {
                if (gameWallView != null && gameWallView.isShown()) {
                    View view = gameWallView.getAnchorView();
                    super.showAtLocation(view, Gravity.NO_GRAVITY, 0, -anchor.getHeight());
                } else {
                    super.showAtLocation(anchor, Gravity.NO_GRAVITY, 0, -anchor.getHeight());
                }
            }

            fireTouchOutsideEnabler();
        } else {
            Logger.error(TAG, "anchor or anchor.getWindowToken() is null");
            //Report back that nothing was shown
            if (onDismissListener != null) {
                onDismissListener.onDismiss();
            }
        }
    }

    private void fireTouchOutsideEnabler() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setOutsideTouchable(true);
                update();
            }
        }, getContentView().getContext().getResources().getInteger(R.integer.bee7_notification_banner_anim_speed));
    }
}
