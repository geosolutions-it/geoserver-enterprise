/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_0_0;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.gml2.FeatureTypeCache;
import org.geotools.xml.BindingWalkerFactory;
import org.geotools.xml.Configuration;
import org.geotools.xml.ElementInstance;
import org.geotools.xml.Node;
import org.geotools.xml.SchemaIndex;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;


public final class GMLAbstractFeatureTypeBinding extends org.geotools.gml2.bindings.GMLAbstractFeatureTypeBinding {
    GeometryFactory geometryFactory;
    Catalog catalog;
    //SchemaIndex schemaIndex;
    public GMLAbstractFeatureTypeBinding(FeatureTypeCache featureTypeCache,
        BindingWalkerFactory bwFactory, SchemaIndex schemaIndex, GeometryFactory geometryFactory, Catalog catalog, Configuration configuration) {
        super(featureTypeCache, bwFactory, schemaIndex, configuration);
        this.geometryFactory = geometryFactory;
        this.catalog = catalog;
    }

    public Object parse(ElementInstance instance, Node node, Object value)
        throws Exception {
        //pre process parsee tree to make sure types match up
        FeatureTypeInfo meta = catalog.getFeatureTypeByName(instance.getNamespace(), instance.getName());
        if (meta != null) {
            FeatureType featureType = meta.getFeatureType();

            //go through each attribute, performing various hacks to make make sure things 
            // cocher
            for (PropertyDescriptor pd : featureType.getDescriptors()) {
                if (pd instanceof AttributeDescriptor) {
                    AttributeDescriptor attributeType = (AttributeDescriptor) pd;
                    String name = attributeType.getLocalName();
                    Class type = attributeType.getType().getBinding();

                    if ("boundedBy".equals(name)) {
                        Node boundedByNode = node.getChild("boundedBy");

                        //hack 1: if boundedBy is in the parse tree has a bounding box and the attribute 
                        // needs a polygon, convert
                        if (boundedByNode.getValue() instanceof Envelope) {
                            Envelope bounds = (Envelope) boundedByNode.getValue();

                            if (type.isAssignableFrom(Polygon.class)) {
                                Polygon polygon = polygon(bounds);
                                boundedByNode.setValue(polygon);
                            } else if (type.isAssignableFrom(MultiPolygon.class)) {
                                MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(new Polygon[] {
                                        polygon(bounds)
                                });
                                boundedByNode.setValue(multiPolygon);
                            }
                        }
                    }

                }
            }
        }

        return super.parse(instance, node, value);
    }

    Polygon polygon(Envelope bounds) {
        return geometryFactory.createPolygon(geometryFactory.createLinearRing(
                new Coordinate[] {
                    new Coordinate(bounds.getMinX(), bounds.getMinY()),
                    new Coordinate(bounds.getMinX(), bounds.getMaxY()),
                    new Coordinate(bounds.getMaxX(), bounds.getMaxY()),
                    new Coordinate(bounds.getMaxX(), bounds.getMinY()),
                    new Coordinate(bounds.getMinX(), bounds.getMinY())
                }), null);
    }
}
