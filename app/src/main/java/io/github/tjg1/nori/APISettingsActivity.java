/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

import io.github.tjg1.library.norilib.clients.SearchClient;
import io.github.tjg1.library.norilib.service.ServiceTypeDetectionService;
import io.github.tjg1.nori.adapter.APISettingsListAdapter;
import io.github.tjg1.nori.database.APISettingsDatabase;
import io.github.tjg1.nori.fragment.EditAPISettingDialogFragment;

/**
 * Adds, edits or removes API settings from {@link io.github.tjg1.nori.database.APISettingsDatabase}.
 */
public class APISettingsActivity extends AppCompatActivity
        implements EditAPISettingDialogFragment.Listener, APISettingsListAdapter.Listener {

    //region Intent actions
    /**
     * Intent action used to indicate that the add service dialog should be displayed, when the Activity is created.
     */
    public static final String ACTION_CREATE_SERVICE = "CREATE_SERVICE";
    //endregion

    //region Constants
    /**
     * A new row will be inserted into the database when this row ID value is passed to {@link #editService(long, String, String, String, String)}.
     */
    private static final long ROW_ID_INSERT = -1L;
    //endregion

    //region Activity methods (Lifecycle)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate layout XML.
        setContentView(R.layout.activity_service_settings);

        // Set up Toolbar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Hide the app icon and use the activity title as the home button.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Set up the ListView adapter and OnItemClickListener.
        ListView listView = (ListView) findViewById(android.R.id.list);
        APISettingsListAdapter listAdapter = new APISettingsListAdapter(this, getSupportLoaderManager(), this);
        listView.setOnItemClickListener(listAdapter);
        listView.setAdapter(listAdapter);

        // Show the service creation dialog, if started with the create service intent action.
        Intent intent = getIntent();
        if (savedInstanceState == null && ACTION_CREATE_SERVICE.equals(intent.getAction())) {
            showCreateServiceDialog();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu XML.
        getMenuInflater().inflate(R.menu.api_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar button clicks.
        switch (item.getItemId()) {
            case android.R.id.home:
                // Make pressing the action bar "up" button perform same action as pressing the physical "back" button.
                onBackPressed();
                return true;
            case R.id.action_add:
                // Show dialog to let user add a new service.
                showCreateServiceDialog();
                return true;
            default:
                // Perform default action.
                return super.onOptionsItemSelected(item);
        }
    }
    //endregion

    //region APISettingsListAdapter.Listener methods
    @Override
    public void onServiceSelected(long serviceId, SearchClient.Settings serviceSettings) {
        // Show dialog to edit the service settings object.
        EditAPISettingDialogFragment.newInstance(serviceId, serviceSettings)
                .show(getSupportFragmentManager(), "EditAPISettingsDialogFragment");
    }

    @Override
    public void onServiceRemoved(final long serviceId) {
        // Remove setting from database on a background thread.
        // This is so database I/O doesn't block the UI thread.
        new Thread(() -> new APISettingsDatabase(APISettingsActivity.this).delete(serviceId)).start();
    }
    //endregion

    //region EditAPISettingDialogFragment.Listener methods
    @Override
    public void addService(String name, String url, String username, String passphrase) {
        editService(ROW_ID_INSERT, name, url, username, passphrase);
    }

    @Override
    public void editService(final long rowId, final String name, final String url, final String username, final String passphrase) {
        // Show progress dialog during the service type detection process.
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.setMessage(getString(R.string.dialog_message_detectingApiType));
        dialog.show();

        // Register broadcast receiver to get results from the background service type detection service.
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get result code from received intent.
                int resultCode = intent.getIntExtra(ServiceTypeDetectionService.RESULT_CODE, -1);
                if (resultCode == ServiceTypeDetectionService.RESULT_OK) {
                    // Add a new service to the database on a background thread.
                    // This is so database I/O doesn't block the UI thread.
                    SearchClient.Settings.APIType apiType =
                            SearchClient.Settings.APIType.values()[intent.getIntExtra(ServiceTypeDetectionService.API_TYPE, 0)];
                    String endpointUrl = intent.getStringExtra(ServiceTypeDetectionService.ENDPOINT_URL);
                    final SearchClient.Settings settings = new SearchClient.Settings(apiType, name, endpointUrl, username, passphrase);
                    new Thread(() -> {
                        APISettingsDatabase database = new APISettingsDatabase(APISettingsActivity.this);
                        if (rowId == ROW_ID_INSERT) {
                            database.insert(settings);
                        } else {
                            database.update(rowId, settings);
                        }
                        database.close();
                    }).start();
                } else if (resultCode == ServiceTypeDetectionService.RESULT_FAIL_INVALID_URL) {
                    Snackbar.make(findViewById(R.id.root), R.string.toast_error_serviceUriInvalid, Snackbar.LENGTH_LONG).show();
                } else if (resultCode == ServiceTypeDetectionService.RESULT_FAIL_NETWORK) {
                    Snackbar.make(findViewById(R.id.root), R.string.toast_error_noNetwork, Snackbar.LENGTH_LONG).show();
                } else if (resultCode == ServiceTypeDetectionService.RESULT_FAIL_NO_API) {
                    Snackbar.make(findViewById(R.id.root), R.string.toast_error_noServiceAtGivenUri, Snackbar.LENGTH_LONG).show();
                }

                // Unregister the broadcast receiver.
                unregisterReceiver(this);
                // Dismiss progress dialog.
                dialog.dismiss();
            }
        }, new IntentFilter(ServiceTypeDetectionService.ACTION_DONE));

        // Start the background service type detection service.
        Intent serviceIntent = new Intent(this, ServiceTypeDetectionService.class);
        serviceIntent.putExtra(ServiceTypeDetectionService.ENDPOINT_URL, url);
        startService(serviceIntent);
    }
    //endregion

    //region Service dialog

    /**
     * Displays the service creation dialog.
     */
    private void showCreateServiceDialog() {
        new EditAPISettingDialogFragment().show(getSupportFragmentManager(), "EditAPISettingDialogFragment");
    }
    //endregion
}
