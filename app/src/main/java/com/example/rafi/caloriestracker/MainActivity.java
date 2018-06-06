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
    GoogleApiClient mClient1;
    static float expendedCalories=0;
    int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE =11;
    String TAG = "TAG";
    TextView activityStill;
    TextView countStill;
    TextView activityWalking;
    TextView countWalking;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        activityStill = (TextView)  findViewById(R.id.activity_still);
        countStill = (TextView)  findViewById(R.id.count);
        activityWalking = (TextView)  findViewById(R.id.activity_walking);
        countWalking = (TextView)  findViewById(R.id.count_walking);


        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);

        Log.i("TAG", "onCreate"+String.valueOf(resultCode));

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
        Log.i("TAG", "ON ACTIVITY RESULT");
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
        Log.i("TAG", "CONNECTED");
        fetchUserGoogleFitData("2018-06-5");

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("TAG", "Suspended");
    }

    private void buildFitnessClient() {
        // Create the Google API Client
        Log.d("TAG", "buildFitnessClient");
         mClient1 = new GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                 .addApi(Fitness.CONFIG_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                 .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .useDefaultAccount().build();
        mClient1.connect();

    }



    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("TAG", connectionResult.toString());
    }

    public void fetchUserGoogleFitData(String date) {
        if (mClient1 != null && mClient1.isConnected()) {
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date d1 = null;
            try{
                d1 = originalFormat.parse(date);
            }catch (Exception e){

            }
            Calendar calendar = Calendar.getInstance();

            try{
                calendar.setTime(d1);
            }catch (Exception e){
                calendar.setTime(new Date());
            }
            DataReadRequest readRequest = queryDateFitnessData(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            new GetCaloriesAsyncTask(readRequest, mClient1).execute();

        }
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
        // [START parse_read_data_result]
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        if (dataReadResult.getBuckets().size() > 0) {
            Log.e(TAG, "Number of returned buckets of DataSets is: "+ dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                String bucketActivity = bucket.getActivity();
                if (bucketActivity.contains(FitnessActivities.STILL)) {
                    Log.e(TAG, "bucket type->" + bucket.getActivity());
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        dumpDataSet(dataSet);
                    }
                }
            }

            countStill.setText(String.valueOf(expendedCalories));
            expendedCalories=0;
            for (Bucket bucket : dataReadResult.getBuckets()) {
                String bucketActivity = bucket.getActivity();
                if (bucketActivity.contains(FitnessActivities.WALKING)) {
                    Log.e(TAG, "bucket type->" + bucket.getActivity());
                    List<DataSet> dataSets = bucket.getDataSets();
                    for (DataSet dataSet : dataSets) {
                        dumpDataSet(dataSet);
                    }
                }
            }

            countWalking.setText(String.valueOf(expendedCalories));


        }


    }

    // [START parse_dataset]
    private void dumpDataSet(DataSet dataSet) {
        Log.e(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());

        for (DataPoint dp : dataSet.getDataPoints()) {
            if (dp.getEndTime(TimeUnit.MILLISECONDS) > dp.getStartTime(TimeUnit.MILLISECONDS)) {
                for (Field field : dp.getDataType().getFields()) {
                    expendedCalories = expendedCalories + dp.getValue(field).asFloat();
                }
            }
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
                // The data request can specify multiple data types to return, effectively
                // combining multiple data queries into one call.
                // In this example, it's very unlikely that the request is for several hundred
                // datapoints each consisting of a few steps and a timestamp.  The more likely
                // scenario is wanting to see how many steps were walked per day, for 7 days.
                //.aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                //.aggregate(DataType.TYPE_CALORIES_EXPENDED,DataType.AGGREGATE_CALORIES_EXPENDED)
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                // .read(DataType.TYPE_CALORIES_EXPENDED)
                // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                // bucketByTime allows for a time span, whereas bucketBySession would allow
                // bucketing by "sessions", which would need to be defined in code.
                //.bucketByTime(1, TimeUnit.DAYS)
                .bucketByActivitySegment(1, TimeUnit.MILLISECONDS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

    }

    public void launchActivity(View view)
    {
        Intent intent = new Intent(this, TrackerActivity.class);
        startActivity(intent);
    }

}
