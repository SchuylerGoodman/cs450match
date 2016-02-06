package com.company;

import com.sun.javaws.exceptions.InvalidArgumentException;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Enter data file name: ");
            String filename = br.readLine();
            File file = new File(filename);

            BufferedReader br2 = new BufferedReader(new FileReader(file));
            List<Double> dataList = new ArrayList<>();
            for (String line; (line = br2.readLine()) != null; ) {
                dataList.add(Double.parseDouble(line));
            }

            Double[] template = BASE_TEMPLATE;
            int templateRadius = template.length / 2;
            for (int i = 0; i < templateRadius; ++i) {
                dataList.add(0, 0.0); // Add 0.0 to beginning and end past data for template comparison.
                dataList.add(0.0);
            }

            Double[] data = new Double[dataList.size()];
            dataList.toArray(data);
            int notchIndex = Main.getNotchIndex(template, data);

            System.out.println(String.format("Dicrotic Notch is at index %d, value=%f", notchIndex, data[notchIndex]));
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    private static Double[] BASE_TEMPLATE = new Double[] {
            7.0, 5.0, 3.0, 0.0, 1.0, 2.0, 3.0
    };

    private static Double[] setMean(Double[] list, double mean) {

        Double[] newList = new Double[list.length];

        double average = 0.0;
        for (int i = 0; i < list.length; ++i) {
            average += list[i];
        }
        average /= list.length;

        double difference = average - mean;

        for (int i = 0; i < list.length; ++i) {
            newList[i] = list[i] - difference;
        }

        return newList;
    }

    private static int getNotchIndex(Double[] inTemplate, Double[] data) throws InvalidArgumentException {

        Double[] template = Main.setMean(inTemplate, 0.0);
        Double[] window = new Double[template.length];

        int templateRadius = template.length / 2;

        // Cross correlate template with data to get values at every index.
        double maxCorrelation = -Double.MIN_VALUE;
        int maxIndex = -1;
        for (int i = 0; i + template.length <= data.length; ++i) {

            System.arraycopy(data, i, window, 0, window.length);
            double correlation = Main.crossCorrelate(template, window);

            // Find index with highest correlation value.
            if (correlation > maxCorrelation) {
                maxCorrelation = correlation;
                maxIndex = i + templateRadius;
            }
        }

        return maxIndex;
    }

    /**
     * Run 0 mean cross-correlation on two dimension-matched arrays.
     *
     * @param template = the template array to match.
     * @param window = the window array to match.
     * @return the similarity measure for the cross-correlation.
     */
    private static double crossCorrelate(Double[] template, Double[] window) throws InvalidArgumentException {

        if (template.length != window.length) {
            throw new InvalidArgumentException(new String[] {"Cross correlation array lengths must match."});
        }

        int length = template.length;

        double windowAverage = 0.0;
        for (int i = 0; i < length; ++i) {
            windowAverage += window[i];
        }
        windowAverage /= length;

        // Calculate 0 mean normalized cross-correlation
        // See https://siddhantahuja.files.wordpress.com/2009/05/zncc2.png
        double templateWeight;
        double windowWeight;
        double numerator = 0.0;
        double templateDenominator = 0.0;
        double windowDenominator = 0.0;
        for (int i = 0; i < length; ++i) {
            templateWeight = template[i]; // Template is already 0 mean.
            windowWeight = window[i] - windowAverage;
            numerator += templateWeight * windowWeight;
            templateDenominator += Math.pow(templateWeight, 2);
            windowDenominator += Math.pow(windowWeight, 2);
        }
        double denominator = Math.sqrt(templateDenominator * windowDenominator);

        double correlation = numerator / denominator;

        return correlation;
    }
}
