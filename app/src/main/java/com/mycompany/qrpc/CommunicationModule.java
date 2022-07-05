package com.mycompany.qrpc;

import static com.mycompany.qrpc.SerializationHelper.deserialize;
import static com.mycompany.qrpc.SerializationHelper.serialize;

import android.app.Activity;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CommunicationModule {

    private static final String TAG = "QRPC";

    // Cliente de conexiones
    private ConnectionsClient connectionsClient;

    // Id del dispositivo
    private final String id = CodenameGenerator.generate();

    // Nombre del paquete
    private String packageName;

    // Estrategia de conexión
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    // Datos de las conexiones
    private ArrayList<Endpoint> endpoints;

    // Actividad principal
    private Activity activity;


    // Callbacks cuando nuestro dispositivo se conecta a otros dispositivos
    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(@NonNull String endpointId, ConnectionInfo connectionInfo) {
                    // Aceptamos la conexión
                    Log.i(TAG, "onConnectionInitiated: Aceptando conexión de");
                    connectionsClient.acceptConnection(endpointId, payloadCallback);
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

                        endpoints.add(new Endpoint(endpointId, ll_endpoint));
                    // Si la conexión falla
                    } else {
                        Log.i(TAG, "onConnectionResult: Conexión fallida");
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    Log.i(TAG, "onDisconnected: Ha habido una desconexión del punto de conexión");
                    LinearLayout ll = null;
                    for (Endpoint e : endpoints){
                        if (e.getId().equals(endpointId)){
                            ll = e.getLinearlayout();
                        }
                    }
                    if (ll != null){
                        UIModule.removeEndpointLayout(activity,ll);
                    }
                }
            };

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
                        LinearLayout ll = null;
                        for (Endpoint e : endpoints){
                            if (e.getId().equals(endpointId)){
                                ll = e.getLinearlayout();
                            }
                        }
                        if (ll != null){
                            UIModule.updateEndpointLayout(activity,ll,targetLocation);
                        }

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

                    // Pedimos conectar al punto de conexión encontrado
                    connectionsClient.requestConnection(id, endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(@NonNull String endpointId) {

                }
            };

    public CommunicationModule(Activity activity){
        // Creamos un cliente de conexiones
        connectionsClient = Nearby.getConnectionsClient(activity);

        packageName = activity.getPackageName();
        this.activity = activity;
        this.endpoints = new ArrayList<Endpoint>();
    }

    // Iniciar anuncios de nuestra presencia
    private void startAdvertising() {
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startAdvertising(
                id, packageName, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }

    // Encontrar objetivo
    public void connect() {
        connectionsClient.startAdvertising(
                id, packageName, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
        connectionsClient.startDiscovery(
                activity.getPackageName(), endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());

        UIModule.setButtonState(activity, true);
    }

    // Desconectarse del objetivo y reiniciar la UI
    public void disconnect() {
        connectionsClient.stopAllEndpoints();
        connectionsClient.stopAdvertising();
        connectionsClient.stopDiscovery();

        UIModule.resetGUI(activity);
    }

    // Iniciar descubrimiento de dispositivos objetivos
    private void startDiscovery() {
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startDiscovery(
                activity.getPackageName(), endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }

    public void sendPayload(Payload payload){
        //CAMBIAR ID!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        connectionsClient.sendPayload(
                "34", payload);
        Log.i(TAG,"onLocationResult: Ubicación enviada");
    }

    // Devuelve la id del dispositivo
    public String getId(){return id;}
}
