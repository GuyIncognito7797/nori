/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.test;

import android.os.Bundle;
import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.tjg1.library.norilib.Tag;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Tests for the {@link io.github.tjg1.library.norilib.Tag} class.
 */
public class TagTests extends AndroidTestCase {

    /**
     * Verify that Tags are read and written to Parcels correctly.
     */
    public void testWriteToParcel() throws Throwable {
        final Tag original = new Tag("quack", Tag.Type.COPYRIGHT);
        final Bundle bundle = new Bundle();
        final Tag unParceled;

        // Write a Tag to a parcel and create a new Tag from the parceled data.
        bundle.putParcelable("tag", original);
        unParceled = bundle.getParcelable("tag");

        // Compare data between original and un-parceled tag.
        assertThat(unParceled.getName()).isEqualTo(original.getName());
        assertThat(unParceled.getType()).isEqualTo(original.getType());
    }

    /**
     * Tests the {@link Tag#equals(Object)}  method.
     */
    public void testEquals() throws Throwable {
        final Tag tag1 = new Tag("duck", Tag.Type.CHARACTER);
        final Tag tag2 = new Tag("duck", Tag.Type.CHARACTER);
        final Tag tag3 = new Tag("bird", Tag.Type.CHARACTER);
        final Tag tag4 = new Tag("duck", Tag.Type.ARTIST);

        // Two tags with the same name and type should be equal to one another.
        assertThat(tag1.equals(tag2)).isTrue();
        assertThat(tag1.equals(tag3)).isFalse();
        assertThat(tag1.equals(tag4)).isFalse();
    }

    /**
     * Tests sorting collections of Tags using the {@link Tag#compareTo(Tag)} method.
     */
    public void testCompareTo() throws Throwable {
        final List<Tag> tagList = new ArrayList<>();
        tagList.add(new Tag("duck", Tag.Type.CHARACTER));
        tagList.add(new Tag("quack", Tag.Type.GENERAL));
        tagList.add(new Tag("bird", Tag.Type.ARTIST));

        // Sort the list.
        Collections.sort(tagList);
        // Make sure tags are sorted alphabetically.
        assertThat(tagList.get(0).getName()).isEqualTo("bird");
        assertThat(tagList.get(1).getName()).isEqualTo("duck");
        assertThat(tagList.get(2).getName()).isEqualTo("quack");
    }

    /**
     * Test creating space-separated queries using the {@link Tag#stringFromArray(Tag[])} method.
     */
    public void testStringFromArray() throws Throwable {
        final String tags = Tag.stringFromArray(new Tag[]{new Tag("duck"), new Tag("quack")});
        assertThat(tags).isEqualTo("duck quack");
    }

    /**
     * Test creating tag arrays from space-separated queries using the {@link Tag#arrayFromString(String) method.
     */
    public void testArrayFromString() throws Throwable {
        final Tag[] tags = Tag.arrayFromString("duck quack", Tag.Type.CHARACTER);
        assertThat(tags).hasSize(2);
        assertThat(tags[0]).isEqualTo(new Tag("duck", Tag.Type.CHARACTER));
        assertThat(tags[1]).isEqualTo(new Tag("quack", Tag.Type.CHARACTER));
    }
}
