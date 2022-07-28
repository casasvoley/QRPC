package com.mycompany.qrpc;

import static androidx.core.app.ActivityCompat.requestPermissions;

import static com.mycompany.qrpc.SerializationHelper.serialize;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class GPSModule {

    private static final String TAG = "QRPC";

    // Constantes de ubicación
    private static final double DEFAULT_UPDATE_INTERVAL = 1;
    private static final int FASTEST_UPDATE_INTERVAL = 0;
    private static final double MAX_LOCATION_LIFETIME = 0;
    private static final int PERMISSIONS_FINE_LOCATION = 99;

    // Cliente del proveedor de ubicación
    private FusedLocationProviderClient fusedLocationProviderClient;

    // Location request
    private LocationRequest locationRequest;

    // Parámetros de la solicitud de ubicación
    private CurrentLocationRequest currentLocationRequest;

    // Location callback
    private LocationCallback locationCallback;

    // Actividad principal
    Activity activity;

    // Ubicación actual
    private Map<String,Double> coordinates;

    // Handler que calcula la ubicación periódicamente
    private Handler gpsHandler;

    // Timer
    private Timer timer;


    // CancellationTokenSource que cancela el funcionamiento del handler
    private CancellationTokenSource cancellationSource;


    // Listener que se ejecuta cuando se recibe una actualización de la ubicación
    public interface GPSModuleListener {
        public void onNewLocationUpdate(Location location);
    }

    private OnSuccessListener<Location> gpsListener;

    // Constructor
    public GPSModule(Activity activity, OnSuccessListener<Location> listener) {
        this.activity = activity;

        Map<String,Double> map = new HashMap<>();
        map.put("longitude", null);
        map.put("latitude", null);
        map.put("longitude_speed", null);
        map.put("latitude_speed", null);
        this.coordinates = map;

        // Establecemos las propiedades de las peticiones de ubicación (LocalizationRequest)
        locationRequest = LocationRequest.create();
        locationRequest.setInterval((long) (1000 * DEFAULT_UPDATE_INTERVAL));
        locationRequest.setFastestInterval(1000 * FASTEST_UPDATE_INTERVAL);
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        currentLocationRequest = new CurrentLocationRequest.Builder().setMaxUpdateAgeMillis((long) (1000 * MAX_LOCATION_LIFETIME)).build();

        // Listener que se ejecuta cuando el dispositivo recibe una actualización de su ubicación
        this.gpsListener = listener;

        // Instanciamos el handler
        this.gpsHandler = new Handler();

        // Creamos un cliente del proveedor de ubicación
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Pedimos los permisos ya que no los tenemos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    public Map<String, Double> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Map<String,Double> m) {
        this.coordinates = m;
    }

    // Inicia las actualizaciones de ubicación
    public void startLocationUpdates(Activity activity) {

        Log.i(TAG, "startLocationUpdates: Iniciando las actualizaciones de ubicación");

        // Activamos las actualizaciones de ubicación
        /*gpsHandler = new Handler();
        cancellationSource = new CancellationTokenSource();
        gpsHandler.postDelayed(new Runnable() {
            public void run() {

                // Calculamos la ubicación actual
                fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationSource.getToken()).addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Ejecutamos el listener al recibir una nueva actualización de la ubicación
                        if (gpsListener != null) {
                            gpsListener.onNewLocationUpdate(location);
                        }
                    }
                });
                gpsHandler.postDelayed(this, (long) (DEFAULT_UPDATE_INTERVAL * 1000));

            }
        }, (long) (DEFAULT_UPDATE_INTERVAL * 1000));*/

        /*if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Pedimos los permisos ya que no los tenemos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);*/

        // Instanciamos el CancellationTokenSource
        this.cancellationSource = new CancellationTokenSource();

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Pedimos los permisos ya que no los tenemos
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
                    }
                }
                fusedLocationProviderClient.getCurrentLocation(currentLocationRequest, cancellationSource.getToken()).addOnSuccessListener(gpsListener);
            }
        }, 0, (long) (DEFAULT_UPDATE_INTERVAL*1000));

    }

    // Detiene las actualizaciones de ubicación
    public void stopLocationUpdates(){
        //fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        cancellationSource.cancel();
        gpsHandler.removeCallbacksAndMessages(null);
    }

    // Solicita una nueva ubicación
    /*public void requestLocation() {
        gpsHandler.postDelayed(new Runnable() {
            public void run() {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // Pedimos los permisos ya que no los tenemos
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
                    }
                }
                fusedLocationProviderClient.getCurrentLocation(currentLocationRequest, cancellationSource.getToken()).addOnSuccessListener(gpsListener);

                requestLocation();
            }
        }, (long) (DEFAULT_UPDATE_INTERVAL*1000));
    }*/
}
