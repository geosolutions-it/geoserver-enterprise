package org.geoserver.wfs.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import net.opengis.wfs.FeatureCollectionType;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.WfsFactory;

import org.geoserver.data.test.MockData;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import au.com.bytecode.opencsv.CSVReader;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


public class CSVOutputFormatTest extends WFSTestSupport {

    public void testFullRequest() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?version=1.1.0&request=GetFeature&typeName=sf:PrimitiveGeoFeature&outputFormat=csv");
        
        FeatureSource fs = getFeatureSource(MockData.PRIMITIVEGEOFEATURE);
        
//        System.out.println(resp.getOutputStreamContent());
        
        // check the mime type
        assertEquals("text/csv", resp.getContentType());
        
        // check the content disposition
        assertEquals("attachment; filename=PrimitiveGeoFeature.csv", resp.getHeader("Content-Disposition"));
        
        // read the response back with a parser that can handle escaping, newlines and what not
        List<String[]> lines = readLines(resp.getOutputStreamContent());
        
        // we should have one header line and then all the features in that feature type
        assertEquals(fs.getCount(Query.ALL) + 1, lines.size());
        
        for (String[] line : lines) {
            // check each line has the expected number of elements (num of att + 1 for the id)
            assertEquals(fs.getSchema().getDescriptors().size() + 1, line.length);
        }
    }
    
    public void testEscapes() throws Exception {
        // build some fake data in memory, the property data store cannot handle newlines in its data
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.add("geom", Point.class);
        builder.add("label", String.class);
        builder.setName("funnyLabels");
        SimpleFeatureType type = builder.buildFeatureType();
        
        GeometryFactory gf = new GeometryFactory();
        SimpleFeature f1 = SimpleFeatureBuilder.build(type, new Object[]{gf.createPoint(new Coordinate(5, 8)), "A label with \"quotes\""}, null);
        SimpleFeature f2 = SimpleFeatureBuilder.build(type, new Object[]{gf.createPoint(new Coordinate(5, 4)), "A long label\nwith newlines"}, null);
        
        MemoryDataStore data = new MemoryDataStore();
        data.addFeature(f1);
        data.addFeature(f2);
        SimpleFeatureSource fs = data.getFeatureSource("funnyLabels");
        
        // build the request objects and feed the output format
        GetFeatureType gft = WfsFactory.eINSTANCE.createGetFeatureType();
        Operation op = new Operation("GetFeature", getServiceDescriptor10(), null, new Object[] {gft});
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct = 
            FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(fs.getFeatures());
        
        // write out the results
        CSVOutputFormat format = new CSVOutputFormat(getGeoServer());
        format.write(fct, bos, op);
        
        // read the response back with a parser that can handle escaping, newlines and what not
        List<String[]> lines = readLines(bos.toString());
        
        // we should have one header line and then all the features in that feature type
        assertEquals(fs.getCount(Query.ALL) + 1, lines.size());
        
        for (String[] line : lines) {
            // check each line has the expected number of elements
            assertEquals(fs.getSchema().getAttributeCount() + 1, line.length);
        }
        
        // check we have the expected values in the string attributes
        assertEquals(f1.getAttribute("label"), lines.get(1)[2]);
        assertEquals(f2.getAttribute("label"), lines.get(2)[2]);
    }
    
    /**
     * Convenience to read the csv content and 
     * @param csvContent
     * @return
     * @throws IOException
     */
    private List<String[]> readLines(String csvContent) throws IOException  {
        CSVReader reader = new CSVReader(new StringReader(csvContent));
        
        List<String[]> result = new ArrayList<String[]>();
        String [] nextLine;
        while ((nextLine = reader.readNext()) != null) {
            result.add(nextLine);
        }
        return result;
    }
}
