package org.javacord.lavaplayerwrapper.youtube;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;
import org.javacord.api.audio.BufferableAudioSource;
import org.javacord.api.audio.DownloadableAudioSource;
import org.javacord.api.audio.PauseableAudioSource;
import org.javacord.api.audio.SeekableAudioSource;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * An audio source that can play YouTube videos.
 *
 * <p>Use the {@link YouTubeAudioSourceBuilder} to create instances of this class.
 */
public class YouTubeAudioSource extends AudioSourceBase implements SeekableAudioSource, DownloadableAudioSource,
        PauseableAudioSource, BufferableAudioSource {

    // Lavaplayer objects
    private final AudioPlayer player;
    private AudioFrame lastFrame;
    private volatile AudioTrack track;

    // Some general information about the YouTube video
    private final String url;
    private final String title;
    private final String creatorName;

    private volatile boolean paused = false;

    private volatile List<byte[]> allFrames = null;
    private int position = 0;
    private long bufferDurationInMillis;

    /**
     * Creates a new YouTube audio source.
     *
     * @param api A Discord api instance.
     * @param player The used audio player.
     * @param bufferDurationInMillis The initial buffer duration of the used audio player.
     * @param url The url of the YouTube video.
     */
    YouTubeAudioSource(DiscordApi api, AudioPlayer player, long bufferDurationInMillis, String url) {
        super(api);
        this.player = player;
        this.bufferDurationInMillis = bufferDurationInMillis;
        this.track = player.getPlayingTrack();

        this.url = url;
        this.title = track.getInfo().title;
        this.creatorName = track.getInfo().author;
    }

    /**
     * Creates a new YouTube audio source that uses some pre-downloaded audio frames.
     *
     * @param api The Discord api instance.
     * @param allFrames All audio frames of the YouTube video.
     * @param url The url of the YouTube video.
     * @param title The title of the YouTube video.
     * @param creatorName The name of the creator of the YouTube video.
     */
    YouTubeAudioSource(DiscordApi api, List<byte[]> allFrames, String url, String title, String creatorName) {
        super(api);
        this.player = null;
        this.bufferDurationInMillis = Long.MAX_VALUE;
        this.allFrames = allFrames;

        this.url = url;
        this.title = title;
        this.creatorName = creatorName;
    }

    /**
     * Creates a new audio source of this class.
     *
     * <p>This methods is meant as as less verbose variant than the {@link YouTubeAudioSourceBuilder} which provides
     * more configuration.
     *
     * @param api The Discord api instance.
     * @param url The url of the youtube video.
     * @return A new youtube audio source.
     */
    public static CompletableFuture<YouTubeAudioSource> of(DiscordApi api, String url) {
        return new YouTubeAudioSourceBuilder(api)
                .setUrl(url)
                .build();
    }

    /**
     * Gets the url of the YouTube video.
     *
     * @return The url.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the title of the YouTube video.
     *
     * @return The title.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the name of the creator of the YouTube video.
     *
     * @return The creator's name.
     */
    public String getCreatorName() {
        return creatorName;
    }

    @Override
    public long setPosition(long position, TimeUnit unit) {
        long frameNumber = (unit.toMillis(position) / 20);
        long positionInMillis = frameNumber * 20;
        if (allFrames != null) {
            this.position = (int) frameNumber;
            return positionInMillis;
        }
        track.setPosition(positionInMillis);
        return positionInMillis;
    }

    @Override
    public Duration getPosition() {
        if(allFrames != null) {
            return Duration.ofMillis(position * 20);
        }
        return Duration.ofMillis(track.getPosition());
    }

    @Override
    public Duration getDuration() {
        return Duration.ofMillis(track.getDuration());
    }

    @Override
    public void setBufferSize(long size, TimeUnit unit) {
        long duration = unit.toMillis(size);
        if (duration > Integer.MAX_VALUE) {
            duration = Integer.MAX_VALUE;
        }
        bufferDurationInMillis = duration;
        player.setFrameBufferDuration((int) duration);
    }

    @Override
    public Duration getBufferSize() {
        if (allFrames != null) {
            return ChronoUnit.FOREVER.getDuration();
        }
        return Duration.ofMillis(bufferDurationInMillis);
    }

    @Override
    public Duration getUsedBufferSize() {
        if (allFrames != null) {
            return Duration.ofMillis(allFrames.size() * 20);
        }
        // Lavaplayer does not provide a way to tell the used buffer size
        return Duration.ofMillis(-1);
    }

    @Override
    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    @Override
    public boolean isPaused() {
        return paused;
    }

    @Override
    public CompletableFuture<YouTubeAudioSource> download() {
        CompletableFuture<YouTubeAudioSource> future = new CompletableFuture<>();
        if (allFrames != null) {
            future.complete(this);
            return future;
        }

        getApi().getThreadPool().getExecutorService().submit(() -> {
            List<byte[]> frames = new ArrayList<>();
            AudioFrame frame;

            try {
                frame = player.provide(1, TimeUnit.MINUTES);
                while (frame != null) {
                    frames.add(frame.getData());
                    frame = player.provide(1, TimeUnit.MINUTES);
                }
            } catch (Throwable t) {
                future.completeExceptionally(t);
                return;
            }

            this.allFrames = frames;
            // Clean up the players, as we no longer need them
            this.player.destroy();

            future.complete(this);
        });

        return future;
    }

    @Override
    public boolean isFullyDownloaded() {
        return allFrames != null;
    }

    @Override
    public byte[] getNextFrame() {
        if(paused) {
            return null;
        }
        if (allFrames != null) {
            return applyTransformers(allFrames.get(position++));
        }
        if (lastFrame == null) {
            return null;
        }
        return applyTransformers(lastFrame.getData());
    }

    @Override
    public boolean hasFinished() {
        if (allFrames != null) {
            return position >= allFrames.size();
        }
        return track != null && track.getState() == AudioTrackState.FINISHED;
    }

    @Override
    public boolean hasNextFrame() {
        if (paused) {
            return false;
        }
        if (allFrames != null) {
            return position < allFrames.size();
        }
        lastFrame = player.provide();
        return lastFrame != null;
    }

    @Override
    public AudioSource copy() {
        if (allFrames != null) {
            return new YouTubeAudioSource(getApi(), allFrames, url, title, creatorName);
        }
        AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        AudioPlayer player = playerManager.createPlayer();
        player.playTrack(track.makeClone());
        return new YouTubeAudioSource(getApi(), player, playerManager.getFrameBufferDuration(), url);
    }
}
