package com.example.missionplanningapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class FlightPath extends AppCompatActivity implements OnMapReadyCallback,
    LocationListener,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    GoogleApiClient googleApiClient;

    LocationRequest locationRequest;
    Location currentLocation;
    String lastUpdateTime;
    int count = 0;
    int wp_count = 0;

    private double lat, lng, alt;
    private LatLng prev_loc;
    private ArrayList<Waypoints> WpList = new ArrayList<>();

    private boolean map_waypoints = false;
    //private boolean started = false;

    private TextView GPSaccuracyTextView;
    private String gps_accuracy;
    DatabaseHelper db;

    private final static String TAG = FlightPath.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flight_path);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Set Flight Path");
        }
        mToolbar.setTitleTextColor(Color.WHITE);

        db = new DatabaseHelper(getApplicationContext());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(FlightPath.this).build();
            googleApiClient.connect();
            locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest);

            // **************************
            builder.setAlwaysShow(true);
            // **************************

            PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi
                    .checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    final LocationSettingsStates state = result
                            .getLocationSettingsStates();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.SUCCESS:
                            // All location settings are satisfied. The client can
                            // initialize location
                            // requests here.
                            break;
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            // Location settings are not satisfied. But could be
                            // fixed by showing the user
                            // a dialog.
                            try {
                                // Show the dialog by calling
                                // startResolutionForResult(),
                                // and check the result in onActivityResult().
                                status.startResolutionForResult(FlightPath.this, 1000);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                            // Location settings are not satisfied. However, we have
                            // no way to fix the
                            // settings so we won't show the dialog.
                            break;
                    }
                }
            });
        }

        GPSaccuracyTextView = findViewById(R.id.GPS_accuracy);
        gps_accuracy = getResources().getString(R.string.gps_accuracy);

    }

    public void setWaypoint(View view){
        if (currentLocation == null) {
            AlertDialog.Builder location = new AlertDialog.Builder(FlightPath.this);
            location.setTitle("No Location Data")
                    .setMessage("Please Check if GPS is enabled")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            AlertDialog alert = location.create();
            alert.show();
        } else {
            lat = currentLocation.getLatitude();
            lng = currentLocation.getLongitude();
            final LatLng loc = new LatLng(lat, lng);

            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle(R.string.altitude_message);
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setRawInputType(Configuration.KEYBOARD_12KEY);
            alert.setView(input);
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    String value = input.getText().toString();
                    if(!value.isEmpty()) {
                        wp_count++;
                        alt = Double.parseDouble(value);
                        Waypoints WP = new Waypoints();
                        WP.setLat(lat);
                        WP.setLng(lng);
                        WP.setalt(alt);
                        WpList.add(WP);

                        mMap.addMarker(new MarkerOptions().position(loc).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                .title("Waypoint #" + wp_count).snippet("Altitude: " + alt + "m")
                                .draggable(true));
                        mMap.addCircle(new CircleOptions().center(loc).radius(2).strokeColor(0xffff0000).strokeWidth(2));

                        if (wp_count > 1) {
                            mMap.addPolyline(new PolylineOptions().add(new LatLng(lat, lng), prev_loc)
                                    .width(5).color(Color.RED));
                        }
                        prev_loc = new LatLng(lat, lng);
                    }
                    else {
                        AlertDialog.Builder no_alt = new AlertDialog.Builder(FlightPath.this);
                        no_alt.setTitle(R.string.no_altitude_message);
                        no_alt.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        no_alt.show();
                    }
                }
            });
            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            alert.show();



        }
    }

    public void ReturnToStart(View view) {
        if (WpList != null) {
            Waypoints WP = WpList.get(0);
            WpList.add(WP);
            wp_count++;
            alt = WP.getalt();
            lat = WP.getLat();
            lng = WP.getLng();

            LatLng loc = new LatLng(lat,lng);

            mMap.addMarker(new MarkerOptions().position(loc).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .title("Waypoint #" + wp_count).snippet("Altitude: " + alt + "m")
                    .draggable(true));
            if (wp_count > 1) {
                mMap.addPolyline(new PolylineOptions().add(new LatLng(lat, lng), prev_loc)
                        .width(5).color(Color.RED));
            }
            prev_loc = new LatLng(lat, lng);
        }
    }

    public void MapWaypoints(View view) {
        map_waypoints = ((CheckBox) view).isChecked();
    }

    public void acceptMission(View view) {
        //Toast toast = Toast.makeText(getApplicationContext(),String.valueOf(WpList.size()),Toast.LENGTH_SHORT);
        //toast.show();
        if (WpList.size()>0){
            //db.createMostRecentlySavedMissionData(WpList);
            db.deleteCurrentMission();
            db.createCurrentMissionData(WpList);
            this.finish();
        }
        else {
            AlertDialog.Builder accept = new AlertDialog.Builder(FlightPath.this);
            accept.setTitle("No Mission Data")
                    .setMessage("No waypoints have been selected")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = accept.create();
            alert.show();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
                if(map_waypoints) {
                    AlertDialog.Builder altitude = new AlertDialog.Builder(FlightPath.this);
                    altitude.setTitle(R.string.altitude_message);
                    final EditText input = new EditText(FlightPath.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    input.setRawInputType(Configuration.KEYBOARD_12KEY);
                    altitude.setView(input);
                    altitude.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String value = input.getText().toString();
                            if(!value.isEmpty()) {
                                wp_count++;
                                double alt = Double.parseDouble(value);
                                Waypoints WP = new Waypoints();
                                WP.setLat(latLng.latitude);
                                WP.setLng(latLng.longitude);
                                WP.setalt(alt);
                                WpList.add(WP);
                                mMap.addMarker(new MarkerOptions().position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                        .title("Waypoint #" + wp_count).snippet("Altitude: " + alt + "m")
                                        .draggable(true));
                                mMap.addCircle(new CircleOptions().center(latLng).radius(2).strokeColor(0xffff0000).strokeWidth(2));
                                if (wp_count > 1) {
                                    mMap.addPolyline(new PolylineOptions().add(latLng, prev_loc)
                                            .width(5).color(Color.RED));
                                }
                                prev_loc = latLng;
                            }
                            else {
                                AlertDialog.Builder no_alt = new AlertDialog.Builder(FlightPath.this);
                                no_alt.setTitle(R.string.no_altitude_message);
                                no_alt.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {

                                    }
                                });
                                no_alt.show();
                            }
                        }
                    });
                    altitude.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    altitude.show();
                }
            }
        });

/*        mMap.setOnMarkerClickListener(new OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Toast.makeText(FlightPath.this, "Selected", Toast.LENGTH_SHORT).show();

                return false;
            }
        });*/
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
                String title = marker.getTitle();
                String[] t2 = title.split("#");
                String c = t2[t2.length-1];
                int cnt = Integer.parseInt(c);

                marker.remove();
                MarkerOptions options = new MarkerOptions();
                Waypoints wp_update = WpList.get(cnt-1);
                LatLng pos = new LatLng(wp_update.getLat(), wp_update.getLng());
                options.position(pos);
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                options.title("Waypoint #" + cnt);
                options.snippet("Altitude: " + wp_update.getalt() + "m");
                options.draggable(marker.isDraggable());
                mMap.addMarker(options);

                // AlertDialog
                AlertDialog.Builder alert = new AlertDialog.Builder(FlightPath.this);
                alert.setTitle(R.string.drag_message);
                alert.setCancelable(false);
                alert.setPositiveButton("Replace", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                alert.setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
                alert.setNegativeButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                alert.show();

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {

            }
        });

    }


    @Override
    public void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onBackPressed() {
        if (wp_count > 0) {
            // insert alert
            AlertDialog.Builder back = new AlertDialog.Builder(this);
            back.setTitle("Waypoints Not Saved")
                    .setMessage("Are you sure you would like to exit?")
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //db.deleteCurrentMission();
                            FlightPath.this.finish();
                        }
                    }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            AlertDialog alert = back.create();
            alert.show();

        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        //db.deleteCurrentMission();
        super.onDestroy();
//        if (started) {
//            stopLocationUpdates();
//        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (googleApiClient.isConnected()) {
            startLocationUpdates();
        }
        count = 0;
    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
        try {
            mMap.setMyLocationEnabled(true);
           }
        catch(SecurityException e){
            Log.e(TAG, "Location Exception");
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        lastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        if (count<1) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 18));
        }
        count++;
        float accuracy = location.getAccuracy();
        String full_gps_accuracy = gps_accuracy + " " + String.valueOf(accuracy) + "m";
        GPSaccuracyTextView.setText(full_gps_accuracy);
    }

    protected void startLocationUpdates() {
        try {
            /*PendingResult<Status> pendingResult = LocationServices.FusedLocationApi
                    .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);*/
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
            //started = true;
        } catch(SecurityException e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermissions();
            }
        }
    }

    // region Permissions
    @TargetApi(Build.VERSION_CODES.M)
    private void requestLocationPermissions() {
        // Android M Permission check
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("This app needs location access");
        builder.setMessage("Please grant location access so this app can use the GPS");
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }
}