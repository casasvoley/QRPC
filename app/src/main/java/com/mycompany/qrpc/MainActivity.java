package com.mycompany.qrpc;

import static android.os.Build.VERSION_CODES.S;
import static com.mycompany.qrpc.SerializationHelper.deserialize;
import static com.mycompany.qrpc.SerializationHelper.serialize;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.main_layout);

        // Instanciamos módulo de comunicaciones
        communicationModule = new CommunicationModule(this, new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, Payload payload) {
                try {
                    Log.i(TAG,"onPayloadReceived: Paquete recibido: " + endpointId);

                    // Deserializamos el map recibido
                    Map<String, Double> referenceInfo = (HashMap<String, Double>) deserialize(payload.asBytes());

                    // Calculamos la velocidad del punto de conexión
                    Endpoint e = communicationModule.getEndpoint(endpointId);
                    double longitude = referenceInfo.get("longitude");
                    double latitude = referenceInfo.get("latitude");
                    double longitude_speed;
                    double latitude_speed;
                    if (longitude == e.getLastLongitude() && latitude == e.getLastLatitude()){
                        longitude_speed = e.getLastLongitudeSpeed();
                        latitude_speed = e.getLastLatitudeSpeed();
                    } else {
                        longitude_speed = longitude - e.getLastLongitude();
                        e.setLastLongitudeSpeed(longitude_speed);
                        latitude_speed = latitude - e.getLastLatitude();
                        e.setLastLatitudeSpeed(latitude_speed);
                    }
                    referenceInfo.put("longitude_speed", longitude_speed);
                    referenceInfo.put("latitude_speed", latitude_speed);

                    // Calculamos la distancia al punto de conexión
                    double distance = distanceToEndpoint(referenceInfo.get("longitude"), referenceInfo.get("latitude"));
                    e.setDistance(distance);

                    // Actualizamos el orden en el que se muestran los puntos de conexión
                    UIModule.updateEndpointOrder(activity, communicationModule.getEndpoints());

                    // Actualizamos el patrón del punto de conexión
                    LinearLayout ll = e.getLinearlayout();
                    if (ll != null){
                        int patternId = PatternLogicModule.calculateAtomicPattern(gpsModule.getCoordinates(), referenceInfo);
                        if (patternId != -1) UIModule.updateEndpointLayout(activity, ll, patternId);
                        /*UIModule.updateEndpointLayout(activity,ll,
                                longitude,latitude,
                                longitude_speed,latitude_speed);*/
                    }

                    // Actualizamos la posición del punto de conexión guardada
                    e.setLastLongitude(referenceInfo.get("longitude"));
                    e.setLastLatitude(referenceInfo.get("latitude"));

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

                    // Creamos el LinearLayout donde se mostrará la información sobre este punto de conexión
                    LinearLayout ll_endpoint = new LinearLayout(activity);
                    ll_endpoint.setOrientation(LinearLayout.HORIZONTAL);
                    TextView text_view = new TextView(activity);
                    text_view.setText("Pattern:");
                    text_view.setTextSize(TypedValue.COMPLEX_UNIT_PT, 15);
                    ll_endpoint.addView(text_view);
                    ImageView pattern_view = new ImageView(activity);
                    ll_endpoint.addView(pattern_view);
                    /*TextView lon = new TextView(activity);
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
                    ll_endpoint.addView(sp);*/

                    // Añadimos el LinearLayout a la UI
                    UIModule.addEndpointLayout(activity, ll_endpoint);

                    // Guardamos el punto de conexión
                    communicationModule.addEndpoint(endpointId, ll_endpoint);

                // Si la conexión falla
                } else {
                    Log.i(TAG, "onConnectionResult: Conexión fallida");
                }
            }

            @Override
            public void onDisconnected(@NonNull String endpointId) {
                // El punto de conexión se ha desconectado
                Log.i(TAG, "onDisconnected: Ha habido una desconexión del punto de conexión");
                LinearLayout ll = communicationModule.getEndpoint(endpointId).getLinearlayout();
                if (ll != null){
                    UIModule.removeEndpointLayout(activity,ll);
                }
            }
        });

        // Instanciamos módulo de GPS
        gpsModule = new GPSModule(this, location -> {
            Log.i(TAG,"onLocationResult: Ubicación medida satisfactoriamente: " + location.getLongitude());

            // Enviamos nuestra ubicación como un Map
            Map<String, Double> coordinates = new HashMap<>();
            coordinates.put("longitude", location.getLongitude());
            coordinates.put("latitude", location.getLatitude());
            try {
                communicationModule.sendPayload(Payload.fromBytes(serialize(coordinates)));
            } catch (IOException e) {
                Log.e(TAG, "onLocationResult: Error al serializar las coordenadas");
            }

            // Accedemos a las coordenadas que tiene guardadas el GPSModule
            Map<String, Double> pastCoordinates = gpsModule.getCoordinates();

            // Si las coordenadas guardadas son nulas
            if (pastCoordinates.get("longitude") == null && pastCoordinates.get("latitude") == null) {
                // Sustituimos la longitud y la latitud por las nuevas y dejamos las velocidades como nulas
                coordinates.put("longitude_speed",null);
                coordinates.put("latitude_speed",null);
                gpsModule.setCoordinates(coordinates);
            // Si las nuevas coordenadas son distintas a las guardadas, las sustuitimos
            } else if (!Objects.equals(coordinates.get("longitude"), pastCoordinates.get("longitude")) || !Objects.equals(coordinates.get("latitude"), pastCoordinates.get("latitude"))){
                coordinates.put("longitude_speed",coordinates.get("longitude") - pastCoordinates.get("longitude"));
                coordinates.put("latitude_speed",coordinates.get("latitude") - pastCoordinates.get("latitude"));
                gpsModule.setCoordinates(coordinates);
            }
        });

        UIModule.resetGUI(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Comprobamos los permisos de la aplicación
        if (!hasPermissions(this, REQUIRED_PERMISSIONS)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Detenemos los módulos de GPS y de comunicación
        gpsModule.stopLocationUpdates();
        communicationModule.disconnect();
        UIModule.resetGUI(this);
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

    // Activa la comunicación y el cálculo de la ubicación
    public void connect(View view){
        Log.i(TAG, "Comunicación activada");
        communicationModule.connect();
        gpsModule.startLocationUpdates(this);
        UIModule.setButtonState(this, true);
    }

    // Desactiva la comunicación y el cálculo de la ubicación
    public void disconnect(View view){
        Log.i(TAG, "Comunicación desactivada");
        communicationModule.disconnect();
        gpsModule.stopLocationUpdates();
        UIModule.resetGUI(this);
    }

    // Calcula la distancia entre el dispositivo y un punto de conexión,
    // sabiendo su longitud y su latitud
    public double distanceToEndpoint(double longitud, double latitud) {
        // Ver fórmulas de Vincenty
    }
}