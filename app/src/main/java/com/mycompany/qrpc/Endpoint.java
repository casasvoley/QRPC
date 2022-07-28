package com.mycompany.qrpc;

import android.widget.LinearLayout;

public class Endpoint {

    private String id;
    private LinearLayout linearlayout;
    private double lastLongitude;
    private double lastLatitude;
    private double lastLongitudeSpeed;
    private double lastLatitudeSpeed;

    public Endpoint(String id, LinearLayout linearlayout){
        this.id = id;
        this.linearlayout = linearlayout;
    }

    public String getId(){
        return id;
    }

    public LinearLayout getLinearlayout() { return linearlayout; }

    public double getLastLongitude() {return lastLongitude;}

    public double getLastLatitude() {return lastLatitude;}

    public void setLastLongitude(double d) {this.lastLongitude = d;}

    public void setLastLatitude(double d) {this.lastLatitude = d;}

    public double getLastLongitudeSpeed() {return lastLongitudeSpeed;}

    public double getLastLatitudeSpeed() {return lastLatitudeSpeed;}

    public void setLastLongitudeSpeed(double d) {this.lastLongitudeSpeed = d;}

    public void setLastLatitudeSpeed(double d) {this.lastLatitudeSpeed = d;}
}
