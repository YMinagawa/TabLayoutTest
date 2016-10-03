package jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject;

import android.app.Application;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by ym on 2016/09/30.
 */


//Applicationクラスを継承したUserAppクラスを作成する


public class UserApp extends Application{

    public void onCreate(){
        super.onCreate();
        //アプリケーションの"files"ディレクトリにRealmファイルを作成するRealmConfigurationを作成
        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder(this).build();
        Realm.setDefaultConfiguration(realmConfiguration);
    }


}
