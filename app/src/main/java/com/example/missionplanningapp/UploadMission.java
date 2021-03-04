package com.example.missionplanningapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.MissionApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.mission.Mission;
//import com.o3dr.services.android.lib.drone.mission.item.command.Takeoff;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Land;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.util.ArrayList;

public class UploadMission extends AppCompatActivity implements DroneListener, TowerListener {

    private static final String TAG = UploadMission.class.getSimpleName();

    ArrayList<Waypoints> WpList = new ArrayList<>();

    DatabaseHelper db;

    private ControlTower controlTower;
    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private final Handler handler = new Handler();

    private static final int DEFAULT_USB_BAUD_RATE = 57600;
    private String tcpServerIP = "192.168.4.1";
    private int tcpServerPort = 6789;
    int selectedConnectionType = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_mission);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Upload Mission");
        }
        mToolbar.setTitleTextColor(Color.WHITE);

        db = new DatabaseHelper(getApplicationContext());

        WpList = db.getCurrentMissionData();

        // Initialize the service manager
        this.controlTower = new ControlTower(getApplicationContext());
        this.drone = new Drone(getApplicationContext());
    }

    public void BtnConnect(View view) {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
        Spinner connectionSelector = findViewById(R.id.selectConnectionType);
        selectedConnectionType = connectionSelector.getSelectedItemPosition();
        ConnectionParameter connectionParams;
        // USB Connection
        if (selectedConnectionType == 0) {
            connectionParams = ConnectionParameter.newUsbConnection(DEFAULT_USB_BAUD_RATE, null);
        }
        // TCP/IP Connection
        else {
            connectionParams = ConnectionParameter.newTcpConnection(tcpServerIP, tcpServerPort, null);
        }
        this.drone.connect(connectionParams);
        }
    }

    public void MissionUpload(View view) {
        Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
        this.droneType = newDroneType.getDroneType();
        if (this.droneType == Type.TYPE_COPTER) {
            Mission currentMission = new Mission();
            //Takeoff takeoff = new Takeoff();
            //takeoff.setTakeoffAltitude(3);
            //currentMission.addMissionItem(takeoff);
            Waypoints wp = new Waypoints();
            for (int i = 0; i < WpList.size(); i++) {
                Waypoint wp1 = new Waypoint();
                wp = WpList.get(i);
                wp1.setCoordinate(new LatLongAlt(wp.getLat(),wp.getLng(),wp.getalt()));
                wp1.setAcceptanceRadius(1);
                wp1.setDelay(1);
                currentMission.addMissionItem(wp1);
            }
            Land land = new Land();
            land.setCoordinate(new LatLongAlt(wp.getLat(),wp.getLng(),0d));
            currentMission.addMissionItem(land);
            MissionApi missionApi = MissionApi.getApi(drone);
            missionApi.setMission(currentMission,true);
        }
        //else if (this.droneType == Type.TYPE_PLANE) {

        //}
    }

    public void startChecks(View view) {
        if (drone.isConnected()) {
            if (selectedConnectionType == 0) {
                Intent intent = new Intent(this, Checks.class);
                Bundle bundle = new Bundle();
                bundle.putInt("Type", selectedConnectionType);
                bundle.putInt("Baud", DEFAULT_USB_BAUD_RATE);
                intent.putExtras(bundle);
                startActivity(intent);
            } else if(selectedConnectionType == 1) {
                Intent intent = new Intent(this, Checks.class);
                Bundle bundle = new Bundle();
                bundle.putInt("Type", selectedConnectionType);
                bundle.putString("IP", tcpServerIP);
                bundle.putInt("Port",tcpServerPort);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        } else {
            alertUser("Please connect to a drone");
        }
    }

    public void startCalibration(View view) {
        if (drone.isConnected()) {
            if (selectedConnectionType == 0) {
                Intent intent = new Intent(this, Calibration.class);
                Bundle bundle = new Bundle();
                bundle.putInt("Type", selectedConnectionType);
                bundle.putInt("Baud", DEFAULT_USB_BAUD_RATE);
                intent.putExtras(bundle);
                startActivity(intent);
            } else if(selectedConnectionType == 1) {
                Intent intent = new Intent(this, Calibration.class);
                Bundle bundle = new Bundle();
                bundle.putInt("Type", selectedConnectionType);
                bundle.putString("IP", tcpServerIP);
                bundle.putInt("Port",tcpServerPort);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        } else {
            alertUser("Please connect to a drone");
        }
    }

    public void startMissionTracker(View view) {
        if (drone.isConnected()) {
            if (selectedConnectionType == 0) {
                Intent intent = new Intent(this, MissionTracker.class);
                Bundle bundle = new Bundle();
                bundle.putInt("Type", selectedConnectionType);
                bundle.putInt("Baud", DEFAULT_USB_BAUD_RATE);
                intent.putExtras(bundle);
                startActivity(intent);
            } else if(selectedConnectionType == 1) {
                Intent intent = new Intent(this, MissionTracker.class);
                Bundle bundle = new Bundle();
                bundle.putInt("Type", selectedConnectionType);
                bundle.putString("IP", tcpServerIP);
                bundle.putInt("Port",tcpServerPort);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        } else {
            alertUser("Please connect to a drone");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        if (selectedConnectionType != -1) {
            this.drone.disconnect();
            BtnConnect(findViewById(R.id.btnConnect));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    // 3DR Services Listener
    @Override
    public void onTowerConnected() {
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);

        if (!this.drone.isConnected()) {
            // USB Connection
            if (selectedConnectionType == 0) {
                ConnectionParameter connectionParams = ConnectionParameter.newUsbConnection(DEFAULT_USB_BAUD_RATE, null);
                this.drone.connect(connectionParams);
            }
            // TCP/IP Connection
            else if(selectedConnectionType == 1) {
                ConnectionParameter connectionParams = ConnectionParameter.newTcpConnection(tcpServerIP, tcpServerPort, null);
                this.drone.connect(connectionParams);
            }
        }
    }

    @Override
    public void onTowerDisconnected() {

    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {

        switch (event) {
            case AttributeEvent.MISSION_SENT:
                alertUser("Mission Sent");
                break;

            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateConnectedText();
                updateUploadMissionButton();
                updatePreFlightButton();
                updateCalibrationButton();
                updateTelemetryButton();
                //checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateUploadMissionButton();
                updateConnectedText();
                updatePreFlightButton();
                updateCalibrationButton();
                //updateArmButton();
                updateTelemetryButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                }
                break;

            default:
//                Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText(R.string.Disconnect);
        } else {
            connectButton.setText(R.string.Connect);
        }
    }

    protected void updateConnectedText() {
        TextView connectionTextView = findViewById(R.id.ConnectionTextView);

        if (this.drone.isConnected()) {
            connectionTextView.setText(R.string.Connected);
        } else {
            connectionTextView.setText(R.string.noConnection);
        }
    }

    protected void updateUploadMissionButton() {
        Button UploadButton = findViewById(R.id.btnUploadMission);
        if (!this.drone.isConnected()) {
            UploadButton.setVisibility(View.INVISIBLE);
        } else {
            UploadButton.setVisibility(View.VISIBLE);
        }
    }

 /*   protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = findViewById(R.id.btnArmTakeOff);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText(R.string.Land);
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText(R.string.TakeOff);
        } else if (vehicleState.isConnected()){
            // Connected but not Armed
            armButton.setText(R.string.Arm);
        }
    }*/

    protected void updatePreFlightButton() {
        Button armButton = findViewById(R.id.btnPreFlight);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }
    }

    protected void updateCalibrationButton() {
        Button calibrationButton = findViewById(R.id.btn_calibrate);

        if (!this.drone.isConnected()) {
            calibrationButton.setVisibility(View.INVISIBLE);
        } else {
            calibrationButton.setVisibility(View.VISIBLE);
        }
    }

    protected void updateTelemetryButton() {
        Button telemButton = findViewById(R.id.btnTelemetry);

        if (!this.drone.isConnected()) {
            telemButton.setVisibility(View.INVISIBLE);
        } else {
            telemButton.setVisibility(View.VISIBLE);
        }
    }

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }
}