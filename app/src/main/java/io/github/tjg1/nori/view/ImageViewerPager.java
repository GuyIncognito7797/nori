/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.view;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;

import android.view.View;

import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * View pager used in {@link io.github.tjg1.nori.ImageViewerActivity}. Gives touch event precedence to
 * multi-touch events sent to the {@link uk.co.senab.photoview.PhotoView} in the contained fragment.
 */
public class ImageViewerPager extends ViewPager {

  //region Constructors
  public ImageViewerPager(Context context) {
    super(context);
  }

  public ImageViewerPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }
  //endregion

  //region View methods (Touch Events)
  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    // PhotoView in ViewPager fix.
    try {
      return super.onInterceptTouchEvent(ev);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
  //endregion
}
