package com.mycompany.qrpc;

import android.widget.LinearLayout;

public class Endpoint {

    // ID
    private String id;

    // LinearLayout de la UI donde se muestra información sobre este punto de conexión
    private LinearLayout linearlayout;

    // Última longitud conocida
    private double lastLongitude;

    // Última latitud conocida
    private double lastLatitude;

    // Última velocidad en sentido de la longitud conocida
    private double lastLongitudeSpeed;

    // Última velocidad en sentido de la latitud conocida
    private double lastLatitudeSpeed;

    // Distancia entre este punto de conexión y el dispositivo
    private double distance;

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

    public double getDistance() {return distance;}

    public void setDistance(double distance) {this.distance = distance;}
}
