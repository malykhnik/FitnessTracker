package com.example.fitnesstracker;

import static com.google.android.gms.fitness.data.DataType.TYPE_HEART_RATE_BPM;
import static com.google.android.gms.fitness.data.DataType.TYPE_SLEEP_SEGMENT;
import static com.google.android.gms.fitness.data.DataType.TYPE_STEP_COUNT_DELTA;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.HistoryClient;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FitnessTrackerLOG";
    private GoogleSignInAccount myAccount;
    private GoogleSignInOptionsExtension fitnessOptions;

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    myAccount = account;
                }
            } catch (ApiException e) {
                e.printStackTrace();
            }
        }
    }

    public void setupListeners() {
        //что происходит при нажатии на кнопку refresh
        refresh.setOnClickListener(view -> {
            //обновление данных из MiFit
            getGoogleFitData(myAccount);
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
        Fitness.getConfigClient(this, myAccount)
                .disableFit()
                .addOnSuccessListener(unused ->
                        Log.i(TAG, "Disabled Google Fit"))
                .addOnFailureListener(e ->
                        Log.w(TAG, "There was an error disabling Google Fit", e));
    }

    private void loginSetup() {
        //настройка параметров фитнеса
        fitnessOptions = FitnessOptions.builder()
                .addDataType(TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                .addDataType(TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ).build();

        //настройка параметра входа в google
        GoogleSignInOptions googleSignInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .addExtension(fitnessOptions).requestEmail().build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, 0);
    }

    private void getGoogleFitData(GoogleSignInAccount account) {
        //данные о шагах за день
        Fitness.getHistoryClient(this, account)
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
                .addOnSuccessListener(dataSet -> {
                    if (dataSet.isEmpty()) {
                        Log.d(TAG, "No steps rate data available");
                        return;
                    }

                    int totalSteps = 0;
                    for (DataPoint dp : dataSet.getDataPoints()) {
                        for (Field field : dp.getDataType().getFields()) {
                            totalSteps += dp.getValue(field).asInt();
                        }
                    }
                    Log.d(TAG, "Total steps today: " + totalSteps);
                    // Обновление данных
                    tvStepCount.setText(String.valueOf(totalSteps));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to read steps rate data", e));

        //данные о последнем замере ЧСС
        Fitness.getHistoryClient(this, account)
                .readDailyTotal(DataType.TYPE_HEART_RATE_BPM)
                .addOnSuccessListener(dataSet -> {
                    if (dataSet.isEmpty()) {
                        Log.d(TAG, "No heart rate data available");
                        return;
                    }

                    DataPoint dataPoint = dataSet.getDataPoints().get(dataSet.getDataPoints().size() - 1);
                    float heartRate = dataPoint.getValue(Field.FIELD_BPM).asFloat();

                    // Обновление данных
                    tvHeartCount.setText(String.valueOf(heartRate));

                    Log.d(TAG, "Last heart rate measurement: " + heartRate);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to read heart rate data", e));

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
    }

}
