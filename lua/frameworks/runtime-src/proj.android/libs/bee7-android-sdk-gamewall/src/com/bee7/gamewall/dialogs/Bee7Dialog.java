package com.bee7.gamewall.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.bee7.gamewall.R;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.common.util.Utils;

public class Bee7Dialog extends Dialog {

    protected static final String PREF_DIALOG_CONF = "pref_dialog_conf";
    protected static final String PREF_DIALOG_TUTORIAL_SHOWN = "pref_dialog_1";
    protected static final String PREF_DIALOG_REWARD_TUTORIAL_SHOWN = "pref_dialog_2";
    boolean immersiveMode = false;

    public Bee7Dialog(Context context, boolean immersiveMode) {
        super(context, R.style.CustomBee7DialogTheme);

        this.immersiveMode = immersiveMode;
        if (immersiveMode && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            Utils.setImmersiveModeFlags(getWindow().getDecorView());
        }
    }

    @Override
    public void show() {
        if (immersiveMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            Utils.setImmersiveModeFlags(getWindow().getDecorView());
        }

        try {
            super.show();
        } catch (Exception e) {
            Logger.error("Bee7Dialog", e, "Failed to show dialog");
        }

        if (immersiveMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }
}
