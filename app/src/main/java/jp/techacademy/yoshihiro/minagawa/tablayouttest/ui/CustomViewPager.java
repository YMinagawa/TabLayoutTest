package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by ym on 2016/10/26.
 */

public class CustomViewPager extends ViewPager{

    private FrameLayout mFrameLayout;
    private View mView_analyze_imageData;

    public CustomViewPager(Context context){
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public void setFrameLayout(FrameLayout frameLayout, View view_analyze_imageData){
        mFrameLayout = frameLayout;
        mView_analyze_imageData = view_analyze_imageData;
    }


    @Override
    //ここでtrueを返すと子供のView(子FrameLayout, 孫LinearLayoutView、ひ孫ImageView)にイベントを伝搬しない。
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if(mFrameLayout !=null) {


            View currentView = mFrameLayout.getChildAt(0);
            if(currentView ==mView_analyze_imageData){
                mFrameLayout.getParent().requestDisallowInterceptTouchEvent(true);
                Log.d("mTypeCustomView", "DisallowInterceptTouch");
                return false;
            }
        }

//        if(ev.getAction() == ev.ACTION_UP){
//              return false;
//        }
        //子のイベントを優先させるため、falseを常に返すようにする

        return super.onInterceptTouchEvent(ev);

    }

    @Override
    protected void onPageScrolled(int position, float offset, int offsetPixels) {

        Log.d("mTestCustomViewPager", "offset = " + offset);
        //OnPageScrolledで1%以上動きがなければ、処理を渡さないようにする。
//        if(offset < 0.05 || 0.99 < offset){
//            if(mFrameLayout != null) {
//                mFrameLayout.getParent().requestDisallowInterceptTouchEvent(true);
//            }
//        }
        super.onPageScrolled(position, offset, offsetPixels);
    }
}
