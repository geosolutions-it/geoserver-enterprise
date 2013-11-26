package org.geoserver.wps;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;

import javax.xml.namespace.QName;

import org.geoserver.data.test.MockData;
import org.geoserver.wps.raster.algebra.AlgebricProcess;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.NullProgressListener;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

public class AlgebricProcessTest extends WPSTestSupport {

    private static final int BBOX_DIM = 500;

    private static CoordinateReferenceSystem EPSG_4326 = null;

    static {
        try {
            EPSG_4326 = CRS.decode("EPSG:4326");
        } catch (Exception e) {

        }
    }

    AlgebricProcess process;

    public static QName srtm_39_04_1 = new QName(WCS_URI, "srtm_39_04_1_mod", WCS_PREFIX);

    public static QName srtm_39_04_2 = new QName(WCS_URI, "srtm_39_04_2", WCS_PREFIX);

    public static QName srtm_39_04_3 = new QName(WCS_URI, "srtm_39_04_3", WCS_PREFIX);

    public static QName srtm_39_04 = new QName(WCS_URI, "srtm_39_04", WCS_PREFIX);

    public static QName asott = new QName(WCS_URI, "acque_sotterranee", WCS_PREFIX);

    public static QName asup = new QName(WCS_URI, "acque_superficiali", WCS_PREFIX);

    public static QName aagr = new QName(WCS_URI, "aree_agricole", WCS_PREFIX);

    public static boolean testExecuted;

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWcs11Coverages();

        testExecuted = true;

        try {
            dataDirectory.addCoverage(srtm_39_04_1,
                    getClass().getResource("srtm_39_04_1_mod.tiff"), MockData.TIFF, "raster.sld");
            dataDirectory.addCoverage(srtm_39_04_2, getClass().getResource("srtm_39_04_2.tiff"),
                    MockData.TIFF, "raster.sld");
            dataDirectory.addCoverage(srtm_39_04_3, getClass().getResource("srtm_39_04_3.tiff"),
                    MockData.TIFF, "raster.sld");
            dataDirectory.addCoverage(srtm_39_04, getClass().getResource("srtm_39_04.tiff"),
                    MockData.TIFF, "raster.sld");

            dataDirectory.addCoverage(asott, getClass().getResource("acque_sotterranee.tif"),
                    MockData.TIFF, "raster.sld");
            dataDirectory.addCoverage(asup, getClass().getResource("acque_superficiali.tif"),
                    MockData.TIFF, "raster.sld");
            dataDirectory.addCoverage(aagr, getClass().getResource("aree_agricole.tif"),
                    MockData.TIFF, "raster.sld");
        } catch (Exception e) {
            testExecuted = false;
        }

    }

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        process = new AlgebricProcess(catalog);
    }

    public void testWithTwoCoverages() {

        if (testExecuted) {
            ReferencedEnvelope worldEnv = new ReferencedEnvelope(-180, 180, -90, 90, EPSG_4326);

            GridCoverage2D coverage = process.execute(null, "srtm_39_04_1_mod,srtm_39_04_2",
                    Operator.SUM, null, null, null, worldEnv, BBOX_DIM, BBOX_DIM,
                    new NullProgressListener());

            assertNotNull(coverage);

            assertTrue(CRS.equalsIgnoreMetadata(coverage.getCoordinateReferenceSystem(), EPSG_4326));

            GridGeometry2D gridGeometry = coverage.getGridGeometry();

            GridEnvelope gridRange = gridGeometry.getGridRange();

            assertEquals(BBOX_DIM, gridRange.getHigh(0) + 1);
            assertEquals(BBOX_DIM, gridRange.getHigh(1) + 1);

            ReferencedEnvelope coverageEnv = new ReferencedEnvelope(coverage.getEnvelope2D());

            Envelope coverage_env = coverageEnv;
            Envelope world_env = worldEnv;
            
            assertTrue(worldEnv.contains(coverage_env) && coverageEnv.contains(world_env));
        } else {
            System.err
                    .println("WARNING:AlgebricProcessTest.testWithTwoCoverages() not executed because the images are not present");
        }

    }

    public void testWithThreeCoverages() {
        if (testExecuted) {
            ReferencedEnvelope worldEnv = new ReferencedEnvelope(-90, 90, -90, 90, EPSG_4326);

            GridCoverage2D coverage = process.execute(null,
                    "acque_sotterranee,acque_superficiali,aree_agricole", Operator.SUM, 0d, 0d, 0d,
                    worldEnv, BBOX_DIM, BBOX_DIM, new NullProgressListener());

            assertNotNull(coverage);

            assertTrue(CRS.equalsIgnoreMetadata(coverage.getCoordinateReferenceSystem(), EPSG_4326));

            GridGeometry2D gridGeometry = coverage.getGridGeometry();

            GridEnvelope gridRange = gridGeometry.getGridRange();

            assertEquals(BBOX_DIM, gridRange.getHigh(0) + 1);
            assertEquals(BBOX_DIM, gridRange.getHigh(1) + 1);

            ReferencedEnvelope coverageEnv = new ReferencedEnvelope(coverage.getEnvelope2D());

            Envelope coverage_env = coverageEnv;
            Envelope world_env = worldEnv;
           
            assertTrue(worldEnv.contains(coverage_env) && coverageEnv.contains(world_env));

        } else {
            System.err
                    .println("WARNING:AlgebricProcessTest.testWithTwoCoverages() not executed because the images are not present");
        }

    }
}
