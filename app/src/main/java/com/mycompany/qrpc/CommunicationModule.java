package com.mycompany.qrpc;

import android.app.Activity;
import android.util.Log;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.ArrayList;

public class CommunicationModule {

    private static final String TAG = "QRPC";

    // Cliente de conexiones
    private ConnectionsClient connectionsClient;

    // Id de instalación del dispositivo
    private String installationId;

    // Nombre del paquete
    private String packageName;

    // Estrategia de conexión
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    // Datos de las conexiones
    private ArrayList<Endpoint> endpoints;

    // Datos de los endpoints que aún no tienen una conexión establecida
    private ArrayList<Endpoint> tempEndpoints;

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
                    connectionsClient.requestConnection(installationId, endpointId, connectionLifecycleCallback);
                }

                @Override
                public void onEndpointLost(@NonNull String endpointId) {}
            };

    public CommunicationModule(Activity activity, String installationId, PayloadCallback payloadCallback, ConnectionLifecycleCallback connectionLifecycleCallback){
        // Creamos un cliente de conexiones
        this.connectionsClient = Nearby.getConnectionsClient(activity);
        this.installationId = installationId;
        this.packageName = activity.getPackageName();
        this.activity = activity;
        this.payloadCallback = payloadCallback;
        this.connectionLifecycleCallback = connectionLifecycleCallback;
        this.endpoints = new ArrayList<Endpoint>();
        this.tempEndpoints = new ArrayList<Endpoint>();
    }

    // Comienza a emitir anuncios de nuestra presencia y a buscar otros puntos de conexión
    public void connect() {
        connectionsClient.startAdvertising(
                installationId, packageName, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
        connectionsClient.startDiscovery(
                packageName, endpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }

    // Detiene todos los sistemas de conectividad
    public void disconnect() {
        connectionsClient.stopAllEndpoints();
        connectionsClient.stopAdvertising();
        connectionsClient.stopDiscovery();
    }

    // Envía un mensaje a todos los puntos de conexión
    public void sendPayload(Payload payload){
        for (Endpoint endpoint : endpoints) {
            connectionsClient.sendPayload(endpoint.getId(), payload);
        }
        Log.i(TAG,"onLocationResult: Ubicación enviada");
    }

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

    // Devuelve el punto de conexión a la espera de conexión con la ID dada
    public Endpoint getTempEndpoint(String endpointId){

        Endpoint endpoint = null;
        for (Endpoint e : tempEndpoints){
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

    // Añade un nuevo punto de conexión con una conexión establecida
    public void addEndpoint(String endpointId, String endpointDevId, LinearLayout ll_endpoint) {
        endpoints.add(new Endpoint(endpointId, endpointDevId, ll_endpoint));
    }

    // Convierte un punto de conexión de temporal a permanente porque ya ha se establecido una conexión
    public void saveEndpoint(String endpointId, LinearLayout ll_endpoint) {
        Endpoint endpoint = getTempEndpoint(endpointId);
        endpoint.setEndpointlayout(ll_endpoint);
        endpoints.add(endpoint);
        removeTempEndpoint(endpoint);
    }

    // Añade un nuevo punto de conexión a la espera de que se establezca una conexión
    public void addTempEndpoint(String endpointId, String endpointDevId, LinearLayout ll_endpoint) {
        tempEndpoints.add(new Endpoint(endpointId, endpointDevId, ll_endpoint));
    }

    // Devuelve la lista de puntos de conexión
    public ArrayList<Endpoint> getEndpoints() {return endpoints;}

    // Elimina un punto de conexión
    public void removeEndpoint(String endpointId){
        Endpoint endpoint = null;
        for (Endpoint e : endpoints){
            if (e.getId().equals(endpointId)){
                endpoint = e;
            }
        }
        endpoints.remove(endpoint);
    }

    // Elimina un punto de conexión temporal
    public void removeTempEndpoint(Endpoint e){
        tempEndpoints.remove(e);
    }

    // Comprueba si hay puntos de conexión con conexiones establecidas
    public boolean isThereConnections() {
        return !endpoints.isEmpty();
    }
}
