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
    private final ConnectionLifecycleCallback connectionLifecycleCallback;


    // Callbacks cuando se reciben payloads
    private final PayloadCallback payloadCallback;


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

    public CommunicationModule(Activity activity, PayloadCallback payloadCallback, ConnectionLifecycleCallback connectionLifecycleCallback){
        // Creamos un cliente de conexiones
        connectionsClient = Nearby.getConnectionsClient(activity);

        packageName = activity.getPackageName();
        this.activity = activity;
        this.payloadCallback = payloadCallback;
        this.connectionLifecycleCallback = connectionLifecycleCallback;
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
    }

    // Desconectarse del objetivo y reiniciar la UI
    public void disconnect() {
        connectionsClient.stopAllEndpoints();
        connectionsClient.stopAdvertising();
        connectionsClient.stopDiscovery();
    }

    // Iniciar descubrimiento de dispositivos objetivos
    private void startDiscovery() {
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startDiscovery(
                activity.getPackageName(), endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }

    public void sendPayload(Payload payload){
        for (Endpoint endpoint : endpoints) {
            connectionsClient.sendPayload(endpoint.getId(), payload);
        }
        Log.i(TAG,"onLocationResult: Ubicación enviada");
    }

    // Devuelve la id del dispositivo
    public String getId(){return id;}

    // Devuelve el punto de conexión con la ID dada
    public Endpoint getEndpoint(String endpointId){

        Endpoint endpoint = null;
        for (Endpoint e : endpoints){
            if (e.getId().equals(endpointId)){
                endpoint = e;
            }
        }
        return endpoint;
    }

    // Acepta la conexión con un nuevo punto de conexión que se ha encontrado
    public void acceptConnection(String endpointId) {
        connectionsClient.acceptConnection(endpointId, payloadCallback);
    }

    // Añade un nuevo punto de conexión
    public void addEndpoint(String endpointId, LinearLayout ll_endpoint) {
        endpoints.add(new Endpoint(endpointId, ll_endpoint));
    }

}
