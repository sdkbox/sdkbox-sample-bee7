package com.bee7.gamewall.video;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.view.Surface;
import android.view.TextureView;

import com.bee7.sdk.common.util.Logger;

import java.util.concurrent.TimeUnit;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class NativeVideoPlayer extends AbstractVideoPlayer implements VideoPlayerInterface, TextureView.SurfaceTextureListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    String TAG = NativeVideoPlayer.class.toString();

    private TextureView textureView;
    private MediaPlayer mediaPlayer;
    private VideoCallbackListener videoCallbackListener;
    private boolean waitForVideoShow;
    private String contentUri;
    private boolean audioMute;
    private long playerPosition;
    private long playerDuration;
    private int audioVolume = 1;
    private Handler handler = new Handler();

    public NativeVideoPlayer(Context context, String videoUrl, long seekPosition, boolean audioMute, boolean waitForVideoShow, VideoCallbackListener videoCallbackListener) {
        Logger.debug(TAG, "NativeVideoPlayer");
        if (videoCallbackListener == null) {
            throw new RuntimeException("VideoCallbackListener can not be null");
        }

        this.videoCallbackListener = videoCallbackListener;
        this.waitForVideoShow = waitForVideoShow;
        this.contentUri = videoUrl;
        this.audioMute = audioMute;
        this.playerPosition = seekPosition;
        this.playerDuration = 0;

        if (audioMute) {
            audioVolume = 0;
        } else {
            audioVolume = 1;
        }

        videoCallbackListener.onBuffer(true);

        textureView = new TextureView(context);
        textureView.setSurfaceTextureListener(this);
    }

    /**
     * AbstractVideoPlayer
     */
    @Override
    protected void preparePlayer() {
        Logger.debug(TAG, "preparePlayer");
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(contentUri);

            mediaPlayer.setOnErrorListener(this);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            if (audioMute) {
                muteSound();
            }

            if (textureView.getSurfaceTexture() != null) {
                Surface surface = new Surface(textureView.getSurfaceTexture());
                Logger.debug(TAG, "mediaPlayer.setSurface");
                mediaPlayer.setSurface(surface);
            }

            mediaPlayer.prepareAsync();
        } catch(Exception e) {
            e.printStackTrace();
            videoCallbackListener.onError("NativeMediaPlayer preparePlayer " + e.getMessage());
            stopVideo();
        }
    }

    @Override
    protected void releasePlayer() {
        Logger.debug(TAG, "releasePlayer");
        if (mediaPlayer != null) {
            if (isVideoMuted()) {
                audioVolume = 0;
            } else {
                audioVolume = 1;
            }
            playerPosition = mediaPlayer.getCurrentPosition();
            playerDuration = mediaPlayer.getDuration();

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * TextureView.SurfaceTextureListener
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Logger.debug(TAG, "onSurfaceTextureAvailable");
        preparePlayer();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        //not implemented
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Logger.debug(TAG, "onSurfaceTextureDestroyed");
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //not implemented
    }

    /**
     * MediaPlayer.OnPreparedListener
     */
    @Override
    public void onPrepared(MediaPlayer mp) {
        Logger.debug(TAG, "onPrepared");
        startVideoPlayback();
    }

    /**
     * MediaPlayer.OnCompletionListener
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        Logger.debug(TAG, "onCompletion");
        stopProgressUpdateReporting();
        videoCallbackListener.onVideoEnd(getProgress(), false);
    }

    /**
     * MediaPlayer.OnErrorListener
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Logger.debug(TAG, "onError");
        videoCallbackListener.onVideoEnd(getProgress(), true);
        videoCallbackListener.onError("NativeMediaPlayer: what:" + what + ", extra:" + extra);
        stopVideo();
        return false;
    }

    /**
     * VideoPlayerInterface
     */
    @Override
    public void onResume() {
        preparePlayer();
    }

    @Override
    public void onPause() {
       stopVideo();
    }

    @Override
    public void onDestroy() {
        stopVideo();
        videoCallbackListener = null;
        textureView = null;
    }

    @Override
    public long getCurrentProgress() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public boolean isVideoMuted() {
        if (mediaPlayer != null) {
            if (audioVolume > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean toggleSound() {
        if (mediaPlayer != null) {
            if (audioVolume == 1) {
                muteSound();
                return false;
            } else {
                mediaPlayer.setVolume(1,1);
                audioVolume = 1;
                audioMute = false;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean replayVideo() {
        if (mediaPlayer != null) {
            startVideoPlayback();
            return true;
        } else {
            preparePlayer();
            return false;
        }
    }

    @Override
    public int getProgress() {
        int playedPercentage = -1;
        if (mediaPlayer != null) {
            try {
                //video played position in %
                playedPercentage = (int) (((double)mediaPlayer.getCurrentPosition() / (double)mediaPlayer.getDuration()) * 100);
            } catch (Exception ignored) { }
        } else {
            try {
                //video played position in %
                playedPercentage = (int) (((double)playerPosition / (double)playerDuration) * 100);
            } catch (Exception ignored) { }
        }

        return playedPercentage;
    }

    @Override
    public void showVideo() {
        Logger.debug(TAG, "showVideo");
        if (waitForVideoShow) {
            Logger.debug(TAG, "onSurfaceView");
            videoCallbackListener.onSurfaceView(textureView);
        }
    }

    @Override
    public void stopVideo() {
        releasePlayer();
    }

    @Override
    public boolean isVideoPlaying() {
        if (mediaPlayer != null) {
            return mediaPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public void hideMediaController() {
        //not implemented
    }

    @Override
    public void showMediaController() {
        //not implemented
    }

    @Override
    public void pauseVideo() {
        releasePlayer();
    }

    @Override
    public void resumeVideo() {
        preparePlayer();
    }

    @Override
    public boolean isFinishedPlaying() {
        return getProgress() >= 98;
    }

    /**
     * private methods
     */
    public void muteSound() {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(0,0);
            audioVolume = 0;
            audioMute = true;
        }
    }

    private void startVideoPlayback() {
        Logger.debug(TAG, "startVideoPlayback " + isVideoPlaying());
        if (!isVideoPlaying() && mediaPlayer != null) {
            Logger.debug(TAG, "startVideoPlayback");
            mediaPlayer.seekTo((int) playerPosition);
            mediaPlayer.start();

            startProgressUpdateReporting();

            videoCallbackListener.onBuffer(false);
            videoCallbackListener.onVideoStart();
        }
    }

    private void startProgressUpdateReporting() {
        handler.postDelayed(new Runnable(){
            public void run(){
                if (videoCallbackListener != null && mediaPlayer != null) {
                    long timeToEnd = mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition();
                    if (timeToEnd >= 0) {
                        videoCallbackListener.onTimeToEndUpdate(TimeUnit.MILLISECONDS.toSeconds(timeToEnd));
                    }
                } else {
                    stopProgressUpdateReporting();
                }
                handler.postDelayed(this, 1000);
            }
        }, 0);
    }

    private void stopProgressUpdateReporting() {
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean isVideoAtEnd() {
        if (mediaPlayer != null && (mediaPlayer.getDuration() - mediaPlayer.getCurrentPosition()) <= 2000) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void seekToVideonEnd(long millisecondsFromEnd) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo((int) (mediaPlayer.getDuration() - millisecondsFromEnd));
            mediaPlayer.start();
        } else {
            stopProgressUpdateReporting();
            videoCallbackListener.onVideoEnd(getProgress(), false);
        }
    }

}