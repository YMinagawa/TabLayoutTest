package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.camera;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;

/**
 * Created by ym on 2016/10/04.
 */

public interface CameraInterface {
    SurfaceTexture getSurfaceTextureFromTextureView();
    Size getPreviewSize();
    Handler getBackgroundHandler();
    Surface getImageRenderSurface();
    int getRotation();
}

