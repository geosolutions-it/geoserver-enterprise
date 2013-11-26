/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.raster.algebra;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.wps.raster.GridCoverage2DRIA;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.Crop;
import org.geotools.coverage.processing.operation.Resample;
import org.geotools.factory.Hints;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;

import com.sun.media.jai.opimage.RIFUtil;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Implementation of {@link ExpressionVisitor} and {@link FilterVisitor} that collects the coverages from the provided filter.
 * 
 * <p>
 * This class assumes the first one is the reference one, which means, its {@link CoordinateReferenceSystem} and GridToWorld {@link AffineTransform2D}
 * will be used to impose the final ones.
 * 
 * <p>
 * The final {@link Envelope} will be set to the intersection of the provided coverages.
 * 
 * <p>
 * The {@link PropertyName}s extracted from the provided {@link FilterVisitor} must indicate via a name existing coverage in the GeoServer in which
 * this process is running. The provided name may or not include the workspace in the usual workspace:identifier form. <strong>If the workspace is not
 * provided the default one will be used</strong>.
 * 
 * 
 * @author Simone Giannecchini, GeoSolutions SAS TODO make the logic to choose the final {@link AffineTransform2D} more generic and if possible
 *         customizable.
 */
public class ListCoverageCollector extends AbstractCoverageCollector {

    private static ParameterValueGroup resampleParams;

    private static ParameterValueGroup cropParams;

    static {

        // ///////////////////////////////////////////////////////////////////
        //
        // Caching parameters for performing the various operations.
        //
        // ///////////////////////////////////////////////////////////////////
        final CoverageProcessor processor = new CoverageProcessor(new Hints(
                Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE));
        resampleParams = processor.getOperation("Resample").getParameters();
        cropParams = processor.getOperation("CoverageCrop").getParameters();
    }

    private final static Logger LOGGER = Logging.getLogger(ListCoverageCollector.class);

    private GridGeometry gridGeo;

    private ReferencedEnvelope referenceEnvelope;

    private ParameterValue<String> suggestedTileSizeParam;

    private ParameterValue<Boolean> streamingReadParam;

    private Resample resampleOp;

    private Crop cropOp;

    /**
     * Constructor.
     * 
     * @param catalog the GeoServer {@link Catalog} to get {@link CoverageInfo} from.
     * @param resolutionChoice how to choose the final pixel size.
     * @param hints {@link Hints} to be used when instantiating {@link GridCoverage2D}.
     */
    public ListCoverageCollector(Catalog catalog, ResolutionChoice resolutionChoice, Hints hints) {
        this(catalog, resolutionChoice, null, hints);
    }

    /**
     * @param catalog2
     * @param resolutionChoice2
     * @param roi
     * @param hints2
     */
    public ListCoverageCollector(Catalog catalog, ReferencedEnvelope referenceEnvelope,
            GridGeometry gridGeo, Hints hints) {
        super(catalog, ResolutionChoice.PROVIDED, null, hints);

        this.referenceEnvelope = referenceEnvelope;
        this.gridGeo = gridGeo;

        suggestedTileSizeParam = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
        final ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        if (layout != null && layout.isValid(ImageLayout.TILE_HEIGHT_MASK)
                && layout.isValid(ImageLayout.TILE_WIDTH_MASK)) {
            suggestedTileSizeParam.setValue(String.valueOf(layout.getTileWidth(null)) + ","
                    + String.valueOf(layout.getTileHeight(null)));
        } else {
            // default
            suggestedTileSizeParam.setValue(String.valueOf(JAI.getDefaultTileSize().width) + ","
                    + String.valueOf(JAI.getDefaultTileSize().height));
        }

        streamingReadParam = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        streamingReadParam.setValue(true);

        resampleOp = new Resample();

        cropOp = new Crop();
    }

    /**
     * @param catalog2
     * @param resolutionChoice2
     * @param roi
     * @param hints2
     */
    public ListCoverageCollector(Catalog catalog, ResolutionChoice resolutionChoice, Geometry roi,
            Hints hints) {
        super(catalog, resolutionChoice, roi, hints);
    }

    /**
     * {@link PropertyName} properties indicate coverage names as per the instance in which this process is running.
     * 
     */
    public void collect(List<String> inputs) {
        org.geotools.util.Utilities.ensureNonNull("inputs", inputs);

        // === get and check name
        for (String entry : inputs) {
            visitCoverage(entry);
        }
    }

    /**
     * @param name
     */
    protected void visitCoverage(String name) {
        // === extract from catalog and check the coverage
        final CoverageInfo coverage = catalog.getCoverageByName(name);
        if (coverage == null) {
            throw new IllegalArgumentException("Unable to locate coverage:" + name);
        } else {

            // pixel scale
            MathTransform tempTransform = coverage.getGrid().getGridToCRS();
            if (!(tempTransform instanceof AffineTransform)) {
                throw new IllegalArgumentException(
                        "Grid to world tranform is not an AffineTransform:" + name);

            }
            if (resolutionChoice != ResolutionChoice.PROVIDED) {

                AffineTransform tr = (AffineTransform) tempTransform;

                if (referenceCoverage == null) {
                    // set the first use as reference coverage
                    referenceCoverage = coverage;
                    referenceCRS = referenceCoverage.getCRS();

                    try {
                        finalEnvelope = referenceCoverage.getNativeBoundingBox();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, e.getMessage(), e);
                    }

                    // resolution
                    pixelSizesX.add(XAffineTransform.getScaleX0(tr));
                    pixelSizesY.add(XAffineTransform.getScaleY0(tr));

                } else {

                    // === we already have a reference coverage
                    boolean reproject = false;

                    // get envelope and crs
                    final CoordinateReferenceSystem crs = coverage.getCRS();
                    ReferencedEnvelope envelope = null;
                    try {
                        envelope = coverage.getNativeBoundingBox();

                        // reproject the coverage envelope if needed
                        if (!CRS.equalsIgnoreMetadata(crs, referenceCRS)) {
                            envelope = envelope.transform(referenceCRS, true);
                            reproject = true;
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    // intersect the reference envelope with the coverage one
                    finalEnvelope = new ReferencedEnvelope(finalEnvelope.intersection(envelope),
                            this.referenceCRS);

                    // resolution
                    if (!reproject) {
                        pixelSizesX.add(XAffineTransform.getScaleX0(tr));
                        pixelSizesY.add(XAffineTransform.getScaleY0(tr));
                    } else {
                        // simulate reprojection
                        tr = new GridToEnvelopeMapper(coverage.getGrid().getGridRange(), envelope)
                                .createAffineTransform();
                        pixelSizesX.add(XAffineTransform.getScaleX0(tr));
                        pixelSizesY.add(XAffineTransform.getScaleY0(tr));
                    }

                    // add to the set as this is not a reference coverage
                    coverageNames.add(coverage);
                }
            } else {
                if (referenceCRS == null) {
                    // Selection of the reference CRS
                    referenceCRS = referenceEnvelope.getCoordinateReferenceSystem();
                    // definition of the final envelope
                    finalEnvelope = new ReferencedEnvelope(referenceEnvelope, this.referenceCRS);
                    // Coverages map
                    coverages = new HashMap<String, GridCoverage2D>();
                    // Final GridGeometry object created only for avoiding the calculation on the "prepareFinalGridGeometry()" method
                    finalGridGeometry = new GridGeometry2D(gridGeo);
                }

                // get envelope and crs
                final CoordinateReferenceSystem crs = coverage.getCRS();
                ReferencedEnvelope envelope = null;
                boolean reproject = false;
                try {
                    // reproject the reference envelope if needed
                    if (!CRS.equalsIgnoreMetadata(crs, referenceCRS)) {

                        envelope = new ReferencedEnvelope(finalEnvelope.transform(crs, true));
                        reproject = true;
                    } else {
                        // Else the reference envelope is taken
                        envelope = new ReferencedEnvelope(finalEnvelope, referenceCRS);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                // Definition of the gridGeometry associated to the envelope
                GridGeometry gridGeom = new GridGeometry2D(gridGeo.getGridRange(), envelope);

                // Definition of the parameters associated to the gridGeometry
                final ParameterValue<GridGeometry2D> readGG = AbstractGridFormat.READ_GRIDGEOMETRY2D
                        .createValue();
                readGG.setValue(gridGeom);
                // Reading of the coverage in the source CRS in the defined envelope
                GridCoverage2D coverageInSrcCRS = null;
                try {
                    coverageInSrcCRS = (GridCoverage2D) coverage.getGridCoverageReader(
                            new NullProgressListener(), hints).read(
                            new GeneralParameterValue[] { streamingReadParam, readGG,
                                    suggestedTileSizeParam });
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                // If the reprojection must be done, the image is reprojected to the final CRS and cropped to the reference envelope
                GridCoverage2D coverageInDestCRS;
                if (reproject) {
                    final ParameterValueGroup param = (ParameterValueGroup) resampleParams.clone();
                    param.parameter("source").setValue(coverageInSrcCRS);
                    param.parameter("CoordinateReferenceSystem").setValue(referenceCRS);

                    coverageInDestCRS = (GridCoverage2D) resampleOp.doOperation(param, hints);
 
                } else {
                    coverageInDestCRS = coverageInSrcCRS;
                }
                
                final ParameterValueGroup paramCrop = (ParameterValueGroup) cropParams.clone();
                paramCrop.parameter("source").setValue(coverageInDestCRS);
                paramCrop.parameter("envelope").setValue(referenceEnvelope);

                coverageInDestCRS = (GridCoverage2D) cropOp.doOperation(paramCrop, hints);
                
                // Selection of the no data
                double noDataValue = 0;

                Object noData = null;
                // Selection of the properties associated to the coverage reprojected
                Map coverageProperties = coverageInDestCRS.getProperties();
                Object noDataFinal = coverageProperties.get("GC_NODATA");
                // Check if the NODATA is defined in the last coverage created
                if (noDataFinal != null) {

                    if (noData instanceof Number) {
                        noDataValue = ((Number) noData).doubleValue();
                    } else {
                        // If the value is not a Number then the No Data is taken from the source
                        // image properties and then set as a final image properties
                        try {
                            noData = ((GridCoverage2D) coverage.getGridCoverage(null, hints))
                                    .getProperty("GC_NODATA");
                        } catch (IOException e) {
                            LOGGER.log(Level.FINER, e.getMessage(), e);
                        }

                        if (noData instanceof Number) {
                            noDataValue = ((Number) noData).doubleValue();
                            coverageProperties.put("GC_NODATA", noDataValue);
                        }
                    }
                } else {
                    // If the value is not present then the No Data is taken from the source
                    // image properties and then set as a final image properties
                    try {
                        noData = ((GridCoverage2D) coverage.getGridCoverage(null, hints))
                                .getProperty("GC_NODATA");
                    } catch (IOException e) {
                        LOGGER.log(Level.FINER, e.getMessage(), e);
                    }

                    if (noData instanceof Number) {
                        noDataValue = ((Number) noData).doubleValue();
                        coverageProperties.put("GC_NODATA", noDataValue);
                    }
                }
                
                Envelope finalCoverageEnvelope = coverageInDestCRS.getEnvelope();
                
                ReferencedEnvelope finalRefEnv = new ReferencedEnvelope(finalCoverageEnvelope);
                
                com.vividsolutions.jts.geom.Envelope refEnv = finalEnvelope;
                
                // Creation of the coverage associated
                GridCoverage2D finalCoverage;
                
                if(!finalRefEnv.contains(refEnv)){ 
                    // Expansion to the final GridGeometry already defined
                    GridCoverage2DRIA expandedIMG = GridCoverage2DRIA.create(coverageInDestCRS,
                            (GridGeometry2D) gridGeo, noDataValue);
                    
                    finalCoverage = CoverageFactoryFinder.getGridCoverageFactory(hints)
                            .create(coverageInDestCRS.getName(), expandedIMG, (GridGeometry2D) gridGeo,
                                    coverageInDestCRS.getSampleDimensions(), null, coverageProperties);
                    
                }else{
                    finalCoverage = CoverageFactoryFinder.getGridCoverageFactory(hints)
                            .create(coverageInDestCRS.getName(), coverageInDestCRS.getRenderedImage(), (GridGeometry2D) gridGeo,
                                    coverageInDestCRS.getSampleDimensions(), null, coverageProperties);
                }


                // add to the set
                coverages.put(coverage.prefixedName(), finalCoverage);
            }
        }
    }

    /**
     * @throws IOException
     * 
     */
    protected void prepareCoveragesList() throws IOException {
        // === checks, we don't want to build this twice
        if (coverages != null) {
            return;
        }

        // === make sure we read in streaming and we read just what we need
        final ParameterValue<Boolean> streamingRead = AbstractGridFormat.USE_JAI_IMAGEREAD
                .createValue();
        streamingRead.setValue(true);

        final ParameterValue<GridGeometry2D> readGG = AbstractGridFormat.READ_GRIDGEOMETRY2D
                .createValue();
        readGG.setValue(finalGridGeometry);

        final ParameterValue<String> suggestedTileSize = AbstractGridFormat.SUGGESTED_TILE_SIZE
                .createValue();
        final ImageLayout layout = RIFUtil.getImageLayoutHint(hints);
        if (layout != null && layout.isValid(ImageLayout.TILE_HEIGHT_MASK)
                && layout.isValid(ImageLayout.TILE_WIDTH_MASK)) {
            suggestedTileSize.setValue(String.valueOf(layout.getTileWidth(null)) + ","
                    + String.valueOf(layout.getTileHeight(null)));
        } else {
            // default
            suggestedTileSize.setValue(String.valueOf(JAI.getDefaultTileSize().width) + ","
                    + String.valueOf(JAI.getDefaultTileSize().height));
        }

        // now prepare the target coverages to match the target GridGeometry

        // === we have other grid coverage beside the reference one, let's process them
        // add the reference one
        coverages = new HashMap<String, GridCoverage2D>();

        coverages.put(
                referenceCoverage.prefixedName(),
                (GridCoverage2D) referenceCoverage.getGridCoverageReader(
                        new NullProgressListener(), hints).read(
                        new GeneralParameterValue[] { streamingRead, readGG, suggestedTileSize }));
        // add the others with proper reprojection if needed
        for (CoverageInfo cov : coverageNames) {
            coverages.put(
                    cov.prefixedName(),
                    (GridCoverage2D) cov.getGridCoverageReader(new NullProgressListener(), hints)
                            .read(new GeneralParameterValue[] { streamingRead, readGG,
                                    suggestedTileSize }));
        }
    }

    /**
     * Create, once, the final {@link GridGeometry2D} to be used for futher processing.
     * 
     * @throws Exception
     * 
     */
    protected void prepareFinalGridGeometry() throws Exception {
        if (finalGridGeometry == null) {
            // prepare the envelope and make sure the CRS is set

            // use ROI if present
            if (roi != null) {
                final com.vividsolutions.jts.geom.Envelope envelope = roi.getEnvelopeInternal();
                final CoordinateReferenceSystem roiCRS;
                // assuming WGS84 if no SRID is there
                final int srid = roi.getSRID();
                if (srid > 0) {
                    roiCRS = CRS.decode("EPSG:" + srid);
                } else {
                    roiCRS = CRS.decode("EPSG:4326");
                }

                ReferencedEnvelope refEnvelope = new ReferencedEnvelope(envelope, roiCRS);
                refEnvelope = refEnvelope.transform(referenceCRS, true);
                if (finalEnvelope.contains(JTS.transform(envelope,
                        CRS.findMathTransform(roiCRS, referenceCRS))))
                    finalEnvelope = new ReferencedEnvelope(refEnvelope.intersection(finalEnvelope),
                            referenceCRS);
                else
                    finalEnvelope = refEnvelope;

            }
            final GeneralEnvelope envelope = new GeneralEnvelope(finalEnvelope);
            envelope.setCoordinateReferenceSystem(referenceCRS);

            // check envelope and ROI to make sure it is not empty
            if (envelope.isEmpty()) {
                throw new IllegalStateException("Final envelope is empty!");
            }

            // G2W transform
            double finalScaleX = resolutionChoice.compute(pixelSizesX);
            double finalScaleY = resolutionChoice.compute(pixelSizesY);

            final MathTransform g2w = new AffineTransform2D(finalScaleX, 0, 0, -finalScaleY,// TODO make this generic with respect to CRS
                    envelope.getLowerCorner().getOrdinate(0), envelope.getUpperCorner()
                            .getOrdinate(1));

            // prepare final gridgeometry
            finalGridGeometry = new GridGeometry2D(PixelInCell.CELL_CORNER, g2w, envelope, hints);
        }
    }
}
