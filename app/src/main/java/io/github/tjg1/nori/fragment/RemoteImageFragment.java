/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.chrisbanes.photoview.OnViewTapListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.builder.AnimateGifMode;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.nori.BuildConfig;
import io.github.tjg1.nori.R;

/**
 * Fragment using the {@link PhotoView} widget
 * and the Picasso HTTP image loading library to display images.
 */
public class RemoteImageFragment extends ImageFragment {

    //region Instance fields
    /**
     * Progress bar used to display image fetch progress.
     */
    private ProgressBar progressBar;
    /**
     * PhotoView used to show images.
     */
    private PhotoView photoView;
    /**
     * TextView used to show image loading errors.
     */
    private TextView errorTextView;
    /**
     * Image loading Future.
     */
    private Future<?> imageLoadingFuture;
    //endregion

    //region Constructors

    /**
     * Required public empty constructor.
     */
    public RemoteImageFragment() {
    }
    //endregion

    //region Static methods (newInstance)

    /**
     * Factory method used to construct new fragments
     *
     * @param image Image object to display in the created fragment.
     * @return New RemoteImageFragment with the image object appended to its arguments bundle.
     */
    public static RemoteImageFragment newInstance(Image image) {
        // Create a new instance of the fragment.
        RemoteImageFragment fragment = new RemoteImageFragment();

        // Add the image object to the fragment's arguments Bundle.
        Bundle arguments = new Bundle();
        arguments.putParcelable(BUNDLE_ID_IMAGE, image);
        fragment.setArguments(arguments);

        return fragment;
    }
    //endregion

    //region Fragment methods (inflating view)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_remote_image, container, false);
        errorTextView = (TextView) view.findViewById(R.id.errorText);

        // Initialize the ProgressBar.
        this.progressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        // Initialize the ImageView widget.
        this.photoView = (PhotoView) view.findViewById(R.id.imageView);
        this.photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        this.photoView.setMaximumScale(4);
        this.photoView.setOnViewTapListener(new OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                listener.onViewTap(view, x, y);
            }
        });

        // Defer loading GIF images until the fragment is active.
        if (!"gif".equals(image.getFileExtension()) || this.isActive) {
            loadImage();
        }

        return view;
    }
    //endregion

    //region ViewPager onShown/onHidden triggers
    @Override
    public void onShown() {
        super.onShown();

        // Start loading the image, if it's not already loading.
        if (photoView != null && imageLoadingFuture == null) {
            loadImage();
        } else if (progressBar != null && progressBar.getProgress() < 100) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onHidden() {
        super.onHidden();

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }
    //endregion

    //region Loading images into image view

    /**
     * Load remote image into the ImageView.
     */
    private void loadImage() {
        // Show the progress bar.
        if (this.isActive) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // Load image into the view.
        String imageUrl = shouldLoadImageSamples() ? image.sampleUrl : image.fileUrl;
        imageLoadingFuture = Ion.with(this)
                .load(imageUrl)
                .progressBar(progressBar)
                .userAgent("nori/" + BuildConfig.VERSION_NAME)
                .addHeader("Referer", image.previewUrl)
                .withBitmap()
                .animateGif(AnimateGifMode.ANIMATE)
                //.deepZoom() // (disabled due to poor scaling quality)
                .intoImageView(photoView)
                .setCallback(new FutureCallback<ImageView>() {
                    @Override
                    public void onCompleted(Exception e, ImageView result) {
                        if (e != null) {
                            errorTextView.setVisibility(View.VISIBLE);
                            errorTextView.setText(e.getLocalizedMessage());
                        }
                        progressBar.setProgress(100); // for cached images.
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }
    //endregion
}
