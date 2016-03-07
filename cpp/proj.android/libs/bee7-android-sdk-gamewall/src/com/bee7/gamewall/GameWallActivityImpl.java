package com.bee7.gamewall;

import android.app.Activity;
import android.content.Intent;

import com.bee7.gamewall.interfaces.Bee7GameWallManager;
import com.bee7.sdk.common.Reward;
import com.bee7.sdk.common.util.Logger;

/**
 * Created by Bee7 on 19/07/15.
 */
public class GameWallActivityImpl implements Bee7GameWallManager {
    static final String TAG = GameWallActivityImpl.class.getSimpleName();

    private static GameWallActivityImpl instance;

    private Activity mContext;
    private GameWallActivity mActivity;
    private Bee7GameWallManager mManager;

    private GameWallImpl mGameWall;

    private boolean settingUpGameWall;

    public static GameWallActivityImpl sharedInstance() {
        if (instance == null) {
            instance = new GameWallActivityImpl();
        }

        return instance;
    }

    private GameWallActivityImpl() {}

    /***********************************************
     * Manager calls
     ***********************************************/

    public void init(Activity ctx, final Bee7GameWallManager manager, String apiKey) {
        init(ctx, manager, apiKey, null);
    }

    public void init(Activity ctx, final Bee7GameWallManager manager, String apiKey, String vendorId) {
        // check if already initialized
        if (mGameWall == null) {
            try {
                mGameWall = new GameWallImpl(ctx, this, apiKey, vendorId, null);

                mGameWall.checkForClaimData(ctx.getIntent());

                mContext = ctx;
                mManager = manager;
                mActivity = null;

                settingUpGameWall = false;
            } catch (Exception ex) {
                Logger.debug(TAG, ex, "Failed to init game wall");
            }
        }
    }

    public void resume() {
        if (mGameWall != null) {
            Logger.debug(TAG, "GW resumed");

            mGameWall.resume();
        }
    }

    public void pause() {
        // do not pause if game wall activity is being set up
        if (!settingUpGameWall && mGameWall != null) {
            Logger.debug(TAG, "GW paused");

            mGameWall.pause();
        }
    }

    public void destroy() {
        // do not destroy if game wall activity is on top
        if (mActivity == null) {
            if (mGameWall != null) {
                Logger.debug(TAG, "GW destroyed");

                mGameWall.destroy();

                mGameWall = null;
            }
        }
    }

    public void checkForClaimData(Intent intent) {
        if (mGameWall != null && intent != null) {
            mGameWall.checkForClaimData(intent);
        }
    }

    public void setAgeGate(boolean hasPassed) {
        if (mGameWall != null) {
            mGameWall.setAgeGate(hasPassed);
        }
    }

    public void show() {
        try {
            if (mActivity == null) {
                if (mContext != null) {
                    settingUpGameWall = true;

                    Intent intent = new Intent(mContext, GameWallActivity.class);

                    mContext.startActivity(intent);

                    Logger.debug(TAG, "GW starting activity");
                }
            } else {
                if (mGameWall != null) {
                    Logger.debug(TAG, "GW showed");

                    mGameWall.show(mActivity);
                }
            }
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to show game wall");
        }
    }

    public void showReward(Reward reward) {
        if (mGameWall != null) {
            if (mActivity != null && mActivity.isVisible()) {
                Logger.debug(TAG, "GW show reward on GW activity");

                mGameWall.showReward(reward, mActivity);
            } else {
                Logger.debug(TAG, "GW show reward on main activity");

                mGameWall.showReward(reward, mContext);
            }
        }
    }

    public void onGameWallButtonImpression() {
        if (mGameWall != null) {
            mGameWall.onGameWallButtonImpression();
        }
    }

    /**************************************************
     * Activity calls
     **************************************************/
    public void addGameWallContent(GameWallActivity activity) {
        mActivity = activity;

        settingUpGameWall = true;

        if (mGameWall != null) {
            Logger.debug(TAG, "GW show on GW activity");

            mGameWall.show(mActivity);
        }
    }

    public void resumeGameWall() {
        // first resume after game wall activity was created
        if (settingUpGameWall) {
            settingUpGameWall = false;
        } else if (mGameWall != null) {
            // resume of game wall activity
            mGameWall.resume();

            Logger.debug(TAG, "GW resumed from GW activity");
        }
    }

    public void pauseGameWall() {
        // pausing with game wall activity on top
        if (mActivity != null) {
            if (mGameWall != null) {
                mGameWall.pause();

                Logger.debug(TAG, "GW paused from GW activity");
            }
        }
    }

    public void destroyGameWall() {
        // hide game wall in case main activity was activated again
        if (mGameWall != null) {
            Logger.debug(TAG, "GW hide from GW activity");

            mGameWall.hide();
        }

        // reset activity reference
        mActivity = null;
    }

    public boolean onBackPressed() {
        if (mGameWall != null) {
            return mGameWall.onBackPressed();
        }

        return false;
    }

    public void updateView() {
        if (mActivity != null) {
            if (mGameWall != null) {
                Logger.debug(TAG, "GW updated from GW activity");

                mGameWall.updateView();
            }
        }
    }

    /***************************************************
     * Bee7GameWallManager impl
     ***************************************************/

    @Override
    public void onGiveReward(Reward reward) {
        if (mManager != null) {
            mManager.onGiveReward(reward);
        }
    }

    @Override
    public void onAvailableChange(boolean available) {
        if (mManager != null) {
            mManager.onAvailableChange(available);
        }
    }

    @Override
    public void onVisibleChange(boolean visible, boolean isGameWall) {
        if (!visible && isGameWall) {
            if (mActivity != null) {
                mActivity.finish();

                mActivity = null;
            }
        }

        if (mManager != null) {
            mManager.onVisibleChange(visible, isGameWall);
        }
    }

    @Override
    public boolean onGameWallWillClose() {
        if (mManager != null) {
            return mManager.onGameWallWillClose();
        }

        return true;
    }

    @Override
    public void onReportingId(String reportingId, long reportingIdTs) {}
}
