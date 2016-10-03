package jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject;

import java.io.Serializable;
import java.util.Date;

import io.realm.RealmObject;

/**
 * Created by ym on 2016/09/29.
 */

public class CapturedImageObject extends RealmObject implements Serializable{

    //測定結果:
    //日時・枚数・解析された画像の保存場所・解析された最新の日時と結果

    //ファイルネーム
    private String fileName;

    //1つ1つのデータの撮影時間
    private Date capturedDate;

    //解析回数
    private int numOfAnalysis;


}
