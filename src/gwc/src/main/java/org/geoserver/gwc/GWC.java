/* Copyright (c) 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.getRootCause;
import static com.google.common.base.Throwables.propagate;
import static org.geowebcache.grid.GridUtil.findBestMatchingGrid;
import static org.geowebcache.seed.GWCTask.TYPE.TRUNCATE;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.config.GWCConfigPersister;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.CatalogLayerEventListener;
import org.geoserver.gwc.layer.CatalogStyleChangeListener;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Response;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Operation;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.map.RenderedImageMap;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.config.Configuration;
import org.geowebcache.config.XMLConfiguration;
import org.geowebcache.config.XMLGridSet;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.diskquota.DiskQuotaConfig;
import org.geowebcache.diskquota.DiskQuotaMonitor;
import org.geowebcache.diskquota.QuotaStore;
import org.geowebcache.diskquota.storage.LayerQuota;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.diskquota.storage.TileSet;
import org.geowebcache.diskquota.storage.TileSetVisitor;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.GridSubsetFactory;
import org.geowebcache.grid.SRS;
import org.geowebcache.io.ByteArrayResource;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerDispatcher;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.geowebcache.seed.GWCTask;
import org.geowebcache.seed.GWCTask.TYPE;
import org.geowebcache.seed.SeedRequest;
import org.geowebcache.seed.TileBreeder;
import org.geowebcache.service.Service;
import org.geowebcache.storage.StorageBroker;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.TileRange;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Spring bean acting as a mediator between GWC and GeoServer for the GWC integration classes so
 * that they don't need to worry about complexities nor API changes in either.
 * 
 * @author Gabriel Roldan
 * @version $Id$
 * 
 */
/**
 * @author groldan
 * 
 */
public class GWC implements DisposableBean, InitializingBean {

    /**
     * @see #get()
     */
    private static GWC INSTANCE;

    static final Logger log = Logging.getLogger(GWC.class);

    /**
     * @see #getResponseEncoder(MimeType, RenderedImageMap)
     */
    private static Map<String, Response> cachedTileEncoders = new HashMap<String, Response>();

    private final TileLayerDispatcher tld;

    private final StorageBroker storageBroker;

    private final TileBreeder tileBreeder;

    private final QuotaStore quotaStore;

    private final GWCConfigPersister gwcConfigPersister;

    private final Dispatcher owsDispatcher;

    private final GridSetBroker gridSetBroker;

    private DiskQuotaMonitor monitor;

    private CatalogLayerEventListener catalogLayerEventListener;

    private CatalogStyleChangeListener catalogStyleChangeListener;

    private final Catalog rawCatalog;

    public GWC(final GWCConfigPersister gwcConfigPersister, final StorageBroker sb,
            final TileLayerDispatcher tld, final GridSetBroker gridSetBroker,
            final TileBreeder tileBreeder, final QuotaStore quotaStore,
            final DiskQuotaMonitor monitor, final Dispatcher owsDispatcher, final Catalog rawCatalog) {

        this.gwcConfigPersister = gwcConfigPersister;
        this.tld = tld;
        this.storageBroker = sb;
        this.gridSetBroker = gridSetBroker;
        this.tileBreeder = tileBreeder;
        this.monitor = monitor;
        this.owsDispatcher = owsDispatcher;
        this.quotaStore = quotaStore;
        this.rawCatalog = rawCatalog;

        catalogLayerEventListener = new CatalogLayerEventListener(this);
        catalogStyleChangeListener = new CatalogStyleChangeListener(this);
        this.rawCatalog.addListener(catalogLayerEventListener);
        this.rawCatalog.addListener(catalogStyleChangeListener);
    }

    public synchronized static GWC get() {
        if (GWC.INSTANCE == null) {
            GWC.INSTANCE = GeoServerExtensions.bean(GWC.class);
            if (GWC.INSTANCE == null) {
                throw new IllegalStateException("No bean of type " + GWC.class.getName()
                        + " found by GeoServerExtensions");
            }
        }
        return GWC.INSTANCE;
    }

    /**
     * Only to aid in unit testing for the places where a mock GWC mediator is needed and
     * {@link GWC#get()} is used; set it to {@code null} at each {@code tearDown} methed for each
     * test that sets it through {@link GWC#set(GWC)} at its {@code setUp} method
     */
    public static void set(GWC instance) {
        GWC.INSTANCE = instance;
    }

    /**
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     * @see #initialize()
     */
    public void afterPropertiesSet() throws Exception {
        GWC.set(this);
    }

    /**
     * 
     * @see org.springframework.beans.factory.DisposableBean#destroy()
     */
    public void destroy() throws Exception {
        Catalog catalog = getCatalog();
        if (this.catalogLayerEventListener != null) {
            catalog.removeListener(this.catalogLayerEventListener);
        }
        if (this.catalogStyleChangeListener != null) {
            catalog.removeListener(this.catalogStyleChangeListener);
        }
        GWC.set(null);
    }

    public Catalog getCatalog() {
        return rawCatalog;
    }

    public GWCConfig getConfig() {
        return gwcConfigPersister.getConfig();
    }

    /**
     * Fully truncates the given layer, including any ParameterFilter
     * 
     * @param layerName
     */
    public void truncate(final String layerName) {
        checkNotNull(layerName, "layerName is null");
        // easy, no need to issue truncate tasks
        TileLayer layer;
        try {
            layer = tld.getTileLayer(layerName);
        } catch (GeoWebCacheException e) {
            log.log(Level.INFO, e.getMessage(), e);
            return;
        }
        final Set<String> gridSubsets = layer.getGridSubsets();
        for (String gridSetId : gridSubsets) {
            deleteCacheByGridSetId(layerName, gridSetId);
        }
    }

    /**
     * Truncates the cache for the given layer/style combination
     * 
     * @param layerName
     * @param styleName
     */
    public void truncateByLayerAndStyle(final String layerName, final String styleName) {

        // check if the given style is actually cached
        if (log.isLoggable(Level.FINE)) {
            log.fine("Truncate for layer/style called. Checking if style '" + styleName
                    + "' is cached for layer '" + layerName + "'");
        }
        if (!isStyleCached(layerName, styleName)) {
            log.fine("Style '" + styleName + "' is not cached for layer " + layerName
                    + "'. No need to truncate.");
            return;
        }
        log.fine("truncating '" + layerName + "' for style '" + styleName + "'");
        String gridSetId = null; // all of them
        BoundingBox bounds = null;// all of them
        String format = null;// all of them
        truncate(layerName, styleName, gridSetId, bounds, format);
    }

    public void truncate(final String layerName, final ReferencedEnvelope bounds)
            throws GeoWebCacheException {

        final TileLayer tileLayer = tld.getTileLayer(layerName);
        final Collection<String> gridSubSets = tileLayer.getGridSubsets();

        /*
         * Create a truncate task for each gridSubset (CRS), format and style
         */
        for (String gridSetId : gridSubSets) {
            GridSubset layerGrid = tileLayer.getGridSubset(gridSetId);
            BoundingBox intersectingBounds = getIntersectingBounds(layerName, layerGrid, bounds);
            if (intersectingBounds == null) {
                continue;
            }
            String styleName = null;// all of them
            String format = null;// all of them
            truncate(layerName, styleName, gridSetId, intersectingBounds, format);
        }
    }

    private BoundingBox getIntersectingBounds(String layerName, GridSubset layerGrid,
            ReferencedEnvelope bounds) {
        final GridSet gridSet = layerGrid.getGridSet();
        final String gridSetId = gridSet.getName();
        final SRS srs = gridSet.getSrs();
        final CoordinateReferenceSystem gridSetCrs;
        try {
            gridSetCrs = CRS.decode("EPSG:" + srs.getNumber(), true);
        } catch (Exception e) {
            throw new RuntimeException("Can't decode SRS for layer '" + layerName + "': ESPG:"
                    + srs.getNumber());
        }

        ReferencedEnvelope truncateBoundsInGridsetCrs;

        try {
            truncateBoundsInGridsetCrs = bounds.transform(gridSetCrs, true);
        } catch (Exception e) {
            log.warning("Can't truncate layer " + layerName
                    + ": error transforming requested bounds to layer gridset " + gridSetId + ": "
                    + e.getMessage());
            return null;
        }

        final double minx = truncateBoundsInGridsetCrs.getMinX();
        final double miny = truncateBoundsInGridsetCrs.getMinY();
        final double maxx = truncateBoundsInGridsetCrs.getMaxX();
        final double maxy = truncateBoundsInGridsetCrs.getMaxY();
        final BoundingBox reqBounds = new BoundingBox(minx, miny, maxx, maxy);
        /*
         * layerGrid.getCoverageIntersections is not too robust, so we better check the requested
         * bounds intersect the layer bounds
         */
        // final BoundingBox layerBounds = layerGrid.getCoverageBestFitBounds();
        final BoundingBox layerBounds = layerGrid.getOriginalExtent();
        if (!layerBounds.intersects(reqBounds)) {
            log.fine("Requested truncation bounds do not intersect cached layer bounds, ignoring truncate request");
            return null;
        }
        final BoundingBox intersectingBounds = BoundingBox.intersection(layerBounds, reqBounds);
        return intersectingBounds;
    }

    /**
     * @param layerName
     *            name of the layer to truncate, non {@code null}
     * @param styleName
     *            style to truncate, or {@code null} for all
     * @param gridSetName
     *            grid set to truncate, {@code null} for all
     * @param bounds
     *            bounds to truncate based on, or {@code null} for whole layer
     * @param format
     *            {@link MimeType#getFormat() format} to truncate, or {@code null} for all
     */
    public void truncate(final String layerName, final String styleName, final String gridSetName,
            final BoundingBox bounds, final String format) {

        checkNotNull(layerName, "layerName can't be null");

        final TileLayer layer = getTileLayerByName(layerName);
        final Set<String> styleNames;
        final Set<String> gridSetIds;
        final List<MimeType> mimeTypes;
        if (styleName == null) {
            styleNames = getCachedStyles(layerName);
            if (styleNames.size() == 0) {
                styleNames.add("");
            }
        } else {
            styleNames = Collections.singleton(styleName);
        }
        if (gridSetName == null) {
            gridSetIds = layer.getGridSubsets();
        } else {
            gridSetIds = Collections.singleton(gridSetName);
        }
        if (format == null) {
            mimeTypes = layer.getMimeTypes();
        } else {
            try {
                mimeTypes = Collections.singletonList(MimeType.createFromFormat(format));
            } catch (MimeException e) {
                throw new RuntimeException();
            }
        }

        final String defaultStyle = layer.getStyles();

        for (String gridSetId : gridSetIds) {
            GridSubset gridSubset = layer.getGridSubset(gridSetId);
            if (gridSubset == null) {
                // layer may no longer have this gridsubset, but we want to truncate any remaining
                // tiles
                GridSet gridSet = gridSetBroker.get(gridSetId);
                gridSubset = GridSubsetFactory.createGridSubSet(gridSet);
            }
            for (String style : styleNames) {
                Map<String, String> parameters;
                if (style.length() == 0 || style.equals(defaultStyle)) {
                    log.finer("'" + style + "' is the layer's default style, "
                            + "not adding a parameter filter");
                    parameters = null;
                } else {
                    parameters = Collections.singletonMap("STYLES", style);
                }
                for (MimeType mime : mimeTypes) {
                    String formatName = mime.getFormat();
                    truncate(layer, bounds, gridSubset, formatName, parameters);
                }
            }
        }
    }

    private void truncate(final TileLayer layer, final BoundingBox bounds,
            final GridSubset gridSubset, String formatName, Map<String, String> parameters) {
        final int threadCount = 1;
        int zoomStart;
        int zoomStop;
        zoomStart = gridSubset.getZoomStart();
        zoomStop = gridSubset.getZoomStop();
        final TYPE taskType = TRUNCATE;
        SeedRequest req = new SeedRequest(layer.getName(), bounds, gridSubset.getName(),
                threadCount, zoomStart, zoomStop, formatName, taskType, parameters);

        GWCTask[] tasks;
        try {
            TileRange tr = TileBreeder.createTileRange(req, layer);
            boolean filterUpdate = false;
            tasks = tileBreeder.createTasks(tr, taskType, threadCount, filterUpdate);
        } catch (GeoWebCacheException e) {
            throw new RuntimeException(e);
        }

        tileBreeder.dispatchTasks(tasks);
    }

    private boolean isStyleCached(final String layerName, final String styleName) {
        Set<String> cachedStyles = getCachedStyles(layerName);
        boolean styleIsCached = cachedStyles.contains(styleName);
        return styleIsCached;
    }

    /**
     * Returns the names of the styles for the layer, including the default style
     * 
     * @param layerName
     * @return
     */
    private Set<String> getCachedStyles(final String layerName) {
        final TileLayer l = getTileLayerByName(layerName);
        Set<String> cachedStyles = new HashSet<String>();
        String defaultStyle = l.getStyles();
        if (defaultStyle != null) {
            cachedStyles.add(defaultStyle);
        }
        List<ParameterFilter> parameterFilters = l.getParameterFilters();
        if (parameterFilters != null) {
            for (ParameterFilter pf : parameterFilters) {
                if (!"STYLES".equalsIgnoreCase(pf.getKey())) {
                    continue;
                }
                cachedStyles.add(pf.getDefaultValue());
                cachedStyles.addAll(pf.getLegalValues());
                break;
            }
        }
        return cachedStyles;
    }

    /**
     * Completely eliminates a {@link GeoServerTileLayer} from GWC.
     * <p>
     * This method is intended to be called whenever a {@link LayerInfo} or {@link LayerGroupInfo}
     * is removed from GeoServer, or it is configured not to create a cached layer for it, in order
     * to delete the cache for the layer.
     * </p>
     * 
     * @param prefixedName
     *            the name of the layer to remove.
     * @return {@code true} if the removal of the entire cache for the layer has succeeded,
     *         {@code false} if there wasn't a cache for that layer.
     */
    public synchronized boolean layerRemoved(final String prefixedName) {
        try {
            return storageBroker.delete(prefixedName);
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reloads the configuration and notifies GWC of any externally removed layer.
     */
    public void reload() {
        final Set<String> currLayerNames = new HashSet<String>(getTileLayerNames());
        try {
            tld.reInit();
        } catch (RuntimeException e) {
            log.log(Level.WARNING, "Unable to reinit TileLayerDispatcher", e);
            throw e;
        }
        Set<String> newLayerNames = getTileLayerNames();
        SetView<String> removedExternally = Sets.difference(currLayerNames, newLayerNames);
        for (String removedLayerName : removedExternally) {
            log.info("Notifying of TileLayer '" + removedLayerName + "' removed externally");
            layerRemoved(removedLayerName);
        }
    }

    /**
     * Tries to dispatch a tile request represented by a GeoServer WMS {@link GetMapRequest} through
     * GeoWebCache, and returns the {@link ConveyorTile} if succeeded or {@code null} if it wasn't
     * possible.
     * <p>
     * Preconditions:
     * <ul>
     * <li><code>{@link GetMapRequest#isTiled() request.isTiled()} == true</code>
     * </ul>
     * </p>
     * 
     * @param request
     * @param requestMistmatchTarget
     *            target string builder where to write the reason of the request mismatch with the
     *            tile cache
     * @return the GWC generated tile result if the request matches a tile cache, or {@code null}
     *         otherwise.
     */
    public final ConveyorTile dispatch(final GetMapRequest request,
            StringBuilder requestMistmatchTarget) {

        final String layerName = request.getRawKvp().get("LAYERS");
        /*
         * This is a quick way of checking if the request was for a single layer. We can't really
         * use request.getLayers() because in the event that a layerGroup was requested, the request
         * parser turned it into a list of actual Layers
         */
        if (layerName.indexOf(',') != -1) {
            requestMistmatchTarget.append("more than one layer requested");
            return null;
        }

        if (!tld.layerExists(layerName)) {
            requestMistmatchTarget.append("not a tile layer");
            return null;
        }

        final TileLayer tileLayer;
        try {
            tileLayer = this.tld.getTileLayer(layerName);
        } catch (GeoWebCacheException e) {
            throw new RuntimeException(e);
        }
        if (!tileLayer.isEnabled()) {
            requestMistmatchTarget.append("tile layer disabled");
            return null;
        }

        ConveyorTile tileReq = prepareRequest(tileLayer, request, requestMistmatchTarget);
        if (null == tileReq) {
            return null;
        }
        ConveyorTile tileResp = null;
        try {
            tileResp = tileLayer.getTile(tileReq);
        } catch (Exception e) {
            log.log(Level.INFO, "Error dispatching tile request to GeoServer", e);
        }
        return tileResp;
    }

    ConveyorTile prepareRequest(TileLayer tileLayer, GetMapRequest request,
            StringBuilder requestMistmatchTarget) {

        if (!isCachingPossible(tileLayer, request, requestMistmatchTarget)) {
            return null;
        }

        final MimeType mimeType;
        try {
            mimeType = MimeType.createFromFormat(request.getFormat());
            List<MimeType> tileLayerFormats = tileLayer.getMimeTypes();
            if (!tileLayerFormats.contains(mimeType)) {
                requestMistmatchTarget.append("no tile cache for requested format");
                return null;
            }
        } catch (MimeException me) {
            // not a GWC supported format
            requestMistmatchTarget.append("not a GWC supported format: ").append(me.getMessage());
            return null;
        }

        final GridSubset gridSubset;
        final long[] tileIndex;
        final Map<String, String> fullParameters;
        try {
            final BoundingBox tileBounds;
            {
                Envelope bbox = request.getBbox();
                tileBounds = new BoundingBox(bbox.getMinX(), bbox.getMinY(), bbox.getMaxX(),
                        bbox.getMaxY());
            }
            final List<GridSubset> crsMatchingGridSubsets;
            {
                String srs = request.getSRS();
                int epsgId = Integer.parseInt(srs.substring(srs.indexOf(':') + 1));
                SRS srs2 = SRS.getSRS(epsgId);
                crsMatchingGridSubsets = tileLayer.getGridSubsetsForSRS(srs2);
            }

            if (crsMatchingGridSubsets.isEmpty()) {
                requestMistmatchTarget.append("no cache exists for requested CRS");
                return null;
            }

            {
                long[] matchingTileIndex = new long[3];
                final int reqW = request.getWidth();
                final int reqH = request.getHeight();
                gridSubset = findBestMatchingGrid(tileBounds, crsMatchingGridSubsets, reqW, reqH,
                        matchingTileIndex);
                if (gridSubset == null) {
                    requestMistmatchTarget.append("request does not align to grid(s) ");
                    for (GridSubset gs : crsMatchingGridSubsets) {
                        requestMistmatchTarget.append('\'').append(gs.getName()).append("' ");
                    }
                    return null;
                }
                tileIndex = matchingTileIndex;
            }

            {
                Map<String, String> requestParameterMap = request.getRawKvp();
                fullParameters = tileLayer.getModifiableParameters(requestParameterMap, "UTF-8");
            }

        } catch (Exception e) {
            if (log.isLoggable(Level.FINE)) {
                e.printStackTrace();
                log.log(Level.FINE, "Exception caught checking gwc dispatch preconditions", e);
            }
            Throwable rootCause = getRootCause(e);
            requestMistmatchTarget.append("exception occurred: ")
                    .append(rootCause.getClass().getSimpleName()).append(": ")
                    .append(e.getMessage());
            return null;
        }

        ConveyorTile tileReq;
        final String gridSetId = gridSubset.getName();
        HttpServletRequest servletReq = null;
        HttpServletResponse servletResp = null;
        String layerName = tileLayer.getName();
        tileReq = new ConveyorTile(storageBroker, layerName, gridSetId, tileIndex, mimeType,
                fullParameters, servletReq, servletResp);
        return tileReq;
    }

    /**
     * Determines whether the given {@link GetMapRequest} is a candidate to match a GWC tile or not.
     * 
     * @param layer
     *            the layer name to check against
     * 
     * @param request
     *            the GetMap request to check whether it might match a tile
     * @param requestMistmatchTarget
     */
    boolean isCachingPossible(TileLayer layer, GetMapRequest request,
            StringBuilder requestMistmatchTarget) {

        if (null != request.getRemoteOwsType() || null != request.getRemoteOwsURL()) {
            requestMistmatchTarget.append("request uses remote OWS");
            return false;
        }

        Map<String, ParameterFilter> filters;
        {
            List<ParameterFilter> parameterFilters = layer.getParameterFilters();
            if (null != parameterFilters && parameterFilters.size() > 0) {
                filters = new HashMap<String, ParameterFilter>();
                for (ParameterFilter pf : parameterFilters) {
                    filters.put(pf.getKey().toUpperCase(), pf);
                }
            } else {
                filters = Collections.emptyMap();
            }
        }

        // if (request.isTransparent()) {
        // if (!filterApplies(filters, request, "TRANSPARENT")) {
        // return false;
        // }
        // }

        if (request.getEnv() != null && !request.getEnv().isEmpty()) {
            if (!filterApplies(filters, request, "ENV", requestMistmatchTarget)) {
                return false;
            }
        }

        if (request.getFormatOptions() != null && !request.getFormatOptions().isEmpty()) {
            if (!filterApplies(filters, request, "FORMAT_OPTIONS", requestMistmatchTarget)) {
                return false;
            }
        }
        if (0.0 != request.getAngle()) {
            if (!filterApplies(filters, request, "ANGLE", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getRawKvp().get("BGCOLOR")) {
            if (!filterApplies(filters, request, "BGCOLOR", requestMistmatchTarget)) {
                return false;
            }
        }
        if (0 != request.getBuffer()) {
            if (!filterApplies(filters, request, "BUFFER", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getCQLFilter() && !request.getCQLFilter().isEmpty()) {
            if (!filterApplies(filters, request, "CQL_FILTER", requestMistmatchTarget)) {
                return false;
            }
        }
        if (request.getElevation() != null && !request.getElevation().isEmpty()) {
            if (null != request.getElevation().get(0) &&
                    !filterApplies(filters, request, "ELEVATION", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getFeatureId() && !request.getFeatureId().isEmpty()) {
            if (!filterApplies(filters, request, "FEATUREID", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getFilter() && !request.getFilter().isEmpty()) {
            if (!filterApplies(filters, request, "FILTER", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getPalette()) {
            if (!filterApplies(filters, request, "PALETTE", requestMistmatchTarget)) {
                return false;
            }
        }

        // REVISIT: should these be taken into account?
        // if (null != request.getSld()) {
        // if (!filterApplies(filters, request, "SLD", requestMistmatchTarget)) {
        // return false;
        // }
        // }
        // if (null != request.getSldBody()) {
        // if (!filterApplies(filters, request, "SLD_BODY", requestMistmatchTarget)) {
        // return false;
        // }
        // }

        if (null != request.getStartIndex()) {
            if (!filterApplies(filters, request, "STARTINDEX", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getMaxFeatures()) {
            if (!filterApplies(filters, request, "MAXFEATURES", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getTime() && !request.getTime().isEmpty()) {
            if (null != request.getTime().get(0) && 
                    !filterApplies(filters, request, "TIME", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getViewParams() && !request.getViewParams().isEmpty()) {
            if (!filterApplies(filters, request, "VIEWPARAMS", requestMistmatchTarget)) {
                return false;
            }
        }
        if (null != request.getFeatureVersion()) {
            if (!filterApplies(filters, request, "FEATUREVERSION", requestMistmatchTarget)) {
                return false;
            }
        }

        return true;
    }

    private boolean filterApplies(Map<String, ParameterFilter> filters, GetMapRequest request,
            String key, StringBuilder requestMistmatchTarget) {

        ParameterFilter parameterFilter = filters.get(key);
        if (parameterFilter == null) {
            requestMistmatchTarget.append("no parameter filter exists for ").append(key);
            return false;
        }
        String parameter = request.getRawKvp().get(key);
        boolean applies = parameterFilter.applies(parameter);
        if (!applies) {
            requestMistmatchTarget.append(key).append(
                    " does not apply to parameter filter of the same name");
        }
        return applies;
    }

    /**
     * @param layerName
     * @return the tile layer named {@code layerName}
     * @throws IllegalArgumentException
     *             if no {@link TileLayer} named {@code layeName} is found
     */
    public TileLayer getTileLayerByName(String layerName) throws IllegalArgumentException {
        TileLayer tileLayer;
        try {
            tileLayer = tld.getTileLayer(layerName);
        } catch (GeoWebCacheException e) {
            throw new IllegalArgumentException(e.getMessage(), Throwables.getRootCause(e));
        }
        return tileLayer;
    }

    public Set<String> getTileLayerNames() {
        return tld.getLayerNames();
    }

    /**
     * @return all the GWC tile layers, both GeoServer's and externally defined
     * @see #getGeoServerTileLayers()
     */
    public Iterable<TileLayer> getTileLayers() {
        return tld.getLayerList();
    }

    /**
     * @param nsPrefix
     *            the namespace prefix to filter upon, or {@code null} to return all layers
     * @return the tile layers that belong to a layer(group)info in the given prefix, or all the
     *         {@link TileLayer}s in the {@link TileLayerDispatcher} if {@code nsPrefix == null}
     */
    public Iterable<? extends TileLayer> getTileLayersByNamespacePrefix(final String nsPrefix) {
        if (nsPrefix == null) {
            return getTileLayers();
        }

        final Catalog catalog = getCatalog();

        final NamespaceInfo namespaceFilter = catalog.getNamespaceByPrefix(nsPrefix);
        if (namespaceFilter == null) {
            Iterable<TileLayer> tileLayers = getTileLayers();
            return tileLayers;
        }

        Iterable<GeoServerTileLayer> geoServerTileLayers = getGeoServerTileLayers();

        return Iterables.filter(geoServerTileLayers, new Predicate<GeoServerTileLayer>() {
            @Override
            public boolean apply(GeoServerTileLayer tileLayer) {
                String layerName = tileLayer.getName();
                if (-1 == layerName.indexOf(':')) {
                    return false;
                }
                LayerInfo layerInfo = catalog.getLayerByName(layerName);
                if (layerInfo != null) {
                    NamespaceInfo layerNamespace = layerInfo.getResource().getNamespace();
                    if (namespaceFilter.equals(layerNamespace)) {
                        return true;
                    }
                }
                LayerGroupInfo layerGroupInfo = catalog.getLayerGroupByName(layerName);
                if (layerGroupInfo != null && layerGroupInfo.getLayers() != null && layerGroupInfo.getLayers().size() > 0) {
                	NamespaceInfo layerGroupNamespace = layerGroupInfo.getLayers().get(0).getResource().getNamespace();
                    if (namespaceFilter.equals(layerGroupNamespace)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public Set<String> getLayerNamesForGridSets(final Set<String> gridSetIds) {

        Set<String> layerNames = new TreeSet<String>();

        for (TileLayer layer : getTileLayers()) {
            Set<String> layerGrids = layer.getGridSubsets();
            if (!Sets.intersection(gridSetIds, layerGrids).isEmpty()) {
                layerNames.add(layer.getName());
            }

        }
        return layerNames;
    }

    /**
     * Returns whether the disk quota module is available at all.
     * <p>
     * If not, none of the other diskquota related methods should be even called. The disk quota
     * module may have been completely disabled through the {@code GWC_DISKQUOTA_DISABLED=true}
     * environment variable
     * </p>
     * 
     * @return whether the disk quota module is available at all.
     */
    public boolean isDiskQuotaAvailable() {
        DiskQuotaMonitor diskQuotaMonitor = getDiskQuotaMonitor();
        return diskQuotaMonitor.isEnabled();
    }

    /**
     * @return the current DiskQuota configuration or {@code null} if the disk quota module has been
     *         disabled (i.e. through the {@code GWC_DISKQUOTA_DISABLED=true} environment variable)
     */
    public DiskQuotaConfig getDiskQuotaConfig() {
        if (!isDiskQuotaAvailable()) {
            return null;
        }
        DiskQuotaMonitor monitor = getDiskQuotaMonitor();
        return monitor.getConfig();
    }

    private DiskQuotaMonitor getDiskQuotaMonitor() {
        return monitor;
    }

    public void saveConfig(GWCConfig gwcConfig) throws IOException {
        gwcConfigPersister.save(gwcConfig);
    }

    public void saveDiskQuotaConfig(DiskQuotaConfig config) {
        checkArgument(isDiskQuotaAvailable(), "DiskQuota is not enabled");
        DiskQuotaMonitor monitor = getDiskQuotaMonitor();
        monitor.saveConfig(config);
    }

    public Quota getGlobalQuota() {
        if (!isDiskQuotaAvailable()) {
            return null;
        }
        return getDiskQuotaConfig().getGlobalQuota();
    }

    /**
     * @return the globally used quota, {@code null} if diskquota is disabled
     */
    public Quota getGlobalUsedQuota() {
        if (!isDiskQuotaAvailable()) {
            return null;
        }
        try {
            return quotaStore.getGloballyUsedQuota();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param gridSetName
     * @return {@code null} if disk quota is not enabled, the aggregated quota used by all layer
     *         cached for the given gridset otherwise.
     */
    public Quota getUsedQuotaByGridSet(final String gridSetName) {
        checkNotNull(gridSetName, "GridSet name is null");
        if (!isDiskQuotaAvailable()) {
            return null;
        }

        final Quota quota = new Quota();

        TileSetVisitor visitor = new TileSetVisitor() {
            @Override
            public void visit(final TileSet tileSet, final QuotaStore store) {
                if (!gridSetName.equals(tileSet.getGridsetId())) {
                    return;
                }

                final String tileSetId = tileSet.getId();
                try {
                    Quota used = store.getUsedQuotaByTileSetId(tileSetId);
                    quota.add(used);
                } catch (InterruptedException e) {
                    log.fine(e.getMessage());
                    return;
                }
            }
        };
        quotaStore.accept(visitor);
        return quota;
    }

    /**
     * 
     * @return the Quota limit for the given layer, or {@code null} if no specific limit has been
     *         set for that layer
     */
    public Quota getQuotaLimit(final String layerName) {
        if (!isDiskQuotaAvailable()) {
            return null;
        }

        DiskQuotaConfig disQuotaConfig = getDiskQuotaConfig();
        List<LayerQuota> layerQuotas = disQuotaConfig.getLayerQuotas();
        if (layerQuotas == null) {
            return null;
        }
        for (LayerQuota lq : layerQuotas) {
            if (layerName.equals(lq.getLayer())) {
                return new Quota(lq.getQuota());
            }
        }
        return null;
    }

    /**
     * @return the currently used disk quota for the layer or {@code null} if can't be determined
     */
    public Quota getUsedQuota(final String layerName) {
        if (!isDiskQuotaAvailable()) {
            return null;
        }
        try {
            Quota usedQuotaByLayerName = quotaStore.getUsedQuotaByLayerName(layerName);
            return usedQuotaByLayerName;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Dispatches a request to the GeoServer OWS {@link Dispatcher}
     * 
     * @param params
     *            the KVP map of OWS parameters
     * @param cookies
     * @return an http response wrapper where to grab the raw dispatcher response from
     * @throws Exception
     */
    public Resource dispatchOwsRequest(final Map<String, String> params, Cookie[] cookies)
            throws Exception {

        FakeHttpServletRequest req = new FakeHttpServletRequest(params, cookies);
        FakeHttpServletResponse resp = new FakeHttpServletResponse();

        owsDispatcher.handleRequest(req, resp);
        return new ByteArrayResource(resp.getBytes());
    }

    public GridSetBroker getGridSetBroker() {
        return gridSetBroker;
    }

    // public GeoServerTileLayer getTileLayerById(final String id) {
    // return embeddedConfig.getLayerById(id);
    // }

    public List<LayerInfo> getLayerInfos() {
        return getCatalog().getLayers();
    }

    public List<LayerGroupInfo> getLayerGroups() {
        return getCatalog().getLayerGroups();
    }

    public LayerInfo getLayerInfoById(String layerId) {
        return getCatalog().getLayer(layerId);
    }

    public LayerInfo getLayerInfoByName(String layerName) {
        return getCatalog().getLayerByName(layerName);
    }

    public LayerGroupInfo getLayerGroupByName(String layerName) {
        return getCatalog().getLayerGroupByName(layerName);
    }

    public LayerGroupInfo getLayerGroupById(String id) {
        return getCatalog().getLayerGroup(id);
    }

    /**
     * Adds a layer to the {@link CatalogConfiguration} and saves it.
     */
    public void add(GeoServerTileLayer tileLayer) {
        Configuration config = tld.addLayer(tileLayer);
        try {
            config.save();
        } catch (IOException e) {
            propagate(getRootCause(e));
        }
    }

    /**
     * Notification that a layer has been added; to be called by {@link CatalogConfiguration}
     * whenever {@link CatalogConfiguration#save() save} is called and a layer is added..
     * <p>
     * NOTE: this should be hanlded by GWC itself somehow, like with a configuration listener of
     * some sort.
     * 
     * @param layerName
     */
    public void layerAdded(String layerName) {
        if (isDiskQuotaAvailable()) {
            try {
                quotaStore.createLayer(layerName);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Notification that a layer has been added; to be called by {@link CatalogConfiguration}
     * whenever {@link CatalogConfiguration#save() save} is called and a layer has been renamed.
     * <p>
     * NOTE: this should be hanlded by GWC itself somehow, like with a configuration listener of
     * some sort.
     * 
     * @param oldLayerName
     * @param newLayerName
     */
    public void layerRenamed(String oldLayerName, String newLayerName) {
        try {
            log.info("Renaming GWC TileLayer '" + oldLayerName + "' as '" + newLayerName + "'");
            // /embeddedConfig.rename(oldLayerName, newLayerName);
            storageBroker.rename(oldLayerName, newLayerName);
        } catch (StorageException e) {
            log.log(Level.WARNING, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public boolean isServiceEnabled(final Service service) {
        return getConfig().isEnabled(service.getPathName());
    }

    /**
     * @return {@code true} if there's a TileLayer named {@code layerName}
     */
    public boolean tileLayerExists(final String layerName) {
        return tld.layerExists(layerName);
    }

    /**
     * @param namespaceURI
     *            the feature type namespace
     * @param typeName
     *            the feature type name
     * @return the set of TileLayer names (from LayerInfos and LayerGroupInfos) affected by the
     *         feature type, may be empty
     */
    public Set<String> getTileLayersByFeatureType(final String namespaceURI, final String typeName) {
        NamespaceInfo namespace;
        if (namespaceURI == null || XMLConstants.DEFAULT_NS_PREFIX.equals(namespaceURI)) {
            namespace = getCatalog().getDefaultNamespace();
        } else {
            namespace = getCatalog().getNamespaceByURI(namespaceURI);
        }

        final FeatureTypeInfo typeInfo = getCatalog().getFeatureTypeByName(namespace, typeName);
        final List<LayerInfo> layers = getCatalog().getLayers(typeInfo);

        Set<String> affectedLayers = new HashSet<String>();

        for (LayerInfo layer : layers) {
            final String tileLayerName = tileLayerName(layer);
            if (tileLayerExists(tileLayerName)) {
                affectedLayers.add(tileLayerName);
            }
        }

        for (LayerGroupInfo lgi : getLayerGroups()) {
            final String tileLayerName = tileLayerName(lgi);
            if (!tileLayerExists(tileLayerName)) {
                continue;
            }
            for (LayerInfo li : lgi.getLayers()) {
                ResourceInfo resource = li.getResource();
                if (typeInfo.equals(resource)) {
                    affectedLayers.add(tileLayerName);
                }
            }
        }
        return affectedLayers;
    }

    public synchronized void addGridSet(final GridSet gridSet) throws IllegalArgumentException,
            IOException {
        checkNotNull(gridSet);
        tld.addGridSet(gridSet);
    }

    public synchronized void modifyGridSet(final String oldGridSetName, final GridSet newGridSet)
            throws IllegalArgumentException, IOException {

        checkNotNull(oldGridSetName);
        checkNotNull(newGridSet);

        final GridSet oldGridSet = gridSetBroker.get(oldGridSetName);
        if (null == oldGridSet) {
            throw new IllegalArgumentException("GridSet " + oldGridSetName + " does not exist");
        }

        final boolean needsTruncate = oldGridSet.shouldTruncateIfChanged(newGridSet);
        if (needsTruncate) {
            log.warning("### Changes in gridset force truncation of affected Tile layers");
            log.info("### Old gridset: " + oldGridSet);
            log.info("### New gridset: " + newGridSet);
        }

        Map<TileLayer, GridSubset> affectedLayers = new HashMap<TileLayer, GridSubset>();
        try {

            for (TileLayer layer : getTileLayers()) {
                GridSubset gridSubet;
                if (null != (gridSubet = layer.getGridSubset(oldGridSetName))) {
                    layer.acquireLayerLock();// no more cache fetches until we're done here
                    affectedLayers.put(layer, gridSubet);
                    layer.removeGridSubset(oldGridSetName);
                    if (needsTruncate) {
                        deleteCacheByGridSetId(layer.getName(), oldGridSetName);
                    }
                }
            }

            // now no layer is referencing it

            XMLConfiguration mainConfig = getXmlConfiguration();

            mainConfig.removeGridset(oldGridSetName);
            mainConfig.addOrReplaceGridSet(new XMLGridSet(newGridSet));
            mainConfig.save();
            getGridSetBroker().remove(oldGridSetName);
            getGridSetBroker().put(newGridSet);

            // tld.removeGridset(oldGridSetName);
            // tld.addGridSet(newGridSet);
            // if (isRename && !needsTruncate) {
            // // /TODO: quotaStore.renameGridSet(oldGridSetName, newGidSetName);
            // }

            final boolean sameSRS = oldGridSet.getSrs().equals(newGridSet.getSrs());

            final int maxZoomLevel = newGridSet.getNumLevels() - 1;

            Set<Configuration> saveConfigurations = new HashSet<Configuration>();

            // now restore the gridsubset for each layer
            for (Map.Entry<TileLayer, GridSubset> entry : affectedLayers.entrySet()) {
                TileLayer layer = entry.getKey();
                GridSubset gsubset = entry.getValue();

                BoundingBox gridSetExtent = gsubset.getOriginalExtent();
                if (null != gridSetExtent && sameSRS) {
                    gridSetExtent = newGridSet.getOriginalExtent().intersection(gridSetExtent);
                }

                int zoomStart = gsubset.getZoomStart();
                int zoomStop = gsubset.getZoomStop();

                if (zoomStart > maxZoomLevel) {
                    zoomStart = maxZoomLevel;
                }
                if (zoomStop > maxZoomLevel || zoomStop < zoomStart) {
                    zoomStop = maxZoomLevel;
                }

                GridSubset newGridSubset = GridSubsetFactory.createGridSubSet(newGridSet,
                        gridSetExtent, zoomStart, zoomStop);

                layer.removeGridSubset(oldGridSetName);
                layer.addGridSubset(newGridSubset);

                Configuration config = tld.getConfiguration(layer);
                config.modifyLayer(layer);
                saveConfigurations.add(config);
            }

            for (Configuration config : saveConfigurations) {
                config.save();
            }
        } finally {
            for (TileLayer layer : affectedLayers.keySet()) {
                layer.releaseLayerLock();
            }
        }
    }

    XMLConfiguration getXmlConfiguration() {
        XMLConfiguration mainConfig = GeoWebCacheExtensions.bean(XMLConfiguration.class);
        return mainConfig;
    }

    @SuppressWarnings("unchecked")
    public Response getResponseEncoder(MimeType responseFormat, RenderedImageMap metaTileMap) {
        final String format = responseFormat.getFormat();
        final String mimeType = responseFormat.getMimeType();

        Response response = cachedTileEncoders.get(format);
        if (response == null) {
            final Operation operation;
            {
                GetMapRequest getMap = new GetMapRequest();
                getMap.setFormat(mimeType);
                Object[] parameters = { getMap };
                org.geoserver.platform.Service service = (org.geoserver.platform.Service) GeoServerExtensions
                        .bean("wms-1_1_1-ServiceDescriptor");
                if (service == null) {
                    throw new IllegalStateException(
                            "Didn't find service descriptor 'wms-1_1_1-ServiceDescriptor'");
                }
                operation = new Operation("GetMap", service, (Method) null, parameters);
            }

            final List<Response> extensions = GeoServerExtensions.extensions(Response.class);
            final Class<?> webMapClass = metaTileMap.getClass();
            for (Response r : extensions) {
                if (r.getBinding().isAssignableFrom(webMapClass) && r.canHandle(operation)) {
                    synchronized (cachedTileEncoders) {
                        cachedTileEncoders.put(mimeType, r);
                        response = r;
                        break;
                    }
                }
            }
            if (response == null) {
                throw new IllegalStateException("Didn't find a " + Response.class.getName()
                        + " to handle " + mimeType);
            }
        }
        return response;
    }

    public boolean isQueryable(final GeoServerTileLayer geoServerTileLayer) {
        WMS wmsMediator = WMS.get();
        LayerInfo layerInfo = geoServerTileLayer.getLayerInfo();
        if (layerInfo != null) {
            return wmsMediator.isQueryable(layerInfo);
        }
        LayerGroupInfo lgi = geoServerTileLayer.getLayerGroupInfo();
        return wmsMediator.isQueryable(lgi);
    }

    /**
     * @return all tile layers backed by a geoserver layer/layergroup
     * @see #getTileLayers()
     */
    public Iterable<GeoServerTileLayer> getGeoServerTileLayers() {
        final Iterable<TileLayer> tileLayers = getTileLayers();

        Iterable<GeoServerTileLayer> filtered = Iterables.filter(tileLayers,
                GeoServerTileLayer.class);

        return filtered;
    }

    public void save(final GeoServerTileLayer layer) {
        checkNotNull(layer);
        log.info("Saving GeoSeverTileLayer " + layer.getName());

        Configuration modifiedConfig = tld.modify(layer);
        try {
            modifiedConfig.save();
        } catch (IOException e) {
            Throwable rootCause = Throwables.getRootCause(e);
            throw Throwables.propagate(rootCause);
        }
    }

    /**
     * Returns the tile layers that refer to the given style, either as the tile layer's
     * {@link GeoServerTileLayer#getStyles() default style} or one of the
     * {@link GeoServerTileLayerInfoImpl#cachedStyles() cached styles}.
     * <p>
     * The result may be different from {@link #getLayerInfosFor(StyleInfo)} and
     * {@link #getLayerGroupsFor(StyleInfo)} as the {@link GeoServerTileLayerInfoImpl}'s backing
     * each {@link GeoServerTileLayer} may have assigned a subset of the layerinfo styles for
     * caching.
     * </p>
     */
    public List<GeoServerTileLayer> getTileLayersForStyle(final String styleName) {

        Iterable<GeoServerTileLayer> tileLayers = getGeoServerTileLayers();

        List<GeoServerTileLayer> affected = new ArrayList<GeoServerTileLayer>();
        for (GeoServerTileLayer tl : tileLayers) {
            GeoServerTileLayerInfo info = tl.getInfo();
            String defaultStyle = tl.getStyles();// may be null if backed by a LayerGroupInfo
            Set<String> cachedStyles = info.cachedStyles();
            if (styleName.equals(defaultStyle) || cachedStyles.contains(styleName)) {
                affected.add(tl);
            }
        }
        return affected;
    }

    /**
     * @return all the {@link LayerInfo}s in the {@link Catalog} that somehow refer to the given
     *         style
     */
    public Iterable<LayerInfo> getLayerInfosFor(final StyleInfo style) {
        final String styleName = style.getName();
        List<LayerInfo> result = new ArrayList<LayerInfo>();
        {
            for (LayerInfo layer : getLayerInfos()) {
                String name = layer.getDefaultStyle().getName();
                if (styleName.equals(name)) {
                    result.add(layer);
                    continue;
                }
                for (StyleInfo alternateStyle : layer.getStyles()) {
                    name = alternateStyle.getName();
                    if (styleName.equals(name)) {
                        result.add(layer);
                        break;
                    }
                }
            }
        }
        return result;
    }

    /**
     * @return all the layergroups that somehow refer to the given style
     */
    public Iterable<LayerGroupInfo> getLayerGroupsFor(final StyleInfo style) {
        List<LayerGroupInfo> layerGroups = new ArrayList<LayerGroupInfo>();

        for (LayerGroupInfo layerGroup : getLayerGroups()) {

            final List<StyleInfo> explicitLayerGroupStyles = layerGroup.getStyles();
            final List<LayerInfo> groupLayers = layerGroup.getLayers();

            for (int layerN = 0; layerN < groupLayers.size(); layerN++) {

                LayerInfo childLayer = groupLayers.get(layerN);
                StyleInfo assignedLayerStyle = explicitLayerGroupStyles.get(layerN);
                if (assignedLayerStyle == null) {
                    assignedLayerStyle = childLayer.getDefaultStyle();
                }

                if (style.equals(assignedLayerStyle)) {
                    layerGroups.add(layerGroup);
                    break;
                }
            }
        }
        return layerGroups;
    }

    /**
     * Computes and returns the area of validity for the given CoordinateReferenceSystem.
     * <p>
     * This method returns the prescribed area of validity for the CRS as computed by
     * {@link CRS#getEnvelope(CoordinateReferenceSystem)} with the exception that the following
     * {@code EPSG:900913} compatible CRS's return the GeoWebCache prescribed bounds so that they
     * align with Google Map tiles: {@code EPSG:900913, EPSG:3857, EPSG:3785}.
     * </p>
     * 
     * @param coordSys
     *            the CRS to compute the area of validity for
     * @return the aov for the CRS, or {@code null} if the CRS does not provide such information
     *         (with the exception of EPSG:900913, see above).
     */
    public ReferencedEnvelope getAreaOfValidity(final CoordinateReferenceSystem coordSys) {
        Geometry aovGeom = getAreaOfValidityAsGeometry(coordSys, gridSetBroker);
        if (aovGeom == null) {
            return null;
        }
        Envelope envelope = aovGeom.getEnvelopeInternal();
        double x1 = envelope.getMinX();
        double x2 = envelope.getMaxX();
        double y1 = envelope.getMinY();
        double y2 = envelope.getMaxY();

        ReferencedEnvelope aov = new ReferencedEnvelope(coordSys);
        aov.init(x1, x2, y1, y2);
        return aov;
    }

    public static Geometry getAreaOfValidityAsGeometry(final CoordinateReferenceSystem targetCrs,
            final GridSetBroker gridSetBroker) {

        CoordinateReferenceSystem variant;
        String[] variants = { "EPSG:900913", "EPSG:3857", "EPSG:3785" };

        boolean is900913Compatible = false;
        for (String variantCode : variants) {
            variant = variant(variantCode);
            is900913Compatible = variant != null && CRS.equalsIgnoreMetadata(targetCrs, variant);
            if (is900913Compatible) {
                break;
            }
        }

        if (is900913Compatible) {
            BoundingBox prescribedBounds = gridSetBroker.WORLD_EPSG3857.getBounds();
            return JTS.toGeometry(new Envelope(prescribedBounds.getMinX(), prescribedBounds
                    .getMaxX(), prescribedBounds.getMinY(), prescribedBounds.getMaxY()));
        }

        final org.opengis.geometry.Envelope envelope = CRS.getEnvelope(targetCrs);
        if (envelope == null) {
            return null;
        }

        Geometry aovGeom;

        final double tolerance = 1E-6;
        if (envelope.getSpan(0) < tolerance || envelope.getSpan(1) < tolerance) {
            //
            GeographicBoundingBox latLonBBox = CRS.getGeographicBoundingBox(targetCrs);
            ReferencedEnvelope bbox = new ReferencedEnvelope(new GeneralEnvelope(latLonBBox));
            Polygon geometry = JTS.toGeometry(bbox);
            double distanceTolerance = Math.max(bbox.getSpan(0), bbox.getSpan(1)) / 2E5;
            Geometry densifiedGeom = Densifier.densify(geometry, distanceTolerance);
            MathTransform mathTransform;
            try {
                CoordinateReferenceSystem sourceCRS = bbox.getCoordinateReferenceSystem();
                mathTransform = CRS.findMathTransform(sourceCRS, targetCrs);
                aovGeom = JTS.transform(densifiedGeom, mathTransform);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            aovGeom = JTS.toGeometry(new Envelope(envelope.getMinimum(0), envelope.getMaximum(0),
                    envelope.getMinimum(1), envelope.getMaximum(1)));
        }

        aovGeom.setUserData(targetCrs);
        return aovGeom;
    }

    private static CoordinateReferenceSystem variant(String code) {
        CoordinateReferenceSystem variant;
        try {
            variant = CRS.decode(code);
        } catch (Exception e) {
            log.log(Level.FINE, e.getMessage(), e);
            return null;
        }
        return variant;
    }

    /**
     * @return {@code true} if the GridSet named {@code gridSetId} is a GWC internally defined one,
     *         {@code false} otherwise
     */
    public boolean isInternalGridSet(final String gridSetId) {
        boolean internal = gridSetBroker.getEmbeddedNames().contains(gridSetId);
        return internal;
    }

    /**
     * Completely deletes the cache for a layer/gridset combination; differs from truncate that the
     * layer doesn't need to have a gridSubset associated for the given gridset at runtime (in order
     * to handle the deletion of a layer's gridsubset)
     * 
     * @param layerName
     * @param removedGridset
     * @TODO: make async?, it may take a while to the metastore to delete all tiles (sigh)
     */
    public void deleteCacheByGridSetId(final String layerName, final String gridSetId) {
        try {
            storageBroker.deleteByGridSetId(layerName, gridSetId);
        } catch (StorageException e) {
            throw propagate(getRootCause(e));
        }
    }

    /**
     * Completely and persistently eliminates, including the cached contents, the given tile layers.
     * 
     * @param tileLayerNames
     */
    public void removeTileLayers(final List<String> tileLayerNames) {
        checkNotNull(tileLayerNames);

        Set<Configuration> confsToSave = new HashSet<Configuration>();

        for (String tileLayerName : tileLayerNames) {
            Configuration configuration = tld.removeLayer(tileLayerName);
            if (configuration != null) {
                confsToSave.add(configuration);
            }
        }

        for (Configuration conf : confsToSave) {
            try {
                conf.save();
            } catch (IOException e) {
                log.log(Level.WARNING, "Error saving GWC Configuration " + conf.getIdentifier(), e);
            }
        }
    }

    public synchronized void removeGridSets(final Set<String> gridsetIds) throws IOException {
        checkNotNull(gridsetIds);

        final Set<String> affectedLayers = getLayerNamesForGridSets(gridsetIds);

        final Set<Configuration> changedConfigs = new HashSet<Configuration>();

        for (String layerName : affectedLayers) {
            TileLayer tileLayer = getTileLayerByName(layerName);
            tileLayer.acquireLayerLock();
            try {
                for (String gridSetId : gridsetIds) {
                    if (tileLayer.getGridSubsets().contains(gridSetId)) {
                        tileLayer.removeGridSubset(gridSetId);
                        deleteCacheByGridSetId(layerName, gridSetId);
                    }
                }
                if (tileLayer.getGridSubsets().isEmpty()) {
                    tileLayer.setEnabled(false);
                }
                try {
                    Configuration configuration = tld.modify(tileLayer);
                    changedConfigs.add(configuration);
                } catch (IllegalArgumentException ignore) {
                    // layer removed? don't care
                }
            } finally {
                tileLayer.releaseLayerLock();
            }
        }

        // All referencing layers updated, now can remove the gridsets
        for (String gridSetId : gridsetIds) {
            Configuration configuration = tld.removeGridset(gridSetId);
            changedConfigs.add(configuration);
        }

        // now make it all persistent
        for (Configuration config : changedConfigs) {
            config.save();
        }
    }

   
    /**
     * Creates new tile layers for the layers and layergroups given by their names using the
     * settings of the given default config options
     * 
     * @param catalogLayerNames
     * @param saneConfig
     */
    public void autoConfigureLayers(List<String> catalogLayerNames, GWCConfig saneConfig) {
        checkArgument(saneConfig.isSane());

        final Catalog catalog = getCatalog();
        for (String name : catalogLayerNames) {

            checkArgument(!tileLayerExists(name),
                    "Can't auto configure Layer, a tile layer named '", name, "' already exists.");

            GeoServerTileLayer tileLayer = null;
            LayerInfo layer = catalog.getLayerByName(name);
            
            if (layer != null) {
                tileLayer = new GeoServerTileLayer(layer, saneConfig, gridSetBroker);
            } else {
                LayerGroupInfo layerGroup = catalog.getLayerGroupByName(name);
                if (layerGroup != null) {
                    tileLayer = new GeoServerTileLayer(layerGroup, saneConfig, gridSetBroker);
                }
            }
            if (tileLayer != null) {
                add(tileLayer);
            } else {
                log.warning("Requested layer " + name + " does not exist. Won't create TileLayer");
            }
        }
    }

    /**
     * @param source
     *            either a {@link LayerInfo} or a {@link LayerGroupInfo}
     * @return {@code true} if source has a tile layer associated, false otherwise, even if source
     *         is not an instance of {@link LayerInfo} or {@link LayerGroupInfo}
     */
    public boolean hasTileLayer(CatalogInfo source) {
        final String tileLayerId;
        if (source instanceof ResourceInfo) {
            LayerInfo layerInfo = getCatalog().getLayerByName(
                    ((ResourceInfo) source).prefixedName());
            if (layerInfo == null) {
                return false;
            }
            tileLayerId = layerInfo.getId();
        } else if (source instanceof LayerInfo) {
            tileLayerId = ((LayerInfo) source).getId();
        } else if (source instanceof LayerGroupInfo) {
            tileLayerId = ((LayerGroupInfo) source).getId();
        } else {
            return false;
        }
        Configuration configuration;
        try {
            configuration = tld.getConfiguration(tileLayerId);
        } catch (IllegalArgumentException notFound) {
            return false;
        }
        return configuration instanceof CatalogConfiguration;
    }

    /**
     * @param source
     *            either a {@link LayerInfo}, {@link ResourceInfo}, or a {@link LayerGroupInfo}
     * @return {@code null}
     * @throws IllegalArgumentException
     *             if source is not of a supported type
     */
    public GeoServerTileLayer getTileLayer(CatalogInfo source) {
        final String name;
        if (source instanceof ResourceInfo) {
            name = ((ResourceInfo) source).prefixedName();
        } else if (source instanceof LayerInfo) {
            name = tileLayerName(((LayerInfo) source));
        } else if (source instanceof LayerGroupInfo) {
            name = tileLayerName(((LayerGroupInfo) source));
        } else {
            return null;
        }
        TileLayer tileLayer;
        try {
            tileLayer = tld.getTileLayer(name);
        } catch (GeoWebCacheException notFound) {
            return null;
        }
        if (tileLayer instanceof GeoServerTileLayer) {
            return (GeoServerTileLayer) tileLayer;
        }
        return null;
    }

    public CoordinateReferenceSystem getDeclaredCrs(final String geoServerTileLayerName) {
        GeoServerTileLayer layer = (GeoServerTileLayer) getTileLayerByName(geoServerTileLayerName);
        LayerInfo layerInfo = layer.getLayerInfo();
        if (layerInfo != null) {
            return layerInfo.getResource().getCRS();
        }
        LayerGroupInfo layerGroupInfo = layer.getLayerGroupInfo();
        ReferencedEnvelope bounds = layerGroupInfo.getBounds();
        return bounds.getCoordinateReferenceSystem();
    }

    public static String tileLayerName(LayerInfo li) {
        // REVISIT when/if layerinfo.getName gets decoupled from LayerInfo.resource.name
        return li.getResource().prefixedName();
    }

    public static String tileLayerName(LayerGroupInfo lgi) {
        return lgi.prefixedName();
    }

    /**
     * Flush caches
     */
    public void reset() {
        CatalogConfiguration c = GeoServerExtensions.bean(CatalogConfiguration.class);
        if (c != null) {
            c.reset();
        }
    }
}
