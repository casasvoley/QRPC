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

    public static void addEndpointLayout(Activity activity, LinearLayout endpointLayout){
        LinearLayout ll = activity.findViewById(R.id.linear_layout);
        ll.addView(endpointLayout);
    }

    // Actualiza los valores de la UI
    /*public static void updateEndpointLayout(Activity activity, LinearLayout endpoint,
                                            double longitude, double latitude,
                                            double longitude_speed, double latitude_speed) {
        TextView tv_lat = (TextView) endpoint.getChildAt(0);
        tv_lat.setText("Lon: " + String.valueOf(longitude));
        TextView tv_lon = (TextView) endpoint.getChildAt(1);
        tv_lon.setText("Lat: " + String.valueOf(latitude));


        TextView tv_x_velocity = (TextView) endpoint.getChildAt(2);
        tv_x_velocity.setText("X: " + String.valueOf(longitude_speed));

        TextView tv_y_velocity = (TextView) endpoint.getChildAt(3);
        tv_y_velocity.setText("Y: " + String.valueOf(latitude_speed));
    }*/

    public static void updateEndpointLayout(Activity activity, LinearLayout endpointLayout, String pattern){
        TextView tv = (TextView) endpointLayout.getChildAt(0);
        tv.setText("Pattern: " + pattern);
    }

    public static void removeEndpointLayout(Activity activity, LinearLayout endpointLayout){
        LinearLayout ll = activity.findViewById(R.id.linear_layout);
        ll.removeView(endpointLayout);
    }
}
