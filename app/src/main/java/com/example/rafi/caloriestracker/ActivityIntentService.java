package com.example.rafi.caloriestracker;

import android.app.IntentService;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityIntentService extends IntentService{
    protected static final String TAG = "Activity";
    //Call the super IntentService constructor with the name for the worker thread//
    public ActivityIntentService() {
        super(TAG);
    }
    @Override
    public void onCreate() {
        super.onCreate();
    }
//Define an onHandleIntent() method, which will be called whenever an activity detection update is available//

    @Override
    protected void onHandleIntent(Intent intent) {
//Check whether the Intent contains activity recognition data//
        if (ActivityRecognitionResult.hasResult(intent)) {

//If data is available, then extract the ActivityRecognitionResult from the Intent//
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);


            DetectedActivity mostProbableActivity
                    = result.getMostProbableActivity();

//Get the confidence percentage//

            int confidence = mostProbableActivity.getConfidence();

//Get the activity type//

            int activityType = mostProbableActivity.getType();

            Log.i("kkk",String.valueOf(mostProbableActivity.getType()));


            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString("detActivity",
                            String.valueOf(activityType))
                    .apply();
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putString("score",
                            String.valueOf(confidence))
                    .apply();


        }
    }

}
