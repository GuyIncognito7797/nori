/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.nori.fragment.ImageFragment;
import io.github.tjg1.nori.fragment.RemoteImageFragment;
import io.github.tjg1.nori.fragment.VideoPlayerFragment;

/** Adapter used to populate {@link androidx.core.view.ViewPager} with {@link io.github.tjg1.nori.fragment.ImageFragment}s. */
public class ImagePagerAdapter extends FragmentStatePagerAdapter {

  //region Instance fields
  /** Listener used to interact with the activity using this adapter. */
  private final ImagePagerAdapter.Listener listener;
  /** Fragment currently being displayed. */
  private ImageFragment activeFragment;
  //endregion

  //region Constructors
  /**
   * Create a new {@link androidx.core.view.PagerAdapter} displaying {@link Image}s using
   * Fragments.
   *
   * @param fm       Android support library fragment manager.
   * @param listener Listener used to interact with the {@link android.app.Activity} using this adapter.
   */
  public ImagePagerAdapter(FragmentManager fm, Listener listener) {
    super(fm);

    this.listener = listener;
  }
  //endregion

  //region FragmentStatePagerAdapter methods
  @Override
  public Fragment getItem(int position) {
    // Create a new instance of ImageFragment for the given image.
    Image image = listener.getSearchResult().getImages()[position];

    if (shouldUseVideoPlayerFragment(image)) {
      return VideoPlayerFragment.newInstance(image);
    } else {
      return RemoteImageFragment.newInstance(image);
    }
  }

  @Override
  public void setPrimaryItem(ViewGroup container, int position, Object object) {
    super.setPrimaryItem(container, position, object);
    if (activeFragment != object) {
      if (activeFragment != null) {
        activeFragment.onHidden();
      }
      activeFragment = (ImageFragment) object;
      activeFragment.onShown();
    }
  }

  @Override
  public int getCount() {
    // Return the search result count.
    if (listener.getSearchResult() == null) {
      return 0;
    }
    return listener.getSearchResult().getImages().length;
  }
  //endregion

  //region Static method: choosing ImageFragment type based on image file type
  /** Returns true if the {@link Image} object is a WebM/MP4 animation. */
  private static boolean shouldUseVideoPlayerFragment(Image image) {
    String fileExt = image.getFileExtension();
    return "mp4".equals(fileExt) || "webm".equals(fileExt);
  }
  //endregion

  //region Listener interface
  /** Interface used to interact with the {@link android.app.Activity} using this adapter. */
  public interface Listener {

    /** Get the {@link SearchResult} containing {@link Image}s displayed by this adapter. */
    public SearchResult getSearchResult();

  }
  //endregion
}
