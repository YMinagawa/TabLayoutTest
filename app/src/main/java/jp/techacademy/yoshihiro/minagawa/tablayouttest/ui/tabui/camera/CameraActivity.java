package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.camera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;

public class CameraActivity extends Activity implements CameraInterface, SensorEventListener{

    private static final String TAG = "Camera2App";

    private int REQUEST_CODE_CAMERA_PERMISSION = 0x01;

    private Size mPreviewSize;
    private AutoFitTextureView mTextureView;

    private ImageReader mImageReader;
    private BackgroundThreadHelper mThread;
    private CustomCamera mCamera;

    private ImageButton mIbtn_shutter;
    private ImageButton mIbtn_camera_config;
    private ImageButton mIbtn_ae;
    private boolean mIsAEState;
    private Button mBtn;
    private SeekBar mSbISO;
    private SeekBar mSbExopsureTime;
    private Range<Integer> mRangeISO;
    private Range<Long> mRangeExposureTime;

    private FrameLayout mFrameLayout;
    private View mCameraView;
    private View mCameraConfigView;

    private int mISO;
    private long mDurationTime;
    private long mExposureTime;

    AnimationController mAnimationController = new AnimationController();
    SensorManager mSensorManager = null;
    private boolean mIsRegisteredSensor;
    float touchY;
    float moveY;
    boolean isLandScape;
    RotateAnimation mRotateAnimation;

    private Toast mToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //センサーマネージャーの取得
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mIsRegisteredSensor = false;

        //Camera2 APIを別クラスへ切り出し
        mCamera = new CustomCamera();
        mCamera.setCameraActivity(this);
        mCamera.setInterface(this);

        //レイアウトの設定
        //FrameLayoutの上にViewを貼り付ける
        mFrameLayout = new FrameLayout(this);
        setContentView(mFrameLayout);
        mCameraView = getLayoutInflater().inflate(R.layout.activity_camera, null);
        mFrameLayout.addView(mCameraView);

        //AutoFixTextureViewはTextureViewのサブクラス
        //設定したAspect比に応じてサイズが自動的に切り替える機能が提供されている
        //もしconfig画面が出ている場合、Textureに触れるとconfig画面を閉じる
        mTextureView = (AutoFitTextureView) mCameraView.findViewById(R.id.texture);
        mTextureView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(mFrameLayout.getChildCount()==2){
                    mFrameLayout.removeView(mCameraConfigView);
                }
            }
        });
        mThread = new BackgroundThreadHelper();

        //シャッター用のボタン
        mIbtn_shutter = (ImageButton)findViewById(R.id.ibtn_shutter);
        mIbtn_shutter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture();
            }
        });

        //カメラコンフィグ用のボタン
        mCameraConfigView = getLayoutInflater().inflate(R.layout.view_camera_config, null);
        mIbtn_camera_config = (ImageButton)findViewById(R.id.ibtn_camera_config);
        mIbtn_camera_config.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //Viewの数で削除するかどうかを決める

                if(mFrameLayout.getChildCount() !=2 && mIsAEState == false){
                    mFrameLayout.addView(mCameraConfigView);
                }else if(mFrameLayout.getChildCount() == 2) {
                    mFrameLayout.removeView(mCameraConfigView);
                }else if(mFrameLayout.getChildCount() !=2 && mIsAEState == true){
                    if(mToast != null){
                        mToast.cancel();
                    }
                    mToast = new Toast(CameraActivity.this);
                    mToast.makeText(CameraActivity.this, "Turn OFF AE !!", Toast.LENGTH_SHORT).show();
                }



            }
        });

        //AE ON/OFF用のボタン
        mIbtn_ae = (ImageButton)findViewById(R.id.ibtn_ae);
        mIsAEState = true;
        mIbtn_ae.setActivated(mIsAEState);
        mIbtn_ae.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsAEState = !mIsAEState;
                mIbtn_ae.setActivated(mIsAEState);
                if(mIsAEState == true){
                    mCamera.changeAEON();

                    if(mFrameLayout.getChildCount() == 2){
                        mFrameLayout.removeView(mCameraConfigView);
                    }
                }else if(mIsAEState == false){
                    mCamera.changeAEOFF();
                }
            }
        });

        //SeekBar ISOの設定
        mSbISO = (SeekBar)mCameraConfigView.findViewById(R.id.seekBar_iso);
        mSbISO.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //ツマミをドラッグしたときに呼ばれる
                //次の表示にすぐに切り替えられるように消す
                if(mToast != null) {
                    mToast.cancel();
                }

                mToast = new Toast(CameraActivity.this);
                View toastlayout = getLayoutInflater().inflate(R.layout.toast_layout, null);
                TextView toastText = (TextView)toastlayout.findViewById(R.id.textView_toast);
                //seekbarのmin値は0で固定で変えられないので、
                //得られた値をmin分だけ底上げした値を用いる
                mISO = progress + mRangeISO.getLower();
                toastText.setText("ISO " + mISO);
                mToast.setView(toastlayout);
                //mToast = Toast.makeText(CameraActivity.this, "ISO " + progress, Toast.LENGTH_SHORT);
                mToast.show();

                mCamera.changeExposureParam(mISO, mExposureTime, mDurationTime);


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //ツマミに触れたときに呼ばれる
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //ツマミを離したときに呼ばれる

            }
        });

        //SeekBar Exposure Timeの設定
        mSbExopsureTime = (SeekBar)mCameraConfigView.findViewById(R.id.seekBar_exptime);
        mSbExopsureTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //次の表示にすぐに切り替えられるように消す
                if(mToast != null) {
                    mToast.cancel();
                }

                mToast = new Toast(CameraActivity.this);
                View toastlayout = getLayoutInflater().inflate(R.layout.toast_layout, null);
                TextView toastText = (TextView)toastlayout.findViewById(R.id.textView_toast);
                //seekbarのmin値は0で固定で変えられないので、
                //得られた値をmin分だけ底上げした値を用いる
                mExposureTime = (int)(progress + mRangeExposureTime.getLower());
                toastText.setText("ExposureTime " + mExposureTime);
                mToast.setView(toastlayout);
                mToast.show();

                mCamera.changeExposureParam(mISO, mExposureTime, mDurationTime);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        //テスト
        mBtn = (Button)mCameraConfigView.findViewById(R.id.btn_test);
        mBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                testMethod();
            }
        });

    }

    public void testMethod(){
        Log.d("mTestCameraActivity", "test");
    }


    @Override
    public void onResume() {
        super.onResume();
        mThread.start();

        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
        if(sensors.size() > 0){
            Sensor sensor = sensors.get(0);
            mIsRegisteredSensor = mSensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        }

        if (mTextureView.isAvailable()) {
            // Preview用のTextureViewの準備ができている
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            // 準備完了通知を受け取るためにリスナーを登録する
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {

        if(mIsRegisteredSensor){
            mSensorManager.unregisterListener(this);
            mIsRegisteredSensor = false;
        }

        closeCamera();
        mThread.stop();
        super.onPause();
    }

    //条件に適するカメラIDの選別
    //CameraMangerのインスタンスからgetCameraIdList()によってカメラIDを入手
    //その後、getCameraCharacteristicsで(front/rear, 解像度)を入手
    //CameraCharacteristicsからcharacteristics.get(Camera~)でパラメーターを引き出し、選別をする
    private String setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                // フロントカメラを利用しない
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }
                // ストリーム制御をサポートしていない場合、セットアップを中断する
                // Camera2ではストリームとして画像を扱う(ファイルではない)
                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // 最大サイズでキャプチャする
                // getOusputSizeでJPEGを指定して、そこで取得できるサイズの最も大きいものを取り出す
                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());

                setUpPreview(map.getOutputSizes(SurfaceTexture.class),
                        width, height, largest);
                configurePreviewTransform(width, height);

                //ImageReaderの生成
                //Surfaceに描画するイメージに直接アクセスできる機能を提供するクラス
                //maxImagesは同時にImageReaderから取得できるImage Objectsの数
                //ImageReader内でmaxImagesで指定した枚数のImageを保持してくれていて、
                //必要なタイミングで古いイメージも取得できる
                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);


                //ImageReaderにコールバック用のListenerとHandlerを設定
                //Handlerを指定(第２引数として指定)しない場合は、コール元threadでCallbackが呼ばれる
                //Imageが生成されるとイベントが通知されるので、最初からBackground threadを指定する
                mImageReader.setOnImageAvailableListener(
                        new ImageReader.OnImageAvailableListener() {

                            @Override
                            public void onImageAvailable(ImageReader reader) {
                                File file = new File(getExternalFilesDir(null), "pic.jpg");
                                mThread.getHandler().post(new ImageStore(reader.acquireNextImage(), file));
                            }

                        }, mThread.getHandler());

                return cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Camera2 API未サポート
            Log.e(TAG, "Camera Error:not support Camera2API");
        }

        return null;
    }

    private void openCamera(int width, int height) {
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission();
            return;
        }

        //条件に適するカメラIDをsetUpCameraOutputs()で選別する
        //カメラID選別の条件はsetUpCameraOutputs()に記載
        String cameraId = setUpCameraOutputs(width, height);

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!mCamera.isLocked()) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }

            //最適なCamera(ID)からパラメーターを入手
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            mRangeISO = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            //SeekBarにminを設定できないため、maxをminだけ下げておき、出力時に+minする
            mSbISO.setMax(mRangeISO.getUpper() - mRangeISO.getLower());
            //Log.d("mTestCameraActivity", "min = " + mRangeISO.getLower());
            mRangeExposureTime = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            mSbExopsureTime.setMax((int)(mRangeExposureTime.getUpper() - mRangeExposureTime.getLower()));

            manager.openCamera(cameraId, mCamera.stateCallback, mThread.getHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void closeCamera() {
        mCamera.close();
        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }

    //Texture Listener
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            // SurfaceTextureの準備が完了した
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            // Viewのサイズに変更があったためPreviewサイズを計算し直す
            configurePreviewTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    private void setUpPreview(Size[] choices, int width, int height, Size aspectRatio) {
        // カメラ性能を超えたサイズを指定するとキャプチャデータにゴミ(garbage capture data)がまじるため、注意

        // 表示するSurfaceより、高い解像度のプレビューサイズを抽出する
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // プレビューを表示するSurfaceに最も近い（小さな）解像度を選択する
        if (bigEnough.size() > 0) {
            mPreviewSize = Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            mPreviewSize = choices[0];
        }

        // プレビューが歪まないようにアスペクト比を調整する
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mTextureView.setAspectRatio(
                    mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } else {
            mTextureView.setAspectRatio(
                    mPreviewSize.getHeight(), mPreviewSize.getWidth());
        }
    }

    private void configurePreviewTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    // パーミッションの処理シーケンスはまだおかしい
    // Parmission handling for Android 6.0
    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            // 権限チェックした結果、持っていない場合はダイアログを出す
            new AlertDialog.Builder(this)
                    .setMessage("Request Permission")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestPermissions(new String[]{Manifest.permission.CAMERA},
                                    REQUEST_CODE_CAMERA_PERMISSION);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                    .create();
            return;
        }

        // 権限を取得する
        requestPermissions(new String[]{Manifest.permission.CAMERA},
                REQUEST_CODE_CAMERA_PERMISSION);
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                new AlertDialog.Builder(this)
                        .setMessage("Need Camera Permission")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        })
                        .create();
            }
            return;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public SurfaceTexture getSurfaceTextureFromTextureView() {
        return mTextureView.getSurfaceTexture();
    }

    @Override
    public Size getPreviewSize() {
        return mPreviewSize;
    }

    @Override
    public Handler getBackgroundHandler() {
        return mThread.getHandler();
    }

    @Override
    public Surface getImageRenderSurface() {
        return mImageReader.getSurface();
    }

    @Override
    public int getRotation() {
        return getWindowManager().getDefaultDisplay().getRotation();
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    //センサーに変化があったときのリスナー
    @Override
    public void onSensorChanged(SensorEvent event) {
        if(event.sensor.getType() == Sensor.TYPE_ORIENTATION){
            ImageButton[] ibtns = {mIbtn_shutter};
            mAnimationController.rotateScreen(event, ibtns);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //AEmode時の最適なDurationFrame, ISO, ExposureTimeを受け取る
    public void setCameraParam(int ISO, long ExposureTime, long DurationTime){
        mISO = ISO;
        mExposureTime = ExposureTime;
        mDurationTime = DurationTime;
        mSbISO.setProgress(mISO);
        mSbExopsureTime.setProgress((int)mDurationTime);
    }

}

