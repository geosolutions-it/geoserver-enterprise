package org.geoserver.wps.gs;

import org.geoserver.wps.WPSTestSupport;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.feature.gs.BufferFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.PrecisionModel;

public class BufferFeatureCollectionTest extends WPSTestSupport {

    FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    public void testExecutePoint() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        for (int i = 0; i < 2; i++) {
            b.add(gf.createPoint(new Coordinate(i, i)));
            b.add(i);
            features.add(b.buildFeature(i + ""));
        }
        Double distance = new Double(500);
        BufferFeatureCollection process = new BufferFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, distance, null);
        assertEquals(2, output.size());

        SimpleFeatureIterator iterator = output.features();
        for (int i = 0; i < 2; i++) {
            Geometry expected = gf.createPoint(new Coordinate(i, i)).buffer(distance);
            SimpleFeature sf = iterator.next();
            assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
        }
    }

    public void testExecuteLineString() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate array[] = new Coordinate[4];
            int j = 0;
            for (int i = 0 + numFeatures; i < 4 + numFeatures; i++) {
                array[j] = new Coordinate(i, i);
                j++;
            }
            b.add(gf.createLineString(array));
            b.add(0);
            features.add(b.buildFeature(numFeatures + ""));
        }
        Double distance = new Double(500);
        BufferFeatureCollection process = new BufferFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, distance, null);
        assertEquals(5, output.size());
        SimpleFeatureIterator iterator = output.features();
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate[] array = new Coordinate[4];
            int j = 0;
            for (int i = 0 + numFeatures; i < 4 + numFeatures; i++) {
                array[j] = new Coordinate(i, i);
                j++;
            }
            Geometry expected = gf.createLineString(array).buffer(distance);
            SimpleFeature sf = iterator.next();
            assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
        }
    }

    public void testExecutePolygon() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate array[] = new Coordinate[4];
            int j = 0;
            for (int i = 0 + numFeatures; i < 3 + numFeatures; i++) {
                array[j] = new Coordinate(i, i);
                j++;
            }
            array[3] = new Coordinate(numFeatures, numFeatures);
            LinearRing shell = new LinearRing(array, new PrecisionModel(), 0);
            b.add(gf.createPolygon(shell, null));
            b.add(0);
            features.add(b.buildFeature(numFeatures + ""));
        }
        Double distance = new Double(500);
        BufferFeatureCollection process = new BufferFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, distance, null);
        assertEquals(5, output.size());
        SimpleFeatureIterator iterator = output.features();
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate[] array = new Coordinate[4];
            int j = 0;
            for (int i = 0 + numFeatures; i < 3 + numFeatures; i++) {
                array[j] = new Coordinate(i, i);
                j++;
            }
            array[3] = new Coordinate(numFeatures, numFeatures);
            LinearRing shell = new LinearRing(array, new PrecisionModel(), 0);
            Geometry expected = gf.createPolygon(shell, null).buffer(distance);

            SimpleFeature sf = iterator.next();
            assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
        }
    }

    public void testExecuteBufferAttribute() throws Exception {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("featureType");
        tb.add("geometry", Geometry.class);
        tb.add("integer", Integer.class);
        tb.add("buffer", Double.class);

        GeometryFactory gf = new GeometryFactory();
        SimpleFeatureBuilder b = new SimpleFeatureBuilder(tb.buildFeatureType());

        DefaultFeatureCollection features = new DefaultFeatureCollection(null, b.getFeatureType());
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate array[] = new Coordinate[4];
            int j = 0;
            for (int i = 0 + numFeatures; i < 3 + numFeatures; i++) {
                array[j] = new Coordinate(i, i);
                j++;
            }
            array[3] = new Coordinate(numFeatures, numFeatures);
            LinearRing shell = new LinearRing(array, new PrecisionModel(), 0);
            b.add(gf.createPolygon(shell, null));
            b.add(0);
            b.add(500);
            features.add(b.buildFeature(numFeatures + ""));
        }

        BufferFeatureCollection process = new BufferFeatureCollection();
        SimpleFeatureCollection output = process.execute(features, null, "buffer");
        assertEquals(5, output.size());
        SimpleFeatureIterator iterator = output.features();
        for (int numFeatures = 0; numFeatures < 5; numFeatures++) {
            Coordinate[] array = new Coordinate[4];
            int j = 0;
            for (int i = 0 + numFeatures; i < 3 + numFeatures; i++) {
                array[j] = new Coordinate(i, i);
                j++;
            }
            array[3] = new Coordinate(numFeatures, numFeatures);
            LinearRing shell = new LinearRing(array, new PrecisionModel(), 0);
            Geometry expected = gf.createPolygon(shell, null).buffer(500);

            SimpleFeature sf = iterator.next();
            assertTrue(expected.equals((Geometry) sf.getDefaultGeometry()));
        }
    }
}
