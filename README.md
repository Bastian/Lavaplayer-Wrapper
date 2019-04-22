# LavaplayerWrapper

A simple wrapper around the [Lavaplayer](https://github.com/sedmelluq/lavaplayer) library that implements various
[Javacord](https://github.com/Javacord/Javacord) audio sources.

The goal of the project is to provide Javacord audio sources by wrapping the Lavaplayer library. This is only a
temporary solution and we are going to create a lightweight audio sources repository that is independent of Lavaplayer 
once Javacord's audio api get's released and is fully usable. At the moment, the focus is on making Javacord's audio
stable and ready for release.

## Disclaimer

This repository is using [Bastian's Javacord fork](https://github.com/Bastian/Javacord/tree/audio-support) which adds 
audio support to Javacord. It is still in development and can introduce breaking changes at any time. Once it will be
merged into the official Javacord repository, this repository will be updated to use the official audio api.

## Download

This repository is currently only available through Jitpack. Future version will be published to Maven Central.

```groovy
repositories {
    jcenter()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation group: 'com.github.Bastian', name: 'Lavaplayer-Wrapper', version: 'master-SNAPSHOT'
    implementation group: 'com.github.Bastian.Javacord', name: 'javacord', version: 'ae51320'
}
```

For best compatibility, do not include Javacord or Lavaplayer but use the one that ships with this dependency.

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
        player.play(track);
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        for (AudioTrack track : playlist.getTracks()) {
            player.play(track);
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

An audio source that supports playing YouTube videos.

```java
DiscordApi api = ...;
AudioConnection connection = ...;

// Simple version
AudioSource source = YouTubeAudioSource.of(api, "https://youtu.be/NvS351QKFV4").join();
connection.queue(source);

// Advanced
AudioSource source = new YouTubeAudioSourceBuilder(api)
    .setUrl("https://youtu.be/NvS351QKFV4")
    .build()
    .thenCompose(YouTubeAudioSource::download) // Optional: Download the full song before queueing it.
    .thenAccept(connection::queue)
    .exceptionally(throwable -> {
        // Loading or downloading the youtube video failed
        return null;
    });
```
