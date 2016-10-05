package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.UserObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.CustomItemDecoration;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.CustomRecyclerItemClickListener;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.analyzedata.MeasuredDateListRecycleAdapter;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.camera.CameraActivity;

public class TabMainActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener,
        PageFragment.OnFragmentInteractionListener{

    //User
    private UserObject mUserObject;

    //idをインテントで入手するためのメンバ変数
    int mId;

    //Framelayoutの設定
    FrameLayout mFL_Camera;
    FrameLayout mFL_DataAnalysis;
    FrameLayout mFL_History;
    View mView_select_measuredDate;
    View mView_camera_config;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    RecyclerView.Adapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //インテントからidを入手してどのユーザーかを決定する
        Intent intent = getIntent();
        mId = intent.getIntExtra("id", 0);

        //ここで選択されたユーザーオブジェクトの入手を行う
        //getDefalultInstance()をしたら必ずcloseする
        Realm realm = Realm.getDefaultInstance();
        RealmResults<UserObject> userRealmResults = realm.where(UserObject.class).equalTo("id", mId).findAll();
        realm.close();
        mUserObject = userRealmResults.get(0);
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
        createDataAnalysisPage();

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
        mView_camera_config = getLayoutInflater().inflate(R.layout.view_camera_config, null);
        mFL_Camera.addView(mView_camera_config);
        FloatingActionButton fab_camera = (FloatingActionButton)mView_camera_config.findViewById(R.id.fab_camera);
        fab_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TabMainActivity.this, CameraActivity.class);
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
    public void createDataAnalysisPage(){

        mFL_DataAnalysis = new FrameLayout(this);

        //1ページ目の設定 : データを測定した日付を選ぶページ
        mView_select_measuredDate = getLayoutInflater().inflate(R.layout.view_select_measureddate, null);
        mFL_DataAnalysis.addView(mView_select_measuredDate);

        //1ページ目 : RecycleViewの設定
        mRecyclerView = (RecyclerView)mView_select_measuredDate.findViewById(R.id.recyclerView_selectcapdate);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        //1ページ目 : RecycleViewにItemDecorationをセットする
        mRecyclerView.addItemDecoration(new CustomItemDecoration(this));

        mAdapter = new MeasuredDateListRecycleAdapter(mUserObject.getMeasuredDateAndDataList());
        mRecyclerView.setAdapter(mAdapter);

        //1ページ目 : 独自に作成したRecycleItemOnClickListenerを実装する
        mRecyclerView.addOnItemTouchListener(
                new CustomRecyclerItemClickListener(this, mRecyclerView, new CustomRecyclerItemClickListener.OnItemClickListener(){
                    @Override
                    public void onItemClick(View view, int position) {
                        Log.d("mTest", "measureddate normal click");
                    }

                    @Override
                    public void onItemLongClick(View view, int position) {

                    }
                })
        );

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
