package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.analyzedata;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.MeasuredDateAndDataObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.realmobject.UserObject;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.MyItemDecoration;
import jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.MyRecyclerItemClickListener;


public class SelectMeasuredDateFragment extends Fragment {

    private static final String ARG_USER_ID = "userid";

    private UserObject mUserObject;
    private int mId;
    private OnFragmentInteractionListener mListener;

    //RecycledViewのメンバ変数
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private RealmList<MeasuredDateAndDataObject> mMeasuredDateAndDataObjectsList;


    public SelectMeasuredDateFragment() {
        // Required empty public constructor
    }

    public static SelectMeasuredDateFragment newInstance(int userid) {
        SelectMeasuredDateFragment fragment = new SelectMeasuredDateFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null){
            mId = getArguments().getInt(ARG_USER_ID);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        //realmからuseridを用いてUserObjectを取得する
        Realm realm = Realm.getDefaultInstance();
        RealmResults<UserObject> userRealmResults = realm.where(UserObject.class).equalTo("id", mId).findAll();
        realm.close();
        mUserObject = userRealmResults.get(0);

        //レイアウトの設定
        View view = inflater.inflate(R.layout.fragment_select_measureddate, container, false);

        //RecyclerViewの設定
        mRecyclerView = (RecyclerView)view.findViewById(R.id.recyclerView_selectcapdate);
        //mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);
        //RecyclerViewにItemDecorationをセットする
        mRecyclerView.addItemDecoration(new MyItemDecoration(getActivity()));

        mAdapter = new MeasuredDateListRecycleAdapter(mUserObject.getMeasuredDateAndDataList());
        mRecyclerView.setAdapter(mAdapter);

        //独自に作成したRecyclerItemOnClickListenerを実装する
        mRecyclerView.addOnItemTouchListener(
                new MyRecyclerItemClickListener(getActivity(), mRecyclerView, new MyRecyclerItemClickListener.OnItemClickListener(){
                    @Override
                    public void onItemClick(View view, int position) {
                        Log.d("mTestSelectMeasured", "normal click");
                    }

                    @Override
                    public void onItemLongClick(View view, int position) {
                        Log.d("mTestSelectMeasured", "long click");
                    }
                })
        );

        //mAdapter = new CapturedDateListRecycleAdapter()
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
