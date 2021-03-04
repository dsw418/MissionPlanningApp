package com.example.missionplanningapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.CalibrationApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeEventExtra;
import com.o3dr.services.android.lib.drone.calibration.magnetometer.MagnetometerCalibrationProgress;
import com.o3dr.services.android.lib.drone.calibration.magnetometer.MagnetometerCalibrationResult;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

public class Calibration extends AppCompatActivity implements DroneListener, TowerListener {

    private static final String TAG = Calibration.class.getSimpleName();

    private ControlTower controlTower;
    private Drone drone;
    private final Handler handler = new Handler();

    private int selectedConnectionType;
    private int DEFAULT_USB_BAUD_RATE;
    private String tcpServerIP;
    private int tcpServerPort;
    private ConnectionParameter connectionParams;

    private int imu_step = 0;

    private TextView IMU_textView;
    private Button IMU_button;
    private ImageView IMU_image;

    private static final int MAX_PROGRESS = 100;

    private static final int STEP_BEGIN_CALIBRATION = 0;
    private static final int STEP_CALIBRATION_WAITING_TO_START = 1;
    private static final int STEP_CALIBRATION_STARTED = 2;
    private static final int STEP_CALIBRATION_SUCCESSFUL = 3;
    private static final int STEP_CALIBRATION_FAILED = 4;
    private static final int STEP_CALIBRATION_CANCELLED = 5;

    private int calibrationStep;

    private final SparseArray<MagCalibrationStatus> calibrationTracker = new SparseArray<>();

    private Button Mag_button;
    private TextView calibrationInstructions;
    private ProgressBar calibrationProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Calibration");
        }
        mToolbar.setTitleTextColor(Color.WHITE);

        // Initialize the service manager
        this.controlTower = new ControlTower(getApplicationContext());
        this.drone = new Drone(getApplicationContext());

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

        IMU_textView = findViewById(R.id.IMU_textView);
        IMU_button = findViewById(R.id.IMU_button);
        IMU_image = findViewById(R.id.IMU_image);
        calibrationProgress = findViewById(R.id.ProgressBar);
        Mag_button = findViewById(R.id.Compass_button);
        calibrationInstructions = findViewById(R.id.Compass_textView);
    }

    public void IMU_cal_button(View view) {
        processIMUCalibrationStep(imu_step);

    }

    public void Compass_cal_button(View view) {
        proceedWithCalibration(calibrationStep);
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
    public void onDroneEvent(String event, Bundle extras) {

        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                if (imu_step == 0) {
                    resetIMUCalibration();
                    IMU_button.setEnabled(true);
                }
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                IMU_button.setEnabled(false);
                resetIMUCalibration();
                cancelCalibration();
                alertUser("Drone Disconnected");
                alertUser("Attempting to Reconnect");
                attemptReconnect();
                break;

            case AttributeEvent.CALIBRATION_IMU:
                String message = extras.getString(AttributeEventExtra.EXTRA_CALIBRATION_IMU_MESSAGE);
                if (message != null) {
                    processIMUMAVMessage(message);
                }
                break;

            case AttributeEvent.CALIBRATION_IMU_TIMEOUT:
                if(this.drone.isConnected()) {
                    String message_imu = extras.getString(AttributeEventExtra.EXTRA_CALIBRATION_IMU_MESSAGE);
                    if (message_imu != null) {
                        alertUser(message_imu);
                    }
                }
                break;

            case AttributeEvent.CALIBRATION_MAG_CANCELLED:
                updateUI(STEP_CALIBRATION_CANCELLED);
                break;

            case AttributeEvent.CALIBRATION_MAG_COMPLETED:
                final MagnetometerCalibrationResult result = extras.getParcelable(AttributeEventExtra.EXTRA_CALIBRATION_MAG_RESULT);
                handleMagResult(result);
                break;

            case AttributeEvent.CALIBRATION_MAG_PROGRESS:
                final MagnetometerCalibrationProgress progress = extras.getParcelable(AttributeEventExtra.EXTRA_CALIBRATION_MAG_PROGRESS);
                handleMagProgress(progress);
                break;

            default:
                Log.i("DRONE_EVENT", event);
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

    public void attemptReconnect() {
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!Calibration.this.drone.isConnected()) {
                    Calibration.this.drone.connect(connectionParams, new LinkListener() {
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

    private void sendAck(int step) {
        if (this.drone.isConnected()) {
            CalibrationApi.getApi(this.drone).sendIMUAck(step);
        }
    }

    private void startIMUCalibration() {
        if (this.drone.isConnected()) {
            CalibrationApi.getApi(this.drone).startIMUCalibration(new SimpleCommandListener(){
                @Override
                public void onError(int error){
                    Toast.makeText(Calibration.this, R.string.imu_calibration_start_error, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void processIMUMAVMessage(String message) {
        if (message.contains("Place") || message.contains("Calibration")) {
            /*if(updateTime) {
                updateTimestamp = System.currentTimeMillis();
            }*/

            processOrientation(message);
        }
/*        else if (message.contains("Offsets")) {
            textViewOffset.setVisibility(View.VISIBLE);
            textViewOffset.setText(message);
        } else if (message.contains("Scaling")) {
            textViewScaling.setVisibility(View.VISIBLE);
            textViewScaling.setText(message);
        }*/
    }

    private void processOrientation(String message) {
        if (message.contains("level"))
            imu_step = 1;
        else if (message.contains("LEFT"))
            imu_step = 2;
        else if (message.contains("RIGHT"))
            imu_step = 3;
        else if (message.contains("DOWN"))
            imu_step = 4;
        else if (message.contains("UP"))
            imu_step = 5;
        else if (message.contains("BACK"))
            imu_step = 6;
        else if (message.contains("Calibration"))
            imu_step = 7;

        String msg = message.replace("any key.", "'Next'");

        IMU_textView.setText(msg);

        updateIMUDescription(imu_step);
    }

    private void processIMUCalibrationStep(int step) {
        if (step == 0) {
            startIMUCalibration();
            //updateTimestamp = System.currentTimeMillis();
        } else if (step > 0 && step < 7) {
            sendAck(step);
        } else {
            imu_step = 0;

            IMU_textView.setText(R.string.imu_start);

            //textViewOffset.setVisibility(View.INVISIBLE);
            //textViewScaling.setVisibility(View.INVISIBLE);

            updateIMUDescription(imu_step);
        }
    }

    public void updateIMUDescription(int calibration_step) {
        int id;
        int image_id = 0;
        switch (calibration_step) {
            case 0:
                id = R.string.setup_imu_start;
                break;
            case 1:
                id = R.string.setup_imu_normal;
                image_id = R.drawable.calibration_level;
                break;
            case 2:
                id = R.string.setup_imu_left;
                image_id = R.drawable.calibration_left_side;
                break;
            case 3:
                id = R.string.setup_imu_right;
                image_id = R.drawable.calibration_right_side;
                break;
            case 4:
                id = R.string.setup_imu_nosedown;
                image_id = R.drawable.calibration_nose_down;
                break;
            case 5:
                id = R.string.setup_imu_noseup;
                image_id = R.drawable.calibration_nose_up;
                break;
            case 6:
                id = R.string.setup_imu_back;
                image_id = R.drawable.calibration_top_down;
                break;
            case 7:
                id = R.string.setup_imu_completed;
                break;
            default:
                return;
        }

        IMU_textView.setText(id);

        if (calibration_step == 0) {
            IMU_button.setText(R.string.imu_but);
            IMU_image.setImageResource(android.R.color.transparent);
        }
        else if (calibration_step == 7) {
            IMU_button.setText(R.string.done);
            IMU_image.setImageResource(android.R.color.transparent);
        }
        else {
            IMU_button.setText(R.string.next);
            IMU_image.setImageResource(image_id);
        }

/*        if (calibration_step == 7 || calibration_step == 0) {
            handler.removeCallbacks(runnable);

            pbTimeOut.setVisibility(View.INVISIBLE);
            textViewTimeOut.setVisibility(View.INVISIBLE);
        } else {
            handler.removeCallbacks(runnable);

            textViewTimeOut.setVisibility(View.VISIBLE);
            pbTimeOut.setIndeterminate(true);
            pbTimeOut.setVisibility(View.VISIBLE);
            handler.postDelayed(runnable, UPDATE_TIMEOUT_PERIOD);
        }*/
    }

    private void resetIMUCalibration(){
        imu_step = 0;
        updateIMUDescription(imu_step);
    }

    private void handleMagProgress(MagnetometerCalibrationProgress progress) {
        if (progress == null)
            return;

        updateUI(STEP_CALIBRATION_STARTED);

        MagCalibrationStatus calStatus = calibrationTracker.get(progress.getCompassId());
        if (calStatus == null) {
            calStatus = new MagCalibrationStatus();
            calibrationTracker.append(progress.getCompassId(), calStatus);
        }

        calStatus.percentage = progress.getCompletionPercentage();

        int totalPercentage = 0;
        int calibrationsCount = calibrationTracker.size();
        for (int i = 0; i < calibrationsCount; i++) {
            totalPercentage += calibrationTracker.valueAt(i).percentage;
        }

        int calPercentage = calibrationsCount > 0 ? totalPercentage / calibrationsCount : 0;

        if (calibrationProgress.isIndeterminate()) {
            calibrationProgress.setIndeterminate(false);
            calibrationProgress.setMax(MAX_PROGRESS);
            calibrationProgress.setProgress(0);
        }

        if (calibrationProgress.getProgress() < calPercentage) {
            calibrationProgress.setProgress(calPercentage);
        }
    }

    private void handleMagResult(MagnetometerCalibrationResult result) {
        if (result == null)
            return;

        MagCalibrationStatus reportStatus = calibrationTracker.get(result.getCompassId());
        if (reportStatus == null) {
            return;
        }

        reportStatus.percentage = 100;
        reportStatus.isComplete = true;
        reportStatus.isSuccessful = result.isCalibrationSuccessful();

        boolean areCalibrationsComplete = true;
        boolean areCalibrationsSuccessful = true;
        for (int i = 0; i < calibrationTracker.size(); i++) {
            final MagCalibrationStatus calStatus = calibrationTracker.valueAt(i);
            areCalibrationsComplete = areCalibrationsComplete && calStatus.isComplete;
            areCalibrationsSuccessful = areCalibrationsSuccessful && calStatus.isSuccessful;
        }

        if (areCalibrationsComplete) {
            if (areCalibrationsSuccessful)
                updateUI(STEP_CALIBRATION_SUCCESSFUL);
            else {
                updateUI(STEP_CALIBRATION_FAILED);
            }

            if (this.drone != null){
                CalibrationApi.getApi(this.drone).acceptMagnetometerCalibration();
            }
        }
    }

    private void cancelCalibration() {
        if (this.drone != null){
            CalibrationApi.getApi(this.drone).cancelMagnetometerCalibration();
        }
    }

    private void proceedWithCalibration( int step) {
        if(this.drone == null || !this.drone.isConnected()){
            Toast.makeText(getApplicationContext(), "Please connect drone before proceeding.", Toast.LENGTH_LONG).show();
            return;
        }

        switch (step) {
            case STEP_BEGIN_CALIBRATION:
            case STEP_CALIBRATION_FAILED:
            case STEP_CALIBRATION_CANCELLED:
                startCalibration();
                break;

            case STEP_CALIBRATION_SUCCESSFUL:
                this.finish();
                break;

            case STEP_CALIBRATION_STARTED:
            case STEP_CALIBRATION_WAITING_TO_START:
            default:
                //nothing to do
                break;
        }
    }

    private void startCalibration() {
        CalibrationApi.getApi(this.drone).startMagnetometerCalibration(false, false, 5);
        updateUI(STEP_CALIBRATION_WAITING_TO_START, true);
    }

    private void updateUI(int step) {
        updateUI(step, false);
    }

    private void updateUI(int step, boolean force) {
        if (!force && step <= calibrationStep)
            return;

        calibrationStep = step;

        switch (step) {
            case STEP_BEGIN_CALIBRATION:
            case STEP_CALIBRATION_CANCELLED:

                calibrationProgress.setVisibility(View.INVISIBLE);

                calibrationInstructions.setVisibility(View.VISIBLE);
                calibrationInstructions.setText(R.string.instruction_compass_begin_calibration);


                Mag_button.setVisibility(View.VISIBLE);

                break;

            case STEP_CALIBRATION_STARTED:

            case STEP_CALIBRATION_WAITING_TO_START:
                calibrationTracker.clear();

                calibrationProgress.setVisibility(View.VISIBLE);
                calibrationProgress.setProgress(0);
                calibrationProgress.setIndeterminate(true);

                calibrationInstructions.setVisibility(View.GONE);

                Mag_button.setVisibility(View.GONE);

                break;

            case STEP_CALIBRATION_SUCCESSFUL:

                calibrationProgress.setVisibility(View.VISIBLE);
                calibrationProgress.setIndeterminate(false);
                calibrationProgress.setMax(MAX_PROGRESS);
                calibrationProgress.setProgress(MAX_PROGRESS);

                calibrationInstructions.setVisibility(View.VISIBLE);
                calibrationInstructions.setText(R.string.label_success);

                Mag_button.setVisibility(View.VISIBLE);
                Mag_button.setText(R.string.label_ready_to_fly);

                AlertDialog.Builder powerCycle = new AlertDialog.Builder(Calibration.this);
                powerCycle.setTitle("Please Power Cycle the UAV")
                        .setMessage("To complete the calibration please restart the UAV.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = powerCycle.create();
                alert.show();

                break;

            case STEP_CALIBRATION_FAILED:

                calibrationProgress.setVisibility(View.VISIBLE);
                calibrationProgress.setIndeterminate(false);
                calibrationProgress.setMax(MAX_PROGRESS);
                calibrationProgress.setProgress(MAX_PROGRESS);

                calibrationInstructions.setVisibility(View.VISIBLE);
                calibrationInstructions.setText(R.string.label_compass_calibration_failed);

                Mag_button.setVisibility(View.VISIBLE);
                Mag_button.setText(R.string.try_again);

                break;
        }
    }


    private static class MagCalibrationStatus {
        int percentage;
        boolean isComplete;
        boolean isSuccessful;
    }
}