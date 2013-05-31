/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.geoserver.gwc.GWC.tileLayerName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geotools.util.logging.Logging;
import org.geowebcache.filter.parameters.StringParameterFilter;
import org.geowebcache.grid.GridSetBroker;
import org.geowebcache.storage.StorageBroker;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Listens to {@link Catalog} layers added/removed events and adds/removes
 * {@link GeoServerTileLayer}s to/from the {@link CatalogConfiguration}
 * <p>
 * Handles the following cases:
 * <ul>
 * <li><b>Layer added</b>: a {@link LayerInfo} or {@link LayerGroupInfo} has been added. A
 * {@link GeoServerTileLayer} is {@link CatalogConfiguration#createLayer is created} with the
 * {@link GWCConfig default settings} only if the integrated GWC configuration is set to
 * {@link GWCConfig#isCacheLayersByDefault() cache layers by default}.</li>
 * <li><b>Layer removed</b>: a {@code LayerInfo} or {@code LayerGroupInfo} has been removed. GWC is
 * instructed to remove the layer, deleting it's cache and any other associated information
 * completely (for example, the disk quota information for the layer is also deleted and the global
 * usage updated accordingly).
 * <li><b>Layer renamed</b>: a {@link LayerInfo} or {@link LayerGroupInfo} has been renamed. GWC is
 * {@link StorageBroker#rename instructed to rename} the corresponding tile layer preserving the
 * cache and any other information (usage statistics, disk quota usage, etc).</li>
 * <li><b>Namespace changed</b>: a {@link ResourceInfo} has been assigned to a different
 * {@link NamespaceInfo namespace}. As the GWC tile layers are named after the resource's
 * {@link ResourceInfo#prefixedName() prefixed name} and not only after the
 * {@link LayerInfo#getName()} (at least until GeoServer separates out data from publication - the
 * famous data/publish split), GWC is instructed to rename the layer preserving the cache and any
 * other information for the layer.</li>
 * <li><b>LayerGroupInfo modified</b>: either the {@link LayerGroupInfo#getLayers() layers} or
 * {@link LayerGroupInfo#getStyles() styles} changed for a {@code LayerGroupInfo}. It's cache is
 * truncated.</li>
 * <li><b>LayerInfo default style replaced</b>: a {@code LayerInfo} has been assigned a different
 * {@link LayerInfo#getDefaultStyle() default style}. The corresponding tile layer's cache is
 * truncated for the default style.</li>
 * <li><b>LayerInfo alternate styles changed</b> the set of a {@code LayerInfo}'s
 * {@link LayerInfo#getStyles() alternate styles} has been modified. For any added style, if the
 * {@link GeoServerTileLayer} is configured to {@link GeoServerTileLayerInfo#isAutoCacheStyles()
 * automatically cache all styles}, the style name is added to the set of
 * {@link GeoServerTileLayerInfo#cachedStyles() cached styles}. For any <b>removed</b> style, if it
 * was one of the {@link GeoServerTileLayerInfo#cachedStyles() cached styles}, the layer's cache for
 * that style is truncated, and it's removed from the tile layer's set of cached styles.
 * Subsequently, the {@link GeoServerTileLayer} will create a {@link StringParameterFilter "STYLES"
 * parameter filter} for all the cached styles on demand</li>
 * </ul>
 * </p>
 * 
 * @author Arne Kepp
 * @author Gabriel Roldan
 */
public class CatalogLayerEventListener implements CatalogListener {

    private static Logger log = Logging.getLogger(CatalogLayerEventListener.class);

    private final GWC mediator;

    /**
     * Holds the CatalogModifyEvent from {@link #handleModifyEvent} to be taken after the change was
     * applied to the {@link Catalog} at {@link #handlePostModifyEvent} and check whether it is
     * necessary to perform any action on the cache based on the changed properties
     */
    private static ThreadLocal<CatalogModifyEvent> PRE_MODIFY_EVENT = new ThreadLocal<CatalogModifyEvent>();

    private static ThreadLocal<GeoServerTileLayerInfo> PRE_MODIFY_TILELAYER = new ThreadLocal<GeoServerTileLayerInfo>();

    public CatalogLayerEventListener(final GWC mediator) {
        this.mediator = mediator;
    }

    /**
     * If either a {@link LayerInfo} or {@link LayerGroupInfo} has been added to the {@link Catalog}
     * , create a corresponding GWC TileLayer depending on the value of
     * {@link GWCConfig#isCacheLayersByDefault()}.
     * 
     * @see org.geoserver.catalog.event.CatalogListener#handleAddEvent
     * @see GWC#createLayer(LayerInfo)
     * @see GWC#createLayer(LayerGroupInfo)
     */
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        GWCConfig config = mediator.getConfig();
        boolean sane = config.isSane();
        boolean cacheLayersByDefault = config.isCacheLayersByDefault();
        if (!cacheLayersByDefault) {
            return;
        }
        if (!sane) {
            log.info("Ignoring auto-creation of tile layer for " + event.getSource()
                    + ": global gwc settings are not sane");
        }
        Object obj = event.getSource();
        // We only handle layers here. Layer groups are initially empty
        if (obj instanceof LayerInfo) {
            log.finer("Handling add event: " + obj);
            LayerInfo layerInfo = (LayerInfo) obj;
            createTileLayer(layerInfo);
        } else if (obj instanceof LayerGroupInfo) {
            LayerGroupInfo lgi = (LayerGroupInfo) obj;
            createTileLayer(lgi);
        }
    }

    /**
     * LayerInfo has been created, add a matching {@link GeoServerTileLayer}
     * 
     * @see CatalogLayerEventListener#handleAddEvent
     * @see GWC#add(GeoServerTileLayer)
     */
    void createTileLayer(final LayerInfo layerInfo) {
        GWCConfig defaults = mediator.getConfig();
        if (defaults.isSane() && defaults.isCacheLayersByDefault()) {
            GridSetBroker gridSetBroker = mediator.getGridSetBroker();
            GeoServerTileLayer tileLayer = new GeoServerTileLayer(layerInfo, defaults,
                    gridSetBroker);
            mediator.add(tileLayer);
        }
    }

    /**
     * LayerGroupInfo has been created, add a matching {@link GeoServerTileLayer}
     * 
     * @see CatalogLayerEventListener#handleAddEvent
     * @see GWC#add(GeoServerTileLayer)
     */
    public void createTileLayer(LayerGroupInfo lgi) {
        GWCConfig defaults = mediator.getConfig();
        GridSetBroker gridSetBroker = mediator.getGridSetBroker();
        GeoServerTileLayer tileLayer = new GeoServerTileLayer(lgi, defaults, gridSetBroker);
        mediator.add(tileLayer);
    }

    /**
     * @see org.geoserver.catalog.event.CatalogListener#handleModifyEvent(org.geoserver.catalog.event.CatalogModifyEvent)
     * @see #handlePostModifyEvent
     */
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        CatalogInfo source = event.getSource();
        if (source instanceof LayerInfo || source instanceof LayerGroupInfo
                || source instanceof FeatureTypeInfo || source instanceof CoverageInfo
                || source instanceof WMSLayerInfo) {
            PRE_MODIFY_EVENT.set(event);

            if (mediator.hasTileLayer(source)) {
                try {
                    GeoServerTileLayer tileLayer = mediator.getTileLayer(source);
                    GeoServerTileLayerInfo tileLayerInfo = tileLayer.getInfo();
                    PRE_MODIFY_TILELAYER.set(tileLayerInfo);
                } catch (RuntimeException e) {
                    log.info("Ignoring misconfigured tile layer info for " + source);
                }
            }
        }
    }

    /**
     * In case the event refers to the addition or removal of a {@link LayerInfo} or
     * {@link LayerGroupInfo} adds or removes the corresponding {@link GeoServerTileLayer} through
     * {@link GWC#createLayer}.
     * <p>
     * Note this method does not discriminate whether the change in the layer or layergroup deserves
     * a change in its matching TileLayer, it just re-creates the TileLayer
     * </p>
     * 
     * @see org.geoserver.catalog.event.CatalogListener#handlePostModifyEvent(org.geoserver.catalog.event.CatalogPostModifyEvent)
     */
    public void handlePostModifyEvent(final CatalogPostModifyEvent event) throws CatalogException {
        final CatalogInfo source = event.getSource();
        if (!(source instanceof LayerInfo || source instanceof LayerGroupInfo
                || source instanceof FeatureTypeInfo || source instanceof CoverageInfo || source instanceof WMSLayerInfo)) {
            return;
        }

        final GeoServerTileLayerInfo tileLayerInfo = PRE_MODIFY_TILELAYER.get();
        PRE_MODIFY_TILELAYER.remove();

        final CatalogModifyEvent preModifyEvent = PRE_MODIFY_EVENT.get();
        PRE_MODIFY_EVENT.remove();

        if (tileLayerInfo == null) {
            return;// no tile layer assiociated, no need to continue
        }
        if (preModifyEvent == null) {
            throw new IllegalStateException(
                    "PostModifyEvent called without having called handlePreModify first?");
        }

        final List<String> changedProperties = preModifyEvent.getPropertyNames();
        final List<Object> oldValues = preModifyEvent.getOldValues();
        final List<Object> newValues = preModifyEvent.getNewValues();

        log.finer("Handling modify event for " + source);
        if (source instanceof FeatureTypeInfo || source instanceof CoverageInfo
                || source instanceof WMSLayerInfo || source instanceof LayerGroupInfo) {
            /*
             * Handle the rename case. For LayerInfos it's actually the related ResourceInfo what
             * gets renamed, at least until the data/publish split is implemented in GeoServer. For
             * LayerGroupInfo it's either the group name itself or its workspace
             */
            if (changedProperties.contains("name") || changedProperties.contains("namespace")
                    || changedProperties.contains("workspace")) {
                handleRename(tileLayerInfo, source, changedProperties, oldValues, newValues);
            }
        }

        if (source instanceof LayerInfo) {
            final LayerInfo li = (LayerInfo) source;

            handleLayerInfoChange(changedProperties, oldValues, newValues, li, tileLayerInfo);

        } else if (source instanceof LayerGroupInfo) {
            LayerGroupInfo lgInfo = (LayerGroupInfo) source;
            handleLayerGroupInfoChange(changedProperties, oldValues, newValues, lgInfo,
                    tileLayerInfo);
        }
    }

    private void handleLayerGroupInfoChange(final List<String> changedProperties,
            final List<Object> oldValues, final List<Object> newValues,
            final LayerGroupInfo lgInfo, final GeoServerTileLayerInfo tileLayerInfo) {

        checkNotNull(lgInfo);
        checkNotNull(tileLayerInfo);

        final String layerName = tileLayerName(lgInfo);

        boolean truncate = false;
        if (changedProperties.contains("layers")) {
            final int layersIndex = changedProperties.indexOf("layers");
            Object oldLayers = oldValues.get(layersIndex);
            Object newLayers = newValues.get(layersIndex);
            truncate = !oldLayers.equals(newLayers);
        }

        if (!truncate && changedProperties.contains("styles")) {
            final int stylesIndex = changedProperties.indexOf("styles");
            Object oldStyles = oldValues.get(stylesIndex);
            Object newStyles = newValues.get(stylesIndex);
            truncate = !oldStyles.equals(newStyles);
        }
        if (truncate) {
            log.info("Truncating TileLayer for layer group '" + layerName
                    + "' due to a change in its layers or styles");
            mediator.truncate(layerName);
        }
    }

    /**
     * Handles changes of interest to GWC on a {@link LayerInfo}.
     * <ul>
     * <li>If the name of the default style changed, then the layer's cache for the default style is
     * truncated. This method doesn't check if the contents of the styles are equal. That is handled
     * by {@link CatalogStyleChangeListener} whenever a style is modified.
     * <li>If the tile layer is {@link GeoServerTileLayerInfo#isAutoCacheStyles() auto caching
     * styles} and the layerinfo's "styles" list changed, the tile layer's STYLE parameter filter is
     * updated to match the actual list of layer styles and any removed style is truncated.
     * </ul>
     * 
     * @param changedProperties
     * @param oldValues
     * @param newValues
     * @param li
     * @param tileLayerInfo
     */
    private void handleLayerInfoChange(final List<String> changedProperties,
            final List<Object> oldValues, final List<Object> newValues, final LayerInfo li,
            final GeoServerTileLayerInfo tileLayerInfo) {

        checkNotNull(tileLayerInfo);

        final String layerName = tileLayerName(li);

        boolean save = false;

        final String defaultStyle;

        /*
         * If default style name changed
         */
        if (changedProperties.contains("defaultStyle")) {
            final int propIndex = changedProperties.indexOf("defaultStyle");
            final StyleInfo oldStyle = (StyleInfo) oldValues.get(propIndex);
            final StyleInfo newStyle = (StyleInfo) newValues.get(propIndex);

            final String oldStyleName = oldStyle.getName();
            defaultStyle = newStyle.getName();
            if (!Objects.equal(oldStyleName, defaultStyle)) {
                save = true;
                log.info("Truncating default style for layer " + layerName
                        + ", as it changed from " + oldStyleName + " to " + defaultStyle);
                mediator.truncateByLayerAndStyle(layerName, oldStyleName);
            }
        } else {
            StyleInfo styleInfo = li.getDefaultStyle();
            defaultStyle = styleInfo == null ? null : styleInfo.getName();
        }

        if (tileLayerInfo.isAutoCacheStyles()) {
            Set<String> styles = new HashSet<String>();
            for (StyleInfo s : li.getStyles()) {
                styles.add(s.getName());
            }
            ImmutableSet<String> cachedStyles = tileLayerInfo.cachedStyles();
            if (!styles.equals(cachedStyles)) {
                // truncate no longer existing cached styles
                Set<String> notCachedAnyMore = Sets.difference(cachedStyles, styles);
                for (String oldCachedStyle : notCachedAnyMore) {
                    log.info("Truncating cached style " + oldCachedStyle + " of layer " + layerName
                            + " as it's no longer one of the layer's styles");
                    mediator.truncateByLayerAndStyle(layerName, oldCachedStyle);
                }
                // reset STYLES parameter filter
                final boolean createParamIfNotExists = true;
                TileLayerInfoUtil.updateStringParameterFilter(tileLayerInfo, "STYLES",
                        createParamIfNotExists, defaultStyle, styles);
                save = true;
            }
        }

        if (save) {
            GridSetBroker gridSetBroker = mediator.getGridSetBroker();
            GeoServerTileLayer tileLayer = new GeoServerTileLayer(li, gridSetBroker, tileLayerInfo);
            mediator.save(tileLayer);
        }
    }

    private void handleRename(final GeoServerTileLayerInfo tileLayerInfo, final CatalogInfo source,
            final List<String> changedProperties, final List<Object> oldValues,
            final List<Object> newValues) {

        final int nameIndex = changedProperties.indexOf("name");
        final int namespaceIndex = changedProperties.indexOf("namespace");

        String oldLayerName;
        String newLayerName;
        if (source instanceof ResourceInfo) {// covers LayerInfo, CoverageInfo, and WMSLayerInfo
            // must cover prefix:name
            final ResourceInfo resourceInfo = (ResourceInfo) source;
            final NamespaceInfo currNamespace = resourceInfo.getNamespace();
            final NamespaceInfo oldNamespace;
            if (namespaceIndex > -1) {
                // namespace changed
                oldNamespace = (NamespaceInfo) oldValues.get(namespaceIndex);
            } else {
                oldNamespace = currNamespace;
            }

            newLayerName = resourceInfo.prefixedName();
            if (nameIndex > -1) {
                oldLayerName = (String) oldValues.get(nameIndex);
            } else {
                oldLayerName = resourceInfo.getName();
            }
            oldLayerName = oldNamespace.getPrefix() + ":" + oldLayerName;
        } else {
            // it's a layer group, no need to worry about namespace
            oldLayerName = tileLayerInfo.getName();
            newLayerName = tileLayerName((LayerGroupInfo) source);
        }

        if (!oldLayerName.equals(newLayerName)) {
            tileLayerInfo.setName(newLayerName);

            // notify the mediator of the rename so it changes the name of the layer in GWC without
            // affecting its caches
            GridSetBroker gridSetBroker = mediator.getGridSetBroker();

            final GeoServerTileLayer oldTileLayer = (GeoServerTileLayer) mediator
                    .getTileLayerByName(oldLayerName);

            checkState(null != oldTileLayer, "hanldeRename: old tile layer not found: '"
                    + oldLayerName + "'. New name: '" + newLayerName + "'");

            final GeoServerTileLayer modifiedTileLayer;

            if (oldTileLayer.getLayerInfo() != null) {
                LayerInfo layerInfo = oldTileLayer.getLayerInfo();
                modifiedTileLayer = new GeoServerTileLayer(layerInfo, gridSetBroker, tileLayerInfo);
            } else {
                LayerGroupInfo layerGroup = oldTileLayer.getLayerGroupInfo();
                modifiedTileLayer = new GeoServerTileLayer(layerGroup, gridSetBroker, tileLayerInfo);
            }
            mediator.save(modifiedTileLayer);
        }
    }

    /**
     * 
     * @see org.geoserver.catalog.event.CatalogListener#handleRemoveEvent(org.geoserver.catalog.event.CatalogRemoveEvent)
     * @see GWC#removeTileLayers(List)
     */
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        CatalogInfo obj = event.getSource();
        if (!(obj instanceof LayerInfo || obj instanceof LayerGroupInfo)) {
            return;
        }
        if (!mediator.hasTileLayer(obj)) {
            return;
        }

        String prefixedName = null;

        if (obj instanceof LayerGroupInfo) {
            LayerGroupInfo lgInfo = (LayerGroupInfo) obj;
            prefixedName = tileLayerName(lgInfo);
        } else if (obj instanceof LayerInfo) {
            LayerInfo layerInfo = (LayerInfo) obj;
            prefixedName = tileLayerName(layerInfo);
        }

        if (null != prefixedName) {
            // notify the layer has been removed
            mediator.removeTileLayers(Arrays.asList(prefixedName));
        }
    }

    /**
     * 
     * @see org.geoserver.catalog.event.CatalogListener#reloaded()
     */
    public void reloaded() {
        //
    }

}
