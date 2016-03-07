package com.bee7.gamewall.assets;

import android.content.Context;
import android.graphics.Bitmap;

import java.net.URL;

/**
 * Created by Bee7 on 02/12/14.
 */
public abstract class AssetsManagerSetBitmapTask {
    private final Context context;
    private final URL iconURL;

    private Object params;
    private UnscaledBitmapLoader.ScreenDPI sourceImageDPI = UnscaledBitmapLoader.ScreenDPI.DENSITY_HDPI;

    public abstract void bitmapLoadedPost(Bitmap bitmap);

    public AssetsManagerSetBitmapTask(URL iconURL, Context context) {
        this.iconURL = iconURL;
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public URL getIconURL() {
        return iconURL;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    public UnscaledBitmapLoader.ScreenDPI getSourceImageDPI() {
        return sourceImageDPI;
    }

    public void setSourceImageDPI(UnscaledBitmapLoader.ScreenDPI sourceImageDPI) {
        this.sourceImageDPI = sourceImageDPI;
    }
}
