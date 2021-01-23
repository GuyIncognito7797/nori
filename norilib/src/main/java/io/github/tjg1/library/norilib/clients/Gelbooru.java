/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.clients;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;

/**
 * {@link io.github.tjg1.library.norilib.clients.SearchClient} for the Gelbooru imageboard.
 */
public class Gelbooru extends Danbooru {
    //region Constants
    /**
     * Number of images to fetch with each search.
     */
    private static final int DEFAULT_LIMIT = 100;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM d HH:mm:ss Z yyyy", Locale.US);
    //endregion

    //region Constructors
    public Gelbooru(Context context, String name, String endpoint) {
        super(context, name, endpoint);
    }

    public Gelbooru(Context context, String name, String endpoint, String username, String password) {
        super(context, name, endpoint, username, password);
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
    public static String detectService(@NonNull Context context, @NonNull Uri uri, int timeout) {
        final String endpointUrl = Uri.withAppendedPath(uri, "/index.php?page=dapi&s=post&q=index").toString();
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
        } catch (InterruptedException | ExecutionException ignored) { }
        return null;
    }
    //endregion

    //region SearchClient methods
    @Override
    public Settings getSettings() {
        return new Settings(Settings.APIType.GELBOARD, name, apiEndpoint, username, apiKey);
    }
    //endregion

    @Override
    protected String createSearchURL(String tags, int pid, int limit) {
        // Unlike DanbooruLegacy, page numbers are 0-indexed for Gelbooru APIs.
        return String.format(Locale.US, "%s/index.php?page=dapi&s=post&q=index&tags=%s&pid=%d&limit=%d&json=1",
                apiEndpoint, Uri.encode(tags), pid, limit);
    }

    //region Parsing responses
    @Override
    protected String webUrlFromId(String id) {
        return String.format(Locale.US, "%s/index.php?page=post&s=view&id=%s",
                apiEndpoint, id);
    }

    @Override
    protected SearchResult parseAPIResponse(String body, String tags, int offset) {
        return parseJSONResponse(body, tags, offset);
    }

    @Override
    protected SearchResult parseJSONResponse(String body, String tags, int offset) {
        final List<Image> imageList = new ArrayList<>(DEFAULT_LIMIT);
        String sampleURL;
        int sampleWidth;
        int sampleHeight;

        try {
            JSONArray jsonArray = new JSONArray(body);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject post = (JSONObject) jsonArray.get(i);

                final Image image = new Image();
                image.searchPage = offset;
                image.searchPagePosition = i;

                // Base level attributes
                image.id = post.get("id").toString();
                try {
                    image.createdAt = dateFromString(post.get("created_at").toString());
                } catch (JSONException e) {
                    image.createdAt = null;
                }
                image.safeSearchRating = Image.SafeSearchRating.fromString(post.get("rating").toString());

                // File attributes
                image.fileUrl = post.get("file_url").toString();
                image.md5 = post.get("hash").toString();
                image.width = Integer.parseInt(post.get("width").toString());
                image.height = Integer.parseInt(post.get("height").toString());

                // Sample attributes
                boolean sampleBoolean;
                try {
                    // Gelbooru returns an integer here while...
                    sampleBoolean = (int) post.get("sample") == 1;
                } catch ( ClassCastException e ) {
                    // ... rule34.xxx returns a boolean!
                    sampleBoolean = (boolean) post.get("sample");
                }

                // TODO - This is a bit of a mess and could do with a cleanup.
                if ( sampleBoolean || post.has("sample_url")) {
                    if (post.has("sample_url")) {
                        sampleURL = post.getString("sample_url");
                    } else {
                        sampleURL = getSampleURL(image.fileUrl, post.get("image").toString(), image.md5);
                    }
                    sampleWidth = image.previewWidth = (int) post.get("sample_width");
                    sampleHeight = image.previewWidth = (int) post.get("sample_height");
                } else {
                    if (image.getFileExtension().equals("mp4") && apiEndpoint.contains("gelbooru")) {
                        sampleURL = getVideoSampleURL(image.fileUrl, image.md5);
                    } else {
                        sampleURL = image.fileUrl;
                    }
                    sampleWidth = image.width;
                    sampleHeight = image.height;
                }

                image.previewUrl = sampleURL;
                image.previewWidth = sampleWidth;
                image.previewHeight = sampleHeight;
                image.sampleUrl = sampleURL;
                image.sampleWidth = sampleWidth;
                image.sampleHeight = sampleHeight;

                // Sources
                try {
                    image.source = post.get("source").toString();
                } catch (JSONException e) {
                    image.source = null;
                }

                // TODO - Use Tag API to get tag types
                image.tags = Tag.arrayFromString(post.get("tags").toString());

                image.webUrl = webUrlFromId(image.id);
                image.parentId = post.get("parent_id").toString();

                // Score attributes
                image.score = (int) post.get("score");

                imageList.add(image);
            }
        } catch ( JSONException | ParseException e ) {
            Log.e("nori", Objects.requireNonNull(e.getMessage()));
        }

        return new SearchResult(imageList.toArray(new Image[0]), Tag.arrayFromString(tags), offset);
    }

    /**
     * Create a {@link java.util.Date} object from String date representation used by this API.
     *
     * @param date Date string.
     * @return Date converted from given String.
     */
    protected static Date dateFromString(String date) throws ParseException {
        // Normalise the ISO8601 time zone into a format parse-able by SimpleDateFormat.
        if (!TextUtils.isEmpty(date)) {
            return DATE_FORMAT.parse(date);
        }
        return null;
    }

    protected static String getSampleURL(String fileUrl, String filename, String hash) {
        fileUrl = fileUrl.replace("images", "samples")
                .replace(filename, "sample_" + hash + ".jpg");
        return fileUrl;
    }

    protected static String getVideoSampleURL(String fileUrl, String hash) {
        String[] urlParts = fileUrl.split("/");
        String fileName = "thumbnail_" + hash + ".jpg";
        // Gelbooru file URLs are in the format
        // <subdomain>.gelbooru.com/images/<2 character alphanumeric string>/<2 character alphanumeric string>/<hash>.<extension>
        // The thumbnail URL can be worked out by swapping out the sub-domain with "thumbs"
        // and the file name with "thumbnail_hash.jpg"
        String subdirOne = urlParts[urlParts.length - 3];
        String subdirTwo = urlParts[urlParts.length - 2];

        return String.format(
                "https://thumbs.gelbooru.com/%s/%s/%s",
                subdirOne,
                subdirTwo,
                fileName
        );
    }
}
