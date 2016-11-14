package jp.techacademy.yoshihiro.minagawa.tablayouttest.calculator;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Created by ym on 2016/11/11.
 */


//ImageJからの移植
//大まかな流れは
// 転がすBallの大きさ(radius)を受け取ってRollingBallクラスのインスタンスを作成する
// rollingBallFloatBackGround内でshrinkMatを呼び出してMatをshrinkさせる
// そしてBallを転がす(rollBallメソッド)
// shrinkしていたらmatをenlargeする

public class BackGroundSubtracter {

    Mat subBackGroundMat = new Mat();

    public BackGroundSubtracter(Mat targetMat, double radius){
        rollingBallBackground(targetMat, radius);
    }

    public void rollingBallBackground(Mat targetMat, double radius){

        RollingBall ball = null;
        ball = new RollingBall(radius);

        Mat backGroundMat = rollingBallFloatBackground(targetMat, (float)radius, ball);
        float[] bgPixels = MatToArray(backGroundMat);

        float[] pixels = MatToArray(targetMat);

        //バックグランドを引いた結果を格納するArray
        float[] subBackPixels = new float[targetMat.height()*targetMat.width()];

        for(int p=0; p<bgPixels.length; p++){
            subBackPixels[p] = pixels[p] - bgPixels[p];
        }

        this.subBackGroundMat = ArrayToMat(subBackPixels, targetMat.width(), targetMat.height());

    }

    Mat rollingBallFloatBackground(Mat targetMat, float radius, RollingBall ball){

        boolean isShrink = ball.shrinkFactor > 1;
        Mat smallMat = isShrink ? shrinkMat(targetMat, ball.shrinkFactor) : targetMat;
        Mat backGroundMat = rollBall(ball, smallMat);
        if(isShrink){
            backGroundMat = enlargeMat(backGroundMat, targetMat, ball.shrinkFactor);
        }

        return backGroundMat;
    }

    Mat shrinkMat(Mat mat, int shrinkFactor){

        int width = mat.width();
        int height = mat.height();

        int sWidth = (width+shrinkFactor-1)/shrinkFactor;
        int sHeight =(height+shrinkFactor-1)/shrinkFactor;
        float[] pixels = MatToArray(mat);
        Mat smallMat = new Mat(sHeight, sWidth, mat.type());
        double min, thisPixel;

        for(int ySmall = 0; ySmall < sHeight; ySmall++){
            for(int xSmall = 0; xSmall < sWidth; xSmall++){
                min = Float.MAX_VALUE;
                for(int j=0, y=shrinkFactor*ySmall; j<shrinkFactor&&y<height; j++, y++){
                    for(int k=0, x=shrinkFactor*xSmall; k<shrinkFactor&&x<width; k++, x++){
                        thisPixel = pixels[x+y*width];
                        if(thisPixel < min){
                            min = thisPixel;
                        }
                    }
                }
                double[] value = new double[0];
                value[0] = min;
                smallMat.put(ySmall, xSmall, value);
            }
        }
        return smallMat;
    }

    Mat rollBall(RollingBall ball, Mat mat){

        float[] pixels = MatToArray(mat);
        int width = mat.width();
        int height = mat.height();
        float[] zBall = ball.data;
        int ballWidth = ball.width;
        int radius = ballWidth/2;
        float[] cache = new float[width*ballWidth];

        Thread thread = Thread.currentThread();
        long lastTime = System.currentTimeMillis();
        for(int y = -radius; y<height+radius;y++){
            long time = System.currentTimeMillis();
            if(time-lastTime>100){
                lastTime = time;
                //if(thread.isInterrupted()) return new Mat();
            }
            int nextLineToWriteInCache = (y+radius)%ballWidth;
            int nextLineToRead = y + radius;
            if(nextLineToRead<height){
                System.arraycopy(pixels, nextLineToRead*width, cache, nextLineToWriteInCache*width, width);
                for(int x =0, p = nextLineToRead*width; x<width; x++, p++){
                    pixels[p] = -Float.MAX_VALUE; //unprocessed pixels start at minus infinity
                }
            }
            int y0 = y-radius;
            if(y0<0){y0=0;}
            int yBall0 = y0-y+radius;
            int yend = y+radius;
            if(yend>=height){yend=height-1;}

            for(int x=-radius; x<width+radius; x++){
                float z = Float.MAX_VALUE;
                int x0 = x-radius;
                if(x0<0){x0=0;}
                int xBall0 = x0-x+radius;
                int xend = x+radius;
                if(xend>=width){xend=width-1;}
                for(int yp=y0, yBall=yBall0; yp<=yend; yp++, yBall++){
                    int cachePointer = (yp%ballWidth)*width+x0;
                    for(int xp=0, bp=xBall0+yBall*ballWidth; xp<= xend; xp++, cachePointer++, bp++){
                        float zReduced = cache[cachePointer] - zBall[bp];
                        if(z>zReduced){
                            z=zReduced;
                        }
                    }
                }

                for(int yp=y0, yBall=yBall0; yp<=yend; yp++, yBall++){
                    for(int xp=x0, p=xp+yp+width, bp=xBall0+yBall*ballWidth; xp<=xend; xp++, p++, bp++){
                        float zMin = z + zBall[bp];
                        if(pixels[p] < zMin){
                            pixels[p] = zMin;
                        }
                    }
                }
            }
        }

        return ArrayToMat(pixels, mat.width(), mat.height());
    }

    Mat enlargeMat(Mat smallMat, Mat mat, int shrinkFactor){
        int width = mat.width();
        int height = mat.height();
        int smallWidth = smallMat.width();
        int smallHeight = smallMat.height();
        float[] pixels = MatToArray(mat);
        float[] sPixels = MatToArray(smallMat);
        int[] xSmallIndices = new int[width];
        float[] xWeights = new float[width];
        makeInterpolationArrays(xSmallIndices, xWeights, width, smallWidth, shrinkFactor);
        int[] ySmallIndices = new int[height];
        float[] yWeights = new float[height];
        makeInterpolationArrays(ySmallIndices, yWeights, height, smallHeight, shrinkFactor);
        float[] line0 = new float[width];
        float[] line1 = new float[width];
        for (int x=0; x<width; x++)                 //x-interpolation of the first smallImage line
            line1[x] = sPixels[xSmallIndices[x]] * xWeights[x] +
                    sPixels[xSmallIndices[x]+1] * (1f - xWeights[x]);
        int ySmallLine0 = -1;                       //line0 corresponds to this y of smallImage
        for (int y=0; y<height; y++) {
            if (ySmallLine0 < ySmallIndices[y]) {
                float[] swap = line0;               //previous line1 -> line0
                line0 = line1;
                line1 = swap;                       //keep the other array for filling with new data
                ySmallLine0++;
                int sYPointer = (ySmallIndices[y]+1)*smallWidth; //points to line0 + 1 in smallImage
                for (int x=0; x<width; x++)         //x-interpolation of the new smallImage line -> line1
                    line1[x] = sPixels[sYPointer+xSmallIndices[x]] * xWeights[x] +
                            sPixels[sYPointer+xSmallIndices[x]+1] * (1f - xWeights[x]);
            }
            float weight = yWeights[y];
            for (int x=0, p=y*width; x<width; x++,p++)
                pixels[p] = line0[x]*weight + line1[x]*(1f - weight);
        }

        return ArrayToMat(pixels, mat.width(), mat.height());
    }

    void makeInterpolationArrays(int[] smallIndices, float[] weights, int length, int smallLength, int shrinkFactor) {
        for (int i=0; i<length; i++) {
            int smallIndex = (i - shrinkFactor/2)/shrinkFactor;
            if (smallIndex >= smallLength-1) smallIndex = smallLength - 2;
            smallIndices[i] = smallIndex;
            float distance = (i + 0.5f)/shrinkFactor - (smallIndex + 0.5f); //distance of pixel centers (in smallImage pixels)
            weights[i] = 1f - distance;
            //if(i<12)IJ.log("i,sI="+i+","+smallIndex+", weight="+weights[i]);
        }
    }

    private float[] MatToArray(Mat mat){

        float[] pixels = new float[mat.width()*mat.height()];
        for(int y = 0; y < mat.height(); y++){
            for(int x = 0; x < mat.width(); x++){
                pixels[x+mat.width()*y] = (float)(mat.get(y, x)[0]);
            }

        }
        return pixels;
    }

    private Mat ArrayToMat(float[] pixels, int width, int height){

        Mat mat = new Mat(height, width, CvType.CV_8UC1);

        for(int y = 0; y < mat.height(); y++){
            for(int x = 0; x < mat.width(); x++){
                double[] value = new double[1];
                value[0] = pixels[x+mat.width()*y];
                mat.put(y, x, value);
            }
        }

        return mat;

    }


}

//  C L A S S   R O L L I N G B A L L

/** A rolling ball (or actually a square part thereof)
 *  Here it is also determined whether to shrink the image
 */
class RollingBall {

    float[] data;
    int width;
    int shrinkFactor;

    RollingBall(double radius) {
        int arcTrimPer;
        if (radius<=10) {
            shrinkFactor = 1;
            arcTrimPer = 24; // trim 24% in x and y
        } else if (radius<=30) {
            shrinkFactor = 2;
            arcTrimPer = 24; // trim 24% in x and y
        } else if (radius<=100) {
            shrinkFactor = 4;
            arcTrimPer = 32; // trim 32% in x and y
        } else {
            shrinkFactor = 8;
            arcTrimPer = 40; // trim 40% in x and y
        }
        buildRollingBall(radius, arcTrimPer);
    }

    /** Computes the location of each point on the rolling ball patch relative to the
     center of the sphere containing it.  The patch is located in the top half
     of this sphere.  The vertical axis of the sphere passes through the center of
     the patch.  The projection of the patch in the xy-plane below is a square.
     */
    void buildRollingBall(double ballradius, int arcTrimPer) {
        double rsquare;     // rolling ball radius squared
        int xtrim;          // # of pixels trimmed off each end of ball to make patch
        int xval, yval;     // x,y-values on patch relative to center of rolling ball
        double smallballradius; // radius of rolling ball (downscaled in x,y and z when image is shrunk)
        int halfWidth;      // distance in x or y from center of patch to any edge (patch "radius")

        this.shrinkFactor = shrinkFactor;
        smallballradius = ballradius/shrinkFactor;
        if (smallballradius<1)
            smallballradius = 1;
        rsquare = smallballradius*smallballradius;
        xtrim = (int)(arcTrimPer*smallballradius)/100; // only use a patch of the rolling ball
        halfWidth = (int)Math.round(smallballradius - xtrim);
        width = 2*halfWidth+1;
        data = new float[width*width];

        for (int y=0, p=0; y<width; y++)
            for (int x=0; x<width; x++, p++) {
                xval = x - halfWidth;
                yval = y - halfWidth;
                double temp = rsquare - xval*xval - yval*yval;
                data[p] = temp>0. ? (float)(Math.sqrt(temp)) : 0f;
                //-Float.MAX_VALUE might be better than 0f, but gives different results than earlier versions
            }
        //IJ.log(ballradius+"\t"+smallballradius+"\t"+width); //###
        //IJ.log("half patch width="+halfWidth+", size="+data.length);
    }
}



