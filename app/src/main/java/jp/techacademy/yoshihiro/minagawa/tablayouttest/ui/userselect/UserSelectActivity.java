package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.userselect;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.ActivityCompat;
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

    private static final int REQUEST_PERMISSIONS = 0x01;

    private static final String[] ALL_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_select);

        //Permissionの確認を全てここで行う
        if(!hasAllPermissionGranted()){
            requestCameraPermission();
        }


//        if(!OpenCVLoader.initDebug()){
//            Log.i("OpenCV", "Failed");
//        }else{
//            Log.i("OpenCV", "successfully built!");
//
//        }

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
                        //LongClickで削除するかどうかのインジケーターを出す予定
                        //DeleteMeasuredDateDialogFragment deleteMeasuredDateDialogFragment = DeleteMeasuredDateDialogFragment.newInstance();
                        //deleteMeasuredDateDialogFragment.show(UserSelectActivity.this.getFragmentManager(), "DeleteMeasuredDateDialogFragment");
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
            String username = "test_user" + String.valueOf(i);
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

        mUserObjectArrayList = new ArrayList<>();

        for(int i = 0; i < mUserRealmResults.size(); i++){
            UserObject user = new UserObject();

            user.setId(mUserRealmResults.get(i).getId());
            user.setName(mUserRealmResults.get(i).getName());
            user.setUserPath(mUserRealmResults.get(i).getUserPath());
            user.setMeasureDataAndDateList(mUserRealmResults.get(i).getMeasuredDateAndDataList());

            mUserObjectArrayList.add(user);
        }

        mAdapter = new UserListRecycleAdapter(mUserObjectArrayList);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }


    //全部のパーミッションがあるかの確認
    private boolean hasAllPermissionGranted(){
        for(String permission : ALL_PERMISSIONS){
            if(ActivityCompat.checkSelfPermission(UserSelectActivity.this, permission)
                    != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    //Permission handling for Android6.0
    private void requestCameraPermission() {
        for (String permission : ALL_PERMISSIONS) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Log.d("mTestUserSelectActivity", "shouldShowRequestPermissionRationale:追加説明");
                //権限チェックした結果、持っていない場合はダイアログを出す
                new AlertDialog.Builder(this)
                        .setTitle("パーミッションに関する説明")
                        .setMessage("このアプリではカメラ制御、データ書き込み、データ読み込みに関するパーミッションが必要です")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(UserSelectActivity.this,
                                        ALL_PERMISSIONS,
                                        REQUEST_PERMISSIONS);
                            }
                        }).create().show();
                return;
            }
        }

        //権限の取得
        ActivityCompat.requestPermissions(this, ALL_PERMISSIONS, REQUEST_PERMISSIONS);
        return;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        int req = requestCode;
        String[] permi = permissions;
        int[] grantR = grantResults;

        int i = 0;

        if(requestCode == REQUEST_PERMISSIONS){
            for(int result: grantResults) {

                if(result != PackageManager.PERMISSION_GRANTED){

                    Log.d("mTestUserSelectActivity", "onRequestPermissionResult:DENYED");


                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[i])){
                        Log.d("mTestUserSelectActivity", "[show error]");
                        new AlertDialog.Builder(this)
                                .setTitle("パーミッション取得エラー1")
                                .setMessage("再試行する場合は、再度Requestボタンを押してください")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        requestCameraPermission();
                                        return;
                                    }
                                }).create().show();
                    }else{
                        Log.d("mTestUserSelectActivity", "[show app setting guide");
                        new AlertDialog.Builder(this)
                                .setTitle("パーミッション取得エラー2")
                                .setMessage("今後は許可しないが選択されました。アプリ設定>権限をチェックしてください")
                                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        openSetting();
                                    }
                                }).create().show();
                    }

                }else{
                    Log.d("mTestUserSelectActivity", "onRequestPermissionResult:GRANTED");
                    //Todoをここに書く場合がある。実際にカメラを使うのは後なので、今回は特になし。
                }
                i += 1;
            }
        }else{
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void openSetting(){
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        //Fragmenetの場合はgetContext().getPackageName()
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}
