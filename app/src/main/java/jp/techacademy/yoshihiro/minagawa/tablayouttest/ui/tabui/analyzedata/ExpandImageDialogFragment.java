package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.analyzedata;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.File;

import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;

import static android.graphics.BitmapFactory.decodeFile;

public class ExpandImageDialogFragment extends DialogFragment {

    public static ExpandImageDialogFragment newInstance(String pathName){

        ExpandImageDialogFragment instance = new ExpandImageDialogFragment();
        //ダイアログに渡すパラメーターはBundleにまとめる
        Bundle arguments = new Bundle();
        arguments.putString("pathname", pathName);
        instance.setArguments(arguments);
        return instance;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Dialog dialog = new Dialog(getActivity());

        //タイトル非表示
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        //フルスクリーン
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        dialog.setContentView(R.layout.fragment_expand_image_dialog);
        //背景を透明にする
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        String pathName = getArguments().getString("pathname");
        File imageFile = new File(pathName);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = 3;
        decodeFile(imageFile.getAbsolutePath(), options);
        Bitmap imageBitmap = decodeFile(imageFile.getAbsolutePath(), options);

        RelativeLayout rl = (RelativeLayout)dialog.findViewById(R.id.rl_expandimage);
        rl.measure(rl.getMeasuredWidth(), rl.getMeasuredHeight());
        //final int height = rl.getMeasuredHeight();
        final int width = rl.getMeasuredWidth();

        //final int height = options.outHeight;
        //final int width = options.outWidth;

        ImageView expandImageView = (ImageView)dialog.findViewById(R.id.imageView_expand);
        expandImageView.setMaxHeight(width*options.outHeight/options.outWidth);

        expandImageView.setImageBitmap(imageBitmap);

        //OKボタンのリスナー
        //dialog.findViewById(R.id.btn_df_positive).setOnClickListener(new View.OnClickListener() {
        //    @Override
        //    public void onClick(View v) {

        //    }
        //});

        //Closeボタンのリスナー
        dialog.findViewById(R.id.btn_close).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                ExpandImageDialogFragment.this.dismiss();
            }
        });

        return dialog;
    }

    private int calculateSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight){

        //画像の元サイズ
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if(height>reqHeight || width > reqWidth){
            if(width > height){
                inSampleSize = (int)Math.floor((float)height/(float)reqHeight);
            }else{
                inSampleSize = (int)Math.floor((float)width/(float)reqWidth);
            }
        }

        return inSampleSize;
    }
}
