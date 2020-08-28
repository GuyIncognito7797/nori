/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.fragment;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.nori.BuildConfig;
import io.github.tjg1.nori.R;
import io.github.tjg1.nori.util.NetworkUtils;

//import android.support.design.widget.Snackbar;

/**
 * A fragment for playing back MP4 and WebM videos in {@link io.github.tjg1.nori.ImageViewerActivity}.
 */
public class VideoPlayerFragment extends ImageFragment {

    //region Instance fields
    /**
     * VideoView used to play the video.
     */
    private VideoView videoView;
    /**
     * True if the video player already has a video loaded.
     */
    private boolean isPrepared = false;

    /**
     * Gesture detector used to detect single taps (to toggle the ActionBar).
     */
    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (listener != null) {
                listener.onViewTap(videoView, e.getX(), e.getY());
            }
            return true;
        }
    };
    //endregion

    //region Constructors

    /**
     * Required public empty constructor.
     */
    public VideoPlayerFragment() {
    }
    //endregion

    //region Static methods (newInstance)

    /**
     * Factory method used to construct new fragments
     *
     * @param image {@link Image} (video) object to display in the created fragment.
     * @return New VideoPlayerFragment with the {@link Image} object appended to its arguments bundle.
     */
    public static VideoPlayerFragment newInstance(Image image) {
        // Create a new instance of the fragment.
        VideoPlayerFragment fragment = new VideoPlayerFragment();

        // Add the image object to the fragment's arguments Bundle.
        Bundle arguments = new Bundle();
        arguments.putParcelable(BUNDLE_ID_IMAGE, image);
        fragment.setArguments(arguments);

        return fragment;
    }
    //endregion

    //region Fragment methods (Lifecycle)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video_player, container, false);

        // Detect single taps to toggle the ActionBar.
        final GestureDetector gestureDetector = new GestureDetector(getContext(), gestureListener);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector.onTouchEvent(motionEvent);
                return true;
            }
        });

        videoView = (VideoView) view.findViewById(R.id.video_view);
        if (isActive) {
            preparePlayerAndStartPlayback();
        }

        return view;
    }
    //endregion

    //region ViewPager onShown/onHidden triggers

    /**
     * Called by the FragmentStatePagerAdapter when this fragment is currently the primary item
     * (shown to the user).
     */
    @Override
    public void onShown() {
        super.onShown();
        if (videoView != null) {
            preparePlayerAndStartPlayback();
        }
    }

    /**
     * Called by the FragmentStatePagerAdapter when this fragment is scrolled away (hidden).
     */
    @Override
    public void onHidden() {
        super.onHidden();
        if (videoView != null) {
            videoView.pause();
        }
    }
    //endregion

    //region Initializing video playback

    /**
     * Used to pass the media URL to the {@link VideoView} to start downloading.
     */
    private void preparePlayerAndStartPlayback() {
        if (VideoPlayerFragment.this.isPrepared) {
            videoView.start();
        } else if (!NetworkUtils.shouldDownloadVideos(getContext())) {
            View view = getView();
            if (view != null) {
                Snackbar.make(view, R.string.toast_error_videoMeteredConnection, Snackbar.LENGTH_LONG)
                        .show();
            }
        } else {
            // Set the video URL and user agent.
            HashMap<String, String> headers = new HashMap<>(1);
            headers.put("User-Agent", "nori/" + BuildConfig.VERSION_NAME);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                videoView.setVideoURI(Uri.parse(image.fileUrl), headers);
            } else {
                videoView.setVideoURI(Uri.parse(image.fileUrl));
            }
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    // Start video, if the fragment is active.
                    VideoPlayerFragment.this.isPrepared = true;
                    if (VideoPlayerFragment.this.isActive) {
                        // TODO: Progress bar.
                        mediaPlayer.start();
                    }
                }
            });
            videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    // Loop the video.
                    if (VideoPlayerFragment.this.isPrepared) {
                        mediaPlayer.start();
                    }
                }
            });
        }
    }
    //endregion
}
