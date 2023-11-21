package com.example.fitnesstracker.entity;

public class Indicator {
    private String user_id;
    private Integer steps;
    private Integer pulse;
    private String sleep;

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public Integer getSteps() {
        return steps;
    }

    public void setSteps(Integer steps) {
        this.steps = steps;
    }

    public Integer getPulse() {
        return pulse;
    }

    public void setPulse(Integer pulse) {
        this.pulse = pulse;
    }

    public String getSleep() {
        return sleep;
    }

    public void setSleep(String sleep) {
        this.sleep = sleep;
    }

    @Override
    public String toString() {
        return "Indicator{" +
                "user_id='" + user_id + '\'' +
                ", steps=" + steps +
                ", pulse=" + pulse +
                ", sleep='" + sleep + '\'' +
                '}';
    }
}
