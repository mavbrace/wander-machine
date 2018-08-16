//--- Mavis Brace ---//

package com.example.mavis.place;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;

public class recordedPlaces extends AppCompatActivity implements View.OnClickListener {

    private String FOLDER_NAME = "PLACE_DATA";
    private String FILE_NAME = "placeData";

    Button back_button;
    TextView statusText;

    File root;

    //--display--//
    String [] place_names;
    RelativeLayout rl;
    //List<int[]> positions = new ArrayList<int[]>; //list of int arrays (note: each has 2 elements: x,y)

    //-- drawing the lines --//
    ImageView imageView_lines;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;

    int num_of_places = 0;
    int past_x;
    int past_y;

    int light_colour = 255;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorded_places);

        back_button = (Button) this.findViewById(R.id.back_button);
        back_button.setOnClickListener(this);

        root = getPublicAlbumStorageDir();

        statusText = (TextView) this.findViewById(R.id.place_status_text);
        statusText.setTextSize(20);

        imageView_lines = (ImageView) this.findViewById(R.id.line_visual);
        bitmap = Bitmap.createBitmap(800,1200, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.WHITE);
        imageView_lines.setImageBitmap(bitmap);

        initDecorate();

    }

    @Override
    protected void onStart() {
        super.onStart();

        int numFiles = root.list().length;

        if (numFiles > num_of_places){
            //draw the new text / shapes
            decorate(numFiles);
        }

        statusText.setText("I've found " + Integer.toString(numFiles) + " place" + pluralize(numFiles) + " so far.");

        num_of_places = numFiles;
    }

    public void decorate(int num_of_files){

        String [] place_names_temp = new String[num_of_files];

        for (int i = 0; i < num_of_places; i++){
            place_names_temp[i] = place_names[i];
        }

        int x;
        int y;

        for (int i = num_of_places; i < num_of_files; i++){
            try{
                x = new Random().nextInt(imageView_lines.getWidth());
                y = new Random().nextInt(imageView_lines.getHeight());
            } catch (IllegalArgumentException e){
                e.printStackTrace();
                x = new Random().nextInt(600);
                y = new Random().nextInt(1000);
            }
            place_names_temp[i] = readFile(i);
            drawEverything(place_names_temp[i], x, y);
            past_x = x;
            past_y = y;
        }

        place_names = place_names_temp;
    }

    public void initDecorate(){
        int numFiles = root.list().length;
        num_of_places = numFiles;
        rl = (RelativeLayout) findViewById(R.id.activity_recorded_places);
        place_names = new String [numFiles];

        int x = 0;
        int y = 0;

        for (int i = 0; i < numFiles; i++){
            past_x = x;
            past_y = y;
            try{
                x = new Random().nextInt(imageView_lines.getWidth());
                y = new Random().nextInt(imageView_lines.getHeight());
            } catch (IllegalArgumentException e){
                e.printStackTrace();
                x = new Random().nextInt(600);
                y = new Random().nextInt(1000);
            }
            place_names[i] = readFile(i);
            drawEverything(place_names[i], x, y);
        }

        past_x = x;
        past_y = y;

        statusText.setText("I've found " + Integer.toString(numFiles) + " place" + pluralize(numFiles) + " so far.");

    }

    public void drawEverything(String name, int x, int y){
        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setBackgroundColor(0xFF343741);
        tv.setTextColor(0xFFC0C9E8);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = x;
        params.topMargin = y;
        tv.setLayoutParams(params);

        //draw line + shape
        drawLine(past_x, past_y, x, y);

        //draw text
        if (rl != null){
            rl.addView(tv, params);
            Log.d("hey","added text");
        } else {
            Log.d("hey","relative layout is null.");
        }

    }


    //line AND shape
    public void drawLine(int x1, int y1, int x2, int y2){
        //canvas.drawColor(0x50545F); // refresh canvas (paint over old lines)

        paint.setColor(Color.WHITE);
        canvas.drawLine(x1, y1, x2, y2, paint);

        paint.setColor(Color.rgb(light_colour,light_colour,light_colour));
        canvas.drawCircle(x2, y2, 40, paint);

        imageView_lines.invalidate();
    }

    public void onClick(View view){
        //go back to main screen
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    //TODO: line colour = illuminations, line 1 (averaged light)
    public String readFile(int whichFile){
        String name = "...";
        try {
            FileInputStream is = new FileInputStream(root + File.separator + FILE_NAME + Integer.toString(whichFile) + ".txt");
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr, 8192);
            String line;
            int counter = 0;
            while (true){
                //line 4 = user input strings, value 0 = name
                line = br.readLine();
                if (line == null) break;
                // GRAB OUR INFORMATION
                //illumination
                if (counter == 1){
                    String[] lights = line.split(",");
                    if (lights.length > 1){
                        light_colour = averagedLight(lights);
                    }
                }
                //name
                if (counter == 4){
                    String[] user_inputs = line.split(",");
                    if (user_inputs.length > 1){
                        name = user_inputs[0];
                    }
                }
                counter = counter + 1;
            }
            isr.close();
            is.close();
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.d("hey", "File not found.");
        } catch (IOException e) {
            e.printStackTrace();
        }

        return name;

    }

    public int averagedLight(String[] lights){
        if (lights == null){
            return 255;
        }
        if (lights.length == 0) {
            return 255;
        }
        double total = 0.0;
        int count = 0;
        for (int i = 0; i < lights.length; i++){
            try {
                double light = Double.parseDouble(lights[i]);
                if (light != 0){
                    total = total + Double.parseDouble(lights[i]);
                    count++;
                }
            } catch (NumberFormatException e){
                //uh oh.....
            }
        }
        double light_val = (total / count);
        if (light_val > 1.0f){
            light_val = 1.0f;
        }
        return (int) (light_val * 255.0);
    }

    //this is a copy of the method within MainActivity (should eventually fix that)
    public File getPublicAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File root = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), FOLDER_NAME);
        if (!root.mkdirs()) {
            Log.d("hey", "Directory not created.");
        }
        return root;
    }


    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public String pluralize(int number){
        if (number == 1){
            return "";
        } else {
            return "s";
        }
    }

}
