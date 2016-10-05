package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.userselect;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmResults;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.MeasuredDateAndDataObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.UserObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.CustomRecyclerItemClickListener;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.TabMainActivity;

public class UserSelectActivity extends AppCompatActivity {

    public final static String EXTRA_USER = "jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.USER";

    //Realmのメンバ変数
    private Realm mRealm;
    private RealmResults<UserObject> mUserRealmResults;
    private RealmChangeListener mRealmChangeListener = new RealmChangeListener() {
        @Override
        //realmに変化があった際にはRecycledViewを更新する
        //最初に表示するリストもreloadRecycledView()で読み込む
        public void onChange() {
            reloadRecycledView();
        }
    };

    //RecycledViewのメンバ変数
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    //UserLayoutActivity
    private ArrayList<UserObject> mUserObjectArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_select);

        //CollapseintToolBarLayoutの設定
        CollapsingToolbarLayout toolbarLayout = (CollapsingToolbarLayout)findViewById(R.id.collapsing_toolbar);
        //タイトル文字設定
        toolbarLayout.setTitle("UserSelect");
        //文字色(縮小時)
        toolbarLayout.setCollapsedTitleTextColor(Color.BLACK);
        //文字色(展開時)
        toolbarLayout.setExpandedTitleColor(Color.WHITE);

        //Realmの設定
        mRealm = Realm.getDefaultInstance();
        mUserRealmResults = mRealm.where(UserObject.class).findAll();
        mRealm.addChangeListener(mRealmChangeListener);

        //RecycleViewの設定(仮)
        mRecyclerView = (RecyclerView)findViewById(R.id.recyclerView_userselect);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mUserObjectArrayList = new ArrayList<>();
        mAdapter = new UserListRecycleAdapter(mUserObjectArrayList);
        mRecyclerView.setAdapter(mAdapter);

        //独自に作成したRecyclerItemClickListenerを実装
        mRecyclerView.addOnItemTouchListener(
                new CustomRecyclerItemClickListener(UserSelectActivity.this, mRecyclerView, new CustomRecyclerItemClickListener.OnItemClickListener(){
                    @Override
                    public void onItemClick(View view, int position){

                        Log.d("mTestUseSelect", "normal click");
                        Intent intent = new Intent(UserSelectActivity.this, TabMainActivity.class);
                        intent.putExtra("id", mUserObjectArrayList.get(position).getId());
                        Log.d("mTestUserSelect", mUserObjectArrayList.get(position).getName());

                        startActivity(intent);
                    }

                    @Override
                    public void onItemLongClick(View view, int position) {
                        //LongClickで削除するかどうかのインジケーターを出す。
                        Log.d("mTestUserSelect", "long click");
                    }
                })
        );

        if(mUserRealmResults.size() == 0){
            addUserForTest();
        }

        //最後にRecycledViewの更新
        reloadRecycledView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Settingの設定
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if(id == R.id.action_settings){
            addItem();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRealm.close();
    }

    protected void addItem(){
        //mDataset.add(0, "X");
        mAdapter.notifyItemInserted(0);
    }

    public void addUserForTest(){

        for(int i=0; i<3; i++) {

            UserObject user = new UserObject();
            String username = "test user" + String.valueOf(i);
            user.setName(username);
            user.setId(i);

            //仮のMeasuredDataAndDataObjectのListを実装して、
            //あとで日付を読み込む
            RealmList<MeasuredDateAndDataObject> dateAndDataList = new RealmList<MeasuredDateAndDataObject>();

            for (int j = 0; j < 3; j++) {
                MeasuredDateAndDataObject dateAndDataObject = new MeasuredDateAndDataObject();
                dateAndDataObject.setMeasuredDate(new Date());
                dateAndDataList.add(dateAndDataObject);
            }

            user.setMeasureDataAndDateList(dateAndDataList);

            mRealm.beginTransaction();
            mRealm.copyToRealmOrUpdate(user);
            mRealm.commitTransaction();
        }
    }

    public void reloadRecycledView(){

        for(int i = 0; i < mUserRealmResults.size(); i++){
            UserObject user = new UserObject();

            user.setId(mUserRealmResults.get(i).getId());
            user.setName(mUserRealmResults.get(i).getName());
            user.setSaveFolderName(mUserRealmResults.get(i).getSaveFolderName());
            user.setMeasureDataAndDateList(mUserRealmResults.get(i).getMeasuredDateAndDataList());

            mUserObjectArrayList.add(user);
        }

        mAdapter = new UserListRecycleAdapter(mUserObjectArrayList);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }
}
