package com.bee7.gamewall.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.bee7.gamewall.R;
import com.bee7.gamewall.interfaces.Bee7GameWallManager;
import com.bee7.gamewall.interfaces.OnVideoRewardGeneratedListener;
import com.bee7.sdk.common.Reward;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.common.util.Utils;
import com.bee7.sdk.publisher.Publisher;
import com.bee7.sdk.publisher.appoffer.AppOffer;
import com.bee7.sdk.publisher.appoffer.AppOffersModel;

import java.util.List;

public class DialogDebug extends Dialog {

    private final static String TAG = DialogDebug.class.getSimpleName();

    private Bee7GameWallManager manager;
    private DialogDebugInterface dialogDebugInterface;
    private Publisher publisher;
    private OnVideoRewardGeneratedListener onVideoRewardGeneratedListener;
    private static String storeId;

    public static void setStoreId(String storeId) {
        DialogDebug.storeId = storeId;
    }

    public interface DialogDebugInterface {
        void onRewardGenerated(Reward reward);
    }

    public DialogDebug(Context context, Publisher publisher,
                       OnVideoRewardGeneratedListener onVideoRewardGeneratedListener,
                       Bee7GameWallManager manager,
                       DialogDebugInterface dialogDebugInterface) {
        super(context);

        this.publisher = publisher;
        this.onVideoRewardGeneratedListener = onVideoRewardGeneratedListener;
        this.manager = manager;
        this.dialogDebugInterface = dialogDebugInterface;

        init();
    }

    private void init() {
        setTitle("Debug tools");

        setContentView(R.layout.gamewall_dialog_debug);

        Button buttonReward = (Button) findViewById(R.id.button_reward);
        Button buttonVideoReward = (Button) findViewById(R.id.button_video_reward);
        TextView debugUrlTxt = (TextView) findViewById(R.id.debug_url_txt);
        TextView storeIdTxt = (TextView) findViewById(R.id.debug_store_id);

        if (storeId != null && !TextUtils.isEmpty(storeId)) {
            storeIdTxt.setText("Store id: " + storeId);
        } else {
            storeIdTxt.setText("Store id: not set");
        }

        if (Utils.isDevBackendEnabled(getContext())) {
            debugUrlTxt.setVisibility(View.VISIBLE);
            debugUrlTxt.setText("Connected to: " + Utils.getBackendUrl(getContext(), null, null, 0));
        } else {
            debugUrlTxt.setVisibility(View.GONE);
        }

        buttonReward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AppOffersModel appOffersModel = publisher.getAppOffersModel();
                        List<AppOffer> appsNotInstalled = appOffersModel.getCurrentOrderedAppOffers(AppOffersModel.AppOffersState.NOT_CONNECTED_AND_PENDING_INSTALL);

                        if (appsNotInstalled != null && !appsNotInstalled.isEmpty()) {
                            AppOffer appOffer = appsNotInstalled.get(0);
                            Logger.debug(TAG, "generate Reward for " + appOffer.getLocalizedName());
                            Reward reward = publisher.generateVideoReward(appOffer);

                            if (reward != null && onVideoRewardGeneratedListener != null) {
                                reward.setVideoReward(false);

                                if (dialogDebugInterface != null) {
                                    dialogDebugInterface.onRewardGenerated(reward);
                                }
                                //onVideoRewardGeneratedListener.onVideoRewardGenerated(appOffer);
                            } else {
                                Logger.error(TAG, "Generated reward or onVideoRewardGeneratedListener is null");
                            }
                        } else {
                            Logger.error(TAG, "AppsNotInstalled is null or empty. Offer not found for reward to generate");
                        }

                    }
                }, 5000);

            }
        });

        buttonVideoReward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AppOffersModel appOffersModel = publisher.getAppOffersModel();
                        List<AppOffer> appsNotInstalled = appOffersModel.getCurrentOrderedAppOffers(AppOffersModel.AppOffersState.NOT_CONNECTED_AND_PENDING_INSTALL);

                        AppOffer appOffer = appsNotInstalled.get(0);
                        Logger.debug(TAG, "generate Video Reward for " + appOffer.getLocalizedName());
                        Reward reward = publisher.generateVideoReward(appOffer);

                        if (reward != null && onVideoRewardGeneratedListener != null) {
                            onVideoRewardGeneratedListener.onVideoRewardGenerated(appOffer);
                        } else {
                            Logger.error(TAG, "Generated reward or onVideoRewardGeneratedListener is null");
                        }
                    }
                }, 3000);

            }
        });

    }
}