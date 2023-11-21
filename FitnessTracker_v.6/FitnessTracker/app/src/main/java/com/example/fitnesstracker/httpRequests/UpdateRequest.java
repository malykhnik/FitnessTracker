package com.example.fitnesstracker.httpRequests;

import android.util.Log;

import com.example.fitnesstracker.entity.Indicator;
import com.example.fitnesstracker.entity.JsonConverter;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UpdateRequest {

    private final JsonConverter converter = new JsonConverter();

    public void updateIndicatorsRequest (Indicator indicator) {
        if (indicator.getPulse() == 0 && indicator.getSteps() == 0) {
            return;
        }
        Log.i("INDICATOR", "INDICATOR " + indicator.toString());
        MediaType json = MediaType.get("application/json; charset=utf-8");
        String jsonRequest = converter.indicatorToJson(indicator);
        OkHttpClient client = new OkHttpClient();

        RequestBody body = RequestBody.create(jsonRequest, json);
        Request.Builder builder = new Request.Builder().url("http://s3v3nny.sloth-1.suslovd.ru:9555/update").post(body);
        Request request = builder.build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.i("UpdateRequest", "Unsuccessfull request" );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}