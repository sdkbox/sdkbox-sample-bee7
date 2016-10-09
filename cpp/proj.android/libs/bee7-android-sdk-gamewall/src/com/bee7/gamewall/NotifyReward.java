package com.bee7.gamewall;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;

import com.bee7.gamewall.dialogs.DialogReward;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.DefaultPublisher;

/**
 * Reward notification wrapper, controls bubble visibility
 */
public class NotifyReward {
	public interface DisplayedChangeListener {
		public void onDisplayedChange(boolean displayed);
	}
	
    public static final String TAG = NotifyReward.class.getName();

    public RewardQueue messageQueue;
    public Activity activity;
    public long executeTime;
    public boolean queueOnStoppedQueue;

    protected Lock msgLock = new ReentrantLock();
    protected boolean dontRun;
    private DialogReward dialogReward;

    private String mText;
    private Bitmap mAppIcon;
    private Drawable mVCIcon;
    private Drawable mPublisherIcon;
    private boolean mVideoReward;
    public String pendingId;
    
    private DisplayedChangeListener mDisplayedListener;

    private long now;
    private Handler handler;

    public NotifyReward(DisplayedChangeListener displayedListener) {
        this.mDisplayedListener = displayedListener;
    }

    public NotifyReward addMsg(String text, Bitmap appIcon, Drawable vcIcon, Drawable publisherIcon, boolean videoReward, String pendingId) {
        Logger.debug(TAG, "addMsg isVideoReward " + videoReward);
        mText = text;
        mAppIcon = appIcon;
        mVCIcon = vcIcon;
        mPublisherIcon = publisherIcon;
        mVideoReward = videoReward;
        this.pendingId = pendingId;

        return this;
    }

    public synchronized boolean exec(DefaultPublisher publisher) {
        dontRun = false;
        now = System.currentTimeMillis();
        executeTime = now;
        handler = new Handler();

        if (publisher != null) {
            publisher.removePendingReward(pendingId);
        }

        dialogReward = new DialogReward(activity, mVideoReward, messageQueue.getImmersiveMode());
        dialogReward.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                handler.removeCallbacks(runnable);
                runnable.run();
            }
        });

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Logger.debug(TAG, "exec runOnUiThread run");
                msgLock.lock();
                try {
                    if (dontRun) {
                        return;
                    }
                    dialogReward.show(mText, mAppIcon, mVCIcon, mPublisherIcon);
                } finally {
                    msgLock.unlock();
                }
            }
        });

        handler.postDelayed(runnable, 10 * 1000);

        return true;
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            removeBubble(true, now);
        }
    };

    public synchronized void removeBubble(boolean removeMsg, long tm) {
        if (tm != executeTime && tm != 0) {
            return;
        }
        msgLock.lock();

        try {
            dontRun = true;
        } finally {
            msgLock.unlock();
        }

        if (dialogReward == null) {
            return;
        }

        if (dialogReward.isShowing()) {
            try {
                dialogReward.dismiss();
            } catch (Exception e) {
                Logger.warn(TAG, e, "Failed to dismiss reward dialog, already removed.");
            }
        }


        long now = System.currentTimeMillis();

        if (removeMsg) {
            messageQueue.removeMessage();
        } else if (now - executeTime > 3 * 1000) {
            messageQueue.dropMessage();
        }
        
        if (mDisplayedListener != null) {
        	mDisplayedListener.onDisplayedChange(false);
        }
    }
}