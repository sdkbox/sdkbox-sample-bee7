package com.bee7.gamewall;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.bee7.gamewall.interfaces.Bee7InnerApp;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.appoffer.AppOfferDefaultIconListener;

import org.json.JSONObject;

import java.net.URL;
import java.util.concurrent.Callable;

/**
 * Created by Bee7 on 02/06/15.
 */
public class Bee7InnerAppImpl implements Bee7InnerApp {
    private String id;
    private Drawable icon;
    private String name;
    private Callable<Boolean> runMinigame;

    public Bee7InnerAppImpl(String appId, Drawable icon, String name, Callable<Boolean> runMinigame) {
        this.id = appId;
        this.icon = icon;
        this.name = name;
        this.runMinigame = runMinigame;
    }

    public static Bee7InnerApp create(String appId, Resources resources, int iconRID, String name, Callable<Boolean> runMinigame) {
        return new Bee7InnerAppImpl(appId, resources.getDrawable(iconRID), name, runMinigame);
    }

    @Override
    public Drawable getIcon() {
        return this.icon;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean start() {
        try {
            return runMinigame.call();
        } catch (Exception e) {
            Logger.error("Bee7InnerAppImpl", e, "can't start mini-game: {0}", id);
            e.printStackTrace();
        }

        return false;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public long getCampaignId() {
        return 0;
    }

    @Override
    public String getLocalizedName() {
        return this.name;
    }

    @Override
    public String getLocalizedShortName() {
        return this.name;
    }

    @Override
    public String getLocalizedDescription() {
        return null;
    }

    @Override
    public URL getIconUrl(IconUrlSize size) {
        return null;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public State getState() {
        return State.CONNECTED;
    }

    @Override
    public boolean isShowGameWallTitle() {
        return false;
    }

    @Override
    public JSONObject toJson() {
        return null;
    }

    @Override
    public void getDefaultIconBitmap(Context context, IconUrlSize iconSize, AppOfferDefaultIconListener listener) {

    }

    @Override
    public boolean showVideoButton() {
        return false;
    }

    @Override
    public String getVideoUrl() {
        return null;
    }

    @Override
    public String getCreativeUrl() {
        return null;
    }

    @Override
    public int getVideoReward() {
        return 0;
    }

    @Override
    public boolean isInnerApp() {
        return true;
    }

    @Override
    public void startInnerApp() {
        this.start();
    }

    @Override
    public Drawable getIconDrawable() {
        return this.icon;
    }

    @Override
    public long getLastPlayedTimestamp(Context context) {
        return 0;
    }

    @Override
    public void updateLastPlayedTimestamp(Context context) {

    }

    @Override
    public boolean showUserRatings() {
        return false;
    }

    @Override
    public double getUserRating() {
        return 0;
    }
}
