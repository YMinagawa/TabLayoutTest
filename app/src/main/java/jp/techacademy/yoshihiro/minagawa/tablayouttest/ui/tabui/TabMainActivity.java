package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import io.realm.Realm;
import io.realm.RealmResults;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.UserObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.analyzedata.SelectMeasuredDateFragment;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.camera.CameraFragment;

public class TabMainActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener,
        PageFragment.OnFragmentInteractionListener, CameraFragment.OnFragmentInteractionListener, SelectMeasuredDateFragment.OnFragmentInteractionListener {

    //User
    private UserObject mUserObject;

    //idをインテントで入手するためのメンバ変数
    int mId;

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

        //表示Pageに必要な項目を設定
        FragmentPagerAdapter adapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {

                if (position == 0) {
                    return CameraFragment.newInstance();
                }else if(position == 1) {

                    //RealmのObjectをBundleで引き渡すと、後でエラー(NotSerializableException)がでる。
                    //primary key (id)での引き渡しに変更
                    return SelectMeasuredDateFragment.newInstance(mId);

                }else if(position == 2){
                    return PageFragment.newInstance(position + 1);
                }else{
                    return null;
                }
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return pageTitle[position];
            }

            @Override
            public int getCount() {
                return pageTitle.length;
            }
        };

        //ViewPageにページを設定
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(this);

        //ViewPagerをTabLayoutを設定
        tabLayout.setupWithViewPager(viewPager);

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
