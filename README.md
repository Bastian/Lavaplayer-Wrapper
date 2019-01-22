# LavaplayerWrapper

A simple wrapper around the [Lavaplayer](https://github.com/sedmelluq/lavaplayer) library that implements various
[Javacord](https://github.com/Javacord/Javacord) audio sources.

## Supported audio sources

### LavaplayerAudioSource

An audio source that uses Lavaplayer's `AudioPlayer` to provide audio. While this is the most flexible audio source, you
exclusively work with Lavaplayer's api which technically is exactly the opposite of what this library tries to archive.
You should only use it as a fallback for Lavaplayer features that are not yet covered by this library. Otherwise
you can just directly use the Lavaplayer library.

```java
DiscordApi api = ...;
AudioConnection connection = ...;
// Create a player manager
AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
AudioPlayer player = playerManager.createPlayer();

// Create an audio source and add it to the audio connection's queue
AudioSource source = new LavaplayerAudioSource(api, player);
connection.queue(source);

// You can now use the AudioPlayer like you would normaly do with Lavaplayer, e.g.
playerManager.loadItem(identifier, new AudioLoadResultHandler() {
    @Override
    public void trackLoaded(AudioTrack track) {
        trackScheduler.queue(track);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        for (AudioTrack track : playlist.getTracks()) {
            trackScheduler.queue(track);
        }
    }

    @Override
    public void noMatches() {
        // Notify the user that we've got nothing
    }

    @Override
    public void loadFailed(FriendlyException throwable) {
        // Notify the user that everything exploded
    }
}
```

Take a look at Lavaplayer's documentation for more details.

### YouTubeAudioSource

... coming soon
