package com.example.fitnesstracker.entity;


import com.google.gson.Gson;
public class JsonConverter {

    private final Gson gson = new Gson();

    public String userToJson (UserAccount user) {
        return gson.toJson(user);
    }

    public String indicatorToJson (Indicator indicator) {
        return gson.toJson(indicator);
    }

}