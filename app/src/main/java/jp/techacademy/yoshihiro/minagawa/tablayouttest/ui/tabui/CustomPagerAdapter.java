package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Created by ym on 2016/10/04.
 */

public class CustomPagerAdapter extends PagerAdapter{

    List<View> mViewList;
    String[] mPageTitles;

    public CustomPagerAdapter(List<View> list, String[] pageTitles){
        this.mViewList = list;
        this.mPageTitles = pageTitles;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        //コンテナに指定のページを追加
        container.addView(mViewList.get(position));
        return mViewList.get(position);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View)object);
    }

    @Override
    public int getCount() {
        return mViewList.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mPageTitles[position];
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }
}
