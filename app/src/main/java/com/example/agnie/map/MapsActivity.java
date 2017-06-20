package com.example.agnie.map;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MapsActivity extends FragmentActivity implements GoogleMap.InfoWindowAdapter, LocationListener, OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    // -------------------------- SOME WEIRD ANDROID THINGS ----------------------------------------
    //                "keeping everything in one file is a great idea"
    //                                              - self-proclaimed BEST PROGRAMMER EVER

    // CONSTRUCTOR

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        this.myContentsView = getLayoutInflater().inflate(R.layout.map_info_window, null);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        this.apiClient = new GoogleApiClient.Builder(this).addApi(LocationServices.API).addApi(AppIndex.API).addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();
        // Initing the places/events storages
        this.places = new HashMap<>();
        this.events = new HashMap<>();
        this.markerEvents = new HashMap<>();
        this.eventMarkers = new HashMap<>();

    }

    // MAP CONSTRUCTOR
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Making a map
        this.mMap = googleMap;
        this.mMap.setOnInfoWindowClickListener(this);
        // Checking location permissions
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_ACCESS_FINE_LOCATION);
            return;
        }
        // Custom info windows
        this.mMap.setInfoWindowAdapter(this);
        // Blue location dot
        googleMap.setMyLocationEnabled(true);

        Event e;
        Marker tempMarker;
        for(Map.Entry<String, Event> entry : this.events.entrySet()) {
            e = entry.getValue();
            if (this.eventMarkers.get(e) == null) {
                BitmapDescriptor bit = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                tempMarker = this.mMap.addMarker(new MarkerOptions().icon(bit).position(e.place.getLatLng()).title(e.getName()).snippet(e.getDescription()));
                this.markerEvents.put(tempMarker, e);
                this.eventMarkers.put(e, tempMarker);
            }
        }

    }

    // INDEX API, WHATEVER THAT IS
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Maps Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        apiClient.connect();
        AppIndex.AppIndexApi.start(apiClient, getIndexApiAction());
        // ???
        LocationManager locationmanager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria cr = new Criteria();
        String provider = locationmanager.getBestProvider(cr, true);
        this.currentLocation = locationmanager.getLastKnownLocation(provider);
        // <Initial> Update events and display them
        try {
            JSONArray loc_events = this.getEventsByLocation(this.currentLocation.getLatitude(), this.currentLocation.getLongitude(), this.searchRange);
            JSONObject eventJSON, placeJSON;
            Place tempPlace;
            Event tempEvent;

            for (int i = 0; i < loc_events.length(); i++) {
                eventJSON = loc_events.getJSONObject(i);
                tempPlace = this.places.get(eventJSON.getJSONObject("place").getString("id"));
                if (tempPlace == null) {
                    placeJSON = eventJSON.getJSONObject("place");
                    tempPlace = new Place(new LatLng(placeJSON.getDouble("lat"), placeJSON.getDouble("lon")), placeJSON.getString("id"));
                    this.places.put(tempPlace.getId(), tempPlace);
                }
                tempEvent = this.events.get(eventJSON.getString("id"));
                if (tempEvent == null) {
                    tempEvent = new Event(tempPlace,
                            eventJSON.getString("id"), eventJSON.getString("name"), eventJSON.getString("description"), new Date((long) eventJSON.getInt("start_time") * 1000));
                    events.put(tempEvent.getId(), tempEvent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Number of events: " + events.size());

    }

    @Override
    public void onStop() {
        super.onStop();
        AppIndex.AppIndexApi.end(apiClient, getIndexApiAction());
        apiClient.disconnect();
    }


    // LOCATION SERVICES
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(1000); // Update location every second
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(apiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {
        System.out.println("GoogleApiClient connection has been suspend");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        System.out.println("GoogleApiClient connection has failed");
    }

    @Override
    public void onLocationChanged(Location location) {
        System.out.println("New location : " + location.toString());
        this.currentLocation = location;
        try {
            JSONArray loc_events = this.getEventsByLocation(this.currentLocation.getLatitude(), this.currentLocation.getLongitude(), this.searchRange);
            JSONObject eventJSON, placeJSON;
            Place tempPlace;
            Event tempEvent;

            for (int i = 0; i < loc_events.length(); i++) {
                eventJSON = loc_events.getJSONObject(i);
                tempPlace = this.places.get(eventJSON.getJSONObject("place").getString("id"));
                if (tempPlace == null) {
                    placeJSON = eventJSON.getJSONObject("place");
                    tempPlace = new Place(new LatLng(placeJSON.getDouble("lat"), placeJSON.getDouble("lon")), placeJSON.getString("id"));
                    this.places.put(tempPlace.getId(), tempPlace);
                }
                tempEvent = this.events.get(eventJSON.getString("id"));
                if (tempEvent == null) {
                    tempEvent = new Event(tempPlace,
                            eventJSON.getString("id"), eventJSON.getString("name"), eventJSON.getString("description"), new Date((long) eventJSON.getInt("start_time") * 1000));
                    events.put(tempEvent.getId(), tempEvent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Number of events: " + events.size());
        Event e;
        Marker tempMarker;
        for(Map.Entry<String, Event> entry : this.events.entrySet()) {
            e = entry.getValue();
            if (this.eventMarkers.get(e) == null) {
                BitmapDescriptor bit = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                tempMarker = this.mMap.addMarker(new MarkerOptions().icon(bit).position(e.place.getLatLng()).title(e.getName()).snippet(e.getDescription()));
                this.markerEvents.put(tempMarker, e);
                this.eventMarkers.put(e, tempMarker);
            }
        }
        // this is straight out heresy btw
    }

    // INFO WINDOW
    @Override
    public void onInfoWindowClick(Marker marker) {
        String url = "https://www.facebook.com/" + this.markerEvents.get(marker).getId();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    @Override
    public View getInfoWindow(Marker marker) {
        // if we return null, the default frame will be used
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        // This fills out the text views from the map info window xml
        TextView tvTime = ((TextView) myContentsView.findViewById(R.id.time));
        tvTime.setText(this.displayDateFormat.format(this.markerEvents.get(marker).getStartTime()));

        TextView tvTitle = ((TextView) myContentsView.findViewById(R.id.title));
        tvTitle.setText(marker.getTitle());

        TextView tvSnippet = ((TextView) myContentsView.findViewById(R.id.snippet));
        tvSnippet.setText(marker.getSnippet().split("(?<=[a-z])[\\.!?]\\s+")[0] + "\nClick to learn more...");

        return myContentsView;
    }

    // PERMISSIONS
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MapsActivity.PERMISSION_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                } else {
                    System.exit(1);
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------------------

    // -------------------------------- CUSTOM SUBCLASSES ------------------------------------------
    class Event {
        protected Place place;
        protected String name;
        protected String id;
        protected String description;
        protected Date startTime;

        Event(Place place, String id, String name, String description, Date startTime) {
            this.id = id;
            this.place = place;
            this.name = name;
            this.description = description;
            this.startTime = startTime;
        }

        public String getName() {
            return this.name;
        }

        public String getId() {
            return this.id;
        }

        public Date getStartTime() {
            return this.startTime;
        }

        public Place getPlace() {
            return this.place;
        }

        public String getDescription() {
            return this.description;
        }
    }

    class Place {
        protected LatLng lat_lng;
        protected String id;

        Place(LatLng lat_lng, String id) {
            this.id = id;
            this.lat_lng = lat_lng;
        }

        public String getId() {
            return this.id;
        }

        public LatLng getLatLng() {
            return this.lat_lng;
        }

    }
    // ---------------------------------------------------------------------------------------------

    // --------------------------------------- CUSTOM METHODS --------------------------------------
    protected JSONArray getEventById(String id) throws IOException, ExecutionException, InterruptedException {
        String url = String.format("http://devety.com:5000/events?id=%s", id);
        return new FetchDataTask().execute(url).get();
    }

    protected JSONArray getPlaceById(String id) throws IOException, ExecutionException, InterruptedException {
        String url = String.format("http://devety.com/places?id=%s", id);
        return new FetchDataTask().execute(url).get();
    }


    protected JSONArray getEventsByLocation(double lat, double lon, float meters) throws IOException, ExecutionException, InterruptedException {
        String url = String.format("http://devety.com/events?lat=%f&lon=%f&dist=%f&new_method=True", lat, lon, meters).replaceAll(",", ".");
        return new FetchDataTask().execute(url).get();
    }

    protected JSONArray getPlacesByLocation(double lat, double lon, float meters) throws IOException, ExecutionException, InterruptedException {
        // wow that's a hacky solution for a 123.123 formatting if I ever saw one :D
        String url = String.format("http://devety.com/places?lat=%f&lon=%f&dist=%f&new_method=True", lat, lon, meters).replaceAll(",", ".");
        return new FetchDataTask().execute(url).get();
    }

    protected JSONArray getAllEventIds() throws IOException, ExecutionException, InterruptedException {
        String url = "http://devety.com/events";
        return new FetchDataTask().execute(url).get();
    }

    protected JSONArray getAllPlaceIds() throws IOException, ExecutionException, InterruptedException {
        String url = "http://devety.com/places";
        return new FetchDataTask().execute(url).get();
    }

    protected void updateEvents() {
        // This uses quite a lot of processing if we have a lot of events. Maybe lookup the values
        // before overriding everything?
        try {
            JSONArray loc_events = this.getEventsByLocation(this.currentLocation.getLatitude(), this.currentLocation.getLongitude(), this.searchRange);
            JSONObject eventJSON, placeJSON;
            Place tempPlace;
            Event tempEvent;

            for (int i = 0; i < loc_events.length(); i++) {
                eventJSON = loc_events.getJSONObject(i);
                tempPlace = this.places.get(eventJSON.getJSONObject("place").getString("id"));
                if (tempPlace == null) {
                    placeJSON = eventJSON.getJSONObject("place");
                    tempPlace = new Place(new LatLng(placeJSON.getDouble("lat"), placeJSON.getDouble("lon")), placeJSON.getString("id"));
                    this.places.put(tempPlace.getId(), tempPlace);
                }
                tempEvent = this.events.get(eventJSON.getString("id"));
                if (tempEvent == null) {
                    tempEvent = new Event(tempPlace,
                            eventJSON.getString("id"), eventJSON.getString("name"), eventJSON.getString("description"), new Date((long) eventJSON.getInt("start_time") * 1000));
                    events.put(tempEvent.getId(), tempEvent);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void updateMap() {
        Event e;
        Marker tempMarker;
        for(Map.Entry<String, Event> entry : this.events.entrySet()) {
            e = entry.getValue();
            if (this.eventMarkers.get(e) == null) {
                BitmapDescriptor bit = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
                tempMarker = this.mMap.addMarker(new MarkerOptions().icon(bit).position(e.place.getLatLng()).title(e.getName()).snippet(e.getDescription()));
                this.markerEvents.put(tempMarker, e);
                this.eventMarkers.put(e, tempMarker);
            }
        }
    }

    // -------------------------------------- CUSTOM PROPERTIES ------------------------------------
    private GoogleMap mMap;
    private GoogleApiClient apiClient;
    private LocationRequest mLocationRequest;
    protected View myContentsView;
    protected HashMap<String, Place> places;
    protected HashMap<String, Event> events;
    // Redundancy much. (1-1 relationship)
    protected HashMap<Marker, Event> markerEvents;
    protected HashMap<Event, Marker> eventMarkers;
    protected Location currentLocation;
    protected SimpleDateFormat displayDateFormat = new SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm");
    protected float searchRange = 2000.0f;
    public static final int PERMISSION_ACCESS_FINE_LOCATION = 0;



}
