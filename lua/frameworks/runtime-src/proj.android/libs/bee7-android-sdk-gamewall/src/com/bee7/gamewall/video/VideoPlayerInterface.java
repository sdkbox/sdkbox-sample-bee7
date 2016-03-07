package com.bee7.gamewall.video;

public interface VideoPlayerInterface {
    /**
     * Activity lifecycle call. Should probably rename this to more descriptive name
     */
    void onResume();

    /**
     * Activity lifecycle call. Should probably rename this to more descriptive name
     */
    void onPause();

    /**
     * Activity lifecycle call. Should probably rename this to more descriptive name
     */
    void onDestroy();

    /**
     * Gets the current playback position in milliseconds.
     *
     * @return The current playback position in milliseconds.
     */
    long getCurrentProgress();

    /**
     * Returns if video is in mute status.
     *
     * @return true if audio is muted, false otherwise.
     */
    boolean isVideoMuted();

    /**
     * Toggles audio status (mute - unmute)
     *
     * @return true if audio is enabled, false otherwise.
     */
    boolean toggleSound();

    /**
     * Replays video from start if possible.
     *
     * @return true if video was instructed to replay, false otherwise.
     */
    boolean replayVideo();

    /**
     * Gets the current playback position in percentages.
     *
     * @return The current playback position in percentages.
     */
    int getProgress();

    /**
     * If we want to show actual video after we already instate video interface.
     */
    void showVideo();

    /**
     * Stops video when it is still playing (if we are removing view).
     */
    void stopVideo();

    /**
     * Returns if video is playing or not.
     *
     * @return true if player is preparing, buffering or its ready to play, false otherwise.
     */
    boolean isVideoPlaying();

    /**
     * Hides media controller component
     */
    void hideMediaController();

    /**
     * Shows media controller component
     */
    void showMediaController();

    /**
     * Pauses video (if activity goes to paused state)
     */
    void pauseVideo();

    /**
     * Resume video (if activity comes from paused state)
     */
    void resumeVideo();

    /**
     *
     * @return true if video finished playing
     */
    boolean isFinishedPlaying();

    /**
     *
     * @return true if video is at end (less than three seconds to end), false otherwise
     */
    boolean isVideoAtEnd();

    /**
     * This method triggers video end event.
     */
    void seekToVideonEnd(long millisecondsFromEnd);

}
