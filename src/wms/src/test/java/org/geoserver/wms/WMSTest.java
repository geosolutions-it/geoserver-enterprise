package org.geoserver.wms;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.Date;
import javax.xml.namespace.QName;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.opengis.filter.Filter;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class WMSTest extends WMSTestSupport {
    
    static final QName TIME_WITH_START_END = new QName(MockData.SF_URI, "TimeWithStartEnd", MockData.SF_PREFIX);
    WMS wms;
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        dataDirectory.addPropertiesType(TIME_WITH_START_END, 
                getClass().getResource("TimeElevationWithStartEnd.properties"),Collections.EMPTY_MAP);
    }

    protected void setupStartEndTimeDimension(String featureTypeName, String dimension, String start, String end) {
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(featureTypeName);
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute(start);
        di.setEndAttribute(end);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(dimension, di);
        getCatalog().save(info);
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        wms = new WMS(getGeoServer());
    }
    
    public void testGetTimeElevationToFilterStartEndDate() throws Exception {
        
        setupStartEndTimeDimension(TIME_WITH_START_END.getLocalPart(), "time", "startTime", "endTime");
        setupStartEndTimeDimension(TIME_WITH_START_END.getLocalPart(), "elevation", "startElevation", "endElevation");
        
        /* Reference for test assertions
        TimeElevation.0=0|2012-02-11|2012-02-12|1|2
        TimeElevation.1=1|2012-02-12|2012-02-13|2|3
        TimeElevation.2=2|2012-02-11|2011-05-13|1|3
         */
        
        doTimeElevationFilter( Date.valueOf("2012-02-10"), null);
        doTimeElevationFilter( Date.valueOf("2012-02-11"), null, 0, 2);
        doTimeElevationFilter( Date.valueOf("2012-02-12"), null, 0, 1, 2);
        doTimeElevationFilter( Date.valueOf("2012-02-13"), null, 1, 2);
        doTimeElevationFilter( Date.valueOf("2012-02-14"), null);
        
        doTimeElevationFilter( 
                new DateRange(Date.valueOf("2012-02-09"), Date.valueOf("2012-02-10")), null
        );
        doTimeElevationFilter( 
                new DateRange(Date.valueOf("2012-02-09"), Date.valueOf("2012-02-11")), null,
                0, 2
        );
        doTimeElevationFilter( 
                new DateRange(Date.valueOf("2012-02-11"), Date.valueOf("2012-02-13")), null,
                0, 1, 2
        );
        doTimeElevationFilter( 
                new DateRange(Date.valueOf("2012-02-09"), Date.valueOf("2012-02-14")), null,
                0, 1, 2
        );
        doTimeElevationFilter( 
                new DateRange(Date.valueOf("2012-02-13"), Date.valueOf("2012-02-14")), null,
                1, 2
        );
        doTimeElevationFilter( 
                new DateRange(Date.valueOf("2012-02-14"), Date.valueOf("2012-02-15")), null
        );
        
        doTimeElevationFilter( null, 0);
        doTimeElevationFilter( null, 1, 0 , 2);
        doTimeElevationFilter( null, 2, 0 , 1, 2);
        doTimeElevationFilter( null, 3, 1 , 2);
        doTimeElevationFilter( null, 4);
        
        doTimeElevationFilter( null, new NumberRange(Integer.class,-1,0));
        doTimeElevationFilter( null, new NumberRange(Integer.class,-1,1),0,2);
        doTimeElevationFilter( null, new NumberRange(Integer.class,1,3),0,1,2);
        doTimeElevationFilter( null, new NumberRange(Integer.class,-1,4),0,1,2);
        doTimeElevationFilter( null, new NumberRange(Integer.class,3,4),1,2);
        doTimeElevationFilter( null, new NumberRange(Integer.class,4,5));
        
        // combined date/elevation - this should be an 'and' filter
        doTimeElevationFilter( Date.valueOf("2012-02-12"), 2, 0, 1, 2);
        // disjunct verification
        doTimeElevationFilter( Date.valueOf("2012-02-11"), 3);
    }
    
    public void doTimeElevationFilter( Object time, Object elevation, Integer... expectedIds) throws Exception {
        
        FeatureTypeInfo timeWithStartEnd = getCatalog().getFeatureTypeByName(TIME_WITH_START_END.getLocalPart());
        FeatureSource fs = timeWithStartEnd.getFeatureSource(null, null);
        
        List times = time == null ? null : Arrays.asList(time);
        List elevations = elevation == null ? null : Arrays.asList(elevation);
                
        Filter filter = wms.getTimeElevationToFilter(times, elevations, timeWithStartEnd);
        FeatureCollection features = fs.getFeatures(filter);
        
        Set<Integer> results = new HashSet<Integer>();
        FeatureIterator it = features.features();
        while (it.hasNext()) {
            results.add( (Integer) it.next().getProperty("id").getValue());
        }
        assertTrue("expected " + Arrays.toString(expectedIds) + " but got " + results,
                results.containsAll(Arrays.asList(expectedIds)));
    }
    
}
