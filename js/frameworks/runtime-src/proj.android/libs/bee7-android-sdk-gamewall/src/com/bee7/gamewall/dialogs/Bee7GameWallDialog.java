package com.bee7.gamewall.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.View;

import com.bee7.gamewall.R;
import com.bee7.sdk.common.util.Utils;

public class Bee7GameWallDialog extends Dialog{

    public interface OnBackPressedListener {
        void onBackPressed();
    }

    private boolean immersiveMode = false;
    private OnBackPressedListener onBackPressedListener;

    public Bee7GameWallDialog(Context context, boolean immersiveMode) {
        super(context, R.style.GameWallDialogTheme);

        this.immersiveMode = immersiveMode;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            if (immersiveMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Utils.setImmersiveModeFlags(getWindow().getDecorView());
            }
        }
    }

    public void setOnBackPressedListener(OnBackPressedListener onBackPressedListener) {
        this.onBackPressedListener = onBackPressedListener;
    }

    @Override
    public void onBackPressed() {
        if(onBackPressedListener != null) {
            onBackPressedListener.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }
}
