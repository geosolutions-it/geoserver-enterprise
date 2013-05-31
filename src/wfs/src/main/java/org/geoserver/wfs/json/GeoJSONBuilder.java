/*
 * Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.io.Writer;
import java.util.Calendar;
import java.util.logging.Logger;

import net.sf.json.JSONException;
import net.sf.json.util.JSONBuilder;

import org.geotools.util.Converters;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;


/**
 * This class extends the JSONBuilder to be able to write out geometric types.  It is coded
 * against the draft 5 version of the spec on http://geojson.org
 *
 * @author Chris Holmes, The Open Planning Project
 * @version $Id$
 *
 */
public class GeoJSONBuilder extends JSONBuilder {
    private final Logger LOGGER = org.geotools.util.logging.Logging
    .getLogger(this.getClass());
    
    public GeoJSONBuilder(Writer w) {
        super(w);
    }

    /**
     * Writes any geometry object.  This class figures out which geometry representation to write
     * and calls subclasses to actually write the object.
     * @param geometry The geoemtry be encoded
     * @return The JSONBuilder with the new geoemtry
     * @throws JSONException If anything goes wrong
     */
    public JSONBuilder writeGeom(Geometry geometry) throws JSONException {
        this.object();
        this.key("type");
        this.value(getGeometryName(geometry));

        final int geometryType = getGeometryType(geometry);

        if (geometryType != MULTIGEOMETRY) {
            this.key("coordinates");

            switch (geometryType) {
            case POINT:
                Point point = (Point)geometry;
                writeCoordinate(point.getX(), point.getY());
                break;
            case LINESTRING:
                writeCoordinates(((LineString)geometry).getCoordinateSequence());
                break;
            case MULTIPOINT:
                writeCoordinates(geometry.getCoordinates());
                break;
            case POLYGON:
                writePolygon((Polygon) geometry);

                break;

            case MULTILINESTRING:
                this.array();

                for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
                    writeCoordinates(((LineString)geometry.getGeometryN(i)).getCoordinateSequence());
                }

                this.endArray();

                break;

            case MULTIPOLYGON:
                this.array();

                for (int i = 0, n = geometry.getNumGeometries(); i < n; i++) {
                    writePolygon((Polygon) geometry.getGeometryN(i));
                }

                this.endArray();

                break;
            }
        } else {
            writeGeomCollection((GeometryCollection) geometry);
        }

        return this.endObject();
    }

    private JSONBuilder writeGeomCollection(GeometryCollection collection) {
        this.array();
        this.key("geometries");

        for (int i = 0, n = collection.getNumGeometries(); i < n; i++) {
            writeGeom(collection.getGeometryN(i));
        }

        return this.endArray();
    }

    private JSONBuilder writeCoordinates(Coordinate[] coords)
        throws JSONException {
        return writeCoordinates(new CoordinateArraySequence(coords));
    }
    
    /**
     * Write the coordinates of a geometry
     * @param coords The coordinates to write
     * @return this
     * @throws JSONException
     */
    private JSONBuilder writeCoordinates(CoordinateSequence coords)
        throws JSONException {
        this.array();

        final int coordCount = coords.size();
        for (int i = 0; i < coordCount; i++) {
            writeCoordinate(coords.getX(i), coords.getY(i));
        }

        return this.endArray();
    }

    private JSONBuilder writeCoordinate(double x, double y) {
        this.array();
        this.value(x);
        this.value(y);

        return this.endArray();
    }
    /**
     * Turns an envelope into an array [minX,minY,maxX,maxY]
     * @param env envelope representing bounding box
     * @return this
     */
    protected JSONBuilder writeBoundingBox(Envelope env) {
        this.key("bbox");
        this.array();
        this.value(env.getMinX());
        this.value(env.getMinY());
        this.value(env.getMaxX());
        this.value(env.getMaxY());
        return this.endArray();
    }

    /**
     * Writes a polygon
     * @param geometry The polygon to write
     * @throws JSONException
     */
    private void writePolygon(Polygon geometry) throws JSONException {
        this.array();
        writeCoordinates(geometry.getExteriorRing().getCoordinateSequence());

        for (int i = 0, ii = geometry.getNumInteriorRing(); i < ii; i++) {
            writeCoordinates(geometry.getInteriorRingN(i).getCoordinateSequence());
        }

        this.endArray(); //end the linear ring
                         //this.endObject(); //end the 
    }

    /** Internal representation of OGC SF Point */
    protected static final int POINT = 1;

    /** Internal representation of OGC SF LineString */
    protected static final int LINESTRING = 2;

    /** Internal representation of OGC SF Polygon */
    protected static final int POLYGON = 3;

    /** Internal representation of OGC SF MultiPoint */
    protected static final int MULTIPOINT = 4;

    /** Internal representation of OGC SF MultiLineString */
    protected static final int MULTILINESTRING = 5;

    /** Internal representation of OGC SF MultiPolygon */
    protected static final int MULTIPOLYGON = 6;

    /** Internal representation of OGC SF MultiGeometry */
    protected static final int MULTIGEOMETRY = 7;

    public static String getGeometryName(Geometry geometry) {
        if (geometry instanceof Point) {
            return "Point";
        } else if (geometry instanceof LineString) {
            return "LineString";
        } else if (geometry instanceof Polygon) {
            return "Polygon";
        } else if (geometry instanceof MultiPoint) {
            return "MultiPoint";
        } else if (geometry instanceof MultiLineString) {
            return "MultiLineString";
        } else if (geometry instanceof MultiPolygon) {
            return "MultiPolygon";
        } else if (geometry instanceof GeometryCollection) {
            return "GeometryCollection";
        } else {
            throw new IllegalArgumentException("Unknown geometry type " + geometry.getClass());
        }
    }

    /**
     * Gets the internal representation for the given Geometry
     *
     * @param geometry a Geometry
     *
     * @return int representation of Geometry
     */
    public static int getGeometryType(Geometry geometry) {
        //LOGGER.entering("GMLUtils", "getGeometryType", geometry);
        if (geometry instanceof Point) {
            //LOGGER.finest("found point");
            return POINT;
        } else if (geometry instanceof LineString) {
            //LOGGER.finest("found linestring");
            return LINESTRING;
        } else if (geometry instanceof Polygon) {
            //LOGGER.finest("found polygon");
            return POLYGON;
        } else if (geometry instanceof MultiPoint) {
            //LOGGER.finest("found multiPoint");
            return MULTIPOINT;
        } else if (geometry instanceof MultiLineString) {
            return MULTILINESTRING;
        } else if (geometry instanceof MultiPolygon) {
            return MULTIPOLYGON;
        } else if (geometry instanceof GeometryCollection) {
            return MULTIGEOMETRY;
        } else {
            throw new IllegalArgumentException(
                "Unable to determine geometry type " + geometry.getClass());
        }
    }
    
    /**
     * Overrides to handle the case of encoding {@code java.util.Date} and its date/time/timestamp
     * descendants, as well as {@code java.util.Calendar} instances as ISO 8601 strings.
     * 
     * @see net.sf.json.util.JSONBuilder#value(java.lang.Object)
     */
    @Override
    public GeoJSONBuilder value(Object value) {
        if (value instanceof java.util.Date || value instanceof Calendar) {
            value = Converters.convert(value, String.class);
        }
        super.value(value);
        return this;
    }
}
