package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.userselect;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.UserObject;

public class UserListRecycleAdapter extends RecyclerView.Adapter<UserListRecycleAdapter.ItemViewHolder> {

    private ArrayList<UserObject> mUserObjectArrayList;

    public static class ItemViewHolder extends RecyclerView.ViewHolder{

        public TextView mUserNameTextView;
        public TextView mAgeTextView;

        public ItemViewHolder(View v){
            super(v);
            mUserNameTextView = (TextView)v.findViewById(R.id.textView_username);
            mAgeTextView = (TextView)v.findViewById(R.id.textView_age);
        }
    }

    public UserListRecycleAdapter(ArrayList<UserObject> userObjectArrayList){
        this.mUserObjectArrayList = userObjectArrayList;
    }

    @Override
    public UserListRecycleAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_users_recycler, parent, false);

        /**
        //要素がクリックされたときの処理
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        //要素が長押しされたときの処理
        v.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View v) {
                return false;
            }
        });
        **/
        return new ItemViewHolder(v);

    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        //super.onBindViewHolder(holder, position, payloads);
        //ここで各要素(アイテム)のテキスト等の設定を行う

        final String name;
        name = mUserObjectArrayList.get(position).getName();
        holder.mUserNameTextView.setText(name);

    }

    @Override
    public int getItemCount() {
        return mUserObjectArrayList.size();
    }

    public ArrayList<UserObject> getUserObjectArrayList(){
        return mUserObjectArrayList;
    }

    //長押しで消すかどうかの判断にする？
    protected void removeFromDataset(String username){
        for(int i=0; i<mUserObjectArrayList.size(); i++){
            if(mUserObjectArrayList.get(i).getName().equals(username)){
                mUserObjectArrayList.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }
}
