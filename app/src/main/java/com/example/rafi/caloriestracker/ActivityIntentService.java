package com.example.rafi.caloriestracker;

import android.app.IntentService;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class ActivityIntentService extends IntentService {
    protected static final String TAG = "Activity";

    public ActivityIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    protected void onHandleIntent(Intent intent) {

        if (ActivityRecognitionResult.hasResult(intent)) {


            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            DetectedActivity mostProbableActivity
                    = result.getMostProbableActivity();

            int confidence = mostProbableActivity.getConfidence();
            int activityType = mostProbableActivity.getType();

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
