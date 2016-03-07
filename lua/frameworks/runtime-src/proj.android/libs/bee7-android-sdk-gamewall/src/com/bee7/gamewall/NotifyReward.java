package com.bee7.gamewall;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;

import com.bee7.gamewall.dialogs.DialogReward;
import com.bee7.sdk.common.util.Logger;

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
    
    private DisplayedChangeListener mDisplayedListener;

    public NotifyReward(Activity activity, DisplayedChangeListener displayedListener) {
        this.activity = activity;
        this.mDisplayedListener = displayedListener;
    }

    public NotifyReward addMsg(String text, Bitmap appIcon, Drawable vcIcon, Drawable publisherIcon, boolean videoReward) {
        mText = text;
        mAppIcon = appIcon;
        mVCIcon = vcIcon;
        mPublisherIcon = publisherIcon;
        mVideoReward = videoReward;

        return this;
    }

    public synchronized boolean exec() {
        dontRun = false;
        final long now = System.currentTimeMillis();
        executeTime = now;
        dialogReward = new DialogReward(activity, mVideoReward);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                removeBubble(true, now);
            }
        }, 10 * 1000);

        return true;
    }

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