package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.camera;

import android.hardware.SensorEvent;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;

/**
 * Created by ym on 2016/10/05.
 */

public class AnimationController {

    boolean isLandScape = false;
    float touchY;
    long ANIM_ROTATE_SPEED = 200;



    public void rotateScreen(SensorEvent event, ImageButton[] img){

        //event.valuesはY軸の傾きの値
        if(!isLandScape && event.values[2]>60 && event.values[2] < 80) {

            for (ImageButton target : img) {
                RotateAnimation rotate = new RotateAnimation(0, 90, target.getWidth()/2, target.getHeight()/2);
                rotate.setDuration(ANIM_ROTATE_SPEED);
                rotate.setFillAfter(true);
                target.startAnimation(rotate);
            }

            isLandScape = true;
        }

        if(isLandScape && event.values[2] > -10 && event.values[2] < 10){

            for (ImageButton target : img) {
                RotateAnimation rotate = new RotateAnimation(90, 0, target.getWidth()/2, target.getHeight()/2);
                rotate.setDuration(ANIM_ROTATE_SPEED);
                rotate.setFillAfter(true);
                target.startAnimation(rotate);
            }

            isLandScape = false;

        }
    }






}
