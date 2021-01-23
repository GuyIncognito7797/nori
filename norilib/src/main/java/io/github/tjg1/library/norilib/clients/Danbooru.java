/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.clients;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;

/**
 * Client for the Danbooru 2.x API.
 */
public class Danbooru implements SearchClient {
    static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
    //region Constants
    /**
     * Number of images per search results page.
     * Best to use a large value to minimize number of unique HTTP requests.
     */
    private static final int DEFAULT_LIMIT = 100;
    /**
     * Thumbnail size set if not returned by the API.
     */
    private static final int THUMBNAIL_SIZE = 150;
    /**
     * Sample size set if not returned by the API.
     */
    private static final int SAMPLE_SIZE = 850;
    //endregion

    //region Service configuration instance fields
    /**
     * Android context.
     */
    protected final Context context;
    /**
     * Human-readable service name
     */
    protected final String name;
    /**
     * URL to the HTTP API Endpoint - the server implementing the API.
     */
    protected final String apiEndpoint;
    /**
     * Username used for authentication. (optional)
     */
    protected final String username;
    /**
     * API key used for authentication. (optional)
     */
    protected final String apiKey;
    //endregion

    //region Constructors

    /**
     * Create a new Danbooru 2.x client without authentication.
     *
     * @param name     Human-readable service name.
     * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
     */
    public Danbooru(Context context, String name, String endpoint) {
        this.context = context;
        this.name = name;
        this.apiEndpoint = endpoint;
        this.username = null;
        this.apiKey = null;
    }

    /**
     * Create a new Danbooru 1.x client with authentication.
     *
     * @param name     Human-readable service name.
     * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
     * @param username Username used for authentication.
     * @param apiKey   API key used for authentication.
     */
    public Danbooru(Context context, String name, String endpoint, String username, final String apiKey) {
        this.context = context;
        this.name = name;
        this.apiEndpoint = endpoint;
        this.username = username;
        this.apiKey = apiKey;
    }
    //endregion

    //region Service detection

    /**
     * Checks if the given URL exposes a supported API endpoint.
     *
     * @param context Android {@link Context}.
     * @param uri     URL to test.
     * @param timeout Timeout in milliseconds.
     * @return Detected endpoint URL. null, if no supported endpoint URL was detected.
     */
    @Nullable
    public static String detectService(@NonNull Context context, @NonNull Uri uri, int timeout) {
        final String endpointUrl = Uri.withAppendedPath(uri, "/post.json").toString();

        try {
            final Response<DataEmitter> response = Ion.with(context)
                    .load(endpointUrl)
                    .setTimeout(timeout)
                    .userAgent(SearchClient.USER_AGENT)
                    .followRedirect(false)
                    .noCache()
                    .asDataEmitter()
                    .withResponse()
                    .get();

            // Close the connection.
            final DataEmitter dataEmitter = response.getResult();
            if (dataEmitter != null) dataEmitter.close();

            if (response.getHeaders().code() == 200) {
                return uri.toString();
            }
        } catch (InterruptedException | ExecutionException ignored) {
        }
        return null;
    }
    //endregion

    /**
     * Create a {@link java.util.Date} object from String date representation used by this API.
     *
     * @param date Date string.
     * @return Date converted from given String.
     */
    protected static Date dateFromString(String date) throws ParseException {
        // Normalise the ISO8601 time zone into a format parse-able by SimpleDateFormat.
        if (!TextUtils.isEmpty(date)) {
            String newDate = date.replace("Z", "+0000");
            if (newDate.length() == 25) {
                newDate = newDate.substring(0, 22) + newDate.substring(23); // Remove timezone colon.
            }
            return DATE_FORMAT.parse(newDate);
        }
        return null;
    }

    //region SearchClient methods
    @Override
    public SearchResult search(String tags) throws IOException {
        // Return results for page 0.
        return search(tags, 0);
    }

    @Override
    public SearchResult search(String tags, int pid) throws IOException {
        try {
            return Ion.with(this.context)
                    .load(createSearchURL(tags, pid, DEFAULT_LIMIT))
                    .userAgent(SearchClient.USER_AGENT)
                    .as(new SearchResultParser(tags, pid))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            // Normalise exception to IOException, so method signatures are not tied to a single HTTP
            // library.
            throw new IOException(e);
        }
    }

    @Override
    public void search(String tags, SearchCallback callback) {
        // Return results for page 0.
        search(tags, 0, callback);
    }

    @Override
    public void search(final String tags, final int pid, final SearchCallback callback) {
        Ion.with(this.context)
                .load(createSearchURL(tags, pid, DEFAULT_LIMIT))
                .userAgent(SearchClient.USER_AGENT)
                .as(new SearchResultParser(tags, pid))
                .setCallback((e, result) -> {
                    if (e != null) {
                        callback.onFailure(new IOException(e));
                    } else {
                        callback.onSuccess(result);
                    }
                });
    }

    @Override
    public String getDefaultQuery() {
        // Show work-safe images by default.
        return "";
    }

    @Override
    public Settings getSettings() {
        return new Settings(Settings.APIType.DANBOARD, name, apiEndpoint, username, apiKey);
    }
    //endregion

    //region Creating search URLs

    @Override
    public AuthenticationType requiresAuthentication() {
        return AuthenticationType.OPTIONAL;
    }
    //endregion

    //region Parsing responses

    /**
     * Generate request URL to the search API endpoint.
     *
     * @param tags  Space-separated tags.
     * @param pid   Page number (0-indexed).
     * @param limit Images to fetch per page.
     * @return URL to search results API.
     */
    protected String createSearchURL(String tags, int pid, int limit) {
        // Page numbers are 1-indexed for this API.
        final int page = pid + 1;

        if (!TextUtils.isEmpty(this.username) && !TextUtils.isEmpty(this.apiKey)) {

            return String.format(Locale.US, "%s/posts.json?tags=%s&page=%d&limit=%d&login=%s&api_key=%s",
                    apiEndpoint, Uri.encode(tags), page, limit, Uri.encode(this.username), Uri.encode(this.apiKey));
        }
        return String.format(Locale.US,  "%s/posts.json?tags=%s&page=%d&limit=%d",
                apiEndpoint, Uri.encode(tags), page, limit);
    }

    protected SearchResult parseAPIResponse(String body, String tags, int offset) {
        return parseJSONResponse(body, tags, offset);
    }

    /**
     * Parse a JSON response returned by the API.
     *
     * @param body   HTTP Response body.
     * @param tags   Tags used to retrieve the response.
     * @param offset Current paging offset.
     * @return A {@link io.github.tjg1.library.norilib.SearchResult} parsed from given JSON.
     */
    @SuppressWarnings("FeatureEnvy")
    protected SearchResult parseJSONResponse(String body, String tags, int offset) {
        // Create variables to hold the values as JSON is being parsed.
        final List<Image> imageList = new ArrayList<>(DEFAULT_LIMIT);

        try {
            JSONArray jsonArray = new JSONArray(body);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = (JSONObject) jsonArray.get(i);
                final Image image = new Image();
                image.searchPage = offset;
                image.searchPagePosition = i;

                // Base level attributes
                image.id = jsonObject.get("id").toString();
                image.createdAt = dateFromString(jsonObject.get("created_at").toString());
                image.safeSearchRating = Image.SafeSearchRating.fromString(jsonObject.get("rating").toString());

                // File attributes
                image.fileUrl = jsonObject.get("file_url").toString();
                image.md5 = jsonObject.get("md5").toString();
                image.width = (int) jsonObject.get("image_width");
                image.height = (int) jsonObject.get("image_height");

                // Preview attributes
                image.previewUrl = jsonObject.get("preview_file_url").toString();
                // FIXME: API does not return thumbnail sizes.
                image.previewWidth = THUMBNAIL_SIZE;
                image.previewHeight = THUMBNAIL_SIZE;

                // Sample attributes
                image.sampleUrl = jsonObject.get("large_file_url").toString();
                // FIXME: API does not return sample sizes.
                image.sampleWidth = SAMPLE_SIZE;
                image.sampleHeight = SAMPLE_SIZE;

                // Sources
                image.source = jsonObject.get("source").toString();

                // Tags string
                String artistTags = jsonObject.get("tag_string_artist").toString();
                String characterTags = jsonObject.get("tag_string_character").toString();
                String copyrightTags = jsonObject.get("tag_string_copyright").toString();
                String generalTags = jsonObject.get("tag_string_general").toString();
                String metaTags = jsonObject.get("tag_string_meta").toString();

                Tag[] artistTagsArr = Tag.arrayFromString(artistTags, Tag.Type.ARTIST);
                Tag[] characterTagsArr = Tag.arrayFromString(characterTags, Tag.Type.CHARACTER);
                Tag[] copyrightTagsArr = Tag.arrayFromString(copyrightTags, Tag.Type.COPYRIGHT);
                Tag[] generalTagsArr = Tag.arrayFromString(generalTags, Tag.Type.GENERAL);
                Tag[] metaTagsArr = Tag.arrayFromString(metaTags, Tag.Type.GENERAL);

                // Create Tag arrays for each type
                image.tags = Tag.arrayFromTagArrays(artistTagsArr, characterTagsArr, copyrightTagsArr, generalTagsArr, metaTagsArr);

                image.webUrl = webUrlFromId(image.id);
                image.parentId = jsonObject.get("parent_id").toString();

                // Score attributes
                image.score = (int) jsonObject.get("score");

                imageList.add(image);
            }
        } catch (ParseException | JSONException e) {
            e.printStackTrace();
        }

        return new SearchResult(imageList.toArray(new Image[0]), Tag.arrayFromString(tags), offset);
    }

    /**
     * Get a URL viewable in the system web browser for given Image ID.
     *
     * @param id {@link io.github.tjg1.library.norilib.Image} ID.
     * @return URL for viewing the image in the browser.
     */
    protected String webUrlFromId(String id) {
        return String.format(Locale.US, "%s/posts/%s", apiEndpoint, id);
    }
    //endregion

    //region Ion async SearchResult parser

    /**
     * Asynchronous search parser to use with ion.
     */
    protected class SearchResultParser implements AsyncParser<SearchResult> {
        /**
         * Tags searched for.
         */
        protected final String tags;
        /**
         * Current page offset.
         */
        protected final int pageOffset;

        public SearchResultParser(String tags, int pageOffset) {
            this.tags = tags;
            this.pageOffset = pageOffset;
        }

        @Override
        public Future<SearchResult> parse(DataEmitter emitter) {
            return new StringParser().parse(emitter)
                    .then(new TransformFuture<SearchResult, String>() {
                        @Override
                        protected void transform(String result) {
                            setComplete(parseAPIResponse(result, tags, pageOffset));
                        }
                    });
        }

        @Override
        public void write(DataSink sink, SearchResult value, CompletedCallback completed) {
            // Not implemented.
        }

        @Override
        public Type getType() {
            return null;
        }
    }
    //endregion
}
