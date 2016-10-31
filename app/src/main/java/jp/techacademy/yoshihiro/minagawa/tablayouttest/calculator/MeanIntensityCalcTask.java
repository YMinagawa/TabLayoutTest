package jp.techacademy.yoshihiro.minagawa.tablayouttest.calculator;

/**
 * Created by ym on 2016/10/29.
 */

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.concurrent.Callable;

public class MeanIntensityCalcTask implements Callable<Double> {

    static{System.loadLibrary("opencv_java3");}
    Rect mRect;
    static Mat mImageMat;

    public MeanIntensityCalcTask(Rect rect){
        mRect = rect;
    }

    public static void setImageMat(Mat imageMat){
        mImageMat = imageMat;
    }

    @Override
    public Double call() throws Exception {

        Mat dstMat = new Mat(mImageMat, mRect);
        //Log.d("mCalcTask", "dstMatWidth = " + dstMat.width() +  "  dstMatHeight = " + dstMat.height());
        //Log.d("mCalcTask", "RectWidth = " + mRect.width +  "  RectHeight = " + mRect.height);
        //Log.d("mCalcTask", "dstMat (x,y) = (0,0) = " + dstMat.get(0,0)[0]);
        //Log.d("mCalcTask", "srcMat " + "(x,y) = (" + mRect.x + "," + mRect.y + ") = " + mImageMat.get(mRect.x, mRect.y)[0] );

        double sum = 0;
        int count = 0;
        for(int x = 0; x < mRect.width; x++){
            for(int y = 0; y < mRect.height; y++){
                //Log.d("mCalcTask", "dstMat (y,x) = (" + y + "," + x+") = " + dstMat.get(y, x)[0]);
                count++;
                sum += dstMat.get(y, x)[0];
            }
        }

        //Log.d("mCalcTask", "sum = " + (value/count));
        double meanIntensity = sum/count;
        return meanIntensity;
    }
}
