/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.fragment;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ShareActionProvider;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.clients.SearchClient;
import io.github.tjg1.nori.R;
import io.github.tjg1.nori.util.NetworkUtils;


/**
 * Fragment used to display images in {@link io.github.tjg1.nori.ImageViewerActivity}.
 */
public abstract class ImageFragment extends Fragment {

    //region Constants (Bundle IDs and hardcoded URLs)
    /**
     * Bundle identifier used to save the displayed image object in {@link #onSaveInstanceState(android.os.Bundle)}.
     */
    protected static final String BUNDLE_ID_IMAGE = "io.github.tjg1.nori.Image";
    /**
     * String to prepend to Pixiv IDs to open them in the system web browser.
     */
    private static final String PIXIV_URL_PREFIX = "http://www.pixiv.net/member_illust.php?mode=medium&illust_id=";
    //endregion

    //region Instance fields
    /**
     * Image object displayed in this fragment.
     */
    protected Image image;
    /**
     * Class used for communication with the class that contains this fragment.
     */
    protected ImageFragmentListener listener;
    /**
     * True if this fragment is the currently active fragment (viewed by the user).
     */
    protected boolean isActive = false;
    //endregion

    //region Fragment methods (Lifecycle)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the image object from the fragment's arguments bundle.
        image = getArguments().getParcelable(BUNDLE_ID_IMAGE);

        // Enable options menu items for fragment.
        setHasOptionsMenu(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (ImageFragmentListener) getContext();
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public abstract View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.image, menu);

        // Set up ShareActionProvider
        MenuItem shareItem = menu.findItem(R.id.action_share);
        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(shareItem);
        shareActionProvider.setShareIntent(getShareIntent());

        // Hide the view on Pixiv menu item, if the Image does not have a Pixiv source URL.
        MenuItem shareOnPixivItem = menu.findItem(R.id.action_viewOnPixiv);
        if (image.pixivId == null || TextUtils.isEmpty(image.pixivId)) {
            shareOnPixivItem.setVisible(false);
        }

        // Hide the view source menu item, if the Image does not have a source URL.
        MenuItem viewSourceItem = menu.findItem(R.id.action_viewSource);
        if (image.source == null || TextUtils.isEmpty(image.source)) {
            viewSourceItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_tags:
                showTagListDialog();
                return true;
            case R.id.action_download:
                downloadImage();
                return true;
            case R.id.action_viewOnWeb:
                viewOnWeb();
                return true;
            case R.id.action_viewOnPixiv:
                viewOnPixiv();
                return true;
            case R.id.action_viewSource:
                viewSource();
                return true;
            case R.id.action_setAsWallpaper:
                setAsWallpaper();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    //endregion

    //region ViewPager onShown/onHidden

    /**
     * Called by the FragmentStatePagerAdapter when this fragment is currently the primary item
     * (shown to the user).
     */
    public void onShown() {
        this.isActive = true;
    }

    /**
     * Called by the FragmentStatePagerAdapter when this fragment is scrolled away (hidden).
     */
    public void onHidden() {
        this.isActive = false;
    }
    //endregion

    //region Options menu item helper methods

    /**
     * Get {@link android.content.Intent} to be sent by the {@link android.support.v7.widget.ShareActionProvider}.
     *
     * @return Share intent.
     */
    protected Intent getShareIntent() {
        // Send web URL to image.
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, image.webUrl);
        intent.setType("text/plain");
        return intent;
    }

    /**
     * Show the {@link io.github.tjg1.nori.fragment.TagListDialogFragment} for the current image.
     */
    protected void showTagListDialog() {
        DialogFragment tagListFragment = TagListDialogFragment.newInstance(image, listener.getSearchClientSettings());
        tagListFragment.show(getFragmentManager(), "TagListDialogFragment");
    }

    /**
     * Use the system {@link android.app.DownloadManager} service to download the image.
     */
    protected void downloadImage() {
        if (listener != null) {
            listener.downloadImage(image.fileUrl);
        }
    }

    /**
     * Opens the image Danbooru page in the system web browser.
     */
    protected void viewOnWeb() {
        // Create and send intent to display the image in the web browser.
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(image.webUrl));
        startActivity(intent);
    }

    /**
     * Opens the image Pixiv page in the system web browser.
     */
    protected void viewOnPixiv() {
        // Create and send to intent to display the image's pixiv page in the web browser.
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(PIXIV_URL_PREFIX + image.pixivId));
        startActivity(intent);
    }

    /**
     * Opens the image Pixiv page in the system web browser.
     */
    protected void viewSource() {
        // Create and send to intent to display the image's pixiv page in the web browser.
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(image.source));
        startActivity(intent);
    }

    /**
     * Downloads the full-resolution image in the background and sets it as the wallpaper.
     */
    protected void setAsWallpaper() {
        // Fetch and set full-screen image as wallpaper on background thread.
        final Context context = getContext();
        final String imageUrl = image.fileUrl;
        final WallpaperManager wallpaperManager = WallpaperManager.getInstance(getContext());

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... ignored) {
                try {
                    InputStream inputStream = new URL(imageUrl).openStream();
                    wallpaperManager.setStream(inputStream);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception error) {
                if (error != null) {
                    // Show error message to the user.
                    View view = getView();
                    if (view != null) {
                        Snackbar.make(view, String.format(context.getString(R.string.toast_couldNotSetWallpaper),
                                error.getLocalizedMessage()), Snackbar.LENGTH_LONG).show();
                    }
                }
            }
        }.execute();
    }
    //endregion

    //region Should load full-res images?

    /**
     * Evaluate the current network conditions using the {@link io.github.tjg1.nori.util.NetworkUtils} class to decide
     * if lower resolution images should be loaded to conserve bandwidth.
     *
     * @return True if lower resolution images should be used.
     */
    protected boolean shouldLoadImageSamples() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        return preferences.getBoolean(getString(R.string.preference_image_viewer_conserveBandwidth_key), true)
                || NetworkUtils.shouldFetchImageSamples(getContext());
    }
    //endregion

    //region Activity listener interface
    public interface ImageFragmentListener {
        /**
         * Should return the {@link SearchClient.Settings} object with the same settings used to fetch the image displayed by this fragment.
         */
        SearchClient.Settings getSearchClientSettings();

        /**
         * Downloads an image using {@link android.app.DownloadManager}, asking the user to grant storage write permission, if necessary.
         */
        void downloadImage(@NonNull String fireUrl);

        /**
         * Called when the ImageView within the fragment is single-tapped.
         */
        void onViewTap(View view, float x, float y);
    }
    //endregion
}
