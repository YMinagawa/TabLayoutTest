package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.analyzedata;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;

import io.realm.RealmList;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.CapturedImageObject;

/**
 * Created by ym on 2016/10/15.
 */

public class ImageDataListRecyclerAdapter
        extends RecyclerView.Adapter<ImageDataListRecyclerAdapter.ItemViewHolder>{


    private static final int TYPE_ITEM = 1;
    private static final int TYPE_FOOTER = 2;


    private RealmList<CapturedImageObject> mCapturedImageList;
    public static View mCardView;

    public static class ItemViewHolder extends RecyclerView.ViewHolder{


        public ImageView mImageView;

        public ItemViewHolder(View itemView) {
            super(itemView);
            mImageView = (ImageView) itemView.findViewById(R.id.imageView_measuredata);
            mCardView = itemView;


        }
    }

    //コンストラクタ
    public ImageDataListRecyclerAdapter(RealmList<CapturedImageObject> capturedImageList){
        this.mCapturedImageList = capturedImageList;
    }

    @Override
    public ImageDataListRecyclerAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.listcard_image_data_recycler, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        File imageFile = new File(mCapturedImageList.get(position).getFilePath());
        //実際に表示するサイズ(ImageView)より大きいBitmapを読み込むとメモリの無駄(重くなる)
        //ので、効率的に読み込む(小さくして読み込む)
        BitmapFactory.Options options = new BitmapFactory.Options();
        //inJustDecodeBoundsをtrueにすると、Bitmapがメモリに展開されない。
        //代わりにoutHeight, outWidth, outMimeTypeプロパティに読み込んだ画像の情報がセットされる。
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

        //画像のサイズがわかったので、縮小して読み込むのかそのまま読み込むか決める
        //大体ImageViewに合わせるのが良い
        //BitmapFactory.optionsのinSampleSizeに1より大きい整数値を指定する
        //大元のサイズ/inSampleSizeになる
        //inSampleSizeは別途メソッド(calculateSampleSize)内で計算する

        //・・・が、mImageViewの大きさが0になる・・画像が当てはまるまで決まらない？
        //ので、全体のCardViewのHeightとWidthを使って計算する。
        mCardView.measure(mCardView.getMeasuredWidth(), mCardView.getMeasuredHeight());
        int height = mCardView.getMeasuredHeight();
        int width = mCardView.getMeasuredWidth();
        int inSampleSize = calculateSampleSize(options, width, height);

        Log.d("mImageDataList", "inSampleSize = " + inSampleSize);
        //inSampleSizeをセットしてデコードする
        options.inJustDecodeBounds =   false;
        //先程計算した縮尺値を指定
        options.inSampleSize = inSampleSize;
        Bitmap imageBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);
        holder.mImageView.setImageBitmap(imageBitmap);



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
