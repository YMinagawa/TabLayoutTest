package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.analyzedata;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.MeasuredDateAndDataObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.UserObject;

/**
 * Created by ym on 2016/11/04.
 */

public class DeleteMeasuredDateDialogFragment extends DialogFragment {

    public static DeleteMeasuredDateDialogFragment newInstance(String userName, Date measuredDate){

        DeleteMeasuredDateDialogFragment instance = new DeleteMeasuredDateDialogFragment();
        //ダイアログに渡すパラメーターはBundleにまとめる
        Bundle arguments = new Bundle();
        arguments.putString("UserName", userName);
        arguments.putSerializable("MeasuredDate", measuredDate);
        instance.setArguments(arguments);
        return instance;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = new Dialog(getActivity());

        final String userName = getArguments().getString("UserName");
        final Date measuredDate = (Date)getArguments().getSerializable("MeasuredDate");

        //タイトル非表示
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        //フルスクリーン
        //dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        dialog.setContentView(R.layout.fragment_measureddate_delete_dialog);
        //背景を透明にする
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        TextView textViewMeasuredDate = (TextView)dialog.findViewById(R.id.textView_measureddate);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd  HH:mm");
        textViewMeasuredDate.setText(sdf.format(measuredDate));

        Button btnDeletePos = (Button)dialog.findViewById(R.id.btn_delete_positive);
        btnDeletePos.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                removeDateFromDataList(userName, measuredDate);
                DeleteMeasuredDateDialogFragment.this.getDialog().dismiss();
            }
        });

        //Closeボタンのリスナー
        dialog.findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                DeleteMeasuredDateDialogFragment.this.getDialog().dismiss();
            }
        });

        return dialog;
    }

    private void removeDateFromDataList(String userName, Date measuredDate){
        //一応ユーザーオブジェクトから検索をかけて、相当する日付を削除する
        Realm realm = Realm.getDefaultInstance();
        RealmResults<UserObject> userRealmResults = realm.where(UserObject.class).equalTo("name", userName).findAll();
        UserObject userObject = userRealmResults.get(0);
        RealmList<MeasuredDateAndDataObject> dateAndDataList =  userObject.getMeasuredDateAndDataList();
        realm.beginTransaction();
        for(int i = 0; i < dateAndDataList.size(); i++){
            if(dateAndDataList.get(i).getMeasuredDate().equals(measuredDate)){
                dateAndDataList.remove(i);
                break;
            }
        }

        //RealmResults<MeasuredDateAndDataObject> measuredDateRealmResults = realm.where(MeasuredDateAndDataObject.class).equalTo("measuredDate", measuredDate).findAll();
        //MeasuredDateAndDataObject measuredDateAndDataObject = measuredDateRealmResults.get(0);
        realm.copyToRealmOrUpdate(userObject);
        realm.commitTransaction();
        realm.close();
    }
}
