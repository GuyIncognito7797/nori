/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: GNU GPLv2
 */

package io.github.tjg1.library.norilib.util;

import android.text.TextUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cryptographic hashing utils.
 */
public interface HashUtils {

    //region SHA512 helper method

    /**
     * SHA-512 hashing function.
     *
     * @param plaintext Plaintext to hash.
     * @param salt      Salt to append to the plaintext before hashing.
     * @return Hashed hex string.
     */
    public static String sha512(String plaintext, String salt) {
        final String s = !TextUtils.isEmpty(salt) ? plaintext + salt : plaintext;

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(s.getBytes());
            byte[] byteData = md.digest();

            // Convert to hex.
            StringBuilder hashBuffer = new StringBuilder();
            for (byte aByteData : byteData) {
                hashBuffer.append(Integer.toString((aByteData & 0xff) + 0x100, 16).substring(1));
            }
            return hashBuffer.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
    //endregion
}
