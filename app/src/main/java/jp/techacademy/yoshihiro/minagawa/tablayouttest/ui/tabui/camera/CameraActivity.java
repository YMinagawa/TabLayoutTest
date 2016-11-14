package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.camera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.CapturedImageObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.MeasuredDateAndDataObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.UserObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.CustomSeekbar;

import static android.R.attr.button;
import static android.widget.Toast.makeText;

public class CameraActivity extends Activity implements CameraInterface, SensorEventListener{

    private static final String TAG = "Camera2App";

    //User
    //Realmのメンバ変数
    private Realm mRealm;
    private RealmResults<UserObject> mUserRealmResults;
    private RealmChangeListener mRealmChangeListener = new RealmChangeListener() {
        @Override
        public void onChange() {
        }
    };

    private UserObject mUserObject;

    //idをインテントで入手するためのメンバ変数
    int mId;

    private static Toast mToast;

    private int REQUEST_CODE_CAMERA_PERMISSION = 0x01;
    private int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 0x02;
    private int mCaptNum = 0;

    private Size mPreviewSize;
    private AutoFitTextureView mTextureView;

    private ImageReader mImageReader;
    private BackgroundThreadHelper mThread;
    private CustomCamera mCamera;

    private ImageButton mIbtn_shutter;
    private ImageButton mIbtn_camera_config;
    private ImageButton mIbtn_ae;
    private boolean mIsAEState;
    private ImageButton mIbtn_af;
    private boolean mIsAFState;
    private Button mBtn;
    private Button mBtn_reset;
    private SeekBar mSbISO;
    private SeekBar mSbExopsureTime;
    private CustomSeekbar mCSbFocusDistance;
    private Range<Integer> mRangeISO;
    private Range<Long> mRangeExposureTime;
    private Range mRangeFocusDistance;
    private float mMinimumLens;

    private FrameLayout mFrameLayout;
    private View mCameraView;
    private View mCameraConfigView;

    private int mISO;
    private long mDurationTime;
    private long mExposureTime;
    private float mFocusDistance;

    private String mUserName;

    private Date mDate;

    AnimationController mAnimationController = new AnimationController();
    SensorManager mSensorManager = null;
    private boolean mIsRegisteredSensor;
    float touchY;
    float moveY;
    boolean isLandScape;
    RotateAnimation mRotateAnimation;

    private Toast mConfigToast;

    ImageSaver mImageSaver;

    private File mImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //インテントからidを入手してどのユーザーかを決定する
        Intent intent = getIntent();
        mId = intent.getIntExtra("id", 0);

        //ここで選択されたユーザーオブジェクトの入手を行う
        //getDefalultInstance()をしたら必ずcloseする
        //Realmの設定
        mRealm = Realm.getDefaultInstance();
        mUserRealmResults = mRealm.where(UserObject.class).equalTo("id", mId).findAll();
        mUserObject = mUserRealmResults.get(0);
        mUserName = mUserObject.getName();


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
                //requestWriteStoragePermission();

                mIbtn_shutter.setEnabled(false);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mIbtn_shutter.setEnabled(true);
                    }
                }, 3000L);

                try {
                    mImageFile = createImageFile();
                }catch(IOException e){
                    e.printStackTrace();
                }
                mCamera.takePicture(mIsAEState);
            }
        });

        //カメラコンフィグ用のボタン
        mCameraConfigView = getLayoutInflater().inflate(R.layout.view_camera_config, null);
        mIbtn_camera_config = (ImageButton)findViewById(R.id.ibtn_camera_config);
        mIbtn_camera_config.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //Viewの数でConfig画面を削除するかどうかを決める

                if(mFrameLayout.getChildCount() !=2 && mIsAEState == false){
                    mFrameLayout.addView(mCameraConfigView);
                }else if(mFrameLayout.getChildCount() == 2) {
                    mFrameLayout.removeView(mCameraConfigView);
                }else if(mFrameLayout.getChildCount() !=2 && mIsAEState == true){
                    toast("Turn OFF AE !!");

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
                    mCamera.changeCameraCondition(mIsAFState, mIsAEState);

                    //カメラコンフィグが出ていたら消す
                    if(mFrameLayout.getChildCount() == 2){
                        mFrameLayout.removeView(mCameraConfigView);
                    }
                }else if(mIsAEState == false){
                    mCamera.changeCameraCondition(mIsAFState, mIsAEState);
                }
            }
        });

        //AF ON/OFF用のボタン
        mIbtn_af = (ImageButton)findViewById(R.id.ibtn_af);
        mIsAFState = true;
        mIbtn_af.setActivated(mIsAFState);
        mIbtn_af.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsAFState = !mIsAFState;
                mIbtn_af.setActivated(mIsAFState);
                if(mIsAFState == true){
                    mCSbFocusDistance.setEnabled(false);
                    mCSbFocusDistance.setVisibility(View.INVISIBLE);
                    //mCamera.changeAFON();
                }else if(mIsAFState == false){
                    mCSbFocusDistance.setEnabled(true);
                    mCSbFocusDistance.setVisibility(View.VISIBLE);
                    //mCamera.changeAFOFF();
                }
            }
        });

        //リセットボタン
        mBtn_reset = (Button)findViewById(R.id.btn_reset);
        mBtn_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCaptNum = 0;
            }
        });

        //FocusDistanceを変えるCustomSeekbarの設定
        mCSbFocusDistance = (CustomSeekbar)findViewById(R.id.seekbar_focusdist);
        mCSbFocusDistance.setEnabled(false);
        mCSbFocusDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //mFocusDistance =(((float)progress)*mMinimumLens/100);
                //mCamera.changeExposureParam(mISO, mExposureTime, mDurationTime, mFocusDistance);
                //Log.d("mCameraActivity", "focus : " + mFocusDistance);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //ここからCameraConfigViewの設定
        //SeekBar ISOの設定(CameraCongigView)
        mSbISO = (SeekBar)mCameraConfigView.findViewById(R.id.seekBar_iso);
        mSbISO.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //ツマミをドラッグしたときに呼ばれる
                //次の表示にすぐに切り替えられるように消す
                if(mConfigToast != null) {
                    mConfigToast.cancel();
                }

                mConfigToast = new Toast(CameraActivity.this);
                View toastlayout = getLayoutInflater().inflate(R.layout.toast_layout, null);
                TextView toastText = (TextView)toastlayout.findViewById(R.id.textView_toast);
                //seekbarのmin値は0で固定で変えられないので、
                //得られた値をmin分だけ底上げした値を用いる
                mISO = progress + mRangeISO.getLower();
                toastText.setText("ISO " + mISO);
                mConfigToast.setView(toastlayout);
                //mConfigToast = Toast.makeText(CameraActivity.this, "ISO " + progress, Toast.LENGTH_SHORT);
                mConfigToast.show();

                //seekbarで設定された値を入力する
                mCamera.changeExposureParam(mISO, mExposureTime, mDurationTime, 0);
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

        //SeekBar Exposure Timeの設定(CameraConfigView)
        mSbExopsureTime = (SeekBar)mCameraConfigView.findViewById(R.id.seekBar_exptime);
        mSbExopsureTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //次の表示にすぐに切り替えられるように消す
                if(mConfigToast != null) {
                    mConfigToast.cancel();
                }

                mConfigToast = new Toast(CameraActivity.this);
                View toastlayout = getLayoutInflater().inflate(R.layout.toast_layout, null);
                TextView toastText = (TextView)toastlayout.findViewById(R.id.textView_toast);
                //seekbarのmin値は0で固定で変えられないので、
                //得られた値をmin分だけ底上げした値を用いる
                mExposureTime = (int)(progress + mRangeExposureTime.getLower());
                String extime = String.format("%.3f ms", mExposureTime/1e6);
                toastText.setText("ExposureTime " + extime);
                mConfigToast.setView(toastlayout);
                mConfigToast.show();
                //Log.d("mTestCustomCamera" , "DurationTime = " + mDurationTime);

                mCamera.changeExposureParam(mISO, mExposureTime, mDurationTime, mFocusDistance);
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

        if(mCamera == null){
            mCamera = new CustomCamera();
            mCamera.setCameraActivity(this);
            mCamera.setInterface(this);
        }

        Log.d("CameraActivity", "onResume");

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
        Log.d("CameraActivity", "OnPause");
        closeCamera();
        mThread.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mRealm.close();
        super.onDestroy();

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

                //Raw画像の撮影ができない場合、セットアップを中断する
                int[] capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
                Log.d("mTestTabLayoutText", "capasiblities" + capabilities);
                //if(!contains(capabilities, CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)){
                //    continue;
                //}

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

                //ImageFormat.FLEX_RGB_888

                //ImageReaderにコールバック用のListenerとHandlerを設定
                //Handlerを指定(第２引数として指定)しない場合は、コール元threadでCallbackが呼ばれる
                //Imageが生成されるとイベントが通知されるので、最初からBackground threadを指定する
                mImageReader.setOnImageAvailableListener(
                        new ImageReader.OnImageAvailableListener() {


                            @Override
                            public void onImageAvailable(ImageReader reader) {
                                mThread.getHandler().post(new ImageSaver(reader.acquireLatestImage(), getApplicationContext(), mImageFile));
                                //makeText(CameraActivity.this, "Finished Save Image", Toast.LENGTH_SHORT).show();
                                toast("Finished Save Image");
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

            //最適なCamera(ID)からパラメーター(ISOとExposureTime)を入手
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            mRangeISO = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
            //SeekBarにminを設定できないため、maxをminだけ下げておき、出力時に+minする
            mSbISO.setMax(mRangeISO.getUpper() - mRangeISO.getLower());
            //Log.d("mTestCameraActivity", "min = " + mRangeISO.getLower());
            mRangeExposureTime = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            mSbExopsureTime.setMax((int)(mRangeExposureTime.getUpper() - mRangeExposureTime.getLower()));
            mMinimumLens = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
            //

            manager.openCamera(cameraId, mCamera.stateCallback, mThread.getHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private void closeCamera() {

        if(null != mCamera){
            mCamera.close();
            mCamera = null;
        }
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
    //カメラを使って良いかの許可要求
    public void onRequestPermissionsResult(int requestCode,
                                               String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

        if(requestCode == REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                new AlertDialog.Builder(this)
                        .setMessage("Need Write External Storage Permission")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        }).create();
            }
        }
    }


    //ImageReaderでCallBackされたときに保存する先のフォルダの作成
    //+ Realmの処理(測定日や撮影した図のファイルパスなど・・)
    private String mTimeStamp;
    String IMAGE_LOCATION = "TestImageLocation";

    File createImageFile() throws IOException{

        mRealm.beginTransaction();

        RealmList<MeasuredDateAndDataObject> dateAndDataList = mUserObject.getMeasuredDateAndDataList();
        if(dateAndDataList == null){
            dateAndDataList = new RealmList<MeasuredDateAndDataObject>();
        }
        MeasuredDateAndDataObject measuredDateAndDataObject = null;
        RealmList<CapturedImageObject> capturedImages = null;


        //とりあえず画像を保存する先はFolderを外部公開共有領域に作成する
        //フォルダ名は ユーザー/日付日時/
        if(mCaptNum == 0) {
            mDate = new Date();
            mTimeStamp = new SimpleDateFormat("_yyyyMMdd_HHmmss").format(mDate);
            measuredDateAndDataObject = new MeasuredDateAndDataObject();
            measuredDateAndDataObject.setMeasuredDate(mDate);
            measuredDateAndDataObject.setISO(mCamera.getISO());
            float expTime = (float)(mCamera.getExposureTime()/1e6);
            measuredDateAndDataObject.setExposureTime(expTime);
            capturedImages = new RealmList<CapturedImageObject>();
        }else{
            //同じ日付のフォルダがないか検索をかける
            RealmResults<MeasuredDateAndDataObject> results = dateAndDataList.where().equalTo("measuredDate", mDate).findAll();
            measuredDateAndDataObject = results.get(0);
            capturedImages = measuredDateAndDataObject.getCapturedImages();
        }

        //何故か毎回アクセスしないとReadOnlyで弾かれる
        //外部公開共有領域のピクチャーフォルダーに保存する場合
        //File storageDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        //内部データ領域に保存する場合
        File storageDirectory = getApplicationContext().getFilesDir();

        File ImageFolder = new File(storageDirectory.getPath()+"/"+ mUserName + "/" + IMAGE_LOCATION+mTimeStamp);
        if (!ImageFolder.exists()) {
            ImageFolder.mkdirs();
        }

        //画像ファイルの作成
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMAGE_" + timeStamp + ".jpg" ;
        //File image = File.createTempFile(imageFileName, ".jpg", mTestImageFolder);
        File image = new File(ImageFolder, imageFileName);

        //RealmData
        //captureImageObjectにimagefileをセット
        //それをcapturedImageListをセット
        CapturedImageObject capturedImageObject = new CapturedImageObject();
        capturedImageObject.setFilePath(image.getPath());
        capturedImages.add(capturedImageObject);

        //元々あるデータ(getしたRealmオブジェクト)を再度セットすると初期化されるため、初回のみセットする
        if(mCaptNum == 0){
            measuredDateAndDataObject.setDirectoryPath(ImageFolder.getPath());
            measuredDateAndDataObject.setCapturedImages(capturedImages);
            dateAndDataList.add(measuredDateAndDataObject);
        }

        //ここでもう一度UserにDataAndDateListをセットすると初期化されるのでコメントアウト・・・
        //mUserObject.setMeasureDataAndDateList(dateAndDataList);

        //Realmの更新

        mRealm.copyToRealmOrUpdate(mUserObject);
        mRealm.commitTransaction();


//        File image = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
//                "JPEG_"+timeStamp+".jpg");
        Log.i("ImageSaver", "capt number = " + mCaptNum);
        mCaptNum += 1;
        return image;
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
    public void setCameraParam(int ISO, long ExposureTime, long DurationTime, float FocusDistance){
        mISO = ISO;
        mExposureTime = ExposureTime;
        mDurationTime = DurationTime;
        mFocusDistance = FocusDistance;
        mSbISO.setProgress(mISO);
        mSbExopsureTime.setProgress((int)mDurationTime);
    }

    //ある配列中にその数字があればtrueを返す
    private static boolean contains(int[] modes, int mode){
        if(modes == null){
            return false;
        }
        for(int i : modes){
            if(i==mode){
                return true;
            }
        }
        return false;
    }

    public void toast(String message){
        if(mToast != null){
            mToast.cancel();
        }

        mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        mToast.show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mToast.cancel();
            }
        }, 1000L);
    }


}

