package org.geoserver.monitor;

import static org.junit.Assert.*;

import org.opengis.geometry.BoundingBox;

public class BBoxAsserts {

    /**
     * Asserts two BoundingBoxes are equal to within delta.
     * @param expected
     * @param result
     * @param delta
     */
    static public void assertEqualsBbox(BoundingBox expected, BoundingBox result, double delta) {
        assertNotNull(String.format("Expected %s but got null", expected),result);
        assertEquals(expected.getMaxX(), result.getMaxX(), delta);
        assertEquals(expected.getMinX(), result.getMinX(), delta);
        assertEquals(expected.getMaxY(), result.getMaxY(), delta);
        assertEquals(expected.getMinY(), result.getMinY(), delta);
        assertEquals(expected.getCoordinateReferenceSystem(), result.getCoordinateReferenceSystem());
    }
}
