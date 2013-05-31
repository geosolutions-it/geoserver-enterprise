/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.vfny.geoserver.global.dto.EqualsLibrary;


/**
 * EqualsLibraryTest purpose.
 * 
 * <p>
 * Description of EqualsLibraryTest ...
 * </p>
 * 
 * <p></p>
 *
 * @author dzwiers, Refractions Research, Inc.
 * @version $Id$
 */
public class EqualsLibraryTest extends TestCase {
    /**
     * Constructor for EqualsLibraryTest.
     *
     * @param arg0
     */
    public EqualsLibraryTest(String arg0) {
        super(arg0);
    }

    /*
     * Test for boolean equals(List, List)
     */
    public void testEqualsListList() {
        List a;
        List b;
        a = new LinkedList();
        b = new LinkedList();
        a.add("a");
        b.add("a");
        a.add("b");
        b.add("b");
        a.add("c");
        b.add("c");

        assertTrue(EqualsLibrary.equals(a, b));

        a.add("d");
        b.add("e");
        a.add("e");
        b.add("d");

        assertTrue(EqualsLibrary.equals(a, b));

        a.add("f");
        b.add("a");
        a.add("a");
        b.add("f");

        assertTrue(EqualsLibrary.equals(a, b));

        a.add("g");

        assertTrue(!EqualsLibrary.equals(a, b));
    }

    /*
     * Test for boolean equals(Map, Map)
     */
    public void testEqualsMapMap() {
        Map a;
        Map b;
        a = new HashMap();
        b = new HashMap();
        a.put("0", new Integer(0));
        b.put("0", new Integer(0));
        a.put("1", new Integer(1));
        b.put("1", new Integer(1));
        a.put("2", new Integer(2));
        b.put("2", new Integer(2));

        assertTrue(EqualsLibrary.equals(a, b));

        a.put("3", new Integer(3));
        b.put("4", new Integer(4));
        a.put("4", new Integer(4));
        b.put("3", new Integer(3));

        assertTrue(EqualsLibrary.equals(a, b));

        a.put("5", new Integer(5));
        b.put("6", new Integer(5));

        assertTrue(!EqualsLibrary.equals(a, b));

        a.put("5", new Integer(5));
        b.put("5", new Integer(5));
        b.remove("6");

        assertTrue(EqualsLibrary.equals(a, b));

        a.put("5", new Integer(6));

        assertTrue(!EqualsLibrary.equals(a, b));

        a.put("5", new Integer(5));

        assertTrue(EqualsLibrary.equals(a, b));
    }
}
