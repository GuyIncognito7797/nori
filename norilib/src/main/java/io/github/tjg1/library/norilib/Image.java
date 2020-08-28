/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadata received from the API for each image.
 */
public class Image implements Parcelable {

    //region Parcelable
    /**
     * Class loader used when deserializing from a {@link Parcel}.
     */
    public static final Parcelable.Creator<Image> CREATOR = new Parcelable.Creator<Image>() {

        @Override
        public Image createFromParcel(Parcel source) {
            // Use the Parcel constructor.
            return new Image(source);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };

    /**
     * Create a new Image by deserializing it from a {@link android.os.Parcel}.
     *
     * @param in {@link android.os.Parcel} used to deserialize the image.
     */
    protected Image(Parcel in) {
        // Deserialize values from Parcel.
        fileUrl = in.readString();
        width = in.readInt();
        height = in.readInt();
        previewUrl = in.readString();
        previewWidth = in.readInt();
        previewHeight = in.readInt();
        sampleUrl = in.readString();
        sampleWidth = in.readInt();
        sampleHeight = in.readInt();
        tags = in.createTypedArray(Tag.CREATOR);
        id = in.readString();
        parentId = in.readString();
        webUrl = in.readString();
        pixivId = in.readString();
        safeSearchRating = SafeSearchRating.values()[in.readInt()];
        score = in.readInt();
        source = in.readString();
        md5 = in.readString();
        final int tmpSearchPage = in.readInt();
        searchPage = (tmpSearchPage != -1) ? tmpSearchPage : null;
        final int tmpSearchPagePosition = in.readInt();
        searchPagePosition = (tmpSearchPagePosition != -1) ? tmpSearchPagePosition : null;
        final long tmpCreatedAt = in.readLong();
        createdAt = (tmpCreatedAt != -1) ? new Date(tmpCreatedAt) : null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Serialize data into a Parcel.
        dest.writeString(fileUrl); //
        dest.writeInt(width); //
        dest.writeInt(height); //
        dest.writeString(previewUrl); //
        dest.writeInt(previewWidth); //
        dest.writeInt(previewHeight); //
        dest.writeString(sampleUrl); //
        dest.writeInt(sampleWidth); //
        dest.writeInt(sampleHeight); //
        dest.writeTypedArray(tags, 0); //
        dest.writeString(id); //
        dest.writeString(parentId); //
        dest.writeString(webUrl); //
        dest.writeString(pixivId); //
        dest.writeInt(safeSearchRating.ordinal());
        dest.writeInt(score);
        dest.writeString(source);
        dest.writeString(md5);
        dest.writeInt(searchPage != null ? searchPage : -1);
        dest.writeInt(searchPagePosition != null ? searchPagePosition : -1);
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1L);
    }
    //endregion

    //region Regular expressions
    /**
     * Regular expression for matching Pixiv image ID from Pixiv URLs
     */
    private static final Pattern PIXIV_ID_FROM_URL_PATTERN = Pattern.compile("http://(?:www|i\\d)\\.pixiv\\.net/.+?(?:illust_id=|img/.+?/)(\\d+)");
    //endregion

    //region Instance fields
    /**
     * Full-resolution image URL.
     */
    public String fileUrl;
    /**
     * Image width.
     */
    public int width;
    /**
     * Image height.
     */
    public int height;
    /**
     * Thumbnail URL.
     */
    public String previewUrl;

    // Samples are medium-resolution images downsized for viewing on the web.
    // Usually no more than ~1000px width.
    // Suitable for slow networks and low resolution devices (mdpi or less).
    /**
     * Thumbnail width.
     */
    public int previewWidth = 0;
    /**
     * Thumbnail height
     */
    public int previewHeight = 0;
    /**
     * Sample URL.
     */
    public String sampleUrl;
    /**
     * Sample width.
     */
    public int sampleWidth = 0;
    /**
     * Sample height.
     */
    public int sampleHeight = 0;
    /**
     * Image tags.
     */
    public Tag[] tags;
    /**
     * Image ID
     */
    public String id;
    /**
     * Image parent ID. Used when there are multiple similar images.
     */
    public String parentId;
    /**
     * Parent ID
     */
    public String pixivId;
    /**
     * Web URL.
     */
    public String webUrl;
    /**
     * Source URL.
     */
    public String source;
    /**
     * MD5 hash
     */
    public String md5;
    /**
     * Search result page that contains this Image.
     */
    public Integer searchPage;
    /**
     * The position of the Image on the search result page.
     */
    public Integer searchPagePosition;
    /**
     * SafeSearch rating.
     */
    public SafeSearchRating safeSearchRating;

    // Parcelables are the standard Android serialization API used to retain data between sessions.
    /**
     * Popularity score.
     */
    public Integer score;
    /**
     * Upload date.
     */
    public Date createdAt;
    //endregion

    //region Constructors

    /**
     * Default constructor
     */
    public Image() {
    }
    //endregion

    //region Static method: Pixiv URLs from IDs

    /**
     * Extract a Pixiv ID from URL to an image's Pixiv page.
     *
     * @param url Pixiv URL.
     * @return Pixiv ID. Null if an ID could not be matched.
     */
    public static String getPixivIdFromUrl(String url) {
        // Make sure the URL isn't empty or null.
        if (url == null || url.isEmpty()) {
            return null;
        }

        // Match regular expression against URL.
        Matcher matcher = PIXIV_ID_FROM_URL_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // No ID matched.
        return null;
    }
    //endregion

    //region File extension from URL

    /**
     * Attempts to *guess* the file type of the {@link Image} based on the File URL.
     * May not be accurate for API types that don't include the file extension in image file names.
     *
     * @return Lower-case file extension, without the preceding dot. "jpeg" gets normalised into
     * "jpg".
     */
    @Nullable
    public String getFileExtension() {
        String path = Uri.parse(this.fileUrl).getLastPathSegment();
        String fileExt = (!TextUtils.isEmpty(path) && path.contains(".")) ?
                path.toLowerCase(Locale.US).substring(path.lastIndexOf('.') + 1) : null;

        return "jpeg".equals(fileExt) ? "jpg" : fileExt;
    }
    //endregion

    //region SafeSearchRating enum

    /**
     * Safe-for-work ratings.
     * Users can choose to hide images with certain SafeSearch ratings.
     */
    public enum SafeSearchRating {
        /**
         * Image is safe for work.
         */
        S,
        /**
         * Image is generally safe, but may contain some suggestive content
         */
        Q,
        /**
         * Image is not safe for work.
         */
        E,
        /**
         * Rating is unknown or has not been set.
         */
        U;

        //region Static helper methods

        /**
         * Convert a String array into an array of {@link SafeSearchRating}s.
         *
         * @param strings String array.
         * @return Array of {@link SafeSearchRating}s.
         */
        public static SafeSearchRating[] arrayFromStrings(String... strings) {
            final List<SafeSearchRating> ratingList = new ArrayList<>(4);

            for (String string : strings) {
                string = string.toLowerCase(Locale.US);
                if (string.contains("f")) { // saFe
                    ratingList.add(SafeSearchRating.S);
                } else if (string.contains("q")) { // Questionable
                    ratingList.add(SafeSearchRating.Q);
                } else if (string.contains("x")) { // eXplicit
                    ratingList.add(SafeSearchRating.E);
                } else if (string.contains("u")) { // Undefined
                    ratingList.add(SafeSearchRating.U);
                }
            }

            return ratingList.toArray(new SafeSearchRating[ratingList.size()]);
        }

        /**
         * Get a SafeSearchRating from a raw String representation returned by the API.
         *
         * @param s String returned by the API.
         * @return SafeSearchRating for given value.
         */
        public static SafeSearchRating fromString(String s) {
            // Convert string to lower-case and look at first character only.
            switch (s.toLowerCase(Locale.US).charAt(0)) {
                case 's':
                    return SafeSearchRating.S;
                case 'q':
                    return SafeSearchRating.Q;
                case 'e':
                    return SafeSearchRating.E;
                default:
                    return SafeSearchRating.U;
            }
        }
        //endregion
    }
    //endregion
}
