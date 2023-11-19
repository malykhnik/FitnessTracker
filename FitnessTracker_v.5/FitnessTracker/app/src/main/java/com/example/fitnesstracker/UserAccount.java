package com.example.fitnesstracker;

import java.util.UUID;

public class UserAccount {
    private String firstName;
    private String secondName;
    private String id;
    private UUID uuid;

    public void createUUID() {
        uuid = UUID.randomUUID();
        id = uuid.toString();
    }

    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getSecondName() {
        return secondName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return  firstName + " " +
                secondName + " " +
                id;
    }
}
