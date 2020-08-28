/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.tjg1.nori.fragment.AddTagFilterDialogFragment;
import io.github.tjg1.nori.util.StringUtils;

/**
 * Manages the list of tags filtered out from {@link io.github.tjg1.library.norilib.SearchResult}s in {@link io.github.tjg1.nori.SearchActivity}
 * and {@link io.github.tjg1.nori.ImageViewerActivity}.
 */
public class TagFilterSettingsActivity extends AppCompatActivity implements View.OnClickListener,
        AddTagFilterDialogFragment.AddTagListener {

    //region Instance fields
    /**
     * Default {@link android.content.SharedPreferences} object.
     */
    private SharedPreferences sharedPreferences;
    /**
     * List of filtered tags currently stored in {@link #sharedPreferences}.
     */
    private List<String> filteredTags = new ArrayList<>();
    /**
     * Adapter used by the tag {@link android.widget.ListView}.
     */
    private BaseAdapter tagListAdapter = new BaseAdapter() {

        @Override
        public int getCount() {
            return filteredTags.size();
        }

        @Override
        public String getItem(int position) {
            return filteredTags.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View recycledView, ViewGroup container) {
            // Recycle old view, if possible.
            View view = recycledView;
            if (view == null) {
                final LayoutInflater layoutInflater = getLayoutInflater();
                view = layoutInflater.inflate(R.layout.listitem_tag_filter, container, false);
            }

            // Populate views with content.
            final TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(getItem(position));
            final ImageButton removeButton = (ImageButton) view.findViewById(R.id.action_remove);
            removeButton.setTag(position);
            removeButton.setOnClickListener(TagFilterSettingsActivity.this);

            return view;
        }
    };
    //endregion

    //region Activity lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate layout XML.
        setContentView(R.layout.activity_tag_filter_settings);

        // Get shared preferences.
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        // Load list of filtered tags from shared preferences.
        if (sharedPreferences.contains(getString(R.string.preference_tagFilter_key))) {
            final String tagFilter = sharedPreferences.getString(getString(R.string.preference_tagFilter_key), "").trim();
            if (!TextUtils.isEmpty(tagFilter)) {
                filteredTags.addAll(Arrays.asList(tagFilter.split(" ")));
            }
        }

        // Set Toolbar as the Activity's app bar.
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Hide the app icon and use the activity title as the home button.
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Set up the ListView adapter.
        final ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setAdapter(tagListAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu XML.
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.tag_filter_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar and menu item clicks.
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.action_add:
                showAddTagDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //endregion

    //region View.OnClickListener methods (removing tags)
    @Override
    public void onClick(View view) {
        // Handle remove button clicks.
        final int position = (int) view.getTag();
        filteredTags.remove(position);
        updateSharedPreferences();
    }
    //endregion

    //region AddTagFilterDialogFragment.AddTagListener methods
    @Override
    public void addTag(String tag) {
        filteredTags.add(tag);
        updateSharedPreferences();

    }
    //endregion

    //region Showing add dialog

    /**
     * Show a dialog with an interface to add a new Tag to the filter list.
     */
    private void showAddTagDialog() {
        DialogFragment addTagFragment = new AddTagFilterDialogFragment();
        addTagFragment.show(getSupportFragmentManager(), "AddTagDialogFragment");
    }
    //endregion

    //region SharedPreferences

    /**
     * Update the tag filter SharedPreference with data stored in {@link #filteredTags}.
     */
    private void updateSharedPreferences() {
        sharedPreferences.edit()
                .putString(getString(R.string.preference_tagFilter_key),
                        StringUtils.mergeStringArray(filteredTags.toArray(new String[filteredTags.size()]), " ").trim())
                .apply();
        tagListAdapter.notifyDataSetChanged();
    }
    //endregion
}
