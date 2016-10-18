package jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject;

import java.io.Serializable;
import java.util.Date;

import io.realm.RealmList;
import io.realm.RealmObject;

/**
 * Created by ym on 2016/09/30.
 */

public class MeasuredDateAndDataObject extends RealmObject implements Serializable{

    //日時とカメラアプリが起動した時間
    private Date measuredDate;

    //測定した画像の保存先のPath
    private String directoryPath;

    //撮影した画像のデータ
    //日時毎に保存する
    private RealmList<CapturedImageObject> capturedImages;
    //解析した結果の格納も日時毎に保存する
    private RealmList<AnalysisResultObject> analysisResults;

    //任意で書き込み可能なメモの保存
    private String memo;

    //封入後の経過時間
    private Date ElapsedTime;

    //ISOの値
    private int iso;

    //ExposureTimeの値
    private float exposureTime;


    //Getter and Setter
    //日時
    public Date getMeasuredDate() {
        return measuredDate;
    }

    public void setMeasuredDate(Date measuredDate){
        this.measuredDate = measuredDate;
    }

    //Getter and Setter
    //imageDirectory
    public String getDirectoryPath(){
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath){
        this.directoryPath = directoryPath;
    }

    //Getter and Setter
    //captureImaged
    public RealmList<CapturedImageObject> getCapturedImages(){
        return capturedImages;
    }

    public void setCapturedImages(RealmList<CapturedImageObject> capturedImages){
        this.capturedImages = capturedImages;
    }

    //Getter and Setter
    //AnalyzedResult
    public RealmList<AnalysisResultObject> getAnalysisResults(){
        return analysisResults;
    }

    public void setAnalysisResults(RealmList<AnalysisResultObject> analysisResults){
        this.analysisResults = analysisResults;
    }

    //Getter and Setter
    //ISO
    public int getISO(){
        return iso;
    }

    public void setISO(int iso){
        this.iso = iso;
    }

    //Getter and Setter
    //ExposureTime
    public float getExposureTime(){
        return exposureTime;
    }

    public void setExposureTime(float exposureTime){
        this.exposureTime = exposureTime;
    }


}
