/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.test.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;

import io.github.tjg1.nori.BuildConfig;
import io.github.tjg1.nori.R;
import io.github.tjg1.nori.database.SearchSuggestionDatabase;

import static io.github.tjg1.nori.database.SearchSuggestionDatabase.COLUMN_ICON;
import static io.github.tjg1.nori.database.SearchSuggestionDatabase.COLUMN_ID;
import static io.github.tjg1.nori.database.SearchSuggestionDatabase.TABLE_NAME;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Tests the {@link SearchSuggestionDatabase} class.
 */
public class SearchSuggestionDatabaseTest extends InstrumentationTestCase {
    /**
     * App context used for testing.
     */
    private Context context;

    @Override
    protected void setUp() throws Exception {
        // Set up a new app context before each test.
        context = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "_test");
    }

    /**
     * Tests if the database is created and pre-populated with the Safebooru Top 1000 data set.
     */
    public void testDatabaseCreation() throws Throwable {
        // Get a read-only instance of the database.
        SearchSuggestionDatabase searchSuggestionDatabase = new SearchSuggestionDatabase(context);
        SQLiteDatabase db = searchSuggestionDatabase.getReadableDatabase();

        // Get all suggestions added from the Safebooru Top 1000 data set from the database.
        Cursor c = db.query(TABLE_NAME, null, COLUMN_ICON + " = " + Integer.toString(R.drawable.ic_search_suggestion_builtin), null, null, null, COLUMN_ID);

        // Number of returned rows should match the number of lines in the "tags.txt" asset file.
        if (!"google".equals(BuildConfig.FLAVOR)) {
            assertThat(c.getCount()).isEqualTo(962);
        } else {
            assertThat(c.getCount()).isEqualTo(0);
        }

        // Clean-up native resources.
        c.close();
        db.close();
    }
}
