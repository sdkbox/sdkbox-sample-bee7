package com.bee7.gamewall.assets;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import com.bee7.gamewall.R;
import com.bee7.sdk.common.NonObfuscatable;
import com.bee7.sdk.common.util.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Bee7 on 02/12/14.
 */
public class AssetsManager {
    static final String TAG = AssetsManager.class.getSimpleName();

    private static final String CACHE_LOCAL_DIR = ".Bee7PublisherImgCache";

    private static AssetsManager instance;

    private final Set<URL> currDownloads = new HashSet<URL>();

    private File cacheDir;

    public static enum IconUrlSize implements NonObfuscatable {
        SMALL, LARGE
    }

    private final static String DEFAULT_DRAWABLE_ICON_URL_120 = "https://storage.googleapis.com/bee7/gamewall.default.icon/icon120.png";
    private final static String DEFAULT_DRAWABLE_ICON_URL_240 = "https://storage.googleapis.com/bee7/gamewall.default.icon/icon240.png";

    private AssetsManager() {}

    public static AssetsManager getInstance() {
        if (instance == null) {
            instance = new AssetsManager();
        }

        return instance;
    }

    private String md5(String s) {
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(s.getBytes("UTF-8"));
            byte messageDigest[] = algorithm.digest();

            // convert to hex string
            StringBuffer sigHex = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xff & messageDigest[i]);
                if (hex.length() == 1) {
                    sigHex.append('0');
                }
                sigHex.append(hex);
            }
            return sigHex.toString();
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private byte[] download(URL thumbLink) {
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpUriRequest request;
            try {
                request = new HttpGet(thumbLink.toString());
                HttpResponse response = httpClient.execute(request);
                StatusLine statusLine = response.getStatusLine();
                if (statusLine.getStatusCode() != 200) return null;

                HttpEntity entity = response.getEntity();
                if (entity == null) return null;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                entity.writeTo(baos);
                entity.consumeContent();
                return baos.toByteArray();
            } catch (Exception e) {
                Logger.error(TAG, e, "Failed to download url {0}", thumbLink.toString());
            }
            return null;
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
    }

    public Bitmap makeBitmap(byte[] ba, Context context, int density) {
        if (ba == null) {
            Logger.debug(TAG, "Cached bitmap not provided for makeBitmap");
            return null;
        }

        ByteArrayInputStream is = null;

        try {
            is = new ByteArrayInputStream(ba);

            /*
             * The following doesn't work on Xperia C1505, OS 4.1.1: Bitmap bm = BitmapFactory.decodeStream(is, null,
             * UnscaledBitmapLoader.getStandardOptionsScaledForDPI( task.getSourceImageDPI(),
             * task.getContext().getResources())); Therefore load the unscaled bitmap and resize manually. Don't ask...
             * *sigh*
             */
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            options.inDither = true;
            Bitmap bm = BitmapFactory.decodeStream(is, null, options);
            float r = (float) context.getResources().getDisplayMetrics().densityDpi / density;
            bm = Bitmap.createScaledBitmap(bm, (int) (bm.getWidth() * r), (int) (bm.getHeight() * r), true);
            return bm;
        } catch (Exception ex) {
            Logger.debug(TAG, ex, "Failed to make bitmap");

            return null;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /*
     * Sync'ed so only one thread at a time will touch the cache.
     */
    private synchronized Bitmap getDrawable(final AssetsManagerSetBitmapTask task) {
        URL thumbLink = task.getIconURL();
        byte[] data = getCachedBitmap(task.getContext(), task.getIconURL());

        // Got a cached bitmap?
        if (data != null) {
            /*
             * Yep. Let's see if it's valid and if yes, use it. If not, delete the file from the cache, unless I got
             * OOM, in which case the cache entry is valid and it won't be redownloaded after we fall through.
             */
            try {
                Bitmap bm = makeBitmap(data, task.getContext(), task.getSourceImageDPI().getDensity());
                if (bm != null) return bm;
                deleteFromDisk(thumbLink);
            } catch (OutOfMemoryError e) {
                Logger.error(TAG, e, "Failed to get bitmap {0}", task.getIconURL());
            } catch (Throwable e) {
                Logger.error(TAG, e, "Failed to get bitmap {0}", task.getIconURL());
                deleteFromDisk(thumbLink);
            }
        }

        Logger.debug(TAG, "Adding download icon: {0}", task.getIconURL());

        new Thread(new Runnable() {
            @Override
            public void run() {
                URL thumbLink = task.getIconURL();
                Logger.debug(TAG, "Downloading task started: {0}", task.getIconURL());

                byte[] data = getCachedBitmap(task.getContext(), task.getIconURL());
                if (data == null) {
                    Logger.debug(TAG, "Downloading icon: {0}", task.getIconURL());

                    // Cache it.
                    cacheBitmap(thumbLink);

                    // The image might have been cached while this thread was waiting for its turn to download it.
                    //                    if (haveCachedBitmap(thumbLink)) return;

                    //                    data = download(thumbLink);
                    //                    if (data != null) {
                    //                        storeToDisk(data, thumbLink);
                    //                    }

                    Logger.debug(TAG, "Done downloading icon: {0}", task.getIconURL());

                    // Do I have it?
                    data = getCachedBitmap(task.getContext(), task.getIconURL());
                    // If not invalid or still null, return.
                    if (data == null) {
                        Logger.error(TAG, "Can't get icon {0}", task.getIconURL());

                        IconUrlSize iconUrlSize = getDefaultIconUrlSize(task.getContext().getResources());
                        String iconUrl = DEFAULT_DRAWABLE_ICON_URL_120;

                        switch (iconUrlSize) {
                            case LARGE:
                                iconUrl = DEFAULT_DRAWABLE_ICON_URL_240; // large
                                break;
                        }

                        //check if we have default icon in cache
                        try {
                            URL defaultDrawableUrl = new URL(iconUrl);

                            byte[] defaultDataIcon = getCachedBitmap(task.getContext(), defaultDrawableUrl);
                            if (defaultDataIcon == null) {
                                //Download it. Cache it. Check it. Use it.
                                Logger.debug(TAG, "Downloading icon: {0}", defaultDrawableUrl);
                                cacheBitmap(defaultDrawableUrl);
                                Logger.debug(TAG, "Done downloading icon: {0}", defaultDrawableUrl);

                                // Do I have it?
                                defaultDataIcon = getCachedBitmap(task.getContext(), defaultDrawableUrl);
                                if (defaultDataIcon == null) {
                                    //just use de default that comes from resources
                                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                                        @Override
                                        public void run() {
                                            task.bitmapLoadedPost(getDefaultBitmap(task.getContext()));
                                        }
                                    });
                                }

                            } else {
                                Logger.debug(TAG, "Already downloaded: {0}", defaultDrawableUrl);
                            }

                            makeAndApplyBitmap(defaultDataIcon, task, defaultDrawableUrl);
                        } catch (MalformedURLException e) {
                            Logger.warn(TAG, e, "DEFAULT_DRAWABLE_ICON_URL = " + iconUrl);

                            //just use de default that comes from resources
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    task.bitmapLoadedPost(getDefaultBitmap(task.getContext()));
                                }
                            });
                        }
                        return;
                    }
                } else {
                    Logger.debug(TAG, "Already downloaded: {0}", task.getIconURL());
                }

                makeAndApplyBitmap(data, task, thumbLink);
            }
        }).start();

        return null;
    }

    private void makeAndApplyBitmap(byte[] data, final AssetsManagerSetBitmapTask task, URL thumbLink) {
        /*
         * Don't let any bitmap exceptions ruin the show, they are getting downloaded, after all. Just delete
         * the file from the cache unless it's an OOM.
         */
        try {
            final Bitmap bm = makeBitmap(data, task.getContext(), task.getSourceImageDPI().getDensity());
            if (bm == null) {
                Logger.error(TAG, "Can't make bitmap icon {0}", task.getIconURL());
                deleteFromDisk(thumbLink);
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        task.bitmapLoadedPost(getDefaultBitmap(task.getContext()));
                    }
                });
                return;
            }

            Logger.debug(TAG, "Applying icon: {0}", task.getIconURL());
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    task.bitmapLoadedPost(bm);
                }
            });
            return;
        } catch (OutOfMemoryError e) {
            Logger.error(TAG, e, "Failed to make bitmap {0}", task.getIconURL());
        } catch (Throwable e) {
            Logger.error(TAG, e, "Failed to make bitmap {0}", task.getIconURL());
            deleteFromDisk(thumbLink);
        }

        Logger.debug(TAG, "Got null for {0}", task.getIconURL());

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                task.bitmapLoadedPost(getDefaultBitmap(task.getContext()));
            }
        });
    }

    public static Bitmap getDefaultBitmap(Context context) {
        Drawable drawable = context.getResources().getDrawable(R.drawable.default_game_icon);
        int iconSize = context.getResources().getDimensionPixelSize(R.dimen.bee7_default_game_icon_size);

        Bitmap bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;

    }

    public synchronized byte[] getCachedBitmap(Context context, URL thumbLink) {
        if (cacheDir == null) {
            cacheDir = new File(context.getCacheDir(), CACHE_LOCAL_DIR);

            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
        }

        if(thumbLink == null) {
            return null;
        }
        File thumbFile = new File(cacheDir, md5(thumbLink.toString()));
        return readFromDisk(thumbFile);
    }

    private void cacheBitmap(final URL thumbLink) {
        // Check that I'm not already downloading this image.
        synchronized (currDownloads) {
            while (currDownloads.contains(thumbLink)) {
                try {
                    currDownloads.wait();
                } catch (InterruptedException e) {
                }
            }
            // Mark it as currently downloading.
            currDownloads.add(thumbLink);
        }
        try {
            // The image might have been cached while this thread was waiting for its turn to download it.
            if (haveCachedBitmap(thumbLink)) return;

            byte[] data = download(thumbLink);
            if (data != null) storeToDisk(data, thumbLink);
        } finally {
            synchronized (currDownloads) {
                currDownloads.remove(thumbLink);
                currDownloads.notifyAll();
            }
        }
    }

    private synchronized void storeToDisk(byte[] ba, URL thumbLink) {
        if (!isCacheValid()) return;

        File thumbFile = new File(cacheDir, md5(thumbLink.toString()));
        try {
            OutputStream os = new BufferedOutputStream(new FileOutputStream(thumbFile));
            try {
                os.write(ba);
            } catch (Exception e) {
                Logger.error(TAG, e, "Failed to store {0}", thumbFile.toString());
            } finally {
                os.close();
            }
        } catch (Exception e) {
            Logger.error(TAG, e, "Failed to store {0}", thumbFile.toString());
        }
    }

    private synchronized void deleteFromDisk(URL thumbLink) {
        File thumbFile = new File(cacheDir, md5(thumbLink.toString()));
        thumbFile.delete();
    }

    private synchronized boolean haveCachedBitmap(URL thumbLink) {
        File thumbFile = new File(cacheDir, md5(thumbLink.toString()));
        return thumbFile.exists();
    }

    private synchronized byte[] readFromDisk(File thumbFile) {
        if (!thumbFile.exists()) return null;

        byte[] ba = null;
        try {
            InputStream is = new BufferedInputStream(new FileInputStream(thumbFile));
            try {
                ba = new byte[(int) thumbFile.length()];
                int offset = 0, count = ba.length;
                while (true) {
                    if (count == 0) break;
                    int read = is.read(ba, offset, count);
                    if (read == -1) break;
                    offset += read;
                    count -= read;
                }
            } catch (Exception e) {
                Logger.error(TAG, e, "Failed to read file {0}", thumbFile.toString());
            } finally {
                is.close();
            }
        } catch (Exception e) {
            Logger.error(TAG, e, "Failed to read file {0}", thumbFile.toString());
        }

        return ba;
    }

    private boolean isCacheValid() {
        return cacheDir != null && cacheDir.exists() && cacheDir.isDirectory();
    }

    /**
     * This task calls {@link AssetsManagerSetBitmapTask#bitmapLoadedPost(android.graphics.Bitmap)} immediately, if a
     * cached bitmap is available. If it's not, it calls
     * {@link AssetsManagerSetBitmapTask#bitmapLoadedPost(android.graphics.Bitmap)} when the bitmap is downloaded and
     * it's in a new post (UI thread).
     *
     * @param task to postpone setting icons until the downloading of the bitmap is finished
     */
    public void runIconTask(AssetsManagerSetBitmapTask task) {
        Bitmap bitmap = getDrawable(task);

        if (bitmap != null) {
            task.bitmapLoadedPost(bitmap);
        }
    }

    public static IconUrlSize getDefaultIconUrlSize(Resources resources) {
        IconUrlSize iconUrlSize = IconUrlSize.SMALL;

        if (resources.getString(R.string.bee7_gamewallIconSize).equalsIgnoreCase("large")) {
            iconUrlSize = IconUrlSize.LARGE;
        }

        return iconUrlSize;
    }

    public Bitmap getVideoRewardBitmap(Context context) {
        Drawable drawable = context.getResources().getDrawable(R.drawable.bee7_btn_play_popup);
        if (drawable != null) {
            int iconHeight = drawable.getIntrinsicHeight();
            int iconWidth = drawable.getIntrinsicWidth();

            Bitmap bitmap = Bitmap.createBitmap(iconWidth, iconHeight, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return bitmap;
        }

        return null;
    }
}
