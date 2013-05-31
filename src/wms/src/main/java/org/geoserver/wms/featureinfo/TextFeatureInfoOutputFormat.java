/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.logging.Level;

import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetFeatureInfoRequest;
import org.geoserver.wms.WMS;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Generates a FeatureInfoResponse of type text. This simply reports the attributes of the feature
 * requested as a text string. This class just performs the writeTo, the GetFeatureInfoDelegate and
 * abstract feature info class handle the rest.
 * 
 * @author James Macgill, PSU
 * @version $Id$
 */
public class TextFeatureInfoOutputFormat extends GetFeatureInfoOutputFormat {

    private WMS wms;

    public TextFeatureInfoOutputFormat(final WMS wms) {
        super("text/plain");
        this.wms = wms;
    }

    /**
     * Writes the feature information to the client in text/plain format.
     * 
     * @see GetFeatureInfoOutputFormat#write
     */
    //@SuppressWarnings("unchecked")
    public void write(FeatureCollectionType results, GetFeatureInfoRequest request, OutputStream out)
            throws ServiceException, IOException {
        Charset charSet = wms.getCharSet();
        OutputStreamWriter osw = new OutputStreamWriter(out, charSet);

        // getRequest().getGeoServer().getCharSet());
        PrintWriter writer = new PrintWriter(osw);

        // DJB: this is to limit the number of features read - as per the spec
        // 7.3.3.7 FEATURE_COUNT
        int featuresPrinted = 0; // how many features we've actually printed
                                 // so far!

        int maxfeatures = request.getFeatureCount(); // will default to 1
                                                     // if not specified
                                                     // in the request

        FeatureIterator reader = null;

        try {
            final List collections = results.getFeature();
            final int size = collections.size();
            FeatureCollection fr;
            SimpleFeature f;

            SimpleFeatureType schema;
            List<AttributeDescriptor> types;
            
            for (int i = 0; i < size; i++) // for each layer queried
            {
                fr = (FeatureCollection) collections.get(i);                
                reader = fr.features();
              
                boolean startFeat = true;
                while (reader.hasNext()) {
                    Feature feature = reader.next();
                    
                    if (startFeat) {
                        writer.println("Results for FeatureType '" + fr.getSchema().getName()
                            + "':");
                        startFeat = false;
                    }
                    
                    if (featuresPrinted < maxfeatures) {
                        writer.println("--------------------------------------------");
                    
                        if (feature instanceof SimpleFeature)
                        {
                            f = (SimpleFeature) feature;
                            schema = (SimpleFeatureType) f.getType();
                            types = schema.getAttributeDescriptors();
        
                            for (AttributeDescriptor descriptor : types) {
                                final Name name = descriptor.getName();
                                if (Geometry.class.isAssignableFrom(descriptor.getType().getBinding())) {
                                    // writer.println(types[j].getName() + " =
                                    // [GEOMETRY]");
    
                                    // DJB: changed this to print out WKT - its very
                                    // nice for users
                                    // Geometry g = (Geometry)
                                    // f.getAttribute(types[j].getName());
                                    // writer.println(types[j].getName() + " =
                                    // [GEOMETRY] = "+g.toText() );
    
                                    // DJB: decided that all the geometry info was
                                    // too much - they should use GML version if
                                    // they want those details
                                    Geometry g = (Geometry) f.getAttribute(name);
                                    writer.println(name + " = [GEOMETRY (" + g.getGeometryType()
                                            + ") with " + g.getNumPoints() + " points]");
                                } else {
                                    writer.println(name + " = " + f.getAttribute(name));
                                }
                            }
                            
                        }

                        else
                        {
                            writer.println(feature.toString());
                        }
                    }
                    
                    writer.println("--------------------------------------------");
                    featuresPrinted++;
                    
                }
            }
        } catch (Exception ife) {
            LOGGER.log(Level.WARNING, "Error generating getFeaturInfo, HTML format", ife);
            writer.println("Unable to generate information " + ife);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        if (featuresPrinted == 0) {
            writer.println("no features were found");
        }

        writer.flush();
    }
}
