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
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.tasks.Task;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FitnessTrackerLOG";
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1;
    private GoogleSignInAccount account;
    private GoogleSignInOptionsExtension fitnessOptions;
    private DataReadRequest readRequest;

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

    //ИЗ ИНЕТА
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            try {
//                GoogleSignInAccount account = task.getResult(ApiException.class);
//                if (account != null) {
//                    myAccount = account;
//                }
//            } catch (ApiException e) {
//                e.printStackTrace();
//            }
//        }
//    }

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
        ZonedDateTime endTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
        ZonedDateTime startTime = endTime.minusWeeks(1);
        Log.i(TAG, "Range Start: " + startTime);
        Log.i(TAG, "Range End: " + endTime);

        readRequest = new DataReadRequest.Builder()
//                .aggregate(TYPE_STEP_COUNT_DELTA)
//                .aggregate(TYPE_HEART_RATE_BPM)
//                .aggregate(TYPE_SLEEP_SEGMENT)
                .aggregate(AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build();
        account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);
    }

    private void getGoogleFitData(GoogleSignInAccount account) {
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

//        //данные о шагах за день
//        Fitness.getHistoryClient(this, account)
//                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
//                .addOnSuccessListener(dataSet -> {
//                    if (dataSet.isEmpty()) {
//                        Log.d(TAG, "No steps rate data available");
//                        return;
//                    }
//
//                    int totalSteps = 0;
//                    for (DataPoint dp : dataSet.getDataPoints()) {
//                        for (Field field : dp.getDataType().getFields()) {
//                            totalSteps += dp.getValue(field).asInt();
//                        }
//                    }
//                    Log.d(TAG, "Total steps today: " + totalSteps);
//                    // Обновление данных
//                    tvStepCount.setText(String.valueOf(totalSteps));
//                })
//                .addOnFailureListener(e -> Log.e(TAG, "Failed to read steps rate data", e));
//
//        //данные о последнем замере ЧСС
//        Fitness.getHistoryClient(this, account)
//                .readDailyTotal(DataType.TYPE_HEART_RATE_BPM)
//                .addOnSuccessListener(dataSet -> {
//                    if (dataSet.isEmpty()) {
//                        Log.d(TAG, "No heart rate data available");
//                        return;
//                    }
//
//                    DataPoint dataPoint = dataSet.getDataPoints().get(dataSet.getDataPoints().size() - 1);
//                    float heartRate = dataPoint.getValue(Field.FIELD_BPM).asFloat();
//
//                    // Обновление данных
//                    tvHeartCount.setText(String.valueOf(heartRate));
//
//                    Log.d(TAG, "Last heart rate measurement: " + heartRate);
//                })
//                .addOnFailureListener(e -> Log.e(TAG, "Failed to read heart rate data", e));

    //данные о сне
//        Fitness.getHistoryClient(this, account)
//                .readData(new DataReadRequest.Builder()
//                        .read(DataType.TYPE_SLEEP_SEGMENT)
//                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
//                        .setLimit(1)
//                        .build())
//                .addOnSuccessListener(dataReadResponse -> {
    //                    // Обработка полученных данных о сне
//                    List<DataSet> dataSets = dataReadResponse.getDataSets();
//                    // Проверка наличия данных о сне
//
//                    if (dataSets.isEmpty()) {
//                        Log.d(TAG, "No sleep rate data available");
//                        return;
//                    }
//
//                    DataSet dataSet = dataSets.get(0);
//                    for (DataPoint dataPoint : dataSet.getDataPoints()) {
//                        // Извлечение информации о сне из DataPoint
//                        // и вывод в TextView
//                        String sleepInfo = dataPoint.getValue(Field.FIELD_SLEEP_SEGMENT_TYPE).asString();
//                        //обновление данных
//                        tvSleep.setText(sleepInfo);
//
//                        Log.d(TAG, "Info about sleep: " + sleepInfo);
//                    }
//                })
//                .addOnFailureListener(e -> Log.e(TAG, "Failed to read sleep rate data", e));
//}

}
