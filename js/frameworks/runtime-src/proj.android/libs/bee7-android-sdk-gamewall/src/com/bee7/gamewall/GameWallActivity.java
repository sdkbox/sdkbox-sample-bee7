package com.bee7.gamewall;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;

import com.bee7.sdk.common.util.Logger;

/**
 * Created by Bee7 on 20/07/15.
 */
public class GameWallActivity extends Activity {
    private static final String TAG = GameWallActivity.class.getName();

    private boolean visible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        visible = false;

        setTheme(R.style.bee7_ActivityTheme_Transparent);

        setContentView(R.layout.gamewall_activity);

        GameWallActivityImpl.sharedInstance().addGameWallContent(this);

        Logger.debug(TAG, "GW activity created");
    }

    @Override
    protected void onResume() {
        super.onResume();

        visible = true;

        GameWallActivityImpl.sharedInstance().resumeGameWall();

        Logger.debug(TAG, "GW activity resumed");
    }

    @Override
    protected void onPause() {
        super.onPause();

        visible = false;

        GameWallActivityImpl.sharedInstance().pauseGameWall();

        Logger.debug(TAG, "GW activity paused");
    }

    @Override
    protected void onDestroy() {
        visible = false;

        GameWallActivityImpl.sharedInstance().destroyGameWall();

        Logger.debug(TAG, "GW activity destroyed");

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        if (!GameWallActivityImpl.sharedInstance().onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        GameWallActivityImpl.sharedInstance().updateView();

        Logger.debug(TAG, "GW activity updated");
    }

    public boolean isVisible() {
        return visible;
    }
}
