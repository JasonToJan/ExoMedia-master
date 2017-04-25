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

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.exoplayer.util.Assertions;
import com.google.android.exoplayer.util.Clock;
import com.google.android.exoplayer.util.SlidingPercentile;
import com.google.android.exoplayer.util.SystemClock;
import com.google.android.exoplayer.upstream.BandwidthMeter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Counts transferred bytes while transfers are open and creates a bandwidth sample and updated
 * bandwidth estimate each time a transfer ends.
 */
public final class DefaultBandwidthMeterDemo implements BandwidthMeter {

    public static final int DEFAULT_MAX_WEIGHT = 2000;

    private final Handler eventHandler;
    private final EventListener eventListener;
    private final Clock clock;
    private final SlidingPercentile slidingPercentile;

    private long bytesAccumulator;
    private long startTimeMs;
    private long bitrateEstimate;
    private int streamCount;

    private FuzzyAlgorithm fuzzyCalculation;
    private File myFile;
    private long mstartTimeMs;

    public DefaultBandwidthMeterDemo() {
        this(null, null);
    }

    public DefaultBandwidthMeterDemo(Handler eventHandler, EventListener eventListener) {
        this(eventHandler, eventListener, new SystemClock());
        /*this.fuzzyCalculation = new FuzzyAlgorithm();
        try {
            this.fuzzyCalculation.initiateSeassion();
            myFile = new File("/sdcard/sdResultFile.txt");
            myFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        mstartTimeMs = clock.elapsedRealtime();
    }

    public DefaultBandwidthMeterDemo(Handler eventHandler, EventListener eventListener, Clock clock) {
        this(eventHandler, eventListener, clock, DEFAULT_MAX_WEIGHT);
    }

    public DefaultBandwidthMeterDemo(Handler eventHandler, EventListener eventListener, int maxWeight) {
        this(eventHandler, eventListener, new SystemClock(), maxWeight);
    }

    public DefaultBandwidthMeterDemo(Handler eventHandler, EventListener eventListener, Clock clock,
                                     int maxWeight) {
        this.eventHandler = eventHandler;
        this.eventListener = eventListener;
        this.clock = clock;
        this.slidingPercentile = new SlidingPercentile(maxWeight);
        bitrateEstimate = NO_ESTIMATE;
    }

    @Override
    public synchronized long getBitrateEstimate() {
        return bitrateEstimate;
    }

    @Override
    public synchronized void onTransferStart() {
        if (streamCount == 0) {
            startTimeMs = clock.elapsedRealtime();
        }
        streamCount++;
    }

    @Override
    public synchronized void onBytesTransferred(int bytes) {
        bytesAccumulator += bytes;
    }

    @Override
    public synchronized void onTransferEnd() {
        Assertions.checkState(streamCount > 0);
        long nowMs = clock.elapsedRealtime();
        int elapsedMs = (int) (nowMs - startTimeMs);

        if (elapsedMs > 0) {
            float bitsPerSecond = (bytesAccumulator * 8000) / elapsedMs;
            slidingPercentile.addSample((int) Math.sqrt(bytesAccumulator), bitsPerSecond);
            float bandwidthEstimateFloat = slidingPercentile.getPercentile(0.5f);
            bitrateEstimate = Float.isNaN(bandwidthEstimateFloat) ? NO_ESTIMATE
                    : (long) bandwidthEstimateFloat;
            /*double[] predict = fuzzyCalculation.fuzzyAlgorithm(elapsedMs, bitrateEstimate);

            try {
                FileOutputStream fOut = new FileOutputStream(myFile, true);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                myOutWriter.append(bitrateEstimate + " " + ((long) predict[0]) + " " + (nowMs - mstartTimeMs) + "\n");
                myOutWriter.close();
                fOut.close();
            } catch (IOException e) {
                Log.e("Log IOException", (bitrateEstimate + ", " + ((long) predict[0])) + ", " + elapsedMs);
            }
            // bitrateEstimate = ((long) predict[0]);
            //bitrateEstimate = (long) predict[0];
            */

            notifyBandwidthSample(elapsedMs, bytesAccumulator, bitrateEstimate);
        }
        streamCount--;
        if (streamCount > 0) {
            startTimeMs = nowMs;
        }
        bytesAccumulator = 0;
    }

    private void notifyBandwidthSample(final int elapsedMs, final long bytes, final long bitrate) {
        if (eventHandler != null && eventListener != null) {
            eventHandler.post(new Runnable() {
                @Override
                public void run() {
                    eventListener.onBandwidthSample(elapsedMs, bytes, bitrate);
                }
            });
        }
    }
}
