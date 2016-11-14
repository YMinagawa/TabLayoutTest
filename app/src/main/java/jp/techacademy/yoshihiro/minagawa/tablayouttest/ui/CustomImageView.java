package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/**
 * Created by ym on 2016/10/25.
 */

public class CustomImageView extends ImageView {

    //ImageViewにはMatrixを使用して描画する機能が備わっているので、
    // Matrixで拡大、縮小、移動を制御することが可能
    private Matrix mMatrix = new Matrix();
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;
    //SCALE_MAX、SCALE_MINで最大拡大率と、最小縮小率を設定
    private final float SCALE_MAX = 6.0f;
    private float mScaleMin;
    private final float PINCH_SENSITIVITY = 15.0f;
    private boolean isFirstDraw = true;

    //コンストラクタ1
    public CustomImageView(Context context) {
        super(context);
        init(context);
    }

    //コンストラクタ2
    public CustomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    //コンストラクタ3
    public CustomImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    //mScaleGestureDetectorを初期化
    private void init(Context context) {
        //setImageResource();
        Log.d("mTestCustomImage", "initialized!!");
        setScaleType(ScaleType.MATRIX);
        mScaleGestureDetector = new ScaleGestureDetector(context, mSimpleOnScaleGestureListener);
        mGestureDetector = new GestureDetector(context, mSimpleOnGestureListener);

    }

    @Override
    //Viewのライフサイクル上でView#onAttachedToWindow()→View#onMeasure()→「View#onLayout(true,)」
    //→View#onMeasure()→View#onDrawの順番で呼ばれる。
    //ここでImageViewとImageの中心位置の合わせとスケールの初期化(画面がぴったり合うサイズをScaleMinにする)を行う
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if(isFirstDraw == true) {
            Log.d("mTestCustomImage", "FirstDraw on Layout");
            //ScaleMinをここで定義して、
            //ImageをImageViewにFitする

            //まず中心位置に移動させる
            float imageWidth = getImageWidth();
            float imageHeight = getImageHeight();
            float imageViewWidth = getWidth();
            float imageViewHeight = getHeight();

            float x = imageViewWidth/2 - (getMatrixValue(Matrix.MSCALE_X) + getImageWidth()/2);
            float y = imageViewHeight/2 - (getMatrixValue(Matrix.MSCALE_Y) + getImageHeight()/2);

            mMatrix.postTranslate(x, y);

            //次に、スケールをあわせて、Viewの中心で縮小を行う
            float widthScale = imageViewWidth / imageWidth;
            float heightScale = imageViewHeight / imageHeight;

            if (widthScale < 1 && heightScale < 1) {
                if (widthScale < heightScale) {
                    mScaleMin = widthScale;
                } else {
                    mScaleMin = heightScale;
                }
            } else if (widthScale < 1) {
                mScaleMin = widthScale;
            } else if (heightScale < 1) {
                mScaleMin = heightScale;
            }

            //拡大・縮小(第3、第4引数は拡大、縮小の基点)
            mMatrix.postScale(mScaleMin, mScaleMin, imageViewWidth / 2, imageViewHeight / 2);
            this.setImageMatrix(mMatrix);
            isFirstDraw = false;
        }
        Log.d("mTestCustomImage", "On Layout");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d("mTestCustomImage", "OnDraw");

    }

    //OnTouchEvent内にmScaleGestureDetector.onTouchEventを呼ぶことで
    //mScaleGestureDetectorに処理を委譲する。
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.setImageMatrix(mMatrix);
        mScaleGestureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);
        return true;
    }

    private ScaleGestureDetector.SimpleOnScaleGestureListener mSimpleOnScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        //二本指でタッチしたときの、タッチされた二点の中心位置
        float focusX;
        float focusY;

        @Override
        //ジェスチャー中(二本指でタッチしている間)に呼ばれる
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = 1.0f;
            //Matrix.MSCALE_Yはy方向の縮小・拡大率
            float previousScale = getMatrixValue(Matrix.MSCALE_Y);

            //gestureの距離変化比をそのまま使うと反応が良すぎるので、
            //PINCH_SENSITIVITYで割って、鈍くしている
            //getScaleFactor()は「今回２点タッチの距離/前回の2点タッチの距離」を返す
            //今回＞前回のとき拡大、前回>今回のとき縮小
            if (detector.getScaleFactor() >= 1.0f) {
                scaleFactor = 1 + (detector.getScaleFactor() - 1) / (previousScale * PINCH_SENSITIVITY);
            } else {
                scaleFactor = 1 - (1 - detector.getScaleFactor()) / (previousScale * PINCH_SENSITIVITY);
            }

            float scale = scaleFactor * previousScale;
            if (scale < mScaleMin || SCALE_MAX < scale) {
                return false;
            }

            //拡大・縮小(第3、第4引数は拡大、縮小の基点)
            mMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
            //描画を行いたい為にinvalidateメソッドを呼びだす
            invalidate();

            return super.onScale(detector);
        }

        @Override
        //onScaleBeginはジェスチャー開始時(二本指でタッチする)に呼ばれる
        //タッチした２点の中心点をScaleGestureDetectorのgetFocusXとgetFocusYで得ることができる。
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            focusX = detector.getFocusX();
            focusY = detector.getFocusY();
            return super.onScaleBegin(detector);
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
        }
    };

    private float getMatrixValue(int index) {
        if (mMatrix == null) {
            mMatrix = getImageMatrix();
        }

        float[] values = new float[9];
        mMatrix.getValues(values);

        float value = values[index];
        return value;

    }

    private final GestureDetector.SimpleOnGestureListener mSimpleOnGestureListener = new GestureDetector.SimpleOnGestureListener(){
        @Override
        //onScrollは画面が押されスクロールが開始するとスクロールの間(指が離れるまで)呼ばれます
        //float distanceX, distanceYはそれぞれ押下時の指の位置から現在の指の位置までの移動距離
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //ImageViewの縦横長(Viweの縦横は常に一定)
            float imageViewWidth = getWidth();
            float imageViewHeight = getHeight();
            //画像の縦横長(拡大縮小後の縦横長さ(可変))
            float imageWidth = getImageWidth();
            float imageHeight = getImageHeight();
            //画像の左辺、右辺のx座標
            //MTRANS_Xは画像の左端頂点のx座標(可変)
            float leftSideX = getMatrixValue(Matrix.MTRANS_X);
            float rightSideX = leftSideX + imageWidth;
            //画像の上辺、底辺のy座標
            //MTRANS_Yは画像の左端頂点のy座標
            float topY = getMatrixValue(Matrix.MTRANS_Y);
            float bottomY = topY + imageHeight;

            if(imageViewWidth >= imageWidth && imageViewHeight >= imageHeight){
                return false;
            }
            //指の動きに追随してほしいため符号を反転
            float x = -distanceX;
            float y = -distanceY;

            if(imageViewWidth > imageWidth) {
                x = 0;
            }else{
                if(leftSideX > 0 && x > 0){
                    //画像の左端がImageViewの中に入ったときも揺り戻す
                    //つまり左端を0に合わせるようにする
                    x = -leftSideX;
                }else if(rightSideX < imageViewWidth && x < 0){
                    //画像の右端がImageViewの右端より小さいとき(中に入ったとき)には揺り戻す
                    //つまり右端をImageViewWidthの位置に来るようにする
                    x = imageViewWidth - rightSideX;
                }
            }

            if(imageViewHeight > imageHeight){
                y = 0;
            }else{
                if(topY > 0 && y > 0){
                    y = - topY;
                }else if(bottomY < imageViewHeight && y < 0){
                    y = imageViewHeight - bottomY;
                }
            }
            //Matrixを操作
            //呼ばれた指の移動距離の応じた操作を加えた後invalidateを呼び再描画を行う
            mMatrix.postTranslate(x, y);
            //再描画
            invalidate();

            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    };

    private float getImageWidth(){
        return(getDrawable().getIntrinsicWidth())*getMatrixValue(Matrix.MSCALE_X);
    }

    private float getImageHeight(){
        return(getDrawable().getIntrinsicHeight())*getMatrixValue(Matrix.MSCALE_Y);
    }
}

