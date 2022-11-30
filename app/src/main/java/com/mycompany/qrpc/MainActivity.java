package com.mycompany.qrpc;

import static android.os.Build.VERSION_CODES.S;
import static com.mycompany.qrpc.SerializationHelper.deserialize;
import static com.mycompany.qrpc.SerializationHelper.serialize;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // Instancia de la base de datos
    FirebaseFirestore db;

    // Identificador de la instalación
    String installationId;

    @Override
    protected void onCreate(Bundle bundle) {
        // Obtenemos el identificador de instalación
        installationId = Installation.id(this);

        // Instanciamos la base de datos
        db = FirebaseFirestore.getInstance();

        // Create a new user with a first and last name
        Map<String, Object> user = new HashMap<>();
        user.put("first", "Ada");
        user.put("last", "Lovelace");
        user.put("born", 1815);

        // Add a new document with a generated ID
        db.collection("users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });


        super.onCreate(bundle);
        setContentView(R.layout.main_layout);

        // Instanciamos módulo de comunicaciones
        communicationModule = new CommunicationModule(this, installationId, new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
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
                    Map<String, Double> targetInfo = gpsModule.getCoordinates();
                    float[] distance = {0};
                    Location.distanceBetween(referenceInfo.get("latitude"), referenceInfo.get("longitude"), targetInfo.get("latitude"), targetInfo.get("longitude"), distance);
                    e.setDistance(distance[0]);

                    // Actualizamos el orden en el que se muestran los puntos de conexión
                    UIModule.updateEndpointOrder(activity, communicationModule.getEndpoints());

                    // Actualizamos el patrón del punto de conexión
                    LinearLayout ll = e.getEndpointlayout();
                    if (ll != null){
                        String pattern = PatternLogicModule.calculateAtomicPattern(gpsModule.getCoordinates(), referenceInfo);
                        if (!pattern.equals("")) {
                            // Comprobamos si el patrón es distinto al último calculado
                            String lastPattern = e.getLastPattern();
                            if(!pattern.equals(lastPattern)){
                                // Si es distinto, actualizamos la UI con el nuevo patrón
                                UIModule.updateEndpointLayout(activity, ll, pattern);
                                // Actualizamos el último patrón calculado
                                e.setLastPattern(pattern);
                                // Y lo añadimos a la base de datos
                                Date date = new Date();
                                DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                                df.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
                                Map<String, Object> dbDocument = new HashMap<>();
                                dbDocument.put("pattern", pattern);
                                dbDocument.put("date-time", df.format(date));
                                db.collection(installationId).document(e.getDevId()).collection("pattern_history").document(String.valueOf(date.getTime())).set(dbDocument);
                            }

                        }
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

                // Añadimos el punto de conexión que está esperando a establecer conexión
                communicationModule.addTempEndpoint(endpointId, connectionInfo.getEndpointName(), null);
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
                    text_view.setText(endpointId + ": ");
                    text_view.setTextSize(TypedValue.COMPLEX_UNIT_PT, 15);
                    ll_endpoint.addView(text_view);
                    ImageView pattern_view = new ImageView(activity);
                    pattern_view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    pattern_view.setAdjustViewBounds(true);
                    pattern_view.setMaxHeight(500);
                    ll_endpoint.addView(pattern_view);

                    // Añadimos el LinearLayout a la UI
                    UIModule.addEndpointLayout(activity, ll_endpoint);

                    // Guardamos el punto de conexión que ya ha establecido su conexión
                    communicationModule.saveEndpoint(endpointId, ll_endpoint);

                // Si la conexión falla
                } else {
                    Log.i(TAG, "onConnectionResult: Conexión fallida");
                }
            }

            @Override
            public void onDisconnected(@NonNull String endpointId) {
                // El punto de conexión se ha desconectado
                Log.i(TAG, "onDisconnected: Ha habido una desconexión del punto de conexión");
                LinearLayout ll = communicationModule.getEndpoint(endpointId).getEndpointlayout();
                if (ll != null){
                    UIModule.removeEndpointLayout(activity,ll);
                }
                communicationModule.removeEndpoint(endpointId);
            }
        });

        // Instanciamos módulo de GPS
        gpsModule = new GPSModule(this, location -> {
            Log.i(TAG,"onLocationResult: Ubicación medida satisfactoriamente: " + location.getLongitude());

            // Convertimos las coordenadas en un Map
            Map<String, Double> coordinates = new HashMap<>();
            coordinates.put("longitude", location.getLongitude());
            coordinates.put("latitude", location.getLatitude());

            // Enviamos nuestra ubicación como un Map si hay conexiones establecidas
            if (communicationModule.isThereConnections()) {
                try {
                    communicationModule.sendPayload(Payload.fromBytes(serialize(coordinates)));
                } catch (IOException e) {
                    Log.e(TAG, "onLocationResult: Error al serializar las coordenadas");
                }
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
    public void disconnect(View view) {
        Log.i(TAG, "Comunicación desactivada");
        communicationModule.disconnect();
        gpsModule.stopLocationUpdates();
        UIModule.resetGUI(this);
    }
}