package org.javacord.lavaplayerwrapper;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioSource;
import org.javacord.api.audio.AudioSourceBase;

/**
 * An audio source that uses Lavaplayer's `AudioPlayer` to provide audio.
 *
 * <p>While this is the most flexible audio source, you  exclusively work with Lavaplayer's api which technically is
 * exactly the opposite of what this library tries to archive.
 *
 * <p>You should only use it as a fallback for Lavaplayer features that are not yet covered by this library. Otherwise
 * you can just directly use the Lavaplayer library and copy the source code of this class into your project.
 */
public class LavaplayerAudioSource extends AudioSourceBase {

    private final AudioPlayer audioPlayer;
    private AudioFrame lastFrame;

    /**
     * Creates a new lavaplayer audio source.
     *
     * @param api A discord api instance.
     * @param audioPlayer An audio player from Lavaplayer.
     */
    public LavaplayerAudioSource(DiscordApi api, AudioPlayer audioPlayer) {
        super(api);
        this.audioPlayer = audioPlayer;
    }

    @Override
    public byte[] getNextFrame() {
        if (lastFrame == null) {
            return null;
        }
        return applySynthesizers(lastFrame.getData());
    }

    @Override
    public boolean hasFinished() {
        return false;
    }

    @Override
    public boolean hasNextFrame() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public AudioSource clone() {
        return new LavaplayerAudioSource(getApi(), audioPlayer);
    }
}
