package org.geoserver.wps.gs;

import org.geoserver.test.GeoServerTestSupport;

public class CmdLineProcessTest extends GeoServerTestSupport {
    /**
     * Try to re-import buildings as another layer (different name, different projection)
     */
    public void testCmdLine() throws Exception {
        CmdLine cmdLineProcess = new CmdLine(getGeoServer());

		// --silent --eval \"rxlevel({_SPL},{_LAT},{_LON},{_FILEPATH});\" --path C:/Programmi/MMRM-TDA/matlabcode2/
        // --verbose --eval \"rxlevel(1,40,6,\\\"C:/data/NURC-IDA/output/test_\\\");\" --path C:/data/NURC-IDA/matlabcode2/
        
		/*String results = cmdLineProcess.execute(
        		Arrays.asList(
        				"C:/Octave/3.2.4_gcc-4.4.0/bin/octave.exe", 
        				"--silent", "", 
        				"--eval", "\"rxlevel(1,40,6,\\\"C:/data/NURC-IDA/output/test_\\\");\"", 
        				"--path", "C:/data/NURC-IDA/matlabcode2/"),
        		new File("C:/data/NURC-IDA/output"),
        		true,
        		null);
        
        System.out.println(results);*/
//        assertNotNull(features);
//        
//        assertEquals(1, features.size());
//        
//        SimpleFeature feature = features.features().next();
//        
//        assertEquals(geometry, feature.getDefaultGeometryProperty().getValue());
//        
//        assertEquals("theValue", feature.getAttribute("theName"));
    }
}
