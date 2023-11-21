package com.example.fitnesstracker.httpRequests;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CheckRequest {

    public boolean checkUuidRequest (String uuid) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://s3v3nny.sloth-1.suslovd.ru:9555/check/" + uuid)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.i("CheckRequest", "Unsuccessfull request" );
            }

            return response.code() == 200;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}