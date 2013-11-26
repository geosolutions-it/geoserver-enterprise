package org.geoserver.wps.raster.algebra;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.factory.Hints;
import org.geotools.filter.visitor.DefaultFilterVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.Utilities;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.ExpressionVisitor;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

public abstract class AbstractCoverageCollector extends DefaultFilterVisitor implements
        FilterVisitor, ExpressionVisitor {

    /** The {@link CoverageInfo} objects that we need. */
    protected final Set<CoverageInfo> coverageNames = new HashSet<CoverageInfo>();

    /** The coverage to be used as reference for the computation of the final {@link GridGeometry2D}. */
    protected CoverageInfo referenceCoverage;

    /** GeoServer {@link Catalog} to be used to extract the {@link CoverageInfo} information. */
    protected final Catalog catalog;

    /** Reference {@link CoordinateReferenceSystem}. */
    protected CoordinateReferenceSystem referenceCRS;

    /** Final envelope that contains the intersection of the various envelopes. It shall not be empty */
    protected ReferencedEnvelope finalEnvelope;

    protected final Hints hints;

    /** Final {@link GridGeometry2D}. */
    protected GridGeometry2D finalGridGeometry;

    /** Map that maps names to {@link GridCoverage2D} instances. At the end of the visit it contains all the coverages used in the {@link Filter}. */
    protected Map<String, GridCoverage2D> coverages = new HashMap<String, GridCoverage2D>();

    protected ResolutionChoice resolutionChoice;

    /** The list of Pixel Size on the X axis. */
    protected List<Double> pixelSizesX = new ArrayList<Double>();

    /** The list Pixel Size on the Y axis. */
    protected List<Double> pixelSizesY = new ArrayList<Double>();

    protected Geometry roi;

    /**
     * @param catalog2
     * @param resolutionChoice2
     * @param roi
     * @param hints2
     */
    public AbstractCoverageCollector(Catalog catalog, ResolutionChoice resolutionChoice,
            Geometry roi, Hints hints) {
        Utilities.ensureNonNull("resolutionChoice", resolutionChoice);
        Utilities.ensureNonNull("catalog", catalog);
        Utilities.ensureNonNull("hints", hints);

        this.catalog = catalog;
        this.hints = hints.clone();
        this.roi = roi;
        this.resolutionChoice = resolutionChoice;
    }

    /**
     * Retrieves a {@link Map} that contains the source {@link GridCoverage2D} along with its name as the key in order to use it later on.
     * 
     * @return a {@link Map} that contains the source {@link GridCoverage2D} along with its name as the key in order to use it later on.
     * 
     * @throws IOException in case something bad happens when reading the {@link GridCoverage2D}.
     */
    public synchronized Map<String, GridCoverage2D> getCoverages() throws IOException {

        // compute final GridGeometry
        try {
            prepareFinalGridGeometry();
        } catch (Exception e) {
            throw new IOException(e);
        }

        // prepare coverages
        prepareCoveragesList();

        return new HashMap<String, GridCoverage2D>(coverages);
    }

    /**
     * Provides access to the {@link GridGeometry2D} created for further processing.
     * 
     * @return
     * @throws IOException
     */
    public synchronized GridGeometry2D getGridGeometry() throws IOException {
        try {
            prepareFinalGridGeometry();
        } catch (Exception e) {
            throw new IOException(e);
        }
        prepareCoveragesList();
        return finalGridGeometry;
    }

    /**
     * @param name
     */
    protected abstract void visitCoverage(String name);

    /**
     * @throws IOException
     * 
     */
    protected abstract void prepareCoveragesList() throws IOException;

    /**
     * Create, once, the final {@link GridGeometry2D} to be used for futher processing.
     * 
     * @throws Exception
     * 
     */
    protected abstract void prepareFinalGridGeometry() throws Exception;

    /**
     * {@link PropertyName} properties indicate coverage names as per the instance in which this process is running.
     * 
     */
    public abstract void collect(List<String> inputs); 
    
    
    @Override
    public Object visit(Function function, Object arg1) {
        Utilities.ensureNonNull("function", function);

        final List<Expression> params = function.getParameters();
        for (Expression exp : params) {
            exp.accept(this, null);
        }
        return null;
    }
    
    /**
     * Perform clean up on internal resources.
     * 
     * <p>
     * Using this {@link CoverageCollector} after this method has been invoked may result in unexpected behaviors.
     * 
     */
    public synchronized void dispose() {
        // === clean up
        coverageNames.clear();
        if (coverages != null) {
            // clean
            for (GridCoverage2D gc : coverages.values()) {
                CoverageCleanerCallback.disposeCoverage(gc);
            }

            // clean map
            coverages.clear();
        }
    }
}
