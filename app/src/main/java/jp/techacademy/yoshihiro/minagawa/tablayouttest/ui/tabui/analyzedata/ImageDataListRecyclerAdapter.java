package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.analyzedata;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmList;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.CapturedImageObject;

import static android.graphics.BitmapFactory.decodeFile;

/**
 * Created by ym on 2016/10/15.
 */

public class ImageDataListRecyclerAdapter
        extends RecyclerView.Adapter<ImageDataListRecyclerAdapter.ItemViewHolder>{

    private static RealmList<CapturedImageObject> mCapturedImageList;
    public static View mCardView;
    public static Activity tabMainActivity;
    public static ArrayList<Bitmap> mBitmapList;

    public static class ItemViewHolder extends RecyclerView.ViewHolder{

        public ImageView mImageView;
        public TextView mTextView;
        public CheckBox mCheckBox;

        public ItemViewHolder(View itemView) {

            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.imageView_measuredata);
            mTextView = (TextView)itemView.findViewById(R.id.textView_imagenumber);
            mCheckBox = (CheckBox)itemView.findViewById(R.id.checkbox_analyze);
            mCardView = itemView;

            mImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    int position = Integer.parseInt((mTextView.getText()).toString()) - 1;

                    //File imageFile = new File(mCapturedImageList.get(position).getFilePath());
                    //BitmapFactory.Options options = new BitmapFactory.Options();
                    //options.inJustDecodeBounds = false;
                    //decodeFile(imageFile.getAbsolutePath(), options);
                    //Bitmap imageBitmap = decodeFile(imageFile.getAbsolutePath(), options);
                    ExpandImageDialogFragment exImageDialogFragment = ExpandImageDialogFragment.newInstance(mCapturedImageList.get(position).getFilePath());
                    exImageDialogFragment.show(tabMainActivity.getFragmentManager(), "ExpandImageDialogFragment");
                }
            });

            mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    int position = Integer.parseInt((mTextView.getText()).toString()) -1;
                    //Realmを更新してcheck状況を保存する
                    Realm realm = Realm.getDefaultInstance();
                    realm.beginTransaction();
                    mCapturedImageList.get(position).setIsChecked(mCheckBox.isChecked());
                    realm.commitTransaction();
                    realm.close();
                }
            });
        }
    }

    //コンストラクタ
    public ImageDataListRecyclerAdapter(RealmList<CapturedImageObject> capturedImageList, Activity tabMainActivity){
        this.mCapturedImageList = capturedImageList;
        this.tabMainActivity = tabMainActivity;
        this.mBitmapList = new ArrayList<Bitmap>(mCapturedImageList.size());

        for(int i = 0; i < mCapturedImageList.size(); i++){
            File imageFile = new File(mCapturedImageList.get(i).getFilePath());
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            options.inSampleSize = 4;
            Bitmap imageBitmap = decodeFile(imageFile.getAbsolutePath(), options);
            mBitmapList.add(imageBitmap);
        }
    }

    @Override
    public ImageDataListRecyclerAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.listcard_image_data_recycler, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {

        //File imageFile = new File(mCapturedImageList.get(position).getFilePath());
        //実際に表示するサイズ(ImageView)より大きいBitmapを読み込むとメモリの無駄(重くなる)
        //ので、効率的に読み込む(小さくして読み込む)
        //BitmapFactory.Options options = new BitmapFactory.Options();
        //inJustDecodeBoundsをtrueにすると、Bitmapがメモリに展開されない。
        //代わりにoutHeight, outWidth, outMimeTypeプロパティに読み込んだ画像の情報がセットされる。
        //options.inJustDecodeBounds = true;

        //decodeFile(imageFile.getAbsolutePath(), options);

        //画像のサイズがわかったので、縮小して読み込むのかそのまま読み込むか決める
        //大体ImageViewに合わせるのが良い
        //BitmapFactory.optionsのinSampleSizeに1より大きい整数値を指定する
        //大元のサイズ/inSampleSizeになる
        //inSampleSizeは別途メソッド(calculateSampleSize)内で計算する

        //・・・が、mImageViewの大きさが0になる・・画像が当てはまるまで決まらない？
        //ので、全体のCardViewのHeightとWidthを使って計算する。
        //mCardView.measure(mCardView.getMeasuredWidth(), mCardView.getMeasuredHeight());
        //int height = mCardView.getMeasuredHeight();
        //int width = mCardView.getMeasuredWidth();
        //int inSampleSize = calculateSampleSize(options, width, height);

        //openCVテスト用
        holder.mImageView.setImageBitmap(mBitmapList.get(position));

        //TextViewに画像のナンバーを入力
        holder.mTextView.setText(String.valueOf(position+1));

        //CheckBoxの設定
        boolean checked = mCapturedImageList.get(position).getChecked();
        holder.mCheckBox.setChecked(checked);

    }

    @Override
    public int getItemCount() {
        return mCapturedImageList.size();
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
