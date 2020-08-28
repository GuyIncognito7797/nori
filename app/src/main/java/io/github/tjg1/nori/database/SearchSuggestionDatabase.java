/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.database;

import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import io.github.tjg1.nori.R;

/**
 * Backing store for the custom search suggestions in {@link io.github.tjg1.nori.SearchActivity}.
 * It gets pre-populated with the 1000 most popular tags on Safebooru.org when the database is first created.
 * It also stores and suggests queries searched  previously by the user that are not part of the Safebooru data set.
 */
public class SearchSuggestionDatabase extends SQLiteOpenHelper {

    //region SQLite Constants
    /**
     * Search suggestion table name.
     */
    public static final String TABLE_NAME = "search_suggestions";
    /**
     * Unique ID (primary key) column.
     */
    public static final String COLUMN_ID = "_id";
    /**
     * Tag name column. These values are presented as search suggestions.
     */
    public static final String COLUMN_NAME = SearchManager.SUGGEST_COLUMN_TEXT_1;
    /**
     * Column holding the resource ID of the icon displayed next to the suggestion to indicate its type (recent/Safebooru top 1000).
     */
    public static final String COLUMN_ICON = SearchManager.SUGGEST_COLUMN_ICON_1;
    /**
     * Filename of the underlying SQLite database.
     */
    private static final String DATABASE_NAME = "search_suggestions.db";
    /**
     * Resource ID of the icon used to represent recent search history items.
     */
    private static final String RESOURCE_ICON_RECENT_HISTORY = Integer.toString(R.drawable.ic_search_suggestion_recent);
    /**
     * Resource ID of the icon used to represent suggestions from the built-in tag data set
     */
    private static final String RESOURCE_ICON_BUILT_IN = Integer.toString(R.drawable.ic_search_suggestion_builtin);
    /**
     * Database schema version.
     */
    private static final int SCHEMA_VERSION = 1;
    //endregion

    //region Instance fields (Context)
    /**
     * Android activity context.
     */
    private final Context context;
    //endregion

    //region Constructors
    public SearchSuggestionDatabase(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA_VERSION);
        this.context = context;
    }
    //endregion

    //region SQLiteOpenHelper methods
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Execute query to create the table schema.
        db.execSQL(String.format(Locale.US, "CREATE TABLE %s (%s INTEGER PRIMARY KEY AUTOINCREMENT, %s TEXT NOT NULL UNIQUE ON CONFLICT IGNORE, %s TEXT);",
                TABLE_NAME, COLUMN_ID, COLUMN_NAME, COLUMN_ICON));

        try {
            // Pre-populate the database with the Safebooru.org Top 1000 tags data set.
            // Open the file containing the tag data set from app assets.
            BufferedReader in = new BufferedReader(new InputStreamReader(context.getAssets().open("tags.txt")));
            String line;

            // Insert each line into the database.
            while ((line = in.readLine()) != null) {
                db.execSQL(String.format(Locale.US, "INSERT INTO %s (%s, %s) VALUES (?, ?);",
                        TABLE_NAME, COLUMN_NAME, COLUMN_ICON),
                        new String[]{line, RESOURCE_ICON_BUILT_IN});
            }

            // Close the file.
            in.close();
        } catch (IOException ignored) {
            // Too bad :(
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do nothing.
    }
    //endregion

    //region CRUD methods

    /**
     * Insert a new search history item into the search suggestion database.
     *
     * @param tag Name of the tag to be added into the database.
     * @return ID of the newly inserted row.
     */
    public long insert(String tag) {
        // Don't add queries shorter than 3 characters,
        // since that's the minimum threshold at which the suggestion dropdown is shown.
        if (tag.length() < 3) {
            return -1;
        }

        // Get a writable instance of the database.
        SQLiteDatabase db = getWritableDatabase();

        // Convert the tag into a ContentValues object.
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, tag);
        values.put(COLUMN_ICON, RESOURCE_ICON_RECENT_HISTORY);

        // Insert the tag into the database.
        long id = db.insert(TABLE_NAME, null, values);

        // Close the database and return id of the newly created row.
        db.close();
        return id;
    }

    /**
     * Remove all search history entries from the database. This does not affect the built-in Safebooru.org tag data set.
     *
     * @return Number of database rows removed.
     */
    public int eraseSearchHistory() {
        // Get a writable instance of the database.
        SQLiteDatabase db = getWritableDatabase();

        // Remove search history entries from the database.
        int rows = db.delete(TABLE_NAME, COLUMN_ICON + " = ?", new String[]{RESOURCE_ICON_RECENT_HISTORY});

        // Close the database and return the number of affected rows.
        db.close();
        return rows;
    }
    //endregion
}
