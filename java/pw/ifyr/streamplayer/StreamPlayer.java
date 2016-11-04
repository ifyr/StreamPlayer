package pw.ifyr.streamplayer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import java.io.FileDescriptor;
import java.nio.ByteBuffer;

public class StreamPlayer implements Runnable {
    //public final String LOG_TAG = "StreamPlayer";

    protected int mediaSpeed;
    protected FileDescriptor mediaFD;
    protected long mediaOffset;
    protected long mediaLength;

    public static void play(int speed, FileDescriptor fd, long offset, long length) {
        new Thread(new StreamPlayer(speed, fd, offset, length)).run();
    }

    protected StreamPlayer(int speed, FileDescriptor fd, long offset, long length) {
        this.mediaSpeed = speed;
        this.mediaFD = fd;
        this.mediaOffset = offset;
        this.mediaLength = length;
    }

    private int speedRate(int rate, int speed) {
        final int[] regularRate = {50, 67, 83, 91, 100, 110, 120, 150, 200};
        int s = rate * regularRate[speed] / 100;

        int x1 = s / 4000, t1 = s % 4000;
        if (t1 > 2000) {
            x1 += 1;
            t1 = 4000 - t1;
        }

        int x2 = s / 4410, t2 = s % 4410;
        if (t2 > 2205) {
            x2 += 1;
            t2 = 4410 - t2;
        }

        return (t1 < t2) ? x1 * 4000 : x2 * 4410;
    }

    @Override
    public void run() {
        MediaCodec codec = null;
        AudioTrack track = null;
        try {
            ByteBuffer[] codecInputBuffers;
            ByteBuffer[] codecOutputBuffers;

            // extractor gets information about the stream
            MediaExtractor extractor = new MediaExtractor();
            try {
                extractor.setDataSource(mediaFD, mediaOffset, mediaLength);
            } catch (Exception e) {
                return;
            }

            MediaFormat format = extractor.getTrackFormat(0);
            String mime = format.getString(MediaFormat.KEY_MIME);

            // the actual decoder
            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
            codec.start();
            codecInputBuffers = codec.getInputBuffers();
            codecOutputBuffers = codec.getOutputBuffers();

            // get the sample rate to configure AudioTrack
            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

            // create our AudioTrack instance
            track = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    AudioTrack.getMinBufferSize(
                            sampleRate,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT
                    ),
                    AudioTrack.MODE_STREAM
            );

            // start playing, we will feed you later
            if (mediaSpeed != 4) {
                track.setPlaybackRate(speedRate(sampleRate, mediaSpeed));
            }
            track.play();
            extractor.selectTrack(0);

            // start decoding
            final long kTimeOutUs = 10000;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            int noOutputCounter = 0;
            int noOutputCounterLimit = 50;

            //int bufIndexCheck = 0;
            while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit) {
                noOutputCounter++;
                if (!sawInputEOS) {

                    int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                    //bufIndexCheck++;
                    //Log.d(LOG_TAG, " bufIndexCheck " + bufIndexCheck);
                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                        int sampleSize =
                                extractor.readSampleData(dstBuf, 0 /* offset */);

                        long presentationTimeUs = 0;

                        if (sampleSize < 0) {
                            //Log.d(LOG_TAG, "saw input EOS.");
                            sawInputEOS = true;
                            sampleSize = 0;
                        } else {
                            presentationTimeUs = extractor.getSampleTime();
                        }

                        codec.queueInputBuffer(
                                inputBufIndex,
                                0 /* offset */,
                                sampleSize,
                                presentationTimeUs,
                                sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                        if (!sawInputEOS) {
                            extractor.advance();
                        }
                    } else {
                        //Log.e(LOG_TAG, "inputBufIndex " +inputBufIndex);
                    }
                }

                int res = codec.dequeueOutputBuffer(info, kTimeOutUs);

                if (res >= 0) {
                    //Log.d(LOG_TAG, "got frame, size " + info.size + "/" + info.presentationTimeUs);
                    if (info.size > 0) {
                        noOutputCounter = 0;
                    }

                    int outputBufIndex = res;
                    ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                    final byte[] chunk = new byte[info.size];
                    buf.get(chunk);
                    buf.clear();
                    if (chunk.length > 0) {
                        track.write(chunk, 0, chunk.length);
                    }
                    codec.releaseOutputBuffer(outputBufIndex, false /* render */);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        //Log.d(LOG_TAG, "saw output EOS.");
                        sawOutputEOS = true;
                    }
                } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    //codecOutputBuffers = codec.getOutputBuffers();
                    //Log.d(LOG_TAG, "output buffers have changed.");
                } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    //MediaFormat oformat = codec.getOutputFormat();
                    //Log.d(LOG_TAG, "output format has changed to " + oformat);
                } else {
                    //Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
                }
            }

            //Log.d(LOG_TAG, "stopping...");

            // attempt reconnect
            if (sawOutputEOS) {
                track.play();
                return;
            }
        } catch (Exception e) {
        } finally {
            if (codec != null) {
                codec.stop();
                codec.release();
            }
            if (track != null) {
                track.stop();
                track.flush();
                track.release();
            }
        }
    }
}