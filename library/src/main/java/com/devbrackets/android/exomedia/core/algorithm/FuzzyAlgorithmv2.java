package com.devbrackets.android.exomedia.core.algorithm;

import android.util.Log;

import com.fuzzylite.Engine;
import com.fuzzylite.imex.FisImporter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FuzzyAlgorithmv2 {

    public int TRACEBACK_STEP = 20;

    // algorithm implementation
    private double buff, bandwidth;
    private double[] bandArr;
    private double[] arrBandAverage;
    private double[] buffArr;
    private double[] arrBuffAverage;

    private double[] widthArr;
    private double[] widthArrAverage;
    private double[] heightArr;
    private double[] heightArrAverage;

    private int step;
    private double timePredict;
    private double bandPredict;
    private double buffAverage;
    private double average;

    private Engine engineMatlab;
    private FisImporter fisImportMatlab;
    private BufferedReader fisReader;

    public FuzzyAlgorithmv2() {
        bandArr = new double[TRACEBACK_STEP];
        arrBandAverage = new double[TRACEBACK_STEP];
        buffArr = new double[TRACEBACK_STEP];
        arrBuffAverage = new double[TRACEBACK_STEP];
        widthArr = new double[TRACEBACK_STEP];
        widthArrAverage = new double[TRACEBACK_STEP];
        heightArr = new double[TRACEBACK_STEP];
        heightArrAverage = new double[TRACEBACK_STEP];

        step = 0;

        engineMatlab = new Engine();
        fisImportMatlab = new FisImporter();
    }

    public void initiateSeassion() {
        String line = "";
        String matlabString = getMatlabFuzzy();

        engineMatlab = fisImportMatlab.fromString(matlabString);
        //Log.e("DKM matlabString ", matlabString);

        buff = 6.6135;
        bandwidth = 847.44;
        double width = 270, height = 480;
        movingAverage(buff, buffArr, arrBuffAverage, step);
        movingAverage(bandwidth, bandArr, arrBandAverage, step);

        movingAverage(width, widthArr, widthArrAverage, step);
        movingAverage(height, heightArr, heightArrAverage, step);
        step = 1;
    }

    public double[] fuzzyAlgorithm(double buff, double bandwidth, int format[]) {
        // moving buffer
        movingAverage(buff, buffArr, arrBuffAverage, step);
        // moving bandwidth
        movingAverage(bandwidth, bandArr, arrBandAverage, step);
        // moving format
        double width = 270, height = 480;
        width = movingAverage(format[0], widthArr, widthArrAverage, step);
        height = movingAverage(format[1], heightArr, heightArrAverage, step);
        step += 1;
        double fuzzyBuff = fuzzficationInput(buffArr, arrBuffAverage, step);
        double fuzzyBand = fuzzficationInput(bandArr, arrBandAverage, step);

        engineMatlab.getInputVariable(0).setInputValue(fuzzyBuff);
        engineMatlab.getInputVariable(1).setInputValue(fuzzyBand);
        engineMatlab.process();
        double fuzzyOut = engineMatlab.getOutputVariable(0).getOutputValue();

        Log.e("DKM Fuzzy", fuzzyOut + ", ");

        double[] reVale = new double[2];
        reVale[0] = width + fuzzyOut * sumArray(widthArr, widthArrAverage);
        reVale[1] = height + fuzzyOut * sumArray(heightArr, heightArrAverage);

        return reVale;
    }

    private double movingAverage(double value, double[] valueArr,
                                 double[] averageArr, int step) {
        int array_size = TRACEBACK_STEP;
        int i;
        double temp = 0;
        if (step == 0) {
            valueArr[0] = value;
            averageArr[0] = value;
        } else {
            //
            if (step < array_size) {
                valueArr[step] = value;
                temp = averageArr[step - 1] + (value - averageArr[step - 1])
                        / (step + 1);
                averageArr[step] = temp;
            } else {
                temp = averageArr[array_size - 1]
                        + (value - averageArr[array_size - 1]) / (array_size);
                for (i = 0; i < array_size - 1; i++) {
                    valueArr[i] = valueArr[i + 1];
                    averageArr[i] = averageArr[i + 1];
                }
                valueArr[i] = value;
                averageArr[i] = temp;
            }
        }

        return temp;
    }

    /*
     * length of these arrays below must equal, and it must not exceed
     * TRACEBACK_STEP
     */
    private double fuzzficationInput(double[] value, double[] average, int step) {
        // fuzzification variables
        int array_size = TRACEBACK_STEP;
        double temp;
        int i;
        double fraction = 1;
        double maxDiff = Double.MIN_VALUE;
        double minDiff = Double.MAX_VALUE;
        double sumArr = 0;
        double endElement = 0;
        if (step < array_size) {
            for (i = 0; i < step; i++) {
                temp = Math.abs(value[i] - average[i]);
                sumArr += temp;
                if (Math.abs(temp) > maxDiff) {
                    maxDiff = Math.abs(temp);
                }
            }
        } else {
            for (i = 0; i < array_size; i++) {
                temp = Math.abs(value[i] - average[i]);
                sumArr += temp;
                if (Math.abs(temp) > maxDiff) {
                    maxDiff = Math.abs(temp);
                }
            }
        }

        double outputFuzzy = (1.0 * sumArr / maxDiff);

        return outputFuzzy;
    }

    private double sumArray(double arr[], double average[]) {
        int size = arr.length;
        double sum = 0;
        for (int i = 0; i < size; i++) {
            sum += Math.abs(arr[i] - average[i]);
        }
        return sum / TRACEBACK_STEP;
    }

    private String getMatlabFuzzy() {
        return "[System]\n" +
                "Name='membership_function_pnv2'\n" +
                "Type='mamdani'\n" +
                "Version=2.0\n" +
                "NumInputs=2\n" +
                "NumOutputs=1\n" +
                "NumRules=49\n" +
                "AndMethod='min'\n" +
                "OrMethod='max'\n" +
                "ImpMethod='min'\n" +
                "AggMethod='max'\n" +
                "DefuzzMethod='centroid'\n" +
                "\n" +
                "[Input1]\n" +
                "Name='BANDWIDTH'\n" +
                "Range=[-20 20]\n" +
                "NumMFs=7\n" +
                "MF1='NHD':'trimf',[-20 -20 -12]\n" +
                "MF2='NMD':'trimf',[-18 -12 -6]\n" +
                "MF3='NLD':'trimf',[-12 -6 0]\n" +
                "MF4='D':'trimf',[-6 0 6]\n" +
                "MF5='PLD':'trimf',[0 6 12]\n" +
                "MF6='PMD':'trimf',[6 12 18]\n" +
                "MF7='PHD':'trimf',[12 20 20]\n" +
                "\n" +
                "[Input2]\n" +
                "Name='TIME'\n" +
                "Range=[-20 20]\n" +
                "NumMFs=7\n" +
                "MF1='NHD':'trimf',[-20 -20 -12]\n" +
                "MF2='NMD':'trimf',[-18 -12 -6]\n" +
                "MF3='NLD':'trimf',[-12 -6 0]\n" +
                "MF4='D':'trimf',[-6 0 6]\n" +
                "MF5='PLD':'trimf',[0 6 12]\n" +
                "MF6='PMD':'trimf',[6 12 18]\n" +
                "MF7='PHD':'trimf',[12 20 20]\n" +
                "\n" +
                "[Output1]\n" +
                "Name='RESPONSE'\n" +
                "Range=[-20 20]\n" +
                "NumMFs=7\n" +
                "MF1='PLD':'trimf',[0 0 6]\n" +
                "MF2='PMD':'trimf',[4.21 8.21 12.21]\n" +
                "MF3='PHD':'trimf',[10 20 20]\n" +
                "MF4='NHD':'trimf',[-20 -16 -10]\n" +
                "MF5='NMD':'trimf',[-14 -12 -7]\n" +
                "MF6='NLD':'trimf',[-11 -8 -5]\n" +
                "MF7='D':'trimf',[-8 -2 4]\n" +
                "\n" +
                "[Rules]\n" +
                "1 1, 4 (1) : 1\n" +
                "1 2, 4 (1) : 1\n" +
                "1 3, 5 (1) : 1\n" +
                "1 4, 5 (1) : 1\n" +
                "1 5, 5 (1) : 1\n" +
                "1 6, 6 (1) : 1\n" +
                "1 7, 7 (1) : 1\n" +
                "2 1, 4 (1) : 1\n" +
                "2 2, 5 (1) : 1\n" +
                "2 3, 5 (1) : 1\n" +
                "2 4, 5 (1) : 1\n" +
                "2 5, 6 (1) : 1\n" +
                "2 6, 7 (1) : 1\n" +
                "2 7, 7 (1) : 1\n" +
                "3 1, 5 (1) : 1\n" +
                "3 2, 6 (1) : 1\n" +
                "3 3, 6 (1) : 1\n" +
                "3 4, 6 (1) : 1\n" +
                "3 5, 7 (1) : 1\n" +
                "3 6, 7 (1) : 1\n" +
                "3 7, 7 (1) : 1\n" +
                "4 1, 6 (1) : 1\n" +
                "4 2, 6 (1) : 1\n" +
                "4 3, 7 (1) : 1\n" +
                "4 4, 7 (1) : 1\n" +
                "4 5, 1 (1) : 1\n" +
                "4 6, 1 (1) : 1\n" +
                "4 7, 1 (1) : 1\n" +
                "5 1, 6 (1) : 1\n" +
                "5 2, 7 (1) : 1\n" +
                "5 3, 7 (1) : 1\n" +
                "5 4, 7 (1) : 1\n" +
                "5 5, 1 (1) : 1\n" +
                "5 6, 1 (1) : 1\n" +
                "5 7, 2 (1) : 1\n" +
                "6 1, 7 (1) : 1\n" +
                "6 2, 7 (1) : 1\n" +
                "6 3, 7 (1) : 1\n" +
                "6 4, 1 (1) : 1\n" +
                "6 5, 2 (1) : 1\n" +
                "6 6, 2 (1) : 1\n" +
                "6 7, 2 (1) : 1\n" +
                "7 1, 7 (1) : 1\n" +
                "7 2, 7 (1) : 1\n" +
                "7 3, 1 (1) : 1\n" +
                "7 4, 1 (1) : 1\n" +
                "7 5, 2 (1) : 1\n" +
                "7 6, 2 (1) : 1\n" +
                "7 7, 3 (1) : 1\n";
    }
}
