package com.zetaphase.betago;

import android.annotation.TargetApi;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

class FirstTask extends TimerTask {

    private BetaGo activity;

    FirstTask(BetaGo activity){
        this.activity = activity;
    }

    @Override
    public void run() {
        activity.runOnUiThread(new Runnable() {
            public void run() {
                activity.marker.setPosition(new LatLng(activity.latlist[activity.pointer], activity.longlist[activity.pointer]));
                Location location = activity.lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                activity.latlist[activity.pointer] = latitude;
                activity.longlist[activity.pointer] = longitude;
                activity.pointer = (activity.pointer + 1) % activity.ARRAY_SIZE;
                Toast.makeText(activity.getBaseContext(), "current time: " + new Date(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
};

public class BetaGo extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    public static final int ARRAY_SIZE = 10;
    public double[] latlist = new double[ARRAY_SIZE];
    public double[] longlist = new double[ARRAY_SIZE];
    public int pointer = 0;
    public LocationManager lm;
    public Marker marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beta_go);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        this.lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Location location = this.lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double longitude = location.getLongitude();
        double latitude = location.getLatitude();
        this.marker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("New Marker"));
        Timer timer = new Timer();
        timer.schedule(new FirstTask(this), 0,5000);
        // Add a marker in Sydney and move the camera
        LatLng loc = new LatLng(latitude, longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
    }

}
