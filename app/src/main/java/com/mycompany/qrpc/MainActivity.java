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

    // Estrategia de conexión
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    // Constantes de ubicación
    public static final int DEFAULT_UPDATE_INTERVAL = 10;
    public static final int FASTEST_UPDATE_INTERVAL = 5;
    private static final int PERMISSIONS_FINE_LOCATION = 99;

    // Cliente de conexión
    private ConnectionsClient connectionsClient;

    // Id del dispositivo
    private final String id = CodenameGenerator.generate();

    // Datos del oponente
    private String opponentEndpointId;
    private String opponentName;

    // Botones
    private Button findOpponentButton;
    private Button disconnectButton;

    // Textviews
    private TextView opponentText;
    private TextView statusText;

    // UI elements
    TextView tv_lat, tv_lon, tv_altitude,  tv_speed;

    // Location request
    LocationRequest locationRequest;

    // Location callback
    LocationCallback locationCallback;

    // Location provider client
    FusedLocationProviderClient fusedLocationProviderClient;

    // Callbacks cuando se reciben payloads
    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
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
                        updateUIValues(targetLocation);
                    } catch (IOException | ClassNotFoundException e) {
                        Log.e(TAG, "onPayloadReceived: Error al deserializar el paquete recibido");
                    }

                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate update) {

                }
            };



    // Callbacks cuando se encuentran otros dispositivos
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {

                @Override
                public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
                    Log.i(TAG, "onEndpointFound: Punto de conexión encontrado");

                    // Pedimos conectar al ppunto de conexión encontrado
                    connectionsClient.requestConnection(id, endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(@NonNull String endpointId) {

                }
            };

    // Callbacks cuando se conecta a otros dsipositivos
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(@NonNull String endpointId, ConnectionInfo connectionInfo) {
                    // Aceptamos la conexión
                    Log.i(TAG, "onConnectionInitiated: Aceptando conexión");
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
                    opponentName = connectionInfo.getEndpointName();
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
                    // Si la conexión se establece
                    if (result.getStatus().isSuccess()) {
                        Log.i(TAG, "onConnectionResult: Conexión establecida");

                        connectionsClient.stopDiscovery();
                        connectionsClient.stopAdvertising();

                        opponentEndpointId = endpointId;
                        setOpponentName(opponentName);
                        setStatusText(getString(R.string.status_connected));
                        setButtonState(true);

                    // Si la conexión falla
                    } else {
                        Log.i(TAG, "onConnectionResult: Conexión fallida");
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    Log.i(TAG, "onDisconnected: Ha habido una desconexión del punto de conexión");
                    resetGUI();
                }
            };

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        // Guardamos los elementos de la UI
        findOpponentButton = findViewById(R.id.find_opponent);
        disconnectButton = findViewById(R.id.disconnect);

        opponentText = findViewById(R.id.opponent_name);
        statusText = findViewById(R.id.status);

        TextView nameView = findViewById(R.id.name);
        nameView.setText(getString(R.string.codename, id));

        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_speed = findViewById(R.id.tv_speed);

        // Creamos un cliente de conexiones
        connectionsClient = Nearby.getConnectionsClient(this);

        resetGUI();

        // Establecemos las propiedades de las peticiones de unicación (LocalizationRequest)
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(1000 * DEFAULT_UPDATE_INTERVAL);
        locationRequest.setFastestInterval(1000 * FASTEST_UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Callback que se ejecuta cuando el dispositivo recibe una actualización de su ubicación
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Log.i(TAG,"onLocationResult: Ubicación medida satisfactoriamente");

                // Enviamos nuestra ubicación como un Map
                Location location = locationResult.getLastLocation();
                Map<String,Double> coordinates = new HashMap<>();
                coordinates.put("longitude",location.getLongitude());
                coordinates.put("latitude",location.getLatitude());
                coordinates.put("bearing",(double)location.getBearing());
                coordinates.put("speed",(double)location.getSpeed());
                try {
                    connectionsClient.sendPayload(
                            opponentEndpointId, Payload.fromBytes(serialize(coordinates)));
                    Log.i(TAG,"onLocationResult: Location package sent");
                } catch (IOException e) {
                    Log.e(TAG, "onLocationResult: Error al serializar las coordenadas");
                }

            }
        };

        // Creamos un cliente del proveedor de ubicación
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        // Iniciamos las actualizaciones de ubicación
        startLocationUpdates();
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
        resetGUI();

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

    // Encontrar objetivo
    public void findEndpoint(View view) {
        startAdvertising();
        startDiscovery();
        setStatusText(getString(R.string.status_searching));
        findOpponentButton.setEnabled(false);
    }

    // Desconectarse del objetivo y reiniciar la UI
    public void disconnect(View view) {
        connectionsClient.disconnectFromEndpoint(opponentEndpointId);
        resetGUI();
    }

    // Iniciar descubrimiento de dispositivos objetivos
    private void startDiscovery() {
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startDiscovery(
                getPackageName(), endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }

    // Iniciar anuncios de nuestra presencia
    private void startAdvertising() {
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startAdvertising(
                id, getPackageName(), connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }

    // Elimina el estado del juego y actualiza la GUI en consecuencia
    private void resetGUI() {
        opponentEndpointId = null;
        opponentName = null;

        tv_lon.setText("0.0");
        tv_lat.setText("0.0");
        tv_altitude.setText("0.0");
        tv_speed.setText("0.0");


        setOpponentName(getString(R.string.no_opponent));
        setStatusText(getString(R.string.status_disconnected));
        setButtonState(false);
    }

    // Activa y desactiva los botones de la GUI en función del estado de la conexión
    private void setButtonState(boolean connected) {
        findOpponentButton.setEnabled(true);
        findOpponentButton.setVisibility(connected ? View.GONE : View.VISIBLE);
        disconnectButton.setVisibility(connected ? View.VISIBLE : View.GONE);
    }

    // Muestra al usuario un mensaje de estado
    private void setStatusText(String text) {
        statusText.setText(text);
    }

    // Establece el nombre del oponente
    private void setOpponentName(String opponentName) {
        opponentText.setText(getString(R.string.opponent_name, opponentName));
    }

    // Actualiza los valores de la UI
    private void updateUIValues(Location location) {
        tv_lat.setText(String.valueOf(location.getLatitude()));
        tv_lon.setText(String.valueOf(location.getLongitude()));

        if (location.hasBearing()) {
            tv_altitude.setText(String.valueOf(location.getBearing()));
        } else {
            tv_altitude.setText("Not available");
        }

        if (location.hasSpeed()) {
            tv_speed.setText(String.valueOf(location.getSpeed()));
        } else {
            tv_speed.setText("Not available");
        }
    }

    // Request permissions, get location and update UI
    private void updateGPS() {
        // Get permissions if the app does not have them already


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // We have the permissions: get location
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Update UI
                    // updateUIValues(location);


                }
            });
        } else {
            // We do not have the permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions( new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }

    }

    // Start location tracking
    private void startLocationUpdates() {
        Log.i(TAG,"startLocationUpdates: Iniciando las actualizaciones de ubicación");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Pedimos los permisos ya que no los tenemos
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions( new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }
        }

        // Activamos las actualizaciones de ubicación
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        updateGPS();
    }
}