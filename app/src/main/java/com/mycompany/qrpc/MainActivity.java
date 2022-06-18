package com.mycompany.qrpc;

import static android.os.Build.VERSION_CODES.S;

import static com.mycompany.qrpc.SerializationHelper.deserialize;
import static com.mycompany.qrpc.SerializationHelper.serialize;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import android.location.Location;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "QRPC";

    // Permisos requeridos en función de la versión de Android del dispositivo
    private static final String[] REQUIRED_PERMISSIONS;

    static {
        if (Build.VERSION.SDK_INT >= S) {
            REQUIRED_PERMISSIONS =
                    new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_ADVERTISE,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                    };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            REQUIRED_PERMISSIONS =
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                    };
        } else {
            REQUIRED_PERMISSIONS =
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.CHANGE_WIFI_STATE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                    };
        }
    }

    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;

    // Módulo de GPS
    GPSModule gpsModule;

    // Módulo de comunicaciones
    CommunicationModule communicationModule;



    // Constantes de ubicación
    public static final int DEFAULT_UPDATE_INTERVAL = 10;
    public static final int FASTEST_UPDATE_INTERVAL = 5;
    private static final int PERMISSIONS_FINE_LOCATION = 99;

    // Cliente de conexión
    private ConnectionsClient connectionsClient;

    // Botones
    private Button findOpponentButton;
    private Button disconnectButton;

    // Textviews
    private TextView opponentText;
    private TextView statusText;

    // UI elements
    TextView tv_lat, tv_lon, tv_altitude,  tv_speed;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        // Instanciamos módulos de GPS y de comunicaciones
        communicationModule = new CommunicationModule(this);
        gpsModule = new GPSModule(this, communicationModule);

        // Guardamos los elementos de la UI
        findOpponentButton = findViewById(R.id.find_opponent);
        disconnectButton = findViewById(R.id.disconnect);

        opponentText = findViewById(R.id.opponent_name);
        statusText = findViewById(R.id.status);

        TextView nameView = findViewById(R.id.name);
        nameView.setText(getString(R.string.codename, communicationModule.getId()));

        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_speed = findViewById(R.id.tv_speed);

        UIModule.resetGUI(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }
    }

    @Override
    protected void onStop() {
        connectionsClient.stopAllEndpoints();
        UIModule.resetGUI(this);

        super.onStop();
    }

    // Devuelve verdadero si se han concedido todos los permisos requeridos a la app
    private static boolean hasPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Gestiona la aceptación o no aceptación de los permisos requeridos
    @CallSuper
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        int i = 0;
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Log.i(TAG, "onRequestPermissionsResult: No se pudo obtener el permiso " + permissions[i]);
                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            i++;
        }
        recreate();
    }

    public void findEndpoint(View view){
        Log.i(TAG, "Comunicación activada");
        communicationModule.findEndpoint();
    }

    public void disconnect(View view){
        Log.i(TAG, "Comunicación desactivada");
        communicationModule.disconnect();
    }
}