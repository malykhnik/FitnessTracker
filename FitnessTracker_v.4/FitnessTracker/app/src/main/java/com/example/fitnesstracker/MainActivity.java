package com.example.fitnesstracker;

import static com.google.android.gms.fitness.data.DataType.AGGREGATE_HEART_POINTS;
import static com.google.android.gms.fitness.data.DataType.AGGREGATE_HEART_RATE_SUMMARY;
import static com.google.android.gms.fitness.data.DataType.AGGREGATE_STEP_COUNT_DELTA;
import static com.google.android.gms.fitness.data.DataType.TYPE_HEART_POINTS;
import static com.google.android.gms.fitness.data.DataType.TYPE_HEART_RATE_BPM;
import static com.google.android.gms.fitness.data.DataType.TYPE_STEP_COUNT_DELTA;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.SessionsClient;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionInsertRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FitnessTrackerLOG";
    private static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 1;

    private GoogleSignInAccount account;
    private SessionsClient sessionsClient;

    private GoogleSignInOptionsExtension fitnessOptions;
    private DataReadRequest readRequest;
    private SessionReadRequest sessionReadRequest;

    private ZonedDateTime endTime;
    private ZonedDateTime startTime;

    private List<String> SLEEP_STAGE_NAMES;

    private List<Integer> listStepsRate;
    private List<Integer> listHeartRate;
    private List<HashMap<String, Integer>> listStageSleep;

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

        //типо конструктора
        setupVars();

        //настройка кликабельных кнопок
        setupListeners();

        //настройка входа в аккаунт
        loginSetup();
    }

    private void setupVars() {
        listHeartRate = new ArrayList<>();
        listStepsRate = new ArrayList<>();
        listStageSleep = new ArrayList<>();
        SLEEP_STAGE_NAMES = new ArrayList<>();
        SLEEP_STAGE_NAMES.add("Unused");
        SLEEP_STAGE_NAMES.add("Awake (during sleep)");
        SLEEP_STAGE_NAMES.add("Sleep");
        SLEEP_STAGE_NAMES.add("Out-of-bed");
        SLEEP_STAGE_NAMES.add("Light sleep");
        SLEEP_STAGE_NAMES.add("Deep sleep");
        SLEEP_STAGE_NAMES.add("REM sleep");
    }

    //ИЗ ОФ. ДОКУМЕНТАЦИИ
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GOOGLE_FIT_PERMISSIONS_REQUEST_CODE) {
                accessGoogleFit();
            } else {
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
                .accessSleepSessions(FitnessOptions.ACCESS_WRITE)
                .addDataType(TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(AGGREGATE_HEART_RATE_SUMMARY, FitnessOptions.ACCESS_READ)
                .addDataType(TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ).build();
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

    }

    private void accessGoogleFit() {
        account = GoogleSignIn.getAccountForExtension(this, fitnessOptions);
        sessionsClient = Fitness.getSessionsClient(this, account);
    }

    private void getGoogleFitData(GoogleSignInAccount account) {
//        getDataSteps();
//        getDataHeartRate();
        getDataSleep();
    }

    private void setSettingsForGettingSteps() {
        endTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
        startTime = endTime.toLocalDate().minusWeeks(1).atStartOfDay(ZoneId.systemDefault());
        Log.i(TAG, "(Steps) Range Start: " + startTime);
        Log.i(TAG, "(Steps) Range End: " + endTime);

        readRequest = new DataReadRequest.Builder()
                .aggregate(TYPE_STEP_COUNT_DELTA)
                .enableServerQueries()
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build();
    }

    private void getDataSteps() {
        setSettingsForGettingSteps();

        Fitness.getHistoryClient(this, account)
                .readData(readRequest)
                .addOnSuccessListener(response -> {
                    for (Bucket bucket : response.getBuckets()) {
                        for (DataSet dataSet : bucket.getDataSets()) {
                            Log.i(TAG, "Data returned for Data type " + dataSet.getDataType().getName());
                            if (dataSet.isEmpty()) {
                                Log.i(TAG, "DataSet is Empty!!!!!!!!!!!!!!!!!");
                            }
                            for (DataPoint dp : dataSet.getDataPoints()) {
                                Log.i(TAG, "Data point:");
                                Log.i(TAG, "\tType: " + dp.getDataType().getName());
                                for (Field field : dp.getDataType().getFields()) {
                                    // Добавление данных
                                    Log.i(TAG, "\tField: " + field.getName() +
                                            " Value: " + dp.getValue(field));
                                    String value = String.valueOf(dp.getValue(field));
                                    int stepsRate = Math.round(Integer.parseInt(value));
                                    listStepsRate.add(stepsRate);
                                    tvStepCount.setText(String.valueOf(dp.getValue(field)));
                                }
                            }
                        }
                    }
                    if (listStepsRate.size() == 0) {
                        tvStepCount.setText("Ты не походил!");
                    } else {
                        tvStepCount.setText(String.valueOf(listStepsRate.get(listStepsRate.size() - 1)));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to read steps rate data", e));
    }

    private void setSettingsForGettingHeartRate() {
        endTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
        startTime = endTime.toLocalDate().minusDays(2).atStartOfDay(ZoneId.systemDefault());
        Log.i(TAG, "(Heart) Range Start: " + startTime);
        Log.i(TAG, "(Heart) Range End: " + endTime);

        readRequest = new DataReadRequest.Builder()
                .read(DataType.TYPE_HEART_RATE_BPM)
                .enableServerQueries()
                .bucketByTime(1, TimeUnit.HOURS)
                .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build();
    }

    private void getDataHeartRate() {
        setSettingsForGettingHeartRate();

        Fitness.getHistoryClient(this, account)
                .readData(readRequest)
                .addOnSuccessListener(response -> {
                    for (Bucket bucket : response.getBuckets()) {
                        for (DataSet dataSet : bucket.getDataSets()) {
                            Log.i(TAG, "Data returned for Data type " + dataSet.getDataType().getName());
                            if (dataSet.isEmpty()) {
                                Log.i(TAG, "DataSet is Empty!!!!!!!!!!!!!!!!!");
                                continue;
                            }
                            for (DataPoint dp : dataSet.getDataPoints()) {
                                Log.i(TAG, "Data point:");
                                Log.i(TAG, "\tType: " + dp.getDataType().getName());
                                for (Field field : dp.getDataType().getFields()) {
                                    // Добавление данных
                                    Log.i(TAG, "\tField: " + field.getName() +
                                            " Value: " + dp.getValue(field));
                                    String value = String.valueOf(dp.getValue(field));
                                    int heartRate = (int) Math.round(Double.parseDouble(value));
                                    listHeartRate.add(heartRate);
                                    Log.i(TAG, "heartRate: " + heartRate);

                                }
                            }
                        }
                    }
                    if (listHeartRate.size() == 0) {
                        tvHeartCount.setText("Ты не измерил!");
                    } else {
                        tvHeartCount.setText(String.valueOf(listHeartRate.get(listHeartRate.size() - 1)));
                    }

                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to read steps rate data", e));
    }

    private void setSettingsForGettingSleep() {
        endTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
        startTime = endTime.toLocalDate().minusDays(1).atStartOfDay(ZoneId.systemDefault());
        Log.i(TAG, "(Sleep) Range Start: " + startTime);
        Log.i(TAG, "(Sleep) Range End: " + endTime);

        sessionReadRequest = new SessionReadRequest.Builder()
                .readSessionsFromAllApps()
                .includeSleepSessions()
                .read(DataType.TYPE_SLEEP_SEGMENT)
                .setTimeInterval(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                .build();
    }

    @SuppressLint("SetTextI18n")
    private void getDataSleep() {
        setSettingsForGettingSleep();

        sessionsClient.readSession(sessionReadRequest)
                .addOnSuccessListener(response -> {
                    for (Session session : response.getSessions()) {
                        long sessionStart = session.getStartTime(TimeUnit.MILLISECONDS);
                        long sessionEnd = session.getEndTime(TimeUnit.MILLISECONDS);
                        Log.i(TAG, "Sleep between " + sessionStart + " and " + sessionEnd);

                        long sumSleepingTime = 0;
                        HashMap<String, Integer> stagesAndMinutes = new HashMap<>();
                        stagesAndMinutes.put("Unused", 0);
                        stagesAndMinutes.put("Awake (during sleep)", 0);
                        stagesAndMinutes.put("Sleep", 0);
                        stagesAndMinutes.put("Out-of-bed", 0);
                        stagesAndMinutes.put("Light sleep", 0);
                        stagesAndMinutes.put("Deep sleep", 0);
                        stagesAndMinutes.put("REM sleep", 0);

                        // If the sleep session has finer granularity sub-components, extract them:
                        List<DataSet> dataSets = response.getDataSet(session);
                        for (DataSet dataSet : dataSets) {
                            for (DataPoint point : dataSet.getDataPoints()) {
                                int sleepStageVal = point.getValue(Field.FIELD_SLEEP_SEGMENT_TYPE).asInt();
                                //получаем стадию сна(название)
                                String sleepStage = SLEEP_STAGE_NAMES.get(sleepStageVal);
                                long segmentStart = point.getStartTime(TimeUnit.MINUTES);
                                long segmentEnd = point.getEndTime(TimeUnit.MINUTES);
                                //считаем сколько длилась стадия сна
                                long timeSleepingStage = segmentEnd - segmentStart;
                                sumSleepingTime += timeSleepingStage;
                                //увеличиваем минуты для каждой фазы сна
                                stagesAndMinutes.put(sleepStage,
                                        stagesAndMinutes.get(sleepStage) + (int)timeSleepingStage);
                                Log.i(TAG, "\t* Type " + sleepStage + " between " + segmentStart + " and " + segmentEnd);
                                Log.i(TAG, "All sleep " + timeSleepingStage);
                            }
                        }
                        //добавляем сессию сна в list
                        listStageSleep.add(stagesAndMinutes);
                        //выводим минуты по каждой стадии на текущей сессии
                        for (HashMap<String, Integer> hm: listStageSleep) {
                            for (String key: hm.keySet()) {
                                Log.i(TAG, "Stage " + key + " Minutes " + hm.get(key));
                            }
                        }
                        //переводим минуты в формат (..h ..m)
                        int hours = (int) (sumSleepingTime/60);
                        int minutes = (int) sumSleepingTime % 60;
                        Log.i(TAG, "Hours " + hours + " Minutes " + minutes);
                        //Обновление данных
                        if (hours >= 1) {
                            tvSleep.setText(String.valueOf(hours) + "h " + String.valueOf(minutes) + "m");
                        }else {
                            tvSleep.setText(String.valueOf(minutes) + "m");
                        }

                    }
                });
    }

}
