/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import java.io.IOException;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;
import io.github.tjg1.library.norilib.clients.SearchClient;
import io.github.tjg1.nori.adapter.ImagePagerAdapter;
import io.github.tjg1.nori.fragment.ImageFragment;
import io.github.tjg1.nori.view.ImageViewerPager;

/** Activity used to display full-screen images. */
public class ImageViewerActivity extends AppCompatActivity implements ViewPager.OnPageChangeListener,
    ImageFragment.ImageFragmentListener, ImagePagerAdapter.Listener {

  //region Bundle IDs
  /** Identifier used to keep the displayed {@link io.github.tjg1.library.norilib.SearchResult} in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_RESULT = "io.github.tjg1.nori.SearchResult";
  /** Identifier used to keep the position of the selected {@link io.github.tjg1.library.norilib.Image} in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_IMAGE_INDEX = "io.github.tjg1.nori.ImageIndex";
  /** Identifier used to keep {@link #searchClient} settings in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_CLIENT_SETTINGS = "io.github.tjg1.nori.SearchClient.Settings";
  /** Identifier used to keep a queued {@link android.app.DownloadManager.Request} while we wait for user to grant permissions. */
  private static final String BUNDLE_ID_QUEUED_DOWNLOAD_REQUEST = "io.github.tjg1.nori.QueuedDownloadImageRequest";
  //endregion

  //region Constants
  /** Identifier used to ask permission to download an image to the SD card. */
  private static final int PERMISSION_REQUEST_DOWNLOAD_IMAGE = 0x00;
  /** Fetch more images when the displayed image is this far from the last {@link io.github.tjg1.library.norilib.Image} in the current {@link io.github.tjg1.library.norilib.SearchResult}. */
  private static final int INFINITE_SCROLLING_THRESHOLD = 3;
  //endregion

  //region Instance fields
  /** Default shared preferences. */
  private SharedPreferences sharedPreferences;
  /** View pager used to display the images. */
  private ImageViewerPager viewPager;
  /** Search result shown by the {@link android.support.v4.app.FragmentStatePagerAdapter}. */
  private SearchResult searchResult;
  /** Adapter used to populate the {@link android.support.v4.view.ViewPager} used to display and flip through the images. */
  private ImagePagerAdapter imagePagerAdapter;
  /** Search API client used to retrieve more search results for infinite scrolling. */
  private SearchClient searchClient;
  /** Callback waiting to receive another page of {@link io.github.tjg1.library.norilib.Image}s for the current {@link io.github.tjg1.library.norilib.SearchResult}. */
  private SearchClient.SearchCallback searchCallback;
  /** {@link android.widget.ProgressBar} used to indicated Search API activity. */
  private ProgressBar searchProgressBar;
  /** {@link DownloadManager} used to download images. */
  private DownloadManager downloadManager;
  /** URL to an image to be downloaded once the user grants us permission to write to the SD card. */
  private String queuedDownloadRequestUrl;
  /** True if the {@link AppBarLayout} is currently collapsed. */
  private boolean appBarCollapsed = false;
  //endregion

  //region Activity lifecycle
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Restore state from savedInstanceState.
    super.onCreate(savedInstanceState);

    // Get shared preferences.
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    // Get data out of Intent sent by SearchActivity or restore them from the saved instance
    // state.
    int imageIndex;
    if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_ID_IMAGE_INDEX) &&
        savedInstanceState.containsKey(BUNDLE_ID_SEARCH_RESULT)) {
      imageIndex = savedInstanceState.getInt(BUNDLE_ID_IMAGE_INDEX);
      searchResult = savedInstanceState.getParcelable(BUNDLE_ID_SEARCH_RESULT);
      SearchClient.Settings searchClientSettings = savedInstanceState.getParcelable(BUNDLE_ID_SEARCH_CLIENT_SETTINGS);
      if (searchClientSettings != null) {
        searchClient = searchClientSettings.createSearchClient(this);
      }
      if (savedInstanceState.containsKey(BUNDLE_ID_QUEUED_DOWNLOAD_REQUEST)) {
        String fileUrl = savedInstanceState.getString(BUNDLE_ID_QUEUED_DOWNLOAD_REQUEST);
        if (fileUrl != null) {
          queuedDownloadRequestUrl = savedInstanceState.getString(BUNDLE_ID_QUEUED_DOWNLOAD_REQUEST);
        }
      }
    } else {
      final Intent intent = getIntent();
      imageIndex = intent.getIntExtra(SearchActivity.BUNDLE_ID_IMAGE_INDEX, 0);
      searchResult = intent.getParcelableExtra(SearchActivity.BUNDLE_ID_SEARCH_RESULT);
      searchClient = ((SearchClient.Settings) intent.getParcelableExtra(SearchActivity.BUNDLE_ID_SEARCH_CLIENT_SETTINGS))
          .createSearchClient(this);
    }

    // Keep screen on, if enabled by the user.
    if (sharedPreferences.getBoolean(getString(R.string.preference_image_viewer_keepScreenOn_key), true)) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Populate content view.
    setContentView(R.layout.activity_image_viewer);
    searchProgressBar = (ProgressBar) findViewById(R.id.progressBar);
    final int layoutMargin = ((FrameLayout.LayoutParams) searchProgressBar.getLayoutParams())
        .topMargin;

    // Set up the action bar.
    final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    final ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayShowHomeEnabled(false);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }

    // Create and set the image viewer Fragment pager adapter.
    imagePagerAdapter = new ImagePagerAdapter(getSupportFragmentManager(), this);
    viewPager = (ImageViewerPager) findViewById(R.id.image_pager);
    viewPager.setAdapter(imagePagerAdapter);
    viewPager.addOnPageChangeListener(this);
    viewPager.setCurrentItem(imageIndex);

    // Collapse the ActionBar.
    final AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
    appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
      @Override
      public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        if (verticalOffset < 0) {
          appBarCollapsed = true;
          viewPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        } else {
          appBarCollapsed = false;
          viewPager.setSystemUiVisibility(0);
        }

        // Set progress bar position relative to action bar.
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) searchProgressBar.getLayoutParams();
        params.setMargins(0, appBarLayout.getTotalScrollRange() + verticalOffset + layoutMargin, 0, 0);
        searchProgressBar.setLayoutParams(params);
      }
    });
    appBarLayout.setExpanded(false, true);

    // Set activity title.
    setTitle(searchResult.getImages()[imageIndex]);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle menu item interactions.
    switch (item.getItemId()) {
      case android.R.id.home: // Action bar "back button".
        onBackPressed();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    // Keep search result and the index of currently displayed image.
    outState.putParcelable(BUNDLE_ID_SEARCH_RESULT, searchResult);
    outState.putInt(BUNDLE_ID_IMAGE_INDEX, viewPager.getCurrentItem());
    outState.putParcelable(BUNDLE_ID_SEARCH_CLIENT_SETTINGS, searchClient.getSettings());
    if (queuedDownloadRequestUrl != null) {
      outState.putString(BUNDLE_ID_QUEUED_DOWNLOAD_REQUEST, queuedDownloadRequestUrl);
    }
  }
  //endregion

  //region Marshmallow permissions
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PERMISSION_REQUEST_DOWNLOAD_IMAGE && grantResults.length != 0) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED && queuedDownloadRequestUrl != null) {
        getDownloadManager().enqueue(getImageDownloadRequest(queuedDownloadRequestUrl));
        queuedDownloadRequestUrl = null;
      } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
        Snackbar.make(findViewById(R.id.root), R.string.toast_imageDownloadPermissionDenied,
            Snackbar.LENGTH_LONG).show();
      }
    }
  }
  //endregion

  //region ViewPager.OnPageChangeListener methods
  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    // Do nothing.
  }

  @Override
  public void onPageSelected(int position) {
    // Set activity title to image metadata.
    setTitle(searchResult.getImages()[position]);

    // Fetch more images for infinite scrolling, if available and there isn't another search request being waited on.
    if (searchCallback == null && searchResult.hasNextPage()
        && (searchResult.getImages().length - position) <= INFINITE_SCROLLING_THRESHOLD) {
      fetchMoreImages();
    }
  }

  @Override
  public void onPageScrollStateChanged(int state) {
    // Do nothing.
  }
  //endregion

  //region ImageFragment.ImageFragmentListener methods
  @Override
  public void onViewTap(View view, float x, float y) {
    toggleActionBar();
  }

  @Override
  public SearchResult getSearchResult() {
    return searchResult;
  }

  @Override
  public SearchClient.Settings getSearchClientSettings() {
    return searchClient.getSettings();
  }

  @Override
  public void downloadImage(@NonNull String fileUrl) {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
      queuedDownloadRequestUrl = null;
      DownloadManager.Request request = getImageDownloadRequest(fileUrl);
      request.addRequestHeader("User-Agent", "nori/" + BuildConfig.VERSION_NAME);
      request.addRequestHeader("Referer", fileUrl);
      getDownloadManager().enqueue(request);
    } else {
      queuedDownloadRequestUrl = fileUrl;
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_DOWNLOAD_IMAGE);
    }
  }
  //endregion

  //region Instance methods (UI)
  /**
   * Set the activity title to contain the currently displayed image's metadata.
   *
   * @param image Image to get the metadata from.
   */
  private void setTitle(Image image) {
    String title = String.format(getString(R.string.activity_image_viewer_titleFormat),
        image.id, Tag.stringFromArray(image.tags));

    // Truncate string with ellipsis at the end, if needed.
    if (title.length() > getResources().getInteger(R.integer.activity_image_viewer_titleMaxLength)) {
      title = title.substring(0, getResources().getInteger(R.integer.activity_image_viewer_titleMaxLength)) + "…";
    }

    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      getSupportActionBar().setTitle(title);
    }
  }

  private void toggleActionBar() {
    // Toggle the action bar and UI dim.
    AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appBarLayout);
    if (appBarCollapsed) {
      appBarLayout.setExpanded(true, true);
    } else {
      appBarLayout.setExpanded(false, true);
    }
  }
  //endregion

  //region Downloading images
  /**
   * Create a new {@link DownloadManager} or re-use the existing one.
   *
   * @return {@link DownloadManager} used to download images.
   */
  @NonNull
  private DownloadManager getDownloadManager() {
    if (downloadManager == null) {
      downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
    }
    return downloadManager;
  }

  /** Create a {@link android.app.DownloadManager.Request} to download an image. */
  @NonNull
  private DownloadManager.Request getImageDownloadRequest(@NonNull String fileUrl) {
    // Extract file name from URL.
    String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
    // Create download directory, if it does not already exist.
    //noinspection ResultOfMethodCallIgnored
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).mkdirs();

    // Create and queue download request.
    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl))
        .setTitle(fileName)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        .setVisibleInDownloadsUi(true);
    // Trigger media scanner to add image to system gallery app on Honeycomb and above.
    request.allowScanningByMediaScanner();
    // Show download UI notification on Honeycomb and above.
    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

    return request;
  }
  //endregion

  //region Infinite scrolling
  /**
   * Fetch images from the next page of the {@link io.github.tjg1.library.norilib.SearchResult}, if available.
   */
  private void fetchMoreImages() {
    // Ignore request if there is another API request pending.
    if (searchCallback != null) {
      return;
    }
    // Show the indeterminate progress bar in the action bar.
    searchProgressBar.setVisibility(View.VISIBLE);
    // Request search result from API client.
    searchCallback = new InfiniteScrollingSearchCallback(searchResult);
    searchClient.search(Tag.stringFromArray(searchResult.getQuery()), searchResult.getCurrentOffset() + 1, searchCallback);
  }

  /** Callback waiting to receive more images for infinite scrolling. */
  private class InfiniteScrollingSearchCallback implements SearchClient.SearchCallback {
    private final SearchResult searchResult;

    /**
     * Create a new InfiniteScrollingSearchCallback.
     *
     * @param searchResult Search result to append new results to.
     */
    public InfiniteScrollingSearchCallback(SearchResult searchResult) {
      this.searchResult = searchResult;
    }

    @Override
    public void onFailure(IOException e) {
      // Clear the active search callback and hide the progress bar in the action bar.
      searchCallback = null;
      searchProgressBar.setVisibility(View.GONE);

      // Display error toast notification to the user.
      Snackbar.make(findViewById(R.id.root),
          String.format(getString(R.string.toast_infiniteScrollingFetchError),
              e.getLocalizedMessage()), Snackbar.LENGTH_LONG
      ).show();
    }

    @Override
    public void onSuccess(SearchResult searchResult) {
      // Clear the active search callback and hide the progress bar in the action bar.
      searchCallback = null;
      searchProgressBar.setVisibility(View.GONE);

      if (searchResult.getImages().length == 0) {
        // Just mark the current SearchResult as having reached the last page.
        this.searchResult.onLastPage();
      } else {
        // Filter the received SearchResult.
        if (sharedPreferences.contains(getString(R.string.preference_safeSearch_key))) {
          // Get filter from shared preferences.
          searchResult.filter(Image.SafeSearchRating.arrayFromStrings(
              sharedPreferences.getString(getString(R.string.preference_safeSearch_key), "").split(" ")));
        } else {
          // Get default filter from resources.
          searchResult.filter(Image.SafeSearchRating.arrayFromStrings(getResources().getStringArray(R.array.preference_safeSearch_defaultValues)));
        }
        if (sharedPreferences.contains(getString(R.string.preference_tagFilter_key))) {
          // Get tag filters from shared preferences and filter the result.
          searchResult.filter(Tag.arrayFromString(sharedPreferences.getString(getString(R.string.preference_tagFilter_key), "")));
        }

        // Update the search result and notify the ViewPager adapter that the data set has changed.
        this.searchResult.addImages(searchResult.getImages(), searchResult.getCurrentOffset());
        imagePagerAdapter.notifyDataSetChanged();

        // If all images in the current search result were filtered out, try fetching the next page.
        if (searchResult.getImages().length == 0) {
          fetchMoreImages();
        }
      }
    }
  }
  //endregion
}
