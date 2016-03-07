package com.bee7.gamewall;

import java.util.LinkedList;
import java.util.Queue;

import android.app.Activity;

/**
 * Reward notification queue implementation.
 * There can be multiple claimed rewards, and they should be displayed in sequence.
 */
public class RewardQueue {
    public static final String TAG = RewardQueue.class.getName();

    private Queue<NotifyReward> q = new LinkedList<NotifyReward>();
    private boolean isProcessing;

    private Activity activity;

    public synchronized RewardQueue addMessage(NotifyReward msg) {
        if (!isProcessing && !msg.queueOnStoppedQueue) {
            return this;
        }

        q.offer(msg);

        msg.messageQueue = this;

        if (q.size() == 1 && isProcessing) {
            msg.exec();
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
        msg.exec();

        return this;
    }

    public synchronized RewardQueue dropMessage() {
        q.poll();
        return this;
    }

    public synchronized RewardQueue startProcessing(Activity activity) {
        if(isProcessing) return this;

        this.activity = activity;
        isProcessing = true;

        NotifyReward msg = q.peek();

        if (msg == null) {
            return this;
        }

        msg.activity = activity;
        msg.exec();

        return this;
    }

    public synchronized RewardQueue stopProcessing() {
        isProcessing = false;

        NotifyReward msg = q.peek();

        if (msg == null) {
            return this;
        }

        msg.removeBubble(false, 0);

        return this;
    }

    public int getQueueLength() {
        return q.size();
    }
}
