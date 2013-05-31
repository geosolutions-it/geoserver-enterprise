/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.util;

import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;

import junit.framework.TestCase;

public class XCQLTest extends TestCase {

    public void testToFilter() throws Exception {
        String filter = "IN('foo','bar')";
        try {
            CQL.toFilter(filter);
            fail("filter should have thrown exception");
        }
        catch(CQLException e) {}
        
        Filter f1 = ECQL.toFilter(filter);
        Filter f2 = XCQL.toFilter(filter);
        assertEquals(f1, f2);
    }

    public void testToFilterFallback() throws Exception {
        String filter = "id = 2";
        
        try {
            ECQL.toFilter(filter);
            fail("filter should have thrown exception");
        }
        catch(CQLException e) {
        }
        
        Filter f1 = CQL.toFilter(filter);
        Filter f2 = XCQL.toFilter(filter);
        assertEquals(f1, f2);
        
    }
}
