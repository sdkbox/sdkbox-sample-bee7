package com.bee7.gamewall.interfaces;

import android.view.View;

/**
 *
 */
public abstract class Bee7GameWallViewsInterface {

    /**
     *
     * @param gamewallHeaderCallbackInterface interface to be used in the header view
     * @return header view that will be used instead of default one
     */
    public View provideHeaderView(GamewallHeaderCallbackInterface gamewallHeaderCallbackInterface) {
        return null;
    }
}
