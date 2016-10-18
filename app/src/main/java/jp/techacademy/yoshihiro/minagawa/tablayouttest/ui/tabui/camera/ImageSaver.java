package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.camera;

import android.content.Context;
import android.media.Image;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by ym on 2016/10/04.
 */

public class ImageSaver implements Runnable {

    private final Image mImage;
    private File mImageFile;
    private Context mContext;

    public ImageSaver(Image image, Context context, File imgfile) {
        mImage = image;
        mContext = context;
        mImageFile = imgfile;
    }

    @Override
    public void run() {
        Log.i("ImageSaver", "ImageSaver---> run");
        ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        FileOutputStream fileOutputStream = null;
        boolean success = false;

        try {
            fileOutputStream = new FileOutputStream(mImageFile);
            fileOutputStream.write(bytes);
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mImage.close();
            closeOutput(fileOutputStream);

            //MediaStoreをupdateする別の方法
            //Uri contentUri = Uri.fromFile(imageFile);
            //Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
            //sendBroadcast(mediaScanIntent);


            if (null != fileOutputStream) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //もしファイルのセーブがうまくいったら、MediaStoreをアップデートする
        if(success){
            MediaScannerConnection.scanFile(mContext, new String[]{mImageFile.getPath()},
                    null, new MediaScannerConnection.MediaScannerConnectionClient() {
                        @Override
                        public void onMediaScannerConnected() {

                        }

                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            Log.i("mTestImageSaver", "Scanned" + path + ":");
                            Log.i("mTestImageSvaer", "-> uri =" + uri);
                        }
                    });
        }

    }

    private static void closeOutput(OutputStream outputStream){
        if(null != outputStream){
            try{
                outputStream.close();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

}