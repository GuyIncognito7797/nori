/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.adapter;


import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import java.util.List;

import io.github.tjg1.library.norilib.clients.SearchClient;
import io.github.tjg1.nori.R;
import io.github.tjg1.nori.database.APISettingsDatabase;

/**
 * Populates the {@link android.widget.ListView} with data from {@link io.github.tjg1.nori.database.APISettingsDatabase}.
 */
public class APISettingsListAdapter extends BaseAdapter
        implements LoaderManager.LoaderCallbacks<List<Pair<Integer, SearchClient.Settings>>>,
        AdapterView.OnItemClickListener {

    //region Loader IDs
    /**
     * {@link io.github.tjg1.nori.database.APISettingsDatabase} loader ID.
     */
    private static final int LOADER_ID_DATABASE_LOADER = 0x00;
    //endregion

    //region Instance fields
    /**
     * Android {@link Context} the adapter is used in.
     */
    private final Context context;
    /**
     * Interface handling user inputs.
     */
    private final APISettingsListAdapter.Listener listener;
    /**
     * List of {@link android.util.Pair}s mapping database row IDs to {@link io.github.tjg1.library.norilib.clients.SearchClient.Settings} objects.
     */
    private List<Pair<Integer, SearchClient.Settings>> settingsList;
    //endregion

    //region Constructors

    /**
     * Create a new adapter to populate a ListView with a list of services from the
     * {@link APISettingsDatabase}.
     *
     * @param context       Android context the ListView is used in.
     * @param loaderManager Android support library asynchronous loader manager.
     * @param listener      Listener handling user events.
     */
    public APISettingsListAdapter(Context context, LoaderManager loaderManager, Listener listener) {
        this.context = context;
        this.listener = listener;

        // Initialize the asynchronous database loader.
        loaderManager.initLoader(LOADER_ID_DATABASE_LOADER, null, this);
    }
    //endregion

    //region BaseAdapter methods
    @Override
    public int getCount() {
        if (settingsList != null) {
            return settingsList.size();
        }
        return 0;
    }

    @Override
    public SearchClient.Settings getItem(int position) {
        return settingsList.get(position).second;
    }

    @Override
    public long getItemId(int position) {
        return settingsList.get(position).first;
    }

    @Override
    public View getView(int position, View recycledView, ViewGroup container) {
        // Recycle the view, if possible.
        View view = recycledView;

        if (view == null) {
            // Create a new instance of the view.
            view = LayoutInflater.from(context)
                    .inflate(R.layout.listitem_service_setting, container, false);
        }

        // Get data from the List for current position.
        SearchClient.Settings settings = getItem(position);
        final long id = getItemId(position);
        // Populate views with content.
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(settings.getName());
        TextView summary = (TextView) view.findViewById(R.id.summary);
        summary.setText(settings.getEndpoint());
        // Attach onClickListener to the remove button and hook it up to the #removeSetting method.
        ImageButton actionRemove = (ImageButton) view.findViewById(R.id.action_remove);
        actionRemove.setFocusable(false);
        actionRemove.setOnClickListener(view1 -> listener.onServiceRemoved(id));

        return view;
    }
    //endregion

    //region LoaderManager.LoaderCallbacks<List<Pair<Integer, SearchClient.Settings>>> methods
    @Override
    public Loader<List<Pair<Integer, SearchClient.Settings>>> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_ID_DATABASE_LOADER) {
            // Initialize the database loader.
            return new APISettingsDatabase.Loader(context);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<List<Pair<Integer, SearchClient.Settings>>> loader, List<Pair<Integer, SearchClient.Settings>> data) {
        settingsList = data;
        notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<Pair<Integer, SearchClient.Settings>>> loader) {
        settingsList = null;
        notifyDataSetInvalidated();
    }
    //endregion

    //region AdapterView.OnItemClickListener methods
    @Override
    public void onItemClick(AdapterView<?> listView, View view, int position, long itemId) {
        listener.onServiceSelected(itemId, getItem(position));
    }
    //endregion

    //region Listener interface

    /**
     * Interface used to handle events when a service is selected from the ListView or removed
     * using the remove icon by the user.
     */
    public interface Listener {

        /**
         * Called when a service is selected from the ListView.
         *
         * @param serviceId       Database ID of the service.
         * @param serviceSettings {@link SearchClient.Settings} with settings for the selected service.
         */
        void onServiceSelected(long serviceId, SearchClient.Settings serviceSettings);

        /**
         * Called when the user clicks on the "Remove service" button on an item in the
         * {@link android.widget.ListView}.
         *
         * @param serviceId Database ID of the service to be removed.
         */
        void onServiceRemoved(long serviceId);

    }
    //endregion
}