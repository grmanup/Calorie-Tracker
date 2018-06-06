package com.example.rafi.caloriestracker;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.ListView;
import android.app.PendingIntent;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class TrackerActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";
    ImageView image;

    TextView activityType;
    TextView confidencePercentage;
    private Context mContext;
    private ActivityRecognitionClient mActivityRecognitionClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_track_activity);

        mContext = this;

        image = (ImageView) findViewById(R.id.image);
        activityType = (TextView) findViewById(R.id.activity_type);
        confidencePercentage = (TextView) findViewById(R.id.confidence_percentage);

       // mActivityRecognitionClient = new ActivityRecognitionClient(this);
        mActivityRecognitionClient = ActivityRecognition.getClient(this);
        requestUpdatesHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        updateDetectedActivitiesList();
    }

    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public void requestUpdatesHandler() {
//Set the activity detection interval. I’m using 3 seconds//
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                3000,
                getActivityDetectionPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateDetectedActivitiesList();
            }
        });
    }

    //Get a PendingIntent//
    private PendingIntent getActivityDetectionPendingIntent() {
//Send the activity data to our DetectedActivitiesIntentService class//
        Intent intent = new Intent(this, ActivityIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    //Process the list of activities//
    protected void updateDetectedActivitiesList() {
        activityType.setText("Detected Activity : ");

        switch (PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString("detActivity", "NA")) {
            case "1":
                activityType.append(getString(R.string.bicycle));
                break;
            case "0":
                activityType.append(getString(R.string.vehicle));
                break;
            case "2":
                activityType.append(getString(R.string.foot));
                break;
            case "8":
                activityType.append(getString(R.string.running));
                break;
            case "3":
                activityType.append(getString(R.string.still));
                break;
            case "5":
                activityType.append(getString(R.string.tilting));
                break;
            case "7":
                activityType.append(getString(R.string.walking));
                break;
            default:
                activityType.append(getString(R.string.unknown_activity));
        }


        confidencePercentage.setText(PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString("score", "NA"));
        confidencePercentage.append("%");
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        updateDetectedActivitiesList();
    }
}
