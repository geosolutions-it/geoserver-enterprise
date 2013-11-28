package org.geoserver.wps.raster.algebra;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.wps.raster.GridCoverage2DRIA;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.process.raster.gs.AlgebricCoverageProcess;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.util.ProgressListener;

import com.google.common.collect.Lists;

public class AlgebricProcess implements GSProcess {

    Catalog catalog;

    public AlgebricProcess(Catalog catalog) {
        super();
        this.catalog = catalog;
    }

    @DescribeResult(name = "result", description = "Output raster")
    public GridCoverage2D execute(

            // Input unused parameter
            @DescribeParameter(name = "data", description = "Input features") SimpleFeatureCollection obsFeatures,
            // Input Rasters
            @DescribeParameter(name = "coverages", description = "Input coverage names") String coverageString,
            // Operation to execute
            @DescribeParameter(name = "operation", description = "Operation to execute") Operator operation,
            // Destination No Data value
            @DescribeParameter(name = "destinationNoData", description = "Destination NoData value") Double destNoData,
            // Minimum No Data value to execute
            @DescribeParameter(name = "NoDataMin", description = "Minimum No Data used for calculations", min = 0) Double noDataMin,
            // Maximum No Data value to execute
            @DescribeParameter(name = "NoDataMax", description = "Maximum No Data used for calculations", min = 0) Double noDataMax,
            // output image parameters
            @DescribeParameter(name = "outputBBOX", description = "Bounding box of the output") ReferencedEnvelope outputEnv,
            @DescribeParameter(name = "outputWidth", description = "Width of output raster in pixels") Integer outputWidth,
            @DescribeParameter(name = "outputHeight", description = "Height of output raster in pixels") Integer outputHeight,

            ProgressListener monitor) throws Exception {
        // Selection of the hints
        final Hints hints = GeoTools.getDefaultHints().clone();

        // coverage factory
        GridCoverageFactory gridCoverageFactory = CoverageFactoryFinder
                .getGridCoverageFactory(hints);

        // List of all the image coverages
        List<GridCoverage2D> coverages = new ArrayList<GridCoverage2D>();
        // Selection of the coverage names
        String[] coverageNames = coverageString.split(",");
        // List of all the names associated with each coverage
        List<String> coveragesList = Lists.newArrayList(coverageNames);

        // Rectangle containing the output raster dimensions
        Rectangle rect = new Rectangle(0, 0, outputWidth, outputHeight);
        GridEnvelope gridRange = new GridEnvelope2D(rect);
        // Creation of a GridGeometry object containing the requested bounding box and the output raster grid 
        GridGeometry2D gridGeo = new GridGeometry2D(gridRange, outputEnv);
        // Coverage List collector used for grouping all the input coverages
        ListCoverageCollector collector = new ListCoverageCollector(catalog, gridGeo, hints);
        collector.collect(coveragesList);

        // Get the sources
        final Map<String, GridCoverage2D> mapCoverage = collector.getCoverages();
        // Get the gridGeometry
        final GridGeometry2D destGridGeometry = collector.getGridGeometry();
        // Cycle on all the source coverages
        final GeoTiffFormat format = new GeoTiffFormat();
        for (Map.Entry<String, GridCoverage2D> entry : mapCoverage.entrySet()) {
            // use GridCoverage2DRIA for adapting GridGeometry between sources
            GridCoverage2D coverage = entry.getValue();

            if (coverage == null) {
                continue;
            }
//            GridCoverageWriter writer1 = format.getWriter(new File("/home/geosolutions/original.tiff"));
//            try {
//                writer1.write(coverage, null);
//            } catch (IOException e) {
//            } finally {
//                try {
//                    writer1.dispose();
//                } catch (Throwable e) {
//                }
//            }
            
            // Setting of the input coverage to the desired resolution
            final GridCoverage2DRIA input = GridCoverage2DRIA
                    .create(coverage, 
                            destGridGeometry,
                            org.geotools.resources.coverage.CoverageUtilities.getBackgroundValues(coverage)[0]
                            ); 
            
            // Selection of the Properties associated to the coverage
            Map coverageProperties = coverage.getProperties();

            GridCoverage2D finalCoverage = gridCoverageFactory.create(
                    coverage.getName(), 
                    input, 
                    destGridGeometry,
                    coverage.getSampleDimensions(), 
                    null, 
                    coverageProperties);
            
//            GridCoverageWriter writer = format.getWriter(new File("/home/geosolutions/gc2ria.tiff"));
//            try {
//                writer.write(finalCoverage, null);
//            } catch (IOException e) {
//            } finally {
//                try {
//                    writer.dispose();
//                } catch (Throwable e) {
//                }
//            }

            // Addition to the coverages list
            coverages.add(finalCoverage);
        }

        // If no coverages are present, then an exception is thrown
        if (coverages.isEmpty()) {
            throw new ProcessException("No Coverages present");
        }
        // Algebric process
        AlgebricCoverageProcess process = new AlgebricCoverageProcess();

        int dataType = coverages.get(0).getRenderedImage().getSampleModel().getDataType();

        Range noData = rangeCreation(dataType, noDataMin, noDataMax);

        GridCoverage2D finalCoverage = process.execute(coverages, operation, null, noData,
                destNoData, hints);
        
        
//        GridCoverageWriter writer = format.getWriter(new File("/home/geosolutions/test.tiff"));
//        try {
//            writer.write(finalCoverage, null);
//        } catch (IOException e) {
//        } finally {
//            try {
//                writer.dispose();
//            } catch (Throwable e) {
//            }
//        }

        return finalCoverage;
    }

    private Range rangeCreation(int dataType, Double noDataMin, Double noDataMax) {
        // Range initialization
        Range noData = null;

        // Creation of the No Data
        if (noDataMin == null && noDataMax != null) {
            noDataMin = noDataMax;
        } else if (noDataMax == null && noDataMin != null) {
            noDataMax = noDataMin;
        }
        // Creation of the range associated to the coverage data type
        if (noDataMax != null && noDataMin != null) {
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                noData = RangeFactory.create(noDataMin.byteValue(), true, noDataMax.byteValue(),
                        true);
                break;
            case DataBuffer.TYPE_USHORT:
                noData = RangeFactory.createU(noDataMin.shortValue(), true, noDataMax.shortValue(),
                        true);
                break;
            case DataBuffer.TYPE_SHORT:
                noData = RangeFactory.create(noDataMin.shortValue(), true, noDataMax.shortValue(),
                        true);
                break;
            case DataBuffer.TYPE_INT:
                noData = RangeFactory
                        .create(noDataMin.intValue(), true, noDataMax.intValue(), true);
                break;
            case DataBuffer.TYPE_FLOAT:
                noData = RangeFactory.create(noDataMin.floatValue(), true, noDataMax.floatValue(),
                        true, true);
                break;
            case DataBuffer.TYPE_DOUBLE:
                noData = RangeFactory.create(noDataMin.doubleValue(), true,
                        noDataMax.doubleValue(), true, true);
                break;
            default:
                throw new IllegalArgumentException("Wrong coverage data type");
            }
        }

        return noData;
    }

}
