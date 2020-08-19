/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.fragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.builder.AnimateGifMode;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;
import io.github.tjg1.nori.BuildConfig;
import io.github.tjg1.nori.R;
import io.github.tjg1.nori.widget.SquareImageView;

/** Shows images from a {@link SearchResult} as a scrollable grid of thumbnails. */
public class SearchResultGridFragment extends Fragment implements AdapterView.OnItemClickListener,
    AbsListView.OnScrollListener {

  //region Bundle IDs
  /** Identifier used for saving currently displayed search result in {@link #onSaveInstanceState(android.os.Bundle)}. */
  private static final String BUNDLE_ID_SEARCH_QUERY = "io.github.tjg1.nori.SearchQuery";
  private static final String BUNDLE_ID_VISIBLE_PAGE = "io.github.tjg1.nori.FirstVisibleSearchPage";
  private static final String BUNDLE_ID_VISIBLE_ITEM = "io.github.tjg1.nori.FirstVisibleSearchPagePosition";
  //endregion

  //region Instance fields
  /** Interface used for communication with parent class. */
  private OnSearchResultGridFragmentInteractionListener mListener;
  /** GridView used to display the thumbnails. */
  private GridView gridView;
  /** Search result displayed by the SearchResultGridFragment. */
  private SearchResult searchResult;
  /** Previous first visible item's search page offset, restored from saved instance state. */
  private int firstVisibleSearchPage = 0;
  /** Previous first visible item's position, restored from saved instance state. */
  private int firstVisibleSearchPagePosition = 0;
  /** Previous search query, restored from saved instance state. */
  private String previousSearchQuery = null;
  /** Adapter used by the GridView in this fragment. */
  private BaseAdapter gridAdapter = new BaseAdapter() {
    @Override
    public int getCount() {
      // Return count of images.
      if (searchResult == null) {
        return 0;
      }
      return searchResult.getImages().length;
    }

    @Override
    public Image getItem(int position) {
      // Return image at given position.
      return searchResult.getImages()[position];
    }

    @Override
    public long getItemId(int position) {
      return Long.valueOf(getItem(position).id);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      // Get image object at given position.
      Image image = getItem(position);
      // Create image view for given position.
      ImageView imageView = (ImageView) convertView;

      // Create a new image, if not recycled.
      if (imageView == null) {
        imageView = new SquareImageView(getContext());
        imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      }

      int previewSize;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        // Resize thumbnails to actual GridView column width on Jelly Bean and above.
        previewSize = gridView.getColumnWidth();
      } else {
        // Fallback to requested column width on older versions.
        previewSize = getGridViewColumnWidth();
      }

      // Load image into view.
      Ion.with(getContext())
          .load(image.previewUrl)
          .userAgent("nori/" + BuildConfig.VERSION_NAME)
          .withBitmap()
          .resize(previewSize, previewSize)
          .animateGif(AnimateGifMode.NO_ANIMATE)
          .placeholder(R.color.network_thumbnail_placeholder)
          .centerCrop()
          .intoImageView(imageView);

      return imageView;
    }
  };
  //endregion

  //region Constructor
  /** Required public empty constructor. */
  public SearchResultGridFragment() {
  }
  //endregion

  //region Fragment methods (Lifecycle)
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    // Preserve currently displayed SearchResult.
    if (searchResult != null) {
      if (gridView.getCount() > 0) {
        final Image firstVisibleImage =
                (Image) gridView.getItemAtPosition(gridView.getFirstVisiblePosition());

        final int firstVisibleSearchPage = firstVisibleImage.searchPage;
        final int firstVisibleSearchPagePosition = firstVisibleImage.searchPagePosition;

        outState.putInt(BUNDLE_ID_VISIBLE_PAGE, firstVisibleSearchPage);
        outState.putInt(BUNDLE_ID_VISIBLE_ITEM, firstVisibleSearchPagePosition);
      }

      outState.putString(BUNDLE_ID_SEARCH_QUERY, Tag.stringFromArray(searchResult.getQuery()));
    } else if (previousSearchQuery != null) {
      // Save the previous search query, in case the SearchResult hasn't loaded yet.
      outState.putString(BUNDLE_ID_SEARCH_QUERY, this.previousSearchQuery);
      outState.putInt(BUNDLE_ID_VISIBLE_PAGE, firstVisibleSearchPage);
      outState.putInt(BUNDLE_ID_VISIBLE_ITEM, firstVisibleSearchPagePosition);
    }
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    // Get a reference to parent Context, making sure that it implements the proper callback interface.
    try {
      mListener = (OnSearchResultGridFragmentInteractionListener) getContext();
    } catch (ClassCastException e) {
      throw new ClassCastException(getContext().toString()
          + " must implement OnFragmentInteractionListener");
    }
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    // Restore search query from saved instance state to preserve search results across screen rotations.
    if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_ID_SEARCH_QUERY)) {
      this.previousSearchQuery = savedInstanceState.getString(BUNDLE_ID_SEARCH_QUERY);
      this.firstVisibleSearchPage = savedInstanceState.getInt(BUNDLE_ID_VISIBLE_PAGE, 0);
      this.firstVisibleSearchPagePosition = savedInstanceState.getInt(BUNDLE_ID_VISIBLE_ITEM, 0);

      if (this.previousSearchQuery != null) {
        mListener.onRestoreSearchGridState(this.previousSearchQuery,
            this.firstVisibleSearchPagePosition);
      }
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    mListener = null;
  }
  //endregion

  //region Fragment methods (inflating view)
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_search_result_grid, container, false);

    // Set adapter for GridView.
    gridView = (GridView) view.findViewById(R.id.image_grid);
    gridView.setColumnWidth(getGridViewColumnWidth());
    gridView.setAdapter(gridAdapter);
    gridView.setOnScrollListener(this);
    gridView.setOnItemClickListener(this);

    // Return inflated view.
    return view;
  }
  //endregion

  //region AdapterView.OnItemClickListener methods (starting ImageViewerActivity)
  @Override
  public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
    if (mListener != null) {
      // Notify parent Context that image has been clicked.
      mListener.onImageSelected((Image) gridAdapter.getItem(position), position);
    }
  }
  //endregion

  //region AbsListView.OnScrollListener methods (infinite scrolling)
  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {
    // Do nothing.
  }

  @Override
  public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
    // Implement endless scrolling.
    // Fetch more images if near the end of the list and more images are available for the SearchResult.
    if ((totalItemCount - visibleItemCount) <= (firstVisibleItem + 10) && searchResult != null
        && searchResult.hasNextPage() && mListener != null) {
      mListener.fetchMoreImages(searchResult);
    }
  }
  //endregion

  //region Getters & Setters (SearchResult)
  /**
   * Get search result displayed by this fragment.
   *
   * @return Search result shown in this fragment.
   */
  public SearchResult getSearchResult() {
    return this.searchResult;
  }

  /**
   * Update the SearchResult displayed by this fragment.
   *
   * @param searchResult Search result. Set to null to hide the current search result.
   */
  public void setSearchResult(SearchResult searchResult) {
    if (searchResult == null) {
      this.searchResult = null;
      gridAdapter.notifyDataSetInvalidated();
    } else {
      this.searchResult = searchResult;
      gridAdapter.notifyDataSetChanged();
      if (this.firstVisibleSearchPagePosition != 0) {
        // Restore last visible search page position from saved instance state.
        gridView.smoothScrollToPositionFromTop(firstVisibleSearchPagePosition, 0);
        this.firstVisibleSearchPagePosition = 0;
      }
    }
  }
  //endregion

  //region Grid column width
  /**
   * Get the grid view column size from the thumbnail size shared preference.
   *
   * @return Minimum column size, in pixels.
   */
  private int getGridViewColumnWidth() {
    // Get preference value from SharedPreference.
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
    String previewSize = sharedPreferences.getString(getString(R.string.preference_previewSize_key),
        getString(R.string.preference_previewSize_default));

    // Return dimension for given preference value, in pixels.
    switch (previewSize) {
      case "small":
        return getResources().getDimensionPixelSize(R.dimen.previewSize_small);
      case "medium":
        return getResources().getDimensionPixelSize(R.dimen.previewSize_medium);
      case "large":
        return getResources().getDimensionPixelSize(R.dimen.previewSize_large);
      default:
        return getResources().getDimensionPixelSize(R.dimen.previewSize_medium);
    }

  }
  //endregion

  //region Activity listener interface
  public interface OnSearchResultGridFragmentInteractionListener {
    /**
     * Called when {@link io.github.tjg1.library.norilib.Image} in the search result grid is selected by the user.
     *
     * @param image    Image selected.
     * @param position Index of the image in the {@link SearchResult}.
     */
    public void onImageSelected(Image image, int position);

    /**
     * Called when the user scrolls the thumbnail {@link android.widget.GridView} near the end and more images should be fetched
     * to implement "endless scrolling".
     *
     * @param searchResult Search result for which more images should be fetched.
     */
    public void fetchMoreImages(SearchResult searchResult);

    /**
     * Called when the {@link SearchResult} has to fetched to restore this SearchResultGridFragment's saved instance state.
     * @param savedQuery Saved search query.
     * @param firstVisiblePageOffset Last visible page offset to retrieve for infinite scrolling.
     */
    public void onRestoreSearchGridState(@NonNull String savedQuery, int firstVisiblePageOffset);
  }
  //endregion
}
