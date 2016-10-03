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
    private String saveFolderName;

    private int numOfInspection; //検査回数
    private int numOfInfection; //感染回数

    //private ArrayList<Date> datesOfInspection; //検査日時リスト
    //private ArrayList<Date> datesOfInfection; //感染日時リスト

    //RealmListを使うと保存時間と取得時間を短縮可能
    private RealmList<MeasuredDateAndDataObject> measuredDateAndDataList; //日付毎のデータリスト

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
    public String getSaveFolderName(){
        return saveFolderName;
    }

    public void setSaveFolderName(String saveFolderName){
        this.saveFolderName = saveFolderName;
    }

    //日付毎のデータリスト
    public RealmList<MeasuredDateAndDataObject> getMeasuredDateAndDataList(){
        return measuredDateAndDataList;
    }

    public void setMeasureDataAndDateList(RealmList<MeasuredDateAndDataObject> measuredDateAndDataList){
        this.measuredDateAndDataList = measuredDateAndDataList;
    }

}
