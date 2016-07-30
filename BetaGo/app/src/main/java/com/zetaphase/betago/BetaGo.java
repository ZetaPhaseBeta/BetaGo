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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

class MarkerThread extends Thread {
    private BetaGo activity;

    MarkerThread(BetaGo activity){
        this.activity = activity;
    }

    private StringBuffer request(String urlString, JSONObject jsonObj) {
        // TODO Auto-generated method stub

        StringBuffer chaine = new StringBuffer("");
        try{
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("User-Agent", "");
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.connect();

            Log.d("REQUESTOUTPUT", "requesting");
            byte[] b = jsonObj.toString().getBytes();
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(b);


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

    private static HashMap<String, String> helperMap(String t) throws JSONException {
        HashMap<String, String> map = new HashMap<String, String>();
        JSONObject jObject = new JSONObject(t);
        Iterator<?> keys = jObject.keys();

        while( keys.hasNext() ){
            String key = (String)keys.next();
           String value = jObject.getString(key);
            map.put(key, value);

        }

        return map;
    }

    public static HashMap<String, HashMap<String, String>> jsonToMap(String t) throws JSONException {

        HashMap<String, HashMap<String, String>> map = new HashMap<String, HashMap<String, String>>();
        JSONObject jObject = new JSONObject(t);
        Iterator<?> keys = jObject.keys();

        while( keys.hasNext() ){
            String key = (String)keys.next();
            HashMap<String, String> value = helperMap(jObject.getString(key));
            map.put(key, value);

        }

        return map;
    }

    private void post() throws IOException, JSONException {
        Log.d("POST", "posting");
        Log.d("POSTLM", String.valueOf(activity.lm));
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
        Log.d("RICKY", jsonObj.toString());
        StringBuffer a = request("http://192.168.1.68", jsonObj);
        Log.d("DAVEY", String.valueOf(a));
        Log.d("POST", "Attempting to run on ui thread");
        final HashMap<String, HashMap<String, String>> response = jsonToMap(String.valueOf(a));
        Log.d("RESPONSE", String.valueOf(response));

        activity.runOnUiThread(new Runnable() {
            public void run(){
                //Toast.makeText(activity.getBaseContext(), (CharSequence) httpresponse, Toast.LENGTH_LONG).show();

                for(String key : response.keySet()){
                    double lat = Double.parseDouble(response.get(key).get("lat"));
                    double lng = Double.parseDouble(response.get(key).get("lng"));
                    if (!activity.markerMap.containsKey(key)){
                        Log.d("NOTCONTAINS", "Marker map does not contain key");
                        activity.markerMap.put(key, activity.mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)).title(key)));
                    } else {
                        Log.d("CONTAINS", "Marker map does contains key");
                        activity.markerMap.get(key).setPosition(new LatLng(lat, lng));
                    }
                }

            }
        });

    }

    @Override
    public void run(){
        try {
            post();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e){
            e.printStackTrace();
        }
    }
}

class RecordTask extends TimerTask {
    private BetaGo activity;

    RecordTask(BetaGo activity){this.activity = activity;}

    @Override
    public void run(){
        Location location = activity.lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        activity.myRecord.add(new LatLng(latitude, longitude));
        Log.d("RECORDING", String.valueOf(activity.myRecord));
    }
}

class ReplayTask extends TimerTask {
    private BetaGo activity;

    ReplayTask(BetaGo activity){this.activity = activity;}

    @Override
    public void run(){
        activity.replayMarker.setVisible(true);
        activity.replayMarker.setPosition(activity.myRecord.get(activity.replayPos));
    }
}

class FirstTask extends TimerTask {

    private BetaGo activity;

    FirstTask(BetaGo activity){
        this.activity = activity;
    }

    @Override
    public void run() {
        MarkerThread thread = new MarkerThread(this.activity);
        thread.start();
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
    public HashMap<String, Marker> markerMap = new HashMap<String, Marker>();
    public List<LatLng> myRecord = new ArrayList<LatLng>();
    public int replayPos = 0;
    public Marker replayMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Replay Marker").visible(false));

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
        Log.d("LM", String.valueOf(location));
        if (location!=null){
            final double longitude = location.getLongitude();
            final double latitude = location.getLatitude();
            Button record = (Button) findViewById(R.id.record);
            final BetaGo finalActivity = this;
            record.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v){
                    Toast.makeText(getBaseContext(), "Record", Toast.LENGTH_LONG).show();
                    final Timer timer = new Timer();
                    timer.schedule(new RecordTask(finalActivity), 0, 5000);
                    Button stop = (Button) findViewById(R.id.stop);
                    stop.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v){
                            timer.cancel();
                            timer.purge();
                        }
                    });
                }
            });
            Button replay = (Button) findViewById(R.id.replay);
            replay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View V)
            });
            //this.marker = mMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("New Marker"));
            Timer timer = new Timer();
            timer.schedule(new FirstTask(this), 0, 5000);
            // Add a marker in Sydney and move the camera
            LatLng loc = new LatLng(latitude, longitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
        }
        }

}
