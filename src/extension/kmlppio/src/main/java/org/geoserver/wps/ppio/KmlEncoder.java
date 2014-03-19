/* Copyright (c) 2001 - 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.type.FeatureTypeImpl;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Encoder for KML 2.2. This encoder uses only a streaming XML generator and should scale very well even for large datasets.
 * 
 * @author Peter Hopfgartner (R3 GIS)
 */
class KmlEncoder {
    boolean useSchema = false;

    private Map<String, String> types = new HashMap<String, String>();

    public void encode(OutputStream lFileOutputStream, SimpleFeatureCollection collection)
            throws XMLStreamException, NoSuchAuthorityCodeException, FactoryException {

        // reproject to wgs84 if needed
        final CoordinateReferenceSystem sourceCoordinateReferenceSystem = collection.getSchema()
                .getCoordinateReferenceSystem();
        if (!CRS.equalsIgnoreMetadata(sourceCoordinateReferenceSystem, DefaultGeographicCRS.WGS84)) {
            collection = new ReprojectingFeatureCollection(collection, DefaultGeographicCRS.WGS84);
        }

        XMLOutputFactory xmlFactory = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = xmlFactory.createXMLStreamWriter(lFileOutputStream);
        writer.writeStartDocument();
        writer.writeStartElement("kml");
        writer.writeNamespace("", "http://www.opengis.net/kml/2.2");
        writer.writeStartElement("Document");

        writeStyles(writer);

        String schemaName = "";

        if (useSchema) {
            schemaName = writeSchema(writer, collection);
        }

        FeatureIterator<SimpleFeature> iter = collection.features();
        try {
            while (iter.hasNext()) {
                SimpleFeature f = iter.next();

                writer.writeStartElement("Placemark");
                writer.writeAttribute("id", f.getID());
                writer.writeStartElement("name");
                writer.writeCharacters(f.getID());
                writer.writeEndElement();

                String styleId = "#apbz_pab";
                writer.writeStartElement("styleUrl");
                writer.writeCharacters(styleId);
                writer.writeEndElement();
                
                if (useSchema) {
                    writeExtendedData(writer, f, schemaName);
                } else {
                    writeData(writer, f);
                }

                Geometry g = (Geometry) f.getDefaultGeometry();
                if (g instanceof MultiPolygon) {
                    MultiPolygon mp = (MultiPolygon) g;
                    int numGeometries = mp.getNumGeometries();
                    writer.writeStartElement("MultiGeometry");
                    for (int i = 0; i < numGeometries; i++) {
                        Polygon p = (Polygon) mp.getGeometryN(i);
                        writePolygon(writer, p);
                    }
                    writer.writeEndElement();
                    styleId = "#apbz_pab_poly";
                } else if (g instanceof Polygon) {
                    writePolygon(writer, (Polygon) g);
                    styleId = "#apbz_pab_poly";
                } else if (g instanceof MultiLineString) {
                    MultiLineString mls = (MultiLineString) g;
                    int numGeometries = mls.getNumGeometries();
                    writer.writeStartElement("MultiGeometry");
                    for (int i = 0; i < numGeometries; i++) {
                        LineString ls = (LineString) mls.getGeometryN(i);
                        writeLineString(writer, ls);
                    }
                    writer.writeEndElement();
                } else if (g instanceof LineString) {
                    writeLineString(writer, (LineString) g);
                } else if (g instanceof MultiPoint) {
                    MultiPoint mpt = (MultiPoint) g;
                    int numGeometries = mpt.getNumGeometries();
                    writer.writeStartElement("MultiGeometry");
                    for (int i = 0; i < numGeometries; i++) {
                        Point pt = (Point) mpt.getGeometryN(i);
                        writePoint(writer, pt);
                    }
                    writer.writeEndElement();
                } else if (g instanceof Point) {
                    writePoint(writer, (Point) g);
                } else {
                    throw new IllegalArgumentException("Output for geomtries of type " + g.getClass().getSimpleName() + " is not supported");
                }

                // PlaceMark
                writer.writeEndElement();

            }
        } finally {
            iter.close();
        }

        writer.writeEndDocument(); // </Document>
        writer.writeEndDocument(); // </kml>
        writer.flush();
        writer.close();
        return;
    }

    /**
     * Write styles used for the polygons
     * 
     * @param writer
     * @throws XMLStreamException
     */
    private void writeStyles(XMLStreamWriter writer) throws XMLStreamException {
        // write style
        writer.writeStartElement("Style");
        writer.writeAttribute("id", "apbz_pab");

        writer.writeStartElement("IconStyle");
        writer.writeStartElement("color");
        writer.writeCharacters("ff83fff8");
        writer.writeEndElement();
        writer.writeStartElement("colorMode");
        writer.writeCharacters("normal");
        writer.writeEndElement();
        writer.writeEndElement();

        writer.writeStartElement("LabelStyle");
        writer.writeStartElement("color");
        writer.writeCharacters("ff83fff8");
        writer.writeEndElement();
        writer.writeStartElement("colorMode");
        writer.writeCharacters("normal");
        writer.writeEndElement();
        writer.writeStartElement("scale");
        writer.writeCharacters("0.8");
        writer.writeEndElement();
        writer.writeEndElement();

        writer.writeStartElement("LineStyle");
        writer.writeStartElement("color");
        writer.writeCharacters("ff6dd6cf");
        writer.writeEndElement();
        writer.writeStartElement("colorMode");
        writer.writeCharacters("normal");
        writer.writeEndElement();
        writer.writeStartElement("width");
        writer.writeCharacters("3");
        writer.writeEndElement();
        writer.writeEndElement();
        /*
         * writer.writeStartElement("PolyStyle"); writer.writeStartElement("color"); writer.writeCharacters("807bf1ea"); writer.writeEndElement();
         * writer.writeStartElement("colorMode"); writer.writeCharacters("normal"); writer.writeEndElement(); writer.writeEndElement();
         */
        writer.writeEndElement();

        // Create style for polygons
        // since the line width of the polygon outline changes is different from the
        // line width of the regular linestring, this has to be a separate style
        //
        writer.writeStartElement("Style");
        writer.writeAttribute("id", "apbz_pab_poly");
        /*
         * writer.writeStartElement("IconStyle"); writer.writeStartElement("color"); writer.writeCharacters("ff509c98"); writer.writeEndElement();
         * writer.writeStartElement("colorMode"); writer.writeCharacters("normal"); writer.writeEndElement(); writer.writeEndElement();
         */
        writer.writeStartElement("LabelStyle");
        writer.writeStartElement("color");
        writer.writeCharacters("ff83fff8");
        writer.writeEndElement();
        writer.writeStartElement("colorMode");
        writer.writeCharacters("normal");
        writer.writeEndElement();
        writer.writeEndElement();

        writer.writeStartElement("LineStyle");
        writer.writeStartElement("color");
        writer.writeCharacters("ff6dd6cf");
        writer.writeEndElement();
        writer.writeStartElement("colorMode");
        writer.writeCharacters("normal");
        writer.writeEndElement();
        writer.writeStartElement("width");
        writer.writeCharacters("2");
        writer.writeEndElement();
        writer.writeEndElement();

        writer.writeStartElement("PolyStyle");
        writer.writeStartElement("color");
        writer.writeCharacters("807bf1ea");
        writer.writeEndElement();
        writer.writeStartElement("colorMode");
        writer.writeCharacters("normal");
        writer.writeEndElement();
        writer.writeEndElement();

        writer.writeEndElement();

    }

    private void writePolygon(XMLStreamWriter writer, Polygon p) throws XMLStreamException {
        writer.writeStartElement("Polygon");
        writer.writeStartElement("outerBoundaryIs");
        LineString er = p.getExteriorRing();
        writeLinearRing(writer, er);
        writer.writeEndElement();
        for (int ir = 0; ir < p.getNumInteriorRing(); ir++) {
            writer.writeStartElement("innerBoundaryIs");
            LineString iringn = p.getInteriorRingN(ir);
            writeLinearRing(writer, iringn);
            writer.writeEndElement();

        }
        writer.writeEndElement();

    }

    private void writeCoordinates(XMLStreamWriter writer, LineString ls) throws XMLStreamException {
        writer.writeStartElement("coordinates");
        Coordinate[] coordinates = ls.getCoordinates();
        StringBuffer coordString = new StringBuffer();
        for (int ic = 0; ic < coordinates.length; ic++) {
            coordString.append(coordinates[ic].x + "," + coordinates[ic].y + ",0 ");
        }
        if(coordinates.length > 0) {
            coordString.setLength(coordString.length() - 1);
        }
        writer.writeCharacters(coordString.toString());
        writer.writeEndElement();
        writer.writeCharacters("\n");
    }

    private void writeLineString(XMLStreamWriter writer, LineString ls) throws XMLStreamException {
        writer.writeStartElement("LineString");
        writeCoordinates(writer, ls);
        writer.writeEndElement();
    }

    private void writeLinearRing(XMLStreamWriter writer, LineString lr) throws XMLStreamException {
        writer.writeStartElement("LinearRing");
        writeCoordinates(writer, lr);
        writer.writeEndElement();
    }

    private void writePoint(XMLStreamWriter writer, Point pt) throws XMLStreamException {
        writer.writeStartElement("Point");
        writer.writeStartElement("coordinates");
        StringBuffer coordString = new StringBuffer();
        coordString.append(pt.getX() + "," + pt.getY() + ",0");
        writer.writeCharacters(coordString.toString());
        writer.writeEndElement();
        writer.writeCharacters("\n");
        writer.writeEndElement();
    }

    private String writeSchema(XMLStreamWriter writer, SimpleFeatureCollection fc)
            throws XMLStreamException {
        FeatureTypeImpl schema = (FeatureTypeImpl) fc.getSchema();
        writer.writeStartElement("Schema");
        String schemaName = fc.getID();
        writer.writeAttribute("name", schemaName);
        writer.writeAttribute("id", schemaName);
        for (PropertyDescriptor p : schema.getDescriptors()) {
            Name name = p.getName();
            if (!(p.getType() instanceof GeometryType)) {
                writer.writeStartElement("SimpleField");
                writer.writeAttribute("name", name.getLocalPart());
                Class binding = p.getType().getBinding();
                String kmlType;
                /*
                 * KML has the following types , https://developers.google.com/kml /documentation/kmlreference#simplefield string int uint short
                 * ushort float double bool
                 */
                if (binding == Integer.class || binding == Short.class || binding == Long.class) {
                    kmlType = "int";
                } else if (binding == Float.class || binding == Double.class) {
                    kmlType = "double";
                } else if (binding == Boolean.class) {
                    kmlType = "bool";
                } else {
                    kmlType = "string";
                }
                types.put(name.getLocalPart(), kmlType);

                writer.writeAttribute("type", kmlType);
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
        return schemaName;
    }

    private void writeExtendedData(XMLStreamWriter writer, SimpleFeature f, String schemaName)
            throws XMLStreamException {
        writer.writeStartElement("ExtendedData");
        writer.writeStartElement("SchemaData");
        writer.writeAttribute("schemaUrl", "#" + schemaName);
        for (Property p : f.getProperties()) {
            Name name = p.getName();
            if (!(p.getValue() instanceof Geometry)) {
                writer.writeStartElement("SimpleData");
                writer.writeAttribute("name", name.getLocalPart());
                if (p.getValue() != null) {
                    writer.writeCharacters(p.getValue().toString());
                }
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
        writer.writeEndElement();
    }

    private void writeData(XMLStreamWriter writer, SimpleFeature f) throws XMLStreamException {
        writer.writeStartElement("ExtendedData");
        for (Property p : f.getProperties()) {
            Name name = p.getName();
            if (!(p.getValue() instanceof Geometry) && p.getValue() != null) {
                writer.writeStartElement("Data");
                writer.writeAttribute("name", name.getLocalPart());
                writer.writeStartElement("value");
                writer.writeCharacters(p.getValue().toString());
                writer.writeEndElement();
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

}
