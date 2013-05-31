package org.geoserver.wfs.response;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.namespace.QName;

import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.WfsFactory;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ShapeZipTest extends WFSTestSupport {

    private static final QName ALL_TYPES = new QName(MockData.CITE_URI, "AllTypes",
            MockData.CITE_PREFIX);

    private static final QName ALL_DOTS = new QName(MockData.CITE_URI, "All.Types.Dots",
            MockData.CITE_PREFIX);

    private static final QName GEOMMID = new QName(MockData.CITE_URI, "geommid",
            MockData.CITE_PREFIX);

    private static final QName LONGNAMES = new QName(MockData.CITE_URI, "longnames",
            MockData.CITE_PREFIX);

    private static final QName NULLGEOM = new QName(MockData.CITE_URI, "nullgeom",
            MockData.CITE_PREFIX);

    private static final QName DOTS = new QName(MockData.CITE_URI, "dots.in.name",
            MockData.CITE_PREFIX);

    private Operation op;

    private GetFeatureType gft;


    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        Map params = new HashMap();
        params.put(MockData.KEY_SRS_NUMBER, "4326");
        dataDirectory.addPropertiesType(ALL_TYPES,
                ShapeZipTest.class.getResource("AllTypes.properties"), params);
        dataDirectory.addPropertiesType(ALL_DOTS,
                ShapeZipTest.class.getResource("All.Types.Dots.properties"), params);
        dataDirectory.addPropertiesType(GEOMMID,
                ShapeZipTest.class.getResource("geommid.properties"), params);
        dataDirectory.addPropertiesType(NULLGEOM,
                ShapeZipTest.class.getResource("nullgeom.properties"), params);
        dataDirectory.addPropertiesType(DOTS,
                ShapeZipTest.class.getResource("dots.in.name.properties"), params);
        dataDirectory.addPropertiesType(LONGNAMES,
                ShapeZipTest.class.getResource("longnames.properties"), params);
    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        gft = WfsFactory.eINSTANCE.createGetFeatureType();
        op = new Operation("GetFeature", getServiceDescriptor10(), null, new Object[] { gft });
    }

    public void testNoNativeProjection() throws Exception {
        byte[] zip = writeOut(getFeatureSource(MockData.BASIC_POLYGONS).getFeatures());

        checkShapefileIntegrity(new String[] { "BasicPolygons" }, new ByteArrayInputStream(zip));
    }

    public void testCharset() throws Exception {
        FeatureSource<? extends FeatureType, ? extends Feature> fs;
        fs = getFeatureSource(MockData.BASIC_POLYGONS);
        ShapeZipOutputFormat zip = new ShapeZipOutputFormat();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct = 
            FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(fs.getFeatures());

        // add the charset
        Map options = new HashMap();
        options.put("CHARSET", Charset.forName("ISO-8859-15"));
        gft.setFormatOptions(options);
        zip.write(fct, bos, op);

        checkShapefileIntegrity(new String[] { "BasicPolygons" },
                new ByteArrayInputStream(bos.toByteArray()));
        assertEquals("ISO-8859-15", getCharset(new ByteArrayInputStream(bos.toByteArray())));
    }
    
    public void testRequestUrlNoProxy() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("wfs?service=WFS&version=1.0.0" +
        		"&request=GetFeature&typeName=" + getLayerId(MockData.BASIC_POLYGONS) + "&outputFormat=SHAPE-ZIP");
        assertEquals("application/zip", response.getContentType());
        checkShapefileIntegrity(new String[] { "BasicPolygons" }, getBinaryInputStream(response));
        assertEquals("http://localhost:8080/geoserver/wfs?service=WFS&version=1.0.0&request=GetFeature&typeName=cite:BasicPolygons&outputFormat=SHAPE-ZIP", 
                getRequest(getBinaryInputStream(response)));
    }
    
    public void testRequestUrlWithProxyBase() throws Exception {
        // setup a proxy base url
        GeoServerInfo gs = getGeoServer().getGlobal();
        gs.getSettings().setProxyBaseUrl("https://www.geoserver.org/geoserver");
        getGeoServer().save(gs);
        
        // check it has been honored
        MockHttpServletResponse response = getAsServletResponse("wfs?service=WFS&version=1.0.0" +
                "&request=GetFeature&typeName=" + getLayerId(MockData.BASIC_POLYGONS) + "&outputFormat=SHAPE-ZIP");
        assertEquals("application/zip", response.getContentType());
        checkShapefileIntegrity(new String[] { "BasicPolygons" }, getBinaryInputStream(response));
        assertEquals("https://www.geoserver.org/geoserver/wfs?service=WFS&version=1.0.0&request=GetFeature&typeName=cite:BasicPolygons&outputFormat=SHAPE-ZIP", 
                getRequest(getBinaryInputStream(response)));
    }


    public void testMultiType() throws Exception {
        byte[] zip = writeOut(getFeatureSource(ALL_TYPES).getFeatures());

        final String[] expectedTypes = new String[] { "AllTypesPoint", "AllTypesMPoint",
                "AllTypesPolygon", "AllTypesLine" };
        checkShapefileIntegrity(expectedTypes, new ByteArrayInputStream(zip));
        checkFieldsAreNotEmpty(new ByteArrayInputStream(zip));
    }

    public void testMultiTypeDots() throws Exception {
        byte[] zip = writeOut(getFeatureSource(ALL_DOTS).getFeatures());

        final String[] expectedTypes = new String[] { "All_Types_DotsPoint",
                "All_Types_DotsMPoint", "All_Types_DotsPolygon", "All_Types_DotsLine" };
        checkShapefileIntegrity(expectedTypes, new ByteArrayInputStream(zip));
        checkFieldsAreNotEmpty(new ByteArrayInputStream(zip));
    }

    public void testGeometryInTheMiddle() throws Exception {
        byte[] zip = writeOut(getFeatureSource(GEOMMID).getFeatures());

        checkFieldsAreNotEmpty(new ByteArrayInputStream(zip));
    }

    public void testNullGeometries() throws Exception {
        byte[] zip = writeOut(getFeatureSource(NULLGEOM).getFeatures());

        final String[] expectedTypes = new String[] { "nullgeom" };
        checkShapefileIntegrity(expectedTypes, new ByteArrayInputStream(zip));
    }

    public void testLongNames() throws Exception {
        byte[] zip = writeOut(getFeatureSource(LONGNAMES).getFeatures());

        // check the result is not empty
        SimpleFeatureType schema = checkFieldsAreNotEmpty(new ByteArrayInputStream(zip));

        // check the schema is the expected one
        checkLongNamesSchema(schema);

        // run it again, we had a bug in which the remapped names changed at each run
        zip = writeOut(getFeatureSource(LONGNAMES).getFeatures());
        schema = checkFieldsAreNotEmpty(new ByteArrayInputStream(zip));
        checkLongNamesSchema(schema);

    }

    void checkLongNamesSchema(SimpleFeatureType schema) {
        assertEquals(4, schema.getAttributeCount());
        assertEquals("the_geom", schema.getDescriptor(0).getName().getLocalPart());
        assertEquals(MultiPolygon.class, schema.getDescriptor(0).getType().getBinding());
        assertEquals("FID", schema.getDescriptor(1).getName().getLocalPart());
        assertEquals("VERYLONGNA", schema.getDescriptor(2).getName().getLocalPart());
        assertEquals("VERYLONGN0", schema.getDescriptor(3).getName().getLocalPart());
    }

    public void testDots() throws Exception {
        byte[] zip = writeOut(getFeatureSource(DOTS).getFeatures());

        final String[] expectedTypes = new String[] { "dots_in_name" };
        checkShapefileIntegrity(expectedTypes, new ByteArrayInputStream(zip));
        checkFieldsAreNotEmpty(new ByteArrayInputStream(zip));
    }

    public void testEmptyResult() throws Exception {
        byte[] zip = writeOut(getFeatureSource(MockData.BASIC_POLYGONS).getFeatures(Filter.EXCLUDE));

        checkShapefileIntegrity(new String[] { "BasicPolygons" }, new ByteArrayInputStream(zip));
    }

    public void testEmptyResultMultiGeom() throws Exception {
        byte[] zip = writeOut(getFeatureSource(ALL_DOTS).getFeatures(Filter.EXCLUDE));

        final String[] expectedTypes = new String[] { "All_Types_Dots" };
        checkShapefileIntegrity(expectedTypes, new ByteArrayInputStream(zip));

        boolean foundReadme = false;
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zip));
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            foundReadme |= entry.getName().equals("README.TXT");
        }
    }

    public void testTemplateSingleType() throws Exception {
        // copy the new template to the data dir
        WorkspaceInfo ws = getCatalog().getWorkspaceByName(MockData.BASIC_POLYGONS.getPrefix());
        getDataDirectory().copyToWorkspaceDir(ws,
                getClass().getResourceAsStream("shapeziptest.ftl"), "shapezip.ftl");

        // setup the request params
        SimpleFeatureCollection fc = getFeatureSource(MockData.BASIC_POLYGONS).getFeatures(
                Filter.INCLUDE);
        ShapeZipOutputFormat zip = new ShapeZipOutputFormat();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct = 
            FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(fc);

        // get the file name
        assertEquals("shapezip_BasicPolygons.zip", zip.getAttachmentFileName(fct, op));

        // check the contents
        zip.write(fct, bos, op);
        byte[] zipBytes = bos.toByteArray();
        checkShapefileIntegrity(new String[] { "theshape_BasicPolygons" },
                new ByteArrayInputStream(zipBytes));
    }

    public void testTemplateMultiType() throws Exception {
        // copy the new template to the data dir
        WorkspaceInfo ws = getCatalog().getWorkspaceByName(MockData.BASIC_POLYGONS.getPrefix());
        getDataDirectory().copyToWorkspaceDir(ws,
                getClass().getResourceAsStream("shapeziptest.ftl"), "shapezip.ftl");

        // setup the request params
        ShapeZipOutputFormat zip = new ShapeZipOutputFormat();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct = 
            FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(getFeatureSource(MockData.BASIC_POLYGONS).getFeatures(Filter.INCLUDE));
        fct.getFeature().add(getFeatureSource(MockData.BRIDGES).getFeatures(Filter.INCLUDE));

        // get the file name
        assertEquals("shapezip_BasicPolygons.zip", zip.getAttachmentFileName(fct, op));

        // check the contents
        zip.write(fct, bos, op);
        byte[] zipBytes = bos.toByteArray();
        checkShapefileIntegrity(new String[] { "theshape_BasicPolygons", "theshape_Bridges" },
                new ByteArrayInputStream(zipBytes));
    }

    public void testTemplateMultiGeomType() throws Exception {
        // copy the new template to the data dir
        WorkspaceInfo ws = getCatalog().getWorkspaceByName(ALL_DOTS.getPrefix());
        getDataDirectory().copyToWorkspaceDir(ws,
                getClass().getResourceAsStream("shapeziptest.ftl"), "shapezip.ftl");

        // setup the request params
        ShapeZipOutputFormat zip = new ShapeZipOutputFormat();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct = 
            FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(getFeatureSource(ALL_DOTS).getFeatures(Filter.INCLUDE));

        // get the file name
        assertEquals("shapezip_All_Types_Dots.zip", zip.getAttachmentFileName(fct, op));

        // check the contents
        zip.write(fct, bos, op);
        byte[] zipBytes = bos.toByteArray();
        checkShapefileIntegrity(new String[] { "theshape_All_Types_DotsPoint",
                "theshape_All_Types_DotsMPoint", "theshape_All_Types_DotsPolygon",
                "theshape_All_Types_DotsLine" }, new ByteArrayInputStream(zipBytes));
    }

    public void testTemplatePOSTRequest10() throws Exception {
        String xml = "<wfs:GetFeature " + "service=\"WFS\" " + "version=\"1.0.0\" "
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "outputFormat=\"shape-zip\" "
                + "> " + "<wfs:Query typeName=\"cdf:Other\"> " + "</wfs:Query> "
                + "</wfs:GetFeature>";

        MockHttpServletResponse response = postAsServletResponse("wfs", xml);
        assertEquals("application/zip", response.getContentType());
    }

    public void testOutputZipFileNameSpecifiedInFormatOptions() throws Exception {
        ShapeZipOutputFormat zip = new ShapeZipOutputFormat(getGeoServer(), getCatalog(),
                getResourceLoader());

        FeatureCollectionResponse mockResult = 
            FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        mockResult.getFeature().add(getFeatureSource(ALL_DOTS).getFeatures(Filter.INCLUDE));

        GetFeatureType mockRequest = WfsFactory.eINSTANCE.createGetFeatureType();

        Operation mockOperation = new Operation("GetFeature", getServiceDescriptor10(), null,
                new Object[] { mockRequest });

        assertEquals("All_Types_Dots.zip",  zip.getAttachmentFileName(mockResult, mockOperation));

        mockRequest.getFormatOptions().put("FILENAME", "REQUEST_SUPPLIED_FILENAME.zip");

        assertEquals("REQUEST_SUPPLIED_FILENAME.zip",  zip.getAttachmentFileName(mockResult, mockOperation));

    }

    public void testTemplatePOSTRequest11() throws Exception {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<GetFeature xmlns=\"http://www.opengis.net/wfs\" xmlns:DigitalGlobe=\"http://www.digitalglobe.com\"\n"
                + "    xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "    xmlns:gml=\"http://www.opengis.net/gml\" service=\"WFS\" version=\"1.1.0\"\n"
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "    outputFormat=\"shape-zip\" maxFeatures=\"100\" handle=\"\">\n"
                + "    <Query typeName=\"cdf:Other\" srsName=\"urn:ogc:def:crs:EPSG::4326\">"
                + "</Query> " + "</GetFeature>";

        MockHttpServletResponse response = postAsServletResponse("wfs", xml);
        assertEquals("application/zip", response.getContentType());
    }

    public void testESRIFormat() throws Exception {
        setupESRIPropertyFile();
        FeatureSource<? extends FeatureType, ? extends Feature> fs;
        fs = getFeatureSource(MockData.BASIC_POLYGONS);
        ShapeZipOutputFormat zip = new ShapeZipOutputFormat(getGeoServer(), getCatalog(),
                getResourceLoader());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct = 
            FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(fs.getFeatures());

        // add the charset
        Map options = new HashMap();
        options.put("PRJFILEFORMAT", "ESRI");
        gft.setFormatOptions(options);
        zip.write(fct, bos, op);

        byte[] byteArrayZip = bos.toByteArray();
        checkShapefileIntegrity(new String[] { "BasicPolygons" }, new ByteArrayInputStream(
                byteArrayZip));

        checkFileContent("BasicPolygons.prj", new ByteArrayInputStream(byteArrayZip),
                get4326_ESRI_WKTContent());
    }

    public void testESRIFormatMultiType() throws Exception {
        setupESRIPropertyFile();
        ShapeZipOutputFormat zip = new ShapeZipOutputFormat(getGeoServer(), getCatalog(),
                getResourceLoader());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct = 
            FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(getFeatureSource(ALL_TYPES).getFeatures());
        Map options = new HashMap();
        options.put("PRJFILEFORMAT", "ESRI");
        gft.setFormatOptions(options);
        zip.write(fct, bos, op);
        byte[] byteArrayZip = bos.toByteArray();

        final String[] expectedTypes = new String[] { "AllTypesPoint", "AllTypesMPoint",
                "AllTypesPolygon", "AllTypesLine" };
        checkShapefileIntegrity(expectedTypes, new ByteArrayInputStream(byteArrayZip));

        for (String fileName : expectedTypes) {
            checkFileContent(fileName + ".prj", new ByteArrayInputStream(byteArrayZip),
                    get4326_ESRI_WKTContent());
        }
    }

    public void testESRIFormatFromDefaultValue() throws Exception {
        setupESRIPropertyFile();

        final GeoServer geoServer = getGeoServer();
        setupESRIFormatByDefault(geoServer, true);

        final FeatureSource fs = getFeatureSource(MockData.BASIC_POLYGONS);
        final Catalog catalog = getCatalog();
        final GeoServerResourceLoader resourceLoader = getResourceLoader();

        ShapeZipOutputFormat zip = new ShapeZipOutputFormat(geoServer, catalog, resourceLoader);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct = 
            FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(fs.getFeatures());

        // add the charset
        Map options = new HashMap();
        gft.setFormatOptions(options);
        zip.write(fct, bos, op);

        byte[] byteArrayZip = bos.toByteArray();
        checkShapefileIntegrity(new String[] { "BasicPolygons" }, new ByteArrayInputStream(
                byteArrayZip));

        checkFileContent("BasicPolygons.prj", new ByteArrayInputStream(byteArrayZip),
                get4326_ESRI_WKTContent());
    }

    /**
     * Saves the feature source contents into a zipped shapefile, returns the output as a byte array
     */
    byte[] writeOut(FeatureCollection fc) throws IOException {
        ShapeZipOutputFormat zip = new ShapeZipOutputFormat();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        FeatureCollectionResponse fct = 
            FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());
        fct.getFeature().add(fc);
        zip.write(fct, bos, op);
        return bos.toByteArray();
    }

    private File createTempFolder(String prefix) throws IOException {
        File temp = File.createTempFile(prefix, null);

        temp.delete();
        temp.mkdir();
        return temp;
    }

    private void copyStream(InputStream inStream, OutputStream outStream) throws IOException {
        int count = 0;
        byte[] buf = new byte[8192];
        while ((count = inStream.read(buf, 0, 8192)) != -1)
            outStream.write(buf, 0, count);
    }

    private SimpleFeatureType checkFieldsAreNotEmpty(InputStream in) throws IOException {
        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry entry = null;

        File tempFolder = createTempFolder("shp_");
        String shapeFileName = "";
        while ((entry = zis.getNextEntry()) != null) {
            final String name = entry.getName();
            String outName = tempFolder.getAbsolutePath() + File.separatorChar + name;
            // store .shp file name
            if (name.toLowerCase().endsWith("shp"))
                shapeFileName = outName;
            // copy each file to temp folder

            FileOutputStream outFile = new FileOutputStream(outName);
            copyStream(zis, outFile);
            outFile.close();
            zis.closeEntry();
        }
        zis.close();

        // create a datastore reading the uncompressed shapefile
        File shapeFile = new File(shapeFileName);
        ShapefileDataStore ds = new ShapefileDataStore(shapeFile.toURL());
        SimpleFeatureSource fs = ds.getFeatureSource();
        SimpleFeatureCollection fc = fs.getFeatures();
        SimpleFeatureType schema = fc.getSchema();

        Iterator<SimpleFeature> iter = fc.iterator();
        try {
            // check that every field has a not null or "empty" value
            while (iter.hasNext()) {
                SimpleFeature f = iter.next();
                for (Object attrValue : f.getAttributes()) {
                    assertNotNull(attrValue);
                    if (Geometry.class.isAssignableFrom(attrValue.getClass()))
                        assertFalse("Empty geometry", ((Geometry) attrValue).isEmpty());
                    else
                        assertFalse("Empty value for attribute", attrValue.toString().trim()
                                .equals(""));
                }

            }
        } finally {
            fc.close(iter);
            tempFolder.delete();
        }

        return schema;

    }

    /**
     * Writes out an {@code esri.properties} file to {@code <data_dir>/user_projections/} with the
     * single entry: {@code 4326=<esri version of 4326 WKT>}
     * 
     * @throws IOException
     */
    private void setupESRIPropertyFile() throws IOException {
        String esri_properties = "4326=" + get4326_ESRI_WKTContent();
        InputStream input = new ByteArrayInputStream(esri_properties.getBytes());
        File directory = getResourceLoader().findOrCreateDirectory("user_projections");
        File file = new File(directory, "esri.properties");
        if (file.exists()) {
            file.delete();
        }
        org.geoserver.data.util.IOUtils.copy(input, file);
    }

    private void setupESRIFormatByDefault(GeoServer geoServer, Boolean value) throws IOException {
        WFSInfo wfsInfo = geoServer.getService(WFSInfo.class);
        MetadataMap metadata = wfsInfo.getMetadata();
        metadata.put(ShapeZipOutputFormat.SHAPE_ZIP_DEFAULT_PRJ_IS_ESRI, value);
        geoServer.save(wfsInfo);
    }

    private void checkShapefileIntegrity(String[] typeNames, final InputStream in)
            throws IOException {
        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry entry = null;

        final String[] extensions = new String[] { ".shp", ".shx", ".dbf", ".prj", ".cst" };
        Set names = new HashSet();
        for (String name : typeNames) {
            for (String extension : extensions) {
                names.add(name + extension);
            }
        }
        while ((entry = zis.getNextEntry()) != null) {
            final String name = entry.getName();
            if(name.endsWith(".txt")) {
                // not part of the shapefile, it's the request dump
                continue;
            }
            assertTrue("Missing " + name, names.contains(name));
            names.remove(name);
            zis.closeEntry();
        }
        zis.close();
    }

    /**
     * Asserts the contents for the file named {@code fileName} contained in the zip file given by the {@code zippedIn}
     * matched the {@code expectedContent}
     */
    private void checkFileContent(final String fileName, final InputStream zippedIn,
            final String expectedContent) throws IOException {

        ZipInputStream zis = new ZipInputStream(zippedIn);
        ZipEntry entry = null;
        try {
            while ((entry = zis.getNextEntry()) != null) {
                try {
                    final String name = entry.getName();
                    if (name.toLowerCase().endsWith(fileName.toLowerCase())) {
                        String unzippedFileContents = IOUtils.toString(zis);
                        assertEquals(expectedContent, unzippedFileContents);
                        return;
                    }
                } finally {
                    zis.closeEntry();
                }
            }
        } finally {
            zis.close();
        }
        fail(fileName + " was not found in the provided stream");
    }

    private String getCharset(final InputStream in) throws IOException {
        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry entry = null;
        byte[] bytes = new byte[1024];
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().endsWith(".cst")) {
                zis.read(bytes);
            }
        }
        zis.close();

        if (bytes == null)
            return null;
        else
            return new String(bytes).trim();
    }
    
    private String getRequest(final InputStream in) throws IOException {
        ZipInputStream zis = new ZipInputStream(in);
        ZipEntry entry = null;
        byte[] bytes = new byte[1024];
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.getName().endsWith(".txt")) {
                zis.read(bytes);
            }
        }
        zis.close();

        if (bytes == null)
            return null;
        else
            return new String(bytes).trim();
    }

    /**
     * @return
     */
    private String get4326_ESRI_WKTContent() {
        return "GEOGCS[\"GCS_WGS_1984\",DATUM[\"D_WGS_1984\","
                + "SPHEROID[\"WGS_1984\",6378137.0,298.257223563]]," + "PRIMEM[\"Greenwich\",0.0],"
                + "UNIT[\"Degree\",0.0174532925199433]]";
    }
}
