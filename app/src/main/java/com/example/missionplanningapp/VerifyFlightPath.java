package com.example.missionplanningapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class VerifyFlightPath extends AppCompatActivity implements OnMapReadyCallback,
        GoogleMap.OnInfoWindowClickListener {

    private GoogleMap mMap;

    private ArrayList<Waypoints> WpList = new ArrayList<>();
    private ArrayList<LatLng> PointList = new ArrayList<>();
    private Polyline polyline;
    private ArrayList<Circle> circleList = new ArrayList<>();

    boolean add_waypoints = false;
    boolean delete_waypoints = false;

    private double default_radius = 2;
    private int strokeColor = Color.YELLOW;

    private Marker quadmarker;
    private Bitmap markerIcon;
    private long MOVE_ANIMATION_DURATION = 5000;
    private int mIndexCurrentPoint = 0;

    private Button SimulateFlightButton;

    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_flight_path);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Verify Flight Path");
        }
        mToolbar.setTitleTextColor(Color.WHITE);

        db = new DatabaseHelper(getApplicationContext());
        WpList = db.getCurrentMissionData();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        markerIcon = BitmapFactory.decodeResource(getResources(),R.drawable.redquad);

        SimulateFlightButton = findViewById(R.id.simulate_button);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public void AddWaypoint(View view) {
        add_waypoints = ((CheckBox) view).isChecked();
    }

    public void DeleteWaypoint(View view) {
        delete_waypoints = ((CheckBox) view).isChecked();
    }

    public void SimulateFlight(View view) {
        if (quadmarker != null) {
            quadmarker.remove();
            mIndexCurrentPoint = 0;
        }
        LatLng loc = PointList.get(0);
        quadmarker = mMap.addMarker(new MarkerOptions().position(loc).anchor(0.5f, 0.5f).
                icon(BitmapDescriptorFactory.fromResource(R.drawable.redquad)).flat(true));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 19));
        SimulateFlightButton.setVisibility(View.INVISIBLE);
        //https://stackoverflow.com/questions/40526350/how-to-move-marker-along-polyline-using-google-map/40686476
        animateUAVMove(quadmarker, PointList.get(0), PointList.get(1), MOVE_ANIMATION_DURATION);
    }

    public void AcceptMission(View view) {
        db.deleteCurrentMission();
        db.createCurrentMissionData(WpList);
        this.finish();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        //mMap.setBuildingsEnabled(true);

        showWaypoints();
        updatePointList();
        DrawLine();

        mMap.setOnInfoWindowClickListener(this);

        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {

            }

            @Override
            public void onMarkerDrag(Marker marker) {

            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                Object number = marker.getTag();
                final int num = (int) number;
                LatLng new_pos = marker.getPosition();
                Waypoints wp1 = WpList.get(num);
                wp1.setLat(new_pos.latitude);
                wp1.setLng(new_pos.longitude);
                WpList.set(num,wp1);
                PointList.set(num,new_pos);
                updateCircles();
                DrawLine();
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                if (delete_waypoints) {
                    AlertDialog.Builder delete = new AlertDialog.Builder(VerifyFlightPath.this);
                    delete.setTitle(R.string.delete_message);
                    delete.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Object number = marker.getTag();
                            final int num = (int) number;
                            WpList.remove(num);
                            mMap.clear();
                            showWaypoints();
                            updatePointList();
                            DrawLine();
                        }
                    });
                    delete.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    delete.show();
                    return true;
                }
                return false;
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
                if(add_waypoints) {
                    AlertDialog.Builder altitude = new AlertDialog.Builder(VerifyFlightPath.this);
                    altitude.setTitle(R.string.altitude_message);
                    final EditText input = new EditText(VerifyFlightPath.this);
                    input.setInputType(InputType.TYPE_CLASS_NUMBER);
                    input.setRawInputType(Configuration.KEYBOARD_12KEY);
                    altitude.setView(input);
                    altitude.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String value = input.getText().toString();
                            if(!value.isEmpty()) {
                                double alt = Double.parseDouble(value);
                                Waypoints WP = new Waypoints();
                                WP.setLat(latLng.latitude);
                                WP.setLng(latLng.longitude);
                                WP.setalt(alt);
                                WpList.add(WP);
                                showWaypoints();
                                updatePointList();
                                DrawLine();
                            }
                            else {
                                AlertDialog.Builder no_alt = new AlertDialog.Builder(VerifyFlightPath.this);
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
    }

    @Override
    public void onInfoWindowClick(final Marker marker) {
        Object position = marker.getTag();
        final int pos = (int) position;

        AlertDialog.Builder window_select = new AlertDialog.Builder(this);
        window_select.setTitle("Edit Altitude")
                .setMessage("Would you like to change the altitude for this waypoint?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AlertDialog.Builder altitude_edit = new AlertDialog.Builder(VerifyFlightPath.this);
                        altitude_edit.setTitle(R.string.altitude_message);
                        final EditText input = new EditText(VerifyFlightPath.this);
                        input.setInputType(InputType.TYPE_CLASS_NUMBER);
                        input.setRawInputType(Configuration.KEYBOARD_12KEY);
                        altitude_edit.setView(input);
                        altitude_edit.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String value = input.getText().toString();
                                if(!value.isEmpty()) {
                                    double alt = Double.parseDouble(value);
                                    Waypoints WP = WpList.get(pos);
                                    WP.setalt(alt);
                                    WpList.set(pos,WP);
                                    marker.setSnippet("Altitude: " + alt + "m");
                                    marker.hideInfoWindow();
                                    marker.showInfoWindow();
                                }
                                else {
                                    AlertDialog.Builder no_alt = new AlertDialog.Builder(VerifyFlightPath.this);
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
                        altitude_edit.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        altitude_edit.show();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        AlertDialog alert = window_select.create();
        alert.show();

    }

    protected void showWaypoints() {
        mMap.clear();
        for (int i = 0; i < WpList.size(); i++) {
            float color;
            if (i == 0) {
                color = BitmapDescriptorFactory.HUE_GREEN;
            } else if (i == WpList.size()-1) {
                color = BitmapDescriptorFactory.HUE_RED;
            } else {
                color = BitmapDescriptorFactory.HUE_BLUE;
            }
            Waypoints Wp = WpList.get(i);
            LatLng loc = new LatLng(Wp.getLat(),Wp.getLng());
            mMap.addMarker(new MarkerOptions().position(loc).icon(BitmapDescriptorFactory.defaultMarker(color))
                    .title("Waypoint #" + (i+1)).snippet("Altitude: " + Wp.getalt() + "m")
                    .draggable(true)).setTag(i);
            Circle circle = mMap.addCircle(new CircleOptions().center(loc).radius(default_radius).strokeColor(strokeColor).strokeWidth(2));
            if (circleList.size() < (i+1)) {
                circleList.add(circle);
            } else {
                circleList.set(i, circle);
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 19));
            /*CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(loc)
                    .zoom(21).bearing(67).tilt(45).build();

            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));*/
        }
    }

    protected void updatePointList() {
        PointList.clear();
        for (int i = 0; i < WpList.size(); i++) {
            Waypoints Wp = WpList.get(i);
            PointList.add(new LatLng(Wp.getLat(), Wp.getLng()));
        }
    }

    protected void updateCircles() {
        for (int i = 0; i < circleList.size(); i++) {
            Circle circle = circleList.get(i);
            circle.remove();
            Waypoints wp = WpList.get(i);
            LatLng loc = new LatLng(wp.getLat(),wp.getLng());
            Circle new_circle = mMap.addCircle(new CircleOptions().center(loc).radius(default_radius).strokeColor(strokeColor).strokeWidth(2));
            circleList.set(i,new_circle);
        }
    }

    protected void DrawLine() {
        if(polyline != null) {
            polyline.remove();
        }
        polyline = mMap.addPolyline(new PolylineOptions().addAll(PointList)
                .width(5).color(Color.BLUE));
    }

    private void animateUAVMove(final Marker marker, final LatLng beginLatLng, final LatLng endLatLng, final long duration) {
        final Handler handler = new Handler();
        final long startTime = SystemClock.uptimeMillis();

        final Interpolator interpolator = new LinearInterpolator();

        // set UAV bearing for current part of path
        float angleDeg = (float)(180 * getAngle(beginLatLng, endLatLng) / Math.PI);
        Matrix matrix = new Matrix();
        matrix.postRotate(angleDeg);
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(markerIcon, 0, 0, markerIcon.getWidth(), markerIcon.getHeight(), matrix, true)));

        handler.post(new Runnable() {
            @Override
            public void run() {
                // calculate phase of animation
                long elapsed = SystemClock.uptimeMillis() - startTime;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                // calculate new position for marker
                double lat = (endLatLng.latitude - beginLatLng.latitude) * t + beginLatLng.latitude;
                double lngDelta = endLatLng.longitude - beginLatLng.longitude;

                if (Math.abs(lngDelta) > 180) {
                    lngDelta -= Math.signum(lngDelta) * 360;
                }
                double lng = lngDelta * t + beginLatLng.longitude;

                marker.setPosition(new LatLng(lat, lng));

                // if not end of line segment of path
                if (t < 1.0) {
                    // call next marker position
                    handler.postDelayed(this, 16);
                } else {
                    MediaPlayer mediaPlayer = MediaPlayer.create(VerifyFlightPath.this, R.raw.bell);
                    mediaPlayer.start();
                    // call turn animation
                    nextTurnAnimation();
                }
            }
        });
    }

    private void nextTurnAnimation() {
        mIndexCurrentPoint++;
        long TURN_ANIMATION_DURATION = 1000;

        if (mIndexCurrentPoint < PointList.size() - 1) {
            LatLng prevLatLng = PointList.get(mIndexCurrentPoint - 1);
            LatLng currLatLng = PointList.get(mIndexCurrentPoint);
            LatLng nextLatLng = PointList.get(mIndexCurrentPoint + 1);

            float beginAngle = (float)(180 * getAngle(prevLatLng, currLatLng) / Math.PI);
            float endAngle = (float)(180 * getAngle(currLatLng, nextLatLng) / Math.PI);

            if (beginAngle - endAngle > 180) {
                if (beginAngle < 0 && endAngle > 0) {
                    beginAngle = 360 + beginAngle;
                } else if (beginAngle > 0 && endAngle < 0) {
                    endAngle = 360 + endAngle;
                }
            }

            animateUAVTurn(quadmarker, beginAngle, endAngle, TURN_ANIMATION_DURATION);
        } else {
            SimulateFlightButton.setVisibility(View.VISIBLE);
        }
    }

    private void animateUAVTurn(final Marker marker, final float startAngle, final float endAngle, final long duration) {
        final Handler handler = new Handler();
        final long startTime = SystemClock.uptimeMillis();
        final Interpolator interpolator = new LinearInterpolator();

        final float dAndgle = endAngle - startAngle;

        Matrix matrix = new Matrix();
        matrix.postRotate(startAngle);
        Bitmap rotatedBitmap = Bitmap.createBitmap(markerIcon, 0, 0, markerIcon.getWidth(), markerIcon.getHeight(), matrix, true);
        marker.setIcon(BitmapDescriptorFactory.fromBitmap(rotatedBitmap));

        handler.post(new Runnable() {
            @Override
            public void run() {

                long elapsed = SystemClock.uptimeMillis() - startTime;
                float t = interpolator.getInterpolation((float) elapsed / duration);

                Matrix m = new Matrix();
                m.postRotate(startAngle + dAndgle * t);
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(markerIcon, 0, 0, markerIcon.getWidth(), markerIcon.getHeight(), m, true)));

                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                } else {
                    nextMoveAnimation();
                }
            }
        });
    }

    private void nextMoveAnimation() {
        if (mIndexCurrentPoint <  PointList.size() - 1) {
            animateUAVMove(quadmarker, PointList.get(mIndexCurrentPoint), PointList.get(mIndexCurrentPoint+1), MOVE_ANIMATION_DURATION);
        } else {
            SimulateFlightButton.setVisibility(View.VISIBLE);
        }
    }

    private double getAngle(LatLng beginLatLng, LatLng endLatLng) {
        double f1 = Math.PI * beginLatLng.latitude / 180;
        double f2 = Math.PI * endLatLng.latitude / 180;
        double dl = Math.PI * (endLatLng.longitude - beginLatLng.longitude) / 180;
        return Math.atan2(Math.sin(dl) * Math.cos(f2) , Math.cos(f1) * Math.sin(f2) - Math.sin(f1) * Math.cos(f2) * Math.cos(dl));
    }
}