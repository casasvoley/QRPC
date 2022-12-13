package com.mycompany.qrpc;

import static android.os.Build.VERSION_CODES.S;
import static com.mycompany.qrpc.SerializationHelper.deserialize;
import static com.mycompany.qrpc.SerializationHelper.serialize;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

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

    private static final int REQUEST_CODE = 1;

    // Período de actualización del orden de los
    private final int DEVICE_ORDER_UPDATE_TIME = 2;

    // Módulo de GPS
    private GPSModule gpsModule;

    // Módulo de comunicaciones
    private CommunicationModule communicationModule;

    // Instancia de la base de datos
    private FirebaseFirestore db;

    // Identificador de la instalación
    private String installationId;

    // Timer que actualiza el orden de los dispositivos en pantalla periódicamente
    private Timer timer;
    // Tarea que ejecuta el timer
    private TimerTask timerTask;

    @SuppressLint("InvalidWakeLockTag")
    @Override
    protected void onCreate(Bundle bundle) {
        // Obtenemos el identificador de instalación
        installationId = Installation.id(this);

        // Instanciamos la base de datos
        db = FirebaseFirestore.getInstance();

        super.onCreate(bundle);
        setContentView(R.layout.main_layout);

        // Instanciamos módulo de comunicaciones
        communicationModule = new CommunicationModule(this, installationId,
                new PayloadCallback() {
            @Override
            public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                try {
                    Log.i(TAG,"onPayloadReceived: Paquete recibido: " + endpointId);

                    // Deserializamos el map recibido
                    Map<String, Double> referenceInfo =
                            (HashMap<String, Double>) deserialize(payload.asBytes());

                    // Actualizamos las variables del Endpoint asociado
                    Endpoint e = communicationModule.getEndpoint(endpointId);
                    e.setLastLongitude(referenceInfo.get("longitude"));
                    e.setLastLatitude(referenceInfo.get("latitude"));
                    if (referenceInfo.get("longitude_speed")!=null) {
                        e.setLastLongitudeSpeed(referenceInfo.get("longitude_speed"));
                    }
                    if (referenceInfo.get("latitude_speed")!=null) {
                        e.setLastLatitudeSpeed(referenceInfo.get("latitude_speed"));
                    }

                    // Calculamos la distancia al punto de conexión y la guardamos
                    Map<String, Double> targetInfo = gpsModule.getCoordinates();
                    float[] distance = {0};
                    if (!referenceInfo.containsValue(null)
                            && !targetInfo.containsValue(null)) {
                        Location.distanceBetween(referenceInfo.get("latitude"),
                                referenceInfo.get("longitude"), targetInfo.get("latitude"),
                                targetInfo.get("longitude"), distance);
                        e.setDistance(distance[0]);
                    }

                    // Calculamos y actualizamos el patrón atómico del punto de conexión
                    LinearLayout ll = e.getEndpointlayout();
                    if (ll != null){
                        String pattern = PatternLogicModule
                                .calculateAtomicPattern(gpsModule.getCoordinates(), referenceInfo);
                        if (!pattern.equals("")) {
                            // Comprobamos si el patrón es distinto al último calculado
                            String lastPattern = e.getLastPattern();
                            if(!pattern.equals(lastPattern)){
                                // Si es distinto, actualizamos la UI con el nuevo patrón
                                UIModule.updateEndpointLayout(activity, ll, pattern);
                                // Actualizamos el último patrón calculado
                                e.setLastPattern(pattern);
                                // Guardamos la fecha y hora a la que se ha detectado
                                // el patrón atómico
                                Date date = new Date();
                                DateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                                df.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
                                // Guardamos el patrón, la fecha y la hora en un map
                                Map<String, Object> dbDocument = new HashMap<>();
                                dbDocument.put("pattern", pattern);
                                dbDocument.put("date-time", df.format(date));
                                // Y almacenamos el map como un documento en la base de datos
                                db.collection(installationId).document(e.getDevId())
                                        .collection("pattern_history")
                                        .document(String.valueOf(date.getTime())).set(dbDocument);
                            }

                        }
                    }

                    // Actualizamos la posición del punto de conexión guardada
                    e.setLastLongitude(referenceInfo.get("longitude"));
                    e.setLastLatitude(referenceInfo.get("latitude"));

                } catch (IOException | ClassNotFoundException e) {
                    Log.e(TAG, "onPayloadReceived: Error al deserializar el paquete recibido");
                } catch (Exception ignored){}
            }

            @Override
            public void onPayloadTransferUpdate(String endpointId,
                                                PayloadTransferUpdate payloadTransferUpdate) {}

        }, new ConnectionLifecycleCallback() {

            @Override
            public void onConnectionInitiated(@NonNull String endpointId,
                                              ConnectionInfo connectionInfo) {

                // Comprobamos si este dispositivo ya está pendiente de establecer una conexión
                boolean isThisEndpointRepeated = false;

                ArrayList<Endpoint> tempEndpoints = communicationModule.getTempEndpoints();
                for (Endpoint e : tempEndpoints){
                    if (e.getDevId().equals(connectionInfo.getEndpointName())){
                        isThisEndpointRepeated = true;
                        break;
                    }
                }

                // Si no, comprobamos si ya existe una coexión establecida con este dispositivo
                if(!isThisEndpointRepeated) {
                    ArrayList<Endpoint> endpoints = communicationModule.getEndpoints();
                    for (Endpoint e : endpoints) {
                        if (e.getDevId().equals(connectionInfo.getEndpointName())) {
                            isThisEndpointRepeated = true;
                            break;
                        }
                    }
                }

                // Si no, iniciamos el proceso de establecimiento de conexión
                if(!isThisEndpointRepeated) {
                    // Aceptamos la conexión
                    Log.i(TAG, "onConnectionInitiated: Aceptando conexión");
                    communicationModule.acceptConnection(endpointId);

                    // Añadimos el punto de conexión que está esperando a establecer conexión
                    communicationModule.addTempEndpoint(endpointId,
                            connectionInfo.getEndpointName(), null);
                }
            }

            @Override
            public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
                // Si la conexión se establece
                if (result.getStatus().isSuccess()) {

                    Log.i(TAG, "onConnectionResult: Conexión establecida");

                    // Creamos el LinearLayout donde se mostrará la información
                    // sobre este punto de conexión
                    LinearLayout ll_endpoint = new LinearLayout(activity);
                    ll_endpoint.setOrientation(LinearLayout.HORIZONTAL);
                    ll_endpoint.setBackgroundColor(Color.rgb(
                            endpointId.charAt(0)+endpointId.charAt(1),
                            endpointId.charAt(0)+endpointId.charAt(2),
                            endpointId.charAt(0)+endpointId.charAt(3)));
                    ll_endpoint.getBackground().setAlpha(200);
                    TextView text_view = new TextView(activity);
                    text_view.setText(endpointId + ": ");
                    text_view.setTextSize(TypedValue.COMPLEX_UNIT_PT, 15);
                    text_view.setGravity(Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
                    float density = getResources().getDisplayMetrics().density;
                    params.setMargins((int) (15*density),0,0,0);
                    text_view.setLayoutParams(params);
                    ll_endpoint.addView(text_view);
                    ImageView pattern_view = new ImageView(activity);
                    pattern_view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        pattern_view.setForegroundGravity(Gravity.CENTER_HORIZONTAL);
                    }
                    params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
                    pattern_view.setLayoutParams(params);
                    pattern_view.setAdjustViewBounds(true);
                    pattern_view.setMaxHeight((int) (180*density));
                    ll_endpoint.addView(pattern_view);


                    // Añadimos el LinearLayout a la UI
                    UIModule.addEndpointLayout(activity, ll_endpoint);

                    // Guardamos el punto de conexión que ya ha establecido su conexión
                    communicationModule.saveEndpoint(endpointId, ll_endpoint);

                // Si la conexión falla
                } else {
                    Log.i(TAG, "onConnectionResult: Conexión fallida: ");
                    communicationModule.removeTempEndpoint(endpointId);
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
            if (location != null) {
                Log.i(TAG, "onLocationResult: Ubicación medida satisfactoriamente" );

                // Convertimos las coordenadas en un Map
                Map<String, Double> coordinates = new HashMap<String, Double>();
                coordinates.put("longitude", location.getLongitude());
                coordinates.put("latitude", location.getLatitude());

                // Accedemos a las coordenadas que tiene guardadas el GPSModule
                Map<String, Double> pastCoordinates = gpsModule.getCoordinates();

                // Si las coordenadas guardadas son nulas
                if (pastCoordinates.get("longitude") == null
                        && pastCoordinates.get("latitude") == null) {
                    // Sustituimos la longitud y la latitud por las nuevas
                    // y dejamos las velocidades como nulas
                    coordinates.put("longitude_speed", null);
                    coordinates.put("latitude_speed", null);
                    coordinates.put("has_speed", 0.0);
                    gpsModule.setCoordinates(coordinates);
                // Si el módulo del vecotr de desplazamiento es mayor
                // que el margen de sensibilidad,  consideramos las coordenadas como válidas
                } else if (
                        Math.sqrt(Math.pow((coordinates.get("longitude") - pastCoordinates.get("longitude")), 2)
                                + Math.pow(coordinates.get("latitude") - pastCoordinates.get("latitude"), 2))
                                > gpsModule.getSensibility()) {
                    coordinates.put("longitude_speed",
                            coordinates.get("longitude") - pastCoordinates.get("longitude"));
                    coordinates.put("latitude_speed",
                            coordinates.get("latitude") - pastCoordinates.get("latitude"));
                    coordinates.put("has_speed", 1.0);
                    gpsModule.setCoordinates(coordinates);
                // Si no hay una velocidad guardada y la actual es distinta de 0, la guardamos
                } else if (pastCoordinates.get("has_speed") == 0.0
                        && (coordinates.get("longitude") - pastCoordinates.get("longitude"))!=0.0
                        && (coordinates.get("latitude") - pastCoordinates.get("latitude"))!=0.0){
                    coordinates.put("longitude_speed",
                            coordinates.get("longitude") - pastCoordinates.get("longitude"));
                    coordinates.put("latitude_speed",
                            coordinates.get("latitude") - pastCoordinates.get("latitude"));
                    coordinates.put("has_speed", 1.0);
                    gpsModule.setCoordinates(coordinates);
                // Si no hay una velocidad guardada, usamos el encaramiento
                } else if (pastCoordinates.get("has_speed") == 0.0){
                    coordinates.put("longitude_speed", (double) Math.sin(location.getBearing()));
                    coordinates.put("latitude_speed", (double) Math.cos(location.getBearing()));
                    coordinates.put("has_speed", 1.0);
                    gpsModule.setCoordinates(coordinates);
                // Si no, nos quedamos con la velocidad que teníamos guaradada
                } else {
                    coordinates.put("longitude_speed", pastCoordinates.get("longitude_speed"));
                    coordinates.put("latitude_speed", pastCoordinates.get("latitude_speed"));
                    coordinates.put("has_speed", pastCoordinates.get("has_speed"));
                    gpsModule.setCoordinates(coordinates);
                }

                // Enviamos nuestra ubicación como un Map si hay conexiones establecidas
                if (communicationModule.isThereConnections()) {
                    try {
                        communicationModule.sendPayload(Payload.fromBytes(serialize(coordinates)));
                    } catch (IOException e) {
                        Log.e(TAG, "onLocationResult: Error al serializar las coordenadas");
                    }
                }
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
                requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Detenemos los módulos de GPS y de comunicación
        gpsModule.stopLocationUpdates();
        communicationModule.disconnect();
        // Devolvemos la interfaz al estado inicial
        UIModule.resetGUI(this);
        // Permitimos qeu la pantalla se bloquee por ausencia de interacción
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Detenemos la actualización periódica del orden de los dispositivos
        timer.cancel();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Detenemos los módulos de GPS y de comunicación
        gpsModule.stopLocationUpdates();
        communicationModule.disconnect();
        // Devolvemos la interfaz al estado inicial
        UIModule.resetGUI(this);
        // Permitimos qeu la pantalla se bloquee por ausencia de interacción
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Detenemos la actualización periódica del orden de los dispositivos
        timer.cancel();
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

        if (requestCode != REQUEST_CODE) {
            return;
        }

        int i = 0;
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Log.i(TAG, "onRequestPermissionsResult: No se pudo obtener el permiso "
                        + permissions[i]);
                Toast.makeText(this, R.string.error_missing_permissions,
                        Toast.LENGTH_LONG).show();
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
        // Activamos la comunicación y el GPS
        communicationModule.connect();
        gpsModule.startLocationUpdates(this);

        // Cambiamos el texto y la función del botón
        UIModule.setButtonState(this, true);

        // Impedimos que la pantalla se apague por ausencia de interacción
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Iniciamos el timer que actualiza el orden de los dispositivos en pantalla periódicamente
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        UIModule.updateEndpointOrder(activity, communicationModule.getEndpoints());
                    }
                });
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, DEVICE_ORDER_UPDATE_TIME*1000);
    }

    // Desactiva la comunicación y el cálculo de la ubicación
    public void disconnect(View view) {
        Log.i(TAG, "Comunicación desactivada");
        // Detenemos los módulos de GPS y de comunicación
        gpsModule.stopLocationUpdates();
        communicationModule.disconnect();
        // Devolvemos la interfaz al estado inicial
        UIModule.resetGUI(this);
        // Permitimos qeu la pantalla se apague por ausencia de interacción
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Detenemos la actualización periódica del orden de los dispositivos
        timer.cancel();
    }
}