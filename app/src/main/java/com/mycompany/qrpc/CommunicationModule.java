package com.mycompany.qrpc;

import static com.mycompany.qrpc.SerializationHelper.deserialize;
import static com.mycompany.qrpc.SerializationHelper.serialize;

import android.app.Activity;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import java.util.HashMap;
import java.util.Map;

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

    // Datos del oponente
    private String opponentEndpointId;
    private String opponentName;

    // Actividad principal
    private Activity activity;


    // Callbacks cuando nuestro dispositivo se conecta a otros dispositivos
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
                        UIModule.setOpponentText(activity, activity.getString(R.string.opponent_name, opponentName));
                        UIModule.setStatusText(activity, activity.getString(R.string.status_connected));
                        UIModule.setButtonState(activity, true);

                        // Si la conexión falla
                    } else {
                        Log.i(TAG, "onConnectionResult: Conexión fallida");
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    Log.i(TAG, "onDisconnected: Ha habido una desconexión del punto de conexión");
                    opponentEndpointId = null;
                    opponentName = null;
                    UIModule.resetGUI(activity);
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
                        UIModule.updateUIValues(activity,targetLocation);
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
    }

    // Iniciar anuncios de nuestra presencia
    private void startAdvertising() {
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startAdvertising(
                id, packageName, connectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }

    // Encontrar objetivo
    public void findEndpoint() {
        startAdvertising();
        startDiscovery();
        UIModule.setStatusText(activity, activity.getString(R.string.status_searching));
        UIModule.enableFindOpponentButton(activity, false);
    }

    // Desconectarse del objetivo y reiniciar la UI
    public void disconnect() {
        connectionsClient.disconnectFromEndpoint(opponentEndpointId);
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
        connectionsClient.sendPayload(
                opponentEndpointId, payload);
        Log.i(TAG,"onLocationResult: Location package sent");
    }

    // Devuelve la id del dispositivo
    public String getId(){return id;}
}
