/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.database;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import androidx.loader.content.AsyncTaskLoader;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.tjg1.library.norilib.clients.Flickr;
import io.github.tjg1.library.norilib.clients.SearchClient;

/**
 * Utility class providing access to the SQLite API endpoint settings database.
 */
public class APISettingsDatabase extends SQLiteOpenHelper {

    //region Broadcast IDs
    /**
     * ID of the Broadcast sent when data in the database changes.
     */
    public static final String BROADCAST_UPDATE = "io.github.tjg1.nori.database.APISettingsDatabase.update";
    //endregion

    //region SQLite Constants
    /**
     * Filename of the underlying SQLite database.
     */
    private static final String DATABASE_NAME = "api_settings.db";
    /**
     * API Settings table name.
     */
    private static final String TABLE_NAME = "api_settings";
    /**
     * Unique ID (primary key) column.
     */
    private static final String COLUMN_ID = "_id";
    /**
     * Human-readable endpoint name column.
     */
    private static final String COLUMN_NAME = "name";
    /**
     * API Type ID column.
     */
    private static final String COLUMN_TYPE = "type";
    /**
     * API endpoint URL column.
     */
    private static final String COLUMN_ENDPOINT_URL = "endpoint_url";
    /**
     * Username column.
     */
    private static final String COLUMN_USERNAME = "username";
    /**
     * Password/API key column.
     */
    private static final String COLUMN_PASSPHRASE = "passphrase";
    /**
     * Database schema version.
     */
    private static final int SCHEMA_VERSION = 1;
    //endregion

    //region Instance fields (Context)
    /**
     * Android context.
     */
    private final Context context;
    //endregion

    //region Constructors

    /**
     * Create a new API Settings Database access helper.
     *
     * @param context Android context.
     */
    public APISettingsDatabase(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA_VERSION);
        this.context = context;
    }
    //endregion

    //region SQLiteOpenHelper methods
    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL query used to create the database schema.
        String createSQL = String.format(Locale.US,
                "CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT NOT NULL, %s INTEGER NOT NULL, %s TEXT NOT NULL, %s TEXT, %s TEXT);",
                TABLE_NAME, COLUMN_ID, COLUMN_NAME, COLUMN_TYPE, COLUMN_ENDPOINT_URL, COLUMN_USERNAME, COLUMN_PASSPHRASE);
        db.execSQL(createSQL);

        // SQL query used to populate the database with initial data (when the app is first launched).
        String populateSQL = String.format(Locale.US,
                "INSERT INTO %s (%s, %s, %s) VALUES ('%s', %d, '%s');",
                TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, COLUMN_ENDPOINT_URL, "Flickr", SearchClient.Settings.APIType.FLICKR.ordinal(), Flickr.FLICKR_API_ENDPOINT);
        db.execSQL(populateSQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do nothing.
    }
    //endregion

    //region Static methods: (De)-Serialization of SearchClient.Settings

    /**
     * Serialize a {@link SearchClient.Settings} object into a {@link ContentValues} object used by
     * Android's SQLite API.
     *
     * @param settings Settings object.
     * @return Serialized {@link ContentValues} object.
     */
    private static ContentValues searchClientSettingsToContentValues(SearchClient.Settings settings) {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TYPE, settings.getApiType().ordinal());
        contentValues.put(COLUMN_NAME, settings.getName());
        contentValues.put(COLUMN_ENDPOINT_URL, settings.getEndpoint());
        contentValues.put(COLUMN_USERNAME, settings.getUsername());
        contentValues.put(COLUMN_PASSPHRASE, settings.getPassword());
        return contentValues;
    }

    /**
     * Convert data from a database cursor to a {@link SearchClient.Settings} object.
     *
     * @param c Cursor.
     * @return {@link SearchClient.Settings} object.
     */
    private static SearchClient.Settings cursorToSearchClientSettings(Cursor c) {
        return new SearchClient.Settings(
                SearchClient.Settings.APIType.values()[c.getInt(c.getColumnIndex(COLUMN_TYPE))],
                c.getString(c.getColumnIndex(COLUMN_NAME)),
                c.getString(c.getColumnIndex(COLUMN_ENDPOINT_URL)),
                c.getString(c.getColumnIndex(COLUMN_USERNAME)),
                c.getString(c.getColumnIndex(COLUMN_PASSPHRASE)));
    }
    //endregion

    //region CRUD methods

    /**
     * Get single instance of {@link SearchClient.Settings} from the database.
     *
     * @param id Row ID.
     * @return Search client settings object. Null if given ID does not exist in the database.
     */
    public SearchClient.Settings get(long id) {
        // Query the database.
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, null, COLUMN_ID + " = ?", new String[]{Long.toString(id)}, null, null, COLUMN_ID, "1");

        // Prepare return value.
        final SearchClient.Settings settings = (!c.moveToNext()) ? null : cursorToSearchClientSettings(c);

        // Clean up native resources.
        c.close();
        db.close();

        return settings;
    }

    /**
     * Get all {@link SearchClient.Settings} objects from the database.
     *
     * @return List of pairs mapping database IDs to {@link SearchClient.Settings} objects.
     */
    public List<Pair<Integer, SearchClient.Settings>> getAll() {
        // Query the database.
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_ID);

        // Convert database Cursor to List.
        final List<Pair<Integer, SearchClient.Settings>> settingsList = new ArrayList<>(c.getCount());
        while (c.moveToNext()) {
            settingsList.add(new Pair<>(c.getInt(c.getColumnIndex(COLUMN_ID)), cursorToSearchClientSettings(c)));
        }

        // Clean up native resources.
        c.close();
        db.close();

        return settingsList;
    }

    /**
     * Insert a new {@link SearchClient.Settings} into the database.
     *
     * @param settings Settings object.
     * @return ID of the newly inserted row.
     */
    public long insert(SearchClient.Settings settings) {
        // Insert data into the database.
        SQLiteDatabase db = getWritableDatabase();
        final long id = db.insert(TABLE_NAME, null, searchClientSettingsToContentValues(settings));
        db.close();

        sendUpdateNotification();
        return id;
    }

    /**
     * Update an existing {@link SearchClient.Settings} object in the database.
     *
     * @param id       Row ID.
     * @param settings Settings object with data to update.
     * @return Number of rows affected.
     */
    public int update(long id, SearchClient.Settings settings) {
        // Update data in the database.
        SQLiteDatabase db = getWritableDatabase();
        final int rows = db.update(TABLE_NAME, searchClientSettingsToContentValues(settings), COLUMN_ID + " = ?",
                new String[]{Long.toString(id)});
        db.close();

        sendUpdateNotification();
        return rows;
    }

    /**
     * Delete a row from the database.
     *
     * @param id Row ID.
     * @return Number of rows affected.
     */
    public int delete(long id) {
        // Remove row from the database.
        SQLiteDatabase db = getWritableDatabase();
        final int rows = db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{Long.toString(id)});
        db.close();

        sendUpdateNotification();
        return rows;
    }
    //endregion

    //region Update notification broadcasts

    /**
     * Notify observers that the data in the database has changed.
     */
    private void sendUpdateNotification() {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(BROADCAST_UPDATE));
    }
    //endregion

    //region Inner classes

    /**
     * Loader class used to asynchronously offload database access to a background thread.
     */
    public static class Loader extends AsyncTaskLoader<List<Pair<Integer, SearchClient.Settings>>> {
        /**
         * {@link android.content.BroadcastReceiver} receiving database change notifications.
         */
        private final BroadcastReceiver contentChangedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Loader.this.onContentChanged();
            }
        };
        /**
         * Database access helper.
         */
        private APISettingsDatabase db;
        /**
         * Cached result.
         */
        private List<Pair<Integer, SearchClient.Settings>> settingsList;

        /**
         * Create a new loader to asynchronously offloads {@link APISettingsDatabase} access to a background thread.
         *
         * @param context Android context.
         */
        public Loader(Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            super.onStartLoading();

            // Create database instance.
            this.db = new APISettingsDatabase(getContext());

            // If there is a cached result available, deliver it immediately.
            if (settingsList != null) {
                deliverResult(settingsList);
            }

            // Register broadcast receiver handling database change notifications.
            LocalBroadcastManager.getInstance(getContext()).registerReceiver(contentChangedBroadcastReceiver,
                    new IntentFilter(APISettingsDatabase.BROADCAST_UPDATE));

            // If the data has changed since the last time it was loaded or is not currently available, start a load.
            if (takeContentChanged() || settingsList == null) {
                forceLoad();
            }
        }

        @Override
        protected void onReset() {
            super.onReset();

            // Release resources.
            settingsList = null;
            db.close();

            // Unregister broadcast receiver handling database change notifications.
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(contentChangedBroadcastReceiver);
        }

        @Override
        public List<Pair<Integer, SearchClient.Settings>> loadInBackground() {
            settingsList = db.getAll();
            return settingsList;
        }
    }
    //endregion
}
