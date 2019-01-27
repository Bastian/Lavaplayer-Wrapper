package org.javacord.lavaplayerwrapper.youtube;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import org.javacord.api.DiscordApi;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Used to build {@link YouTubeAudioSource}s.
 */
public class YouTubeAudioSourceBuilder {

    private final DiscordApi api;

    private String url;

    /**
     * Creates a new audio source builder.
     *
     * @param api The Discord api instance.
     */
    public YouTubeAudioSourceBuilder(DiscordApi api) {
        this.api = api;
    }

    /**
     * Gets the Discord api instance that's used by this builder.
     *
     * @return The Discord api instance.
     */
    public DiscordApi getApi() {
        return api;
    }

    /**
     * Sets the url of the youtube video.
     *
     * @param url The url.
     * @return The current instance in order to chain call methods.
     */
    public YouTubeAudioSourceBuilder setUrl(String url) {
        this.url = url;
        return this;
    }

    /**
     * Gets the url of the youtube video that this builder is using.
     *
     * @return The url.
     */
    public Optional<String> getUrl() {
        return Optional.ofNullable(url);
    }

    /**
     * Builds a new youtube audio source.
     *
     * @return The created youtube audio source.
     */
    public CompletableFuture<YouTubeAudioSource> build() {
        CompletableFuture<YouTubeAudioSource> future = new CompletableFuture<>();

        // Do all the Lavaplayer stuff
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
        AudioPlayer player = playerManager.createPlayer();

        // Make a copy of the url to make sure it does not change while the track gets loaded
        String url = this.url;

        // Load the youtube video
        playerManager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                player.playTrack(audioTrack);
                future.complete(new YouTubeAudioSource(api, player, playerManager.getFrameBufferDuration(), url));
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                future.completeExceptionally(new IllegalArgumentException("The provided url is a playlist"));
            }

            @Override
            public void noMatches() {
                future.completeExceptionally(new IllegalArgumentException("No video found for the given url"));
            }

            @Override
            public void loadFailed(FriendlyException e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }
}
