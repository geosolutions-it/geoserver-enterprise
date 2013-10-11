package org.geoserver.wps.gs;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.geoserver.wps.raster.GridCoverage2DRIA;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;

/**
 *
 * @author ETj <etj at geo-solutions.it>
 */
public class Coverage2RenderedImageAdapterTest extends TestCase {

    protected final static double NODATA = 3.0d;

    public Coverage2RenderedImageAdapterTest() {
    }

    /**
     * creates a coverage for testing purposes-
     *
     * @param width width in pixel of the raster
     * @param height height in pixel of the raster
     * @param envX0 envelope minx
     * @param envY0 envelope miny
     * @param envWidth envelope width
     * @param envHeight envelope height
     * @return the test coverage
     */
    protected static GridCoverage2D createTestCoverage(final int width, final int height,
                    final double envX0, final double envY0,
                    final double envWidth, final double envHeight) {

        final GridCoverageFactory factory = CoverageFactoryFinder.getGridCoverageFactory(null);

        WritableRaster raster =
                RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, width, height, 1, null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if( x<50 && y<50 ) { // upper left square: vertical lines
                    if(x % 5 == 0)
                        raster.setSample(x, y, 0, 0);
                    else
                        raster.setSample(x, y, 0, width);
                }
                else if(x < 50 && y > height-50) { // lower left square: horizontal lines
                    if(y % 5 == 0)
                        raster.setSample(x, y, 0, 0);
                    else
                        raster.setSample(x, y, 0, width);
                }
                else if(x > width- 50 && y < 50) { // upper right square: descending diagonal lines
                    if( (x-y) % 5 == 0)
                        raster.setSample(x, y, 0, 0);
                    else
                        raster.setSample(x, y, 0, width);
                }
                else if(x > width- 50 && y > height-50) { // lower right square: ascending diagonal lines
                    if( (x+y) % 5 == 0)
                        raster.setSample(x, y, 0, 0);
                    else
                        raster.setSample(x, y, 0, width);
                }
                else if(x % 50 == 0 || y % 50 == 0 || (x - y) % 100 == 0)
                    raster.setSample(x, y, 0, 0); // bigger lines
                else
                    raster.setSample(x, y, 0, x+y); // normal background
            }
        }
        final Color[] colors = new Color[]{
            Color.BLUE, Color.CYAN, Color.WHITE, Color.YELLOW, Color.RED
        };

        return factory.create("Float coverage", raster,
                new Envelope2D(DefaultGeographicCRS.WGS84, envX0, envY0, envWidth, envHeight),
                null, null,
                null, new Color[][]{colors}, null);
    }

    public void testSame() throws InterruptedException {
        GridCoverage2D src = createTestCoverage(500, 500, 0,0, 10,10);
        GridCoverage2D dst = createTestCoverage(500, 500, 0,0, 10,10);

        GridCoverage2DRIA cria = GridCoverage2DRIA.create(src, dst, NODATA);

        //--- internal points should stay the same
        Point2D psrc = new Point2D.Double(2d,3d); // this is on dst gc
        Point2D pdst = cria.mapSourcePoint(psrc, 0);
        assertEquals(2d, pdst.getX());
        assertEquals(3d, pdst.getY());

        //--- external points should not be remapped
        psrc = new Point2D.Double(600d,600d); // this is on dst gc
        pdst = cria.mapSourcePoint(psrc, 0);
        assertNull(pdst);

    }

    public void testSameWorldSmallerDstRaster() throws InterruptedException {
        GridCoverage2D src = createTestCoverage(500,500, 0,0 ,10,10);
        GridCoverage2D dst = createTestCoverage(250,250, 0,0 ,10,10);

        GridCoverage2DRIA cria = GridCoverage2DRIA.create(src, dst, NODATA);

        //--- internal points should double coords (no interp on coords)
        Point2D psrc = new Point2D.Double(13d,16d); // this is on dst gc
        Point2D pdst = cria.mapSourcePoint(psrc, 0);
        assertNotNull("Can't convert " + psrc, pdst);
        assertEquals(6.5d, pdst.getX());
        assertEquals(8d, pdst.getY());

        //--- external points should not be remapped
        psrc = new Point2D.Double(600d,600d); // this is on dst gc
        pdst = cria.mapSourcePoint(psrc, 0);
        assertNull(pdst);

    }

    /**
     * Same raster dimension, subset word area
     * @throws InterruptedException
     */
    public void testSameRasterSmallerWorld() throws InterruptedException {
        GridCoverage2D src = createTestCoverage(500,500, 0,0 ,10,10);
        GridCoverage2D dst = createTestCoverage(500,500, 0,0 ,5,5);

        GridCoverage2DRIA cria = GridCoverage2DRIA.create(src, dst, NODATA);

        //--- origin of source is outside dest
        Point2D psrc = new Point2D.Double(0d,0d);
        Point2D pdst = cria.mapSourcePoint(psrc, 0);
        assertNull(pdst);
        
        //--- looking for origin of dest
        psrc = new Point2D.Double(0d,250d); // this is on dst gc
        pdst = cria.mapSourcePoint(psrc, 0);
        assertNotNull(pdst);
        assertEquals(0d, pdst.getX());
        assertEquals(0d, pdst.getY());
        psrc=cria.mapDestPoint(pdst, 0);
        assertNotNull(psrc);
        assertEquals(0d, psrc.getX());
        assertEquals(250d, psrc.getY());

        psrc = new Point2D.Double(250d,500d); // this is in src gc but outside dest
        pdst = cria.mapSourcePoint(psrc, 0);
        assertNull(pdst);
        
        psrc = new Point2D.Double(249d,499d); // this is in src gc but outside dest
        pdst = cria.mapSourcePoint(psrc, 0);
        assertNotNull(pdst);
        assertEquals(498d, pdst.getX());
        assertEquals(498d, pdst.getY());        
        // there should be no point in destination that gives nodata
        // as there is source everywhere
        Raster data= cria.getData();
        final RectIter iterator = RectIterFactory.create(data, null);
        iterator.startPixels();
        while(!iterator.finishedLines()){
            while(!iterator.finishedPixels()){
                double val=iterator.getSampleDouble();
                Assert.assertFalse("Value should not be noData", NODATA== val);
                iterator.nextPixel();
            }
            iterator.nextLine();
        }
        
        
    }

    /**
     * Same raster dimension, subset word area
     * @throws InterruptedException
     */
    public void testSameRasterTranslatedWorld0() throws InterruptedException {
        GridCoverage2D src = createTestCoverage(500,500, 0,0 ,5,5);
        GridCoverage2D dst = createTestCoverage(500,500, 2,2 ,5,5);

        GridCoverage2DRIA cria = GridCoverage2DRIA.create(src, dst, NODATA);

        //--- internal points should halves coords (no interp on coords)
        Point2D psrc = new Point2D.Double(0d,499d); // this is on src gc and outside dest gc
        Point2D pdst = cria.mapSourcePoint(psrc, 0);
        assertNull(pdst);
        double val = cria.getData().getSampleFloat(0, 0, 0);
        assertEquals("Value should be noData", NODATA, val);
        
        psrc = new Point2D.Double(200d,200d); // this is inside src gc and inside dest gc
        pdst = cria.mapSourcePoint(psrc, 0);
        assertNotNull(pdst);
        assertEquals(0d, pdst.getX());
        assertEquals(400d, pdst.getY());
        
        psrc = new Point2D.Double(400d,200d); // this is inside src gc and inside dest gc
        pdst = cria.mapSourcePoint(psrc, 0);
        assertNotNull(pdst);
        assertEquals(200d, pdst.getX());
        assertEquals(400d, pdst.getY());

        //--- points not inside dest but inside src shoud be remapped on a novalue cell
        psrc = new Point2D.Double(0d,0d); // this is on dst gc
        pdst = cria.mapSourcePoint(psrc, 0);
        assertNull(pdst); // should not map on src raster
        val = cria.getData().getSampleFloat(0, 0, 0);
        assertEquals("Value should be noData", NODATA, val);

    }


}
