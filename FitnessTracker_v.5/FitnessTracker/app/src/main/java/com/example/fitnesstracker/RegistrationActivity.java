package com.example.fitnesstracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";

    private UserAccount userAccount;

    private Button buttonLogin;
    private Button buttonSave;
    private EditText editTextName;
    private EditText editTextSurname;

    SharedPreferences sPref;

    final String SAVED_TEXT_NAME = "saved_text_name";
    final String SAVED_TEXT_SURNAME = "saved_text_surname";
    final String SAVED_TEXT_ID = "saved_text_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra("finish", false)) finish();
        setContentView(R.layout.registration_activity);

        userAccount = new UserAccount();

        sPref = getSharedPreferences("MyPref", MODE_PRIVATE);
        String savedTextId = sPref.getString(SAVED_TEXT_ID, "");
        if (savedTextId.length() == 0) {
            userAccount.createUUID();
        }

        buttonLogin = findViewById(R.id.buttonLogin);
        buttonSave = findViewById(R.id.buttonSave);
        editTextName = findViewById(R.id.name);
        editTextSurname = findViewById(R.id.surname);

        setupListeners();

        loadText();
    }

    private void setupListeners() {
        buttonLogin.setOnClickListener(view -> {
            Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
            String userInput = userAccount.toString();
            intent.putExtra("userInput", userInput);
            startActivity(intent);
            Log.d(TAG, "You are entered your data and go to MainActivity");
            saveText();
        });

        buttonSave.setOnClickListener(view -> {
            String beforeName = sPref.getString(SAVED_TEXT_NAME, "");
            String beforeSurname = sPref.getString(SAVED_TEXT_SURNAME, "");
            String currentName = editTextName.getText().toString();
            String currentSurname = editTextSurname.getText().toString();

            if (!beforeName.equals(currentName) || !beforeSurname.equals(currentSurname)) {
                userAccount.createUUID();
                saveText();
                Toast.makeText(RegistrationActivity.this, "Changes saved", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "You have been generated new UUID for new Name and Surname");
            } else {
                Toast.makeText(RegistrationActivity.this, "Change the data!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveText() {
        sPref = getSharedPreferences("MyPref", MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString(SAVED_TEXT_NAME, editTextName.getText().toString());
        ed.putString(SAVED_TEXT_SURNAME, editTextSurname.getText().toString());
        if (userAccount.getId() != null) {
            ed.putString(SAVED_TEXT_ID, userAccount.getId());
        }
        ed.apply();
    }

    private void loadText() {
        sPref = getSharedPreferences("MyPref", MODE_PRIVATE);
        String savedTextName = sPref.getString(SAVED_TEXT_NAME, "");
        String savedTextSurname = sPref.getString(SAVED_TEXT_SURNAME, "");
        String savedTextId = sPref.getString(SAVED_TEXT_ID, "");
        editTextName.setText(savedTextName);
        editTextSurname.setText(savedTextSurname);

        userAccount.setFirstName(savedTextName);
        userAccount.setSecondName(savedTextSurname);
        userAccount.setId(savedTextId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        saveText();
    }
}
