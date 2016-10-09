package com.bee7.gamewall;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import com.bee7.sdk.common.util.*;
import com.bee7.sdk.common.util.Utils;

/**
 * Created by Bee7 on 20/07/15.
 */
public class GameWallActivity extends Activity {
    private static final String TAG = GameWallActivity.class.getName();

    public final static String IMMERSIVE_MODE_KEY = "immersiveMode";

    private boolean visible;
    private boolean immersiveMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Intent intent = getIntent();
        if (intent != null) {
            immersiveMode = intent.getBooleanExtra(IMMERSIVE_MODE_KEY, false);
            if (immersiveMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                com.bee7.sdk.common.util.Utils.setImmersiveModeFlags(getWindow().getDecorView());
            }
        }
        visible = false;

        super.onCreate(savedInstanceState);

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

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (immersiveMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Utils.setImmersiveModeFlags(getWindow().getDecorView());
        }
    }
}
