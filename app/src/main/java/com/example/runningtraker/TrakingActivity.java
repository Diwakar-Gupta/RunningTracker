package com.example.runningtraker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TrakingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String REQUESTING_LOCATION_UPDATES_KEY = "REQUESTING_LOCATION_UPDATES_KEY";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    GoogleMap googleMap;
    Button startButton, stopButton;

    private Context context;

    boolean isTraking = false;
    ArrayList<ArrayList<LatLng>> polylines;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traking);

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);

        context = this;
        init();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
//        MapView mapFragment = findViewById(R.id.map);
        mapFragment.getMapAsync(this);
        updateValuesFromBundle(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        // Update the value of requestingLocationUpdates from the Bundle.
        if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
            isTraking = savedInstanceState.getBoolean(
                    REQUESTING_LOCATION_UPDATES_KEY);
        }

        if(isTraking){
            startTraking();
        }

        updateUI();
    }

    public void currentLocation(View view){
        if(polylines.size() == 0)return;
        ArrayList<LatLng> list = polylines.get(polylines.size() - 1);
        if(list.size() == 0)return;

        LatLng last = list.get(list.size()-1);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(last, 17));
    }

    public void startButtonHandler(View View){
        startTraking();
    }

    public void stopButtonHandler(View View){
        stopTraking();
    }

    private void stopTraking(){
        isTraking = false;
        stopLocationUpdates();
        stopButton.setClickable(false);
        startButton.setClickable(true);
    }


    private void startTraking(){
        isTraking = true;
        polylines.add(new ArrayList<>());
        startLocationUpdates();
        stopButton.setClickable(true);
        startButton.setClickable(false);
    }


    private void init(){
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        isTraking = false;
        polylines = new ArrayList<>();

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);// 5 seconds
        locationRequest.setFastestInterval(5000);// 2 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                for(Location location: locationResult.getLocations()){
                    if(location != null){
                        addPathPoint(location);
                    }
                }
            }
        };
    }

    private void addPathPoint(Location location){
        LatLng value = new LatLng(location.getLatitude(), location.getLongitude());
        polylines.get(polylines.size()-1).add(value);
        if(polylines.get(polylines.size()-1).size() == 1){
            polylines.get(polylines.size()-1).add(value);
        }

        Log.v("Traking Activity", value.latitude+" "+value.longitude);
        updateUI();
    }

    private void updateUI(){
        ArrayList<LatLng> lastLine = polylines.get(polylines.size()-1);
        LatLng last = lastLine.get(lastLine.size()-1);
        LatLng sLast = lastLine.get(lastLine.size()-2);

        Polyline polyline = googleMap.addPolyline(new PolylineOptions().add(
                last,
                sLast
        ).visible(true));
        stylePolyline(polyline);
    }
    private void stylePolyline(Polyline polyline) {
        String type = "";
        // Get the data object stored with the polyline.
        if (polyline.getTag() != null) {
            type = polyline.getTag().toString();
        }

        switch ("B") {
            // If no type is given, allow the API to use the default.
            case "A":
                // Use a custom bitmap as the cap at the start of the line.
//                polyline.setStartCap(
//                        new CustomCap(
//                                BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow), 10));
                break;
            case "B":
                // Use a round cap at the start of the line.
                polyline.setStartCap(new RoundCap());
                break;
        }

        polyline.setEndCap(new RoundCap());
        polyline.setWidth(POLYLINE_STROKE_WIDTH_PX);
        polyline.setColor(COLOR_BLACK_ARGB);
        polyline.setJointType(JointType.ROUND);
    }
    private static final int COLOR_BLACK_ARGB = 0xff000000;
    private static final int POLYLINE_STROKE_WIDTH_PX = 12;


    @SuppressLint("MissingPermission")
    private void startLocationUpdates(){
        if(!hasLocationPermission()){
            askLocationPermission();
        }

        askToStartLocation();

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    
    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private boolean hasLocationPermission(){
        String[] permissions = {android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION};

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isTraking) {
            startTraking();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                isTraking);
        // ...
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTraking();
    }

    private void askLocationPermission(){
        String[] PERMISSIONS = {android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION};
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions( this, PERMISSIONS, 112 );
        } else {
            //call get location here
        }
    }

    private void askToStartLocation(){
        LocationSettingsRequest.Builder builderRequest = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    Toast.makeText(context, "Can't get Location", Toast.LENGTH_LONG);
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
//        Polyline polyline1 = googleMap.addPolyline(new PolylineOptions()
//                .clickable(true)
//                .add(
//                        new LatLng(-35.016, 143.321),
//                        new LatLng(-34.747, 145.592),
//                        new LatLng(-34.364, 147.891),
//                        new LatLng(-33.501, 150.217),
//                        new LatLng(-32.306, 149.248),
//                        new LatLng(-32.491, 147.309)));
//        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(-23.684, 133.903), 4));
    }
}