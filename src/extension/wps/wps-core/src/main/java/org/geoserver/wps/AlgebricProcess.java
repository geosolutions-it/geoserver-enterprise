package org.geoserver.wps;

import it.geosolutions.jaiext.algebra.AlgebraDescriptor.Operator;
import it.geosolutions.jaiext.range.Range;
import it.geosolutions.jaiext.range.RangeFactory;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.wps.raster.algebra.ListCoverageCollector;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.process.raster.gs.AlgebricCoverageProcess;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.coverage.grid.GridGeometry;
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
            @DescribeParameter(name = "NoDataMin", description = "Minimum No Data used for calculations", min=0) Double noDataMin,
            // Maximum No Data value to execute
            @DescribeParameter(name = "NoDataMax", description = "Maximum No Data used for calculations", min=0) Double noDataMax,
            // output image parameters
            @DescribeParameter(name = "outputBBOX", description = "Bounding box of the output") ReferencedEnvelope outputEnv,
            @DescribeParameter(name = "outputWidth", description = "Width of output raster in pixels") Integer outputWidth,
            @DescribeParameter(name = "outputHeight", description = "Height of output raster in pixels") Integer outputHeight,

            ProgressListener monitor) throws ProcessException {
        // Hypothesis = the input coverages are all in the same CRS
        // Selection of the hints
        final Hints hints = GeoTools.getDefaultHints().clone();
        // List of all the image coverages
        List<GridCoverage2D> coverages = new ArrayList<GridCoverage2D>();
        // Selection of the coverages
        String[] coverageNames = coverageString.split(",");

        List<String> coveragesList = Lists.newArrayList(coverageNames);
        // USE THE COVERAGE COLLECTOR FOR ELABORATING ALL THE DATA
        Rectangle rect = new Rectangle(0, 0, outputWidth, outputHeight);

        GridEnvelope gridRange = new GridEnvelope2D(rect);

        GridGeometry gridGeo = new GridGeometry2D(gridRange, outputEnv);

        ListCoverageCollector collector = new ListCoverageCollector(catalog, outputEnv, gridGeo,
                hints);

        collector.collect(coveragesList);

        Map<String, GridCoverage2D> mapCoverage;

        try {
            mapCoverage = collector.getCoverages();
        } catch (IOException e) {
            throw new ProcessException("Error while reading the coverages", e);
        }

        GridCoverage2D coverage;

        String elementString;

        for (String key : coveragesList) {
            elementString = catalog.getCoverageByName(key).prefixedName();

            coverage = mapCoverage.get(elementString);
            if (coverage != null) {
                coverages.add(coverage);
            }
        }
        // If no coverages are present, then an exception is thrown
        if (coverages.isEmpty()) {
            throw new ProcessException("No Coverages present");
        }

        // Algebric process
        AlgebricCoverageProcess process = new AlgebricCoverageProcess();
        
        Range noData = null;
        
        int dataType = coverages.get(0).getRenderedImage().getSampleModel().getDataType();
        
        if(noDataMin == null && noDataMax !=null){
            noDataMin = noDataMax;
        }else if(noDataMax == null && noDataMin !=null){
            noDataMax = noDataMin;
        }
        
        if(noDataMax != null && noDataMin !=null){
            switch(dataType){
            case DataBuffer.TYPE_BYTE:
                noData = RangeFactory.create(noDataMin.byteValue(), true,
                        noDataMax.byteValue(), true);
                break;
            case DataBuffer.TYPE_USHORT:
                noData = RangeFactory.createU(noDataMin.shortValue(), true,
                        noDataMax.shortValue(), true);
                break;
            case DataBuffer.TYPE_SHORT:
                noData = RangeFactory.create(noDataMin.shortValue(), true,
                        noDataMax.shortValue(), true);
                break;
            case DataBuffer.TYPE_INT:
                noData = RangeFactory.create(noDataMin.intValue(), true,
                        noDataMax.intValue(), true);
                break;
            case DataBuffer.TYPE_FLOAT:
                noData = RangeFactory.create(noDataMin.floatValue(), true,
                        noDataMax.floatValue(), true, true);
                break;
            case DataBuffer.TYPE_DOUBLE:
                noData = RangeFactory.create(noDataMin.doubleValue(), true,
                        noDataMax.doubleValue(), true, true);
                break;
            default:
                throw new IllegalArgumentException("Wrong coverage data type");
            }
        }
        return process.execute(coverages, operation, null, noData, destNoData, hints);
    }
    
    
    
    

}
