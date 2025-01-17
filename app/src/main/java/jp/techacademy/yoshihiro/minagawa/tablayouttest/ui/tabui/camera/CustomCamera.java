package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.camera;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by ym on 2016/10/04.
 */

public class CustomCamera {

    private static final String TAG = "CustomCamera";

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int STATE_PREVIEW = 0x00;
    private static final int STATE_WAITING_LOCK = 0x01;
    private static final int STATE_WAITING_PRECAPTURE = 0x02;
    private static final int STATE_WAITING_NON_PRECAPTURE = 0x03;
    private static final int STATE_PICTURE_TAKEN = 0x04;
    private int mState = STATE_PREVIEW;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private CameraCaptureSession mCaptureSession = null;

    private CameraInterface mInterface;
    private CameraActivity mCameraActivity;

    private int mISO;
    private long mExposureTime;
    private long mFrameDuration;
    private float mFocusDistance;

    private boolean mIsAEState;
    private boolean mIsAFState;

    public void setInterface(CameraInterface param) {
        mInterface = param;
    }

    //stateCallback
    //@Override
    //onOpened : カメラデバイスと接続が完了
    //onDisconnected : カメラデバイスから切断された
    //onError : 回復不能な状況
    public final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {

        @Override
        //createCaptureSession(ここではcrateCameraPreviewSession())がコールできるようになる
        public void onOpened(CameraDevice cameraDevice) {
            // カメラが利用可能状態になった
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Log.e(TAG, "Camera StateCallback onError: Please Reboot Android OS");
        }

    };

    public boolean isLocked() throws InterruptedException {
        return mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS);
    }

    public void close() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mInterface.getSurfaceTextureFromTextureView();
            assert texture != null;

            // カメラ利用開始時にプレビューの設定を行う
            Size preview = mInterface.getPreviewSize();
            texture.setDefaultBufferSize(preview.getWidth(), preview.getHeight());

            // 画面への表示用のSurfaceを作成する
            //(SurfaceTexture)textureを引数として新しいSurfaceを作成する
            Surface surface = new Surface(texture);

            //Sessionに引き渡すRequestの生成
            //CameraDeviceでCapture"Request"を作成
            mPreviewRequestBuilder
                    = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // プレビュー画面のため、キャプチャセッションを作成する
            //CameraDeviceでCapture"Session"を作成
            Surface imageRenderSurface = mInterface.getImageRenderSurface();
            mCameraDevice.createCaptureSession(Arrays.asList(surface, imageRenderSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // プレビュー準備が完了したのでカメラのAF,AE制御を指定する
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // プレビューがぼやけては困るのでオートフォーカスを利用する
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // 露出、フラッシュは自動モードを仕様する
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

                                //テスト
                                //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                //        CaptureRequest.CONTROL_AE_MODE_OFF);


                                //パラメーター(AF,AM,Flash)を変えたのでBuilderからRequestを更新
                                mPreviewRequest = mPreviewRequestBuilder.build();

                                // カメラプレビューを開始する（ここでは開始要求のみ）
                                //setRepeatingRequestでsessionに要求を出す。
                                //・setRepeatingRequestにより、カメラデバイスはできる限りの最大のレートでイメージを取得する
                                //・Repeating requestはcaptureを毎回行わなくても、プレビューや他フレームのstreamを継続的に取得するシンプルな方法
                                //・Repeating requestはcapture, captureBurstに比べて低優先
                                //・Repeating requestを止めるにはstopRepeatingを使う。
                                //・abortCapturesを呼べば、Repeating requestsも止まる。
                                //・新しいRepeating requestを設定すると、前のrequest、burst、setRepeatingBurstで設定されたリクエスト
                                //  は置き換えられるが、現在の処理中のburstについては、処理が終わってから新しいリクエストが使われる。
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mInterface.getBackgroundHandler());
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "CameraCaptureSession onConfigureFailed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request,
                                        CaptureResult partialResult) {
            // キャプチャの進捗状況（随時呼び出される）
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request,
                                       TotalCaptureResult result) {
            // キャプチャの完了（プレビューの場合、プレビュー状態が継続）
            process(result);
        }


        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    //Log.e("mTestCustomCamera", "SensorSensitivity = " + result.get(CaptureResult.SENSOR_SENSITIVITY));
                    //Log.e("mTestCustomCamera", "ExposureTime = " + result.get(CaptureResult.SENSOR_EXPOSURE_TIME));
                    //Log.e("mTestCustomCamera", "FrameDuration = " + result.get(CaptureResult.SENSOR_FRAME_DURATION));
                    mISO = result.get(CaptureResult.SENSOR_SENSITIVITY);
                    mExposureTime = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                    mFrameDuration = result.get(CaptureResult.SENSOR_FRAME_DURATION);
                    mFocusDistance = result.get(CaptureResult.LENS_FOCUS_DISTANCE);
                    //Log.e("mTestCustomCamera", "progress Focus Distance = " + result.get(CaptureResult.LENS_FOCUS_DISTANCE));

                    break;
                }
                case STATE_WAITING_LOCK: {
                    // 焦点が合った時に静止画を撮影する（AF）
                    Log.d("mTestCustomCamera", "State Waiting Lock");
                    Log.d("mTestCustomCamera", "AfState =" + result.get(CaptureResult.CONTROL_AF_STATE));
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        Log.d("mTestCustomCamera", "afState == null");
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        // CONTROL_AE_STATE がnullのデバイスがある
                        Log.d("mTestCustomCamera", "CONTROL_AE_STATE == null?");
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            Log.d("mTestCustomCamera", "State Picture Taken");
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    Log.d("mTestCustomCamera", "all through");
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    Log.d("mTestCustomCamera", "State Waiting precapture");
                    // キャプチャ準備中
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||  // CONTROL_AE_STATE がnullのデバイスがある
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE がnullのデバイスがある
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

    };


    //静止画の撮影を行う
    private void captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                return;
            }
            // 静止画の撮影を開始する
            final CaptureRequest.Builder captureBuilder =
                        mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);


            captureBuilder.addTarget(mInterface.getImageRenderSurface());
            // 静止画の撮影モードを指定（AF,AE）
            // 露光のパラメーターに変更があった場合にはここに記述?
            // White balanceもここに書く？


            if(mIsAEState == true) {
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
            }else if(mIsAEState == false){
                //AEが外れている時
                //イメージがキャプチャーされている間にフォーカスされることは望まない
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CameraMetadata.CONTROL_AF_MODE_OFF);
                captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_IDLE);


                //AEをOFFにして各パラメーター値(ISO, ExposureTime, FrameDuration)を入れる
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CameraMetadata.CONTROL_AE_MODE_OFF);
                captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, mISO);
                captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mExposureTime);
                captureBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, mFrameDuration);

            }
            // 現在のカメラの向きを指定する（0～270度）
            int rotation = mInterface.getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));


            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session,
                                               CaptureRequest request,
                                               TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    // 静止画撮影が完了した時に呼ばれるコールバック
                    Log.i(TAG, "onCaptureCompleted Picture Saved");
                    // プレビュー用の設定に戻す
                    unlockFocus();
                }
            };

            //撮影するJPEGの質を100まで上げる
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte)100);

            mCaptureSession.stopRepeating(); // プレビューを一旦停止する
            // 静止画を撮影する（captureBuilder）
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            // 静止画の撮影準備：自動露光の準備を開始する
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // 準備完了をまつため、mCaptureCallbackへ進行状況を通知する
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mInterface.getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //AEとAFが切り替わったときにCameraActivityから呼び出される
    public void changeCameraCondition(boolean isAFState, boolean isAEState){
        if (mCaptureSession != null) {
            try {

                mIsAFState = isAFState;
                mIsAEState = isAEState;

                //AFState
                if(mIsAFState) {
                    // プレビューがぼやけては困るのでオートフォーカスを利用する
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                }else{
                    //オートフォーカスを切る前のパラメーターを渡しておく
                    mCameraActivity.setCameraParam(mISO, mExposureTime, mFrameDuration, mFocusDistance);
                    changeExposureParam(mISO, mExposureTime, mFrameDuration, mFocusDistance);
                }

                //AEState
                if(mIsAEState){
                    // 露出、フラッシュは自動モードを仕様する
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                }else{
                    //CameraActivityにAEを切る前のパラメーターを渡しておく
                    mCameraActivity.setCameraParam(mISO, mExposureTime, mFrameDuration, mFocusDistance);
                    changeExposureParam(mISO, mExposureTime, mFrameDuration, mFocusDistance);
                }


                //ホワイトバランス
                //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
                //        CaptureRequest.CONTROL_AWB_MODE_AUTO);


                //パラメーター(AF,AM,Flash)を変えたのでBuilderからRequestを更新
                mPreviewRequest = mPreviewRequestBuilder.build();

                // カメラプレビューを開始する（ここでは開始要求のみ）
                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                        mCaptureCallback, mInterface.getBackgroundHandler());
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }


//    //AEがONになったときCameraActivityから呼び出される
//    public void changeAEOFF(){
//        if (mCaptureSession != null) {
//            mCameraActivity.setCameraParam(mISO, mExposureTime, mFrameDuration);
//            changeExposureParam(mISO, mExposureTime, mFrameDuration, 0);
//        }
//    }

    public void changeExposureParam(int ISO, long exposureTime, long frameDuration, float focusDistance){
        if (mCaptureSession != null) {

            mISO = ISO;
            mExposureTime = exposureTime;
            mFrameDuration = frameDuration;
            mFocusDistance = focusDistance;


            try{
                if(!mIsAFState) {
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_OFF);
                    mPreviewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, mFocusDistance);
                    Log.d("mCustomCamera", "AF parametaer was changed");
                }

                if(!mIsAEState) {
                    //AEをOFFにして各パラメーター値(ISO, ExposureTime, FrameDuration)を入れる
                    Log.d("mCustomCamera", "AE parametaer was changed");
                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                            CaptureRequest.CONTROL_AE_MODE_OFF);
                    mPreviewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, mISO);
                    mPreviewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mExposureTime);
                    mPreviewRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, mFrameDuration);
                    //パラメーター値(ISO, ExposureTime, FrameDuration)をマニュアルで全て変更したので、

                }

                //リクエストをbuildする。
                mPreviewRequest = mPreviewRequestBuilder.build();
                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                        mInterface.getBackgroundHandler());
            }catch(CameraAccessException e){
                e.printStackTrace();
            }
        }
    }


    //静止画を撮影準備を始める
    public void takePicture(boolean AEState) {
        if (mCaptureSession != null) {
            mIsAEState = AEState;
            lockFocus();
        }
    }

    //focusをロックする
    private void lockFocus() {
        try {
            // 静止画を撮影するため、AFをロックする
            //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
            //        CameraMetadata.CONTROL_AF_TRIGGER_START);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                            CameraMetadata.CONTROL_AF_TRIGGER_START);
            // captureを実行する。AFロック完了通知を受け取るため、mCaptureCallbackへ進行状況を通知する
            mState = STATE_WAITING_LOCK;

            //AWBも入れる161014
            //mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE,
            //        CaptureRequest.CONTROL_AWB_MODE_AUTO);

            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mInterface.getBackgroundHandler());

            if(mIsAEState == false){
                mState = STATE_PICTURE_TAKEN;
                captureStillPicture();
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        try {
            // AFのロックを解除する（トリガーをキャンセルする）
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // AFトリガーのキャンセルを実行する
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mInterface.getBackgroundHandler());
            // プレビューを継続するためsetRepeatingRequestメソッドを実行する
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mInterface.getBackgroundHandler());
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void setCameraActivity(CameraActivity cameraActivity){
        mCameraActivity = cameraActivity;
    }

    public int getISO(){
        return mISO;
    }

    public long getExposureTime(){
        return mExposureTime;
    }

}
