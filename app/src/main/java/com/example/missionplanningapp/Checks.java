package com.example.missionplanningapp;

import android.graphics.Color;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;

import java.text.DecimalFormat;

public class Checks extends AppCompatActivity implements DroneListener, TowerListener {

    private ControlTower controlTower;
    private Drone drone;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checks);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("System Checks");
        }
        mToolbar.setTitleTextColor(Color.WHITE);

        // Initialize the service manager
        this.controlTower = new ControlTower(getApplicationContext());
        this.drone = new Drone(getApplicationContext());
    }

    public void confirmButton(View view) {
        this.finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
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
            ConnectionParameter connectionParams;
            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                int selectedConnectionType = bundle.getInt("Type");
                // USB Connection
                if (selectedConnectionType == 0) {
                    int DEFAULT_USB_BAUD_RATE = bundle.getInt("Baud");
                    connectionParams = ConnectionParameter.newUsbConnection(DEFAULT_USB_BAUD_RATE, null);
                }
                // TCP/IP Connection
                else {
                    String tcpServerIP = bundle.getString("IP");
                    int tcpServerPort = bundle.getInt("Port");
                    connectionParams = ConnectionParameter.newTcpConnection(tcpServerIP, tcpServerPort, null);
                }
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
            case AttributeEvent.STATE_CONNECTED:
                //alertUser("Drone Connected");
                //loadMission();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                //alertUser("Drone Disconnected");
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateBattery();
                break;

            case AttributeEvent.GPS_POSITION:
                updateGPSFix();
                updateGPSCount();
                break;

            case AttributeEvent.GPS_FIX:
                updateGPSFix();
                break;

            case AttributeEvent.GPS_COUNT:
                updateGPSCount();
                break;

            case AttributeEvent.MISSION_RECEIVED:
                //alertUser("Mission Downloaded");
                //displayMission();
                break;

            default:
//                Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    protected void updateGPSFix() {
        TextView GPS_info = findViewById(R.id.GPS_status);
        Gps droneGPS = this.drone.getAttribute(AttributeType.GPS);
        String fix = droneGPS.getFixStatus();
        GPS_info.setText(fix);
    }

    protected void updateGPSCount() {
        TextView Sat_cnt = findViewById(R.id.Sat_Count);
        Gps droneGPS = this.drone.getAttribute(AttributeType.GPS);
        int sat = droneGPS.getSatellitesCount();
        String sat_cnt = String.valueOf(sat) + " " + getApplicationContext().getString(R.string.Satellites);
        Sat_cnt.setText(sat_cnt);
    }

    protected void updateBattery() {
        TextView batteryVoltage = findViewById(R.id.bat_voltage);
        TextView batteryVoltageCell = findViewById(R.id.bat_voltage_cell);
        TextView batteryLevel = findViewById(R.id.bat_percent);
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        double bv = droneBattery.getBatteryVoltage();
        DecimalFormat df_bv = new DecimalFormat("#.##");
        batteryVoltage.setText(df_bv.format(bv));
        int cell = 1;
        if (bv > 13) {
            cell = 4;
        } else if(bv > 9) {
            cell = 3;
        } else if(bv > 5) {
            cell = 2;
        }
        double bv_c = bv/cell;
        DecimalFormat df_bv_c = new DecimalFormat("#.##");
        batteryVoltageCell.setText(df_bv_c.format(bv_c));

        int percent = (int) (100*(1 - (4.2 - bv_c)/0.5));
        if (percent > 100) {
            percent = 100;
        } else if (percent < 0 ) {
            percent = 0;
        }
        batteryLevel.setText(String.valueOf(percent));
    }
}
