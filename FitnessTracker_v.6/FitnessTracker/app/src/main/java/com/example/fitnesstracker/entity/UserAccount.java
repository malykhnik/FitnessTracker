package com.example.fitnesstracker.entity;

import java.util.UUID;

public class UserAccount {
    private String first_name;
    private String second_name;
    private String user_id;
    private UUID uuid;

    public void createUUID() {
        uuid = UUID.randomUUID();
        user_id = uuid.toString();
    }

    public String getId() {
        return user_id;
    }

    public void setFirstName(String firstName) {
        this.first_name = firstName;
    }

    public void setSecondName(String secondName) {
        this.second_name = secondName;
    }

    public void setId(String id) {
        this.user_id = id;
    }

    @Override
    public String toString() {
        return  first_name + " " +
                second_name + " " +
                user_id;
    }
}
