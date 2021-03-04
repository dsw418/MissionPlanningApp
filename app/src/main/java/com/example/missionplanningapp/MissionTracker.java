package com.example.missionplanningapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;
import com.o3dr.services.android.lib.util.MathUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MissionTracker extends AppCompatActivity implements DroneListener, TowerListener,
        LocationListener,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback {

    private static final String TAG = MissionTracker.class.getSimpleName();

    private ControlTower controlTower;
    private Drone drone;
    private final Handler handler = new Handler();

    private GoogleMap mMap;
    private Marker marker;
    private LatLong markerlocation;

    private boolean auto_pan = true;
    private int picture;
    private boolean mission_started = false;
    private double update_dist = 1.5;

    private int selectedConnectionType;
    private int DEFAULT_USB_BAUD_RATE;
    private String tcpServerIP;
    private int tcpServerPort;
    private boolean missionDisplayed = false;
    private ConnectionParameter connectionParams;

    private ArrayList<LatLng> flightPath = new ArrayList<>();
    private ArrayList<LatLongAlt> waypointsList = new ArrayList<>();

    GoogleApiClient googleApiClient;
    LocationRequest locationRequest;
    Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mission_tracker);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mission");
        }
        mToolbar.setTitleTextColor(Color.WHITE);

        // Initialize the service manager
        this.controlTower = new ControlTower(getApplicationContext());
        this.drone = new Drone(getApplicationContext());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            selectedConnectionType = bundle.getInt("Type");
            // USB Connection
            if (selectedConnectionType == 0) {
                DEFAULT_USB_BAUD_RATE = bundle.getInt("Baud");
            }
            // TCP/IP Connection
            else {
                tcpServerIP = bundle.getString("IP");
                tcpServerPort = bundle.getInt("Port");
            }
        }

        AlertDialog.Builder preArm = new AlertDialog.Builder(MissionTracker.this);
        preArm.setTitle(R.string.preArm_message);
        preArm.setCancelable(false);
        preArm.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        preArm.show();

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API).addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(MissionTracker.this).build();
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
                                status.startResolutionForResult(MissionTracker.this, 1000);
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
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        googleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        //googleApiClient.disconnect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
        googleApiClient.disconnect();
    }

    // 3DR Services Listener
    @Override
    public void onTowerConnected() {
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);

        // USB Connection
        if (selectedConnectionType == 0) {
            connectionParams = ConnectionParameter.newUsbConnection(DEFAULT_USB_BAUD_RATE, null);
        }
        // TCP/IP Connection
        else {
            connectionParams = ConnectionParameter.newTcpConnection(tcpServerIP, tcpServerPort, null);
        }

        if (!this.drone.isConnected()) {
            this.drone.connect(connectionParams);
        }
    }

    @Override
    public void onTowerDisconnected() {

    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
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
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {

        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                if (!missionDisplayed) {
                    loadMission();
                }
                findType();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                alertUser("Attempting to Reconnect");
                attemptReconnect();
                break;

            case AttributeEvent.STATE_ARMING:
                updateButtons();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                updateAttitude();
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateBattery();
                break;

            case AttributeEvent.GPS_POSITION:
                if (mMap != null) {
                    updateLocation();
                }
                break;

            case AttributeEvent.MISSION_RECEIVED:
                if (!missionDisplayed) {
                    alertUser("Mission Downloaded");
                    displayMission();
                }
                break;

            case AttributeEvent.STATE_UPDATED:
                updateButtons();
                break;

            case AttributeEvent.MISSION_ITEM_REACHED:
                Mission droneMission = this.drone.getAttribute(AttributeType.MISSION);
                Gps droneGPS = this.drone.getAttribute(AttributeType.GPS);
                Altitude droneAlt = this.drone.getAttribute(AttributeType.ALTITUDE);
                LatLongAlt gps_loc = new LatLongAlt(droneGPS.getPosition().getLatitude(), droneGPS.getPosition().getLongitude(), droneAlt.getAltitude());
                int last_wp = droneMission.getCurrentMissionItem() - 1;
                WaypointAccuracy(last_wp, gps_loc);
                WaypointSound();
                break;

            default:
                Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    protected void updateAltitude() {
        TextView altitudeTextView = findViewById(R.id.AltTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double alt = droneAltitude.getAltitude();
        DecimalFormat df = new DecimalFormat("#.##");
        altitudeTextView.setText(df.format(alt));
    }

    protected void updateAttitude() {
        Attitude droneAttitude = this.drone.getAttribute(AttributeType.ATTITUDE);
        double yaw = droneAttitude.getYaw();
        if (marker != null) {
            marker.setRotation((float)yaw);
        }
    }

    protected void updateBattery() {
        TextView batteryTextView = findViewById(R.id.BatTextView);
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        double bv = droneBattery.getBatteryVoltage();
        DecimalFormat df = new DecimalFormat("#.##");
        batteryTextView.setText(df.format(bv));
    }

    protected void updateLocation() {
        Gps droneGPS = this.drone.getAttribute(AttributeType.GPS);
        if (!droneGPS.getFixStatus().equals(Gps.NO_FIX)) {
            LatLong currentLocation = droneGPS.getPosition();
            LatLng loc = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            if (marker != null) {
                double distance = MathUtils.getDistance2D(currentLocation,markerlocation);
                //alertUser(String.valueOf(distance));
                if (distance > update_dist) {
                    marker.remove();
                    marker = mMap.addMarker(new MarkerOptions().position(loc).anchor(0.5f, 0.5f).
                            icon(BitmapDescriptorFactory.fromResource(picture)).flat(true));
                    updateAttitude();
                    markerlocation = new LatLong(loc.latitude,loc.longitude);
                    if (auto_pan) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(loc));
                    }
                    flightPath.add(loc);
                    mMap.addPolyline(new PolylineOptions().add(flightPath.get(flightPath.size()-2),flightPath.get(flightPath.size()-1))
                            .width(5).color(Color.RED));
                }
            } else {
                marker = mMap.addMarker(new MarkerOptions().position(loc).anchor(0.5f, 0.5f).
                        icon(BitmapDescriptorFactory.fromResource(picture)).flat(true));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 19));
                markerlocation = new LatLong(loc.latitude,loc.longitude);
                updateAttitude();
                flightPath.add(loc);
            }
            TextView hdop_textview = findViewById(R.id.HDOPtextView);
            double hdop = droneGPS.getGpsEph();
            DecimalFormat df = new DecimalFormat("#.##");
            hdop_textview.setText(df.format(hdop));
        }
    }

    protected void loadMission() {
        MissionApi.getApi(drone).loadWaypoints();
    }

    protected void findType() {
        Type type = this.drone.getAttribute(AttributeType.TYPE);
        int droneType = type.getDroneType();
        if (droneType == Type.TYPE_COPTER) {
            picture = R.drawable.redquad;
        } else if (droneType == Type.TYPE_PLANE) {
            picture = R.drawable.redplane;
        }
    }

    protected void displayMission() {
        Mission mission = this.drone.getAttribute(AttributeType.MISSION);
        List<MissionItem> waypoints = mission.getMissionItems();
        LatLng prev_loc = new LatLng(0,0);
        for(int i = 0; i < waypoints.size(); i++) {
            if (waypoints.get(i) instanceof MissionItem.SpatialItem) {
                MissionItem.SpatialItem wp = (MissionItem.SpatialItem) waypoints.get(i);
                LatLongAlt wpCoordinate = wp.getCoordinate();
                waypointsList.add(wpCoordinate);
                if (mMap != null) {
                    LatLng location = new LatLng(wpCoordinate.getLatitude(),wpCoordinate.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(location)
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                            .title("Waypoint #" + (i+1)).snippet("Altitude: " + wpCoordinate.getAltitude() +"m"));
                    mMap.addCircle(new CircleOptions().center(location).radius(2).strokeColor(Color.BLUE).strokeWidth(2));
                    if (i > 0) {
                        mMap.addPolyline(new PolylineOptions().add(location, prev_loc)
                                .width(5).color(Color.GREEN));
                    }
                    prev_loc = location;
                }
            }
        }
        missionDisplayed = true;
    }

    protected void WaypointAccuracy(int wp_cnt, LatLongAlt location) {
        if (waypointsList != null) {
            LatLongAlt wp = waypointsList.get(wp_cnt);
            double error = MathUtils.getDistance3D(wp, location);
            alertUser(String.valueOf(error));
        }
    }

    protected void WaypointSound() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.bell);
        mediaPlayer.start();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
    }

    public void onCheckboxClicked(View view) {
        auto_pan = ((CheckBox) view).isChecked();
    }

    public void onTakeoffButtonTap(View view) {
        double safe_takeoff_dist = 5;
        Gps droneGPS = this.drone.getAttribute(AttributeType.GPS);
        double takeoff_dist = MathUtils.getDistance2D(droneGPS.getPosition(),new LatLong(currentLocation.getLatitude(),currentLocation.getLongitude()));
        //if (takeoff_dist > safe_takeoff_dist && currentLocation.getAccuracy() < 20) {
            State vehicleState = this.drone.getAttribute(AttributeType.STATE);
            if (vehicleState.isFlying()) {
                VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        mission_started = false;
                        updateButtons();
                    }

                    @Override
                    public void onError(int executionError) {

                    }

                    @Override
                    public void onTimeout() {

                    }
                });
            } else if (vehicleState.isConnected()) {


                // Connected but not Armed
                VehicleApi.getApi(this.drone).arm(true, false, new SimpleCommandListener() {
                    @Override
                    public void onError(int executionError) {
                        alertUser("Unable to arm vehicle.");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("Arming operation timed out.");
                    }
                });
                VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED);
                ControlApi.getApi(this.drone).takeoff(2, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onError(int executionError) {
                        alertUser("Error on Takeoff");
                    }

                    @Override
                    public void onTimeout() {
                        alertUser("Takeoff Timed Out");
                    }
                });
            }
        /*} else {
            AlertDialog.Builder no_takeoff = new AlertDialog.Builder(this);
            no_takeoff.setTitle(R.string.close_message);
            no_takeoff.setMessage(R.string.no_takeoff_message);
            no_takeoff.setCancelable(false);
            no_takeoff.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            no_takeoff.show();
        }*/
    }

    public void BeginMission(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        if (vehicleState.isFlying() && !mission_started) {
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_AUTO);
            MissionApi missionApi = MissionApi.getApi(drone);
            missionApi.startMission(true, true, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to start mission.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Start mission operation timed out");
                }
                @Override
                public void onSuccess() {
                    mission_started = true;
                    update_dist = 0.5;
                }
            });
        }
    }



    public void Disarm(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        double alt = droneAltitude.getAltitude();
        if (vehicleState.isArmed() || (vehicleState.isFlying() && alt<5)) {
            //VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_STABILIZE);
            VehicleApi.getApi(this.drone).arm(false, true, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to disarm vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Arming operation timed out.");
                }
            });
        }
    }

    public void updateButtons() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button TakeoffButton = findViewById(R.id.TakeoffButton);
        Button missionButton = findViewById(R.id.missionButton);

        if (vehicleState.isFlying()) {
            TakeoffButton.setText(getApplicationContext().getString(R.string.Land));
            if (mission_started) {
                missionButton.setText(getApplicationContext().getString(R.string.stop_mission));
            } else {
                missionButton.setText(getApplicationContext().getString(R.string.begin_mission));
            }
            update_dist = 0.5;
        } else if(vehicleState.isArmed()) {
            TakeoffButton.setText(getApplicationContext().getString(R.string.TakeOff));
            missionButton.setText(getApplicationContext().getString(R.string.begin_mission));
            if (mission_started) {
                mission_started = false;
                update_dist = 1.5;
            }
        } else if (vehicleState.isConnected()) {
            TakeoffButton.setText(getApplicationContext().getString(R.string.TakeOff));
            missionButton.setText(getApplicationContext().getString(R.string.begin_mission));
            if (mission_started) {
                mission_started = false;
                update_dist = 1.5;
            }
        }
    }

    public void attemptReconnect() {
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!MissionTracker.this.drone.isConnected()) {
                    MissionTracker.this.drone.connect(connectionParams, new LinkListener() {
                        @Override
                        public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
                            switch (connectionStatus.getStatusCode()) {
                                case LinkConnectionStatus.FAILED:
                                    alertUser("Reconnection Attempt Failed");
                                    attemptReconnect();
                            }
                        }
                    });
                }
            }
        }, 5000);
    }

    protected void startLocationUpdates() {
        try {
            /*PendingResult<Status> pendingResult = LocationServices.FusedLocationApi
                    .requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);*/
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
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
}