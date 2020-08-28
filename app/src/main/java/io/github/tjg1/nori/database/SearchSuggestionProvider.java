/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.database;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Locale;

import static io.github.tjg1.nori.database.SearchSuggestionDatabase.COLUMN_ICON;
import static io.github.tjg1.nori.database.SearchSuggestionDatabase.COLUMN_ID;
import static io.github.tjg1.nori.database.SearchSuggestionDatabase.COLUMN_NAME;
import static io.github.tjg1.nori.database.SearchSuggestionDatabase.TABLE_NAME;

;

public class SearchSuggestionProvider extends ContentProvider {

    //region ContentProvider constants
    /**
     * Uri path ID for queried suggestions data.
     * This is the path that the search manager will use when querying your content provider for suggestions data based on user input (e.g. looking for partial matches).
     */
    private static final int SEARCH_SUGGEST = 0;
    /**
     * Uri path ID for shortcut validation.
     * This is the path that the search manager will use when querying your content provider to refresh a shortcutted suggestion result and to check if it is still valid.
     * When asked, a source may return an up to date result, or no result.
     * No result indicates the shortcut refers to a no longer valid suggestion.
     */
    private static final int SHORTCUT_REFRESH = 1;
    /**
     * URI parser used to match content provider paths.
     */
    private static final UriMatcher sURIMatcher;
    /**
     * Columns to include in queries to the underlying SQLite database.
     */
    private static final String[] COLUMNS = {
            COLUMN_ID,
            COLUMN_NAME,
            COLUMN_ICON
    };
    /**
     * Content provider authority. (Unique ID)
     */
    public static String AUTHORITY = "io.github.tjg1.nori.SearchSuggestionProvider";

    static {
        // Set up the parser used to match ContentProvider URIs.
        sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        sURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        sURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT, SHORTCUT_REFRESH);
        sURIMatcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", SHORTCUT_REFRESH);
    }
    //endregion

    //region Instance fields (Database)
    /**
     * Read-only instance of the SQLite database.
     */
    private SQLiteDatabase db;
    //endregion

    //region ContentProvider methods
    @Override
    public boolean onCreate() {
        // Open the search suggestion SQLite database in read-only mode.
        SearchSuggestionDatabase dbHelper = new SearchSuggestionDatabase(getContext());
        db = dbHelper.getReadableDatabase();

        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Match the content URI to decide which type of query to run.
        switch (sURIMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                String query = null;
                if (uri.getPathSegments().size() > 1) {
                    query = uri.getLastPathSegment().toLowerCase(Locale.US);
                }
                return getSuggestions(query);
            case SHORTCUT_REFRESH:
                // This is not implemented since the SUGGEST_COLUMN_SHORTCUT_ID column is not defined.
                // It's only useful when providing suggestions for the Quick Search Box (search from the launch screen).
                return null;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public String getType(@NonNull Uri uri) {
        // Match URI and return the appropriate mime type.
        switch (sURIMatcher.match(uri)) {
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case SHORTCUT_REFRESH:
                return SearchManager.SHORTCUT_MIME_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues contentValues) {
        // Use SearchSuggestionDatabase methods instead.
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(@NonNull Uri uri, String s, String[] strings) {
        // Use SearchSuggestionDatabase methods instead.
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues contentValues, String s, String[] strings) {
        // Use SearchSuggestionDatabase methods instead.
        throw new UnsupportedOperationException();
    }
    //endregion

    //region Get suggestions cursor from query

    /**
     * Get tag suggestions from the underlying SQLite database.
     *
     * @param query Query the database for tags starting with this substring.
     * @return Database cursor with returned suggestion.
     */
    private Cursor getSuggestions(String query) {
        // Query the database for search suggestions.
        if (query == null) {
            return db.query(TABLE_NAME, COLUMNS, null, null, null, null, COLUMN_ID + " DESC");
        } else {
            return db.query(TABLE_NAME, COLUMNS, COLUMN_NAME + " LIKE ?", new String[]{query + "%"}, null, null, COLUMN_ID + " DESC");
        }
    }
    //endregion
}
