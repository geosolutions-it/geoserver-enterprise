package org.geoserver.wps.gs;

import java.io.InputStream;

import org.geoserver.data.test.MockData;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.ppio.WFSPPIO;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class RiskSummaryTest extends WPSTestSupport {

	@Override
	protected void populateDataDirectory(MockData dataDirectory)
			throws Exception {
		super.populateDataDirectory(dataDirectory);
		dataDirectory.addWcs11Coverages();
	}

	public void testRisk() throws Exception {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
				+ "  <ows:Identifier>gs:RasterStatistics</ows:Identifier>\n"
				+ "  <wps:DataInputs>\n"
				+ "    <wps:Input>\n"
				+ "      <ows:Identifier>layerName</ows:Identifier>\n"
				+ "      <wps:Data>\n"
				+ "        <wps:LiteralData>" + getLayerId(MockData.TASMANIA_DEM) + "</wps:LiteralData>\n"
				+ "      </wps:Data>\n"
				+ "    </wps:Input>\n"
				+ "    <wps:Input>\n"
				+ "      <ows:Identifier>areaOfInterest</ows:Identifier>\n"
				+ "      <wps:Data>\n"
				+ "        <wps:ComplexData mimeType=\"application/wkt\"><![CDATA[POLYGON((145.5 -41.9, 145.5 -42.1, 145.6 -42, 145.5 -41.9))]]></wps:ComplexData>\n"
				+ "      </wps:Data>\n"
				+ "    </wps:Input>\n"
				+ "  </wps:DataInputs>\n"
				+ "  <wps:ResponseForm>\n"
				+ "    <wps:RawDataOutput mimeType=\"application/gml-2.1.2\">\n"
				+ "      <ows:Identifier>result</ows:Identifier>\n"
				+ "    </wps:RawDataOutput>\n" + "  </wps:ResponseForm>\n"
				+ "</wps:Execute>\n" + "\n" + "";

		MockHttpServletResponse response = postAsServletResponse(root(), xml);
		InputStream is = getBinaryInputStream(response);

		FeatureCollection output = (FeatureCollection) new WFSPPIO.WFS11()
				.decode(is);
		FeatureIterator iterator = output.features();

		while (iterator.hasNext()) {
			SimpleFeature feature = (SimpleFeature) iterator.next();

			assertEquals("132", feature.getAttribute("count"));
			assertEquals("162.0", feature.getAttribute("min"));
			assertEquals("945.0", feature.getAttribute("max"));
			assertEquals("55020.0", feature.getAttribute("sum"));
			assertEquals("416.8181818181819", feature.getAttribute("avg"));
			assertEquals("204.0965128000159", feature.getAttribute("stddev"));
		}

	}
}
