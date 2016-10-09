package com.bee7.gamewall;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import com.bee7.gamewall.interfaces.Bee7InnerApp;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.FrequencyCappingConfiguration;
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
    private boolean locked;
    private int lockLevel;
    private Callable<Void> lockedMiniGameClicked;

    public Bee7InnerAppImpl(String appId, Drawable icon, String name, Callable<Boolean> runMinigame,
                            boolean locked, int lockLevel, Callable<Void> lockedMiniGameClicked) {
        this.id = appId;
        this.icon = icon;
        this.name = name;
        this.runMinigame = runMinigame;
        this.locked = locked;
        this.lockLevel = lockLevel;
        this.lockedMiniGameClicked = lockedMiniGameClicked;
    }

    public static Bee7InnerApp create(String appId, Resources resources, int iconRID, String name,
                                      Callable<Boolean> runMinigame, boolean locked, int lockLevel,
                                      Callable<Void> lockedMiniGameClicked) {
        return new Bee7InnerAppImpl(appId, resources.getDrawable(iconRID), name, runMinigame,
                locked, lockLevel, lockedMiniGameClicked);
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
            if (runMinigame != null) {
                Object obj = runMinigame.call();
                if (obj != null) {
                    return (Boolean) obj;
                } else {
                    return false;
                }
            } else {
                throw new Exception("Callable is null");
            }
        } catch (Exception e) {
            Logger.error("Bee7InnerAppImpl", e, "can't start mini-game: {0}, error: {1}", id, e.getMessage());
        }

        return false;
    }

    @Override
    public boolean isLocked() {
        return this.locked;
    }

    @Override
    public int getLockLevel() {
        return this.lockLevel;
    }

    @Override
    public void lockedMiniGameClicked() {
        try {
            if (lockedMiniGameClicked != null) {
                lockedMiniGameClicked.call();
            } else {
                throw new Exception("Callable is null");
            }
        } catch (Exception e) {
            Logger.error("Bee7InnerAppImpl", e, "Exception callback locked mini-game: {0} error: {1}", id, e.getMessage());
        }
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

    @Override
    public boolean getTestMode() {
        return false;
    }

    @Override
    public void setPriority(int priority) {

    }

    @Override
    public double getScore() {
        return 0;
    }

    @Override
    public double getAdjScore() {
        return 0;
    }

    @Override
    public int getImpCnt() {
        return 0;
    }

    @Override
    public double getImpProb() {
        return 1;
    }

    @Override
    public FrequencyCappingConfiguration.OfferType getOfferType() {
        return null;
    }

    @Override
    public double getFreqCapProbImpForIdx(int idx) {
        return 0;
    }
}
