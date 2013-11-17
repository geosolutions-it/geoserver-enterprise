package org.geoserver.wps.gs;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.wps.ppio.FeatureAttribute;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

public class ToFeatureProcessTest extends GeoServerTestSupport {
    /**
     * Try to re-import buildings as another layer (different name, different projection)
     */
    public void testToFeature() throws Exception {
        ToFeature toFeatureProcess = new ToFeature();
		
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
		Geometry geometry = JTS.toGeometry(new GeneralDirectPosition(12, 24));
        List<FeatureAttribute> attributes = new ArrayList<FeatureAttribute>();
        
        attributes.add(new FeatureAttribute("theName", "theValue"));

        SimpleFeatureCollection features = toFeatureProcess.execute(geometry, crs, "test", attributes, null);
        
        assertNotNull(features);
        
        assertEquals(1, features.size());
        
        SimpleFeature feature = features.features().next();
        
        assertEquals(geometry, feature.getDefaultGeometryProperty().getValue());
        
        assertEquals("theValue", feature.getAttribute("theName"));
    }
}
