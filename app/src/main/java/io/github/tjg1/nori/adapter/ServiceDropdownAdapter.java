/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

import io.github.tjg1.library.norilib.clients.SearchClient;
import io.github.tjg1.nori.APISettingsActivity;
import io.github.tjg1.nori.R;
import io.github.tjg1.nori.database.APISettingsDatabase;

/** Adapter populating the Search API picker in the ActionBar. */
public class ServiceDropdownAdapter extends BaseAdapter
    implements LoaderManager.LoaderCallbacks<List<Pair<Integer, SearchClient.Settings>>>,
    AdapterView.OnItemSelectedListener {

  //region Loader IDs
  /** Search client settings loader ID. */
  private static final int LOADER_ID_API_SETTINGS = 0x00;
  //endregion

  //region SharedPreferences keys
  /** Shared preference key used to store the last active {@link io.github.tjg1.library.norilib.clients.SearchClient}. */
  private static final String SHARED_PREFERENCE_LAST_SELECTED_INDEX = "io.github.tjg1.nori.SearchActivity.lastSelectedServiceIndex";
  //endregion

  //region Instance fields
  /** Context this adapter is used in. */
  private final Context context;
  /** Shared preferences of the application. */
  private final SharedPreferences sharedPreferences;
  /** Adapter view populated by this adapter. */
  private final AdapterView<?> adapterView;
  /** Listener used to interact with the {@link android.app.Activity} using this adapter. */
  private final ServiceDropdownAdapter.Listener listener;

  /** List of service settings loaded from {@link io.github.tjg1.nori.database.APISettingsDatabase}. */
  private List<Pair<Integer, SearchClient.Settings>> settingsList;
  /** ID of the last selected item. */
  private long lastSelectedItem;
  //endregion

  //region Constructors
  /**
   * Create a new adapter that populates a {@link Spinner} with a service selection dropdown.
   *
   * @param context           Android context.
   * @param sharedPreferences Shared preferences (used to restore last selection of the spinner).
   * @param loaderManager     Android support library loader manager.
   * @param adapterView       View populated by this adapter (used to restore last selection of the spinner)
   * @param listener          Listener used to interact with the {@link android.app.Activity} using this
   *                          adapter.
   */
  public ServiceDropdownAdapter(@NonNull Context context,
                                @NonNull SharedPreferences sharedPreferences,
                                @NonNull LoaderManager loaderManager,
                                @NonNull AdapterView<?> adapterView, @NonNull Listener listener) {
    this.context = context;
    this.sharedPreferences = sharedPreferences;
    this.adapterView = adapterView;
    this.listener = listener;

    // Restore last active item from SharedPreferences.
    lastSelectedItem = this.sharedPreferences.getLong(SHARED_PREFERENCE_LAST_SELECTED_INDEX, 1L);
    // Initialize the search client settings database loader.
    loaderManager.initLoader(LOADER_ID_API_SETTINGS, null, this);
  }
  //endregion

  //region BaseAdapter methods
  @Override
  public int getCount() {
    if (settingsList == null) {
      return 1;
    } else {
      return settingsList.size() + 1;
    }
  }

  @Override
  public SearchClient.Settings getItem(int position) {
    if (settingsList == null || position == settingsList.size()) // APISettingActivity View.
      return null;
    return settingsList.get(position).second;
  }

  @Override
  public long getItemId(int position) {
    if (settingsList == null || position == settingsList.size()) // APISettingActivity View.
      return -1;
    // Return database row ID.
    return settingsList.get(position).first;
  }

  @Override
  public View getView(int position, View recycledView, ViewGroup container) {
    // Reuse recycled view, if possible.
    @SuppressLint("ViewHolder") View view = LayoutInflater.from(context)
        .inflate(R.layout.simple_dropdown_item, container, false);

    // Populate views with content.
    TextView text1 = (TextView) view.findViewById(android.R.id.text1);
    SearchClient.Settings settings = getItem(position);
    if (settings != null) {
      text1.setText(settings.getName());
    } else {
      text1.setText(R.string.service_dropdown_settings);
      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          context.startActivity(new Intent(context, APISettingsActivity.class));
        }
      });
    }

    return view;
  }
  //endregion

  //region LoaderManager.LoaderCallbacks<List<Pair<Integer, SearchClient.Settings>>> methods
  @Override
  public Loader<List<Pair<Integer, SearchClient.Settings>>> onCreateLoader(int id, Bundle args) {
    if (id == LOADER_ID_API_SETTINGS) {
      return new APISettingsDatabase.Loader(context);
    }
    return null;
  }

  @Override
  public void onLoadFinished(Loader<List<Pair<Integer, SearchClient.Settings>>> loader, List<Pair<Integer, SearchClient.Settings>> data) {
    if (loader.getId() == LOADER_ID_API_SETTINGS) {
      // Update adapter data.
      settingsList = data;
      notifyDataSetChanged();
      // Reselect last active item.
      if (!data.isEmpty()) {
        adapterView.setSelection(getPositionByItemId(lastSelectedItem));
      } else {
        // Start APISettingActivity.
        Intent intent = new Intent(context, APISettingsActivity.class);
        intent.setAction(APISettingsActivity.ACTION_CREATE_SERVICE);
        context.startActivity(intent);
      }
    }
  }

  @Override
  public void onLoaderReset(Loader<List<Pair<Integer, SearchClient.Settings>>> loader) {
    // Invalidate adapter's data.
    settingsList = null;
    notifyDataSetInvalidated();
  }
  //endregion

  //region AdapterView.OnItemSelectedListener
  @Override
  public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
    // Save last active item to SharedPreferences.
    if (id != -1) {
      // Notify parent activity.
      listener.onSearchAPISelected(getItem(position), id != lastSelectedItem);
      // Update last selected item id.
      lastSelectedItem = id;
      sharedPreferences.edit().putLong(SHARED_PREFERENCE_LAST_SELECTED_INDEX, id).apply();
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> adapterView) {
    // Do nothing.
  }
  //endregion

  //region Get adapter item position by SQLite database row ID
  /**
   * Get position of the item with given database row ID.
   *
   * @param id Row ID.
   * @return Position of the item.
   */
  public int getPositionByItemId(long id) {
    for (int i = 0; i < getCount(); i++) {
      if (getItemId(i) == id) {
        return i;
      }
    }
    return 0;
  }
  //endregion

  //region Listener interface
  /** Listener used to interact with the {@link android.app.Activity} using this adapter. */
  public interface Listener {

    /**
     * Called when a new Search API is selected by the user from the action bar dropdown.
     *
     * @param settings         Selected {@link SearchClient.Settings} object.
     * @param expandActionView Should the SearchView action view be expanded?
     */
    public void onSearchAPISelected(SearchClient.Settings settings, boolean expandActionView);
  }
  //endregion
}
