package com.example.mavis.place;

import android.media.MediaRecorder;
import java.io.IOException;

/**
 * FOR ANALYZING SOUND (WITHOUT SAVING) -- unused
 */

public class SoundAnalyze {

    private MediaRecorder mRecord = null;

    public void start(){
        if (mRecord == null){
            mRecord = new MediaRecorder();
            mRecord.setAudioSource(MediaRecorder.AudioSource.MIC);
            //mRecord.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); //~
            //mRecord.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB); //~
            mRecord.setOutputFile("/dev/null");
            try {
                mRecord.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mRecord.start();
        }
    }

    public void stop(){
        if (mRecord != null){
            mRecord.stop();
            mRecord.release();
            mRecord = null;
        }
    }

    public double getAmplitude(){
        if (mRecord != null){
            return mRecord.getMaxAmplitude();
        } else {
            return 0;
        }
    }



}
