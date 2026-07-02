package io.github.arkosammy12.jemu.frontend.audio;

public enum AudioChannels {
    MONO(1),
    STEREO(2);

    private final int channelCount;

    AudioChannels(int channelCount) {
        this.channelCount = channelCount;
    }

    public int getChannelCount() {
        return this.channelCount;
    }

}
