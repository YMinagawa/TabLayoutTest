package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui;

import android.content.Context;
import android.content.Intent;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
                        createDataAnalysisSecondPage(position);
                    }

                    @Override
                    public void onItemLongClick(View view, int position) {

                    }
                })
        );




    }

    public void createDataAnalysisSecondPage(int position){

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
        RealmList<CapturedImageObject> captureImageList = dateAndDataList.get(position).getCapturedImages();

        mSelectImageAdapter = new ImageDataListRecyclerAdapter(captureImageList);
        mSelectImageRecyclerView.setAdapter(mSelectImageAdapter);

        //2ページ目：RecycleViewの下にDate, ISO, ExposureTimeを表示
        TextView textViewDate = (TextView)mView_select_imageData.findViewById(R.id.textView_measureddate);
        TextView textViewISO = (TextView)mView_select_imageData.findViewById(R.id.textView_iso);
        TextView textViewExpTime = (TextView)mView_select_imageData.findViewById(R.id.textView_exptime);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd  HH:mm");
        textViewDate.setText("Measured Date : " + sdf.format(dateAndDataList.get(position).getMeasuredDate()));
        textViewISO.setText("ISO : " + dateAndDataList.get(position).getISO());
        textViewExpTime.setText("Exposure Time : " + dateAndDataList.get(position).getExposureTime() + " ms");

        mFL_DataAnalysis.addView(mView_select_imageData);
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



}
