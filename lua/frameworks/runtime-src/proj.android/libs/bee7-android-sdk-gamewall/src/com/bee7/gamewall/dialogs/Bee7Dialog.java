package com.bee7.gamewall.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.view.WindowManager;

import com.bee7.gamewall.R;

public class Bee7Dialog extends Dialog {

    protected static final String PREF_DIALOG_CONF = "pref_dialog_conf";
    protected static final String PREF_DIALOG_TUTORIAL_SHOWN = "pref_dialog_1";
    protected static final String PREF_DIALOG_REWARD_TUTORIAL_SHOWN = "pref_dialog_2";

    public Bee7Dialog(Context context) {
        super(context, R.style.CustomBee7DialogTheme);

        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);
    }
}
