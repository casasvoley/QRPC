package com.mycompany.qrpc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

public class UIModule {

    private static final String TAG = "QRPC";

    // HashMap con los iconos de los patrones
    private static final Map<String,Integer> patterns = Map.ofEntries(
            new AbstractMap.SimpleEntry<String,Integer>("↑_+", R.drawable.pattern_1),
            new AbstractMap.SimpleEntry<String,Integer>("↑_-", R.drawable.pattern_2),
            new AbstractMap.SimpleEntry<String,Integer>("↑↑_{+0}", R.drawable.pattern_3),
            new AbstractMap.SimpleEntry<String,Integer>("↑↑_{-0}", R.drawable.pattern_4),
            new AbstractMap.SimpleEntry<String,Integer>("↑↑_{+-}", R.drawable.pattern_5),
            new AbstractMap.SimpleEntry<String,Integer>("↑↑_{--}", R.drawable.pattern_6),
            new AbstractMap.SimpleEntry<String,Integer>("↑↑_{++}", R.drawable.pattern_7),
            new AbstractMap.SimpleEntry<String,Integer>("↑↑_{-+}", R.drawable.pattern_8),
            new AbstractMap.SimpleEntry<String,Integer>("X_+^{+0}", R.drawable.pattern_9),
            new AbstractMap.SimpleEntry<String,Integer>("X_-^{+0}", R.drawable.pattern_10),
            new AbstractMap.SimpleEntry<String,Integer>("X_{+0}^{++}", R.drawable.pattern_11),
            new AbstractMap.SimpleEntry<String,Integer>("X_{-0}^{++}", R.drawable.pattern_12),
            new AbstractMap.SimpleEntry<String,Integer>("X_{+-}^{++}", R.drawable.pattern_13),
            new AbstractMap.SimpleEntry<String,Integer>("X_{--}^{++}", R.drawable.pattern_14),
            new AbstractMap.SimpleEntry<String,Integer>("X_{++}^{++}", R.drawable.pattern_15),
            new AbstractMap.SimpleEntry<String,Integer>("X_{-+}^{++}", R.drawable.pattern_16),
            new AbstractMap.SimpleEntry<String,Integer>("X_{-|+-}^{0-}", R.drawable.pattern_17),
            new AbstractMap.SimpleEntry<String,Integer>("X_{+|--}^{0-}", R.drawable.pattern_18),
            new AbstractMap.SimpleEntry<String,Integer>("X_{+0}^{--}", R.drawable.pattern_19),
            new AbstractMap.SimpleEntry<String,Integer>("X_{-0}^{--}", R.drawable.pattern_20),
            new AbstractMap.SimpleEntry<String,Integer>("X_{+-}^{--}", R.drawable.pattern_21),
            new AbstractMap.SimpleEntry<String,Integer>("X_{--}^{--}", R.drawable.pattern_22),
            new AbstractMap.SimpleEntry<String,Integer>("X_{++}^{--}", R.drawable.pattern_23),
            new AbstractMap.SimpleEntry<String,Integer>("X_{-+}^{--}", R.drawable.pattern_24),
            new AbstractMap.SimpleEntry<String,Integer>("↑↓_{+0}", R.drawable.pattern_25),
            new AbstractMap.SimpleEntry<String,Integer>("↑↓_{-0}", R.drawable.pattern_26),
            new AbstractMap.SimpleEntry<String,Integer>("↕_+", R.drawable.pattern_27),
            new AbstractMap.SimpleEntry<String,Integer>("↑↓_{++}", R.drawable.pattern_28),
            new AbstractMap.SimpleEntry<String,Integer>("↑↓_{-+}", R.drawable.pattern_29),
            new AbstractMap.SimpleEntry<String,Integer>("↕_-", R.drawable.pattern_30),
            new AbstractMap.SimpleEntry<String,Integer>("↑↓_{+-}", R.drawable.pattern_31),
            new AbstractMap.SimpleEntry<String,Integer>("↑↓_{--}", R.drawable.pattern_32),
            new AbstractMap.SimpleEntry<String,Integer>("X_{+0}^{+-}", R.drawable.pattern_33),
            new AbstractMap.SimpleEntry<String,Integer>("X_{-0}^{+-}", R.drawable.pattern_34),
            new AbstractMap.SimpleEntry<String,Integer>("X_{++}^{+-}", R.drawable.pattern_35),
            new AbstractMap.SimpleEntry<String,Integer>("X_{-+}^{+-}", R.drawable.pattern_36),
            new AbstractMap.SimpleEntry<String,Integer>("X_{+-}^{+-}", R.drawable.pattern_37),
            new AbstractMap.SimpleEntry<String,Integer>("X_{--}^{+-}", R.drawable.pattern_38),
            new AbstractMap.SimpleEntry<String,Integer>("X_{+0}^{-+}", R.drawable.pattern_39),
            new AbstractMap.SimpleEntry<String,Integer>("X_{-0}^{-+}", R.drawable.pattern_40),
            new AbstractMap.SimpleEntry<String,Integer>("X_{+|-+}^{0+}", R.drawable.pattern_41),
            new AbstractMap.SimpleEntry<String,Integer>("X_{-|++}^{0+}", R.drawable.pattern_42),
            new AbstractMap.SimpleEntry<String,Integer>("X_{++}^{-+}", R.drawable.pattern_43),
            new AbstractMap.SimpleEntry<String,Integer>("X_{-+}^{-+}", R.drawable.pattern_44),
            new AbstractMap.SimpleEntry<String,Integer>("X_+^{-0}", R.drawable.pattern_45),
            new AbstractMap.SimpleEntry<String,Integer>("X_-^{-0}", R.drawable.pattern_46),
            new AbstractMap.SimpleEntry<String,Integer>("X_{+-}^{-+}", R.drawable.pattern_47),
            new AbstractMap.SimpleEntry<String,Integer>("X_{--}^{-+}", R.drawable.pattern_48)
    );

    // Elimina el estado del juego y actualiza la GUI en consecuencia
    public static void resetGUI(Activity activity) {
        LinearLayout linearlayout = activity.findViewById(R.id.linear_layout);
        linearlayout.removeAllViews();
        setButtonState(activity, false);
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
    @SuppressLint("UseCompatLoadingForDrawables")
    public static void updateEndpointLayout(Activity activity,
                                            LinearLayout endpointLayout, String pattern){
        ImageView pattern_view = (ImageView) endpointLayout.getChildAt(1);
        pattern_view.setImageDrawable(activity.getResources().getDrawable(patterns.get(pattern)));
    }

    // Elimina el LinearLayout de un punto de conexión que se ha desconectado
    public static void removeEndpointLayout(Activity activity, LinearLayout endpointLayout){
        LinearLayout ll = activity.findViewById(R.id.linear_layout);
        ll.removeView(endpointLayout);
    }

    // Actualiza el orden en el que aparecen los puntos de conexión en la UI
    public static void updateEndpointOrder(Activity activity, ArrayList<Endpoint> endpoints){

        // Comprobamos si hay más de un dispositivo conectado
        if (endpoints.size() > 1) {
            LinearLayout mainLayout = activity.findViewById(R.id.linear_layout);
            int numEndpoints = endpoints.size();

            // Ordenamos los dispositivos de menor a mayor distancia
            Collections.sort(endpoints,
                    (o1, o2) -> Float.compare(o1.getDistance(), o2.getDistance()));

            // Los colocamos en la interfaz en ese orden
            mainLayout.removeAllViews();
            for (int i = 0; i < numEndpoints; i++) {
                LinearLayout endpointLayout = endpoints.get(i).getEndpointlayout();
                mainLayout.addView(endpointLayout);
            }
        }
    }
}
