/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONException;

import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.NamedIdentifier;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A GetFeatureInfo response handler specialized in producing Json and JsonP data for a GetFeatureInfo request.
 * 
 * @author Simone Giannecchini, GeoSolutions
 * @author Carlo Cancellieri - GeoSolutions
 * 
 */
public class GeoJSONGetFeatureResponse extends WFSGetFeatureOutputFormat {
    private final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(this.getClass());

    // store the response type
    private final boolean jsonp;

    public GeoJSONGetFeatureResponse(GeoServer gs, String format) {
        super(gs, format);
        if (JSONType.isJsonMimeType(format)) {
            jsonp = false;
        } else if (JSONType.isJsonpMimeType(format)) {
            jsonp = true;
        } else {
            throw new IllegalArgumentException(
                    "Unable to create the JSON Response handler using format: " + format
                            + " supported mymetype are: "
                            + Arrays.toString(JSONType.getSupportedTypes()));
        }

    }

    /**
     * capabilities output format string.
     */
    public String getCapabilitiesElementName() {
        return JSONType.getJSONType(getOutputFormat()).toString();
    }

    /**
     * Returns the mime type
     */
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return getOutputFormat();
    }

    @Override
    protected void write(FeatureCollectionResponse featureCollection, OutputStream output,
            Operation describeFeatureType) throws IOException {

        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.info("about to encode JSON");

        // Generate bounds for every feature?
        WFSInfo wfs = getInfo();
        boolean featureBounding = wfs.isFeatureBounding();

        // prepare to write out
        OutputStreamWriter osw = null;
        Writer outWriter = null;
        boolean hasGeom = false;

        try {
            osw = new OutputStreamWriter(output, gs.getSettings().getCharset());
            outWriter = new BufferedWriter(osw);

            if (jsonp) {
                outWriter.write(getCallbackFunction() + "(");
            }

            final GeoJSONBuilder jsonWriter = new GeoJSONBuilder(outWriter);
            jsonWriter.object().key("type").value("FeatureCollection");
            jsonWriter.key("features");
            jsonWriter.array();

            // execute should of set all the header information
            // including the lockID
            //
            // execute should also fail if all of the locks could not be aquired
            List resultsList = featureCollection.getFeature();
            CoordinateReferenceSystem crs = null;
            for (int i = 0; i < resultsList.size(); i++) {
                FeatureCollection collection = (FeatureCollection) resultsList.get(i);
                FeatureIterator iterator = collection.features();

                try {
                    SimpleFeatureType fType;
                    List<AttributeDescriptor> types;

                    while (iterator.hasNext()) {
                        SimpleFeature feature = (SimpleFeature) iterator.next();
                        jsonWriter.object();
                        jsonWriter.key("type").value("Feature");
                        jsonWriter.key("id").value(feature.getID());

                        fType = feature.getFeatureType();
                        types = fType.getAttributeDescriptors();

                        GeometryDescriptor defaultGeomType = fType.getGeometryDescriptor();

                        if (crs == null && defaultGeomType != null)
                            crs = fType.getGeometryDescriptor().getCoordinateReferenceSystem();

                        jsonWriter.key("geometry");
                        Geometry aGeom = (Geometry) feature.getDefaultGeometry();

                        if (aGeom == null) {
                            // In case the default geometry is not set, we will
                            // just use the first geometry we find
                            for (int j = 0; j < types.size() && aGeom == null; j++) {
                                Object value = feature.getAttribute(j);
                                if (value != null && value instanceof Geometry) {
                                    aGeom = (Geometry) value;
                                }
                            }
                        }
                        // Write the geometry, whether it is a null or not
                        if (aGeom != null) {
                            jsonWriter.writeGeom(aGeom);
                            hasGeom = true;
                        } else {
                            jsonWriter.value(null);
                        }
                        if (defaultGeomType != null)
                            jsonWriter.key("geometry_name").value(defaultGeomType.getLocalName());

                        jsonWriter.key("properties");
                        jsonWriter.object();

                        for (int j = 0; j < types.size(); j++) {
                            Object value = feature.getAttribute(j);
                            AttributeDescriptor ad = types.get(j);

                            if (value != null) {
                                if (value instanceof Geometry) {
                                    // This is an area of the spec where they
                                    // decided to 'let convention evolve',
                                    // that is how to handle multiple
                                    // geometries. My take is to print the
                                    // geometry here if it's not the default.
                                    // If it's the default that you already
                                    // printed above, so you don't need it here.
                                    if (ad.equals(defaultGeomType)) {
                                        // Do nothing, we wrote it above
                                        // jsonWriter.value("geometry_name");
                                    } else {
                                        jsonWriter.key(ad.getLocalName());
                                        jsonWriter.writeGeom((Geometry) value);
                                    }
                                } else {
                                    jsonWriter.key(ad.getLocalName());
                                    jsonWriter.value(value);
                                }

                            } else {
                                jsonWriter.key(ad.getLocalName());
                                jsonWriter.value(null);
                            }
                        }
                        // Bounding box for feature in properties
                        ReferencedEnvelope refenv = new ReferencedEnvelope(feature.getBounds());
                        if (featureBounding && !refenv.isEmpty())
                            jsonWriter.writeBoundingBox(refenv);

                        jsonWriter.endObject(); // end the properties
                        jsonWriter.endObject(); // end the feature
                    }
                } // catch an exception here?
                finally {
                    collection.close(iterator);
                }
            }
            jsonWriter.endArray(); // end features

            // Coordinate Referense System, currently only if the namespace is
            // EPSG
            if (crs != null) {
                Set<ReferenceIdentifier> ids = crs.getIdentifiers();
                // WKT defined crs might not have identifiers at all
                if (ids != null && ids.size() > 0) {
                    NamedIdentifier namedIdent = (NamedIdentifier) ids.iterator().next();
                    String csStr = namedIdent.getCodeSpace().toUpperCase();

                    if (csStr.equals("EPSG")) {
                        jsonWriter.key("crs");
                        jsonWriter.object();
                        jsonWriter.key("type").value(csStr);
                        jsonWriter.key("properties");
                        jsonWriter.object();
                        jsonWriter.key("code");
                        jsonWriter.value(namedIdent.getCode());
                        jsonWriter.endObject(); // end properties
                        jsonWriter.endObject(); // end crs
                    }
                }
            }

            // Bounding box for featurecollection
            if (hasGeom && featureBounding) {
                ReferencedEnvelope e = null;
                for (int i = 0; i < resultsList.size(); i++) {
                    FeatureCollection collection = (FeatureCollection) resultsList.get(i);
                    if (e == null) {
                        e = collection.getBounds();
                    } else {
                        e.expandToInclude(collection.getBounds());
                    }

                }

                if (e != null) {
                    jsonWriter.writeBoundingBox(e);
                }
            }

            jsonWriter.endObject(); // end featurecollection

            if (jsonp) {
                outWriter.write(")");
            }

            outWriter.flush();

        } catch (JSONException jsonException) {
            ServiceException serviceException = new ServiceException("Error: "
                    + jsonException.getMessage());
            serviceException.initCause(jsonException);
            throw serviceException;
        }
    }

    private String getCallbackFunction() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            return JSONType.CALLBACK_FUNCTION;
        }
        return JSONType.getCallbackFunction(request.getKvp());
    }
}
