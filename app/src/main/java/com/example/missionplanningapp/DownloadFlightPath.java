package com.example.missionplanningapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class DownloadFlightPath extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 42;

    private ArrayList<Waypoints> WpList = new ArrayList<>();

    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download_flight_path);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Flight Path Download");
        }
        mToolbar.setTitleTextColor(Color.WHITE);

        db = new DatabaseHelper(getApplicationContext());
    }

    public void loadFile(View view) {
        performFileSearch();
    }

    public void saveMissionPlan (View view) {
        if (WpList.size()>0){
            //db.createMostRecentlySavedMissionData(WpList);
            db.deleteCurrentMission();
            db.createCurrentMissionData(WpList);
            this.finish();
        }
        else {
            AlertDialog.Builder accept = new AlertDialog.Builder(DownloadFlightPath.this);
            accept.setTitle("No Mission Data")
                    .setMessage("No mission file has been selected")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = accept.create();
            alert.show();
        }
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    public void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");

        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                //Log.i(TAG, "Uri: " + uri.toString());
                //readWaypoints(uri);

                try {
                    WpList = readWaypoints(uri);
                    //String string = readTextFromUri(uri);
                    //Toast.makeText(getApplicationContext(), "read", Toast.LENGTH_SHORT).show();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private ArrayList<Waypoints> readWaypoints(Uri uri) throws IOException {
        ArrayList<Waypoints> WpList = new ArrayList<>();
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    inputStream));
            int line_count = 0;
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    if (line_count > 1) {
                        String[] split = line.split("\\s+");
                        Waypoints wp = new Waypoints();
                        wp.setLat(Double.valueOf(split[8]));
                        wp.setLng(Double.valueOf(split[9]));
                        wp.setalt(Double.valueOf(split[10]));
                        WpList.add(wp);
                    }
                    line_count++;
                }
            } catch(IndexOutOfBoundsException e) {
                Toast.makeText(getApplicationContext(), "Failed to read. Improper file format.", Toast.LENGTH_SHORT).show();
            }
        }

        return WpList;
    }
}
