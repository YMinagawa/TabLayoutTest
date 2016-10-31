package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.apptik.widget.MultiSlider;
import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.calculator.MakeHistogramDistribution;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.calculator.MeanIntensityCalcTask;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.CapturedImageObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.MeasuredDateAndDataObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.UserObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.CustomImageView;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.CustomItemDecoration;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.CustomRecyclerItemClickListener;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.CustomViewPager;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.analyzedata.ImageDataListRecyclerAdapter;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.analyzedata.MeasuredDateListRecycleAdapter;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.camera.CameraActivity;

import static android.graphics.BitmapFactory.decodeFile;

public class TabMainActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener,
        ViewPager.OnTouchListener, PageFragment.OnFragmentInteractionListener {

    static{System.loadLibrary("opencv_java3");}

    //User
    private UserObject mUserObject;

    //idをインテントで入手するためのメンバ変数
    int mId;

    //Framelayout (各Viewを乗っける)
    FrameLayout mFL_Camera;
    FrameLayout mFL_DataAnalysis;
    FrameLayout mFL_History;

    //ViewPager
    ViewPager mViewPager;
    CustomViewPager mCustomViewPager;

    //各View
    //Camera
    View mView_camera_config;

    //DataAnalysis
    //1ページ 日付選択ページ
    View mView_select_measuredDate;
    RecyclerView mSelectMeasureDateRecyclerView;
    RecyclerView.LayoutManager mSelectMeasureDateLayoutManager;
    RecyclerView.Adapter mSelectMeasureDateAdapter;
    //2ページ 画像選択ページ
    View mView_select_imageData;
    RecyclerView mSelectImageRecyclerView;
    RecyclerView.LayoutManager mSelectImageLayoutManager;
    RecyclerView.Adapter mSelectImageAdapter;
    FloatingActionButton mFab_plus, mFab_analysis, mFab_delete;
    Animation mFabOpen, mFabClose, mFabRClockwise,  mFabRanticlockwise;
    boolean isOpen =false;
    boolean isGrayThresh = false;
    //3ページ 画像処理ページ
    int mAnalyzeState;
    int NONE = 0;
    int GRAY_SCALE = 1;
    int THRESHOLD = 2;
    int FIND_CONTOUR = 3;

    int mDatePosition;
    int[] mCheckedImageNumArray;
    int mDisplayedImageNum;
    int mDetectMinSize = 10;
    int mDetectMaxSize = 1000;
    int mDrawMinSize = 11;
    int mDrawMaxSize = 999;
    TextView mTextView_SizeRange;
    Mat mSrcMat;
    Mat mGrayImageMat;
    Mat mThreshImageMat;
    Mat mMaskedImageMat;
    Mat mContourMat;
    View mView_analyze_imageData;
    CustomImageView mCustomImageView;
    SeekBar mSb_threshold;
    //SeekBar mSb_size;
    MultiSlider mMS_size;

    double[] mContourAreaArray;
    List<MatOfPoint> mAllDetectedContours;
    ArrayList<Rect> mRectList;
    ArrayList<Rect> mDrawnRectList;

    //4ページ 画像解析結果
    View mView_analyze_result;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.d("mTabMainAct", "OpenCV 読み込み成功");
                }break;
                default:
                {
                    Log.d("mTabmainAct", "OpenCV 読み込み失敗");
                }break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //インテントからidを入手してどのユーザーかを決定する
        Intent intent = getIntent();
        mId = intent.getIntExtra("id", 0);

        //ここで選択されたユーザーオブジェクトの入手を行い、サブタイトルにセットする。
        //更に、中のMeasureDateAndDataListからCaptureImageObjectListを取り出す
        //Realm.getDefalultInstance()をしたら必ずRealm.closeする
        Realm realm = Realm.getDefaultInstance();
        RealmResults<UserObject> userRealmResults = realm.where(UserObject.class).equalTo("id", mId).findAll();
        mUserObject = userRealmResults.get(0);
        RealmList<MeasuredDateAndDataObject> dateAndDataList = mUserObject.getMeasuredDateAndDataList();
        //各ファイルパスから画像ファイルが存在するか確認する。ファイルが存在しなければ削除する。
        for(int i = 0; i < dateAndDataList.size(); i++){
            MeasuredDateAndDataObject measuredDateAndDataObject = dateAndDataList.get(i);

            RealmList<CapturedImageObject> captureImageList = measuredDateAndDataObject.getCapturedImages();

            for(int j = 0; j < captureImageList.size(); j++){
                CapturedImageObject capturedImage = captureImageList.get(j);
                //Log.d("mTabMainActivity", capturedImage.getFilePath());
                File file = new File(capturedImage.getFilePath());
                if(!file.exists()){
                    //画像ファイルが無ければ削除する
                    captureImageList.remove(capturedImage);
                }
            }
        }

        realm.beginTransaction();
        realm.copyToRealmOrUpdate(mUserObject);
        realm.commitTransaction();

        realm.close();

        Log.d("mTestTabMainActivity", mUserObject.getName());
        Log.d("mTestTabMainActivity", mUserObject.getMeasuredDateAndDataList().get(0).getMeasuredDate().toString());

        //ここからレイアウトの設定
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        toolbar.setTitle("TabLayoutTest");
        toolbar.setSubtitle(mUserObject.getName());
        setSupportActionBar(toolbar);

        //xmlからTabLayoutの取得
        TabLayout tabLayout = (TabLayout)findViewById(R.id.tabs);
        //xmlからViewPagerの取得
        //mViewPager = (ViewPager)findViewById(R.id.pager);
        mCustomViewPager = (CustomViewPager)findViewById(R.id.customViewPager);
        //ページタイトル配列
        final String[] pageTitle = {"CAMERA", "ANALYZE DATA", "HISTORY"};

        //カメラページを作る
        createCameraPage();

        //DataAnalysis用のページを作る
        createDataAnalysisFirstPage();

        //History用のページ（未実装)
        mFL_History = new FrameLayout(this);

        //作成したページをviewリストに追加
        List<View> viewList = new ArrayList<>();
        viewList.add(mFL_Camera);
        viewList.add(mFL_DataAnalysis);
        viewList.add(mFL_History);

        //作成したページアダプターにListを渡し、
        //アダプターをviewPagerにセット
        CustomPagerAdapter cpa = new CustomPagerAdapter(viewList, pageTitle);
        //mViewPager.setAdapter(cpa);
        mCustomViewPager.setAdapter(cpa);
        //mViewPager.addOnPageChangeListener(this);
        mCustomViewPager.addOnPageChangeListener(this);
        //ViewPagerをTabLayoutを設定
        //tabLayout.setupWithViewPager(mViewPager);
        tabLayout.setupWithViewPager(mCustomViewPager);
    }

    //カメラタブのカメラページの設定
    private void createCameraPage(){

        //フラグメントの設定を行い、そこにViewを加える
        //その後、View内のアイテムのリスナーの設定を行う
        mFL_Camera = new FrameLayout(this);
        mView_camera_config = getLayoutInflater().inflate(R.layout.view_camera_presetting, null);
        mFL_Camera.addView(mView_camera_config);
        FloatingActionButton fab_camera = (FloatingActionButton)mView_camera_config.findViewById(R.id.fab_camera);
        fab_camera.setImageResource(R.drawable.fab_camera);
        fab_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TabMainActivity.this, CameraActivity.class);
                intent.putExtra("id", mId);
                startActivity(intent);
            }
        });

        //スピナーの文字サイズを変えるための設定(Adapterを用意してspinnerにセットする
        ArrayAdapter<String> adapterHour = new ArrayAdapter<String>(TabMainActivity.this, R.layout.spinner_item_design_for_cam, this.getResources().getStringArray(R.array.hour_list));
        ArrayAdapter<String> adapterMin = new ArrayAdapter<String>(TabMainActivity.this, R.layout.spinner_item_design_for_cam, this.getResources().getStringArray(R.array.min_list));

        Spinner spinnerHour = (Spinner)mView_camera_config.findViewById(R.id.spinner_hour);
        Spinner spinnerMin = (Spinner)mView_camera_config.findViewById(R.id.spinner_min);

        spinnerHour.setAdapter(adapterHour);
        spinnerMin.setAdapter(adapterMin);

        //スピナーのアイテムが選択されたときの動作設定
        spinnerHour.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //スピナーでは呼ばれないが、消せないので「おまじない」
            }
        });

        spinnerMin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //スピナーでは呼ばれないが、消せないので「おまじない」
            }
        });
    }

    //AnalysisDataのページ設定
    public void createDataAnalysisFirstPage(){

        mFL_DataAnalysis = new FrameLayout(this);

        //1ページ目の設定 : データを測定した日付を選ぶページ
        mView_select_measuredDate = getLayoutInflater().inflate(R.layout.view_select_measureddate, null);
        mFL_DataAnalysis.addView(mView_select_measuredDate);

        //1ページ目 : RecycleViewの設定
        mSelectMeasureDateRecyclerView = (RecyclerView)mView_select_measuredDate.findViewById(R.id.recyclerView_selectcapdate);
        mSelectMeasureDateLayoutManager = new LinearLayoutManager(this);
        mSelectMeasureDateRecyclerView.setLayoutManager(mSelectMeasureDateLayoutManager);
        //1ページ目 : RecycleViewにItemDecorationをセットする
        mSelectMeasureDateRecyclerView.addItemDecoration(new CustomItemDecoration(this));

        mSelectMeasureDateAdapter = new MeasuredDateListRecycleAdapter(mUserObject.getMeasuredDateAndDataList());
        mSelectMeasureDateRecyclerView.setAdapter(mSelectMeasureDateAdapter);

        //1ページ目 : 独自に作成したRecycleItemOnClickListenerを実装する
        //          日付がクリックされたら、その日付に対応するViewを作成する。
        mSelectMeasureDateRecyclerView.addOnItemTouchListener(
                new CustomRecyclerItemClickListener(this, mSelectMeasureDateRecyclerView, new CustomRecyclerItemClickListener.OnItemClickListener(){
                    @Override
                    public void onItemClick(View view, int position) {
                        Log.d("mTest", "measureddate normal click");
                        if(mFL_DataAnalysis.getChildCount() < 2) {
                            createDataAnalysisSecondPage(position);
                        }
                    }

                    @Override
                    public void onItemLongClick(View view, int position) {

                    }
                })
        );
    }

    public void createDataAnalysisSecondPage(final int date_position){

        //2ページ目の設定 : 解析する画像を選ぶ(実装は1ページ目のアイテムリスナー)
        mView_select_imageData = getLayoutInflater().inflate(R.layout.view_select_imagedata, null);

        //2ページ目 : RecycleViewの設定
        mSelectImageRecyclerView = (RecyclerView)mView_select_imageData.findViewById(R.id.recyclerView_selectimagedata);
        mSelectImageLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mSelectImageRecyclerView.setHasFixedSize(true);
        mSelectImageRecyclerView.setLayoutManager(mSelectImageLayoutManager);

        //2ページ目：RecyclerViewのアイテム毎の線が入るようにする
        //mSelectImageRecyclerView.addItemDecoration(new CustomItemDecoration(this));

        //2ページ目: ユーザーからデータリストを取り出し、1ページ目でタップされた日時のCaptureImageObject
        // のRealmListをアダプターに引き渡す
        RealmList<MeasuredDateAndDataObject> dateAndDataList = mUserObject.getMeasuredDateAndDataList();
        RealmList<CapturedImageObject> captureImageList = dateAndDataList.get(date_position).getCapturedImages();

        mSelectImageAdapter = new ImageDataListRecyclerAdapter(captureImageList, this);
        mSelectImageRecyclerView.setAdapter(mSelectImageAdapter);

        //2ページ目：RecycleViewの下にDate, ISO, ExposureTimeを表示
        TextView textViewDate = (TextView)mView_select_imageData.findViewById(R.id.textView_measureddate);
        TextView textViewISO = (TextView)mView_select_imageData.findViewById(R.id.textView_iso);
        TextView textViewExpTime = (TextView)mView_select_imageData.findViewById(R.id.textView_exptime);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd  HH:mm");
        textViewDate.setText("Measured Date : " + sdf.format(dateAndDataList.get(date_position).getMeasuredDate()));
        textViewISO.setText("ISO : " + dateAndDataList.get(date_position).getISO());
        textViewExpTime.setText("Exposure Time : " + dateAndDataList.get(date_position).getExposureTime() + " ms");

        //2ページ目：解析に移行するためのFloating Action Button

        mFab_plus = (FloatingActionButton)mView_select_imageData.findViewById(R.id.fab_plus);
        mFab_analysis = (FloatingActionButton)mView_select_imageData.findViewById(R.id.fab_analysis);
        mFab_analysis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDataAnalysisThirdPage(date_position);
            }
        });
        mFab_delete = (FloatingActionButton)mView_select_imageData.findViewById(R.id.fab_delete);
        mFabOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        mFabClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
        mFabRClockwise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_clockwise);
        mFabRanticlockwise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_anticlockwise);

        //mFab_plusを押した時のアニメーション(deleteと解析が開く)
        mFab_plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(isOpen){
                    mFab_delete.startAnimation(mFabClose);
                    mFab_delete.setClickable(false);
                    mFab_analysis.startAnimation(mFabClose);
                    mFab_analysis.setClickable(false);
                    mFab_plus.startAnimation(mFabRanticlockwise);
                    isOpen = false;
                }else{
                    mFab_delete.startAnimation(mFabOpen);
                    mFab_delete.setClickable(true);
                    mFab_analysis.startAnimation(mFabOpen);
                    mFab_analysis.setClickable(true);
                    mFab_plus.startAnimation(mFabRClockwise);
                    isOpen = true;
                }
            }
        });

        mFL_DataAnalysis.removeView(mView_select_measuredDate);
        mFL_DataAnalysis.addView(mView_select_imageData);
    }

    //3ページ目  画像を解析(Gray, Threshold, Erode, FindContour等)
    public void createDataAnalysisThirdPage(int date_position){

        //3ページ目　Viewの設定
        mView_analyze_imageData = getLayoutInflater().inflate(R.layout.view_analyze_imagedata, null);
        //mImageView = (ImageView)mView_analyze_imageData.findViewById(R.id.imageView_analyzeimage);
        mCustomImageView = (CustomImageView)mView_analyze_imageData.findViewById(R.id.customImageView_analyzeimage);
        RealmList<MeasuredDateAndDataObject> dateAndDataList = mUserObject.getMeasuredDateAndDataList();
        RealmList<CapturedImageObject> captureImageList = dateAndDataList.get(date_position).getCapturedImages();
        mCustomViewPager.setFrameLayout(mFL_DataAnalysis, mView_analyze_imageData);
        mTextView_SizeRange = (TextView)mView_analyze_imageData.findViewById(R.id.textView_sizerange);
        mDatePosition = date_position;

        mAnalyzeState = NONE;
        //imageViewにチェックされている画像のデータを入れていく？
        //チェックされている画像がどれなのか数字が必要
        File imageFile = null;
        int checkedImageTotalNumber = 0;
        int[] checkedImageNumArray = new int[captureImageList.size()];
        for(int i = 0; i < captureImageList.size(); i++){
            CapturedImageObject capturedImage = captureImageList.get(i);
            if(capturedImage.getChecked() == true){
                if(checkedImageTotalNumber == 0) {
                    imageFile = new File(capturedImage.getFilePath());
                }
                checkedImageNumArray[checkedImageTotalNumber] = i;
                checkedImageTotalNumber++;
            }
        }

        mCheckedImageNumArray = new int[checkedImageTotalNumber];
        mCheckedImageNumArray = Arrays.copyOf(checkedImageNumArray, checkedImageTotalNumber);
        mDisplayedImageNum = 0;
        mFL_DataAnalysis.getParent().requestDisallowInterceptTouchEvent(true);
        TextView textViewPageNum = (TextView)mView_analyze_imageData.findViewById(R.id.textView_imagepages);
        textViewPageNum.setText((mDisplayedImageNum+1) + "/" + mCheckedImageNumArray.length);

        //イメージファイルがnullかどうかでチェックされたイメージがあるかを確認する
        if(imageFile != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            //とりあえずinSampleSizeは1で。
            options.inJustDecodeBounds = false;
            options.inSampleSize = 1;
            Bitmap analyzedImageBitmap = decodeFile(imageFile.getAbsolutePath(), options);
            mSrcMat = new Mat();
            Utils.bitmapToMat(analyzedImageBitmap, mSrcMat);
            //mImageView.setImageBitmap(mAnalyzedImageBitmap);
            mCustomImageView.setImageBitmap(analyzedImageBitmap);

            mFL_DataAnalysis.removeView(mView_select_imageData);
            mFL_DataAnalysis.addView(mView_analyze_imageData);
        }else{
            Toast.makeText(this, "Please check one image at least !", Toast.LENGTH_SHORT).show();
        }

        //次のイメージに進めるButton設定
        Button btnNextImage = (Button)mView_analyze_imageData.findViewById(R.id.btn_nextpage);
        btnNextImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mDisplayedImageNum += 1;
                if(mDisplayedImageNum == mCheckedImageNumArray.length){
                    mDisplayedImageNum = 0;
                }
                TextView textViewPageNum = (TextView)mView_analyze_imageData.findViewById(R.id.textView_imagepages);
                textViewPageNum.setText((mDisplayedImageNum+1) + "/" + mCheckedImageNumArray.length);
                //ImageViewにSetしたイメージを変更する
                changeDisplayImage();
            }
        });

        //前のイメージに戻るButton設定
        Button btnPreviousImage = (Button)mView_analyze_imageData.findViewById(R.id.btn_previouspage);
        btnPreviousImage.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mDisplayedImageNum -= 1;
                if (mDisplayedImageNum == -1) {
                    mDisplayedImageNum = mCheckedImageNumArray.length - 1;
                }
                TextView textViewPageNum = (TextView) mView_analyze_imageData.findViewById(R.id.textView_imagepages);
                textViewPageNum.setText((mDisplayedImageNum + 1) + "/" + mCheckedImageNumArray.length);
                //ImageViewにSetしたイメージを変更する
                changeDisplayImage();
            }
        });

        //画像を元に戻すButton設定
        Button btnReset = (Button)mView_analyze_imageData.findViewById(R.id.btn_reset);
        btnReset.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                mCustomImageView.setImageBitmap(null);
                Bitmap srcImageBitmap = Bitmap.createBitmap(mSrcMat.width(), mSrcMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mSrcMat, srcImageBitmap);
                mCustomImageView.setImageBitmap(srcImageBitmap);
            }
        });

        //グレイスケール用のButtonの設定
        Button btnGray = (Button)mView_analyze_imageData.findViewById(R.id.btn_gray);
        btnGray.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAnalyzeState = GRAY_SCALE;
                setGlayScaleImage();
            }
        });

//        //反転(inveret)用のButtonの設定
//        Button btnInvert = (Button)mView_analyze_imageData.findViewById(R.id.btn_invert);
//        btnInvert.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(mGrayImageBitmap != null){
//                    System.loadLibrary("opencv_java3");
//                    Mat srcMat = new Mat();
//                    Utils.bitmapToMat(mAnalyzedImageBitmap, srcMat);
//                    Mat grayMat = new Mat();
//                    Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGB2GRAY);
//                    mGrayImageBitmap = Bitmap.createBitmap(grayMat.width(), grayMat.height(), Bitmap.Config.ARGB_8888);
//                    //白黒反転
//                    Core.bitwise_not(grayMat, grayMat);
//                    Utils.matToBitmap(grayMat, mGrayImageBitmap);
//                    //mImageView.setImageBitmap(mGrayImageBitmap);
//                    mCustomImageView.setImageBitmap(mGrayImageBitmap);
//                    srcMat.release();
//                    grayMat.release();
//                }
//
//            }
//        });

        //ThresholdのSeekBarの設定
        //Gray and Color 両対応
        mSb_threshold = (SeekBar)mView_analyze_imageData.findViewById(R.id.seekBar_thresh);
        mSb_threshold.setEnabled(false);
        final View toastlayout = getLayoutInflater().inflate(R.layout.toast_layout, null);
        final TextView toastText = (TextView)toastlayout.findViewById(R.id.textView_toast);

        mSb_threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mAnalyzeState = THRESHOLD;
                //mThreshImageBitmap = mGrayImageBitmap;
                //mImageView.setImageBitmap(mGrayImageBitmap);
                toastText.setText("Threshold " + progress);
                Log.d("mTabMainActivity", "thresh = " + progress);
                //まずグレイスケール用のmatを用意し、

                //プログレスバーの値を閾値として設定する。
                //threshold用のmatを用意して、grayMatにthresholdを適用したものを入れる。
                setThresholdImage();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Threshold用のButtonの設定
        Button btnThreshold = (Button)mView_analyze_imageData.findViewById(R.id.btn_thresh);
        btnThreshold.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                isGrayThresh = false;
                if(mGrayImageMat == null && mThreshImageMat == null){
                    Toast.makeText(TabMainActivity.this, "Please change grayscale first", Toast.LENGTH_SHORT).show();
                }else if(mGrayImageMat !=null && mThreshImageMat == null){
                    //mImageView.setImageBitmap(mGrayImageBitmap);
                    Bitmap srcImageBitmap = Bitmap.createBitmap(mSrcMat.width(), mSrcMat.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mSrcMat, srcImageBitmap);
                    mCustomImageView.setImageBitmap(srcImageBitmap);
                    mSb_threshold.setEnabled(true);
                    mMS_size.setEnabled(false);
                }else if(mMaskedImageMat != null){
                    //mImageView.setImageBitmap(mThreshImageBitmap);
                    Bitmap maskedImageBitmap = Bitmap.createBitmap(mMaskedImageMat.width(), mMaskedImageMat.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mMaskedImageMat, maskedImageBitmap);
                    mCustomImageView.setImageBitmap(maskedImageBitmap);
                    mSb_threshold.setEnabled(true);
                    mMS_size.setEnabled(false);
                }
            }
        });

        //GrayThreshold用のButtonの設定
        Button btnGrayThreshold = (Button)mView_analyze_imageData.findViewById(R.id.btn_graythresh);
        btnGrayThreshold.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                isGrayThresh = true;
                if(mGrayImageMat == null && mThreshImageMat == null){
                    Toast.makeText(TabMainActivity.this, "Please change grayscale first", Toast.LENGTH_SHORT).show();
                }else if(mGrayImageMat !=null && mThreshImageMat == null){
                    //mImageView.setImageBitmap(mGrayImageBitmap);
                    Bitmap grayImageBitmap = Bitmap.createBitmap(mGrayImageMat.width(), mGrayImageMat.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mGrayImageMat, grayImageBitmap);
                    mCustomImageView.setImageBitmap(grayImageBitmap);
                    mSb_threshold.setEnabled(true);
                    //mSb_size.setEnabled(false);
                    mMS_size.setEnabled(false);
                }else if(mThreshImageMat != null){
                    //mImageView.setImageBitmap(mThreshImageBitmap);
                    Bitmap maskedImageBitmap = Bitmap.createBitmap(mMaskedImageMat.width(), mMaskedImageMat.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mMaskedImageMat, maskedImageBitmap);
                    mCustomImageView.setImageBitmap(maskedImageBitmap);
                    mSb_threshold.setEnabled(true);
                    //mSb_size.setEnabled(false);
                    mMS_size.setEnabled(false);
                }
            }
        });

        //erode用のButtonの設定
        Button btnErode = (Button)mView_analyze_imageData.findViewById(R.id.btn_erode);
        btnErode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mThreshImageMat != null) {
                    mMaskedImageMat = new Mat();

                    Imgproc.erode(mThreshImageMat, mThreshImageMat, new Mat());

                    if(isGrayThresh){
                        mGrayImageMat.copyTo(mMaskedImageMat, mThreshImageMat);
                    }else{
                        mSrcMat.copyTo(mMaskedImageMat, mThreshImageMat);
                    }

                    Bitmap erodedImageBitmap = Bitmap.createBitmap(mMaskedImageMat.width(), mMaskedImageMat.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mMaskedImageMat, erodedImageBitmap);
                    mCustomImageView.setImageBitmap(erodedImageBitmap);
                }
            }
        });

        //FindContours用のボタン
        Button btnFindContours = (Button)mView_analyze_imageData.findViewById(R.id.btn_findcontour);
        btnFindContours.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mThreshImageMat != null && mMaskedImageMat != null) {
                    mAnalyzeState = FIND_CONTOUR;
                    setFindContourImage(true);
                }
            }
        });

        //SizeでFilterをかけるMultiSliderの設定
        mMS_size = (MultiSlider)mView_analyze_imageData.findViewById(R.id.multislider_size);
        mMS_size.setMin(0);
        mMS_size.setEnabled(false);
        mMS_size.setOnThumbValueChangeListener(new MultiSlider.OnThumbValueChangeListener(){
            @Override
            public void onValueChanged(MultiSlider multiSlider, MultiSlider.Thumb thumb, int thumbIndex, int value) {
                if(thumbIndex == 0){
                    mDrawMinSize = value;
                    mTextView_SizeRange.setText("Min : " + mDrawMinSize + " - Max : " + mDrawMaxSize);
                    Log.d("mTabMainActivity" , "MinChange : " + value );
                }else if(thumbIndex == 1){
                    mDrawMaxSize = value;
                    mTextView_SizeRange.setText("Min : " + mDrawMinSize + " - Max : " + mDrawMaxSize);
                    Log.d("mTabMainActivity", "MaxChange : " + value);
                }
                setFindContourImage(false);
            }
        });

        //Rect中のピクセル値の平均値の並列計算ボタン
        Button btnCalc = (Button)mView_analyze_imageData.findViewById(R.id.btn_calcmean);
        btnCalc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                double[] calcResultsArray = new double[mDrawnRectList.size()];
                if(mAnalyzeState == FIND_CONTOUR && mDrawnRectList != null) {
                    int threadNumber = 1;
                    ExecutorService executor = Executors.newFixedThreadPool(threadNumber);
                    //タスクリストを作製する
                    List<Callable<Double>> tasks = new ArrayList<Callable<Double>>();

                    //for (int i = 0; i < 2; i++) {
                    for (int i = 0; i < mDrawnRectList.size(); i++) {
                        tasks.add(new MeanIntensityCalcTask(mDrawnRectList.get(i)));
                    }

                    MeanIntensityCalcTask.setImageMat(mContourMat);

                    try{
                        //並列計算実行
                        List<Future<Double>> futures;

                        try{
                            futures = executor.invokeAll(tasks);
                        }catch(InterruptedException e){
                            Log.e("mTabMainActivity", "e = " + e);
                            return;
                        }

                        //結果をresultsに入れる
                        for(int i = 0; i < mDrawnRectList.size(); i++){
                            try{
                                calcResultsArray[i] = (futures.get(i)).get();
                            }catch(Exception e){
                                Log.e("mTabMainActivity", "e = " + e);
                            }
                        }

                    }finally{
                        //終了処理
                        if(executor != null){
                            executor.shutdown();
                        }
                    }
                }

                for(int i = 0; i < mRectList.size();  i++){
                    Log.d("mTabMainActivity", "calcResults[" + i + "] = " + calcResultsArray[i]);
                }
                createDataAnalysisForthPage(calcResultsArray);

            }
        });
    }

    //GlayScale化した画像をセットする
    private void setGlayScaleImage(){
        mGrayImageMat = new Mat();
        //ソースの画像をcvtColor(convertColor)でRGBからGRAYにする
        Imgproc.cvtColor(mSrcMat, mGrayImageMat, Imgproc.COLOR_RGB2GRAY);
        Bitmap displayImageBitmap = Bitmap.createBitmap(mGrayImageMat.width(), mGrayImageMat.height(), Bitmap.Config.ARGB_8888);
        Imgproc.cvtColor(mSrcMat, mGrayImageMat, Imgproc.COLOR_RGB2GRAY);
        Utils.matToBitmap(mGrayImageMat, displayImageBitmap);
        mCustomImageView.setImageBitmap(displayImageBitmap);

    }

    //Thresholdをかけた画像をセットする
    private void setThresholdImage(){
        mThreshImageMat = new Mat();
        Imgproc.threshold(mGrayImageMat, mThreshImageMat, mSb_threshold.getProgress(), 255, Imgproc.THRESH_BINARY);

        mMaskedImageMat = new Mat();
        if(isGrayThresh){
            mGrayImageMat.copyTo(mMaskedImageMat, mThreshImageMat);
        }else{
            mSrcMat.copyTo(mMaskedImageMat, mThreshImageMat);
        }

        Bitmap displayImageBitmap = Bitmap.createBitmap(mGrayImageMat.width(), mGrayImageMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mMaskedImageMat, displayImageBitmap);
        mCustomImageView.setImageBitmap(displayImageBitmap);
    }

    //FindContourをした画像をセットする
    private void setFindContourImage(boolean isNew){

        //画像が新しい(別の画像に切り替わった)場合には
        //もう一度、FindContourをかける
        if(isNew == true){
            //Thresholdのbarを使えなくし、Sizeのseekbarを使えるようにする
            mSb_threshold.setEnabled(false);
            //mSb_size.setEnabled(true);
            mMS_size.setEnabled(true);

            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(mThreshImageMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

            //mContourMat = Mat.zeros(mThreshImageMat.height(), mThreshImageMat.width(), CvType.CV_8UC3);
            mContourMat = new Mat();
            mMaskedImageMat.copyTo(mContourMat);

            if (isGrayThresh) {
                Imgproc.cvtColor(mContourMat, mContourMat, Imgproc.COLOR_GRAY2RGB);
            }

            //Arrayを初期化
            double[] contourAreaArray = new double[contours.size()];
            mRectList = new ArrayList<Rect>();
            mDrawnRectList = new ArrayList<Rect>();
            mAllDetectedContours = new ArrayList<MatOfPoint>();
            List<MatOfPoint> drawContours = new ArrayList<MatOfPoint>();

            int minSize, maxSize;
            if(mDetectMinSize < mDrawMinSize){
                minSize = mDrawMinSize;
            }else{
                minSize = mDetectMinSize;
                mDrawMinSize = mDetectMinSize;
            }

            if(mDetectMaxSize > mDrawMaxSize){
                maxSize = mDrawMaxSize;
            }else{
                maxSize = mDetectMaxSize;
                mDrawMaxSize = mDetectMaxSize;
            }

            //各種輪郭の特徴量の取得
            //Areaが0.0の数を数えて除く
            double maxArea = 0;
            int count = 0;
            for (int i = 0; i < contours.size(); i++) {
                MatOfPoint contour = contours.get(i);

                double area = Math.abs(Imgproc.contourArea(contours.get(i)));



                //DetectMinとDetectMaxの間のareaのRectとContourは全てListに格納する
                if(mDetectMinSize < area && area < mDetectMaxSize){

                        Rect rect = Imgproc.boundingRect(contour);
                        contourAreaArray[count] = area;
                        count++;
                        mRectList.add(rect);
                        mDrawnRectList.add(rect);
                        mAllDetectedContours.add(contour);

                        if (maxArea < area) {
                            maxArea = area;
                        }
                    if (minSize < area && area < maxSize) {
                        Imgproc.rectangle(mContourMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 255), 1);
                        drawContours.add(contour);
                    }
                }
            }

            mMS_size.setMin(mDetectMinSize);
            mMS_size.setMax(mDetectMaxSize);
            mTextView_SizeRange.setText("Min :" + mDetectMinSize + "- Max : " + mDetectMaxSize);

            //必要なArray(0じゃない)部分をメンバ変数にコピーする
            mContourAreaArray = Arrays.copyOf(contourAreaArray, mRectList.size());
            //System.arraycopy(contourAreaArray, 0, mContourAreaArray, mRectList.size(), );

            //数えられた粒子数をSetする
            TextView textViewParticleNum = (TextView) mView_analyze_imageData.findViewById(R.id.textView_particleNum);
            textViewParticleNum.setText("ParticleNumber : " + mRectList.size());

            Log.d("mTabMainActivity", "mRectSize = " + mRectList.size() + "　　mContourAreaArray.size = " + mContourAreaArray.length);
            Scalar color = new Scalar(225, 0, 178);
            Imgproc.drawContours(mContourMat, drawContours, -1, color, 1);
            Bitmap displayImageBitmap = Bitmap.createBitmap(mContourMat.width(), mContourMat.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mContourMat, displayImageBitmap);
            mCustomImageView.setImageBitmap(displayImageBitmap);

        }else if(isNew == false){

            mContourMat = new Mat();
            mMaskedImageMat.copyTo(mContourMat);
            if(isGrayThresh){
                Imgproc.cvtColor(mContourMat, mContourMat, Imgproc.COLOR_GRAY2RGB);
            }
            mDrawnRectList = new ArrayList<Rect>();

            //サイズで閾値を設けて描く輪郭を決定する
            List<MatOfPoint> drawnContours = new ArrayList<MatOfPoint>();
            int nContours = mAllDetectedContours.size();
            for (int i = 0; i < nContours; i++) {
                double area = mContourAreaArray[i];
                if (mDrawMinSize < area && area < mDrawMaxSize) {
                    Rect rect = mRectList.get(i);
                    mDrawnRectList.add(rect);
                    Imgproc.rectangle(mContourMat, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 255), 1);
                    drawnContours.add(mAllDetectedContours.get(i));
                }
            }

            //数えられた粒子数をSetする
            TextView textViewParticleNum = (TextView) mView_analyze_imageData.findViewById(R.id.textView_particleNum);
            textViewParticleNum.setText("ParticleNumber : " + drawnContours.size());

            Scalar color = new Scalar(255, 0, 178);
            Imgproc.drawContours(mContourMat, drawnContours, -1, color, 1);
            Bitmap displayImageBitmap = Bitmap.createBitmap(mContourMat.width(), mContourMat.height(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mContourMat, displayImageBitmap);
            mCustomImageView.setImageBitmap(displayImageBitmap);
        }



    }

    private void changeDisplayImage(){

        RealmList<MeasuredDateAndDataObject> dateAndDataList = mUserObject.getMeasuredDateAndDataList();
        RealmList<CapturedImageObject> captureImageList = dateAndDataList.get(mDatePosition).getCapturedImages();
        CapturedImageObject capturedImage = captureImageList.get(mCheckedImageNumArray[mDisplayedImageNum]);
        File imageFile = new File(capturedImage.getFilePath());
        Bitmap displayImageBitmap = decodeFile(imageFile.getAbsolutePath());
        mSrcMat = new Mat();
        Utils.bitmapToMat(displayImageBitmap, mSrcMat);
        //mImageView.setImageBitmap(mAnalyzedImageBitmap);
        if(mAnalyzeState == NONE) {
            mCustomImageView.setImageBitmap(displayImageBitmap);
            return;
        }

        //進むor戻るを押した時の解析の進度(mAnalyzeState)に応じて、
        //止める(returnする)場所を変える

        setGlayScaleImage();
        if(mAnalyzeState == GRAY_SCALE) {
            return;
        }

        setThresholdImage();
        if(mAnalyzeState == THRESHOLD){
            return;
        }

        setFindContourImage(true);
        if(mAnalyzeState == FIND_CONTOUR){
            return;
        }
    }

    //4ページ目  解析結果の表示
    public void createDataAnalysisForthPage(double[] meanIntensityArray){

        //4ページ目　Viewの設定
        mView_analyze_result = getLayoutInflater().inflate(R.layout.view_analyze_result, null);
        //4ページ目  グラフ作製テスト
        GraphView graph = (GraphView)mView_analyze_result.findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 9),
                new DataPoint(4, 11),
                new DataPoint(5, 7),
                new DataPoint(6, 1),
                new DataPoint(7, 3),
                new DataPoint(8, 16),
        });
        graph.addSeries(series);

        MakeHistogramDistribution.makeFreqDistribution(10, meanIntensityArray);
        double[] freqDist = MakeHistogramDistribution.getFreqDistribution();
        double[] xAxis = MakeHistogramDistribution.getXAxis();


        GraphView bar_graph = (GraphView)mView_analyze_result.findViewById(R.id.graph_bar);
        BarGraphSeries<DataPoint> series2 = new BarGraphSeries<>(new DataPoint[]{

        });

        for(int i = 0; i < xAxis.length; i++){
            series2.appendData(new DataPoint(xAxis[i], freqDist[i]), true, xAxis.length);
        }

        bar_graph.addSeries(series2);
        series2.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(TabMainActivity.this, "Series:OnDataPointClicked" + dataPoint, Toast.LENGTH_SHORT).show();
            }
        });



        mFL_DataAnalysis.removeView(mView_analyze_imageData);
        mFL_DataAnalysis.addView(mView_analyze_result);

    }

    private void initMats(){
        if(mSrcMat != null){
            mSrcMat.release();
            mSrcMat = null;
        }

        if(mGrayImageMat != null) {
            mGrayImageMat.release();
            mGrayImageMat = null;
        }

        if(mThreshImageMat != null){
            mThreshImageMat.release();
            mThreshImageMat = null;
        }

        if(mMaskedImageMat != null){
            mMaskedImageMat.release();
            mMaskedImageMat = null;
        }

        if(mContourMat != null) {
            mContourMat.release();
            mContourMat = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("mTabMainAct", "OpenCV library found inside package. Using it!");
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            View currentView = mFL_DataAnalysis.getChildAt(0);
            if(currentView == mView_analyze_imageData){
                View view_detectparticle_config = getLayoutInflater().inflate(R.layout.view_detectparticle_config, null);
                mFL_DataAnalysis.addView(view_detectparticle_config);

                RelativeLayout rl = (RelativeLayout)view_detectparticle_config.findViewById(R.id.rl_detectparticle_config);
                rl.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mFL_DataAnalysis.removeViewAt(1);
                    }
                });
                //検出する最小サイズと最大サイズを決定する
                final TextView textViewDetectMinSize = (TextView)view_detectparticle_config.findViewById(R.id.textView_detectminsize);
                final SeekBar sbDetectMinSize = (SeekBar)view_detectparticle_config.findViewById(R.id.seekBar_detectminsize);
                sbDetectMinSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if(mDetectMaxSize > progress) {
                            mDetectMinSize = progress;
                            textViewDetectMinSize.setText("DetectMinSize : " + progress);
                        }else{

                            mDetectMinSize = mDetectMaxSize-1;
                            sbDetectMinSize.setProgress(mDetectMinSize);
                            textViewDetectMinSize.setText("DetectMinSize : " + (mDetectMinSize));
                        }

                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                final TextView textViewDetectMaxSize = (TextView)view_detectparticle_config.findViewById(R.id.textView_detectmaxsize);
                SeekBar sbDetectMaxSize = (SeekBar)view_detectparticle_config.findViewById(R.id.seekBar_detectmaxsize);
                sbDetectMaxSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                        if(mDetectMinSize < progress) {
                            mDetectMaxSize = progress;
                            textViewDetectMaxSize.setText("DetectMaxSize : " + progress);
                        }else{
                            mDetectMaxSize = mDetectMinSize+1;
                            sbDetectMinSize.setProgress(mDetectMaxSize);
                            textViewDetectMaxSize.setText("DetectMaxSize : " + (mDetectMaxSize));
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });


            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    //ViewPager.OnPageChangeListener
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        int viewNum = mFL_DataAnalysis.getChildCount();
        //キーボードが出てたら閉じる
        Log.d("mTabMainActivity", "onPageScrolled = " + position);
        InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(mView_camera_config.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    //ViewPager.OnPageChangeListener
    public void onPageSelected(int position) {
        //Log.d("mTabMainActivity", "onPageSelected position = " + position);

    }

    @Override
    //ViewPager.OnPageChangeListener
    public void onPageScrollStateChanged(int state) {
        //Log.d("mTabMainActivity", "onPageScrollStateChange stat = " + state);
    }

    @Override
    //ViewPager.OnPageChangeListener
    public void onFragmentInteraction(Uri uri) {
    }

//    @Override
//    public void onInterceptTouchEvent(MotionEvent event){
//
//    }

    @Override
    public void onBackPressed() {

        //int viewNum = mFL_DataAnalysis.getChildCount();

        View currentView = mFL_DataAnalysis.getChildAt(0);

        if(currentView == mView_select_imageData){
            mFL_DataAnalysis.removeView(currentView);
            mFL_DataAnalysis.addView(mView_select_measuredDate);
        }else if(currentView == mView_analyze_imageData){

            isOpen = false;

            //Matを全部初期化(null)
            initMats();

            mFL_DataAnalysis.removeView(currentView);
            mFL_DataAnalysis.addView(mView_select_imageData);
        }else if(currentView == mView_analyze_result) {
            mFL_DataAnalysis.removeView(currentView);
            mFL_DataAnalysis.addView(mView_analyze_imageData);
        }else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        return false;
    }


    private int calculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        //画像の元サイズ
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = (int) Math.floor((float) height / (float) reqHeight);
            } else {
                inSampleSize = (int) Math.floor((float) width / (float) reqWidth);
            }
        }
        return inSampleSize;
    }
}
