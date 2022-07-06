package com.mycompany.qrpc;

import static androidx.core.app.ActivityCompat.requestPermissions;

import static com.mycompany.qrpc.SerializationHelper.serialize;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.nearby.connection.Payload;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GPSModule {

    private static final String TAG = "QRPC";

    // Constantes de ubicación
    public static final int DEFAULT_UPDATE_INTERVAL = 2;
    public static final int FASTEST_UPDATE_INTERVAL = 1;
    private static final int PERMISSIONS_FINE_LOCATION = 99;

    // Cliente del proveedor de ubicación
    private FusedLocationProviderClient fusedLocationProviderClient;

    // Location request
    private LocationRequest locationRequest;

    // Location callback
    private LocationCallback locationCallback;

    // Actividad principal
    Activity activity;

    // Ubicación actual
    private Location currentLocation;

    // Constructor
    public GPSModule(Activity activity, LocationCallback locationCallback) {
        this.activity = activity;

        // Establecemos las propiedades de las peticiones de unicación (LocalizationRequest)
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(1000 * FASTEST_UPDATE_INTERVAL);
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);

        // Callback que se ejecuta cuando el dispositivo recibe una actualización de su ubicación
        this.locationCallback = locationCallback;

        // Creamos un cliente del proveedor de ubicación
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);

        // Iniciamos las actualizaciones de ubicación
        startLocationUpdates(activity);
    }

    public Location getCurrentLocation(){
        return currentLocation;
    }

    public void setCurrentLocation(Location location){
        this.currentLocation = location;
    }

    // Inicia las actualizaciones de ubicación
    public void startLocationUpdates(Activity activity) {
        Log.i(TAG,"startLocationUpdates: Iniciando las actualizaciones de ubicación");

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Pedimos los permisos ya que no los tenemos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }

        // Activamos las actualizaciones de ubicación
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }
}
