/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.feature;

import junit.framework.TestCase;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;

public class ReprojectingFeatureCollectionTest extends TestCase {

    public void testPerserveUserData() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("foo");
        tb.setSRS("epsg:4326");
        tb.add("geom", Point.class);

        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());
        b.add(new WKTReader().read("POINT(1 1)"));
        SimpleFeature f = b.buildFeature(null);
        f.getUserData().put("foo", "bar");

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        features.add(f);

        FeatureIterator it = features.features();
        
        try {
            assertEquals("bar", it.next().getUserData().get("foo"));
        }
        finally {
            it.close();
        }

        ReprojectingFeatureCollection reprojected = 
                new ReprojectingFeatureCollection(features, CRS.decode("EPSG:3005"));
        it = reprojected.features();
        
        try {
            assertEquals("bar", it.next().getUserData().get("foo"));
        }
        finally {
            it.close();
        }
    }
}
