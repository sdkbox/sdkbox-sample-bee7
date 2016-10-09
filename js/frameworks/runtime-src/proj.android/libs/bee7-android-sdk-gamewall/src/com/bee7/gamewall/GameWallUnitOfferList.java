package com.bee7.gamewall;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bee7.gamewall.interfaces.OnOfferClickListener;
import com.bee7.gamewall.interfaces.OnVideoClickListener;
import com.bee7.gamewall.views.Bee7ImageView;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.GameWallConfiguration;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResult;
import com.bee7.sdk.publisher.appoffer.AppOfferWithResultImpl;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Offer List Holder Gamewall Unit
 */
public class GameWallUnitOfferList extends GameWallUnit {
    private static final String TAG = GameWallUnitOfferList.class.getName();

    public static class OfferTypePair {
        AppOffer appOffer;
        GameWallConfiguration.UnitType unitType;

        public OfferTypePair(AppOffer appOffer, GameWallConfiguration.UnitType unitType) {
            this.appOffer = appOffer;
            this.unitType = unitType;
        }
    }

    private LinearLayout offersListView;
    private List<OfferTypePair> appOffers;
    private List<GameWallUnitOfferListItem> items;

    public GameWallUnitOfferList(Context context, List<OfferTypePair> appOffers,
                                 OnOfferClickListener onOfferClickListener, OnVideoClickListener onVideoClickListener,
                                 AppOffersModel.VideoButtonPosition videoButtonPosition,
                                 AppOffersModel.VideoPrequalType videoPrequaificationlType,
                                 int maxDailyRewardFreq, int _index, int _column, boolean firstInColumnGroup,
                                 float exchangeRate, GameWallConfiguration.LayoutType layoutType,
                                 boolean immersiveMode) {
        super(context, _index, 0,_column);
        Logger.debug(TAG, "Instantiating GameWallUnitOfferList: row: " + index + ", column " + column + ", layoutType: " + layoutType + ", " + Utils.convertToSimpleNames(appOffers));
        this.appOffers = appOffers;

        inflate(getContext(), R.layout.gamewall_unit_offer_list, this);

        offersListView = (LinearLayout)findViewById(R.id.GwUnitOfferListItem_listview);

        if (firstInColumnGroup) {
            /*
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                offersListView.setBackground(getResources().getDrawable(R.drawable.bee7_content_bg));
            } else {
                offersListView.setBackgroundDrawable(getResources().getDrawable(R.drawable.bee7_content_bg));
            }
            LayoutParams params = (LayoutParams) offersListView.getLayoutParams();
            params.height = params.height + getResources().getDimensionPixelSize(R.dimen.bee7_games_list_holder_padding_top);
            offersListView.setLayoutParams(params);
            offersListView.setPadding(
                    offersListView.getPaddingLeft(),
                    offersListView.getPaddingTop() + getResources().getDimensionPixelSize(R.dimen.bee7_games_list_holder_padding_top),
                    offersListView.getPaddingRight(),
                    offersListView.getPaddingBottom());
            */
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                offersListView.setBackground(null);
            } else {
                offersListView.setBackgroundDrawable(null);
            }
        }

        items = new ArrayList<GameWallUnitOfferListItem>();

        for (int i = 0; i < offersListView.getChildCount(); i++) {
            final GameWallUnitOfferListItem item = (GameWallUnitOfferListItem) offersListView.getChildAt(i);

            if (appOffers.size() > i) {
                item.update(appOffers.get(i).appOffer,
                        onOfferClickListener,
                        onVideoClickListener,
                        videoButtonPosition,
                        videoPrequaificationlType,
                        maxDailyRewardFreq,
                        appOffers.get(i).unitType,
                        index,
                        i,
                        exchangeRate,
                        immersiveMode);

                item.getAppOfferWithResult(null).setLayoutType(layoutType);
            } else {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        item.setVisibility(INVISIBLE);
                    }
                });
            }

            items.add(item);
        }
    }

    @Override
    public AppOffer getAppOffer(String appOfferId) {
        for (GameWallUnitOfferListItem item : items) {
            if (item.getAppOffer(null) != null && item.getAppOffer(null).getId().equalsIgnoreCase(appOfferId)) {
                return item.getAppOffer(null);
            }
        }
        return null;
    }

    @Override
    public AppOfferWithResult getAppOfferWithResult(String appOfferId) {
        for (GameWallUnitOfferListItem item : items) {
            if (item != null
                    &&
                    item.getAppOffer(null) != null
                    &&
                    item.getAppOffer(null).getId() != null
                    &&
                    item.getAppOffer(null).getId().equalsIgnoreCase(appOfferId)) {
                return item.getAppOfferWithResult(null);
            }
        }
        return null;
    }

    @Override
    public void update(AppOffer appOffer) {
        if (appOffer != null) {
            for (int i = 0; i < offersListView.getChildCount(); i++) {
                View view = offersListView.getChildAt(i);

                if (view instanceof GameWallUnitOfferListItem) {
                    if (((GameWallUnitOfferListItem)view).getAppOffer(null) != null &&
                            ((GameWallUnitOfferListItem)view).getAppOffer(null).getId().equalsIgnoreCase(appOffer.getId())) {
                        ((GameWallUnitOfferListItem)view).update(appOffer);
                    }
                }
            }
        }
    }

    @Override
    public void update() {
        for (int i = 0; i < offersListView.getChildCount(); i++) {
            View view = offersListView.getChildAt(i);

            if (view instanceof GameWallUnitOfferListItem) {
                if (!((GameWallUnitOfferListItem)view).isEmpty()) {
                    ((GameWallUnitOfferListItem)view).update();
                }
            }
        }
    }

    public List<AppOffer> getAppOffers(GameWallConfiguration.UnitType unitType) {
        List<AppOffer> offers = new ArrayList<AppOffer>();
        for (OfferTypePair offerTypePair : appOffers) {
            if (offerTypePair.unitType == unitType) {
                offers.add(offerTypePair.appOffer);
            }
        }
        return offers;
    }
}
