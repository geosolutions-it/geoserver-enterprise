/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.WfsFactory;

import org.geoserver.feature.RetypingFeatureCollection;
import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.referencing.CRS;
import org.geotools.wfs.v1_0.WFSConfiguration;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;
import org.geotools.xml.Parser;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.ContentHandler;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Allows reading and writing a WFS feature collection
 */
public class WFSPPIO extends XMLPPIO {

    Configuration configuration;

    protected WFSPPIO(Configuration configuration, String mimeType, QName element) {
        super( FeatureCollectionType.class, FeatureCollection.class, mimeType, element);
        this.configuration = configuration;
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        Parser p = new Parser(configuration);
        FeatureCollectionType fct = (FeatureCollectionType) p.parse(input);
        return decode(fct);
    }
    
    @Override
    public Object decode(Object input) throws Exception {
        // xml parsing will most likely return it as parsed already, but if CDATA is used or if
        // it's a KVP parse it will be a string instead
        if(input instanceof String) {
            Parser p = new Parser(configuration);
            input = p.parse(new StringReader((String) input));
        }
        
        // cast and handle the axis flipping
        FeatureCollectionType fct = (FeatureCollectionType) input;
        SimpleFeatureCollection fc = (SimpleFeatureCollection) fct.getFeature().get( 0 );
        // Axis flipping issue, we should determine if the collection needs flipping 
        if(fc.getSchema().getGeometryDescriptor() != null) {
            CoordinateReferenceSystem crs = getCollectionCRS(fc);
            if(crs != null) {
                // do we need to force the crs onto the collection?
                CoordinateReferenceSystem nativeCrs = fc.getSchema().getGeometryDescriptor().getCoordinateReferenceSystem();
                if(nativeCrs == null) {
                    // we need crs forcing
                    fc = new ForceCoordinateSystemFeatureResults(fc, crs, false);
                }
                
                // we assume the crs has a valid EPSG code 
                Integer code = CRS.lookupEpsgCode(crs, false);
                if(code != null) {
                    CoordinateReferenceSystem lonLatCrs = CRS.decode("EPSG:" + code, true);
                    if(!CRS.equalsIgnoreMetadata(crs, lonLatCrs)) {
                        // we need axis flipping
                        fc = new ReprojectingFeatureCollection(fc, lonLatCrs);
                    }
                }
            }
        }
        
        return eliminateFeatureBounds(fc);
    }
    
    /**
     * Parsing GML we often end up with empty attributes that will break shapefiles
     * and common processing algorithms because they introduce bounding boxes (boundedBy) or hijack 
     * the default geometry property (location). We sanitize the collection in this method by
     * removing them.
     * It is not the best approach, but works in most cases, whilst not doing it would break
     * the code in most cases. Would be better to find a more general approach...
     * @param fc
     * @return
     */
    private SimpleFeatureCollection eliminateFeatureBounds(SimpleFeatureCollection fc) {
        final SimpleFeatureType original = fc.getSchema();
        List<String> names = new ArrayList<String>();
        boolean alternateGeometry = true;
        for(AttributeDescriptor ad : original.getAttributeDescriptors()) {
            final String name = ad.getLocalName();
            if(!"boundedBy".equals(name) && !"metadataProperty".equals(name)) {
                names.add(name);
            }
            if(!"location".equals(name) && ad instanceof GeometryDescriptor) {
                alternateGeometry = true;
            }
        }
        // if there is another geometry we assume "location" is going to be empty,
        // otherwise we're going to get always a null geometry
        if(alternateGeometry) {
            names.remove("location");
        }
        
        if(names.size() < original.getDescriptors().size()) {
            String[] namesArray = (String[]) names.toArray(new String[names.size()]);
            SimpleFeatureType target = SimpleFeatureTypeBuilder.retype(original, namesArray);
            return new RetypingFeatureCollection(fc, target);
        }
        return fc;
    }
    
    /**
     * Gets the collection CRS, either from metadata or by scanning the collection contents
     * @param fc
     * @return
     * @throws Exception
     */
    CoordinateReferenceSystem getCollectionCRS(SimpleFeatureCollection fc) throws Exception {
        // this is unlikely to work for remote or embedded collections, but it's also easy to check
        if(fc.getSchema().getCoordinateReferenceSystem() != null) {
            return fc.getSchema().getCoordinateReferenceSystem();
        }
        
        // ok, let's scan the entire collection then...
        CoordinateReferenceSystem crs = null;
        SimpleFeatureIterator fi = null;
        try {
            fi = fc.features();
            while(fi.hasNext()) {
                SimpleFeature f = fi.next();
                CoordinateReferenceSystem featureCrs = null;
                GeometryDescriptor gd = f.getType().getGeometryDescriptor();
                if(gd != null && gd.getCoordinateReferenceSystem() != null) {
                    featureCrs = gd.getCoordinateReferenceSystem();
                }
                if(f.getDefaultGeometry() != null) {
                    Geometry g = (Geometry) f.getDefaultGeometry();
                    if(g.getUserData() instanceof CoordinateReferenceSystem) {
                        featureCrs = (CoordinateReferenceSystem) g.getUserData();
                    }
                }
                
                // collect the feature crs, if it's new, use it, otherwise
                // check the collection does not have mixed crs
                if(featureCrs != null) {
                    if(crs == null) {
                        crs = featureCrs;
                    } else if(!CRS.equalsIgnoreMetadata(featureCrs, crs)) {
                        return null;
                    }
                }
            }
        } finally {
            fi.close();
        }
        
        return crs;
    }

    @Override
    public void encode(Object object, ContentHandler handler) throws Exception {
        FeatureCollection features = (FeatureCollection) object;
        SimpleFeatureType featureType = (SimpleFeatureType) features.getSchema();
        
        FeatureCollectionType fc = WfsFactory.eINSTANCE.createFeatureCollectionType();
        fc.getFeature().add( features );
        
        Encoder e = new Encoder(configuration);
        e.getNamespaces().declarePrefix( "feature", featureType.getName().getNamespaceURI() );
        e.encode( fc, getElement(), handler );
    }
    
    public static class WFS10 extends WFSPPIO {
        
        public WFS10() {
            super(new WFSConfiguration(), "text/xml; subtype=wfs-collection/1.0", 
                    org.geoserver.wfs.xml.v1_0_0.WFS.FEATURECOLLECTION);
        }
        
    }
    
    /**
     * A WFS 1.0 PPIO using alternate MIME type without a ";" that creates troubles in KVP parsing
     */
    public static class WFS10Alternate extends WFSPPIO {
        
        public WFS10Alternate() {
            super(new WFSConfiguration(), "application/wfs-collection-1.0", 
                    org.geoserver.wfs.xml.v1_0_0.WFS.FEATURECOLLECTION);
        }
        
    }
    
    public static class WFS11 extends WFSPPIO {
        
        public WFS11() {
            super(new org.geotools.wfs.v1_1.WFSConfiguration(), "text/xml; subtype=wfs-collection/1.1", 
                    org.geoserver.wfs.xml.v1_1_0.WFS.FEATURECOLLECTION);
        }
        
    }
    
    /**
     * A WFS 1.1 PPIO using alternate MIME type without a ";" that creates troubles in KVP parsing
     */
    public static class WFS11Alternate extends WFSPPIO {
        
        public WFS11Alternate() {
            super(new org.geotools.wfs.v1_1.WFSConfiguration(), "application/wfs-collection-1.1", 
                    org.geoserver.wfs.xml.v1_1_0.WFS.FEATURECOLLECTION);
        }
        
    }
}
