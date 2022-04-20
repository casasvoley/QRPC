package com.mycompany.qrpc;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Strategy;

public class MainActivity extends AppCompatActivity {

    // Permisos requeridos en función de la versión de Android del dispositivo
    private static final String[] REQUIRED_PERMISSIONS;
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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

    // Estartegia de conexión
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;

    // Elección en el juego
    private enum GameChoice {
        ROCK,
        PAPER,
        SCISSORS;

        boolean beats(GameChoice other) {
            return (this == GameChoice.ROCK && other == GameChoice.SCISSORS)
                    || (this == GameChoice.SCISSORS && other == GameChoice.PAPER)
                    || (this == GameChoice.PAPER && other == GameChoice.ROCK);
        }
    }

    // Cliente de conexión
    private ConnectionsClient connectionsClient;

    // Nombre código del jugador
    private final String codeName = CodenameGenerator.generate();

    // Datos del oponente
    private String opponentEndpointId;
    private String opponentName;
    private int opponentScore;
    private GameChoice opponentChoice;

    // Datos del jugador
    private int myScore;
    private GameChoice myChoice;

    // Botones
    private Button findOpponentButton;
    private Button disconnectButton;
    private Button rockButton;
    private Button paperButton;
    private Button scissorsButton;

    // Textviews
    private TextView opponentText;
    private TextView statusText;
    private TextView scoreText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}