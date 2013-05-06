package org.geoserver.importer;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.referencing.CRS;
import org.geotools.resources.image.ImageUtilities;
import org.geotools.util.SimpleInternationalString;
import org.geotools.util.logging.Logging;
import org.geotools.utils.imageoverviews.OverviewsEmbedder;
import org.geotools.utils.progress.ExceptionEvent;
import org.geotools.utils.progress.ProcessingEvent;
import org.geotools.utils.progress.ProcessingEventListener;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * A {@link CoverageTransformer} based on GeoTools native abilities
 * 
 * @author Andrea Aime
 * 
 */
public class GeoToolsCoverageTransformer implements CoverageTransformer {

    static final Logger LOGGER = Logging.getLogger(GeoToolsCoverageTransformer.class);

    private Hints hints;

    public GeoToolsCoverageTransformer(String defaultSRS) {
        // create basic hints
        hints = new Hints(GeoTools.getDefaultHints());
        if (defaultSRS != null) {
            try {
                final CoordinateReferenceSystem crs = CRS.decode(defaultSRS);
                hints.add(new RenderingHints(Hints.DEFAULT_COORDINATE_REFERENCE_SYSTEM, crs));
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Cannot parse CRS " + defaultSRS, new Exception());
                hints = new Hints(GeoTools.getDefaultHints());
            }
        }
    }

    @Override
    public boolean accepts(File file) {
        return ImportUtilities.GEOTIFF_FORMAT.accepts(file, hints);
    }
    
    @Override
    public boolean isGeotiff(File file) throws IOException {
        // we assume accepts has been called already
        return true;
    }

    @Override
    public void rebuild(File inputFile, File outputFile, CompressionConfiguration compression,
            TilingConfiguration tiling, ProgressListener listener) throws IOException {
        GridCoverage2D gc = null;
        GeoTiffWriter writer = null;
        AbstractGridCoverage2DReader reader = null;
        try {
            reader = ImportUtilities.GEOTIFF_FORMAT.getReader(inputFile, hints);
            if (reader == null) {
                throw new IOException("This file is not a valid GeoTiff file: " + inputFile);
            }

            // Sets tiling parameters if needs be
            // FIXME: add tile retaining if retile is true
            final GeoTiffWriteParams wp = new GeoTiffWriteParams();
            if (tiling.isEnabled()) {
                wp.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
                wp.setTiling(tiling.getTileWidth(), tiling.getTileHeight());
            }

            if (compression.isEnabled()) {
                wp.setCompressionMode(GeoToolsWriteParams.MODE_EXPLICIT);

                wp.setCompressionType(compression.getType());
                wp.setCompressionQuality((float) compression.getRatio() / 100);
            }

            final ParameterValueGroup writeParameters = ImportUtilities.GEOTIFF_FORMAT
                    .getWriteParameters();
            writeParameters
                    .parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString())
                    .setValue(wp);

            writer = new GeoTiffWriter(outputFile);

            // SG I would force the use of JAI for large raster
            // plus the same tile size of the output
            // to improve cache usage. Notice that GeoTiff
            // currently always uses JAI ImageRead, but who
            // knows..
            Dimension tileSizes = getTileSize(tiling);
            final ParameterValue<String> tileSizeParam = AbstractGridFormat.SUGGESTED_TILE_SIZE
                    .createValue();
            tileSizeParam.setValue(tileSizes.width + "," + tileSizes.height);

            // SG commented for the moment has we do not have it
            // readParameters.parameter(AbstractGridFormat.USE_JAI_IMAGEREAD.getName().toString()).setValue(Boolean.TRUE);
            gc = (GridCoverage2D) reader.read(new GeneralParameterValue[] { tileSizeParam });
            if (listener != null) {
                writeParameters
                        .parameter(AbstractGridFormat.PROGRESS_LISTENER.getName().toString())
                        .setValue(listener);
            }

            // write down!
            GeneralParameterValue[] params = (GeneralParameterValue[]) writeParameters.values()
                    .toArray(new GeneralParameterValue[writeParameters.values().size()]);
            writer.write(gc, params);
        } finally {
            // Cleans up writer
            try {
                if (writer != null) {
                    writer.dispose();
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Cannot dispose writer of " + outputFile.getCanonicalPath(),
                        e);
            }

            // Cleans up reader
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Exception ee) {
                    LOGGER.log(Level.FINE,
                            "Cannot dispose reader of " + inputFile.getCanonicalPath(), ee);
                }
            }

            // Cleans up coverage
            try {
                if (gc != null) {
                    gc.dispose(true);
                    ImageUtilities.disposePlanarImageChain(PlanarImage.wrapRenderedImage(gc
                            .getRenderedImage()));
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Cannot dispose GridCoverage " + gc.getName(), e);
            }

        }

    }

    @Override
    public void addOverviews(File inputFile, CompressionConfiguration compression,
            TilingConfiguration tiling, OverviewConfiguration overviews,
            final ProgressListener listener) throws IOException {
        AbstractGridCoverage2DReader reader = null;
        OverviewsEmbedder oe = null;
        try {
            reader = ImportUtilities.GEOTIFF_FORMAT.getReader(inputFile, hints);
            if (reader == null) {
                throw new IOException("This file is not a valid GeoTiff file: " + inputFile);
            }

            int numOverviews = getNumOverviews(reader);
            if (numOverviews > 0 && overviews.isRetainOverviews()) {
                return;
            }

            // get size of raster
            int width = reader.getOriginalGridRange().getSpan(0);
            int height = reader.getOriginalGridRange().getSpan(1);

            Dimension tileSizes = getTileSize(tiling);
            int maxSteps = ImportUtilities.computeMaxOverviews(width, height, tileSizes.width,
                    tileSizes.height, overviews.getDownsampleStep());

            maxSteps = Math.min(overviews.getNumOverviews(), maxSteps);

            // if we can really add overviews, do it
            if (maxSteps > 0) {

                oe = new OverviewsEmbedder();

                // Now let's move on with the overviews
                oe.setDownsampleStep(overviews.getDownsampleStep());
                oe.setNumSteps(maxSteps);
                oe.setScaleAlgorithm(overviews.getSubsampleAlgorithm());
                oe.setTileCache(JAI.getDefaultInstance().getTileCache());

                oe.setTileWidth(tileSizes.width);
                oe.setTileHeight(tileSizes.height);

                // TODO: use parameter
                oe.setExternalOverviews(overviews.isExternalOverviews());
                oe.setSourcePath(inputFile.getAbsolutePath());

                if (compression.isEnabled()) {
                    oe.setCompressionRatio(compression.getRatio() / 100);
                    oe.setCompressionScheme(compression.getType());
                }

                if (listener != null) {
                    // bridge GeoToools progress listener with the overview embedder's processing event listener
                    final OverviewsEmbedder oef = oe;
                    ProcessingEventListener pel = new ProcessingEventListener() {

                        public void getNotification(ProcessingEvent event) {
                            listener.setTask(new SimpleInternationalString(event.getMessage()));
                            if (event.getPercentage() >= 0) {
                                listener.progress((float) event.getPercentage());
                            }

                            // stop requested?
                            if (listener.isCanceled()) {
                                oef.stopThread();
                            }
                        }

                        public void exceptionOccurred(ExceptionEvent event) {
                            if (LOGGER.isLoggable(Level.WARNING)) {
                                LOGGER.warning(event.toString());
                            }
                        }

                    };
                    oe.addProcessingEventListener(pel);
                }

                // Adds the overviews
                oe.run();
            }
        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Exception ee) {
                    LOGGER.log(Level.FINE,
                            "Cannot dispose reader of " + inputFile.getCanonicalPath(), ee);
                }
            }

            try {
                if(oe != null) {
                    oe.dispose();
                }
            } catch (Exception e) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "Cannot dispose OverviewEmbedder", e);
                }
            }
        }

    }

    private int getNumOverviews(AbstractGridCoverage2DReader reader) {
        try {
            // bulldoze our way onto the private field for the moment
            Field field = AbstractGridCoverage2DReader.class.getDeclaredField("numOverviews");
            field.setAccessible(true);
            return (Integer) field.get(reader);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Failed to determine the number of overviews in the current reader", e);

            // assume there are none...
            return 0;
        }
    }

    private Dimension getTileSize(TilingConfiguration tiling) {
        // setup tiling sizes, will be used both for inner
        // tiling and overviews
        if (tiling.isEnabled()) {
            return new Dimension(tiling.getTileWidth(), tiling.getTileHeight());
        } else {
            return JAI.getDefaultTileSize();
        }
    }

    @Override
    public boolean isInnerTiled(File file) throws IOException {
        AbstractGridCoverage2DReader reader = null;
        GridCoverage2D gc = null;
        try {
            reader = ImportUtilities.GEOTIFF_FORMAT.getReader(file, hints);
            if (reader == null) {
                throw new IOException("This file is not a valid GeoTiff file: " + file);
            }

            gc = reader.read(null);
            RenderedImage image = gc.getRenderedImage();

            // an image is inner tiled if both tile width and tile height are smaller than the image
            // size, otherwise it's just striped
            return image.getWidth() > image.getTileWidth()
                    && image.getHeight() > image.getTileHeight();
        } finally {
            if (gc != null) {
                gc.dispose(true);
                ImageUtilities.disposePlanarImageChain(PlanarImage.wrapRenderedImage(gc
                        .getRenderedImage()));
            }

            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Exception ee) {
                    LOGGER.log(Level.FINE, "Cannot dispose reader of " + file.getCanonicalPath(),
                            ee);
                }
            }
        }
    }

    @Override
    public boolean hasOverviews(File file) throws IOException {
        AbstractGridCoverage2DReader reader = null;
        OverviewsEmbedder oe = null;
        try {
            reader = ImportUtilities.GEOTIFF_FORMAT.getReader(file, hints);
            if (reader == null) {
                throw new IOException("This file is not a valid GeoTiff file: " + file);
            }

            int numOverviews = getNumOverviews(reader);
            return numOverviews > 0;
        } finally {
            if (reader != null) {
                try {
                    reader.dispose();
                } catch (Exception ee) {
                    LOGGER.log(Level.FINE, "Cannot dispose reader of " + file.getCanonicalPath(),
                            ee);
                }
            }
        }
    }

}
