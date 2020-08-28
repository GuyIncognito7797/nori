/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.test.database;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.test.InstrumentationTestCase;
import android.test.RenamingDelegatingContext;
import android.util.Pair;

import androidx.core.content.LocalBroadcastManager;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.github.tjg1.library.norilib.clients.SearchClient;
import io.github.tjg1.nori.database.APISettingsDatabase;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Tests the {@link io.github.tjg1.nori.database.APISettingsDatabase} class.
 */
public class APISettingsDatabaseTest extends InstrumentationTestCase {
    private Context context;

    @Override
    protected void setUp() {
        context = new RenamingDelegatingContext(getInstrumentation().getTargetContext(), "_test");
    }

    /**
     * Test if the database is correctly initialized and pre-populated with data.
     */
    public void testDatabaseCreation() {
        APISettingsDatabase database = new APISettingsDatabase(context);
        List<Pair<Integer, SearchClient.Settings>> settingsList = database.getAll();
        database.close();

        // The list should contain exactly one object.
        assertThat(settingsList).hasSize(1);
        // The database should contain Safebooru API settings by default.
        SearchClient.Settings settings = settingsList.get(0).second;
        assertThat(settings).isNotNull();
        assertThat(settings.getApiType()).isEqualTo(SearchClient.Settings.APIType.FLICKR);
        assertThat(settings.getName()).isEqualTo("Flickr");
        assertThat(settings.getEndpoint()).isEqualTo("https://api.flickr.com/services/rest");
        assertThat(settings.getUsername()).isNull();
        assertThat(settings.getPassword()).isNull();
    }

    /**
     * Test the {@link io.github.tjg1.nori.database.APISettingsDatabase#getAll()} method.
     */
    public void testGetAll() {
        APISettingsDatabase database = new APISettingsDatabase(context);
        List<Pair<Integer, SearchClient.Settings>> settingsList = database.getAll();
        database.close();

        // The list should not be null or empty.
        assertThat(settingsList).isNotNull().isNotEmpty();
    }

    /**
     * Test the {@link io.github.tjg1.nori.database.APISettingsDatabase#get(long)} method.
     */
    public void testGet() {
        APISettingsDatabase database = new APISettingsDatabase(context);
        SearchClient.Settings settings = database.get(1);
        database.close();

        // The settings object should not be null.
        assertThat(settings).isNotNull();
    }

    /**
     * Test the {@link io.github.tjg1.nori.database.APISettingsDatabase#insert(io.github.tjg1.library.norilib.clients.SearchClient.Settings)} method.
     */
    public void testInsert() {
        APISettingsDatabase database = new APISettingsDatabase(context);
        // Insert new row into the database.
        long rowID = database.insert(new SearchClient.Settings(SearchClient.Settings.APIType.DANBOARD,
                "Danbooru", "http://danbooru.donmai.us"));

        // Now get row from database and verify the data.
        SearchClient.Settings settings = database.get(rowID);
        database.close();
        assertThat(settings.getApiType()).isEqualTo(SearchClient.Settings.APIType.DANBOARD);
        assertThat(settings.getName()).isEqualTo("Danbooru");
        assertThat(settings.getEndpoint()).isEqualTo("http://danbooru.donmai.us");
        assertThat(settings.getUsername()).isNull();
        assertThat(settings.getPassword()).isNull();
    }

    /**
     * Test the {@link io.github.tjg1.nori.database.APISettingsDatabase#update(long, io.github.tjg1.library.norilib.clients.SearchClient.Settings)} method.
     */
    public void testUpdate() {
        APISettingsDatabase database = new APISettingsDatabase(context);

        // Insert new row into the database.
        long rowID = database.insert(new SearchClient.Settings(SearchClient.Settings.APIType.DANBOARD,
                "Danbooru", "http://danbooru.donmai.us"));

        // Update the newly created row.
        int rowsAffected = database.update(rowID, new SearchClient.Settings(SearchClient.Settings.APIType.DANBOARD_LEGACY,
                "Danbooru", "http://danbooru.donmai.us"));
        assertThat(rowsAffected).isEqualTo(1);

        // Now get the row from the database and verify the data.
        SearchClient.Settings settings = database.get(rowID);
        database.close();
        assertThat(settings.getApiType()).isEqualTo(SearchClient.Settings.APIType.DANBOARD_LEGACY);
    }

    /**
     * Test the {@link io.github.tjg1.nori.database.APISettingsDatabase#delete(long)} method.
     */
    public void testDelete() {
        APISettingsDatabase database = new APISettingsDatabase(context);

        // Insert new row into the database.
        long rowID = database.insert(new SearchClient.Settings(SearchClient.Settings.APIType.DANBOARD,
                "Danbooru", "http://danbooru.donmai.us"));

        // Delete the newly created row.
        int rowsAffected = database.delete(rowID);
        assertThat(rowsAffected).isEqualTo(1);

        // Make sure it's really been deleted.
        assertThat(database.get(rowID)).isNull();

        // Clean up.
        database.close();
    }

    /**
     * Test if the database sends a Broadcast to the {@link androidx.core.content.LocalBroadcastManager} when the data is changed.
     */
    public void testUpdateBroadcast() throws Throwable {
        // Create a lock that waits for the broadcast to be received in the background.
        final CountDownLatch lock = new CountDownLatch(3);

        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Register BroadcastReceiver.
                LocalBroadcastManager.getInstance(context).registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        lock.countDown(); // Should receive 3 Broadcasts, one for each database operation.
                        if (lock.getCount() == 0) {
                            // Unregister broadcast receiver.
                            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
                        }
                    }
                }, new IntentFilter(APISettingsDatabase.BROADCAST_UPDATE));
                // Trigger database change broadcasts.
                APISettingsDatabase database = new APISettingsDatabase(context);
                long rowID = database.insert(new SearchClient.Settings(SearchClient.Settings.APIType.DANBOARD,
                        "Danbooru", "http://danbooru.donmai.us"));
                database.update(rowID, new SearchClient.Settings(SearchClient.Settings.APIType.DANBOARD_LEGACY,
                        "Danbooru", "http://danbooru.donmai.us"));
                database.delete(rowID);
                database.close();
            }
        });

        // Wait 10 seconds for the test to complete.
        lock.await(10, TimeUnit.SECONDS);
        assertThat(lock.getCount()).isEqualTo(0);
    }
}
