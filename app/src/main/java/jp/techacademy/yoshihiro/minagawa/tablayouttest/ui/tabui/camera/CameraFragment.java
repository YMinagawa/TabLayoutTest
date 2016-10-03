package jp.techacademy.yoshihiro.minagawa.tablayouttest.ui.tabui.camera;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import jp.techacademy.yoshihiro.minagawa.tablayouttest.R;

public class CameraFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM = "Camera";
    private String mParam;
    private OnFragmentInteractionListener mListener;


    //fragmentのコンストラクタはpublicで無修正なやつじゃないと怒られるらしい。
    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        //Bundle args = new Bundle();
        //args.putString(ARG_PARAM1, param1);
        //args.putString(ARG_PARAM2, param2);
        //fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

        //FloatingActionButtonの設定
        FloatingActionButton fab_camera = (FloatingActionButton)view.findViewById(R.id.fab_camera);
        fab_camera.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CameraActivity.class);
                startActivity(intent);
            }
        });

        //スピナーの文字サイズを変えるための設定(Adapterを用意してspinnerにセットする)
        ArrayAdapter<String> adapterHour = new ArrayAdapter<String>(getActivity(), R.layout.spinner_item_design_for_cam, this.getResources().getStringArray(R.array.hour_list));
        ArrayAdapter<String> adapterMin = new ArrayAdapter<String>(getActivity(), R.layout.spinner_item_design_for_cam, this.getResources().getStringArray(R.array.min_list));
        adapterHour.setDropDownViewResource(R.layout.spinner_dropdown_item_for_com);
        adapterMin.setDropDownViewResource(R.layout.spinner_dropdown_item_for_com);

        Spinner spinnerHour = (Spinner)view.findViewById(R.id.spinner_hour);
        Spinner spinnerMin = (Spinner)view.findViewById(R.id.spinner_min);

        spinnerHour.setAdapter(adapterHour);
        spinnerMin.setAdapter(adapterMin);

        //スピナーのアイテムが選択されたときの動作を設定
        spinnerHour.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //スピナーでは呼ばれないが、消せないので「おまじない」
            }
        });

        spinnerMin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //スピナーでは呼ばれないが、消せないので「おまじない」
            }
        });



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
