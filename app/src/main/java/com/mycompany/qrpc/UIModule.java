package com.mycompany.qrpc;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UIModule {

    // Elimina el estado del juego y actualiza la GUI en consecuencia
    public static void resetGUI(Activity activity) {
        LinearLayout linearlayout = activity.findViewById(R.id.linear_layout);
        linearlayout.removeAllViews();
        setButtonState(activity, false);
    }

    // Muestra al usuario un mensaje de estado
    public static void setStatusText(Activity activity,String text) {
        TextView statusText = activity.findViewById(R.id.status);
        statusText.setText(text);
    }

    // Activa y desactiva los botones de la GUI en función del estado de la conexión
    public static void setButtonState(Activity activity, boolean connected) {
        Button connectButton = activity.findViewById(R.id.connect);
        connectButton.setVisibility(connected ? View.GONE : View.VISIBLE);
        Button disconnectButton = activity.findViewById(R.id.disconnect);
        disconnectButton.setVisibility(connected ? View.VISIBLE : View.GONE);
    }

    // Establece el nombre del oponente
    public static void setOpponentName(Activity activity, String opponentName) {
        TextView opponentText = activity.findViewById(R.id.opponent_name);
        opponentText.setText(activity.getString(R.string.opponent_name, opponentName));
    }



    public static void setOpponentText(Activity activity, String text){
        TextView opponentText = activity.findViewById(R.id.opponent_name);
        opponentText.setText(text);
    }

    public static void addEndpointLayout(Activity activity, LinearLayout endpoint){
        LinearLayout ll = activity.findViewById(R.id.linear_layout);
        ll.addView(endpoint);
    }

    // Actualiza los valores de la UI
    public static void updateEndpointLayout(Activity activity, LinearLayout endpoint, Location location) {
        TextView tv_lat = (TextView) endpoint.getChildAt(0);
        tv_lat.setText("Lon: " + String.valueOf(location.getLatitude()));
        TextView tv_lon = (TextView) endpoint.getChildAt(1);
        tv_lon.setText("Lat: " + String.valueOf(location.getLongitude()));


        TextView tv_bearing = (TextView) endpoint.getChildAt(2);
        if (location.hasBearing()) {
            tv_bearing.setText(String.valueOf("Bear: " + location.getBearing()));
        } else {
            tv_bearing.setText("Bear: Null");
        }


        TextView tv_speed = (TextView) endpoint.getChildAt(3);
        if (location.hasSpeed()) {
            tv_speed.setText("Sp: " + String.valueOf(location.getSpeed()));
        } else {
            tv_speed.setText("Sp: Null");
        }
    }

    public static void removeEndpointLayout(Activity activity, LinearLayout endpoint){
        LinearLayout ll = activity.findViewById(R.id.linear_layout);
        ll.removeView(endpoint);
    }
}
