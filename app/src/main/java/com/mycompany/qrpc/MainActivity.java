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
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

    private Activity activity = this;

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

    // Botones
    private Button connectButton;
    private Button disconnectButton;

    // Textviews
    private TextView opponentText;
    private TextView statusText;

    // ScrollView
    private ScrollView scrollview;

    // LinearLayout
    private LinearLayout linearlayout;

    // UI elements
    TextView tv_lat, tv_lon, tv_altitude,  tv_speed;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.main_layout);

        // Instanciamos módulos de GPS y de comunicaciones
        communicationModule = new CommunicationModule(this, new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, Payload payload) {
                try {
                    Log.i(TAG,"onPayloadReceived: Paquete recibido");

                    // Tranformamos el map recibido en un objeto de la clase Location
                    Map<String, Double> targetCoordinates = (HashMap<String, Double>) deserialize(payload.asBytes());
                    Location targetLocation = new Location("provider");
                    targetLocation.setLongitude(targetCoordinates.get("longitude"));
                    targetLocation.setLatitude(targetCoordinates.get("latitude"));
                    targetLocation.setBearing(targetCoordinates.get("bearing").floatValue());
                    targetLocation.setSpeed(targetCoordinates.get("longitude").floatValue());

                    // Actualizamos la UI
                    LinearLayout ll = communicationModule.getEndpointLayout(endpointId);
                    if (ll != null){
                        UIModule.updateEndpointLayout(activity,ll,targetLocation);
                    }

                } catch (IOException | ClassNotFoundException e) {
                    Log.e(TAG, "onPayloadReceived: Error al deserializar el paquete recibido");
                }
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate payloadTransferUpdate) {}

        }, new ConnectionLifecycleCallback() {

            @Override
            public void onConnectionInitiated(@NonNull String endpointId, ConnectionInfo connectionInfo) {
                // Aceptamos la conexión
                Log.i(TAG, "onConnectionInitiated: Aceptando conexión");
                communicationModule.acceptConnection(endpointId);
            }

            @Override
            public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
                // Si la conexión se establece
                if (result.getStatus().isSuccess()) {

                    Log.i(TAG, "onConnectionResult: Conexión establecida");

                    LinearLayout ll_endpoint = new LinearLayout(activity);
                    ll_endpoint.setOrientation(LinearLayout.HORIZONTAL);
                    TextView lon = new TextView(activity);
                    lon.setText("Lon: 0");
                    ll_endpoint.addView(lon);
                    TextView lat = new TextView(activity);
                    lat.setText("Lat: 0");
                    ll_endpoint.addView(lat);
                    TextView bear = new TextView(activity);
                    bear.setText("Bear: 0");
                    ll_endpoint.addView(bear);
                    TextView sp = new TextView(activity);
                    sp.setText("Sp: 0");
                    ll_endpoint.addView(sp);

                    UIModule.addEndpointLayout(activity, ll_endpoint);

                    communicationModule.addEndpoint(endpointId, ll_endpoint);
                    // Si la conexión falla
                } else {
                    Log.i(TAG, "onConnectionResult: Conexión fallida");
                }
            }

            @Override
            public void onDisconnected(@NonNull String endpointId) {
                Log.i(TAG, "onDisconnected: Ha habido una desconexión del punto de conexión");
                LinearLayout ll = communicationModule.getEndpointLayout(endpointId);
                if (ll != null){
                    UIModule.removeEndpointLayout(activity,ll);
                }
            }
        });

        gpsModule = new GPSModule(this, new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Log.i(TAG,"onLocationResult: Ubicación medida satisfactoriamente");

                // Enviamos nuestra ubicación como un Map
                Location currentLocation = locationResult.getLastLocation();
                gpsModule.setCurrentLocation(currentLocation);
                Map<String,Double> coordinates = new HashMap<>();
                coordinates.put("longitude",currentLocation.getLongitude());
                coordinates.put("latitude",currentLocation.getLatitude());
                coordinates.put("bearing",(double)currentLocation.getBearing());
                coordinates.put("speed",(double)currentLocation.getSpeed());
                try {
                    communicationModule.sendPayload(Payload.fromBytes(serialize(coordinates)));
                } catch (IOException e) {
                    Log.e(TAG, "onLocationResult: Error al serializar las coordenadas");
                }
            }
        });

        // Guardamos los elementos de la UI
        connectButton = findViewById(R.id.connect);
        disconnectButton = findViewById(R.id.disconnect);
        scrollview = findViewById(R.id.scroll_view);
        linearlayout = findViewById(R.id.linear_layout);

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
        communicationModule.disconnect();
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

    public void connect(View view){
        Log.i(TAG, "Comunicación activada");
        communicationModule.connect();
        UIModule.setButtonState(this, true);
    }

    public void disconnect(View view){
        Log.i(TAG, "Comunicación desactivada");
        communicationModule.disconnect();
        UIModule.resetGUI(this);
    }
}