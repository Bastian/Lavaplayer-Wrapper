package org.javacord.lavaplayerwrapper;

import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;

/**
 * An audio source that can play YouTube videos.
 */
public class YouTubeAudioSource extends AudioSourceBase {

    private final AudioPlayer player;
    private AudioFrame lastFrame;
    private final String url;
    private volatile AudioTrack track;

    /**
     * Creates a new YouTube audio source.
     *
     * @param api A discord api instance.
     * @param url The url of the youtube video.
     */
    public YouTubeAudioSource(DiscordApi api, String url) {
        super(api);
        this.url = url;

        // Do all the Lavaplayer stuff
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        player = playerManager.createPlayer();
        playerManager.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);

        // Load the youtube video
        playerManager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                track = audioTrack;
                player.playTrack(audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                // TODO handle this case
            }

            @Override
            public void noMatches() {
                // TODO handle this case
            }

            @Override
            public void loadFailed(FriendlyException e) {
                // TODO handle this case
            }
        });
    }

    @Override
    public byte[] getNextFrame() {
        return lastFrame.getData();
    }

    @Override
    public boolean hasFinished() {
        return track != null && track.getState() == AudioTrackState.FINISHED;
    }

    @Override
    public boolean hasNextFrame() {
        lastFrame = player.provide();
        return lastFrame != null;
    }

    @Override
    public AudioSource clone() {
        return new YouTubeAudioSource(getApi(), url);
    }
}
