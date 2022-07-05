package com.mycompany.qrpc;

import android.widget.LinearLayout;

public class Endpoint {

    private String id;
    private LinearLayout linearlayout;

    public Endpoint(String id, LinearLayout linearlayout){
        this.id = id;
        this.linearlayout = linearlayout;
    }

    public String getId(){
        return id;
    }

    public LinearLayout getLinearlayout() { return linearlayout; }
}
