/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.gs;

import junit.framework.TestCase;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class ROIManagerTest extends TestCase {
    
    
    public void testBase() throws Exception{
        
        // example in wgs84
        final CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326", true);
        Envelope env = new Envelope(8,9,40,41);
        Polygon roi = JTS.toGeometry(env);
        assertTrue(roi.isRectangle());
        roi.setSRID(4326);
        roi.setUserData(wgs84);

        // go to 3857
        final CoordinateReferenceSystem googlem = CRS.decode("EPSG:3857", true);
        final Geometry roiGoogle=JTS.transform(roi,CRS.findMathTransform(wgs84,googlem ));
        assertTrue(roiGoogle.isRectangle());
        
        // target crs is 3003
        final CoordinateReferenceSystem boaga = CRS.decode("EPSG:3003", true);
        final Geometry roiBoaga=JTS.transform(roi,CRS.findMathTransform(wgs84,boaga ));
        assertFalse(roiBoaga.isRectangle());
        assertTrue(roiBoaga.getEnvelope().isRectangle());    
        
        
        // create manager
        final ROIManager roiManager= new ROIManager(roiGoogle, googlem);
        assertTrue(roiManager.isROIBBOX());
        
        // provide native CRS
        roiManager.useNativeCRS(wgs84);
        final Geometry roiNativeCRS=roiManager.getSafeRoiInNativeCRS();
        assertTrue(roiNativeCRS.isRectangle());
        assertTrue(roiNativeCRS.getEnvelope().equalsExact(roi.getEnvelope(), 1E-9));
        
        // provide target CRS
        roiManager.useTargetCRS(boaga);
        final Geometry roiTargetCRS=roiManager.getSafeRoiInTargetCRS();
        assertTrue(roiTargetCRS.isRectangle());
    }
}
