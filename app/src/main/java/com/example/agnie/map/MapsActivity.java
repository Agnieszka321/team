package com.example.agnie.map;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.location.LocationListener;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    LocationManager lm;
    Location location;
    double longitude;
    double latitude;
    private GoogleMap mMap;
    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();

        }


        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            LatLng moja = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.addMarker(new MarkerOptions().position(moja).title("Marker in Sydney"));

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        location = lm.getLastKnownLocation("gps");
        System.out.print("hrrhrhr");
        //  location.setLatitude(10);
        //  location.getLatitude();
      /*  location.getLongitude();
       latitude=  location.getLatitude();*/
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


    protected String getEventsURL(String id)  {
        return String.format("http://devety.com:5000/events?id=%s", id);
    }

    protected String getPlacesURL(String id)  {
        return String.format("http://devety.com:5000/places?id=%s", id);
    }


    protected String getEventsURL (float lat, float lon, float meters)  {
        return String.format("http://devety.com:5000/events?lat=%f&lon=%f&dist=%f", lat, lon, meters);
    }

    protected String getPlacesURL (float lat, float lon, float meters)  {
        return String.format("http://devety.com:5000/places?lat=%f&lon=%f&dist=%f", lat, lon, meters);
    }

    protected String getEventsURL()  {
        return "http://devety.com:5000/events";
    }

    protected String getPlacesURL()  {
        return "http://devety.com:5000/places";
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        
        FetchDataTask fdt = new FetchDataTask();
        // Example usage
        System.out.println(fdt.doInBackground(this.getEventsURL()));
        System.out.println(fdt.doInBackground(this.getEventsURL()));
        System.out.println(fdt.doInBackground(this.getEventsURL("1038711659595725")));
        System.out.println(fdt.doInBackground(this.getEventsURL("103964593108277")));
        System.out.println(fdt.doInBackground(this.getEventsURL(51.1f, 17.01f, 1000.0f)));
        System.out.println(fdt.doInBackground(this.getEventsURL(51.1f, 17.01f, 1000.0f)));

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(51.108, 17.0405);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

    }
}
