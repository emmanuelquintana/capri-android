package io.github.emmanuelquintana.voicefx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** One context per stream. Use and close on the same playback worker. */
public final class CapriCyberVoiceFX implements AutoCloseable {
    public enum Mode { OFF, RING, VOCODER, RESONANCE, HYBRID }

    static { System.loadLibrary("CapriCyberVoiceFX"); }

    private long handle;
    private final int channels;

    public CapriCyberVoiceFX(int sampleRate, int channels, Mode mode, float intensity, float carrierHz) {
        this.channels = channels;
        handle = create(sampleRate, channels, mode.ordinal(), intensity, carrierHz);
        if (handle == 0) throw new IllegalArgumentException("Invalid PCM format or effect parameters");
    }

    /** Process complete float32 PCM frames starting at the current position. Does not advance it. */
    public void process(ByteBuffer pcm, int frames) {
        if (handle == 0) throw new IllegalStateException("Effect closed");
        if (frames < 0 || !pcm.isDirect() || pcm.isReadOnly() ||
            pcm.order() != ByteOrder.nativeOrder() || (long) frames * channels * Float.BYTES > pcm.remaining()) {
            throw new IllegalArgumentException("Required writable native-order direct float32 PCM");
        }
        if (frames > 0 && apply(handle, pcm.slice(), frames, channels) != 0) {
            throw new IllegalArgumentException("PCM buffer is misaligned or invalid");
        }
    }

    public void reset() {
        if (handle == 0) throw new IllegalStateException("Effect closed");
        resetNative(handle);
    }

    @Override public void close() {
        if (handle != 0) release(handle);
        handle = 0;
    }

    private static native long create(int sampleRate, int channels, int mode, float intensity, float carrierHz);
    private static native int apply(long handle, ByteBuffer pcm, int frames, int channels);
    private static native void resetNative(long handle);
    private static native void release(long handle);
}
