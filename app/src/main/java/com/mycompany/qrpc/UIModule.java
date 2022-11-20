package com.mycompany.qrpc;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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

    // Activa y desactiva los botones de la GUI en función de si la conexión está activada
    public static void setButtonState(Activity activity, boolean connected) {
        Button connectButton = activity.findViewById(R.id.connect);
        connectButton.setVisibility(connected ? View.GONE : View.VISIBLE);
        Button disconnectButton = activity.findViewById(R.id.disconnect);
        disconnectButton.setVisibility(connected ? View.VISIBLE : View.GONE);
    }

    // Añade un LinearLayout para un nuevo punto de conexión
    public static void addEndpointLayout(Activity activity, LinearLayout endpointLayout){
        LinearLayout ll = activity.findViewById(R.id.linear_layout);
        ll.addView(endpointLayout);
    }

    // Actualiza los valores de el LinearLayout de un punto de conexión
    public static void updateEndpointLayout(Activity activity, LinearLayout endpointLayout, int patternId){
        ImageView pattern_view = (ImageView) endpointLayout.getChildAt(1);
        pattern_view.setImageResource(patternId);
    }
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

    // Elimina el LinearLayout de un punto de conexión que se ha desconectado
    public static void removeEndpointLayout(Activity activity, LinearLayout endpointLayout){
        LinearLayout ll = activity.findViewById(R.id.linear_layout);
        ll.removeView(endpointLayout);
    }

    // Actualiza el orden en el que aparecen los puntos de conexión en la UI
    public static void updateEndpointOrder(Activity activity, ArrayList<Endpoint> endpoints){

        if (endpoints.size() > 1) {
            LinearLayout mainLayout = activity.findViewById(R.id.linear_layout);
            int numEndpoints = endpoints.size();

            Collections.sort(endpoints, new Comparator<Endpoint>() {
                @Override
                public int compare(Endpoint o1, Endpoint o2) {
                    return Float.compare(o1.getDistance(), o2.getDistance());
                }
            });

            LinearLayout linearLayout = new LinearLayout(activity);
            for (int i = 0; i < numEndpoints; i++) {
                linearLayout.addView(endpoints.get(i).getEndpointlayout());
            }
            mainLayout = linearLayout;
        }
    }
}
