/*
 * Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import junit.framework.Test;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.w3c.dom.Document;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * This is to test spatial (bbox) queries for complex features
 * 
 * @author Derrick Wong, Curtin University of Technology
 */

public class BBoxFilterTest extends AbstractAppSchemaWfsTestSupport {
    private final String WFS_GET_FEATURE = "wfs?request=GetFeature&version=1.1.0&typename=ex:geomContainer";

    private final String WFS_GET_FEATURE_LOG = "WFS GetFeature&typename=ex:geomContainerresponse:\n";

    private final String LONGLAT = "&BBOX=130,-29,134,-24";

    private final String LATLONG = "&BBOX=-29,130,-24,134";

    private final String EPSG_4326 = "EPSG:4326";

    private final String EPSG_4283 = "urn:x-ogc:def:crs:EPSG:4283";

    /**
     * Read-only test so can use one-time setup.
     * 
     */
    public static Test suite() {
        return new OneTimeTestSetup(new BBoxFilterTest());
    }

    protected NamespaceTestData buildTestData() {
        return new BBoxMockData();
    }

    /**
     * The following performs a WFS request and obtains all features specified in
     * BBoxTestPropertyfile.properties
     */
    public void testQuery() {
        Document doc = getAsDOM(WFS_GET_FEATURE);
        LOGGER.info(WFS_GET_FEATURE_LOG + prettyString(doc));
        assertXpathEvaluatesTo("3", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(3, "//ex:geomContainer", doc);

    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering longitude
     * latitude.
     */
    public void testQueryBboxLongLat() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LONGLAT);
        LOGGER.info(WFS_GET_FEATURE_LOG + LONGLAT + prettyString(doc));
        assertXpathEvaluatesTo("0", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(0, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering latitude
     * longitude. This test should return features if the axis ordering behaves similar to queries
     * to Simple features.
     */
    public void testQueryBboxLatLong() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LATLONG);
        LOGGER.info(WFS_GET_FEATURE_LOG + LATLONG + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering longitude
     * latitude along with srs reprojection.
     */
    public void testQueryBboxLatLongSrs4283() throws NoSuchAuthorityCodeException,
            FactoryException, MismatchedDimensionException, TransformException {
        Document doc = getAsDOM(WFS_GET_FEATURE + LATLONG + "&srsName=urn:x-ogc:def:crs:EPSG:4283");
        LOGGER.info(WFS_GET_FEATURE_LOG + LONGLAT + prettyString(doc));

        CoordinateReferenceSystem sourceCRS = (CoordinateReferenceSystem) CRS.decode(EPSG_4326);
        CoordinateReferenceSystem targetCRS = (CoordinateReferenceSystem) CRS.decode(EPSG_4283);
        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
        GeometryFactory factory = new GeometryFactory();
        Point targetPoint = (Point) JTS.transform(factory
                .createPoint(new Coordinate(132.61, -26.98)), transform);
        String targetPointCoord1 = targetPoint.getCoordinate().x + " "
                + targetPoint.getCoordinate().y;
        targetPoint = (Point) JTS.transform(factory.createPoint(new Coordinate(132.71, -26.46)),
                transform);
        String targetPointCoord2 = targetPoint.getCoordinate().x + " "
                + targetPoint.getCoordinate().y;

        assertXpathEvaluatesTo("urn:x-ogc:def:crs:EPSG:4283",
                "//ex:geomContainer[@gml:id='1']/ex:geom/gml:Point/@srsName", doc);
        assertXpathEvaluatesTo("2",
                "//ex:geomContainer[@gml:id='1']/ex:geom/gml:Point/@srsDimension", doc);
        assertXpathEvaluatesTo(targetPointCoord1,
                "//ex:geomContainer[@gml:id='1']/ex:geom/gml:Point/gml:pos", doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4283",
                "//ex:geomContainer[@gml:id='1']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.1']/ex:geom/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//ex:geomContainer[@gml:id='1']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.1']/ex:geom/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                targetPointCoord1,
                "//ex:geomContainer[@gml:id='1']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.1']/ex:geom/gml:Point/gml:pos",
                doc);

        assertXpathEvaluatesTo("urn:x-ogc:def:crs:EPSG:4283",
                "//ex:geomContainer[@gml:id='2']/ex:geom/gml:Point/@srsName", doc);
        assertXpathEvaluatesTo("2",
                "//ex:geomContainer[@gml:id='2']/ex:geom/gml:Point/@srsDimension", doc);
        assertXpathEvaluatesTo(targetPointCoord2,
                "//ex:geomContainer[@gml:id='2']/ex:geom/gml:Point/gml:pos", doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4283",
                "//ex:geomContainer[@gml:id='2']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.2']/ex:geom/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//ex:geomContainer[@gml:id='2']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.2']/ex:geom/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                targetPointCoord2,
                "//ex:geomContainer[@gml:id='2']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.2']/ex:geom/gml:Point/gml:pos",
                doc);

    }

}
