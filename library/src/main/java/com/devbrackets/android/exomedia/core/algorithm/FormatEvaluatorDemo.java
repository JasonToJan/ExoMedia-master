/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devbrackets.android.exomedia.core.algorithm;

import android.util.Log;

import com.google.android.exoplayer.chunk.MediaChunk;
import com.google.android.exoplayer.chunk.Format;
import com.google.android.exoplayer.chunk.FormatEvaluator;
import com.google.android.exoplayer.chunk.Chunk;
import com.google.android.exoplayer.upstream.BandwidthMeter;
import com.google.android.exoplayer.util.Clock;
import com.google.android.exoplayer.util.SystemClock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Selects from a number of available formats during playback.
 */


/**
 * An adaptive evaluator for video formats, which attempts to select the best quality possible
 * given the current network conditions and state of the buffer.
 * <p/>
 * This implementation should be used for video only, and should not be used for audio. It is a
 * reference implementation only. It is recommended that application developers implement their
 * own adaptive evaluator to more precisely suit their use case.
 */
public final class FormatEvaluatorDemo implements FormatEvaluator {

    public static final int DEFAULT_MAX_INITIAL_BITRATE = 800000;

    public static final int DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS = 10000;
    public static final int DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS = 25000;
    public static final int DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS = 25000;
    public static final float DEFAULT_BANDWIDTH_FRACTION = 0.75f;

    private final BandwidthMeter bandwidthMeter;

    private final int maxInitialBitrate;
    private final long minDurationForQualityIncreaseUs;
    private final long maxDurationForQualityDecreaseUs;
    private final long minDurationToRetainAfterDiscardUs;
    private final float bandwidthFraction;

    private File myFile;
    private File myFileFormat;
    private File myFileFormatFuzzy;
    private File myFileBandwidth;
    private Clock mClock;
    private long mstartTimeMs;

    private FuzzyAlgorithmv2 fuzzyAl;

    /**
     * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
     */
    public FormatEvaluatorDemo(BandwidthMeter bandwidthMeter) {
        this(bandwidthMeter, DEFAULT_MAX_INITIAL_BITRATE,
                DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS,
                DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
                DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS, DEFAULT_BANDWIDTH_FRACTION);
    }

    /**
     * @param bandwidthMeter                    Provides an estimate of the currently available bandwidth.
     * @param maxInitialBitrate                 The maximum bitrate in bits per second that should be assumed
     *                                          when bandwidthMeter cannot provide an estimate due to playback having only just started.
     * @param minDurationForQualityIncreaseMs   The minimum duration of buffered data required for
     *                                          the evaluator to consider switching to a higher quality format.
     * @param maxDurationForQualityDecreaseMs   The maximum duration of buffered data required for
     *                                          the evaluator to consider switching to a lower quality format.
     * @param minDurationToRetainAfterDiscardMs When switching to a significantly higher quality
     *                                          format, the evaluator may discard some of the media that it has already buffered at the
     *                                          lower quality, so as to switch up to the higher quality faster. This is the minimum
     *                                          duration of media that must be retained at the lower quality.
     * @param bandwidthFraction                 The fraction of the available bandwidth that the evaluator should
     *                                          consider available for use. Setting to a value less than 1 is recommended to account
     *                                          for inaccuracies in the bandwidth estimator.
     */
    public FormatEvaluatorDemo(BandwidthMeter bandwidthMeter,
                               int maxInitialBitrate,
                               int minDurationForQualityIncreaseMs,
                               int maxDurationForQualityDecreaseMs,
                               int minDurationToRetainAfterDiscardMs,
                               float bandwidthFraction) {
        this.bandwidthMeter = bandwidthMeter;
        this.maxInitialBitrate = maxInitialBitrate;
        this.minDurationForQualityIncreaseUs = minDurationForQualityIncreaseMs * 1000L;
        this.maxDurationForQualityDecreaseUs = maxDurationForQualityDecreaseMs * 1000L;
        this.minDurationToRetainAfterDiscardUs = minDurationToRetainAfterDiscardMs * 1000L;
        this.bandwidthFraction = bandwidthFraction;

        try {
            myFile = new File("/sdcard/sdResultBufferFile.txt");
            myFileFormat = new File("/sdcard/sdResultFormatFile.txt");
            myFileFormatFuzzy = new File("/sdcard/sdResultFormatFuzzyFile.txt");
            myFileBandwidth = new File("/sdcard/sdResultBandwidthFile.txt");
            myFile.createNewFile();
            myFileFormat.createNewFile();
            myFileFormatFuzzy.createNewFile();
            myFileBandwidth.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fuzzyAl = new FuzzyAlgorithmv2();
        fuzzyAl.initiateSeassion();

        mClock = new SystemClock();
        mstartTimeMs = mClock.elapsedRealtime();
    }

    @Override
    public void enable() {
        // Do nothing.
    }

    @Override
    public void disable() {
        // Do nothing.
    }

    @Override
    public void evaluate(List<? extends MediaChunk> queue, long playbackPositionUs,
                         Format[] formats, Evaluation evaluation) {
        long bufferedDurationUs = queue.isEmpty() ? 0
                : queue.get(queue.size() - 1).endTimeUs - playbackPositionUs;
        Format current = evaluation.format;
        long bitrateEstimate = bandwidthMeter.getBitrateEstimate();
        Format ideal = determineIdealFormat(formats, bitrateEstimate);
        boolean isHigher = ideal != null && current != null && ideal.bitrate > current.bitrate;
        boolean isLower = ideal != null && current != null && ideal.bitrate < current.bitrate;

        if (isHigher) {
            if (bufferedDurationUs < minDurationForQualityIncreaseUs) {
                // The ideal format is a higher quality, but we have insufficient buffer to
                // safely switch up. Defer switching up for now.
                ideal = current;
            } else if (bufferedDurationUs >= minDurationToRetainAfterDiscardUs) {
                // We're switching from an SD stream to a stream of higher resolution. Consider
                // discarding already buffered media chunks. Specifically, discard media chunks starting
                // from the first one that is of lower bandwidth, lower resolution and that is not HD.
                for (int i = 1; i < queue.size(); i++) {
                    MediaChunk thisChunk = queue.get(i);
                    long durationBeforeThisSegmentUs = thisChunk.startTimeUs - playbackPositionUs;
                    if (durationBeforeThisSegmentUs >= minDurationToRetainAfterDiscardUs
                            && thisChunk.format.bitrate < ideal.bitrate
                            && thisChunk.format.height < ideal.height
                            && thisChunk.format.height < 720
                            && thisChunk.format.width < 1280) {
                        // Discard chunks from this one onwards.
                        evaluation.queueSize = i;
                        break;
                    }
                }
            }
        } else if (isLower && current != null
                && bufferedDurationUs >= maxDurationForQualityDecreaseUs) {
            // The ideal format is a lower quality, but we have sufficient buffer to defer switching
            // down for now.
            ideal = current;
        }
        if (current != null && ideal != current) {
            evaluation.trigger = Chunk.TRIGGER_ADAPTIVE;
        }
        // evaluation.format = ideal;

        Format fuzzyIdeal = determineIdealFormatFuzzy(formats, bitrateEstimate, bufferedDurationUs, ideal);
        evaluation.format = fuzzyIdeal;

        try {
            FileOutputStream fOut = new FileOutputStream(myFile, true);
            FileOutputStream fFormatOut = new FileOutputStream(myFileFormat, true);
            FileOutputStream fFormatFuzzyOut = new FileOutputStream(myFileFormatFuzzy, true);
            FileOutputStream fBandwidthtOut = new FileOutputStream(myFileBandwidth, true);

            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            OutputStreamWriter myFormatOutWriter = new OutputStreamWriter(fFormatOut);
            OutputStreamWriter myFormatFuzzyOutWriter = new OutputStreamWriter(fFormatFuzzyOut);
            OutputStreamWriter myBandwidthOutWriter = new OutputStreamWriter(fBandwidthtOut);

            long nowMs = mClock.elapsedRealtime();
            myOutWriter.append(bufferedDurationUs + " " + (nowMs - mstartTimeMs) + "\n");
            myOutWriter.close();
            fOut.close();

            myFormatOutWriter.append(ideal.width + " " + ideal.height + " " + (nowMs - mstartTimeMs) + "\n");
            myFormatOutWriter.close();
            myFormatOutWriter.close();

            myFormatFuzzyOutWriter.append(fuzzyIdeal.width + " " + fuzzyIdeal.height + " " + (nowMs - mstartTimeMs) + "\n");
            myFormatFuzzyOutWriter.close();
            myFormatFuzzyOutWriter.close();

            myBandwidthOutWriter.append(bandwidthMeter.getBitrateEstimate() + " " + (nowMs - mstartTimeMs) + "\n");
            myBandwidthOutWriter.close();
            myBandwidthOutWriter.close();
        } catch (IOException e) {
            Log.e("Log IOException", "evaluate");
        }
    }

    /**
     * Compute the ideal format ignoring buffer health.
     */
    private Format determineIdealFormat(Format[] formats, long bitrateEstimate) {
        long effectiveBitrate = bitrateEstimate == BandwidthMeter.NO_ESTIMATE
                ? maxInitialBitrate : (long) (bitrateEstimate * bandwidthFraction);

        for (int i = 0; i < formats.length; i++) {
            Format format = formats[i];
            if (format.bitrate <= effectiveBitrate) {
                return format;
            }
        }
        // We didn't manage to calculate a suitable format. Return the lowest quality format.
        return formats[formats.length - 1];
    }

    private Format determineIdealFormatFuzzy(Format[] formats, long bitrateEstimate, long bufferedDurationUs, Format current) {
        long effectiveBitrate = bitrateEstimate == BandwidthMeter.NO_ESTIMATE
                ? maxInitialBitrate : (long) (bitrateEstimate * bandwidthFraction);

        int width = 10, height = 10;
        if (current != null) {
            width = current.width;
            height = current.height;
        }
        double[] getVal = fuzzyAl.fuzzyAlgorithm(bufferedDurationUs, bitrateEstimate, new int[]{width, height});
        Log.e("DKM", getVal[0] + ", " + getVal[1] + " current: " + width);
        for (int i = 0; i < formats.length; i++) {
            if (getVal[0] >= formats[i].width) {
                // Log.e("DKM-----------", formats[i].width + ", " + formats[i].height + " current: " + width);
                return formats[i];
            }
        }

        // We didn't manage to calculate a suitable format. Return the lowest quality format.
        return formats[formats.length - 1];
    }
}


