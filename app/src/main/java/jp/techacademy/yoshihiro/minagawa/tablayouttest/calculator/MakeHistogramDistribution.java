package jp.techacademy.yoshihiro.minagawa.tablayouttest.calculator;

import java.util.Arrays;

/**
 * Created by ym on 2016/10/31.
 */

public class MakeHistogramDistribution{

    static double[] freqDist;
    static double[] xAxis;
    static int binWidth;

    //ヒストグラムの階級化
    public static void makeFreqDistribution(int binNum, double[] srcArray){

        freqDist = new double[binNum];
        xAxis = new double[binNum];

        double[] tempArray = Arrays.copyOf(srcArray, srcArray.length);

        Arrays.sort(tempArray);
        double max = tempArray[tempArray.length-1];
        binWidth = (int)(max/binNum) + 1;

        for(double value : tempArray){
            int num;
            num = (int)(value/binWidth);
            freqDist[num]++;
        }

        for(int i = 0; i<binNum; i++){
            xAxis[i] = binWidth/2 + binWidth*i;
        }

    }

    public static double[] getFreqDistribution(){
        return freqDist;
    }

    public static double[] getXAxis(){
        return xAxis;
    }

    public static int getBinWidth(){
        return binWidth;
    }

}