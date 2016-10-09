package com.bee7.gamewall.interfaces;

import android.graphics.drawable.Drawable;

import com.bee7.sdk.publisher.appoffer.AppOffer;

/**
 * Created by Bee7 on 02/06/15.
 */
public interface Bee7InnerApp extends AppOffer{

    Drawable getIcon();

    String getName();

    /**
     * @return <b>true</b> if mini-game started successfully, <b>false</b> otherwise
     */
    boolean start();

    /**
     * @return <b>true</b> if mini-game is locked
     */
    boolean isLocked();

    /**
     * @return number of level when mini-game is unlocked
     */
    int getLockLevel();

    void lockedMiniGameClicked();
}
