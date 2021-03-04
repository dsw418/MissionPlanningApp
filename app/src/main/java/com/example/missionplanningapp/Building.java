package com.example.missionplanningapp;

public class Building {

    private String boundary;
    private double height;

    public Building() {}

    // Setters
    public void setBoundary(String boundary) {this.boundary = boundary;}

    public void setBuildingHeight(double height) {this.height = height;}

    // Getters
    public String getBoundary() {return this.boundary;}

    public double getBuildingHeight() {return this.height;}
}
