//---- Mavis Brace ---//

package layout;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.example.mavis.place.R;

import java.util.Random;


public class savePlace extends DialogFragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public interface savePlaceListener {
        void onFinishSavePlaceDialog(String name, String feelings, String sounds);
    }

    private String[] images = {"bridge1","bridge2","bridge3","bridge4","castle1","castle2","castle3","castle4",
    "house1","house2","house3","house4","jail1","jail2","jail3","jail4","mountain1","mountain1","mountain1","mountain1",
    "ocean1","ocean2","ocean3","ocean4","river1","river2","river3","river4","tree1","tree2","tree3","tree4"};

    private static Integer[] all_drawables = {R.drawable.bridge1,R.drawable.bridge2,R.drawable.bridge3,R.drawable.bridge4,
            R.drawable.castle1,R.drawable.castle2,R.drawable.castle3,R.drawable.castle4,
            R.drawable.house1,R.drawable.house2,R.drawable.house3,R.drawable.house4,
            R.drawable.jail1,R.drawable.jail2,R.drawable.jail3,R.drawable.jail4,
            R.drawable.mountain1,R.drawable.mountain1,R.drawable.mountain1,R.drawable.mountain1,
            R.drawable.ocean1,R.drawable.ocean2,R.drawable.ocean3,R.drawable.ocean4,
            R.drawable.river1,R.drawable.river2,R.drawable.river3,R.drawable.river4,
            R.drawable.tree1,R.drawable.tree2,R.drawable.tree3,R.drawable.tree4};

    private Integer[] drawables;

    private EditText placeName_edit;
    private EditText feelings_edit;
    private EditText sounds_edit;
    private Spinner spinner;

    //-----------------------------//

    public savePlace() {
        // Required -empty- public constructor
    }


    public static savePlace newInstance(String param1, String param2) {
        savePlace fragment = new savePlace();

        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_save_place, container, false);
        getDialog().setTitle("PLACE RECORDED.");

        placeName_edit = (EditText) rootView.findViewById(R.id.placeName_edit);
        feelings_edit = (EditText) rootView.findViewById(R.id.feelings_edit);
        sounds_edit = (EditText) rootView.findViewById(R.id.sounds_edit);
        spinner = (Spinner) rootView.findViewById(R.id.spinner);

        drawables = new Integer[all_drawables.length/4];

        //random image views each time (1 in each 4)
        Random rand = new Random();
        for (int i = 0; i < all_drawables.length/4; i++){
            int r = rand.nextInt(4);
            drawables[i] = all_drawables[(i*4) + r];
        }

        CustomArrayAdapter adapter = new CustomArrayAdapter(getContext(),drawables);

        spinner.setAdapter(adapter);

        Button doneButton = (Button) rootView.findViewById(R.id.continueButton_dialog);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //return input text to activity (hopefully)
                savePlaceListener activity = (savePlaceListener) getActivity();
                activity.onFinishSavePlaceDialog(placeName_edit.getText().toString(),
                        feelings_edit.getText().toString(),
                        sounds_edit.getText().toString());
                dismiss();
            }
        });


        return rootView;
    }


}

class CustomArrayAdapter extends ArrayAdapter<Integer> {
    private Integer[] imgs;

    public CustomArrayAdapter(Context context, Integer[] imgs) {
        super(context, android.R.layout.simple_spinner_item, imgs);
        this.imgs = imgs;
    }

    @Override
    public View getDropDownView(int pos, View convertView, ViewGroup parent){
        return getImageForPosition(pos);
    }
    @Override
    public View getView(int pos, View convertView, ViewGroup parent){
        return getImageForPosition(pos);
    }
    private View getImageForPosition(int pos){
        ImageView imageView = new ImageView(getContext());
        imageView.setBackgroundResource(imgs[pos]);
        imageView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return imageView;
    }

}
