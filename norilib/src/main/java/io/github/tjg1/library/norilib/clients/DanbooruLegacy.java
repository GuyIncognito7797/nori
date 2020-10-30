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
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;

/**
 * Client for the Danbooru 1.x API.
 */
public class DanbooruLegacy implements SearchClient {
    //region Constants
    /**
     * Number of images per search results page.
     * Best to use a large value to minimize number of unique HTTP requests.
     */
    private static final int DEFAULT_LIMIT = 100;
    //endregion

    //region Service configuration instance fields
    /**
     * Android context.
     */
    protected final Context context;
    /**
     * Human-readable service name.
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
     * Password used for authentication. (optional)
     */
    protected final String password;
    //endregion

    //region Constructors

    protected static final List<String> XML_IMAGE_TAG_NAMES = Arrays.asList("tag", "post");

    /**
     * Create a new Danbooru 1.x client without authentication.
     *
     * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
     */
    public DanbooruLegacy(Context context, String name, String endpoint) {
        this.context = context;
        this.name = name;
        this.apiEndpoint = endpoint;
        this.username = null;
        this.password = null;
    }

    /**
     * Create a new Danbooru 1.x client with authentication.
     *
     * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
     * @param username Username used for authentication.
     * @param password Password used for authentication.
     */
    public DanbooruLegacy(Context context, String name, String endpoint, String username, String password) {
        this.context = context;
        this.name = name;
        this.apiEndpoint = endpoint;
        this.username = username;
        this.password = password;
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
        final String endpointUrl = Uri.withAppendedPath(uri, "/post/index.xml").toString();

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

    //region SearchClient methods
    @Override
    public SearchResult search(String tags) throws IOException {
        // Return results for page 0.
        return search(tags, 0);
    }

    @Override
    public SearchResult search(String tags, int pid) throws IOException {
        try {
            if (!TextUtils.isEmpty(this.username) && !TextUtils.isEmpty(this.password)) {
                return Ion.with(this.context)
                        .load(createSearchURL(tags, pid, DEFAULT_LIMIT))
                        .userAgent(SearchClient.USER_AGENT)
                        .basicAuthentication(this.username, this.password)
                        .as(new SearchResultParser(tags, pid))
                        .get();
            } else {
                return Ion.with(this.context)
                        .load(createSearchURL(tags, pid, DEFAULT_LIMIT))
                        .userAgent(SearchClient.USER_AGENT)
                        .as(new SearchResultParser(tags, pid))
                        .get();
            }
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
        // Define the ion callback. Not using FutureCallbacks as parameters, so the method signatures
        // are not tied to a single download library.
        FutureCallback<SearchResult> futureCallback = new FutureCallback<SearchResult>() {
            @Override
            public void onCompleted(Exception e, SearchResult result) {
                if (e != null) {
                    callback.onFailure(new IOException(e));
                } else {
                    callback.onSuccess(result);
                }
            }
        };

        // Handle authentication.
        if (!TextUtils.isEmpty(this.username) && !TextUtils.isEmpty(this.password)) {
            Ion.with(this.context)
                    .load(createSearchURL(tags, pid, DEFAULT_LIMIT))
                    .userAgent(SearchClient.USER_AGENT)
                    .basicAuthentication(this.username, this.password)
                    .as(new SearchResultParser(tags, pid))
                    .setCallback(futureCallback);
        } else {
            Ion.with(this.context)
                    .load(createSearchURL(tags, pid, DEFAULT_LIMIT))
                    .userAgent(SearchClient.USER_AGENT)
                    .as(new SearchResultParser(tags, pid))
                    .setCallback(futureCallback);
        }
    }

    @Override
    public String getDefaultQuery() {
        // Show all safe-for-work images by default.
        return "";
    }

    @Override
    public Settings getSettings() {
        return new Settings(Settings.APIType.DANBOARD_LEGACY, name, apiEndpoint, username, password);
    }

    @Override
    public AuthenticationType requiresAuthentication() {
        return AuthenticationType.OPTIONAL;
    }
    //endregion

    //region Creating Search URLs

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

        return String.format(Locale.US,  "%s/post/index.xml?tags=%s&limit=%d&page=%d",
                apiEndpoint, Uri.encode(tags), limit, page);
    }
    //endregion

    //region Parsing responses

    /**
     * Parse an XML response returned by the API.
     *
     * @param body   HTTP Response body.
     * @param tags   Tags used to retrieve the response.
     * @param offset Current paging offset.
     * @return A {@link io.github.tjg1.library.norilib.SearchResult} parsed from given XML.
     */
    protected SearchResult parseXMLResponse(String body, String tags, int offset) throws IOException {
        // Create variables to hold the values as XML is being parsed.
        final List<Image> imageList = new ArrayList<>(DEFAULT_LIMIT);
        int position = 0;

        try {
            // Create an XML parser factory and disable namespace awareness for security reasons.
            // See: (http://lists.w3.org/Archives/Public/public-xmlsec/2009Dec/att-0000/sws5-jensen.pdf).
            final XmlPullParserFactory xmlParserFactory = XmlPullParserFactory.newInstance();
            xmlParserFactory.setNamespaceAware(false);

            // Create a new XML parser and feed HTTP response data into it.
            final XmlPullParser xpp = xmlParserFactory.newPullParser();
            xpp.setInput(new StringReader(body));

            // Iterate over each XML element and handle pull parser "events".
            while (xpp.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (xpp.getEventType() == XmlPullParser.START_TAG) {
                    // At some point Paheal/Shimmie changed their Danbooru API to return images
                    // starting with <tag> posts. I don't know why and I don't trust them not to
                    // randomly change back so this .contains should be robust enough to handle it
                    if (XML_IMAGE_TAG_NAMES.contains(xpp.getName())) {
                        // <post> tags contain metadata for each image.
                        final Image image = new Image();
                        image.searchPage = offset;
                        image.searchPagePosition = position;

                        // Extract image metadata from XML attributes.
                        for (int i = 0; i < xpp.getAttributeCount(); i++) {
                            // Get name and value of current XML attribute.
                            final String name = xpp.getAttributeName(i);
                            final String value = xpp.getAttributeValue(i);

                            // Set the appropriate value for each tag name.
                            if ("file_url".equals(name)) {
                                image.fileUrl = normalizeUrl(value);
                            } else if ("width".equals(name)) {
                                image.width = Integer.parseInt(value);
                            } else if ("height".equals(name)) {
                                image.height = Integer.parseInt(value);
                            } else if ("preview_url".equals(name)) {
                                image.previewUrl = normalizeUrl(value);
                            } else if ("preview_width".equals(name)) {
                                image.previewWidth = Integer.parseInt(value);
                            } else if ("preview_height".equals(name)) {
                                image.previewHeight = Integer.parseInt(value);
                            } else if ("sample_url".equals(name)) {
                                image.sampleUrl = normalizeUrl(value);
                            } else if ("sample_width".equals(name)) {
                                image.sampleWidth = Integer.parseInt(value);
                            } else if ("sample_height".equals(name)) {
                                image.sampleHeight = Integer.parseInt(value);
                            } else if ("tags".equals(name)) {
                                image.tags = Tag.arrayFromString(value, Tag.Type.GENERAL);
                            } else if ("id".equals(name)) {
                                image.id = value;
                            } else if ("parent_id".equals(name)) {
                                image.parentId = value;
                            } else if ("rating".equals(name)) {
                                image.safeSearchRating = Image.SafeSearchRating.fromString(value);
                            } else if ("score".equals(name)) {
                                image.score = Integer.parseInt(value);
                            } else if ("md5".equals(name)) {
                                image.md5 = value;
                            } else if ("created_at".equals(name) || "date".equals(name)) {
                                try {
                                    image.createdAt = dateFromString(value);
                                } catch (ParseException e) {
                                    // There have been too many issues reported in Nori related to date parsing.
                                    // It's almost as if every site uses its own date format and, unfortunately,
                                    // I can't hard code all of them.
                                    image.createdAt = null;
                                }
                            }
                        }

                        // Append values not returned by the API.
                        image.webUrl = webUrlFromId(image.id);
                        image.pixivId = Image.getPixivIdFromUrl(image.source);
                        // Use original file if low-resolution sample does not exist.
                        if (image.sampleUrl == null) {
                            image.sampleUrl = image.fileUrl;
                            image.sampleWidth = image.width;
                            image.sampleHeight = image.height;
                        }
                        // Add Image to search result.
                        imageList.add(image);
                        position++;
                    }
                }
                // Get next XMLPullParser event.
                xpp.next();
            }
        } catch (XmlPullParserException e) {
            // Convert into IOException.
            // Needed for consistent method signatures in the SearchClient interface for different APIs.
            // (Throwing an XmlPullParserException would be fine, until dealing with an API using JSON, etc.)
            throw new IOException(e);
        }
        // Create and return a SearchResult.
        return new SearchResult(imageList.toArray(new Image[0]), Tag.arrayFromString(tags), offset);
    }

    /**
     * Convert a relative image URL to an absolute URL.
     *
     * @param url URL to convert.
     * @return Absolute URL.
     */
    protected String normalizeUrl(String url) throws java.net.MalformedURLException {
        // Return empty string for empty URLs.
        if (url == null || url.isEmpty()) {
            return "";
        }
        return new URL(new URL(apiEndpoint), url).toString();
    }

    /**
     * Get a URL viewable in the system web browser for given Image ID.
     *
     * @param id {@link io.github.tjg1.library.norilib.Image} ID.
     * @return URL for viewing the image in the browser.
     */
    protected String webUrlFromId(String id) {
        return apiEndpoint + "/post/show/" + id;
    }

    /**
     * Create a {@link java.util.Date} object from String date representation used by this API.
     *
     * @param date Date string.
     * @return Date converted from given String.
     */
    protected Date dateFromString(String date) throws ParseException {
        // Parser for the date format used by upstream Danbooru 1.x.
        final DateFormat DATE_FORMAT_DEFAULT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        if (TextUtils.isDigitsOnly(date)) {
            // Moebooru-based boards (Danbooru 1.x fork) use Unix timestamps.
            return new Date(Integer.parseInt(date));
        } else {
            return DATE_FORMAT_DEFAULT.parse(date);
        }
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
        private final String tags;
        /**
         * Current page offset.
         */
        private final int pageOffset;

        public SearchResultParser(String tags, int pageOffset) {
            this.tags = tags;
            this.pageOffset = pageOffset;
        }

        @Override
        public Future<SearchResult> parse(DataEmitter emitter) {
            return new StringParser().parse(emitter)
                    .then(new TransformFuture<SearchResult, String>() {
                        @Override
                        protected void transform(String result) throws Exception {
                            setComplete(parseXMLResponse(result, tags, pageOffset));
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
