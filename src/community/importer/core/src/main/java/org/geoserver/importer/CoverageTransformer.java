package org.geoserver.importer;

import java.io.File;
import java.io.IOException;

import org.opengis.util.ProgressListener;

/**
 * A tool that can be used to transform coverage files, in particular compress and retile them and eventually add overviews to them
 * 
 * @author Andrea Aime - GeoSolutions
 */
public interface CoverageTransformer {

    /**
     * Returns true if the file can be processed with this transformer
     * 
     * @param file
     * @return
     */
    public boolean accepts(File file) throws IOException;
    
    /**
     * Returns true if the file is inner tiled
     * @param file
     * @return
     * @throws IOException
     */
    public boolean isInnerTiled(File file) throws IOException;
    
    /**
     * Returns true if the file has overviews
     * @param file
     * @return
     * @throws IOException
     */
    public boolean hasOverviews(File file) throws IOException;
    
    /**
     * Returns true if the file is a proper Geotiff
     * @param file
     * @return
     */
    public boolean isGeotiff(File file) throws IOException;

    /**
     * Rewrites the input file into the target position using the specified compression and tiling options
     */
    public void rebuild(File input, File output, CompressionConfiguration compression,
            TilingConfiguration tiling, ProgressListener listener) throws IOException;

    /**
     * Adds overviews to the specified file
     */
    public void addOverviews(File file, CompressionConfiguration compression,
            TilingConfiguration tiling, OverviewConfiguration overviews,
            ProgressListener overviewListener) throws IOException;

}
