package com.bee7.gamewall;

import java.util.LinkedList;
import java.util.Queue;

import android.app.Activity;

import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.publisher.DefaultPublisher;

/**
 * Reward notification queue implementation.
 * There can be multiple claimed rewards, and they should be displayed in sequence.
 */
public class RewardQueue {
    public static final String TAG = RewardQueue.class.getName();

    private Queue<NotifyReward> q = new LinkedList<NotifyReward>();
    private boolean isProcessing;

    private Activity activity;

    private DefaultPublisher mPublisher;
    private boolean immersiveMode;

    public RewardQueue(DefaultPublisher publisher) {
        mPublisher = publisher;
    }

    public synchronized RewardQueue addMessage(NotifyReward msg) {
        if (!isProcessing && !msg.queueOnStoppedQueue) {
            return this;
        }

        q.offer(msg);

        msg.messageQueue = this;

        if (q.size() == 1 && isProcessing) {
            msg.activity = activity;
            msg.exec(mPublisher);
        }
        return this;
    }

    public synchronized RewardQueue removeMessage() {
        if (!isProcessing) {
            return this;
        }

        q.poll();
        if (q.size() == 0) {
            return this;
        }

        NotifyReward msg = q.peek();
        msg.activity = activity;
        msg.exec(mPublisher);

        return this;
    }

    public synchronized RewardQueue dropMessage() {
        q.poll();
        return this;
    }

    public synchronized RewardQueue startProcessing(Activity activity, boolean immersiveMode) {
        Logger.debug(TAG, "startProcessing");
        if(isProcessing) return this;

        this.immersiveMode = immersiveMode;
        this.activity = activity;
        isProcessing = true;

        NotifyReward msg = q.peek();

        if (msg == null) {
            Logger.debug(TAG, "msg == null");
            return this;
        }

        msg.activity = activity;
        Logger.debug(TAG, "msg.exec()");
        msg.exec(mPublisher);

        return this;
    }

    public boolean getImmersiveMode() {
        return immersiveMode;
    }
    public void setImmersiveMode(boolean immersiveMode) {
        this.immersiveMode = immersiveMode;
    }
}
