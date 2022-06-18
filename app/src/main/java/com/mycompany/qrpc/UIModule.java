package com.mycompany.qrpc;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class UIModule {

    // Elimina el estado del juego y actualiza la GUI en consecuencia
    public static void resetGUI(Activity activity) {
        TextView tv_lon = activity.findViewById(R.id.tv_lon);
        tv_lon.setText("0.0");
        TextView tv_lat = activity.findViewById(R.id.tv_lat);
        tv_lat.setText("0.0");
        TextView tv_altitude = activity.findViewById(R.id.tv_altitude);
        tv_altitude.setText("0.0");
        TextView tv_speed = activity.findViewById(R.id.tv_speed);
        tv_speed.setText("0.0");


        setOpponentName(activity, activity.getString(R.string.no_opponent));
        setStatusText(activity, activity.getString(R.string.status_disconnected));
        setButtonState(activity, false);
    }

    // Muestra al usuario un mensaje de estado
    public static void setStatusText(Activity activity,String text) {
        TextView statusText = activity.findViewById(R.id.status);
        statusText.setText(text);
    }

    // Activa y desactiva los botones de la GUI en función del estado de la conexión
    public static void setButtonState(Activity activity, boolean connected) {
        Button findOpponentButton = activity.findViewById(R.id.find_opponent);
        findOpponentButton.setEnabled(true);
        findOpponentButton.setVisibility(connected ? View.GONE : View.VISIBLE);
        Button disconnectButton = activity.findViewById(R.id.disconnect);
        disconnectButton.setVisibility(connected ? View.VISIBLE : View.GONE);
    }

    // Establece el nombre del oponente
    public static void setOpponentName(Activity activity, String opponentName) {
        TextView opponentText = activity.findViewById(R.id.opponent_name);
        opponentText.setText(activity.getString(R.string.opponent_name, opponentName));
    }

    // Actualiza los valores de la UI
    public static void updateUIValues(Activity activity,Location location) {
        TextView tv_lat = activity.findViewById(R.id.tv_lat);
        tv_lat.setText(String.valueOf(location.getLatitude()));
        TextView tv_lon = activity.findViewById(R.id.tv_lon);
        tv_lon.setText(String.valueOf(location.getLongitude()));


        TextView tv_altitude = activity.findViewById(R.id.tv_altitude);
        if (location.hasBearing()) {
            tv_altitude.setText(String.valueOf(location.getBearing()));
        } else {
            tv_altitude.setText("Not available");
        }


        TextView tv_speed = activity.findViewById(R.id.tv_speed);
        if (location.hasSpeed()) {
            tv_speed.setText(String.valueOf(location.getSpeed()));
        } else {
            tv_speed.setText("Not available");
        }
    }

    public static void setOpponentText(Activity activity, String text){
        TextView opponentText = activity.findViewById(R.id.opponent_name);
        opponentText.setText(text);
    }

    public static void enableFindOpponentButton(Activity activity, boolean enabled){
        Button findOpponentButton = activity.findViewById(R.id.find_opponent);
        findOpponentButton.setEnabled(enabled);
    }
}
