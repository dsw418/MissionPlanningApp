package com.example.missionplanningapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Log tag
    private static final String LOG = DatabaseHelper.class.getName();

    // Database Version
    private static final int DATABASE_VERSION = 2;

    // Database Name
    private static final String DATABASE_NAME = "Missions";

    // Table Name
    private static final String TABLE_CurrentMission = "CurrentMission";
    private static final String TABLE_Buidling = "Building";

    // Column Names
    private static final String KEY_WaypointCount = "WaypointCount";
    private static final String KEY_Lat = "Lat";
    private static final String KEY_Lng = "Lng";
    private static final String KEY_Alt = "Alt";

    private static final String KEY_Boundary = "Boundary";
    private static final String KEY_Height = "Height";

    //Table Create Statements
    private static final String CREATE_TABLE_CurrentMission = "CREATE TABLE " + TABLE_CurrentMission
            + "(" + KEY_WaypointCount + " INTEGER PRIMARY KEY," + KEY_Lat + " REAL," + KEY_Lng
            + " REAL," + KEY_Alt + " REAL" + ")";

    private static final String CREATE_TABLE_Building = "CREATE TABLE " + TABLE_Buidling
            + "(" + KEY_Boundary + " TEXT," + KEY_Height + " REAL" + ")";


    public DatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //creating required tables
        db.execSQL(CREATE_TABLE_CurrentMission);
        db.execSQL(CREATE_TABLE_Building);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CurrentMission);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_Buidling);

        // create new tables
        onCreate(db);
    }

    // Creating a Current Mission Entry
    public void createCurrentMissionData (ArrayList<Waypoints> WpList) {
        SQLiteDatabase db = this.getWritableDatabase();

        for(int count = 0;count<WpList.size();count++) {
            ContentValues values = new ContentValues();
            Waypoints WP = WpList.get(count);
            values.put(KEY_WaypointCount, count+1);
            values.put(KEY_Lat, WP.getLat());
            values.put(KEY_Lng, WP.getLng());
            values.put(KEY_Alt, WP.getalt());
            db.insert(TABLE_CurrentMission, null, values);
        }
        db.close();

    }

/*    public long createCurrentMissionData (Waypoints WP) {
        SQLiteDatabase db = this.getWritableDatabase();

            ContentValues values = new ContentValues();
            values.put(KEY_WaypointCount, 1);
            values.put(KEY_Lat, WP.getLat());
            values.put(KEY_Lng, WP.getLng());
            values.put(KEY_Alt, WP.getalt());
            long id = db.insert(TABLE_CurrentMission, null, values);

        db.close();
        return id;
    }*/


    // Getting Current Mission Data
    public ArrayList<Waypoints> getCurrentMissionData() {
        ArrayList<Waypoints> WpList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CurrentMission;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if(c.moveToFirst()) {
            do {
                Waypoints wp = new Waypoints();
                wp.setLat(c.getDouble(c.getColumnIndex(KEY_Lat)));
                wp.setLng(c.getDouble(c.getColumnIndex(KEY_Lng)));
                wp.setalt(c.getDouble(c.getColumnIndex(KEY_Alt)));

                // adding to list
                WpList.add(wp);
            } while (c.moveToNext());
        }
        c.close();

        return WpList;
    }

    // Delete Current Mission
    public void deleteCurrentMission() {
        SQLiteDatabase db = this.getWritableDatabase();

        // delete
        db.delete(TABLE_CurrentMission, null, null);
    }

  /*      String cnt = "SELECT count(*) FROM TABLE_MostRecentlySavedMission";
        Cursor mcursor = db.rawQuery(cnt, null);
        mcursor.moveToFirst();
        int icnt = mcursor.getInt(0);

        if (icnt > 0) {
            db.delete(TABLE_MostRecentlySavedMission, null, null);
        }*/

    // Creating a Building Entry
    public void createBuilding (Building building) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_Boundary, building.getBoundary());
        values.put(KEY_Height, building.getBuildingHeight());
        db.insert(TABLE_Buidling, null, values);
        db.close();
    }

    // Getting Building Data
    public Building getBuildingData() {
        Building building = new Building();
        String selectQuery = "SELECT * FROM " + TABLE_Buidling;

        Log.e(LOG, selectQuery);

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery(selectQuery, null);

        if (c != null) {
            c.moveToFirst();

            building.setBoundary(c.getString(c.getColumnIndex(KEY_Boundary)));
            building.setBuildingHeight(c.getDouble(c.getColumnIndex(KEY_Height)));
            c.close();
        }

        return building;
    }

    // Delete Building
    public void deleteBuilding() {
        SQLiteDatabase db = this.getWritableDatabase();

        // delete
        db.delete(TABLE_Buidling, null, null);
    }
}
