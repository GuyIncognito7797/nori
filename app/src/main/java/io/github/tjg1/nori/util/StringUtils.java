/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.nori.util;

/**
 * Class of utility methods related to Java {@link String}s.
 */
public interface StringUtils {

    //region Merge array into string

    /**
     * Merge a string array into a single string. Does the opposite of {@link String#split(String)}.
     *
     * @param array     String array to merge.
     * @param separator Separator to use.
     * @return String merged from array using the given separator.
     */
    public static String mergeStringArray(String[] array, String separator) {
        // Return empty string, if array is null or empty.
        if (array == null || array.length == 0) {
            return "";
        }

        // Create the merged string.
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            // Append separator if current element is not last.
            if (i != (array.length - 1)) {
                sb.append(separator);
            }
        }

        // Return merged string.
        return sb.toString();
    }
    //endregion
}
