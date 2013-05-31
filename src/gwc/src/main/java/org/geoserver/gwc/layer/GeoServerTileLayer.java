/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.propagate;
import static org.geoserver.gwc.GWC.tileLayerName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.RenderedImageMap;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.CanonicalSet;
import org.geotools.util.logging.Logging;
import org.geowebcache.GeoWebCacheException;
import org.geowebcache.config.ConfigurationException;
import org.geowebcache.config.XMLGridSubset;
import org.geowebcache.conveyor.ConveyorTile;
import org.geowebcache.filter.parameters.ParameterException;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.request.RequestFilter;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSet;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.grid.OutsideCoverageException;
import org.geowebcache.grid.SRS;
import org.geowebcache.io.Resource;
import org.geowebcache.layer.GridLocObj;
import org.geowebcache.layer.LayerListenerList;
import org.geowebcache.layer.MetaTile;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.layer.TileLayerListener;
import org.geowebcache.layer.meta.ContactInformation;
import org.geowebcache.layer.meta.LayerMetaInformation;
import org.geowebcache.layer.updatesource.UpdateSourceDefinition;
import org.geowebcache.mime.FormatModifier;
import org.geowebcache.mime.MimeException;
import org.geowebcache.mime.MimeType;
import org.geowebcache.util.GWCVars;
import org.geowebcache.util.ServletUtils;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class GeoServerTileLayer extends TileLayer {

    private static final Logger LOGGER = Logging.getLogger(GeoServerTileLayer.class);

    private final GeoServerTileLayerInfo info;

    public static final String GWC_SEED_INTERCEPT_TOKEN = "GWC_SEED_INTERCEPT";

    public static final ThreadLocal<WebMap> WEB_MAP = new ThreadLocal<WebMap>();

    private final LayerInfo layerInfo;

    private final LayerGroupInfo layerGroupInfo;

    private String configErrorMessage;

    private Map<String, GridSubset> subSets;

    private static LayerListenerList listeners = new LayerListenerList();

    private final GridSetBroker gridSetBroker;

    public GeoServerTileLayer(final LayerGroupInfo layerGroup, final GWCConfig configDefaults,
            final GridSetBroker gridsets) {
        checkNotNull(layerGroup, "layerGroup");
        checkNotNull(gridsets, "gridsets");
        checkNotNull(configDefaults, "configDefaults");

        this.gridSetBroker = gridsets;
        this.layerInfo = null;
        this.layerGroupInfo = layerGroup;
        this.info = TileLayerInfoUtil.loadOrCreate(layerGroup, configDefaults);
    }

    public GeoServerTileLayer(final LayerInfo layerInfo, final GWCConfig configDefaults,
            final GridSetBroker gridsets) {
        checkNotNull(layerInfo, "layerInfo");
        checkNotNull(gridsets, "gridsets");
        checkNotNull(configDefaults, "configDefaults");

        this.gridSetBroker = gridsets;
        this.layerInfo = layerInfo;
        this.layerGroupInfo = null;
        this.info = TileLayerInfoUtil.loadOrCreate(layerInfo, configDefaults);
    }

    public GeoServerTileLayer(final LayerGroupInfo layerGroup, final GridSetBroker gridsets,
            final GeoServerTileLayerInfo state) {
        checkNotNull(layerGroup, "layerGroup");
        checkNotNull(gridsets, "gridsets");
        checkNotNull(state, "state");

        this.gridSetBroker = gridsets;
        this.layerInfo = null;
        this.layerGroupInfo = layerGroup;
        this.info = state;
    }

    public GeoServerTileLayer(final LayerInfo layerInfo, final GridSetBroker gridsets,
            final GeoServerTileLayerInfo state) {
        checkNotNull(layerInfo, "layerInfo");
        checkNotNull(gridsets, "gridsets");
        checkNotNull(state, "state");

        this.gridSetBroker = gridsets;
        this.layerInfo = layerInfo;
        this.layerGroupInfo = null;
        this.info = state;
        TileLayerInfoUtil.checkAutomaticStyles(layerInfo, state);
    }

    @Override
    public String getId() {
        return info.getId();
    }

    @Override
    public String getName() {
        return info.getName();
    }

    void setConfigErrorMessage(String configErrorMessage) {
        this.configErrorMessage = configErrorMessage;
    }

    public String getConfigErrorMessage() {
        return configErrorMessage;
    }

    @Override
    public List<ParameterFilter> getParameterFilters() {
        return new ArrayList<ParameterFilter>(info.getParameterFilters());
    }

    public void resetParameterFilters() {
        super.defaultParameterFilterValues = null;// reset default values
    }

    /**
     * Returns whether this tile layer is enabled.
     * <p>
     * The layer is enabled if the following conditions apply:
     * <ul>
     * <li>Caching for this layer is enabled by configuration
     * <li>Its backing {@link LayerInfo} or {@link LayerGroupInfo} is enabled and not errored (as
     * per {@link LayerInfo#enabled()} {@link LayerGroupInfo#}
     * <li>The layer is not errored ({@link #getConfigErrorMessage() == null}
     * </ul>
     * The layer is enabled by configuration if: the {@code GWC.enabled} metadata property is set to
     * {@code true} in it's corresponding {@link LayerInfo} or {@link LayerGroupInfo}
     * {@link MetadataMap}, or there's no {@code GWC.enabled} property set at all but the global
     * {@link GWCConfig#isCacheLayersByDefault()} is {@code true}.
     * </p>
     * 
     * @see org.geowebcache.layer.TileLayer#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        final boolean tileLayerInfoEnabled = info.isEnabled();
        if (!tileLayerInfoEnabled) {
            return false;
        }
        if (getConfigErrorMessage() != null) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Layer " + getName() + "is not enabled due to config error: "
                        + getConfigErrorMessage());
            }
            return false;
        }
        boolean geoserverLayerEnabled;
        LayerInfo layerInfo = getLayerInfo();
        if (layerInfo != null) {
            geoserverLayerEnabled = layerInfo.enabled();
        } else {
            // LayerGroupInfo has no enabled property, so assume true
            geoserverLayerEnabled = true;
        }
        return tileLayerInfoEnabled && geoserverLayerEnabled;
    }

    @Override
    public void setEnabled(final boolean enabled) {
        info.setEnabled(enabled);
    }

    /**
     * 
     * @see org.geowebcache.layer.TileLayer#isQueryable()
     * @see WMS#isQueryable(LayerGroupInfo)
     * @see WMS#isQueryable(LayerInfo)
     */
    @Override
    public boolean isQueryable() {
        boolean queryable = GWC.get().isQueryable(this);
        return queryable;
    }

    private ReferencedEnvelope getLatLonBbox() throws IllegalStateException {
        final CoordinateReferenceSystem wgs84LonFirst;
        try {
            final boolean longitudeFirst = true;
            wgs84LonFirst = CRS.decode("EPSG:4326", longitudeFirst);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ReferencedEnvelope latLongBbox;
        if (getLayerInfo() == null) {
            LayerGroupInfo groupInfo = getLayerGroupInfo();
            try {
                ReferencedEnvelope bounds = groupInfo.getBounds();
                boolean lenient = true;
                latLongBbox = bounds.transform(wgs84LonFirst, lenient);
            } catch (Exception e) {
                String msg = "Can't get lat long bounds for layer group "
                        + tileLayerName(groupInfo);
                LOGGER.log(Level.WARNING, msg, e);
                throw new IllegalStateException(msg, e);
            }
        } else {
            ResourceInfo resourceInfo = getResourceInfo();
            latLongBbox = resourceInfo.getLatLonBoundingBox();
            if (null == latLongBbox) {
                latLongBbox = new ReferencedEnvelope(wgs84LonFirst);
            }
            if (null == latLongBbox.getCoordinateReferenceSystem()) {
                ReferencedEnvelope tmp = new ReferencedEnvelope(wgs84LonFirst);
                tmp.init(latLongBbox.getMinX(), latLongBbox.getMaxX(), latLongBbox.getMinY(),
                        latLongBbox.getMaxY());
                latLongBbox = tmp;
            }
        }
        return latLongBbox;
    }

    /**
     * @return the {@link LayerInfo} for this layer, or {@code null} if it's backed by a
     *         {@link LayerGroupInfo} instead
     */
    public LayerInfo getLayerInfo() {
        return layerInfo;
    }

    /**
     * @return the {@link LayerGroupInfo} for this layer, or {@code null} if it's backed by a
     *         {@link LayerInfo} instead
     */
    public LayerGroupInfo getLayerGroupInfo() {
        return layerGroupInfo;
    }

    private ResourceInfo getResourceInfo() {
        LayerInfo layerInfo = getLayerInfo();
        return layerInfo == null ? null : layerInfo.getResource();
    }

    /**
     * Overrides to return a dynamic view of the backing {@link LayerInfo} or {@link LayerGroupInfo}
     * metadata adapted to GWC
     * 
     * @see org.geowebcache.layer.TileLayer#getMetaInformation()
     */
    @Override
    public LayerMetaInformation getMetaInformation() {
        LayerMetaInformation meta = null;
        String title = getName();
        String description = "";
        List<String> keywords = Collections.emptyList();
        List<ContactInformation> contacts = Collections.emptyList();

        ResourceInfo resourceInfo = getResourceInfo();
        if (resourceInfo != null) {
            title = resourceInfo.getTitle();
            description = resourceInfo.getAbstract();
            keywords = new ArrayList<String>();
            for (KeywordInfo kw : resourceInfo.getKeywords()) {
                keywords.add(kw.getValue());
            }
        }
        meta = new LayerMetaInformation(title, description, keywords, contacts);
        return meta;
    }

    /**
     * The default style name for the layer, as advertised by its backing
     * {@link LayerInfo#getDefaultStyle()}, or {@code null} if this tile layer is backed by a
     * {@link LayerGroupInfo}.
     * <p>
     * As the default style is always cached, its name is not stored as part of this tile layer's
     * {@link GeoServerTileLayerInfo}. Instead it's 'live' and retrieved from the current
     * {@link LayerInfo} every time this method is invoked.
     * </p>
     * 
     * @see org.geowebcache.layer.TileLayer#getStyles()
     * @see GeoServerTileLayerInfo#getDefaultStyle()
     */
    @Override
    public String getStyles() {
        if (layerGroupInfo != null) {
            // there's no such thing as default style for a layer group
            return null;
        }
        StyleInfo defaultStyle = layerInfo.getDefaultStyle();
        if (defaultStyle == null) {
            setConfigErrorMessage("Underlying GeoSever Layer has no default style");
            return null;
        }
        return defaultStyle.getName();
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getFeatureInfo
     * @see GWC#dispatchOwsRequest
     */
    @Override
    public Resource getFeatureInfo(ConveyorTile convTile, BoundingBox bbox, int height, int width,
            int x, int y) throws GeoWebCacheException {

        Map<String, String> params = buildGetFeatureInfo(convTile, bbox, height, width, x, y);
        Resource response;
        try {
            response = GWC.get().dispatchOwsRequest(params, (Cookie[]) null);
        } catch (Exception e) {
            throw new GeoWebCacheException(e);
        }
        return response;
    }

    private Map<String, String> buildGetFeatureInfo(ConveyorTile convTile, BoundingBox bbox,
            int height, int width, int x, int y) {
        Map<String, String> wmsParams = new HashMap<String, String>();
        wmsParams.put("SERVICE", "WMS");
        wmsParams.put("VERSION", "1.1.1");
        wmsParams.put("REQUEST", "GetFeatureInfo");
        wmsParams.put("LAYERS", getName());
        wmsParams.put("STYLES", "");
        wmsParams.put("QUERY_LAYERS", getName());
        MimeType mimeType = convTile.getMimeType();
        if (mimeType == null) {
            mimeType = getMimeTypes().get(0);
        }
        wmsParams.put("FORMAT", mimeType.getFormat());
        wmsParams.put("EXCEPTIONS", GetMapRequest.SE_XML);

        wmsParams.put("INFO_FORMAT", convTile.getMimeType().getFormat());

        GridSubset gridSubset = convTile.getGridSubset();

        wmsParams.put("SRS", gridSubset.getSRS().toString());
        wmsParams.put("HEIGHT", String.valueOf(height));
        wmsParams.put("WIDTH", String.valueOf(width));
        wmsParams.put("BBOX", bbox.toString());
        wmsParams.put("X", String.valueOf(x));
        wmsParams.put("Y", String.valueOf(y));
        String featureCount;
        {
            Map<String, String> values = ServletUtils.selectedStringsFromMap(
                    convTile.servletReq.getParameterMap(),
                    convTile.servletReq.getCharacterEncoding(), "feature_count");
            featureCount = values.get("feature_count");
        }
        if (featureCount != null) {
            wmsParams.put("FEATURE_COUNT", featureCount);
        }

        Map<String, String> fullParameters = convTile.getFullParameters();
        if (fullParameters.isEmpty()) {
            fullParameters = getDefaultParameterFilters();
        }
        wmsParams.putAll(fullParameters);

        return wmsParams;
    }

    @Override
    public ConveyorTile getTile(ConveyorTile tile) throws GeoWebCacheException, IOException,
            OutsideCoverageException {
        MimeType mime = tile.getMimeType();
        final List<MimeType> formats = getMimeTypes();
        if (mime == null) {
            mime = formats.get(0);
        } else {
            if (!formats.contains(mime)) {
                throw new IllegalArgumentException(mime.getFormat()
                        + " is not a supported format for " + getName());
            }
        }

        final String tileGridSetId = tile.getGridSetId();
        final GridSubset gridSubset = getGridSubset(tileGridSetId);
        if (gridSubset == null) {
            throw new IllegalArgumentException("Requested gridset not found: " + tileGridSetId);
        }

        final long[] gridLoc = tile.getTileIndex();
        checkNotNull(gridLoc);

        // Final preflight check, throws OutsideCoverageException if necessary
        gridSubset.checkCoverage(gridLoc);

        ConveyorTile returnTile;

        int metaX;
        int metaY;
        if (mime.supportsTiling()) {
            metaX = info.getMetaTilingX();
            metaY = info.getMetaTilingY();
        } else {
            metaX = metaY = 1;
        }

        returnTile = getMetatilingReponse(tile, true, metaX, metaY);

        sendTileRequestedEvent(returnTile);

        return returnTile;
    }

    @Override
    public void addLayerListener(final TileLayerListener listener) {
        listeners.addListener(listener);
    }

    @Override
    public boolean removeLayerListener(final TileLayerListener listener) {
        listeners.removeListener(listener);
        return true;
    }

    protected final void sendTileRequestedEvent(ConveyorTile tile) {
        if (listeners != null) {
            listeners.sendTileRequested(this, tile);
        }
    }

    private static final CanonicalSet<GridLocObj> META_GRID_LOCKS = CanonicalSet
            .newInstance(GridLocObj.class);

    private ConveyorTile getMetatilingReponse(ConveyorTile tile, final boolean tryCache,
            final int metaX, final int metaY) throws GeoWebCacheException, IOException {

        final GridSubset gridSubset = getGridSubset(tile.getGridSetId());
        final int zLevel = (int) tile.getTileIndex()[2];
        tile.setMetaTileCacheOnly(!gridSubset.shouldCacheAtZoom(zLevel));

        if (tryCache && tryCacheFetch(tile)) {
            return finalizeTile(tile);
        }

        final GeoServerMetaTile metaTile = createMetaTile(tile, metaX, metaY);
        final GridLocObj metaGridLoc;
        metaGridLoc = META_GRID_LOCKS.unique(new GridLocObj(metaTile.getMetaGridPos(), 32));
        synchronized (metaGridLoc) {
            // got the lock on the meta tile, try again
            if (tryCache && tryCacheFetch(tile)) {
                LOGGER.finest("--> " + Thread.currentThread().getName() + " returns cache hit for "
                        + Arrays.toString(metaTile.getMetaGridPos()));
            } else {
                LOGGER.finer("--> " + Thread.currentThread().getName()
                        + " submitting getMap request for meta grid location "
                        + Arrays.toString(metaTile.getMetaGridPos()) + " on " + metaTile);
                RenderedImageMap map;
                try {
                    map = dispatchGetMap(tile, metaTile);
                    checkNotNull(map, "Did not obtain a WebMap from GeoServer's Dispatcher");
                    metaTile.setWebMap(map);
                    saveTiles(metaTile, tile);
                } catch (Exception e) {
                    throw new GeoWebCacheException("Problem communicating with GeoServer", e);
                } finally {
                    META_GRID_LOCKS.remove(metaGridLoc);
                    metaTile.dispose();
                }
            }
        }

        return finalizeTile(tile);
    }

    private RenderedImageMap dispatchGetMap(final ConveyorTile tile, final MetaTile metaTile)
            throws Exception {

        Map<String, String> params = buildGetMap(tile, metaTile);
        WebMap map;
        try {
            HttpServletRequest actualRequest = tile.servletReq;
            Cookie[] cookies = actualRequest == null ? null : actualRequest.getCookies();

            GWC.get().dispatchOwsRequest(params, cookies);
            map = WEB_MAP.get();
            if (!(map instanceof RenderedImageMap)) {
                throw new IllegalStateException("Expected: RenderedImageMap, got " + map);
            }
        } finally {
            WEB_MAP.remove();
        }

        return (RenderedImageMap) map;
    }

    private GeoServerMetaTile createMetaTile(ConveyorTile tile, final int metaX, final int metaY) {
        GeoServerMetaTile metaTile;

        String tileGridSetId = tile.getGridSetId();
        GridSubset gridSubset = getGridSubset(tileGridSetId);
        MimeType responseFormat = tile.getMimeType();
        FormatModifier formatModifier = null;
        long[] tileGridPosition = tile.getTileIndex();
        int gutter = info.getGutter();
        metaTile = new GeoServerMetaTile(gridSubset, responseFormat, formatModifier,
                tileGridPosition, metaX, metaY, gutter);

        return metaTile;
    }

    private Map<String, String> buildGetMap(final ConveyorTile tile, final MetaTile metaTile)
            throws ParameterException {

        Map<String, String> params = new HashMap<String, String>();

        final MimeType mimeType = tile.getMimeType();
        final String gridSetId = tile.getGridSetId();
        final GridSubset gridSubset = getGridSubset(gridSetId);

        int width = metaTile.getMetaTileWidth();
        int height = metaTile.getMetaTileHeight();
        String srs = gridSubset.getSRS().toString();
        String format = mimeType.getFormat();
        BoundingBox bbox = metaTile.getMetaTileBounds();

        params.put("SERVICE", "WMS");
        params.put("VERSION", "1.1.1");
        params.put("REQUEST", "GetMap");
        params.put("LAYERS", getName());
        params.put("SRS", srs);
        params.put("FORMAT", format);
        params.put("WIDTH", String.valueOf(width));
        params.put("HEIGHT", String.valueOf(height));
        params.put("BBOX", bbox.toString());

        params.put("EXCEPTIONS", GetMapRequest.SE_XML);
        params.put("STYLES", "");
        params.put("TRANSPARENT", "true");
        params.put(GWC_SEED_INTERCEPT_TOKEN, "true");

        Map<String, String> filteredParams = tile.getFullParameters();
        if (filteredParams.isEmpty()) {
            filteredParams = getDefaultParameterFilters();
        }
        params.putAll(filteredParams);

        return params;
    }

    private boolean tryCacheFetch(ConveyorTile tile) {
        int expireCache = this.getExpireCache((int) tile.getTileIndex()[2]);
        if (expireCache != GWCVars.CACHE_DISABLE_CACHE) {
            try {
                return tile.retrieve(expireCache * 1000L);
            } catch (GeoWebCacheException gwce) {
                LOGGER.info(gwce.getMessage());
                tile.setErrorMsg(gwce.getMessage());
                return false;
            }
        }
        return false;
    }

    private ConveyorTile finalizeTile(ConveyorTile tile) {
        if (tile.getStatus() == 0 && !tile.getError()) {
            tile.setStatus(200);
        }

        if (tile.servletResp != null) {
            setExpirationHeader(tile.servletResp, (int) tile.getTileIndex()[2]);
            setTileIndexHeader(tile);
        }

        tile.setTileLayer(this);
        return tile;
    }

    /**
     * @param tile
     */
    private void setTileIndexHeader(ConveyorTile tile) {
        tile.servletResp.addHeader("geowebcache-tile-index", Arrays.toString(tile.getTileIndex()));
    }

    @Override
    public ConveyorTile getNoncachedTile(ConveyorTile tile) throws GeoWebCacheException {
        try {
            return getMetatilingReponse(tile, false, 1, 1);
        } catch (IOException e) {
            throw new GeoWebCacheException(e);
        }
    }

    @Override
    public ConveyorTile doNonMetatilingRequest(ConveyorTile tile) throws GeoWebCacheException {
        try {
            return getMetatilingReponse(tile, true, 1, 1);
        } catch (IOException e) {
            throw new GeoWebCacheException(e);
        }
    }

    @Override
    public void seedTile(ConveyorTile tile, boolean tryCache) throws GeoWebCacheException,
            IOException {

        // Ignore a seed call on a tile that's outside the cached grid levels range
        final GridSubset gridSubset = getGridSubset(tile.getGridSetId());
        final int zLevel = (int) tile.getTileIndex()[2];
        if (!gridSubset.shouldCacheAtZoom(zLevel)) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest("Ignoring seed call on tile " + tile
                        + " as it's outside the cacheable zoom level range");
            }
            return;
        }

        int metaX = info.getMetaTilingX();
        int metaY = info.getMetaTilingY();
        if (!tile.getMimeType().supportsTiling()) {
            metaX = metaY = 1;
        }
        getMetatilingReponse(tile, tryCache, metaX, metaY);
    }

    @Override
    public void acquireLayerLock() {
        // throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void releaseLayerLock() {
        // throw new UnsupportedOperationException("not implemented yet");
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getGridSubsets()
     */
    @Override
    public synchronized Set<String> getGridSubsets() {
        checkGridSubsets();
        return new HashSet<String>(subSets.keySet());
    }

    @Override
    public GridSubset getGridSubset(final String gridSetId) {
        checkGridSubsets();
        return subSets.get(gridSetId);
    }

    private synchronized void checkGridSubsets() {
        if (this.subSets == null) {
            ReferencedEnvelope latLongBbox = getLatLonBbox();
            try {
                this.subSets = getGrids(latLongBbox, gridSetBroker);
            } catch (ConfigurationException e) {
                String msg = "Can't create grids for '" + getName() + "': " + e.getMessage();
                LOGGER.log(Level.WARNING, msg, e);
                setConfigErrorMessage(msg);
            }
        }
    }

    @Override
    public synchronized GridSubset removeGridSubset(String gridSetId) {
        checkGridSubsets();
        final GridSubset oldValue = this.subSets.remove(gridSetId);

        Set<XMLGridSubset> gridSubsets = new HashSet<XMLGridSubset>(info.getGridSubsets());
        for (Iterator<XMLGridSubset> it = gridSubsets.iterator(); it.hasNext();) {
            if (it.next().getGridSetName().equals(gridSetId)) {
                it.remove();
                break;
            }
        }
        info.setGridSubsets(gridSubsets);
        return oldValue;
    }

    @Override
    public void addGridSubset(GridSubset gridSubset) {
        XMLGridSubset gridSubsetInfo = new XMLGridSubset(gridSubset);
        Set<XMLGridSubset> gridSubsets = new HashSet<XMLGridSubset>(info.getGridSubsets());
        gridSubsets.add(gridSubsetInfo);
        info.setGridSubsets(gridSubsets);
        this.subSets = null;
    }

    private Map<String, GridSubset> getGrids(final ReferencedEnvelope latLonBbox,
            final GridSetBroker gridSetBroker) throws ConfigurationException {

        Set<XMLGridSubset> cachedGridSets = info.getGridSubsets();
        if (cachedGridSets.size() == 0) {
            return Collections.emptyMap();
        }

        Map<String, GridSubset> grids = new HashMap<String, GridSubset>(2);
        for (XMLGridSubset gridSubset : cachedGridSets) {
            final String gridSetId = gridSubset.getGridSetName();
            final GridSet gridSet = gridSetBroker.get(gridSetId);
            if (gridSet == null) {
                LOGGER.info("No GWC GridSet named '" + gridSetId + "' exists.");
                continue;
            }
            BoundingBox extent = gridSubset.getExtent();
            if (null == extent) {
                try {
                    SRS srs = gridSet.getSrs();
                    try {
                        extent = getBounds(srs);
                    } catch (RuntimeException cantComputeBounds) {
                        final String msg = "Can't compute bounds for tile layer " + getName()
                                + " in CRS " + srs + ". Assuming full GridSet bounds. ("
                                + cantComputeBounds.getMessage() + ")";
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(Level.FINE, msg, cantComputeBounds);
                        } else {
                            LOGGER.warning(msg);
                        }
                        extent = gridSet.getBounds();
                    }

                    BoundingBox maxBounds = gridSet.getBounds();
                    BoundingBox intersection = maxBounds.intersection(extent);
                    extent = intersection;
                } catch (RuntimeException e) {
                    LOGGER.log(Level.WARNING,
                            "Error computing layer bounds, assuming whole GridSet bounds", e);
                    extent = gridSet.getOriginalExtent();
                }
            }
            gridSubset.setExtent(extent);

            GridSubset gridSubSet = gridSubset.getGridSubSet(gridSetBroker);

            grids.put(gridSetId, gridSubSet);
        }

        return grids;
    }

    private BoundingBox getBounds(final SRS srs) {

        CoordinateReferenceSystem targetCrs;
        try {
            final String epsgCode = srs.toString();
            final boolean longitudeFirst = true;
            targetCrs = CRS.decode(epsgCode, longitudeFirst);
            checkNotNull(targetCrs);
        } catch (Exception e) {
            throw propagate(e);
        }

        ReferencedEnvelope nativeBounds;
        if (getLayerInfo() != null) {
            // projection policy for these bounds are already taken care of by the geoserver
            // configuration
            nativeBounds = getLayerInfo().getResource().getNativeBoundingBox();
        } else {
            nativeBounds = getLayerGroupInfo().getBounds();
        }
        checkState(nativeBounds != null, getName(), " has no native bounds set");

        Envelope transformedBounds;
        // try reprojecting directly
        try {
            transformedBounds = nativeBounds.transform(targetCrs, true, 10000);
        } catch (Exception e) {
            // no luck, try the expensive way
            final Geometry targetAov = GWC.getAreaOfValidityAsGeometry(targetCrs, gridSetBroker);
            if (null == targetAov) {
                String msg = "Can't compute tile layer bouds out of resource native bounds for CRS "
                        + srs;
                LOGGER.log(Level.WARNING, msg, e);
                throw new IllegalArgumentException(msg, e);
            }
            LOGGER.log(Level.FINE, "Can't compute tile layer bouds out of resource "
                    + "native bounds for CRS " + srs, e);

            final CoordinateReferenceSystem nativeCrs = nativeBounds.getCoordinateReferenceSystem();

            try {

                ReferencedEnvelope targetAovBounds = new ReferencedEnvelope(
                        targetAov.getEnvelopeInternal(), targetCrs);
                // transform target AOV in target CRS to native CRS
                ReferencedEnvelope targetAovInNativeCrs = targetAovBounds.transform(nativeCrs,
                        true, 10000);
                // get the intersection between the target aov in native crs and native layer bounds
                Envelope intersection = targetAovInNativeCrs.intersection(nativeBounds);
                ReferencedEnvelope clipped = new ReferencedEnvelope(intersection, nativeCrs);

                // transform covered area in native crs to target crs
                transformedBounds = clipped.transform(targetCrs, true, 10000);
            } catch (Exception e1) {
                throw propagate(e1);
            }
        }

        BoundingBox targetBbox = new BoundingBox(transformedBounds.getMinX(),
                transformedBounds.getMinY(), transformedBounds.getMaxX(),
                transformedBounds.getMaxY());
        return targetBbox;
    }

    public GeoServerTileLayerInfo getInfo() {
        return info;
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getUpdateSources()
     */
    @Override
    public List<UpdateSourceDefinition> getUpdateSources() {
        return Collections.emptyList();
    }

    /**
     * @see org.geowebcache.layer.TileLayer#useETags()
     */
    @Override
    public boolean useETags() {
        return false;
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getFormatModifiers()
     */
    @Override
    public List<FormatModifier> getFormatModifiers() {
        return Collections.emptyList();
    }

    /**
     * @see org.geowebcache.layer.TileLayer#setFormatModifiers(java.util.List)
     */
    @Override
    public void setFormatModifiers(List<FormatModifier> formatModifiers) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getMetaTilingFactors()
     */
    @Override
    public int[] getMetaTilingFactors() {
        return new int[] { info.getMetaTilingX(), info.getMetaTilingY() };
    }

    /**
     * @return {@code true}
     * @see #getNoncachedTile(ConveyorTile)
     * @see org.geowebcache.layer.TileLayer#isCacheBypassAllowed()
     */
    @Override
    public Boolean isCacheBypassAllowed() {
        return true;
    }

    /**
     * @throws UnsupportedOperationException
     * @see org.geowebcache.layer.TileLayer#setCacheBypassAllowed(boolean)
     */
    @Override
    public void setCacheBypassAllowed(boolean allowed) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return {@code 0}
     * @see org.geowebcache.layer.TileLayer#getBackendTimeout()
     */
    @Override
    public Integer getBackendTimeout() {
        return Integer.valueOf(0);
    }

    /**
     * @throws UnsupportedOperationException
     * @see org.geowebcache.layer.TileLayer#setBackendTimeout(int)
     */
    @Override
    public void setBackendTimeout(int seconds) {
        throw new UnsupportedOperationException();
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getMimeTypes()
     */
    @Override
    public List<MimeType> getMimeTypes() {
        Set<String> mimeFormats = info.getMimeFormats();
        List<MimeType> mimeTypes = new ArrayList<MimeType>(mimeFormats.size());
        for (String format : mimeFormats) {
            try {
                mimeTypes.add(MimeType.createFromFormat(format));
            } catch (MimeException e) {
                LOGGER.log(Level.WARNING, "Can't create MimeType from format " + format, e);
            }
        }
        return mimeTypes;
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getExpireClients(int)
     */
    @Override
    public int getExpireClients(int zoomLevel) {
        // TODO: make configurable
        return 0;
    }

    /**
     * @see org.geowebcache.layer.TileLayer#getExpireCache(int)
     */
    @Override
    public int getExpireCache(int zoomLevel) {
        // TODO: make configurable
        return 0;
    }

    /**
     * @return {@code null}, no request filters supported so far
     * @see org.geowebcache.layer.TileLayer#getRequestFilters()
     */
    @Override
    public List<RequestFilter> getRequestFilters() {
        return null;
    }

    /**
     * Empty method, returns {@code true}, initialization is dynamic for this class.
     * 
     * @see org.geowebcache.layer.TileLayer#initialize(org.geowebcache.grid.GridSetBroker)
     */
    @Override
    public boolean initialize(final GridSetBroker gridSetBroker) {
        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder(getClass().getSimpleName()).append("[").append(info).append("]")
                .toString();
    }
}
