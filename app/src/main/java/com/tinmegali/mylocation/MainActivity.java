package com.tinmegali.mylocation;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.DetectedActivityResponse;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import android.widget.Toast;
import java.util.ArrayList;
import android.location.Geocoder;
import android.location.Address;
import java.util.Locale;
import java.util.List;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity
        implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            LocationListener,
            OnMapReadyCallback,
            GoogleMap.OnMapClickListener,
            GoogleMap.OnMarkerClickListener,
            ResultCallback<Status> {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static GoogleMap map;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;

    //DELETE SAIF
    BroadcastReceiver bManager;
    //END

    private TextView textLat, textLong;
    private TextView textMotionState;

    private MapFragment mapFragment;
    private ArrayList<Geofence> mGeofenceList;
    private static int geoId = 0;
    private List<LatLng> geoCenters;
    private List<String> reqIds;
    private ArrayList<Marker> gfmarkr = new ArrayList<Marker>();
    private ArrayList<Circle> gflimits = new ArrayList<>();

    public class geofenceClass {
        private Geofence geofence;
        private Circle geoLimit;
        private Marker geoMarker;
        private int maxOccupancy;
        private int currOccupancy;
        private String requestId;
        private LatLng geoCenter;
        private float radius;

        public void setGeofence(Geofence geofence) {
            this.geofence = geofence;
        }

        public void setGeoMarker(Marker geoMarker) {
            this.geoMarker = geoMarker;
        }

        public void setGeoCenter(LatLng latLng) { this.geoCenter = latLng; }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public void setGeoLimit(Circle geoLimit) {
            this.geoLimit = geoLimit;
        }

        public void setRadius(float radius) { this.radius = radius; }

        public void setMaxOccupancy(int maxOccupancy) {
            this.maxOccupancy = maxOccupancy;
        }

        public void setCurrOccupancy(int currOccupancy) {
            this.currOccupancy = currOccupancy;
        }

        public Geofence getGeofence() {
            return this.geofence;
        }

        public LatLng getGeoCenter() { return this.geoCenter; }

        public Circle getGeoLimit() {
            return this.geoLimit;
        }

        public Marker getGeoMarker() {
            return this.geoMarker;
        }

        public String getRequestId() {
            return this.requestId;
        }

        public float getRadius() { return this.radius; }

        public int getMaxOccupancy() { return  this.maxOccupancy; }

        public int getCurrOccupancy() { return  this.currOccupancy; }

    }

    HashMap<String, geofenceClass> geoHash = new HashMap<>();

    public static ArrayList<geofenceClass> geoObjects = new ArrayList<>();

    public static HashMap<LatLng, geofenceClass> geoLatHash = new HashMap<>();
    public static ArrayList<geofenceClass> pendingDrawGeofence = new ArrayList<>();
    private static final String NOTIFICATION_MSG = "NOTIFICATION MSG";
    // Create a Intent send by the notification
    public static Intent makeNotificationIntent(Context context, String msg) {
        Intent intent = new Intent( context, MainActivity.class );
        intent.putExtra( NOTIFICATION_MSG, msg );
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate called" + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textLat = (TextView) findViewById(R.id.lat);
        textLong = (TextView) findViewById(R.id.lon);
        textMotionState = (TextView) findViewById(R.id.motionstate);

        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<>();
        geoCenters = new ArrayList<LatLng>();
        reqIds = new ArrayList<String>();

        // initialize GoogleMaps
        initGMaps();

        // create GoogleApiClient
        createGoogleApi();

        //DELETE SAIF
        BroadcastReceiver bManager = new MyBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(TRYACCESSMAIN);
        //bManager.registerReceiver(bReceiver, intentFilter);
        this.registerReceiver(bManager, intentFilter);


    }

    // Create GoogleApiClient instance
    private void createGoogleApi() {
        Log.d(TAG, "createGoogleApi()");
        if ( googleApiClient == null ) {
            googleApiClient = new GoogleApiClient.Builder( this )
                    .addConnectionCallbacks( this )
                    .addOnConnectionFailedListener( this )
                    .addApi( LocationServices.API )
                    .build();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Call GoogleApiClient connection when starting the Activity
        googleApiClient.connect();



    }

    //DELETE SAIF
    @Override
    protected void onDestroy() {

       // this.unregisterReceiver(bManager);

    }
    //END

    @Override
    protected void onStop() {
        super.onStop();

        // Disconnect GoogleApiClient when stopping Activity
        googleApiClient.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.main_menu, menu );
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch ( item.getItemId() ) {
            case R.id.geofence: {
                //startGeofence();
                return true;
            }
            case R.id.clear: {
                clearGeofence();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private final int REQ_PERMISSION = 999;

    // Check for permission to access Location
    private boolean checkPermission() {
        Log.d(TAG, "checkPermission()");
        // Ask for permission if it wasn't granted yet
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED );
    }

    // Asks for permission
    private void askPermission() {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                REQ_PERMISSION
        );
    }

    // Verify user's response of the permission requested
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch ( requestCode ) {
            case REQ_PERMISSION: {
                if ( grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                    // Permission granted
                    getLastKnownLocation();

                } else {
                    // Permission denied
                    permissionsDenied();
                }
                break;
            }
        }
    }

    // App cannot work without the permissions
    private void permissionsDenied() {
        Log.w(TAG, "permissionsDenied()");
        // TODO close app and warn user
    }

    // Initialize GoogleMaps
    private void initGMaps(){
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Callback called when Map is ready
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady()");
        map = googleMap;
        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG, "onMapClick("+latLng +")");
        markerForGeofence(latLng);
    }

    private Marker geoCenterMarked;
    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClickListener: " + marker.getPosition() );
        //If click happens on geocenter for removal, we need this
        geoCenterMarked = marker;
        geoFenceMarker = null;
        return false;
    }

    private LocationRequest locationRequest;
    // Defined in mili seconds.
    // This number in extremely low, and should be used only for debug
    private final int UPDATE_INTERVAL =  1000;
    private final int FASTEST_INTERVAL = 900;

    // Start location Updates
    private void startLocationUpdates(){
        Log.i(TAG, "startLocationUpdates()");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if ( checkPermission() )
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged ["+location+"]");
        lastLocation = location;
        writeActualLocation(location);
        writeCurrentMotionState();
    }

    public boolean onConnectedCalled = false;
    // GoogleApiClient.ConnectionCallbacks connected
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (onConnectedCalled == false) {
            onConnectedCalled = true;
            Log.i(TAG, "onConnected()");
            getLastKnownLocation();
            //fetch the geofence Database
            Rest.getInstance().fetch(new FetchReply() {
                @Override
                public void onReply(boolean success, List<OccupancyDatabase> data) {
                    if (success) {
                        Log.d("DEBUG_", "Data received" + data.toString());
                        for (OccupancyDatabase iter : data) {
                            Log.d("SFILARGI", "adding " + iter.geoFenceId);
                            startGeofence(new LatLng(iter.latitude, iter.longitude), (float) iter.radius, iter.max_occupancy, iter.current_occupancy);
                        }

                    } else {
                        Log.d("DEBUG_", "failed fetch");
                    }
                }
            });
        } else {
            //nop
        }
    //    recoverGeofenceMarker();
    }

    // GoogleApiClient.ConnectionCallbacks suspended
    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG, "onConnectionSuspended()");
    }

    // GoogleApiClient.OnConnectionFailedListener fail
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG, "onConnectionFailed()");
    }

    // Get last known location
    private void getLastKnownLocation() {
        Log.d(TAG, "getLastKnownLocation()");
        if ( checkPermission() ) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if ( lastLocation != null ) {
                Log.i(TAG, "LasKnown location. " +
                        "Long: " + lastLocation.getLongitude() +
                        " | Lat: " + lastLocation.getLatitude());
                writeLastLocation();
                startLocationUpdates();
            } else {
                Log.w(TAG, "No location retrieved yet");
                startLocationUpdates();
            }
        }
        else askPermission();
    }

    private void writeActualLocation(Location location) {
        textLat.setText( "Lat: " + location.getLatitude() );
        textLong.setText( "Long: " + location.getLongitude() );

        markerLocation(new LatLng(location.getLatitude(), location.getLongitude()));
    }

    private void writeCurrentMotionState() {
        Log.d(TAG, "writeCurrentMotionState()");
        Awareness.getSnapshotClient(this).getDetectedActivity()
                .addOnSuccessListener(new OnSuccessListener<DetectedActivityResponse>() {
                    @Override
                    public void onSuccess(DetectedActivityResponse dar) {
                        ActivityRecognitionResult arr = dar.getActivityRecognitionResult();
                        // getMostProbableActivity() is good enough for basic Activity detection.
                        // To work within a threshold of confidence,
                        // use ActivityRecognitionResult.getProbableActivities() to get a list of
                        // potential current activities, and check the confidence of each one.
                        DetectedActivity probableActivity = arr.getMostProbableActivity();

                        int confidence = probableActivity.getConfidence();
                        String activityStr = probableActivity.toString();
                        textMotionState.setText("Current motion : " + activityStr +
                                                "confidence:" + confidence + "/100");
                    }
                });
    }

    private void writeLastLocation() {
        writeActualLocation(lastLocation);
    }

    private Marker locationMarker;
    private void markerLocation(LatLng latLng) {
        Log.i(TAG, "markerLocation("+latLng+")");
        String title = latLng.latitude + ", " + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if ( map!=null ) {
            if ( locationMarker != null )
                locationMarker.remove();
            locationMarker = map.addMarker(markerOptions);
            float zoom = 17f;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            map.animateCamera(cameraUpdate);
        }
    }




    private Marker geoFenceMarker;
    private void markerForGeofence(LatLng latLng) {
        Log.i(TAG, "markerForGeofence("+latLng+")");
        String title = latLng.latitude + ", " + latLng.longitude;
        // Define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title);
        if ( map!=null ) {
            // Remove last geoFenceMarker
            /*
            if (geoFenceMarker != null)
                geoFenceMarker.remove();
             */
            geoFenceMarker = map.addMarker(markerOptions);

            gfmarkr.add(geoFenceMarker);
            Log.d(TAG, "geoFenceMarker added");

            map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {

                }

                @Override
                public void onMarkerDrag(Marker marker) {

                }

                @Override
                public void onMarkerDragEnd(Marker marker) {

                    Log.d("System out", "onMarkerDragEnd...");
                    map.animateCamera(CameraUpdateFactory.newLatLng(marker.getPosition()));
                    double lat = marker.getPosition().latitude;
                    double lng = marker.getPosition().longitude;
                    Toast.makeText(getBaseContext(), "" + lat + ", " + lng, Toast.LENGTH_SHORT).show();
                    addressDragged(lat, lng);
                }
            });
        }
    }

    public void addressDragged(final double lat, final double lng){

        Geocoder geocoder = new Geocoder(getBaseContext(), Locale.getDefault());
        String result = "";
        try {
            List<Address> addressList = geocoder.getFromLocation(
                    lat, lng, 1);
            if (addressList != null && addressList.size() > 0) {
                String addres = addressList.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                result = addres;
            }
            else {
                result="Failed to retrieve address.";
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable connect to Geocoder", e);
        }
        Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
    }

    // Start Geofence creation process
    private LatLng finalGeofenceCenter;
    private void startGeofence(LatLng latLng, float radius, int maxoccupancy, int currentoccupancy) {
        Log.i(TAG, "startGeofence()");
        /*if (geoFenceMarker != null) {
            finalGeoFenceMarker = geoFenceMarker;
        } else {
            finalGeoFenceMarker = geoCenterMarked;
        } */


        if( latLng != null ) {
            if (geoFenceIsNonOverlapping(latLng, radius)) {
                finalGeofenceCenter = latLng;
                String reqId = "Geo" + Integer.toString(geoId);
                Log.d(TAG, "adding to reqIds, array size " + reqIds.size());

                //reqIds.add(reqId);

                geoId++;

                Geofence geofence = createGeofence(finalGeofenceCenter, radius, reqId);
                geofenceClass geoObject = new geofenceClass();
                geoObject.setGeofence(geofence);
                //geoObject.setGeoMarker(finalGeofenceCenter);
                geoObject.setGeoCenter(finalGeofenceCenter);
                geoObject.setRequestId(reqId);
                geoObject.setRadius(radius);
                geoObject.setMaxOccupancy(maxoccupancy);
                geoObject.setCurrOccupancy(currentoccupancy);
                geoObjects.add(geoObject);
                pendingDrawGeofence.add(geoObject);
                geoHash.put(reqId, geoObject);
                geoLatHash.put(finalGeofenceCenter, geoObject);
                Log.w(TAG, "Adding to geoLatHash" + finalGeofenceCenter);
                //finalGeoFenceMarker.setTag(reqId);

                //mGeofenceList.add(geofence);

                //geoCenters.add(geoFenceMarker.getPosition());

                GeofencingRequest geofenceRequest = createGeofenceRequest(geofence);
                addGeofence(geofenceRequest);
            }
            else {
                Log.e(TAG, "This geofence overlaps with one that is already present." +
                        "failed to create");
            }
        } else {
            Log.e(TAG, "Geofence marker is null");
        }
    }

    private boolean geoFenceIsNonOverlapping(LatLng latLng, float radius) {
        float[] results = {0};
        for (geofenceClass geoObject : geoObjects) {
            LatLng iterLatLng = geoObject.getGeoCenter();
            Location.distanceBetween(latLng.latitude,
                    latLng.longitude,
                    iterLatLng.latitude,
                    iterLatLng.longitude,
                    results);
            Log.d(TAG, "Overlapping?" + latLng + "," + iterLatLng + ",radius" + radius + "distanceBetween:" + results[0]);
            if (results[0] < 2 * radius) {
                return false;
            }
        }
        return true;
    }

    private static final long GEO_DURATION = 60 * 60 * 1000;
    //private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final float GEOFENCE_RADIUS = 500.0f; // in meters

    // Create a Geofence
    private Geofence createGeofence( LatLng latLng, float radius, String reqId ) {
        Log.d(TAG, "createGeofence");
        return new Geofence.Builder()
                .setRequestId(reqId)
                .setCircularRegion( latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration( GEO_DURATION )
                .setTransitionTypes( Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT )
                .build();
    }

    // Create a Geofence Request
    private GeofencingRequest createGeofenceRequest(Geofence geofence) {
        Log.d(TAG, "createGeofenceRequest");
        return new GeofencingRequest.Builder()
                .setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER )
                .addGeofence( geofence )
                .build();
    }

    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;
    private PendingIntent createGeofencePendingIntent() {
        Log.d(TAG, "createGeofencePendingIntent");
        if ( geoFencePendingIntent != null )
            return geoFencePendingIntent;

        Intent intent = new Intent( this, GeofenceTrasitionService.class);
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT );
    }

    // Add the created GeofenceRequest to the device's monitoring list
    private void addGeofence(GeofencingRequest request) {
        Log.d(TAG, "addGeofence");
        if (checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this);
    }

    @Override
    public void onResult(@NonNull Status status) {
        Log.i(TAG, "onResult: " + status);
        if ( status.isSuccess() ) {
            //saveGeofence();
            geofenceClass geoObject = pendingDrawGeofence.get(0);
            //drawGeofence(geoObject.getGeoCenter(), geoObject.getRadius(),255,255,100);
            pendingDrawGeofence.remove(0);
        } else {
            // inform about fail
        }
    }

    // Draw Geofence circle on GoogleMap
    public static Circle geoFenceLimits;
    public static void drawGeofence(LatLng geoCenter, float radius, int red, int green, int blue) {
        Log.d(TAG, "drawGeofence()");
        try {
            geoLatHash.get(geoCenter).getGeoLimit().remove();
        } catch(NullPointerException e) {
            //nop
        }
        CircleOptions circleOptions = new CircleOptions()
                .center( geoCenter)
                .strokeColor(Color.rgb(red, green, blue))
                .fillColor(Color.rgb(red, green, blue))
                .radius( radius );
        geoFenceLimits = map.addCircle( circleOptions );
        //gflimits.add(geoFenceLimits);
        geoLatHash.get(geoCenter).setGeoLimit(geoFenceLimits);
    }

    private final String KEY_GEOFENCE_LAT = "GEOFENCE LATITUDE";
    private final String KEY_GEOFENCE_LON = "GEOFENCE LONGITUDE";

    // Saving GeoFence marker with prefs mng
    private void saveGeofence() {
        Log.d(TAG, "saveGeofence()");
        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.putLong( KEY_GEOFENCE_LAT, Double.doubleToRawLongBits( geoFenceMarker.getPosition().latitude ));
        editor.putLong( KEY_GEOFENCE_LON, Double.doubleToRawLongBits( geoFenceMarker.getPosition().longitude ));
        editor.apply();
    }

    // Recovering last Geofence marker
    private void recoverGeofenceMarker() {
        Log.d(TAG, "recoverGeofenceMarker");
        SharedPreferences sharedPref = getPreferences( Context.MODE_PRIVATE );

        if ( sharedPref.contains( KEY_GEOFENCE_LAT ) && sharedPref.contains( KEY_GEOFENCE_LON )) {
            double lat = Double.longBitsToDouble( sharedPref.getLong( KEY_GEOFENCE_LAT, -1 ));
            double lon = Double.longBitsToDouble( sharedPref.getLong( KEY_GEOFENCE_LON, -1 ));
            LatLng latLng = new LatLng( lat, lon );
            markerForGeofence(latLng);
            //drawGeofence();
        }
    }

    // Clear Geofence
    private void clearGeofence() {
        List<String> reqIdList = new ArrayList<String>();
        String reqId;
        try {
            Log.w(TAG, "clearGeofence() finding geoLatHash" + geoCenterMarked.getPosition());
            reqId = geoLatHash.get(geoCenterMarked.getPosition()).getRequestId();
        } catch (NullPointerException e ) {
            Log.e(TAG, "No Geofence found for that Position");
            if (geoCenterMarked != null) {
                geoCenterMarked.remove();
            }
            return;
        }
        Log.d(TAG, "reqId for deletion" + reqId);
        final String forward_reqId = reqId;
        reqIdList.add(reqId);

        LocationServices.GeofencingApi.removeGeofences(
                googleApiClient,
                reqIdList
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                if ( status.isSuccess() ) {
                    // remove drawing

                    removeGeofenceDraw(geoHash.get(forward_reqId));
                    geoObjects.remove(geoHash.get(forward_reqId));
                    //reqIds.remove(forward_iter);
                    //Log.d(TAG, "Removing from reqIds, array size " + reqIds.size());
                    //mGeofenceList.remove(forward_iter);
                    //geoCenters.remove(forward_iter);
                }
            }
        });


    }


    private void removeGeofenceDraw(geofenceClass geoObject) {
        Log.d(TAG, "removeGeofenceDraw()");
        if ( geoObject.getGeoMarker() != null)
            geoObject.getGeoMarker().remove();
        if ( geoObject.getGeoLimit() != null )
           geoObject.getGeoLimit().remove();
    }


    //DELETE SAIF
    public static final String TRYACCESSMAIN = "com.tinmegali.mylocation.TRYACCESSMAIN";
    public final Handler handler = new Handler();

 //   private BroadcastReceiver bReceiver = new BroadcastReceiver() {
    private class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            //boolean foundBestGeo = false;
            //geofenceClass bestGeoObject = null;
            Log.i("service", "entered onReceive");
            if(intent.getAction().equals(TRYACCESSMAIN)) {
                String serviceJsonString = intent.getStringExtra("data");
                if (serviceJsonString.equals("printAllGeos") ) {

                    //color all the geofences orange
                    for (geofenceClass geoIter : geoObjects) {
                        Log.i(TAG, "making Orange" + geoIter.getRequestId().toString() + "Center" + geoIter.getGeoCenter());
                        if(!geoIter.getRequestId().equals("Geo0")) {
                            drawGeofence(geoIter.getGeoCenter(), geoIter.getRadius(), 100, 100, 100);
                        }
                    }


                  /*  handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            drawGeofence(geoObjects.get(0).getGeoCenter(), geoObjects.get(0).getRadius(),255,0,0);
                        }
                    }, 1000);*/

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            drawGeofence(geoObjects.get(3).getGeoCenter(), geoObjects.get(3).getRadius(),255,0,0);
                        }
                    }, 1000);

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            drawGeofence(geoObjects.get(4).getGeoCenter(), geoObjects.get(4).getRadius(),255,0,0);
                        }
                    }, 2000);

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            drawGeofence(geoObjects.get(2).getGeoCenter(), geoObjects.get(2).getRadius(),0,255,0);
                        }
                    }, 3000);



                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //geoObjects.get(0).getGeoLimit().remove();
                            geoObjects.get(1).getGeoLimit().remove();
                            geoObjects.get(3).getGeoLimit().remove();
                            geoObjects.get(4).getGeoLimit().remove();

                        }
                    }, 4000);



                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                                    Uri.parse("http://maps.google.com/maps?saddr="+
                                            lastLocation.getLatitude()+","+
                                            lastLocation.getLongitude()+"&daddr="+
                                            geoObjects.get(2).getGeoCenter().latitude+","+
                                            geoObjects.get(2).getGeoCenter().longitude));
                            startActivity(intent);
                        }
                    }, 6000);

/*                    for (int i=0; i<2; i++) {
                        drawGeofence(geoObjects.get(i).getGeoCenter(), geoObjects.get(i).getRadius(),255,0,0);
                        try {
                            TimeUnit.MILLISECONDS.sleep(200);
                        } catch(InterruptedException E) {
                            //nop
                        }
                    }

                    drawGeofence(geoObjects.get(2).getGeoCenter(),geoObjects.get(2).getRadius(),0,255,0);

                    for (int i=0; i<5; i++) {
                        if(i!=2) {
                            try {
                                geoObjects.get(i).getGeoLimit().remove();
                            } catch (NullPointerException e) {
                                //nop
                            }
                        }
                    }*/
                    //select best geoFence
/*                      for (geofenceClass geoIter : geoObjects) {

                      Log.i(TAG, "Looking at best" + geoIter.getRequestId().toString());
                        if (!foundBestGeo) {
                            if (geoIter.getMaxOccupancy() - geoIter.getCurrOccupancy() > 0) {
                                bestGeoObject = geoIter;
                                foundBestGeo = true;
                                break;
                            } else {
                                drawGeofence(geoIter.getGeoCenter(),geoIter.getRadius(), 255, 0,0);
                            }
                        }
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch(InterruptedException E) {
                            //nop
                        }
                    }
                    if(bestGeoObject != null) {
                        Log.i(TAG, "drawingBest" + bestGeoObject.getRequestId().toString());
                        drawGeofence(bestGeoObject.getGeoCenter(),bestGeoObject.getRadius(),0,255,0);
                    }

                    //remove all other geoFences
                    for (geofenceClass geoIter : geoObjects) {
                        if(geoIter.getRequestId() != bestGeoObject.getRequestId()) {
                            geoIter.getGeoLimit().remove();
                        }
                    }
*/

                }
                 //Do something with the string
            }
        }
    };

}
