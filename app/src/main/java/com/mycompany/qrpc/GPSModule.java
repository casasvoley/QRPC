package com.mycompany.qrpc;

import static androidx.core.app.ActivityCompat.requestPermissions;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class GPSModule {

    private static final String TAG = "QRPC";

    // Constantes de ubicación
    private static final double DEFAULT_UPDATE_INTERVAL = 2.2;
    private static final double MAX_LOCATION_LIFETIME = 0;
    private static final int PERMISSIONS_FINE_LOCATION = 99;

    // Sensibilidad para considerar un cambio de coordenadas como un movimiento voluntario
    private final double sensibility = 4E-5;

    // Cliente del proveedor de ubicación
    private FusedLocationProviderClient fusedLocationProviderClient;

    // Parámetros de la solicitud de ubicación
    private CurrentLocationRequest currentLocationRequest;

    // Actividad principal
    private Activity activity;


    private ArrayList<Map<String,Double>> coordinates;

    // Ubicación actual
    private Map<String,Double> currentCoordinates;

    // Timer
    private Timer timer;

    // CancellationTokenSource que cancela el funcionamiento del handler
    private CancellationTokenSource cancellationSource;

    // Listener que se ejecuta al recibir una nueva actualización de la ubicación
    private OnSuccessListener<Location> gpsListener;

    // Constructor
    public GPSModule(Activity activity, OnSuccessListener<Location> listener) {
        this.activity = activity;

        // Inicializamos las coordenadas guardadas como nulas
        Map<String,Double> map = new HashMap<>();
        map.put("longitude", null);
        map.put("latitude", null);
        map.put("longitude_speed", null);
        map.put("latitude_speed", null);
        map.put("has_speed", 0.0);
        this.coordinates = new ArrayList<Map<String,Double>>();

        // Establecemos las propiedades de las peticiones de ubicación (CurrentLocalizationRequest)
        currentLocationRequest = new CurrentLocationRequest.Builder()
                .setMaxUpdateAgeMillis((long) (1000 * MAX_LOCATION_LIFETIME)).build();

        // Listener que se ejecuta cuando el dispositivo recibe una actualización de su ubicación
        this.gpsListener = listener;

        // Creamos un cliente del proveedor de ubicación
        this.fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity);
        // Comprobamos si tenemos los permisos de ubicación
        if (ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Pedimos los permisos ya que no los tenemos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSIONS_FINE_LOCATION);
            }
        }
    }

    // Devuelve las coordenadas guardadas
    public ArrayList<Map<String, Double>> getCoordinates() {
        return coordinates;
    }

    // Actualiza las coordenadas guardadas
    public void setCoordinates(Map<String,Double> m) {
        if(!this.coordinates.contains(m)) this.coordinates.add(m);
    }

    // TEMPORAL
    public void deleteCoordinates() { this.coordinates = new ArrayList<Map<String,Double>>(); }

    // Inicia las actualizaciones de ubicación
    public void startLocationUpdates(Activity activity) {

        Log.i(TAG, "startLocationUpdates: Iniciando las actualizaciones de ubicación");

        // Instanciamos el CancellationTokenSource
        this.cancellationSource = new CancellationTokenSource();

        // Creamos un Timer que realizará una tarea periódicamente
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                activity.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // Pedimos los permisos ya que no los tenemos
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
                            }
                        }

                        // Si no está la ubicación activada, pedimos al usario que la active
                        if (!isLocationActivated()){
                            showSettingsAlert();
                            timer.cancel();
                        }

                        // Solicitamos una actualización de la ubicación actual
                        fusedLocationProviderClient.getCurrentLocation(currentLocationRequest, cancellationSource.getToken()).addOnSuccessListener(gpsListener);
                    }
                });
            }
        }, 0, (long) (DEFAULT_UPDATE_INTERVAL*1000));

    }

    // Detiene las actualizaciones de ubicación
    public void stopLocationUpdates(){
        if (timer!=null){timer.cancel();}
    }

    // Comprueba si el usuario tiene los servicios de ubicación activados
    public boolean isLocationActivated() {
        LocationManager lm;
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        lm = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {
        }

        try {
            networkEnabled = lm
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ignored) {
        }

        return gpsEnabled && networkEnabled;
    }

    // Muestra al usuario una alerta para activar los servicios de ubicación
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);

        // Título de la alerta
        alertDialog.setTitle("Active los servicios de ubicación");

        // Texto de la alerta
        alertDialog.setMessage("La aplicación necesita tener acceso a la ubicación del dispositivo para funcionar correctamente.");

        // Botón de la alerta
        alertDialog.setPositiveButton(
                activity.getResources().getString(R.string.button_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        activity.startActivity(intent);
                    }
                });
        // Mostramos la alerta al usuario
        activity.runOnUiThread(alertDialog::show);
    }

    // Devuelve la sensibilidad del módulo GPS
    public double getSensibility(){return sensibility;}

    public Map<String, Double> getCurrentCoordinates() {
        return currentCoordinates;
    }

    public void setCurrentCoordinates(Map<String, Double> currentCoordinates) {
        this.currentCoordinates = currentCoordinates;
    }
}
