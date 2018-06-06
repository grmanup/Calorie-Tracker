package com.example.rafi.caloriestracker;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.DataReadResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    static float expendedCalories = 0;
    GoogleApiClient mClient;
    int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 11;
    String TAG = MainActivity.class.getName();

    TextView countStill;
    TextView countWalking;
    TextView countStair;
    TextView countOnVehicle;
    TextView countRunningJogging;

    TextView countBiking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        countStill = (TextView) findViewById(R.id.count);
        countWalking = (TextView) findViewById(R.id.count_walking);
        countStair = (TextView) findViewById(R.id.count_stair);
        countOnVehicle = (TextView) findViewById(R.id.count_onVehicle);
        countRunningJogging = (TextView) findViewById(R.id.count_onRunningJogging);
        countBiking = (TextView) findViewById(R.id.count_onBiking);


        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        } else {
            buildFitnessClient();
            accessGoogleFit();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("TAG", "onActivityResult");
        if (resultCode == Activity.RESULT_OK) {
            buildFitnessClient();
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                accessGoogleFit();
            }
        }
    }

    private void accessGoogleFit() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.YEAR, -1);
        long startTime = cal.getTimeInMillis();

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .setTimeRange(startTime, endTime, java.util.concurrent.TimeUnit.MILLISECONDS)
                .bucketByTime(1, java.util.concurrent.TimeUnit.DAYS)
                .build();


        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(readRequest)
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        Log.d("LOG_TAG", "onSuccess()");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("LOG_TAG", "onFailure()", e);
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<DataReadResponse>() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        Log.d("LOG_TAG", "onComplete()");
                    }
                });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        fetchUserGoogleFitData(sdf.format(new Date()));

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("TAG", "Suspended");
    }

    private void buildFitnessClient() {
        // Create the Google API Client
        Log.d("TAG", "buildFitnessClient");
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addApi(Fitness.CONFIG_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .useDefaultAccount().build();
        mClient.connect();

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("TAG", connectionResult.toString());
    }

    public void fetchUserGoogleFitData(String date) {
        if (mClient != null && mClient.isConnected()) {
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date d1 = null;
            try {
                d1 = originalFormat.parse(date);
            } catch (Exception e) {

            }
            Calendar calendar = Calendar.getInstance();

            try {
                calendar.setTime(d1);
            } catch (Exception e) {
                calendar.setTime(new Date());
            }
            DataReadRequest readRequest = queryDateFitnessData(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            new GetCaloriesAsyncTask(readRequest, mClient).execute();

        }
    }

    private DataReadRequest queryDateFitnessData(int year, int month, int day_of_Month) {

        Calendar startCalendar = Calendar.getInstance(Locale.getDefault());
        startCalendar.set(Calendar.YEAR, year);
        startCalendar.set(Calendar.MONTH, month);
        startCalendar.set(Calendar.DAY_OF_MONTH, day_of_Month);
        startCalendar.set(Calendar.HOUR_OF_DAY, 23);
        startCalendar.set(Calendar.MINUTE, 59);
        startCalendar.set(Calendar.SECOND, 59);
        startCalendar.set(Calendar.MILLISECOND, 999);
        long endTime = startCalendar.getTimeInMillis();
        startCalendar.set(Calendar.HOUR_OF_DAY, 0);
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);
        long startTime = startCalendar.getTimeInMillis();

        return new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketByActivitySegment(1, TimeUnit.MILLISECONDS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

    }

    public class GetCaloriesAsyncTask extends AsyncTask<Void, Void, DataReadResult> {
        DataReadRequest readRequest;
        String TAG = GetCaloriesAsyncTask.class.getName();
        GoogleApiClient mClient = null;

        public GetCaloriesAsyncTask(DataReadRequest dataReadRequest_, GoogleApiClient googleApiClient) {
            this.readRequest = dataReadRequest_;
            this.mClient = googleApiClient;
        }

        @Override
        protected DataReadResult doInBackground(Void... params) {
            return Fitness.HistoryApi.readData(mClient, readRequest).await(1, TimeUnit.MINUTES);
        }

        @Override
        protected void onPostExecute(DataReadResult dataReadResult) {
            super.onPostExecute(dataReadResult);
            printData(dataReadResult);
        }

    }

    void printData(DataReadResult dataReadResult) {
        if (dataReadResult.getBuckets().size() > 0) {

            for (Bucket bucket : dataReadResult.getBuckets()) {
                String bucketActivity = bucket.getActivity();
                if (bucketActivity.contains(FitnessActivities.STILL)) {
                    Log.e(TAG, "bucket type " + bucket.getActivity());
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        dumpDataSet(dataSet);
                    }
                }
            }

            countStill.setText(String.valueOf(expendedCalories));
            expendedCalories = 0;
            for (Bucket bucket : dataReadResult.getBuckets()) {
                String bucketActivity = bucket.getActivity();
                if (bucketActivity.contains(FitnessActivities.WALKING)) {
                    Log.e(TAG, "bucket type " + bucket.getActivity());
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        dumpDataSet(dataSet);
                    }
                }
            }
            countWalking.setText(String.valueOf(expendedCalories));

            expendedCalories = 0;
            for (Bucket bucket : dataReadResult.getBuckets()) {
                String bucketActivity = bucket.getActivity();
                if (bucketActivity.contains(FitnessActivities.STAIR_CLIMBING)) {
                    Log.e(TAG, "bucket type " + bucket.getActivity());
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        dumpDataSet(dataSet);
                    }
                }
            }
            countStair.setText(String.valueOf(expendedCalories));

            expendedCalories = 0;
            for (Bucket bucket : dataReadResult.getBuckets()) {
                String bucketActivity = bucket.getActivity();
                if (bucketActivity.contains(FitnessActivities.IN_VEHICLE)) {
                    Log.e(TAG, "bucket type " + bucket.getActivity());
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        dumpDataSet(dataSet);
                    }
                }
            }
            countOnVehicle.setText(String.valueOf(expendedCalories));

            expendedCalories = 0;
            for (Bucket bucket : dataReadResult.getBuckets()) {
                String bucketActivity = bucket.getActivity();
                if (bucketActivity.contains(FitnessActivities.RUNNING_JOGGING)) {
                    Log.e(TAG, "bucket type " + bucket.getActivity());
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        dumpDataSet(dataSet);
                    }
                }
            }
            countRunningJogging.setText(String.valueOf(expendedCalories));

            expendedCalories = 0;
            for (Bucket bucket : dataReadResult.getBuckets()) {
                String bucketActivity = bucket.getActivity();
                if (bucketActivity.contains(FitnessActivities.BIKING)) {
                    Log.e(TAG, "bucket type " + bucket.getActivity());
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        dumpDataSet(dataSet);
                    }
                }
            }
            countBiking.setText(String.valueOf(expendedCalories));

        }


    }

    private void dumpDataSet(DataSet dataSet) {
        Log.e(TAG, " Data type: " + dataSet.getDataType().getName());

        for (DataPoint dp : dataSet.getDataPoints()) {
            if (dp.getEndTime(TimeUnit.MILLISECONDS) > dp.getStartTime(TimeUnit.MILLISECONDS)) {
                for (Field field : dp.getDataType().getFields()) {
                    expendedCalories = expendedCalories + dp.getValue(field).asFloat();
                }
            }
        }
    }


    public void launchActivity(View view) {
        Intent intent = new Intent(this, TrackerActivity.class);
        startActivity(intent);
    }


}
