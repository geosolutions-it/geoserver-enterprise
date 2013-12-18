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

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.factory.Hints;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.builder.GridToEnvelopeMapper;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.Envelope;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
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

    private final static Logger LOGGER = Logging.getLogger(ListCoverageCollector.class);

    private ParameterValue<String> suggestedTileSizeParam;

    private ParameterValue<Boolean> streamingReadParam;

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
    public ListCoverageCollector(Catalog catalog, GridGeometry2D gridGeo, Hints hints) {
        super(catalog, ResolutionChoice.PROVIDED, null, hints);

        Envelope outputBbox = gridGeo.getEnvelope();

        // Selection of the reference CRS
        referenceCRS = outputBbox.getCoordinateReferenceSystem();

        // definition of the final envelope
        finalEnvelope = new ReferencedEnvelope(outputBbox);

        // Final GridGeometry object created only for avoiding the calculation on the "prepareFinalGridGeometry()" method
        finalGridGeometry = new GridGeometry2D(gridGeo);

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
        if (coverage != null) {
            if (resolutionChoice != ResolutionChoice.PROVIDED) {

                resolutionMissing(coverage);
            } else {
                resolutionProvided(coverage);
            }
        }
    }

    /**
     * @param coverageInfo
     * @throws Exception
     */
    private void resolutionProvided(final CoverageInfo coverageInfo) {

        try {
            // SG missing background values for imagemosaic
            final GridCoverageReader gridCoverageReader = coverageInfo.getGridCoverageReader(
                    new NullProgressListener(), hints);
            // is it null?
            if (gridCoverageReader == null) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "Unable to find a read for this coverage info: "
                            + coverageInfo.toString());
                }
                return;
            }

            // check envelope intersection
            // get envelope and crs
            final CoordinateReferenceSystem crs = coverageInfo.getCRS();
            ReferencedEnvelope envelope = null;
            try {
                envelope = coverageInfo.getNativeBoundingBox();

                // reproject the coverage envelope if needed
                if (!CRS.equalsIgnoreMetadata(crs, referenceCRS)) {
                    envelope = envelope.transform(referenceCRS, true);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            // intersect the reference envelope with the coverage one
            if (!envelope.intersects((BoundingBox) finalEnvelope)) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "This coverage does not intersect provided area ");
                }
                return;// SKIP This one

            }
        } catch (Exception e) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.log(Level.INFO, e.getLocalizedMessage(), e);
            }
            return;
        }

        // add to the set as this is not a reference coverage
        coverageNames.add(coverageInfo);
    }

    /**
     * @param coverageInfo
     * @param tempTransform
     * @throws RuntimeException
     * @throws MismatchedDimensionException
     * @throws IllegalStateException
     */
    private void resolutionMissing(final CoverageInfo coverageInfo) throws RuntimeException {

        // pixel scale
        MathTransform tempTransform = coverageInfo.getGrid().getGridToCRS();
        if (!(tempTransform instanceof AffineTransform)) {
            throw new IllegalArgumentException(
                    "Grid to world tranform is not an AffineTransform for coverage: "
                            + coverageInfo.getName());

        }
        AffineTransform tr = (AffineTransform) tempTransform;

        if (referenceCoverage == null) {
            // set the first use as reference coverage
            referenceCoverage = coverageInfo;
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
            final CoordinateReferenceSystem crs = coverageInfo.getCRS();
            ReferencedEnvelope envelope = null;
            try {
                envelope = coverageInfo.getNativeBoundingBox();

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
                tr = new GridToEnvelopeMapper(coverageInfo.getGrid().getGridRange(), envelope)
                        .createAffineTransform();
                pixelSizesX.add(XAffineTransform.getScaleX0(tr));
                pixelSizesY.add(XAffineTransform.getScaleY0(tr));
            }

            // add to the set as this is not a reference coverage
            coverageNames.add(coverageInfo);
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
        
        //Settings of the read parameters
        GeneralParameterValue[] parameters = null;
        
        // Check if ImageMosaicReader is used
        for (CoverageInfo coverageInfo : coverageNames) {
            final GridCoverageReader gridCoverageReader = coverageInfo.getGridCoverageReader(
                    new NullProgressListener(), hints); // is it null?
            if (gridCoverageReader == null) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "Unable to find a read for this coverage info: "
                            + coverageInfo.toString());
                }
                return;
            }else{
                
                Map<String, Serializable> params = coverageInfo.getParameters();
                int numParameters = 3;
                
                Set<String> keys = params.keySet();
                
                ParameterValue<Boolean> allowMultiThreading = null;
                ParameterValue<Color> inputTransparentColor = null;
                ParameterValue<Integer> maxAllowedTiles = null;
                               
                for(String key : keys){                                      
                    if(key.equalsIgnoreCase(ImageMosaicFormat.ALLOW_MULTITHREADING.getName().toString())){
                        allowMultiThreading = ImageMosaicFormat.ALLOW_MULTITHREADING.createValue();
                        allowMultiThreading.setValue(Boolean.parseBoolean(params.get(key).toString()));                        
                        numParameters++;
                    }else if(key.equalsIgnoreCase(ImageMosaicFormat.INPUT_TRANSPARENT_COLOR.getName().toString())){                        
                        String parsedColor = params.get(key).toString();
                        if(parsedColor.isEmpty()){
                             continue;
                        }
                        inputTransparentColor = ImageMosaicFormat.INPUT_TRANSPARENT_COLOR.createValue();
                        int colorInteger =  Long.decode(parsedColor).intValue();                     
                        Color color = new Color(colorInteger);
                        inputTransparentColor.setValue(color); 
                        numParameters++;
                    }else if(key.equalsIgnoreCase(ImageMosaicFormat.MAX_ALLOWED_TILES.getName().toString())){
                        maxAllowedTiles = ImageMosaicFormat.MAX_ALLOWED_TILES.createValue();
                        maxAllowedTiles.setValue(Integer.parseInt(params.get(key).toString())); 
                        numParameters++;
                    }else{
                        continue;
                    }                                       
                }
                parameters = new GeneralParameterValue[numParameters];
                
                parameters[0] = streamingRead;
                parameters[1] = readGG;
                parameters[2] = suggestedTileSize;
                
                int finalNumPar = numParameters +1;
                
                for(int i = 3; i < finalNumPar; i++){
                    if(allowMultiThreading !=null){
                        parameters[i] = allowMultiThreading;
                        allowMultiThreading = null;
                    }else if(inputTransparentColor !=null){
                        parameters[i] = inputTransparentColor;
                        inputTransparentColor = null;
                    }else if(maxAllowedTiles !=null){
                        parameters[i] = maxAllowedTiles;
                        maxAllowedTiles = null;
                    }
                }
            }
        }

        // now prepare the target coverages to match the target GridGeometry

        // === we have other grid coverage beside the reference one, let's process them
        // add the reference one
        coverages = new HashMap<String, GridCoverage2D>();
        if (resolutionChoice != ResolutionChoice.PROVIDED) {
            coverages.put(referenceCoverage.prefixedName(), (GridCoverage2D) referenceCoverage
                    .getGridCoverageReader(new NullProgressListener(), hints).read(parameters));
        }
        // add the others with proper reprojection if needed
        for (CoverageInfo coverageInfo : coverageNames) {
            final String prefixedName = coverageInfo.prefixedName();
            final GridCoverageReader gridCoverageReader = coverageInfo.getGridCoverageReader(
                    new NullProgressListener(), hints); // is it null?
            if (gridCoverageReader == null) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "Unable to find a read for this coverage info: "
                            + coverageInfo.toString());
                }
                return;
            }
            final GridCoverage2D coverage = (GridCoverage2D) gridCoverageReader.read(parameters);
            // is it null?
            if (coverage == null) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.log(Level.INFO, "Unable to read any coverage for the provided GG2D");
                }
                return;
            }
            coverages.put(prefixedName, coverage);
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
