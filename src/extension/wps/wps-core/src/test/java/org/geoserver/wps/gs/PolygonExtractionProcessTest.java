/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

import java.util.ArrayList;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.GeoTools;
import org.geotools.process.raster.PolygonExtractionProcess;
import org.geotools.util.NullProgressListener;
import org.jaitools.numeric.Range;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Test class for the contour process.
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class PolygonExtractionProcessTest extends BaseRasterToVectorTest {

    /**
     * Test basic capabilities for the contour process. It works on the DEM tiff and produces a shapefile.
     * Nothing more nothing less.
     * @throws Exception
     */
    @Test
    public void testProcessStandaloneBasic() throws Exception {
    	final GridCoverage2D gc = (GridCoverage2D) getCatalog().getCoverageByName(DEM.getLocalPart()).getGridCoverage(null, GeoTools.getDefaultHints());
    	scheduleForDisposal(gc);
    	
    	final PolygonExtractionProcess process = new PolygonExtractionProcess();
		final SimpleFeatureCollection fc = process.execute(
				gc, 
				0,
				true, 
				null, 
				null,
				new ArrayList<Range>() {{
				    add(new Range(0d,true, 1000d,false));
				    add(new Range(1000d,true, 2000d,false));
				}},
				new NullProgressListener());
		

		assertNotNull(fc);
		assertTrue(fc.size() > 0);

		SimpleFeatureIterator fi = fc.features();
		while (fi.hasNext()) {
			SimpleFeature sf = fi.next();
			Double value = (Double) sf.getAttribute("value");
			assertTrue(value > 0 && value < 8);
		}
		fi.close();
    }

    

}
