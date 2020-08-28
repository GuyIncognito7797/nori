/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori;


import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.snackbar.Snackbar;

public class WebViewActivity extends AppCompatActivity {

    //region Intent extra keys
    /**
     * Intent extra used to pass the URL to display in the {@link WebView}.
     */
    protected static final String INTENT_EXTRA_URL = "WebViewActivity.URL";
    /**
     * Intent extra used to set the title of this {@link android.app.Activity}.
     */
    protected static final String INTENT_EXTRA_TITLE = "WebViewActivity.TITLE";
    //endregion

    //region Instance fields (Views)
    /**
     * Progress bar used to display fetch progress.
     */
    private ProgressBar mProgressBar;
    //endregion

    //region Instance fields (WebView clients)
    /**
     * WebView client used to intercept webview events.
     */
    private final WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        @SuppressWarnings("deprecation")
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Snackbar.make(findViewById(R.id.root), R.string.toast_error_noNetwork,
                    Snackbar.LENGTH_SHORT).show();
            WebViewActivity.this.finish();
        }

        @TargetApi(android.os.Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            onReceivedError(view, error.getErrorCode(), error.getDescription().toString(), request.getUrl().toString());
        }

        @Override
        @SuppressWarnings("deprecation")
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); // Open links in relevant app.
            } catch (ActivityNotFoundException ignored) {
                Snackbar.make(findViewById(R.id.root), R.string.toast_error_noApplicationFound,
                        Snackbar.LENGTH_LONG).show();
            }
            return true;
        }

        @TargetApi(android.os.Build.VERSION_CODES.M)
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return shouldOverrideUrlLoading(view, request.getUrl().toString());
        }
    };
    /**
     * WebView Chrome client used to intercept progress events.
     */
    private final WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress < 100 && mProgressBar != null) {
                mProgressBar.setVisibility(View.VISIBLE);
                mProgressBar.setIndeterminate(false);
                mProgressBar.setProgress(newProgress);
            } else if (newProgress == 100) {
                mProgressBar.setVisibility(View.GONE);
            }
        }
    };
    //endregion

    //region Activity lifecycle
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        Intent intent = getIntent();
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        // Set up the web view.
        WebView webView = setUpWebView((WebView) findViewById(R.id.webView));

        // Set activity title from intent.
        if (intent.hasExtra(INTENT_EXTRA_TITLE)) {
            setTitle(intent.getStringExtra(INTENT_EXTRA_TITLE));
        }

        // Get URL from the intent used to start the activity.
        if (intent.hasExtra(INTENT_EXTRA_URL)) {
            webView.loadUrl(intent.getStringExtra(INTENT_EXTRA_URL));
        } else if ("io.github.tjg1.nori.ABOUT".equals(intent.getAction())) {
            mProgressBar.setVisibility(View.VISIBLE);
            webView.loadUrl("https://tjg1.github.io/nori/about.html?version=" + Uri.encode(BuildConfig.VERSION_NAME));
        } else {
            this.finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    //endregion

    //region Activity methods
    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }
    //endregion

    //region WebView Setup

    /**
     * Sets the default preferences of the {@link WebView} used to display content of this activity.
     *
     * @param webView {@link WebView} to configure.
     * @return Configured WebView instance.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private WebView setUpWebView(WebView webView) {
        webView.setWebViewClient(mWebViewClient);
        webView.setWebChromeClient(mWebChromeClient);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        return webView;
    }
    //endregion
}
