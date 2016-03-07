package com.bee7.gamewall.video.exoplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bee7.gamewall.R;
import com.bee7.gamewall.video.AbstractVideoPlayer;
import com.bee7.gamewall.video.DemoUtil;
import com.bee7.gamewall.video.VideoCallbackListener;
import com.bee7.gamewall.video.VideoPlayerInterface;
import com.bee7.sdk.common.util.Logger;
import com.bee7.sdk.common.util.SharedPreferencesHelper;
import com.bee7.sdk.publisher.GameWallConfiguration;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.exoplayer.metadata.GeobMetadata;
import com.google.android.exoplayer.metadata.PrivMetadata;
import com.google.android.exoplayer.metadata.TxxxMetadata;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class ExoVideoPlayer extends AbstractVideoPlayer implements VideoPlayerInterface, DemoPlayer.Listener,
        DemoPlayer.Id3MetadataListener, AudioCapabilitiesReceiver.Listener, TextureView.SurfaceTextureListener {

    private static final String TAG = ExoVideoPlayer.class.toString();

    private VideoCallbackListener videoCallbackListener;
    private Context context;
    private boolean waitForVideoShow;
    private GameWallConfiguration.VideoPrequalGlobalConfig videoPrequalGlobalConfig;
    private Uri contentUri;
    private long playerPosition;
    private long playerDuration;
    private boolean audioMute;

    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private TextureView textureView;
    private AudioCapabilities audioCapabilities;
    private DemoPlayer player;
    private boolean playerNeedsPrepare;
    private EventLogger eventLogger;
    private TextView playerStateTextView;
    private Handler handler = new Handler();

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public ExoVideoPlayer(Context _context, String videoUrl, long seekPosition, boolean audioMute, boolean waitForVideoShow,
                          VideoCallbackListener videoCallbackListener, GameWallConfiguration.VideoPrequalGlobalConfig videoPrequalGlobalConfig) {
        if (videoCallbackListener == null) {
            throw new RuntimeException("VideoCallbackListener can not be null");
        }
        this.videoCallbackListener = videoCallbackListener;
        this.context = _context;
        this.videoPrequalGlobalConfig = videoPrequalGlobalConfig;
        this.contentUri = Uri.parse(videoUrl);
        this.waitForVideoShow = waitForVideoShow;
        this.playerPosition = seekPosition;
        this.audioMute = context.getSharedPreferences(PREF_COM_PLAYER, Context.MODE_PRIVATE).getBoolean(PREF_PLAYER_MUTE_CONF_KEY, false);
        this.playerDuration = 0;

        this.audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(context.getApplicationContext(), this);

        this.textureView = new TextureView(context);
        this.textureView.setSurfaceTextureListener(this);

        videoCallbackListener.onBuffer(true);
        if (!waitForVideoShow) {
            ViewGroup parent = videoCallbackListener.onSurfaceView(textureView);

            //playerStateTextView = new TextView(context);
            //playerStateTextView.setTextColor(Color.WHITE);
            //parent.addView(playerStateTextView);

            preparePlayer();
        }

        DemoUtil.setDefaultCookieManager();
    }

    @Override
    public void onResume() {
        // The player will be prepared on receiving audio capabilities.
        audioCapabilitiesReceiver.register();
    }

    @Override
    public void onPause() {
        releasePlayer();
        audioCapabilitiesReceiver.unregister();
    }

    @Override
    public void onDestroy() {
        releasePlayer();
        videoPrequalGlobalConfig = null;
        videoCallbackListener = null;
        textureView = null;
        audioCapabilitiesReceiver = null;
        audioCapabilities = null;
    }

    @Override
    public boolean replayVideo() {
        if (player != null) {
            player.seekTo(DemoUtil.DEMO_SEEK_TIME);
            player.setPlayWhenReady(true);
            return true;
        } else {
            playerPosition = 0;
            preparePlayer();
            return false;
        }
    }

    // AudioCapabilitiesReceiver.Listener methods

    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        boolean audioCapabilitiesChanged = !audioCapabilities.equals(this.audioCapabilities);
        if (player == null || audioCapabilitiesChanged) {
            this.audioCapabilities = audioCapabilities;
            releasePlayer();
            preparePlayer();
        } else if (player != null) {
            player.setBackgrounded(false);
        }
    }

    // Internal methods

    private DemoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = DemoUtil.getUserAgent(context);
        return new HlsRendererBuilder(context, userAgent, contentUri.toString(),
                audioCapabilities, videoPrequalGlobalConfig);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected void preparePlayer() {
        if (player == null) {
            player = new DemoPlayer(getRendererBuilder(), videoPrequalGlobalConfig, audioMute);
            player.addListener(this);
            player.setMetadataListener(this);
            player.seekTo(playerPosition);
            playerNeedsPrepare = true;
            eventLogger = new EventLogger();
            eventLogger.startSession();
            player.addListener(eventLogger);
            player.setInfoListener(eventLogger);
            player.setInternalErrorListener(eventLogger);
        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
            //updateButtonVisibilities();
        }
        if (textureView.getSurfaceTexture() != null) {
            Surface surface = new Surface(textureView.getSurfaceTexture());
            player.setSurface(surface);
        }
        //player.setSurface(surfaceView.getHolder().getSurface());
        player.setPlayWhenReady(true);
    }

    @Override
    protected void releasePlayer() {
        if (player != null) {
            playerPosition = player.getCurrentPosition();
            playerDuration = player.getDuration();
            player.release();
            player = null;
            eventLogger.endSession();
            eventLogger = null;
        }
    }

    // DemoPlayer.Listener implementation

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == DemoPlayer.STATE_ENDED) {
            stopProgressUpdateReporting();
            videoCallbackListener.onVideoEnd(getProgress(), false);
        } else if (playbackState == DemoPlayer.STATE_READY) {
            textureView.setVisibility(View.VISIBLE);
            videoCallbackListener.onVideoStart();
            startProgressUpdateReporting();
        }

        if (playbackState == DemoPlayer.STATE_BUFFERING) {
            videoCallbackListener.onBuffer(true);
        } else {
            videoCallbackListener.onBuffer(false);
        }

        if (playerStateTextView != null) {
            String text = "playWhenReady=" + playWhenReady + ", playbackState=";
            switch(playbackState) {
                case ExoPlayer.STATE_BUFFERING:
                    text += "buffering";
                    break;
                case ExoPlayer.STATE_ENDED:
                    text += "ended";
                    break;
                case ExoPlayer.STATE_IDLE:
                    text += "idle";
                    break;
                case ExoPlayer.STATE_PREPARING:
                    text += "preparing";
                    break;
                case ExoPlayer.STATE_READY:
                    text += "ready";
                    break;
                default:
                    text += "unknown";
                    break;
            }

            playerStateTextView.setText(text);
        }

    }

    @Override
    public void onError(Exception e) {
        if (e instanceof UnsupportedDrmException) {
            // Special case DRM failures.
            UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
            int stringId = unsupportedDrmException.reason == UnsupportedDrmException.REASON_NO_DRM
                    ? R.string.drm_error_not_supported
                    : unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                    ? R.string.drm_error_unsupported_scheme
                    : R.string.drm_error_unknown;
            Toast.makeText(context.getApplicationContext(), stringId, Toast.LENGTH_LONG).show();
        }
        stopProgressUpdateReporting();
        videoCallbackListener.onVideoEnd(getProgress(), true);
        videoCallbackListener.onError(e.getMessage());
        playerNeedsPrepare = true;
        releasePlayer();
    }

    @Override
    public void onVideoSizeChanged(int width, int height, float pixelWidthHeightRatio) {
        if (textureView != null) {
            int newVideoWidth;
            int newVideoHeight;

            if (width <= height) {
                newVideoWidth = textureView.getWidth();
                newVideoHeight = (int) (textureView.getWidth() / ((float)width / (float)height));
            } else {
                newVideoWidth = (int) (textureView.getHeight() * ((float)width / (float)height));
                newVideoHeight = textureView.getHeight();
            }

            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) textureView.getLayoutParams();
            params.height = newVideoHeight;
            params.width = newVideoWidth;
            params.gravity = Gravity.CENTER;
            textureView.setLayoutParams(params);
        }
    }

    @Override
    public void onId3Metadata(Map<String, Object> metadata) {
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (TxxxMetadata.TYPE.equals(entry.getKey())) {
                TxxxMetadata txxxMetadata = (TxxxMetadata) entry.getValue();
                Log.i(TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s",
                        TxxxMetadata.TYPE, txxxMetadata.description, txxxMetadata.value));
            } else if (PrivMetadata.TYPE.equals(entry.getKey())) {
                PrivMetadata privMetadata = (PrivMetadata) entry.getValue();
                Log.i(TAG, String.format("ID3 TimedMetadata %s: owner=%s",
                        PrivMetadata.TYPE, privMetadata.owner));
            } else if (GeobMetadata.TYPE.equals(entry.getKey())) {
                GeobMetadata geobMetadata = (GeobMetadata) entry.getValue();
                Log.i(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
                        GeobMetadata.TYPE, geobMetadata.mimeType, geobMetadata.filename,
                        geobMetadata.description));
            } else {
                Log.i(TAG, String.format("ID3 TimedMetadata %s", entry.getKey()));
            }
        }
    }

    @Override
    public long getCurrentProgress() {
        if (player != null) {
            return player.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public boolean isVideoMuted() {
        return audioMute;
    }

    @Override
    public boolean toggleSound() {
        if (player != null) {
            if (audioMute) {
                player.setMute(false);
                audioMute = false;
                context.getSharedPreferences(PREF_COM_PLAYER, Context.MODE_PRIVATE).edit().putBoolean(PREF_PLAYER_MUTE_CONF_KEY, false).commit();
                return true;
            } else {
                player.setMute(true);
                audioMute = true;
                context.getSharedPreferences(PREF_COM_PLAYER, Context.MODE_PRIVATE).edit().putBoolean(PREF_PLAYER_MUTE_CONF_KEY, true).commit();
                return false;
            }
        }

        return false;
    }

    @Override
    public int getProgress() {
        int playedPercentage = -1;
        if (player != null) {
            try {
                //video played position in %
                playedPercentage = (int) (((double)player.getCurrentPosition() / (double)player.getDuration()) * 100);
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
        if (waitForVideoShow) {
            ViewGroup parent = videoCallbackListener.onSurfaceView(textureView);

            //playerStateTextView = new TextView(context);
            //playerStateTextView.setTextColor(Color.WHITE);
            //parent.addView(playerStateTextView);

            preparePlayer();
        }
    }

    @Override
    public void stopVideo() {
        releasePlayer();
    }

    @Override
    public boolean isVideoPlaying() {
        return player != null  && player.getPlayerControl().isPlaying();
    }

    @Override
    public void hideMediaController() {
        //not used
    }

    @Override
    public void showMediaController() {
        //not used
    }

    @Override
    public void pauseVideo() {
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    public void resumeVideo() {
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public boolean isFinishedPlaying() {
        return getProgress() >= 98;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Logger.debug("SurfaceTextureListener", "onSurfaceTextureAvailable");
        if (player != null) {
            Surface surface = new Surface(surfaceTexture);
            player.setSurface(surface);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Logger.debug("SurfaceTextureListener", "onSurfaceTextureSizeChanged");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Logger.debug("SurfaceTextureListener", "onSurfaceTextureDestroyed");
        if (player != null) {
            player.blockingClearSurface();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void startProgressUpdateReporting() {
        handler.postDelayed(new Runnable() {
            public void run() {
                if (videoCallbackListener != null && player != null) {
                    long timeToEnd = player.getDuration() - player.getCurrentPosition();
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
        if ((player.getDuration() - player.getCurrentPosition()) <= 2000 ) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void seekToVideonEnd(long millisecondsFromEnd) {
        if (player != null) {
            player.seekTo(player.getDuration() - millisecondsFromEnd);
            player.setPlayWhenReady(true);
        } else {
            stopProgressUpdateReporting();
            videoCallbackListener.onVideoEnd(getProgress(), false);
        }
    }

}
