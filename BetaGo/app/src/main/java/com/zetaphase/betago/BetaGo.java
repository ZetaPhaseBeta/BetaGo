package com.zetaphase.betago;

import android.annotation.TargetApi;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.TimerTask;

class MarkerThread extends Thread {
    private BetaGo activity;

    MarkerThread(BetaGo activity){
        this.activity = activity;
    }

    private StringBuffer request(String urlString) {
        // TODO Auto-generated method stub

        StringBuffer chaine = new StringBuffer("");
        try{
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("User-Agent", "");
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.connect();

            InputStream inputStream = connection.getInputStream();

            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String line = "";
            while ((line = rd.readLine()) != null) {
                chaine.append(line);
            }

        } catch (IOException e) {
            // writing exception to log
            e.printStackTrace();
        }

        return chaine;
    }

    private void post() throws IOException {
        Log.d("POST", "posting");
        Location location = activity.lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        final double longitude = location.getLongitude();
        final double latitude = location.getLatitude();
        TelephonyManager tMgr = (TelephonyManager)activity.getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = tMgr.getLine1Number();
        HashMap<String, String> hmap = new HashMap<String, String>();
        hmap.put("phone", String.valueOf(mPhoneNumber));
        hmap.put("lat", String.valueOf(latitude));
        hmap.put("lng", String.valueOf(longitude));
        JSONObject jsonObj = new JSONObject(hmap);
        Log.d("POST", jsonObj.toString());
        StringBuffer a = request("http://192.168.1.68");
        Log.d("POST", String.valueOf(a));
        Log.d("POST", "Attempting to run on ui thread");

        activity.runOnUiThread(new Runnable() {
            public void run(){
                //Toast.makeText(activity.getBaseContext(), (CharSequence) httpresponse, Toast.LENGTH_LONG).show();
                activity.marker = activity.mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("New Marker"));
                activity.marker.setVisible(true);
            }
        });

    }

    @Override
    public void run(){
        try {
            post();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

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

    public GoogleMap mMap;
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
        final double longitude = location.getLongitude();
        final double latitude = location.getLatitude();
        MarkerThread thread = new MarkerThread(this);
        thread.start();
        //this.marker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("New Marker"));
        //Timer timer = new Timer();
        //timer.schedule(new FirstTask(this), 0,5000);
        // Add a marker in Sydney and move the camera
        LatLng loc = new LatLng(latitude, longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
    }

}
