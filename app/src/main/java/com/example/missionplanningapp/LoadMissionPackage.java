package com.example.missionplanningapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.ArrayList;

public class LoadMissionPackage extends AppCompatActivity {

    DatabaseHelper db;
    ArrayList<Waypoints> WpList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_mission_package);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Load Mission Package");
        }
        mToolbar.setTitleTextColor(Color.WHITE);

        db = new DatabaseHelper(getApplicationContext());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT > 22) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }

        }
    }

    public void openLoadBuilding(View view) {
        Intent intent = new Intent(this, LoadBuilding.class);
        startActivity(intent);
    }

    public void openEditBuilding(View view) {
        Intent intent = new Intent(this, EditBuilding.class);
        startActivity(intent);
    }

    public void openLoadFlightPath(View view) {
        Intent intent = new Intent(this, LoadFlightPath.class);
        startActivity(intent);
    }

    public void openEditFlightPath(View view) {
        WpList = db.getCurrentMissionData();
        if (WpList.size() > 0) {
            Intent intent = new Intent(this, VerifyFlightPath.class);
            startActivity(intent);
        } else {
            AlertDialog.Builder accept = new AlertDialog.Builder(LoadMissionPackage.this);
            accept.setTitle("No Mission Data")
                    .setMessage("Please Create a Mission First")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = accept.create();
            alert.show();
        }
    }

    public void openUploadMission(View view) {
        WpList = db.getCurrentMissionData();
        if (WpList.size() > 0) {
            Intent intent = new Intent(this, UploadMission.class);
            startActivity(intent);
        } else {
            AlertDialog.Builder accept = new AlertDialog.Builder(LoadMissionPackage.this);
            accept.setTitle("No Mission Data")
                    .setMessage("Please Create a Mission First")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = accept.create();
            alert.show();
        }
    }
}
