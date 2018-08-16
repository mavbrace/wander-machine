//--- Mavis Brace ----//

package com.example.mavis.place;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import ca.uol.aig.fftpack.*;
import layout.savePlace;


public class MainActivity
        extends AppCompatActivity implements SensorEventListener, View.OnClickListener, savePlace.savePlaceListener {

    private SensorManager sensor_manager;

    private TextView amplitude_text;

    private Sensor light_sensor;
    private float light_max;
    private TextView light_text;

    int MAX_ILLUMINATIONS = 256; // set this number however you want
    int illumination_index = 0;
    double [] illuminations;

    //-[ sound ]-//
    int freq = 8000;
    int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private ca.uol.aig.fftpack.RealDoubleFFT transformer;
    int blockSize = 256;

    double[] averagedFS; //averaged frequency spectrum (ongoing average)
    double currentMaxAmplitude;

    Button startStopButton;
    boolean started = false;
    TextView status_text;
    private String FOLDER_NAME = "PLACE_DATA";
    private String FILE_NAME = "placeData";

    Button moreInfoButton;

    ImageView imageView_overlay;

    ImageView imageView_room;
    Bitmap room_bitmap;
    Canvas room_canvas;
    Paint room_paint;

    RecordAudio recordTask;

    int paint_colour = Color.BLACK;

    ImageView imageView_visual;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;

    RelativeLayout rl;
    //-----------//

    private String current_place_name;
    private String current_place_feelings;
    private String current_place_sounds;

    int[] sound_points_x;
    int[] sound_points_y;

    //----------//

    Animation aniRotate;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        moreInfoButton = (Button) findViewById(R.id.more_info_button);
        moreInfoButton.setOnClickListener(this);

        status_text = (TextView) findViewById(R.id.status_text);

        sensor_manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        light_text = (TextView) findViewById(R.id.info_light);
        amplitude_text = (TextView) findViewById(R.id.info_amp);

        light_sensor = sensor_manager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (light_sensor == null) {
            light_text.setText("null - [error]");
            light_max = 0.0f;
        } else {
            light_max = light_sensor.getMaximumRange();
        }

        // ---VISUALS---- //
        //imageView_room = (ImageView) this.findViewById(R.id.gather_img);
        imageView_overlay = (ImageView) findViewById(R.id.gather_img);
        imageView_room = (ImageView) this.findViewById(R.id.circle_graphic);
        room_bitmap = Bitmap.createBitmap(1024,1024,Bitmap.Config.ARGB_8888);
        room_canvas = new Canvas(room_bitmap);
        room_paint = new Paint();
        room_paint.setColor(Color.BLACK);
        imageView_room.setImageBitmap(room_bitmap);

        rl = (RelativeLayout) findViewById(R.id.activity_main);


        //---AUDIO---//
        startStopButton = (Button) this.findViewById(R.id.start_stop_button);
        startStopButton.setOnClickListener(this);

        transformer = new RealDoubleFFT(blockSize);

        imageView_visual = (ImageView) this.findViewById(R.id.visual);
        bitmap = Bitmap.createBitmap(250,250, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.WHITE);
        imageView_visual.setImageBitmap(bitmap);

        sound_points_x = new int[blockSize];
        sound_points_y = new int[blockSize];
        Random rand = new Random();
        for (int i = 0; i < blockSize; i++){
            sound_points_x[i] = rand.nextInt(250);
            sound_points_y[i] = rand.nextInt(250);
        }
        //---AUDIO STUFF end---//

        current_place_name = "";
        current_place_feelings = "";
        current_place_sounds = "";

        //----animation---//

        aniRotate = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate);
        aniRotate.setAnimationListener(new Animation.AnimationListener() {

            //------ANIMATION-------//
            @Override
            public void onAnimationEnd(Animation anim){
                if (started) {
                    //only stop observing if we haven't already stopped observing
                    stopObserving();
                }
            }

            @Override
            public void onAnimationRepeat(Animation anim){
                //leave this empty
            }
            @Override
            public void onAnimationStart(Animation anim){
                //leave this empty
            }
            //----ANIMATION [end]-----//

        });

        Log.d("hey","ONCREATE completed successfully.");

    }

    @Override
    protected void onStart() {
        super.onStart();
        //startTimer();
        if (light_sensor != null) {
            sensor_manager.registerListener(this, light_sensor, sensor_manager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensor_manager.unregisterListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stopTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //startTimer();
    }


    //whenever a sensor reading shifts, this method is called
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //called when new sensor data is available
        int sensorType = sensorEvent.sensor.getType();
        float currentVal = sensorEvent.values[0];
        if (sensorType == Sensor.TYPE_LIGHT) {
            //light_text.setText("AMBIENT LIGHT : " + Float.toString(currentVal));
            float light_amount = currentVal / 5000.0f; //light_max
            if (light_amount > 1.0f){
                light_amount = 1.0f;
            }
            //int colour= (Integer) new ArgbEvaluator().evaluate(light_amount, 0x474D5B, 0x7A839B);
            light_text.setText("AMBIENT LIGHT MAX : " + Float.toString(light_amount));
            int colour = (int) (light_amount * 255.0f);
            //scenery.setBackgroundColor(Color.rgb(colour,colour,colour+20));
            room_paint.setColor(Color.rgb(colour,colour,colour+1));
            paint_colour = Color.rgb(colour,colour,colour+1);

            if (started){
                illuminations[illumination_index] = light_amount;
                illumination_index = illumination_index + 1;
                if (illumination_index >= MAX_ILLUMINATIONS){
                    //TODO: stop. For now just overwrites forever.
                    illumination_index = 0;
                }
            }
        }
    }

    //w = width = height    currently unused :)
    public void drawCircleGraphic(int w){
        //VISUALIZER//
        room_canvas.drawColor(Color.TRANSPARENT); //?
        room_canvas.drawCircle(w/2,w/2,w*2,room_paint);
        imageView_room.invalidate();
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //this can be left empty :)
    }

    //-------------BUTTON CLICK-------------//
    //on button press, this method is called!
    public void onClick(View view) {

        switch (view.getId()) {
            case R.id.more_info_button:
                Intent intent = new Intent(this, recordedPlaces.class);
                startActivity(intent);
                break;
            case R.id.start_stop_button:
                if (started) {
                    //stopping!
                    stopObserving();
                } else {
                    //GO!
                    started = true;
                    //Animation aniRotate = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.rotate);
                    imageView_overlay.startAnimation(aniRotate);
                    startStopButton.setText("STOP");
                    illuminations = new double[MAX_ILLUMINATIONS];
                    illumination_index = 0;
                    recordTask = new RecordAudio();
                    recordTask.execute();
                }
            default:
                break;
        }
    }

    public void stopObserving(){
        started = false;
        startStopButton.setText("START");
        recordTask.cancel(true);
        //SHOW DIALOG BOX!
        showPlaceDialog();
    }

    //--------------BUTTON CLICK end-------------//


    //-------[ DIALOG ]-------//
    public void showPlaceDialog(){
        FragmentManager fm = getSupportFragmentManager();
        savePlace savePlaceDialog = new savePlace();
        savePlaceDialog.show(fm, "hi");
    }

    @Override
    public void onFinishSavePlaceDialog(String name_text, String feelings_text, String sounds_text){
        this.current_place_name = name_text;
        this.current_place_feelings = feelings_text;
        this.current_place_sounds = sounds_text;

        //write to file!
        if (isExternalStorageWritable()){
            writeToFile();
        } else {
            status_text.setText("STATUS: external storage NOT writeable!");
        }
        Toast.makeText(this, name_text + " recorded!", Toast.LENGTH_SHORT).show();
    }
    //------[ DIALOG end ]--------//


    //----------file stuff------------//
    //check if external storage is available for read and write
    public boolean isExternalStorageWritable(){
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }


    public File getPublicAlbumStorageDir() {
        // Get the directory for the user's public pictures directory.
        File root = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), FOLDER_NAME);
        if (!root.mkdirs()) {
            Log.d("hey", "Directory not created");
            status_text.setText("STATUS: directory not created?");
        }
        return root;
    }

    /*
    [-----format!----]
    [----FILE START--]
    [0 date
    [1 illumination - csv
    [2 frequency analysis - csv [256]
    [3 maximum amplitude
    [4 user-input strings - csv [name, feelings, sounds]
    [5 drop-down menu choice
    [----FILE END----]
     */
    public void writeToFile(){
        //storing information from averagedFS
        File root = getPublicAlbumStorageDir();
        //find an available filename (files listed sequentially)
        String [] allFiles = root.list();
        //only using this info to name the new file, though
        File file = new File(root, FILE_NAME + Integer.toString(allFiles.length) + ".txt");
        try{
            FileWriter writer = new FileWriter(file, true); // true = do append
            //LINE 0 = DATE
            Date currentTime = Calendar.getInstance().getTime();
            writer.append(currentTime.toString()); //format = dow mon dd hh:mm:ss zzz yyyy
            //LINE 1 = ILLUMINATIONS, comma separated
            writer.append("\n");
            for (int i = 0; i < illuminations.length; i++){
                String bob;
                if (i != illuminations.length-1){
                    bob = Double.toString(illuminations[i]) + ",";
                } else {
                    bob = Double.toString(illuminations[i]); //LAST VALUE ONLY
                }
                writer.append(bob);
            }
            //LINE 2 = frequency analysis 256 values, comma separated
            writer.append("\n");
            for (int i = 0; i < averagedFS.length; i++){
                String bob;
                if (i != averagedFS.length-1){
                    bob = Double.toString(averagedFS[i]) + ",";
                } else {
                    bob = Double.toString(averagedFS[i]); //LAST VALUE ONLY
                }
                writer.append(bob);
            }
            //LINE 3 = maximum amplitude
            writer.append("\n");
            writer.append(Double.toString(currentMaxAmplitude));
            //LINE 4 = user-input strings [name of place, feelings, sounds]
            writer.append("\n");
            writer.append(this.current_place_name);
            writer.append(",");
            writer.append(this.current_place_feelings);
            writer.append(",");
            writer.append(this.current_place_sounds);

            //--[DONE]--//
            writer.flush();
            writer.close();
            status_text.setText("STATUS: FILE WRITTEN");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.d("hey", "File not found.");
            status_text.setText("STATUS: FILE NOT FOUND");
        } catch (IOException e) {
            e.printStackTrace();
            status_text.setText("STATUS: IO ERROR");
        }
    }

    //----------file END--------//

    //not using this anymore
    public void displayStoppedFreqSpectrum() {
        //VISUALIZER - frequency spectrum -- STOPPED VERSION ONLY//
        //when stopped, shows average frequency spectrum
        canvas.drawColor(paint_colour); // refresh canvas (paint over old amplitudes)

        for (int i = 0; i < averagedFS.length; i++) {
            int downy = (int) (100 - (averagedFS[i] * 10));
            int upy = 100;
            canvas.drawLine(i, downy, i, upy, paint);
        }
        imageView_visual.invalidate();
    }



    //-----------audio stuff------------//
    //-- using the AudioRecord library --//
    public class RecordAudio extends AsyncTask<Void, double[], Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            try {
                int bufferSize = AudioRecord.getMinBufferSize(freq, channelConfiguration, audioEncoding);
                AudioRecord audio_record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        freq, channelConfiguration, audioEncoding, bufferSize);
                //blockSize is 256
                short[] buffer = new short[blockSize];
                double[] toTransform = new double[blockSize];

                currentMaxAmplitude = 0.0; // reset to 0
                //note: in java, double arrays guaranteed to intialize as 0.0
                averagedFS = new double[blockSize]; //clear out old double array
                int counter = 1;

                audio_record.startRecording();

                while (started) {
                    int bufferReadResult = audio_record.read(buffer, 0, blockSize);
                    for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0; //signed 16 bit
                    }
                    transformer.ft(toTransform); //FFT
                    for (int i = 0; i < blockSize && i < bufferReadResult; i++){
                        //ongoing average calculation formula...
                        averagedFS[i] = averagedFS[i] + ((toTransform[i] - averagedFS[i])/counter);
                        if (toTransform[i] > currentMaxAmplitude){
                            currentMaxAmplitude = toTransform[i];
                        }
                    }
                    counter = counter + 1;
                    publishProgress(toTransform);
                }
                audio_record.stop();

            } catch (Throwable t) {
                t.printStackTrace();
                Log.d("hey", "Recording failed!!");

            }
            return null;
        }

        @Override
        protected void onProgressUpdate(double[]... toTransform) {
            //VISUALIZER - frequency spectrum//
            //canvas.drawColor(paint_colour); // refresh canvas (paint over old amplitudes)
            if (rl != null){
                rl.setBackgroundColor(paint_colour); //***test
            }

            for (int i = 0; i < toTransform[0].length; i++) {
                int colour = (int) (toTransform[0][i]/10 * 255);
                paint.setColor(Color.rgb(colour,colour,colour));
                canvas.drawPoint(sound_points_x[i], sound_points_y[i], paint);
            }
            imageView_visual.invalidate();
            amplitude_text.setText("AMPLITUDE : " + Double.toString(toTransform[0][toTransform[0].length/2]));
        }

    } //--end of class RecordAudio --//


}



