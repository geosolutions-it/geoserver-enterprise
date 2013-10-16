package org.geoserver.wps.gs;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

public class WFSLogProcessTest extends GeoServerTestSupport {
    /**
     * Try to re-import buildings as another layer (different name, different projection)
     */
    public void testWFSLog() throws Exception {
        WFSLog wfsLogProcess = new WFSLog(getGeoServer());
		
        FeatureTypeInfo ti = getCatalog().getFeatureTypeByName(getLayerId(MockData.BUILDINGS));
        SimpleFeatureCollection rawSource = (SimpleFeatureCollection) ti.getFeatureSource(null,
                null).getFeatures();

        FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
        
        Filter filter = ff.equals(ff.property("FID"), ff.literal("113"));

        SimpleFeature feature = rawSource.subCollection(filter).features().next();
        
        SimpleFeatureCollection features = 
        	wfsLogProcess.execute(rawSource, MockData.BUILDINGS.getLocalPart(), MockData.CITE_PREFIX, MockData.CITE_PREFIX, filter, false, null);
        
        assertNotNull(features);
        
        assertEquals(1, features.size());
        
        feature = features.features().next();
        
        assertNotNull(feature);
    }
}
