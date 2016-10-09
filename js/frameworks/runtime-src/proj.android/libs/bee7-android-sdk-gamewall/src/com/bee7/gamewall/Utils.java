package com.bee7.gamewall;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;

import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.common.util.SharedPreferencesHistoryHelper;
import com.bee7.sdk.publisher.FrequencyCappingConfiguration;
import com.bee7.sdk.publisher.GameWallConfiguration;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOfferDefaultIconListener;

import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    private static final String TAG = Utils.class.getName();

    /**
     * @param numOfItems Number of app offers to be generated.
     * @param installed If this offers should be connected or not.
     * @return list of generated app offers.
     */
    public static List<AppOffer> generateTestData(int numOfItems, final boolean installed) {
        List<AppOffer> testList = new ArrayList<AppOffer>();

        for (int i = 0; i < numOfItems; i++) {
            final int finalI = i;
            AppOffer appOffer = new AppOffer() {

                @Override
                public boolean showUserRatings() {
                    return true;
                }

                @Override
                public double getUserRating() {
                    return (double)finalI/2d;
                }

                @Override
                public boolean getTestMode() {
                    return false;
                }

                @Override
                public String getId() {
                    if (installed) {
                        return "appOffer.connected.id." + finalI;
                    } else {
                        return "appOffer.offer.id." + finalI;
                    }
                }

                @Override
                public long getCampaignId() {
                    return finalI;
                }

                @Override
                public String getLocalizedName() {
                    //                    return "App localised name " + finalI;
                    StringBuffer name = new StringBuffer(installed ? "Connected " : "Offer ");
                    //name.append(" app localised name ");
                    for (int j = 0; j < finalI; j++) {
                        name.append(finalI);
                    }
                    name.append("");
                    return name.toString();
                }

                @Override
                public URL getIconUrl(IconUrlSize size) {
                    try {
                        switch (size) {
                            case SMALL:
                                return new URL("http://storage.googleapis.com/bee7/gamewall.default.icon/icon120.png"); // small

                            case LARGE:
                                return new URL("http://storage.googleapis.com/bee7/gamewall.default.icon/icon240.png"); // large
                        }
                    } catch (MalformedURLException e) {
                        Logger.error(TAG, "MalformedURLException getIconUrl: {0}", e.getMessage());
                    }
                    return null;
                }

                @Override
                public int getPriority() {
                    return finalI;
                }

                @Override
                public State getState() {
                    if (installed) {
                        return State.CONNECTED;
                    } else {
                        return State.NOT_CONNECTED;
                    }
                }

                @Override
                public boolean isShowGameWallTitle() {
                    return true;
                }

                @Override
                public JSONObject toJson() {
                    return null;
                }

                @Override
                public void getDefaultIconBitmap(Context context, IconUrlSize iconSize, AppOfferDefaultIconListener listener) { }

                @Override
                public boolean showVideoButton() {
                    return true;
                }

                @Override
                public String getVideoUrl() {
                    switch (finalI) {
                        case 0:
                            return "http://cdn.bee7.com/bee7/hls/CandyCrushSoda20s/CandyCrushSoda20s.m3u8";
                        case 1:
                            return "http://cdn.bee7.com/bee7/hls/ClashOfKings15s/ClashOfKings15s.m3u8";
                        case 2:
                            return "http://cdn.bee7.com/bee7/hls/CookieJam/CookiJam30s.m3u8";
                        case 4:
                            return "http://cdn.bee7.com/bee7/hls/DragonCity15s/DragonCity15s.m3u8";
                        case 5:
                            return "http://cdn.bee7.com/bee7/hls/HeroesCharge15s/HeroesCharge15s.m3u8";
                        case 6:
                            return "http://cdn.bee7.com/bee7/hls/JellyBlast30s/jelly_blast_video_2.m3u8";
                        default:
                            return "http://cdn.bee7.com/bee7/hls/PandaPop/PandaPop15s.m3u8";
                    }
                }

                @Override
                public String getCreativeUrl() {
                    switch (finalI) {
                        case 0:
                            return "http://cdn.bee7.com/bee7/hls/VideoEndCreativeImages/CandyCrushSodaSaga1280x800.jpg";
                        case 1:
                            return "http://cdn.bee7.com/bee7/hls/VideoEndCreativeImages/ClashOfKings1024-768.jpg";
                        case 2:
                            return "http://cdn.bee7.com/bee7/hls/VideoEndCreativeImages/CookieJam1903-0.png";
                        case 4:
                            return "http://cdn.bee7.com/bee7/hls/VideoEndCreativeImages/DragonCity2000x1125_end_card_play.jpg";
                        case 5:
                            return "http://cdn.bee7.com/bee7/hls/VideoEndCreativeImages/HeroesCharge720x800.png";
                        case 6:
                            return "http://cdn.bee7.com/bee7/hls/VideoEndCreativeImages/JellyBlast1024x768.jpg";
                        default:
                            return "http://cdn.bee7.com/bee7/hls/VideoEndCreativeImages/PandaPop640x560_WormholePanda.jpg";
                    }
                }

                @Override
                public int getVideoReward() {
                    return finalI;
                }

                @Override
                public String getLocalizedShortName() {
                    //                    return "Short app name " + finalI;
                    StringBuffer name = new StringBuffer("Short ");
                    for (int j = 0; j < finalI; j++) {
                        name.append(finalI);
                    }
                    return name.toString();
                }

                @Override
                public String getLocalizedDescription() {
                    return "Description " + finalI;// + " added text so the description is loooooooooooooooooooooonger. So long, much text!";
                }

                @Override
                public boolean isInnerApp() {
                    return false;
                }

                @Override
                public void startInnerApp() {

                }

                @Override
                public Drawable getIconDrawable() {
                    return null;
                }

                @Override
                public long getLastPlayedTimestamp(Context context) {
                    return SharedPreferencesHistoryHelper.getLastPlayedTimestamp(context, getId());
                }

                @Override
                public void updateLastPlayedTimestamp(Context context) {
                    SharedPreferencesHistoryHelper.updateLastPlayedTimestamp(context, getId());
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
            };
            testList.add(appOffer);
        }

        return testList;
    }

    /**
     * @param numOfItems Number of unity types to be generated.
     * @return Map of Lists of UnitTypes
     */
    public static Map<? extends GameWallConfiguration.LayoutType, ? extends List<GameWallConfiguration.UnitType>> generateTestDataForLayout(int numOfItems) {
        Map<GameWallConfiguration.LayoutType, List<GameWallConfiguration.UnitType>> layoutTypeListMap = new HashMap<GameWallConfiguration.LayoutType, List<GameWallConfiguration.UnitType>>();

        List<GameWallConfiguration.UnitType> unitTypes = new ArrayList<GameWallConfiguration.UnitType>();
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_LIST);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_LIST);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);

        if (unitTypes.size() < numOfItems) {
            for (int i = unitTypes.size(); i < numOfItems; i++){
                unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);
            }
        }

        layoutTypeListMap.put(GameWallConfiguration.LayoutType.PORTRAIT, unitTypes);

        unitTypes = new ArrayList<GameWallConfiguration.UnitType>();
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_LIST);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_LIST);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_LIST);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);

        if (unitTypes.size() < numOfItems) {
            for (int i = unitTypes.size(); i < numOfItems; i++){
                unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);
            }
        }

        layoutTypeListMap.put(GameWallConfiguration.LayoutType.LANDSCAPE_LEFT, unitTypes);

        unitTypes = new ArrayList<GameWallConfiguration.UnitType>();
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_LIST);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_LIST);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);
        unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);

        if (unitTypes.size() < numOfItems) {
            for (int i = unitTypes.size(); i < numOfItems; i++){
                unitTypes.add(GameWallConfiguration.UnitType.OFFER_BANNER);
            }
        }

        layoutTypeListMap.put(GameWallConfiguration.LayoutType.LANDSCAPE_RIGHT, unitTypes);

        return layoutTypeListMap;
    }

    /**
     * @param context to be used for retrieving resources.
     * @return Number of items that are in gw list unit. In case of exception, defaults to 3.
     */
    public static int getNumberOfItemsInGwUnitListHolder(Context context) {
        try {
            ViewGroup view = (ViewGroup) ViewGroup.inflate(context, R.layout.gamewall_unit_offer_list, null);
            Logger.debug("getNumberOfItemsInGwUnitListHolder","getNumberOfItemsInGwUnitListHolder: " + view.getChildCount());
            return view.getChildCount();
        } catch (Exception ex) {
            throw new IllegalStateException("gamewall_unit_offer_list is malformed or not ViewGroup!");
        }
    }

    public static boolean isPortrate(Context context) {
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        } else {
            return false;
        }
    }

    public static String convertToSimpleNames(List<GameWallUnitOfferList.OfferTypePair> appOffers) {
        if (appOffers != null && !appOffers.isEmpty()) {
            String names = "";
            for (GameWallUnitOfferList.OfferTypePair pair : appOffers) {
                if (appOffers.indexOf(pair) > 0) {
                    names = names + ", ";
                }
                names = names + pair.appOffer.getLocalizedName();
            }

            return names;
        }

        return "App offers list is empty";
    }

    public static String convertToSimpleNamesListAppOffer(List<AppOffer> appsNotInstalled) {
        if (appsNotInstalled != null && !appsNotInstalled.isEmpty()) {
            String names = "";
            for (AppOffer appOffer : appsNotInstalled) {
                if (appsNotInstalled.indexOf(appOffer) > 0) {
                    names = names + ", ";
                }
                names = names + appOffer.getLocalizedName();
            }

            return names;
        }

        return "App offers list is empty";
    }

    public static String convertToSimpleNamesListUnitType(List<GameWallConfiguration.UnitType> unitTypes) {
        if (unitTypes != null && !unitTypes.isEmpty()) {
            String names = "";
            for (GameWallConfiguration.UnitType unitType : unitTypes) {
                if (unitTypes.indexOf(unitType) > 0) {
                    names = names + ", ";
                }
                names = names + unitType;
            }

            return names;
        }

        return "Unit Type list is empty";
    }
}
