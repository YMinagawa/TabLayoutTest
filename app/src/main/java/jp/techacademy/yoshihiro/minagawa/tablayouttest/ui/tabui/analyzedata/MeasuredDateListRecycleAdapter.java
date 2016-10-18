package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.analyzedata;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.realm.RealmList;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.MeasuredDateAndDataObject;

/**
 * Created by ym on 2016/10/01.
 */

public class MeasuredDateListRecycleAdapter
        extends RecyclerView.Adapter<MeasuredDateListRecycleAdapter.ItemViewHolder>{

    private RealmList<MeasuredDateAndDataObject> mMeasuredDateAndDataObjectList;

    public static class ItemViewHolder extends RecyclerView.ViewHolder{

        public TextView mCapturedDateTextView;
        public TextView mISOTextView;
        public TextView mExpTimeTextView;
        public TextView mNumOfImageTextView;

        public ItemViewHolder(View v){
            super(v);
            mCapturedDateTextView = (TextView)v.findViewById(R.id.textView_measureddate);
            mISOTextView = (TextView)v.findViewById(R.id.textView_iso);
            mExpTimeTextView = (TextView)v.findViewById(R.id.textView_exptime);
            mNumOfImageTextView = (TextView)v.findViewById(R.id.textView_imagenumber);
        }
    }

    //コンストラクタ
    public MeasuredDateListRecycleAdapter(RealmList<MeasuredDateAndDataObject> MeasuredDateAndDataObjectList){
        this.mMeasuredDateAndDataObjectList = MeasuredDateAndDataObjectList;
    }

    @Override
    public MeasuredDateListRecycleAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_measure_date_recycler, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        //ここで各要素(アイテム)のテキスト等の設定を行う
        Date measuredDate = mMeasuredDateAndDataObjectList.get(position).getMeasuredDate();
        int iso = mMeasuredDateAndDataObjectList.get(position).getISO();
        float expTime = mMeasuredDateAndDataObjectList.get(position).getExposureTime();
        int numOfImage = mMeasuredDateAndDataObjectList.get(position).getCapturedImages().size();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd  HH:mm");
        holder.mCapturedDateTextView.setText(sdf.format(measuredDate));
        holder.mISOTextView.setText("ISO : " + iso);
        //すでにmsになっているので1e6で÷必要なし
        holder.mExpTimeTextView.setText(String.format("ExposureTime : %.3f ms", expTime));
        holder.mNumOfImageTextView.setText("Image : " + numOfImage);

    }

    @Override
    public int getItemCount() {
        return mMeasuredDateAndDataObjectList.size();
    }
}
