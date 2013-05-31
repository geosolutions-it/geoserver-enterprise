package org.geoserver.catalog.rest;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.geotools.util.logging.Logging;
import org.springframework.web.servlet.DispatcherServlet;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class RestConcurrencyTest extends CatalogRESTTestSupport {

    static volatile Exception exception;
    volatile DispatcherServlet dispatcher;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        exception = null;

        // Uncomment this and ... KA-BOOM!!!!
        // GeoServerConfigurationLock locker = (GeoServerConfigurationLock) applicationContext
        // .getBean("configurationLock");
        // locker.setEnabled(false);
    }

    protected void addPropertyDataStores(int typeCount) throws Exception {
        ByteArrayOutputStream zbytes = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(zbytes);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < typeCount; i++) {
            String name = "pds" + i;
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(bytes));
            writer.write("_=name:String,pointProperty:Point\n");
            writer.write(name + ".0='zero'|POINT(0 0)\n");
            writer.write(name + ".1='one'|POINT(1 1)\n");
            writer.flush();

            zout.putNextEntry(new ZipEntry(name + ".properties"));
            zout.write(bytes.toByteArray());
            bytes.reset();
        }

        zout.flush();
        zout.close();

        put("/rest/workspaces/gs/datastores/pds/file.properties?configure=none",
                zbytes.toByteArray(), "application/zip");
    }

    @Override
    protected boolean useLegacyDataDirectory() {
        // if we don't do this the writes will be load and after the reload we won't get
        // the newly added layer
        return false;
    }
    
    @Override
    protected DispatcherServlet getDispatcher() throws Exception {
        if(dispatcher == null) {
            synchronized (this) {
                if(dispatcher == null) {
                    dispatcher = super.getDispatcher();
                }
            }
        }
        return dispatcher;
    }

    public void testFeatureTypeConcurrency() throws Exception {
        int typeCount = 5;
        addPropertyDataStores(typeCount);
        ExecutorService es = Executors.newCachedThreadPool();
        try {
            List<Future<Integer>> futures = new ArrayList<Future<Integer>>();
            for (int i = 0; i < typeCount; i++) {
                futures.add(es.submit(new AddRemoveFeatureTypeWorker("gs", "pds", "pds" + i, 5)));
            }
            for (Future<Integer> future : futures) {
                future.get();
            }
        } finally {
            es.shutdownNow();
        }
    }

    class AddRemoveFeatureTypeWorker implements Callable<Integer> {

        final Logger LOGGER = Logging.getLogger(RestConcurrencyTest.class);

        String typeName;

        String workspace;

        String store;

        int loops;

        public AddRemoveFeatureTypeWorker(String workspace, String store, String typeName, int loops) {
            this.typeName = typeName;
            this.workspace = workspace;
            this.store = store;
            this.loops = loops;
        }

        @Override
        public Integer call() throws Exception {
            try {
                callInternal();
            } catch (Exception e) {
                exception = e;
                throw e;
            }

            return loops;
        }

        private void callInternal() throws Exception {
            doLogin();
            String threadId = Thread.currentThread().getId() + " ";
            for (int i = 0; i < loops && exception == null; i++) {
                // add the type name
                String base = "/rest/workspaces/" + workspace + "/datastores/" + store
                        + "/featuretypes/";
                String xml = "<featureType>" + "<name>" + typeName + "</name>" + "<nativeName>"
                        + typeName + "</nativeName>" + "<srs>EPSG:4326</srs>"
                        + "<nativeCRS>EPSG:4326</nativeCRS>" + "<nativeBoundingBox>"
                        + "<minx>0.0</minx>" + "<maxx>1.0</maxx>" + "<miny>0.0</miny>"
                        + "<maxy>1.0</maxy>" + "<crs>EPSG:4326</crs>" + "</nativeBoundingBox>"
                        + "<store>" + store + "</store>" + "</featureType>";

                LOGGER.info(threadId + "Adding " + typeName);
                MockHttpServletResponse response = postAsServletResponse(base, xml, "text/xml");

                assertEquals(201, response.getStatusCode());
                assertNotNull(response.getHeader("Location"));
                assertTrue(response.getHeader("Location").endsWith(base + typeName));

                // check it's there
                LOGGER.info(threadId + "Checking " + typeName);
                String resourcePath = "/rest/layers/" + workspace + ":" + typeName;
                response = getAsServletResponse(resourcePath + ".xml");
                assertEquals(200, response.getStatusCode());

                // reload
                LOGGER.info(threadId + "Reloading catalog");
                assertEquals(200, postAsServletResponse("/rest/reload", "").getStatusCode());

                // remove it
                LOGGER.info(threadId + "Removing layer");
                String deletePath = resourcePath + "?recurse=true";
                assertEquals(200, deleteAsServletResponse(deletePath).getStatusCode());
            }
        }
    }
}
