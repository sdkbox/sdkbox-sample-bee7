package com.bee7.gamewall.dialogs;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.bee7.gamewall.BannerNotification;
import com.bee7.gamewall.BannerNotificationNotify;
import com.bee7.gamewall.GameWallView;
import com.bee7.gamewall.enums.BannerNotificationPosition;
import com.bee7.gamewall.interfaces.Bee7GameWallManagerV2;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.DefaultPublisher;

/**
 * Wraps generation of view and Bee7PopupWindow
 */
public class Bee7PopupManager {

    private static final String TAG = Bee7PopupManager.class.getSimpleName();

    public interface GamewallBannerInterface {
        void OnBannerClick();
        void OnCloseClick();
    }

    private Bee7PopupWindow bee7PopupWindow;
    private Bee7GameWallManagerV2 gameWallManagerV2;
    private Context context;
    private BannerNotification bannerNotification;
    private BannerNotificationNotify bannerNotificationNotify;
    private DefaultPublisher mPublisher;
    private Bee7PopupWindowView bee7PopupWindowView;
    private GameWallView gameWallView;
    private boolean clickOnBannerHappened = false;
    private boolean timeoutHappened = false;

    private long shownTimestamp = 0;

    public Bee7PopupManager(Context _context, View anchorView,
                            BannerNotificationPosition bannerNotificationPosition,
                            BannerNotification bannerNotification,
                            DefaultPublisher mPublisher,
                            Bee7GameWallManagerV2 _gameWallManagerV2, GameWallView gameWallView,
                            BannerNotificationNotify bannerNotificationNotify) {

        this.gameWallManagerV2 = _gameWallManagerV2;
        this.context = _context;
        this.mPublisher = mPublisher;
        this.bannerNotification = bannerNotification;
        this.bannerNotificationNotify = bannerNotificationNotify;
        this.gameWallView = gameWallView;

        //Generate popup view
        bee7PopupWindowView = new Bee7PopupWindowView(context, bannerNotification, mPublisher, gamewallBannerInterface);

        //New Bee7PopupWindow
        bee7PopupWindow = new Bee7PopupWindow(bee7PopupWindowView.getView(), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                true, bannerNotificationPosition, gameWallView, onDismissListener);
        bee7PopupWindow.showAsDropDown(anchorView);
    }

    public boolean isShowing() {
        if (bee7PopupWindow != null) {
            return  bee7PopupWindow.isShowing();
        }
        return false;
    }

    private PopupWindow.OnDismissListener onDismissListener = new PopupWindow.OnDismissListener() {
        @Override
        public void onDismiss() {
            if (!clickOnBannerHappened && !timeoutHappened && mPublisher != null) {
                mPublisher.onBannerNotificationEvent(getBannerType(), getLayoutType(), 3);

                cancelNotify();
                dismissNotify();
            }

            if (gameWallManagerV2 != null) {
                gameWallManagerV2.onBannerNotificationVisibilityChanged(false);
            }
        }
    };

    /**
     *
     * @param userDismissed true if it was dismissed by a user
     */
    public void dismiss(boolean userDismissed) {
        if (mPublisher != null) {
            if (userDismissed) {
                mPublisher.onBannerNotificationEvent(getBannerType(), getLayoutType(), 3);
            } else {
                timeoutHappened = true;
                mPublisher.onBannerNotificationEvent(getBannerType(), getLayoutType(), 4);
            }
        }

        dismiss();
    }

    public boolean dismissedPrematurely() {
        if (mPublisher.getBannerNotificationConfig() != null) {
            // -200 millis because android. Its due to the view handling (adding/removing/animations) multiples
            // display times can be lower than DisplayTimeSecsMultiple value
            if (System.currentTimeMillis() - shownTimestamp - 200 < mPublisher.getBannerNotificationConfig().getDisplayTimeSecsMultiple()
                    &&
                    mPublisher.getBannerNotificationConfig().getNotificationShowAgain())
            {

                Logger.debug(TAG, "Banner was dismissed prematurely DisplayTimeSecsMultiple: "
                        + mPublisher.getBannerNotificationConfig().getDisplayTimeSecsMultiple()
                        + ", time shown: " + (System.currentTimeMillis() - shownTimestamp - 200));

                return true;
            }
        }

        return false;
    }

    private void dismiss() {
        if (bee7PopupWindow != null) {
            if (bee7PopupWindow.isShowing()) {
                bee7PopupWindow.dismiss();

                cancelNotify();
            }
        }
    }

    private void cancelNotify() {
        if (bannerNotificationNotify != null) {
            bannerNotificationNotify.cancel();
        }
    }

    private void dismissNotify() {
        if (bannerNotificationNotify != null) {
            bannerNotificationNotify.dismiss();
        }
    }

    public void show() {
        if (bee7PopupWindow != null) {
            bee7PopupWindow.show();

            shownTimestamp = System.currentTimeMillis();

            if (mPublisher != null) {
                mPublisher.onBannerNotificationEvent(getBannerType(), getLayoutType(), 0);
            }

            if (gameWallManagerV2 != null) {
                gameWallManagerV2.onBannerNotificationVisibilityChanged(true);
            }
        }
    }

    public BannerNotification.BannerNotificationType getBannerNotificationType() {
        return bannerNotification.getBannerNotificationType();
    }

    private GamewallBannerInterface gamewallBannerInterface = new GamewallBannerInterface() {
        @Override
        public void OnBannerClick() {
            clickOnBannerHappened = true;
            if (gameWallView == null || (gameWallView != null && !gameWallView.isShown())) {
                if (mPublisher != null) {
                    mPublisher.onBannerNotificationEvent(getBannerType(), getLayoutType(), 1);
                }
                dismiss();

                gameWallManagerV2.onBannerNotificationClick();
                gameWallManagerV2.onGameWallShowRequest();
            }

            if (gameWallView != null && gameWallView.isShown()) {
                if (mPublisher != null) {
                    mPublisher.onBannerNotificationEvent(getBannerType(), getLayoutType(), 1);
                }
            }
            dismiss();
            dismissNotify();
        }

        @Override
        public void OnCloseClick() {
            clickOnBannerHappened = true;
            if (gameWallView == null || (gameWallView != null && !gameWallView.isShown())) {
                if (mPublisher != null) {
                    mPublisher.onBannerNotificationEvent(getBannerType(), getLayoutType(), 2);
                }
                dismiss();

                gameWallManagerV2.onBannerNotificationClick();
            }

            if (gameWallView != null && gameWallView.isShown()) {
                if (mPublisher != null) {
                    mPublisher.onBannerNotificationEvent(getBannerType(), getLayoutType(), 2);
                }
            }
            dismiss();
            dismissNotify();
        }
    };

    public int getBannerType() {
        int bannerType = 0;
        switch (bannerNotification.getBannerNotificationType()){
            case REWARD:
                bannerType = 0;
                break;
            case LOW_CURRENCY:
                bannerType = 1;
                break;
            case REMINDER:
                bannerType = 2;
                break;
        }
        return bannerType;
    }

    public int getLayoutType() {
        return bee7PopupWindowView.getLayoutType();
    }
}
