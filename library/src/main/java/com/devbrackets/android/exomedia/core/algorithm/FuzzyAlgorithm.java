package com.devbrackets.android.exomedia.core.algorithm;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.fuzzylite.Engine;
import com.fuzzylite.imex.FisImporter;

public class FuzzyAlgorithm {

    public int TRACEBACK_STEP = 10;
    // algorithm implementation
    private double timeValue, bandwidthValue;
    private double[] bandArr;
    private double[] bandArrAverage;
    private double[] timeArr;
    private double[] timeArrAverage;
    private int step;
    private double timePredict;
    private double bandPredict;
    private double timeAverage;
    private double bandAverage;

    private Engine engineMatlab;
    private FisImporter fisImportMatlab;
    private BufferedReader fisReader;

    public FuzzyAlgorithm() {
        bandArr = new double[TRACEBACK_STEP];
        bandArrAverage = new double[TRACEBACK_STEP];
        timeArr = new double[TRACEBACK_STEP];
        timeArrAverage = new double[TRACEBACK_STEP];
        step = 0;

        engineMatlab = new Engine();
        fisImportMatlab = new FisImporter();
    }

    public void initiateSeassion() throws IOException {
        String line = "";
        String matlabString = getMatlabFuzzy();

        /*fisReader = new BufferedReader(new FileReader(fileLocation));
        while ((line = fisReader.readLine()) != null) {
            matlabString += (line + "\n");
        }*/

        engineMatlab = fisImportMatlab.fromString(matlabString);

        timeValue = 6.6135;
        bandwidthValue = 847.44;
        timeAverage = movingAverage(timeValue, timeArr, timeArrAverage, step);
        bandAverage = movingAverage(bandwidthValue, bandArr, bandArrAverage,
                step);
        step = 1;
    }

    public double[] fuzzyAlgorithm(double timeValue, double bandwidthValue) {
        timeAverage = movingAverage(timeValue, timeArr, timeArrAverage, step);
        bandAverage = movingAverage(bandwidthValue, bandArr, bandArrAverage,
                step);
        step += 1;
        double[] fuzzyTime = fuzzficationInput(timeArr, timeArrAverage, step);
        double[] fuzzyBandw = fuzzficationInput(bandArr, bandArrAverage, step);

        engineMatlab.getInputVariable(0).setInputValue(fuzzyBandw[0]);
        engineMatlab.getInputVariable(1).setInputValue(fuzzyTime[0]);
        engineMatlab.process();
        double fuzzyOut = engineMatlab.getOutputVariable(0).getOutputValue();

        bandPredict = bandAverage + fuzzyBandw[1] * fuzzyOut;
        timePredict = timeAverage + fuzzyTime[1] * fuzzyOut;

        double[] reVale = new double[2];
        reVale[0] = bandPredict;
        reVale[1] = timePredict;

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
    private double[] fuzzficationInput(double[] value, double[] average,
                                       int step) {
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
                if (temp > maxDiff) {
                    maxDiff = temp;
                }
                if (temp < minDiff && temp != 0) {
                    minDiff = temp;
                }
            }
            endElement = value[i - 1] - average[i - 1];
            fraction = 1.0 / step;
        } else {
            for (i = 0; i < array_size; i++) {
                temp = Math.abs(value[i] - average[i]);
                sumArr += temp;
                if (temp > maxDiff) {
                    maxDiff = temp;
                }
                if (temp < minDiff) {
                    minDiff = temp;
                }
            }
            endElement = value[i - 1] - average[i - 1];
            fraction = 1.0 / array_size;
        }

        double outputFuzzy = fraction * (1 / endElement) * sumArr
                * (minDiff / maxDiff);
        double outputFuzzyPredt = fraction * sumArr * (minDiff / maxDiff);
        double[] returnArr = new double[2];
        returnArr[0] = outputFuzzy;
        returnArr[1] = outputFuzzyPredt;

        return returnArr;
    }

    private String getMatlabFuzzy() {
        return "[System]\n" +
                "Name='membership_function_pn'\n" +
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
                "Range=[-1 1]\n" +
                "NumMFs=7\n" +
                "MF1='NHD':'trimf',[-1 -1 -0.6]\n" +
                "MF2='NMD':'trimf',[-0.9 -0.6 -0.3]\n" +
                "MF3='NLD':'trimf',[-0.6 -0.3 0]\n" +
                "MF4='D':'trimf',[-0.3 0 0.3]\n" +
                "MF5='PLD':'trimf',[0 0.3 0.6]\n" +
                "MF6='PMD':'trimf',[0.3 0.6 0.9]\n" +
                "MF7='PHD':'trimf',[0.6 1 1]\n" +
                "\n" +
                "[Input2]\n" +
                "Name='TIME'\n" +
                "Range=[-1 1]\n" +
                "NumMFs=7\n" +
                "MF1='NHD':'trimf',[-1 -1 -0.6]\n" +
                "MF2='NMD':'trimf',[-0.9 -0.6 -0.3]\n" +
                "MF3='NLD':'trimf',[-0.6 -0.3 0]\n" +
                "MF4='D':'trimf',[-0.3 0 0.3]\n" +
                "MF5='PLD':'trimf',[0 0.3 0.6]\n" +
                "MF6='PMD':'trimf',[0.3 0.6 0.9]\n" +
                "MF7='PHD':'trimf',[0.6 1 1]\n" +
                "\n" +
                "[Output1]\n" +
                "Name='RESPONSE'\n" +
                "Range=[-1 1]\n" +
                "NumMFs=7\n" +
                "MF1='PLD':'trimf',[0 0 0.3]\n" +
                "MF2='PMD':'trimf',[0.210582010582011 0.410582010582011 0.610582010582011]\n" +
                "MF3='PHD':'trimf',[0.5 1 1]\n" +
                "MF4='NHD':'trimf',[-1 -0.8 -0.5]\n" +
                "MF5='NMD':'trimf',[-0.7 -0.6 -0.35]\n" +
                "MF6='NLD':'trimf',[-0.55 -0.4 -0.25]\n" +
                "MF7='D':'trimf',[-0.4 -0.1 0.2]\n" +
                "\n" +
                "[Rules]\n" +
                "1 1, 1 (1) : 1\n" +
                "1 2, 7 (1) : 1\n" +
                "1 3, 6 (1) : 1\n" +
                "1 4, 6 (1) : 1\n" +
                "1 5, 6 (1) : 1\n" +
                "1 6, 4 (1) : 1\n" +
                "1 7, 4 (1) : 1\n" +
                "2 1, 1 (1) : 1\n" +
                "2 2, 7 (1) : 1\n" +
                "2 3, 6 (1) : 1\n" +
                "2 4, 6 (1) : 1\n" +
                "2 5, 5 (1) : 1\n" +
                "2 6, 4 (1) : 1\n" +
                "2 7, 4 (1) : 1\n" +
                "3 1, 1 (1) : 1\n" +
                "3 2, 7 (1) : 1\n" +
                "3 3, 7 (1) : 1\n" +
                "3 4, 6 (1) : 1\n" +
                "3 5, 5 (1) : 1\n" +
                "3 6, 5 (1) : 1\n" +
                "3 7, 4 (1) : 1\n" +
                "4 1, 1 (1) : 1\n" +
                "4 2, 7 (1) : 1\n" +
                "4 3, 7 (1) : 1\n" +
                "4 4, 6 (1) : 1\n" +
                "4 5, 6 (1) : 1\n" +
                "4 6, 5 (1) : 1\n" +
                "4 7, 4 (1) : 1\n" +
                "5 1, 1 (1) : 1\n" +
                "5 2, 2 (1) : 1\n" +
                "5 3, 7 (1) : 1\n" +
                "5 4, 7 (1) : 1\n" +
                "5 5, 6 (1) : 1\n" +
                "5 6, 5 (1) : 1\n" +
                "5 7, 5 (1) : 1\n" +
                "6 1, 2 (1) : 1\n" +
                "6 2, 2 (1) : 1\n" +
                "6 3, 1 (1) : 1\n" +
                "6 4, 7 (1) : 1\n" +
                "6 5, 7 (1) : 1\n" +
                "6 6, 6 (1) : 1\n" +
                "6 7, 6 (1) : 1\n" +
                "7 1, 3 (1) : 1\n" +
                "7 2, 3 (1) : 1\n" +
                "7 3, 2 (1) : 1\n" +
                "7 4, 1 (1) : 1\n" +
                "7 5, 1 (1) : 1\n" +
                "7 6, 7 (1) : 1\n" +
                "7 7, 7 (1) : 1\n";
    }
}
