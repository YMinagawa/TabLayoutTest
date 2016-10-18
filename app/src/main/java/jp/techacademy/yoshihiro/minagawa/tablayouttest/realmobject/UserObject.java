package jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject;

/**
 * Created by ym on 2016/09/30.
 */

import java.io.Serializable;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by ym on 2016/09/28.
 */

public class UserObject extends RealmObject implements Serializable {

    //idをプライマリーキーとして設定
    @PrimaryKey
    private int id;

    private String name; //名前
    private String userPath; //フォルダのパス

    private int numOfInspection; //検査回数
    private int numOfInfection; //感染回数

    //private ArrayList<Date> datesOfInspection; //検査日時リスト
    //private ArrayList<Date> datesOfInfection; //感染日時リスト

    //RealmListを使うと保存時間と取得時間を短縮可能
    private RealmList<MeasuredDateAndDataObject> measuredDateAndDataList; //日付毎のデータリスト

    //getterとsetter
    //id
    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id = id;
    }

    //ユーザー名
    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    //フォルダー名
    public String getUserPath(){
        return userPath;
    }

    public void setUserPath(String userPath){
        this.userPath = userPath;
    }

    //検査回数
    public int getNumOfInspection(){return numOfInspection;}

    public void setNumOfInspection(int numOfInspection){this.numOfInspection = numOfInspection;}

    //感染回数
    public int getNumOfInfection(){return numOfInfection;}

    public void setNumOfInfection(int numOfInfection){this.numOfInfection = numOfInfection;}


    //日付毎のデータリスト
    public RealmList<MeasuredDateAndDataObject> getMeasuredDateAndDataList(){
        return measuredDateAndDataList;
    }

    public void setMeasureDataAndDateList(RealmList<MeasuredDateAndDataObject> measuredDateAndDataList){
        this.measuredDateAndDataList = measuredDateAndDataList;
    }

}
