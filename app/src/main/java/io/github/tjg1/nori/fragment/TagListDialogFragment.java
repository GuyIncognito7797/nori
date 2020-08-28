/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.fragment;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.Tag;
import io.github.tjg1.library.norilib.clients.SearchClient;
import io.github.tjg1.nori.R;
import io.github.tjg1.nori.SearchActivity;

;


/**
 * Dialog showing a list of tags for given image in {@link io.github.tjg1.nori.ImageViewerActivity}.
 */
public class TagListDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    //region Bundle IDs
    /**
     * Identifier used for the parceled {@link io.github.tjg1.library.norilib.clients.SearchClient.Settings} object in this fragment's argument bundle.
     */
    private static final String BUNDLE_ID_SEARCH_CLIENT_SETTINGS = "io.github.tjg1.nori.SearchClient.Settings";
    /**
     * Identifier used for the parceled {@link io.github.tjg1.library.norilib.Image} object in this fragment's argument bundle.
     */
    private static final String BUNDLE_ID_IMAGE = "io.github.tjg1.nori.Image";
    //endregion

    //region Instance fields
    /**
     * The image object containing the tags to show in this fragment.
     */
    private Image image;
    /**
     * Search client settings object included in {@link android.content.Intent}s to launch {@link io.github.tjg1.nori.SearchActivity}.
     */
    private SearchClient.Settings settings;
    //endregion

    //region Constructors

    /**
     * Required empty constructor.
     */
    public TagListDialogFragment() {
    }
    //endregion

    //region Static methods (newInstance)

    /**
     * Factory method to create a new TagListDialogFragment for given {@link io.github.tjg1.library.norilib.Image}.
     *
     * @param image    Image to display tags from.
     * @param settings Settings object included in SearchActivity launch intents sent from this dialog.
     * @return A new instance of TagListDialogFragment.
     */
    @SuppressWarnings("TypeMayBeWeakened")
    public static TagListDialogFragment newInstance(Image image, SearchClient.Settings settings) {
        // Package image object into the fragment's argument bundle.
        TagListDialogFragment fragment = new TagListDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(BUNDLE_ID_IMAGE, image);
        arguments.putParcelable(BUNDLE_ID_SEARCH_CLIENT_SETTINGS, settings);
        fragment.setArguments(arguments);

        return fragment;
    }
    //endregion

    //region DialogFragment methods (Lifecycle)
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Extract data from the arguments bundle.
        image = getArguments().getParcelable(BUNDLE_ID_IMAGE);
        settings = getArguments().getParcelable(BUNDLE_ID_SEARCH_CLIENT_SETTINGS);

        return new AlertDialog.Builder(getContext())
                .setAdapter(new TagListAdapter(), this)
                .setPositiveButton(R.string.dialog_tags_closeButton, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dismiss();
                    }

                }).create();
    }
    //endregion

    //region DialogInterface.OnClickListener methods
    @Override
    public void onClick(DialogInterface dialog, int position) {
        // Start SearchActivity with search for the given tag.
        final Intent intent = new Intent(getContext(), SearchActivity.class);
        intent.setAction(Intent.ACTION_SEARCH);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(SearchActivity.INTENT_EXTRA_SEARCH_CLIENT_SETTINGS, settings);
        intent.putExtra(SearchActivity.INTENT_EXTRA_SEARCH_QUERY, image.tags[position].getName());
        startActivity(intent);

        // Dismiss the dialog after the activity is started.
        dialog.dismiss();
    }
    //endregion

    //region List adapter

    /**
     * Adapter used to show the tag list inside a {@link android.widget.ListView}.
     */
    private class TagListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if (image == null || image.tags == null) {
                return 0;
            }
            return image.tags.length;
        }

        @Override
        public Tag getItem(int position) {
            return image.tags[position];
        }

        @Override
        public long getItemId(int position) {
            // Item ID == position.
            return position;
        }

        @Override
        public View getView(int position, View recycleView, ViewGroup container) {
            // Get tag object at given position.
            final Tag tag = getItem(position);

            // Recycle old view if possible.
            TextView textView = (TextView) recycleView;
            if (textView == null) {
                // Inflate new view from XML.
                final LayoutInflater inflater = getLayoutInflater(null);
                textView = (TextView) inflater.inflate(R.layout.listitem_tag, container, false);
            }

            // Set tag name.
            textView.setText(tag.getName());
            // Set appropriate colour for the tag type.
            textView.setTextColor(ContextCompat.getColor(getContext(), tag.getColor()));

            return textView;
        }
    }
    //endregion
}
