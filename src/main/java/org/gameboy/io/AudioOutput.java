package org.gameboy.io;

import org.lwjgl.openal.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentLinkedQueue;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;

public class AudioOutput {
    private static final int SAMPLE_RATE = 44_100;
    private static final int BUFFER_COUNT = 4;
    private static final int BUFFER_SIZE = 1024;

    private final ConcurrentLinkedQueue<short[]> sampleQueue;
    private long device;
    private long context;
    private int source;
    private final int[] buffers = new int[BUFFER_COUNT];
    private boolean running;

    public AudioOutput(ConcurrentLinkedQueue<short[]> sampleQueue) {
        this.sampleQueue = sampleQueue;
    }

    public void start() {
        device = alcOpenDevice((ByteBuffer) null);
        if (device == 0) {
            System.err.println("Failed to open OpenAL device");
            return;
        }
        ALCCapabilities alcCaps = ALC.createCapabilities(device);
        context = alcCreateContext(device, (int[]) null);
        alcMakeContextCurrent(context);
        AL.createCapabilities(alcCaps);
        source = alGenSources();
        alGenBuffers(buffers);
        short[] silence = new short[BUFFER_SIZE * 2];
        for (int buffer : buffers) fillBuffer(buffer, silence);
        alSourceQueueBuffers(source, buffers);
        alSourcePlay(source);
        running = true;
    }

    public void update() {
        if (!running) return;
        int processed = alGetSourcei(source, AL_BUFFERS_PROCESSED);
        while (processed > 0) {
            int buffer = alSourceUnqueueBuffers(source);
            short[] samples = sampleQueue.poll();
            if (samples != null) {
                fillBuffer(buffer, samples);
            } else {
                fillBuffer(buffer, new short[BUFFER_SIZE * 2]);
            }
            alSourceQueueBuffers(source, buffer);
            processed--;
        }
        if (alGetSourcei(source, AL_SOURCE_STATE) != AL_PLAYING) {
            alSourcePlay(source);
        }
    }

    private void fillBuffer(int buffer, short[] samples) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(samples.length * 2).order(ByteOrder.nativeOrder());
        for (short sample : samples) byteBuffer.putShort(sample);
        byteBuffer.flip();
        alBufferData(buffer, AL_FORMAT_STEREO16, byteBuffer, SAMPLE_RATE);
    }

    public void stop() {
        if (!running) return;
        running = false;
        alSourceStop(source);
        alDeleteSources(source);
        alDeleteBuffers(buffers);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }
}
