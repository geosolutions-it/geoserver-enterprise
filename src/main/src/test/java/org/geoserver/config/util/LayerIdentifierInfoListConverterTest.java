/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.impl.LayerIdentifier;

public class LayerIdentifierInfoListConverterTest extends TestCase {

    public void testFromString() {
        final String serialized = "[{\"authority\":\"auth1\",\"identifier\":\"IDENTIFIER_1\"},{\"authority\":\"auth2\",\"identifier\":\"IDENTIFIER_2\"}]";
        List<LayerIdentifierInfo> expected = new ArrayList<LayerIdentifierInfo>();

        LayerIdentifierInfo id1 = new LayerIdentifier();
        id1.setAuthority("auth1");
        id1.setIdentifier("IDENTIFIER_1");
        expected.add(id1);

        LayerIdentifierInfo id2 = new LayerIdentifier();
        id2.setAuthority("auth2");
        id2.setIdentifier("IDENTIFIER_2");
        expected.add(id2);

        List<LayerIdentifierInfo> actual;
        actual = LayerIdentifierInfoListConverter.fromString(serialized);
        
        assertEquals(expected, actual);
    }

    public void testFromInvalidString() {
        final String serialized = "[{\"name:\"auth1\",\"href\":\"http://geoserver.org/auth1?\"},]";

        try {
            LayerIdentifierInfoListConverter.fromString(serialized);
            fail("Expected IAE");
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    public void testToString() {
        List<LayerIdentifierInfo> list = new ArrayList<LayerIdentifierInfo>();

        LayerIdentifierInfo id1 = new LayerIdentifier();
        id1.setAuthority("auth1");
        id1.setIdentifier("IDENTIFIER_1");
        list.add(id1);

        LayerIdentifierInfo id2 = new LayerIdentifier();
        id2.setAuthority("auth2");
        id2.setIdentifier("IDENTIFIER_2");
        list.add(id2);

        String actual = LayerIdentifierInfoListConverter.toString(list);
        System.out.println(actual);
        String expected = "[{\"authority\":\"auth1\",\"identifier\":\"IDENTIFIER_1\"},{\"authority\":\"auth2\",\"identifier\":\"IDENTIFIER_2\"}]";
        assertEquals(expected, actual);
    }

    public void testToStringListWithNullElement() {
        List<LayerIdentifierInfo> list = new ArrayList<LayerIdentifierInfo>();

        LayerIdentifierInfo id1 = new LayerIdentifier();
        id1.setAuthority("auth1");
        id1.setIdentifier("IDENTIFIER_1");
        list.add(id1);

        list.add(null);

        String actual = LayerIdentifierInfoListConverter.toString(list);
        String expected = "[{\"authority\":\"auth1\",\"identifier\":\"IDENTIFIER_1\"}]";
        assertEquals(expected, actual);
    }

    public void testToStringListWithOnlyNullElements() {
        List<LayerIdentifierInfo> list = new ArrayList<LayerIdentifierInfo>();
        list.add(null);
        list.add(null);
        list.add(null);

        assertNull(LayerIdentifierInfoListConverter.toString(list));

    }

    public void testToStringEmptyList() {
        List<LayerIdentifierInfo> list = new ArrayList<LayerIdentifierInfo>();

        String actual = LayerIdentifierInfoListConverter.toString(list);
        assertNull(actual);
    }
}
