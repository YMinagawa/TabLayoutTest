package jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject;

import java.io.Serializable;
import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by ym on 2016/09/29.
 */

public class AnalysisResultObject extends RealmObject implements Serializable{

    // 解析結果:
    //解析された画像の集合の測定日・解析日時・結果(ヒストグラム、値)

    private Date analyzedDate;

    //一度解析を行ったときの結果（解析した画像、解析時間等)
    private RealmList<CapturedImageObject> analyzedImages;

    private String memo;

    //解析条件の入ったObjectを作成するか？
    //結果はどうやって残す？

    //getter and setter


}
