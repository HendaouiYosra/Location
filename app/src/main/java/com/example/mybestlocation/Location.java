package com.example.mybestlocation;

public class Location {
    private int id; // Added ID field
    private String name;
    private String pseudo;
    private double latitude;
    private double longitude;
    public Location() {
        // Provide default values, for example:
        this.name = "";
        this.pseudo = "";
        this.latitude = 0.0;
        this.longitude = 0.0;
    }

    // Constructor
    public Location(int id, String name, String pseudo, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.pseudo = pseudo;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Location(String name, String pseudo, double latitude, double longitude) {
    }


    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    @Override
    public String toString() {
        return "Location{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", pseudo='" + pseudo + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

}
