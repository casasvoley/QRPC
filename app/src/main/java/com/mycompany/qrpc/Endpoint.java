package com.mycompany.qrpc;

import android.widget.LinearLayout;

public class Endpoint{

    // ID de punto de conexión
    private final String id;

    // ID de dispositivo
    private final String devId;

    // LinearLayout de la UI donde se muestra información sobre este punto de conexión
    private LinearLayout layout;

    // Última longitud conocida
    private double lastLongitude;

    // Última latitud conocida
    private double lastLatitude;

    // Última velocidad en sentido de la longitud conocida
    private double lastLongitudeSpeed;

    // Última velocidad en sentido de la latitud conocida
    private double lastLatitudeSpeed;

    // Distancia entre este punto de conexión y el dispositivo
    private float distance;

    // Último patrón calculado con respecto a este punto de conexión
    private String lastPattern;

    public Endpoint(String id, String devId, LinearLayout linearlayout){
        this.id = id;
        this.devId = devId;
        this.layout = linearlayout;
        this.lastPattern = "";
    }

    public String getId(){return id;}

    public String getDevId() {return devId;}

    public LinearLayout getEndpointlayout() {return layout;}

    public void setEndpointlayout(LinearLayout ll) {this.layout = ll;}

    public double getLastLongitude() {return lastLongitude;}

    public double getLastLatitude() {return lastLatitude;}

    public void setLastLongitude(double d) {this.lastLongitude = d;}

    public void setLastLatitude(double d) {this.lastLatitude = d;}

    public double getLastLongitudeSpeed() {return lastLongitudeSpeed;}

    public double getLastLatitudeSpeed() {return lastLatitudeSpeed;}

    public void setLastLongitudeSpeed(double d) {this.lastLongitudeSpeed = d;}

    public void setLastLatitudeSpeed(double d) {this.lastLatitudeSpeed = d;}

    public float getDistance() {return distance;}

    public void setDistance(float distance) {this.distance = distance;}

    public String getLastPattern() {return lastPattern;}

    public void setLastPattern(String lastPattern) {this.lastPattern = lastPattern;}
}
