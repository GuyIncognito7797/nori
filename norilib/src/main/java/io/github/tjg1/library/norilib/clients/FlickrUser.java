/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.library.norilib.clients;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Flickr SearchClient limited to searching for images from a single user.
 */
public class FlickrUser extends Flickr {
    //region Regular expression patterns
    /**
     * Regex pattern used to match Flickr user URLs.
     */
    public static final String FLICKR_USER_REGEX = "^https?://(?:www\\.|m\\.)?flickr\\.com/(?:#/)?photos/(.+?)/?$";
    //endregion

    //region Constructors

    /**
     * Create a new Flickr API client.
     *
     * @param context     Android {@link Context}.
     * @param name        Human-readable service name. (i.e. Flickr)
     * @param apiEndpoint API endpoint. (i.e. https://api.flickr.com/services)
     */
    public FlickrUser(Context context, String name, String apiEndpoint) {
        super(context, name, apiEndpoint);
    }
    //endregion

    //region Service detection

    /**
     * Checks if the given URL exposes a supported API endpoint.
     *
     * @param uri URL to test.
     * @return Detected endpoint URL. null, if no supported endpoint URL was detected.
     */
    @Nullable
    public static String detectService(@NonNull Uri uri) {
        if (uri.toString().matches(FLICKR_USER_REGEX))
            return "https://" + uri.getHost() + uri.getPath();
        return null;
    }
    //endregion

    //region SearchClient methods
    @Override
    public Settings getSettings() {
        return new Settings(Settings.APIType.FLICKR_USER, name, apiEndpoint.toString());
    }
    //endregion

    //region Creating search URLs

    /**
     * Generate request URL to the search API endpoint.
     *
     * @param tags Space-separated tags.
     * @param pid  Page number (0-indexed).
     * @return URL to search results API.
     */
    @Override
    protected String createSearchURL(String tags, int pid) {
        Pattern p = Pattern.compile(FLICKR_USER_REGEX);
        Matcher m = p.matcher(apiEndpoint.toString());

        if (m.matches()) {
            return new Uri.Builder()
                    .scheme(FLICKR_API_ENDPOINT.getScheme())
                    .authority(FLICKR_API_ENDPOINT.getAuthority())
                    .path(FLICKR_API_ENDPOINT.getPath())
                    .appendQueryParameter("api_key", FLICKR_API_KEY)
                    .appendQueryParameter("user_id", m.group(1))
                    .appendQueryParameter("method", !TextUtils.isEmpty(tags) ? "flickr.photos.search" : "flickr.people.getPhotos")
                    .appendQueryParameter("text", tags != null ? tags : "")
                    .appendQueryParameter("per_page", Integer.toString(DEFAULT_LIMIT, 10))
                    .appendQueryParameter("extras", "date_upload,owner_name,media,tags,path_alias,icon_server,o_dims,path_alias,original_format,url_q,url_m,url_l,url_o")
                    .appendQueryParameter("page", Integer.toString(pid + 1, 10))
                    .build()
                    .toString();
        }
        return super.createSearchURL(tags, pid);
    }
    //endregion
}
