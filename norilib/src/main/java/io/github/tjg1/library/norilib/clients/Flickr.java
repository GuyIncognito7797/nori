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

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.ion.Ion;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;

/**
 * Search client for the Flickr API.
 */
public class Flickr implements SearchClient {

    //region Constants
    /**
     * Number of images to fetch per page.
     */
    protected static final int DEFAULT_LIMIT = 100;
    /**
     * Default API endpoint.
     */
    public static final Uri FLICKR_API_ENDPOINT = Uri.parse("https://api.flickr.com/services/rest");
    /**
     * Public API key used to access Flickr services.
     */
    protected static final String FLICKR_API_KEY = "6b74179518fc00c8bef70b230c7ee880";
    //endregion

    //region Instance fields
    /**
     * Android context.
     */
    protected final Context context;
    /**
     * Human readable service name.
     */
    protected final String name;
    /**
     * API Endpoint.
     */
    protected final Uri apiEndpoint;
    //endregion

    //region Constructors

    /**
     * Create a new Flickr API client.
     *
     * @param context     Android {@link Context}.
     * @param name        Human-readable service name. (i.e. Flickr)
     * @param apiEndpoint API endpoint. (i.e. https://api.flickr.com/services)
     */
    public Flickr(Context context, String name, String apiEndpoint) {
        this.context = context;
        this.name = name;
        this.apiEndpoint = apiEndpoint != null ? Uri.parse(apiEndpoint) : FLICKR_API_ENDPOINT;
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
        final String host = uri.getHost();

        // Check for hardcoded URL.
        if ("api.flickr.com".equals(host))
            return FLICKR_API_ENDPOINT.toString();
        return null;
    }
    //endregion

    //region SearchClient methods

    /**
     * Fetch first page of results containing images with the given set of tags.
     *
     * @param tags Search query. A space-separated list of tags.
     * @return A {@link SearchResult} containing a set of Images.
     * @throws IOException Network error.
     */
    @Override
    public SearchResult search(String tags) throws IOException {
        // Return results for page 0.
        return search(tags, 0);
    }

    /**
     * Search for images with the given set of tags.
     *
     * @param tags Search query. A space-separated list of tags.
     * @param pid  Page number. (zero-indexed)
     * @return A {@link SearchResult} containing a set of Images.
     * @throws IOException Network error.
     */
    @Override
    public SearchResult search(String tags, int pid) throws IOException {
        try {
            return Ion.with(this.context)
                    .load(createSearchURL(tags, pid))
                    .userAgent(SearchClient.USER_AGENT)
                    .as(new SearchResultParser(tags, pid))
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            // Normalise exception to IOException, so method signatures are not tied to a single HTTP
            // library.
            throw new IOException(e);
        }
    }

    /**
     * Asynchronously fetch first page of results containing image with the given set of tags.
     *
     * @param tags     Search query. A space-separated list of tags.
     * @param callback Callback listening for the SearchResult returned in the background.
     */
    @Override
    public void search(String tags, SearchCallback callback) {
        // Return results for page 0.
        search(tags, 0, callback);
    }

    /**
     * Asynchronously search for images with the given set of tags.
     *
     * @param tags     Search query. A space-separated list of tags.
     * @param pid      Page number. (zero-indexed)
     * @param callback Callback listening for the SearchResult returned in the background.
     */
    @Override
    public void search(String tags, int pid, final SearchCallback callback) {
        Ion.with(this.context)
                .load(createSearchURL(tags, pid))
                .userAgent(SearchClient.USER_AGENT)
                .as(new SearchResultParser(tags, pid))
                .setCallback(new FutureCallback<SearchResult>() {
                    @Override
                    public void onCompleted(Exception e, SearchResult result) {
                        if (e != null) {
                            callback.onFailure(new IOException(e));
                        } else {
                            callback.onSuccess(result);
                        }
                    }
                });
    }

    /**
     * Get a SafeSearch default query to search for when an app is launched.
     *
     * @return Safe-for-work query to search for when an app is launched.
     */
    @Override
    public String getDefaultQuery() {
        return "";
    }

    /**
     * Get a serializable {@link Settings} object with this
     * {@link SearchClient}'s settings.
     *
     * @return A serializable {@link Settings} object.
     */
    @Override
    public Settings getSettings() {
        return new Settings(Settings.APIType.FLICKR, name, apiEndpoint.toString());
    }

    /**
     * Check if the API server requires or supports optional authentication.
     * <p/>
     * This is used in the API server settings activity as follows:
     * If REQUIRED, the user will need to supply valid credentials.
     * If OPTIONAL, the credential form is shown, but can be left empty.
     * If NONE, the credential form will not be shown.
     *
     * @return {@link AuthenticationType} value for this API backend.
     */
    @Override
    public AuthenticationType requiresAuthentication() {
        return AuthenticationType.NONE;
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
    protected String createSearchURL(String tags, int pid) {
        return new Uri.Builder()
                .scheme(apiEndpoint.getScheme())
                .authority(apiEndpoint.getAuthority())
                .path(apiEndpoint.getPath())
                .appendQueryParameter("api_key", FLICKR_API_KEY)
                .appendQueryParameter("method", !TextUtils.isEmpty(tags) ? "flickr.photos.search" : "flickr.interestingness.getList")
                .appendQueryParameter("text", tags != null ? tags : "")
                .appendQueryParameter("per_page", Integer.toString(DEFAULT_LIMIT, 10))
                .appendQueryParameter("extras", "date_upload,owner_name,media,tags,path_alias,icon_server,o_dims,path_alias,original_format,url_q,url_m,url_l,url_o")
                .appendQueryParameter("page", Integer.toString(pid + 1, 10))
                .build()
                .toString();
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
        final List<Image> imageList = new ArrayList<>(DEFAULT_LIMIT);

        try {
            Document doc = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(new InputSource(new StringReader(body)));

            NodeList nodeList = doc.getElementsByTagName("photo");

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;

                    final Image image = new Image();
                    image.searchPage = offset;
                    image.searchPagePosition = i;

                    String urlQ = element.getAttribute("url_q");
                    String urlM = element.getAttribute("url_m");
                    String urlL = element.getAttribute("url_l");
                    String urlO = element.getAttribute("url_o");

                    // Set file url.
                    if (!TextUtils.isEmpty(urlO)) {
                        image.fileUrl = urlO;
                        image.width = Integer.parseInt(element.getAttribute("width_o"));
                        image.height = Integer.parseInt(element.getAttribute("height_o"));
                    } else if (!TextUtils.isEmpty(urlL)) {
                        image.fileUrl = urlL;
                        image.width = Integer.parseInt(element.getAttribute("width_l"));
                        image.height = Integer.parseInt(element.getAttribute("height_l"));
                    } else if (!TextUtils.isEmpty(urlM)) {
                        image.fileUrl = urlM;
                        image.width = Integer.parseInt(element.getAttribute("width_m"));
                        image.height = Integer.parseInt(element.getAttribute("height_m"));
                    }

                    // Set sample url.
                    if (!TextUtils.isEmpty(urlL)) {
                        image.sampleUrl = urlL;
                        image.sampleWidth = Integer.parseInt(element.getAttribute("width_l"));
                        image.sampleHeight = Integer.parseInt(element.getAttribute("height_l"));
                    } else if (!TextUtils.isEmpty(urlM)) {
                        image.sampleUrl = urlM;
                        image.sampleWidth = Integer.parseInt(element.getAttribute("width_m"));
                        image.sampleHeight = Integer.parseInt(element.getAttribute("height_m"));
                    }

                    // Set preview url.
                    if (!TextUtils.isEmpty(urlQ)) {
                        image.previewUrl = urlQ;
                        image.previewWidth = Integer.parseInt(element.getAttribute("width_q"));
                        image.previewHeight = Integer.parseInt(element.getAttribute("height_q"));
                    }

                    image.tags = Tag.arrayFromString(element.getAttribute("tags"));
                    image.id = element.getAttribute("id");
                    image.webUrl = webUrlFromId(element.getAttribute("owner"), element.getAttribute("id"));
                    image.parentId = null;
                    image.safeSearchRating = Image.SafeSearchRating.S;
                    image.score = 0;
                    image.md5 = "2d57d21f35e060a4c5e81c03aea3efa8"; // not implemented
                    image.createdAt = new Date(Long.parseLong(element.getAttribute("dateupload"), 10) * 1000);

                    imageList.add(image);
                }
            }

        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(e);
        }

        return new SearchResult(imageList.toArray(new Image[imageList.size()]), Tag.arrayFromString(tags), offset);
    }


    /**
     * Create Flickr web url for given user and photo id.
     */
    protected String webUrlFromId(String userId, String photoId) {
        return "https://www.flickr.com/photos/" + userId + "/" + photoId;
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
