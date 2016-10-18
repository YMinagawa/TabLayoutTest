package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

//自前のRecycleItemClickListener
//既存のListenerではpositionの位置を得られないため
//positionの値が得られるように実装しなおし。
public class CustomRecyclerItemClickListener implements RecyclerView.OnItemTouchListener {

    //protected void onItemClick(View view, int position);

    private GestureDetector mGestureDetector;
    private OnItemClickListener mOnItemClickListener;

    public CustomRecyclerItemClickListener(Context context, final RecyclerView recyclerView, OnItemClickListener listener){
        mOnItemClickListener = listener;
        //mUserObjectArrayList = UserArrayList;
        mGestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View childView = recyclerView.findChildViewUnder(e.getX(), e.getY());
                //引数のlistenerに自分を取るため、
                //長押しされた際には自分のonItemLongClickイベントが起こる
                if(childView != null & mOnItemClickListener != null){

                    mOnItemClickListener.onItemLongClick(childView, recyclerView.getChildAdapterPosition(childView));
                }
            }
        });
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        //タッチした箇所のviewの取得
        View childView = rv.findChildViewUnder(e.getX(), e.getY());
        if(childView != null && mOnItemClickListener != null && mGestureDetector.onTouchEvent(e)){
            childView.setPressed(true);
            mOnItemClickListener.onItemClick(childView, rv.getChildAdapterPosition(childView));
        }

        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    public interface OnItemClickListener{
        public void onItemClick(View view, int position);
        public void onItemLongClick(View view, int position);
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }
}
