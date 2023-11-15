package com.example.fitnesstracker;

import static com.google.android.gms.fitness.data.DataType.AGGREGATE_STEP_COUNT_DELTA;
import static com.google.android.gms.fitness.data.DataType.TYPE_STEP_COUNT_DELTA;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.tasks.Task;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FitnessTrackerLOG";
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1;
    private GoogleSignInAccount account;
    private GoogleSignInOptionsExtension fitnessOptions;
    private DataReadRequest readRequest;
    private DataSource dataSource;

    private TextView tvStepCount;
    private TextView tvHeartCount;
    private TextView tvSleep;
    private Button refresh;
    private Button exit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStepCount = findViewById(R.id.tv_step_count);
        tvHeartCount = findViewById(R.id.tv_heart);
        tvSleep = findViewById(R.id.tv_sleep);
        refresh = findViewById(R.id.refreshButton);
        exit = findViewById(R.id.exitButton);

        //настройка кликабельных кнопок
        setupListeners();

        //настройка входа в аккаунт
        loginSetup();
    }

    //ИЗ ОФ. ДОКУМЕНТАЦИИ
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                accessGoogleFit();
            }else {
                Log.d(TAG, "Result wasn't from Google Fit");
            }
        }
    }

    public void setupListeners() {
        //что происходит при нажатии на кнопку refresh
        refresh.setOnClickListener(view -> {
            //обновление данных из MiFit
            getGoogleFitData(account);
            Log.d(TAG, "The \"Refresh\" button has been pressed");
        });

        //что происходит при нажатии на кнопку exit
        exit.setOnClickListener(view -> {
            //выход из аккаунта
            disconnectWithMiFit();
            Log.d(TAG, "You are logged out of your account");
        });
    }

    private void disconnectWithMiFit() {
        Fitness.getConfigClient(this, account)
                .disableFit()
                .addOnSuccessListener(unused ->
                        Log.i(TAG, "Disabled Google Fit"))
                .addOnFailureListener(e ->
                        Log.w(TAG, "There was an error disabling Google Fit", e));
    }

    private void loginSetup() {
        //НАСТРОЙКА ВХОДА ИЗ ОФ.ДОКУМЕНТАЦИИ
        //настройка параметров фитнеса
        fitnessOptions = FitnessOptions.builder()
                .addDataType(TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ).build();
//                .addDataType(TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
//                .addDataType(TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ).build();

        //получили экземпляр объекта Account для использования API
        account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE, // e.g. 1
                    account,
                    fitnessOptions);
        } else {
            Log.w(TAG, "accessGoogleFit");
            accessGoogleFit();
        }

//        //настройка параметра входа в google - ИЗ ИНЕТА
//        GoogleSignInOptions googleSignInOptions =
//                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                        .addExtension(fitnessOptions).requestEmail().build();
//        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
//        Intent signInIntent = googleSignInClient.getSignInIntent();
//        startActivityForResult(signInIntent, 0);
    }

    private void accessGoogleFit() {
//        ZonedDateTime endTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
//        ZonedDateTime startTime = endTime.minusWeeks(1);
        ZonedDateTime endTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
        ZonedDateTime startTime = endTime.toLocalDate().atStartOfDay(ZoneId.systemDefault());

        Log.i(TAG, "Range Start: " + startTime);
        Log.i(TAG, "Range End: " + endTime);

        DataSource dataSource = new DataSource.Builder()
                .setAppPackageName(this.getPackageName())
                .setDataType(DataType.TYPE_SPEED)
                .setType(DataSource.TYPE_RAW)
                .build();
        DataSet dataSet = DataSet.create(dataSource);
        List<DataPoint> dp = dataSet.getDataPoints();
        for (DataPoint val: dp) {
            Log.i(TAG, "DataPoint = " + val);
        }

        readRequest = new DataReadRequest.Builder()
                .aggregate(TYPE_STEP_COUNT_DELTA)
//                .aggregate(TYPE_HEART_RATE_BPM)
//                .aggregate(TYPE_SLEEP_SEGMENT)
//                .aggregate(dataSource)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build();
        account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);
    }

    private void getGoogleFitData(GoogleSignInAccount account) {
        Log.i(TAG, "READREQUEST!!!!!!!!!! " + readRequest.toString());
        Fitness.getHistoryClient(this, account)
                .readData(readRequest)
                .addOnSuccessListener(response -> {
                    // The aggregate query puts datasets into buckets, so convert to a
                    // single list of datasets
                    if (response.getBuckets().size() > 0) {
                        for (Bucket bucket : response.getBuckets()) {
                            for (DataSet dataSet : bucket.getDataSets()) {
                                Log.i(TAG, "Data returned for Data type " +  dataSet.getDataType().getName());
                                if (dataSet.isEmpty()) {
                                    Log.i(TAG,"DataSet is Empty!!!!!!!!!!!!!!!!!");
                                }
                                for (DataPoint dp : dataSet.getDataPoints()) {
                                    Log.i(TAG,"Data point:");
                                    Log.i(TAG,"\tType: " + dp.getDataType().getName());
                                    for (Field field : dp.getDataType().getFields()) {
                                        // Обновление данных
                                        Log.i(TAG,"\tField: " + field.getName() +
                                                      " Value: " +  dp.getValue(field));
                                        tvStepCount.setText(String.valueOf(dp.getValue(field)));
                                    }
                                }
                            }
                        }
                    } else if (response.getDataSets().size() > 0) {
                        for (DataSet dataSet : response.getDataSets()) {
                            Log.i(TAG, "Data returned for Data type " +  dataSet.getDataType().getName());
                            if (dataSet.isEmpty()) {
                                Log.i(TAG,"DataSet is Empty!!!!!!!!!!!!!!!!!");
                            }
                            for (DataPoint dp : dataSet.getDataPoints()) {
                                Log.i(TAG,"Data point:");
                                Log.i(TAG,"\tType: " + dp.getDataType().getName());
                                for (Field field : dp.getDataType().getFields()) {
                                    // Обновление данных
                                    Log.i(TAG,"\tField: " + field.getName() +
                                            " Value: " +  dp.getValue(field));
                                    tvStepCount.setText(String.valueOf(dp.getValue(field)));
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to read steps rate data", e));

    }


}
