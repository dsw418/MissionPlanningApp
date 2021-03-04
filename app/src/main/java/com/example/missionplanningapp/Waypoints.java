package com.example.missionplanningapp;

public class Waypoints {
    //public static final String TAG = Waypoints.class.getSimpleName();

    private double lat;
    private double lng;
    private double alt;

    public Waypoints() {}

    public Waypoints(double lat, double lng, double alt) {
        this.lat = lat;
        this.lng = lng;
        this.alt = alt;
    }

    // setters
    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public void setalt(double alt) {
        this.alt = alt;
    }

    // getters
    public double getLat() {return this.lat;}

    public double getLng() {return this.lng;}

    public double getalt() {return this.alt;}
}
