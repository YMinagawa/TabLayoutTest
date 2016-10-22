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
import android.view.View;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.CapturedImageObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.MeasuredDateAndDataObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.UserObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.CustomItemDecoration;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.CustomRecyclerItemClickListener;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.analyzedata.ImageDataListRecyclerAdapter;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.analyzedata.MeasuredDateListRecycleAdapter;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.camera.CameraActivity;

import static android.graphics.BitmapFactory.decodeFile;

public class TabMainActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener,
        PageFragment.OnFragmentInteractionListener{

    //User
    private UserObject mUserObject;

    //idをインテントで入手するためのメンバ変数
    int mId;

    //Framelayout (各Viewを乗っける)
    FrameLayout mFL_Camera;
    FrameLayout mFL_DataAnalysis;
    FrameLayout mFL_History;

    //各View
    //Camera
    View mView_camera_config;

    //DataAnalysis
    //1ページ
    View mView_select_measuredDate;
    RecyclerView mSelectMeasureDateRecyclerView;
    RecyclerView.LayoutManager mSelectMeasureDateLayoutManager;
    RecyclerView.Adapter mSelectMeasureDateAdapter;
    //2ページ
    View mView_select_imageData;
    RecyclerView mSelectImageRecyclerView;
    RecyclerView.LayoutManager mSelectImageLayoutManager;
    RecyclerView.Adapter mSelectImageAdapter;
    FloatingActionButton mFab_plus, mFab_analysis, mFab_delete;
    Animation mFabOpen, mFabClose, mFabRClockwise,  mFabRanticlockwise;
    boolean isOpen =false;
    //3ページ
    View mView_analyze_imageData;
    ImageView mImageView;
    Bitmap mAnalyzedImageBitmap;
    Bitmap mGrayImageBitmap;
    Bitmap mThreshImageBitmap;
    Bitmap mErodeImageBitmap;
    Bitmap mGaussBlurBitmap;
    Bitmap mContoursBitmap;
    Bitmap mConnectComBitmap;
    SeekBar mSb_threshold;
    SeekBar mSb_size;

    int[] mXArray;
    int[] mYArray;
    int[] mWidthArray;
    int[] mHeightArray;
    int[] mAreaArray;

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

        //xmlからTabLayoutの取得
        TabLayout tabLayout = (TabLayout)findViewById(R.id.tabs);
        //xmlからViewPagerの取得
        ViewPager viewPager = (ViewPager)findViewById(R.id.pager);
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
        viewPager.setAdapter(cpa);
        viewPager.addOnPageChangeListener(this);

        //ViewPagerをTabLayoutを設定
        tabLayout.setupWithViewPager(viewPager);

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

//        mFab_plus = (FloatingActionButton)mView_analyze_imageData.findViewById(R.id.fab_plus);
        mFab_analysis = (FloatingActionButton)mView_select_imageData.findViewById(R.id.fab_analysis);
        mFab_analysis.setImageResource(R.drawable.fab_chart_bar);
        mFab_analysis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createDataAnalysisThirdPage(date_position);
            }
        });
//        mFab_delete = (FloatingActionButton)mView_analyze_imageData.findViewById(R.id.fab_delete);
//        mFabOpen = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
//        mFabClose = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
//        mFabRClockwise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_clockwise);
//        mFabRanticlockwise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_anticlockwise);

        //mFab_plusを押した時のアニメーション
//        mFab_plus.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                if(isOpen){
//                    mFab_delete.startAnimation(mFabClose);
//                    mFab_delete.setClickable(false);
//                    mFab_analysis.startAnimation(mFabClose);
//                    mFab_analysis.setClickable(false);
//                    mFab_plus.startAnimation(mFabRanticlockwise);
//                    isOpen = false;
//                }else{
//                    mFab_delete.startAnimation(mFabOpen);
//                    mFab_delete.setClickable(true);
//                    mFab_analysis.startAnimation(mFabOpen);
//                    mFab_analysis.setClickable(true);
//                    mFab_plus.startAnimation(mFabRClockwise);
//                    isOpen = true;
//
//                }
//            }
//        });



        mFL_DataAnalysis.addView(mView_select_imageData);
    }

    public void createDataAnalysisThirdPage(int date_position){

        System.loadLibrary("opencv_java3");

        mView_analyze_imageData = getLayoutInflater().inflate(R.layout.view_analyze_imagedata, null);
        mImageView = (ImageView)mView_analyze_imageData.findViewById(R.id.imageView_analyzeimage);

        RealmList<MeasuredDateAndDataObject> dateAndDataList = mUserObject.getMeasuredDateAndDataList();
        RealmList<CapturedImageObject> captureImageList = dateAndDataList.get(date_position).getCapturedImages();

        //imageViewにチェックされている画像のデータを入れていく？
        //チェックされている画像がどれなのか数字が必要
        File imageFile = null;
        int imageNumber;
        for(int i = 0; i < captureImageList.size(); i++){
            CapturedImageObject capturedImage = captureImageList.get(i);
            if(capturedImage.getChecked() == true){
                imageFile = new File(capturedImage.getFilePath());
                imageNumber = i;
                break;
            }
        }

        if(imageFile != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inJustDecodeBounds = true;
//            decodeFile(imageFile.getAbsolutePath(), options);
//            LinearLayout ll_analyze_image = (LinearLayout) mView_analyze_imageData.findViewById(R.id.ll_analyze_imagedata);
//            ll_analyze_image.measure(ll_analyze_image.getMeasuredWidth(), ll_analyze_image.getMeasuredHeight());
//            int height = ll_analyze_image.getMeasuredHeight();
//            int width = ll_analyze_image.getMeasuredWidth();
//            int inSampleSize = calculateSampleSize(options, width, height);
            //とりあえずinSampleSizeは1で。
            options.inJustDecodeBounds = false;
            options.inSampleSize = 1;
            mAnalyzedImageBitmap = decodeFile(imageFile.getAbsolutePath(), options);

            mImageView.setImageBitmap(mAnalyzedImageBitmap);

            mFL_DataAnalysis.addView(mView_analyze_imageData);

            mImageView.measure(mImageView.getMeasuredWidth(), mImageView.getMeasuredHeight());
            int height = mImageView.getMeasuredHeight();
            int width = mImageView.getMeasuredWidth();
        }else{
            Toast.makeText(this, "Please check one image at least !", Toast.LENGTH_SHORT).show();
        }

        //元に戻すButton設定
        Button btnReset = (Button)mView_analyze_imageData.findViewById(R.id.btn_reset);
        btnReset.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                System.loadLibrary("opencv_java3");
                mImageView.setImageBitmap(null);
                mImageView.setImageBitmap(mAnalyzedImageBitmap);
            }
        });

        //グレイスケール用のButtonの設定
        Button btnGray = (Button)mView_analyze_imageData.findViewById(R.id.btn_gray);

        btnGray.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.loadLibrary("opencv_java3");
                Mat srcMat = new Mat();
                Utils.bitmapToMat(mAnalyzedImageBitmap, srcMat);
                Mat grayMat = new Mat();
                Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGB2GRAY);
                mGrayImageBitmap = Bitmap.createBitmap(grayMat.width(), grayMat.height(), Bitmap.Config.ARGB_8888);
                //白黒反転
                //Core.bitwise_not(grayMat, grayMat);
                Utils.matToBitmap(grayMat, mGrayImageBitmap);
                mImageView.setImageBitmap(mGrayImageBitmap);
                srcMat.release();
                grayMat.release();
            }
        });

        //反転(inveret)用のButtonの設定
        Button btnInvert = (Button)mView_analyze_imageData.findViewById(R.id.btn_invert);
        btnInvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mGrayImageBitmap != null){
                    System.loadLibrary("opencv_java3");
                    Mat srcMat = new Mat();
                    Utils.bitmapToMat(mAnalyzedImageBitmap, srcMat);
                    Mat grayMat = new Mat();
                    Imgproc.cvtColor(srcMat, grayMat, Imgproc.COLOR_RGB2GRAY);
                    mGrayImageBitmap = Bitmap.createBitmap(grayMat.width(), grayMat.height(), Bitmap.Config.ARGB_8888);
                    //白黒反転
                    Core.bitwise_not(grayMat, grayMat);
                    Utils.matToBitmap(grayMat, mGrayImageBitmap);
                    mImageView.setImageBitmap(mGrayImageBitmap);
                    srcMat.release();
                    grayMat.release();
                }

            }
        });

        //ThresholdのSeekBarの設定
        mSb_threshold = (SeekBar)mView_analyze_imageData.findViewById(R.id.seekBar_thresh);
        mSb_threshold.setEnabled(false);
        final View toastlayout = getLayoutInflater().inflate(R.layout.toast_layout, null);
        final TextView toastText = (TextView)toastlayout.findViewById(R.id.textView_toast);

        mSb_threshold.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                System.loadLibrary("opencv_java3");
                mThreshImageBitmap = mGrayImageBitmap;
                mImageView.setImageBitmap(mGrayImageBitmap);
                toastText.setText("Threshold " + progress);
                Log.d("mTabMainActivity", "thresh = " + progress);
                //まずグレイスケール用のmatを用意し、
                //Bitmapからmatに変換する
                Mat grayMat = new Mat();
                Utils.bitmapToMat(mGrayImageBitmap, grayMat);
                //プログレスバーの値を閾値として設定する。
                //threshold用のmatを用意して、grayMatにthresholdを適用したものを入れる。
                Mat thresholdMat = new Mat();
                Imgproc.threshold(grayMat, thresholdMat, progress, 255, Imgproc.THRESH_BINARY);

                mThreshImageBitmap = Bitmap.createBitmap(thresholdMat.width(), thresholdMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(thresholdMat, mThreshImageBitmap);
                mImageView.setImageBitmap(mThreshImageBitmap);
                grayMat.release();
                thresholdMat.release();

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

                if(mGrayImageBitmap == null && mThreshImageBitmap == null){
                    Toast.makeText(TabMainActivity.this, "Please change grayscale first", Toast.LENGTH_SHORT).show();
                }else if(mGrayImageBitmap !=null && mThreshImageBitmap == null){
                    mImageView.setImageBitmap(mGrayImageBitmap);
                    mSb_threshold.setEnabled(true);
                    mSb_size.setEnabled(false);
                }else if(mThreshImageBitmap != null){
                    mImageView.setImageBitmap(mThreshImageBitmap);
                    mSb_threshold.setEnabled(true);
                    mSb_size.setEnabled(false);
                }

            }
        });

        //erode用のButtonの設定
        Button btnErode = (Button)mView_analyze_imageData.findViewById(R.id.btn_erode);
        btnErode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                System.loadLibrary("opencv_java3");
                Mat thresholdMat = new Mat();
                Utils.bitmapToMat(mThreshImageBitmap, thresholdMat);
                Mat erodeMat = new Mat();

                Imgproc.erode(thresholdMat, erodeMat, new Mat());
                mErodeImageBitmap = Bitmap.createBitmap(erodeMat.width(), erodeMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(erodeMat, mErodeImageBitmap);
                mImageView.setImageBitmap(mErodeImageBitmap);
                erodeMat.release();
                thresholdMat.release();
            }
        });

        //MidianBlur用のButtonの設定
        Button btnMidBlur = (Button)mView_analyze_imageData.findViewById(R.id.btn_medianBlur);
        btnMidBlur.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                System.loadLibrary("opencv_java3");
                Mat srcMat = new Mat();
                Utils.bitmapToMat(mAnalyzedImageBitmap, srcMat);
                Mat medianBlurMat = new Mat();
                Imgproc.medianBlur(srcMat, medianBlurMat, 3);
                mGaussBlurBitmap = Bitmap.createBitmap(medianBlurMat.width(), medianBlurMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(medianBlurMat, mGaussBlurBitmap);
                mImageView.setImageBitmap(mGaussBlurBitmap);
                srcMat.release();
                medianBlurMat.release();
            }
        });

        //findContours用のButtonの設定
        Button btnFindContours = (Button)mView_analyze_imageData.findViewById(R.id.btn_findContours);
        btnFindContours.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(mThreshImageBitmap != null) {
                    System.loadLibrary("opencv_java3");
                    //閾値を決めた画像を読み込む
                    Mat thresholdMat = new Mat();
                    Utils.bitmapToMat(mThreshImageBitmap, thresholdMat);
                    Imgproc.cvtColor(thresholdMat, thresholdMat, Imgproc.COLOR_RGB2GRAY);

                    //輪郭の抽出
                    ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                    //Mat hierarchy = new Mat(thresholdMat.cols(), thresholdMat.rows(), CvType.CV_32SC1);
                    Mat hierarchy = new Mat();
                    Imgproc.findContours(thresholdMat, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

                    //輪郭の色を決める
                    Scalar contourColor = new Scalar(255, 0, 0);

                    //srcMat
                    Mat srcMat = new Mat();
                    Utils.bitmapToMat(mAnalyzedImageBitmap, srcMat);

                    Mat dstMat = Mat.zeros(new Size(srcMat.width(), srcMat.height()), CvType.CV_8UC3);

                    //opencv.jp/opencv-2.1/cpp/structual_analysis_and_shape_descriptors.html
                    //drawContours
                    //image-出力画像、 contours-入力される輪郭
                    //contourIdx-書かれる輪郭(負の場合：全て)、color-輪郭の色
                    //thickness-輪郭の太さ、負の場合は塗りつぶされる
                    Imgproc.drawContours(dstMat, contours, -1, contourColor, 3);

                    mContoursBitmap = Bitmap.createBitmap(srcMat.width(), srcMat.height(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(dstMat, mContoursBitmap);

                    mImageView.setImageBitmap(mContoursBitmap);

                    srcMat.release();
                    thresholdMat.release();
                    dstMat.release();
                    hierarchy.release();
                    contours = null;
                }
            }
        });

        //connectedComponetnsWithStats用のボタン設定
        Button btn_connectComponents = (Button)findViewById(R.id.btn_connectComponents);
        btn_connectComponents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mThreshImageBitmap != null){

                    //Thresholdのbarを使えなくし、Sizeのseekbarを使えるようにする
                    mSb_threshold.setEnabled(false);
                    mSb_size.setEnabled(true);

                    System.loadLibrary("opencv_java3");

                    //閾値処理を行ったMatをBitmapから変換する
                    //CV_8UC1でないと処理できないので、RGB2GRAYをかける
                    //また、dstMatにthresholdMatをdeepcopyする
                    Mat thresholdMat = new Mat();
                    Utils.bitmapToMat(mThreshImageBitmap, thresholdMat);
                    //Mat dstMat = new Mat(thresholdMat.width(), thresholdMat.height(), thresholdMat.type());
                    Imgproc.cvtColor(thresholdMat, thresholdMat, Imgproc.COLOR_RGB2GRAY);

                    //Roiを重ね合わせるsrcの画像を呼び出す
                    Mat srcMat = new Mat();
                    Utils.bitmapToMat(mAnalyzedImageBitmap, srcMat);

                    //ラベリング結果用のMat
                    Mat labelImageMat = new Mat();
                    //Left, Top, WIDTH, HEIGHT, AREA, MAX
                    Mat statMat = new Mat();
                    //重心用
                    Mat centroidMat = new Mat();

                    //connectedComponentsWithStatsで、返り値がintとして、
                    //ラベルした数が返ってくる (nLabels)
                    int nLabels = Imgproc.connectedComponentsWithStats(thresholdMat, labelImageMat, statMat, centroidMat, 8, 4);

                    //statMat.get(x, y)  x : i(ラベル数)   y : stateの数
                    //y = 0 : LEFT,  y = 1 : TOP,  y = 2 : WIDTH
                    //y = 3 : HEIGHT, y = 4 : AREA, y = 5 : MAX???

                    Log.d("Tabmainactivity", "width = " +(int)statMat.get(0, 2)[0] );

                    //ラベルしたオブジェクトにそれぞれ四角のROIをあてはめる
                    //srcMatのTypeを変える8UC4→8UC3
                    Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB);

                    mXArray = new int[nLabels];
                    mYArray = new int[nLabels];
                    mWidthArray = new int[nLabels];
                    mHeightArray = new int[nLabels];
                    mAreaArray = new int[nLabels];
                    //念のため最初の背景の値(0)を代入
                    mXArray[0] = (int)statMat.get(0, 0)[0];
                    mYArray[0] =  (int)statMat.get(0, 1)[0];
                    mWidthArray[0] = (int)statMat.get(0, 2)[0];
                    mHeightArray[0] =  (int)statMat.get(0, 3)[0];
                    mAreaArray[0] =  (int)statMat.get(0, 4)[0];

                    //i = 0は背景にラベルされるので カウント(i)は1からはじめる
                    int count = 0;
                    for(int i = 1; i < nLabels; i++){

                        mXArray[i] = (int)statMat.get(i, 0)[0];
                        mYArray[i] =  (int)statMat.get(i, 1)[0];
                        mWidthArray[i] = (int)statMat.get(i, 2)[0];
                        mHeightArray[i] =  (int)statMat.get(i, 3)[0];
                        mAreaArray[i] =  (int)statMat.get(i, 4)[0];

                        int area = (int)statMat.get(i, 4)[0];
                        if(area > mSb_size.getProgress()){
                            int x = (int)statMat.get(i, 0)[0];
                            int y = (int)statMat.get(i, 1)[0];
                            int width = (int)statMat.get(i, 2)[0];
                            int height = (int)statMat.get(i, 3)[0];
                            Imgproc.rectangle(srcMat, new Point(x, y), new Point(x+width,y+height), new Scalar(0, 255, 255), 10);
                            count++;
                        }
                    }

                    Log.d("TabMainActivity", "count = " + count);

                    //seekbar_sizeのmax値(背景のラベル(0)を除く)を入れる
                    int[] areaArray = mAreaArray.clone();
                    Arrays.sort(areaArray);
                    Log.d("TabMainActivity", "max = " + areaArray[areaArray.length-1]);
                    Log.d("TabMainActivity", "max-1 = " + areaArray[areaArray.length-2]);

                    //現状max値が大きすぎるとbarの最大値がでかくなりすぎる。
                    //大きすぎる場合は制限を設ける
                    if(areaArray[areaArray.length-2] > 20000) {
                        mSb_size.setMax(20000);
                    }else{
                        mSb_size.setMax(areaArray[areaArray.length - 2]);
                    }

                    //Imgproc.cvtColor(dstMat, dstMat, Imgproc.COLOR_RGB2RGBA);
                    //srcMatをdstMat側にTypeをあわせる8UC4→8UC3
                    //Core.add(srcMat, dstMat, dstMat);

                    mConnectComBitmap = Bitmap.createBitmap(srcMat.width(), srcMat.height(), Bitmap.Config.ARGB_8888);

                    Utils.matToBitmap(srcMat, mConnectComBitmap);
                    mImageView.setImageBitmap(mConnectComBitmap);

                    thresholdMat.release();
                    labelImageMat.release();
                    statMat.release();
                    centroidMat.release();
                    srcMat.release();
                    //dstMat.release();

                }else{
                    Toast.makeText(TabMainActivity.this, "Please finish threshold process", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //SizeでFilterをかけるSeekbarの設定
        mSb_size = (SeekBar)mView_analyze_imageData.findViewById(R.id.seekBar_size);
        mSb_size.setEnabled(false);

        mSb_size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                System.loadLibrary("opencv_java3");
                Mat srcMat = new Mat();
                Utils.bitmapToMat(mAnalyzedImageBitmap, srcMat);
                Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB);

                int nLabels = mXArray.length;
                int count = 0;
                for(int i = 1; i < nLabels; i++){

                    int area = mAreaArray[i];
                    if(area > progress){
                        int x = mXArray[i];
                        int y = mYArray[i];
                        int width = mWidthArray[i];
                        int height = mHeightArray[i];
                        Imgproc.rectangle(srcMat, new Point(x, y), new Point(x+width,y+height), new Scalar(0, 255, 255), 10);
                        count++;
                    }
                }

                mConnectComBitmap = Bitmap.createBitmap(srcMat.width(), srcMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(srcMat, mConnectComBitmap);
                mImageView.setImageBitmap(mConnectComBitmap);

                srcMat.release();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public Mat calc(Mat dstMat){
        return dstMat;
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
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        //キーボードが出てたら閉じる
        InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(mView_camera_config.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    public void onPageSelected(int position) {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onBackPressed() {

        int viewNum = mFL_DataAnalysis.getChildCount();

        if(viewNum > 1){
            mFL_DataAnalysis.removeViewAt(viewNum-1);
        }else {
            super.onBackPressed();
        }
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
