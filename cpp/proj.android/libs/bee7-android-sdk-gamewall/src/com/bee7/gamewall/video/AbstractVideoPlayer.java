package com.bee7.gamewall.video;

public abstract class AbstractVideoPlayer {

    protected static final String PREF_COM_PLAYER = "bee7PlayerConf";

    protected static final String PREF_PLAYER_MUTE_CONF_KEY = "pref_player_mute_conf_key";

    protected abstract void preparePlayer();
    protected abstract void releasePlayer();
}
