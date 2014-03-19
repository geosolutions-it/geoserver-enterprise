/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.kml.KML;
import org.geotools.kml.KMLConfiguration;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Configuration;
import org.geotools.xml.StreamingParser;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

public class KMLPPIO extends CDataPPIO {
    private static final Logger LOGGER = Logging.getLogger(KMLPPIO.class);

    Configuration xml;

    SimpleFeatureType type;

    public KMLPPIO() {
        super(FeatureCollection.class, FeatureCollection.class,
                "application/vnd.google-earth.kml+xml");

        this.xml = new KMLConfiguration();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();

        b.setName("puregeometries");

        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("location", Geometry.class);

        this.type = b.buildFeatureType();
    }

    private final static HashMap<Name, Class> getSignature(SimpleFeature f) {
        HashMap ftype = new HashMap();
        Collection properties = f.getProperties();
        for (Object op : properties) {
            Property p = (Property) op;
            Class c = p.getType().getBinding();
            if ((c.isAssignableFrom(String.class)) || (c.isAssignableFrom(Boolean.class))
                    || (c.isAssignableFrom(Integer.class)) || (c.isAssignableFrom(Float.class))
                    || (c.isAssignableFrom(Double.class)) || (c.isAssignableFrom(Geometry.class))) {
                ftype.put(p.getName(), c);
            }
        }
        return ftype;
    }

    private SimpleFeatureType getType(HashMap<Name, Class> ftype) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();

        b.setName("puregeometries");

        b.setCRS(DefaultGeographicCRS.WGS84);
        for (Map.Entry entry : ftype.entrySet()) {
            b.add(((Name) entry.getKey()).toString(), (Class) entry.getValue());
        }
        return b.buildFeatureType();
    }

    public Object decode(InputStream input) throws Exception {
        StreamingParser parser = new StreamingParser(new KMLConfiguration(), input, KML.Placemark);
        SimpleFeature f = null;
        ListFeatureCollection features = null;
        HashMap oldftype = null;
        SimpleFeatureType type = null;
        SimpleFeatureBuilder featureBuilder = null;

        while ((f = (SimpleFeature) parser.parse()) != null) {
            HashMap ftype = getSignature(f);
            if (oldftype == null) {
                oldftype = ftype;
                type = getType(ftype);
                featureBuilder = new SimpleFeatureBuilder(type);
                features = new ListFeatureCollection(type);
            } else {
                if (!oldftype.equals(ftype)) {
                    break;
                }
            }
            for (Object oentry : ftype.entrySet()) {
                Map.Entry entry = (Map.Entry) oentry;
                featureBuilder.add(f.getAttribute((Name) entry.getKey()));
            }
            SimpleFeature fnew = featureBuilder.buildFeature(f.getID());
            features.add(fnew);
        }
        return features;
    }

    public Object decode(String input) throws Exception {
        return decode(new ByteArrayInputStream(input.getBytes()));
    }

    @Override
    public void encode(Object obj, OutputStream os) throws Exception {
        LOGGER.info("KMLPPIO::encode: obj is of class " + obj.getClass().getName()
                + ", handler is of class " + os.getClass().getName());
        KmlEncoder kmlEncoder = new KmlEncoder();
        SimpleFeatureCollection fc = (SimpleFeatureCollection) obj;
        CoordinateReferenceSystem crs = fc.getSchema().getCoordinateReferenceSystem();
        // gpx is defined only in wgs84
        if (crs != null && !CRS.equalsIgnoreMetadata(crs, DefaultGeographicCRS.WGS84)) {
            fc = new ReprojectingFeatureCollection(fc, DefaultGeographicCRS.WGS84);
        }

        kmlEncoder.encode(os, fc);
        os.flush();

    }

    public String getFileExtension() {
        return "kml";
    }
}
