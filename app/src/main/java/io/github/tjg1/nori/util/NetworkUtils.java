/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.util;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;

/**
 * Network utility class.
 */
public abstract class NetworkUtils {

    //region Should download full-res images?

    /**
     * Decides if low-resolution ("sample") images should be fetched by default instead of full-size images, based on:
     * - Screen density
     * - Network link speed and quality
     * - Is the network metered? ($$$ per MB)
     *
     * @return true if low-resolution images should be used.
     */
    public static boolean shouldFetchImageSamples(Context context) {
        // Note that the low-resolution images aren't actually that bad unless the user zooms in on them.
        // They're meant for cases where the original image is much larger than an average desktop browser window.

        // Check screen resolution.
        if (context.getResources().getDisplayMetrics().density <= 1.0) {
            return true;
        }

        // Get system connectivity manager service.
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null) return true;

        // Check if network is metered.
        if (networkInfo.getType() != ConnectivityManager.TYPE_WIFI && isActiveNetworkMetered(cm)) {
            return true;
        }
        // Check link quality.
        return !isConnectionFast(networkInfo.getType(), networkInfo.getSubtype());
    }
    //endregion

    //region Should videos be playable?

    /**
     * Decides if WebM/MP4 files should be downloaded. Returns false, if the device is:
     * - On a metered Internet connection.
     * - On a slow mobile connection (less than 3G).
     *
     * @param context Activity context
     * @return True if videos can be downloaded.
     */
    public static boolean shouldDownloadVideos(Context context) {
        // Get system connectivity manager service.
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null) return false;

        // Check if network is metered.
        if (networkInfo.getType() != ConnectivityManager.TYPE_WIFI && isActiveNetworkMetered(cm)) {
            return false;
        }

        // Check link quality.
        return isConnectionFast(networkInfo.getType(), networkInfo.getSubtype());
    }
    //endregion

    //region Is connection metered/fast?

    /**
     * Check if active connection is metered. (API 16+)
     *
     * @param cm Instance of {@link android.net.ConnectivityManager}
     * @return true if user pays for bandwidth.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static boolean isActiveNetworkMetered(ConnectivityManager cm) {
        return cm.isActiveNetworkMetered();
    }

    /**
     * Check if given connection type is fast enough to download high res images.
     *
     * @param type    Connection type constant, as specified in {@link android.net.ConnectivityManager}.
     * @param subType Connection subtype constant, as specified in {@link android.telephony.TelephonyManager}.
     * @return true if high res images should be downloaded by default.
     */
    private static boolean isConnectionFast(int type, int subType) {
        if (type == ConnectivityManager.TYPE_WIFI || type == ConnectivityManager.TYPE_WIMAX) {
            // WiFi is good.
            return true;
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            // Exclude mobile network types with avg speeds below or close to ~5Mbps.
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_HSDPA: // 2-14 Mbps
                case TelephonyManager.NETWORK_TYPE_HSUPA: // 1-23 Mbps
                case TelephonyManager.NETWORK_TYPE_EVDO_B: // 5 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPAP: // 10-20 Mbps
                case TelephonyManager.NETWORK_TYPE_LTE: // 10+ Mbps
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }
    //endregion
}
