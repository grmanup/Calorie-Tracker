package com.example.rafi.caloriestracker;

import android.content.res.Resources;
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
        Resources res = getResources(); /** from an Activity */
        switch (PreferenceManager.getDefaultSharedPreferences(mContext)
                .getString("detActivity", "NA")) {
            case "1":
                activityType.append(getString(R.string.bicycle));
                image.setImageDrawable(null);
                break;
            case "0":
                activityType.append(getString(R.string.vehicle));
                image.setImageDrawable(null);
                break;
            case "2":
                activityType.append(getString(R.string.foot));
                image.setImageDrawable(res.getDrawable(R.drawable.pedestrianwalking));
                break;
            case "8":
                activityType.append(getString(R.string.running));
                image.setImageDrawable(res.getDrawable(R.drawable.runer));
                break;
            case "3":
                activityType.append(getString(R.string.still));
                image.setImageDrawable(res.getDrawable(R.drawable.yogaposture));
                break;
            case "5":
                activityType.append(getString(R.string.tilting));
                image.setImageDrawable(null);
                break;
            case "7":
                activityType.append(getString(R.string.walking));
                image.setImageDrawable(res.getDrawable(R.drawable.manwalking));
                break;
            default:
                activityType.append(getString(R.string.unknown_activity));
                image.setImageDrawable(res.getDrawable(R.drawable.yogaposture));
                image.setImageDrawable(null);
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
