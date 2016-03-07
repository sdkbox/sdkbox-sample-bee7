package com.bee7.gamewall.video;

import android.view.TextureView;
import android.view.ViewGroup;

public interface VideoCallbackListener {
    /**
     * Returns TextureView that is used by the player for video playback.
     *
     * @param textureView to be added to layout.
     */
    ViewGroup onSurfaceView(TextureView textureView);

    /**
     *
     * @param exception that has occurred.
     */
    void onError(String exception);

    /**
     * Returns when video has stopped playing (Video finished or canceled by user).
     *
     * @param videoPlayed player position in %.
     * @param error flag if error happened
     */
    void onVideoEnd(int videoPlayed, boolean error);

    /**
     * Returns when video starts to play.
     */
    void onVideoStart();

    /**
     *
     * @param buffering
     */
    void onBuffer(boolean buffering);

    /**
     *
     * @param progress in seconds
     */
    void onTimeToEndUpdate(long progress);
}
