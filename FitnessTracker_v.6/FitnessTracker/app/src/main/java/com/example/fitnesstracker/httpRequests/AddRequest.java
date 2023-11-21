package com.example.fitnesstracker.httpRequests;

import android.util.Log;

import com.example.fitnesstracker.entity.JsonConverter;
import com.example.fitnesstracker.entity.UserAccount;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddRequest {

    private final JsonConverter converter = new JsonConverter();

    public void addUserRequest (UserAccount user) {
        MediaType json = MediaType.get("application/json; charset=utf-8");
        String jsonRequest = converter.userToJson(user);
        Log.w("ADD REQUEST", "addUserRequest + " + jsonRequest);
        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(jsonRequest, json);
        Request.Builder builder = new Request.Builder().url("http://s3v3nny.sloth-1.suslovd.ru:9555/add").post(body);
        Request request = builder.build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.i("AddRequest", "Unsuccessfull request" );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
