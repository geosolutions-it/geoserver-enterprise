/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.measure.unit.Unit;
import javax.media.jai.PlanarImage;

import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.data.util.CoverageStoreUtils;
import org.geoserver.data.util.CoverageUtils;
import org.geoserver.ows.util.OwsUtils;
import org.geotools.coverage.Category;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.FeatureSource;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.Layer;
import org.geotools.data.wms.WebMapServer;
import org.geotools.factory.GeoTools;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.resources.image.ImageUtilities;
import org.geotools.util.Version;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.Format;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.metadata.Identifier;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Builder class which provides convenience methods for interacting with the catalog.
 * <p>
 * Warning: this class is stateful, and is not meant to be accessed by multiple threads and should
 * not be an member variable of another class.
 * </p>
 * 
 * @author Justin Deoliveira, OpenGEO
 * 
 */
public class CatalogBuilder {

    static final Logger LOGGER = Logging.getLogger(CatalogBuilder.class);

    /**
     * the catalog
     */
    Catalog catalog;

    /**
     * the current workspace
     */
    WorkspaceInfo workspace;

    /**
     * the current store
     */
    StoreInfo store;

    public CatalogBuilder(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Sets the workspace to be used when creating store objects.
     */
    public void setWorkspace(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }

    /**
     * Sets the store to be used when creating resource objects.
     */
    public void setStore(StoreInfo store) {
        this.store = store;
    }

    /**
     * Updates a workspace with the properties of another.
     * 
     * @param original
     *            The workspace being updated.
     * @param update
     *            The workspace containing the new values.
     */
    public void updateWorkspace(WorkspaceInfo original, WorkspaceInfo update) {
        update(original, update, WorkspaceInfo.class);
    }

    /**
     * Updates a namespace with the properties of another.
     * 
     * @param original
     *            The namespace being updated.
     * @param update
     *            The namespace containing the new values.
     */
    public void updateNamespace(NamespaceInfo original, NamespaceInfo update) {
        update(original, update, NamespaceInfo.class);
    }

    /**
     * Updates a datastore with the properties of another.
     * 
     * @param original
     *            The datastore being updated.
     * @param update
     *            The datastore containing the new values.
     */
    public void updateDataStore(DataStoreInfo original, DataStoreInfo update) {
        update(original, update, DataStoreInfo.class);
    }

    /**
     * Updates a wms store with the properties of another.
     * 
     * @param original
     *            The wms store being updated.
     * @param update
     *            The wms store containing the new values.
     */
    public void updateWMSStore(WMSStoreInfo original, WMSStoreInfo update) {
        update(original, update, WMSStoreInfo.class);
    }

    /**
     * Updates a coveragestore with the properties of another.
     * 
     * @param original
     *            The coveragestore being updated.
     * @param update
     *            The coveragestore containing the new values.
     */
    public void updateCoverageStore(CoverageStoreInfo original, CoverageStoreInfo update) {
        update(original, update, CoverageStoreInfo.class);
    }

    /**
     * Updates a feature type with the properties of another.
     * 
     * @param original
     *            The feature type being updated.
     * @param update
     *            The feature type containing the new values.
     */
    public void updateFeatureType(FeatureTypeInfo original, FeatureTypeInfo update) {
        update(original, update, FeatureTypeInfo.class);
    }

    /**
     * Updates a coverage with the properties of another.
     * 
     * @param original
     *            The coverage being updated.
     * @param update
     *            The coverage containing the new values.
     */
    public void updateCoverage(CoverageInfo original, CoverageInfo update) {
        update(original, update, CoverageInfo.class);
    }

    /**
     * Updates a WMS layer with the properties of another.
     * 
     * @param original
     *            The wms layer being updated.
     * @param update
     *            The wms layer containing the new values.
     */
    public void updateWMSLayer(WMSLayerInfo original, WMSLayerInfo update) {
        update(original, update, WMSLayerInfo.class);
    }

    /**
     * Updates a layer with the properties of another.
     * 
     * @param original
     *            The layer being updated.
     * @param update
     *            The layer containing the new values.
     */
    public void updateLayer(LayerInfo original, LayerInfo update) {
        update(original, update, LayerInfo.class);
    }

    /**
     * Updates a layer group with the properties of another.
     * 
     * @param original
     *            The layer group being updated.
     * @param update
     *            The layer group containing the new values.
     */
    public void updateLayerGroup(LayerGroupInfo original, LayerGroupInfo update) {
        update(original, update, LayerGroupInfo.class);
    }

    /**
     * Updates a style with the properties of another.
     * 
     * @param original
     *            The style being updated.
     * @param update
     *            The style containing the new values.
     */
    public void updateStyle(StyleInfo original, StyleInfo update) {
        update(original, update, StyleInfo.class);
    }

    /**
     * Update method which uses reflection to grab property values from one object and set them on
     * another.
     * <p>
     * Null values from the <tt>update</tt> object are ignored.
     * </p>
     */
    <T> void update(T original, T update, Class<T> clazz) {
        OwsUtils.copy(update, original, clazz);
    }

    /**
     * Builds a new data store.
     */
    public DataStoreInfo buildDataStore(String name) {
        DataStoreInfo info = catalog.getFactory().createDataStore();
        buildStore(info, name);

        return info;
    }

    /**
     * Builds a new coverage store.
     */
    public CoverageStoreInfo buildCoverageStore(String name) {
        CoverageStoreInfo info = catalog.getFactory().createCoverageStore();
        buildStore(info, name);

        return info;
    }

    /**
     * Builds a new WMS store
     */
    public WMSStoreInfo buildWMSStore(String name) throws IOException {
        WMSStoreInfo info = catalog.getFactory().createWebMapServer();
        buildStore(info, name);
        info.setType("WMS");
        info.setMaxConnections(WMSStoreInfoImpl.DEFAULT_MAX_CONNECTIONS);
        info.setConnectTimeout(WMSStoreInfoImpl.DEFAULT_CONNECT_TIMEOUT);
        info.setReadTimeout(WMSStoreInfoImpl.DEFAULT_READ_TIMEOUT);

        return info;
    }

    /**
     * Builds a store.
     * <p>
     * The workspace of the resulting store is {@link #workspace} if set, else the default workspace
     * from the catalog.
     * </p>
     */
    void buildStore(StoreInfo info, String name) {

        info.setName(name);
        info.setEnabled(true);

        // set workspace, falling back on default if none specified
        if (workspace != null) {
            info.setWorkspace(workspace);
        } else {
            info.setWorkspace(catalog.getDefaultWorkspace());
        }
    }

    /**
     * Builds a {@link FeatureTypeInfo} from the current datastore and the specified type name
     * <p>
     * The resulting object is not added to the catalog, it must be done by the calling code after
     * the fact.
     * </p>
     */
    public FeatureTypeInfo buildFeatureType(Name typeName) throws Exception {
        if (store == null || !(store instanceof DataStoreInfo)) {
            throw new IllegalStateException("Data store not set.");
        }

        DataStoreInfo dstore = (DataStoreInfo) store;
        return buildFeatureType(dstore.getDataStore(null).getFeatureSource(typeName));
    }

    /**
     * Builds a feature type from a geotools feature source. The resulting {@link FeatureTypeInfo}
     * will still miss the bounds and might miss the SRS. Use {@link #lookupSRS(FeatureTypeInfo,
     * true)} and {@link #setupBounds(FeatureTypeInfo)} if you want to force them in (and spend time
     * accordingly)
     * <p>
     * The resulting object is not added to the catalog, it must be done by the calling code after
     * the fact.
     * </p>
     */
    public FeatureTypeInfo buildFeatureType(FeatureSource featureSource) {
        if (store == null || !(store instanceof DataStoreInfo)) {
            throw new IllegalStateException("Data store not set.");
        }

        FeatureType featureType = featureSource.getSchema();

        FeatureTypeInfo ftinfo = catalog.getFactory().createFeatureType();
        ftinfo.setStore(store);
        ftinfo.setEnabled(true);

        // naming
        ftinfo.setNativeName(featureType.getName().getLocalPart());
        ftinfo.setName(featureType.getName().getLocalPart());

        WorkspaceInfo workspace = store.getWorkspace();
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspace.getName());
        if (namespace == null) {
            namespace = catalog.getDefaultNamespace();
        }

        ftinfo.setNamespace(namespace);

        CoordinateReferenceSystem crs = featureType.getCoordinateReferenceSystem();
        if (crs == null && featureType.getGeometryDescriptor() != null) {
            crs = featureType.getGeometryDescriptor().getCoordinateReferenceSystem();
        }
        ftinfo.setNativeCRS(crs);

        // srs look and set (by default we just use fast lookup)
        try {
            lookupSRS(ftinfo, false);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "SRS lookup failed", e);
        }
        setupProjectionPolicy(ftinfo);

        // fill in metadata, first check if the datastore itself can provide some metadata for us
        try {
            setupMetadata(ftinfo, featureSource);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Metadata lookup failed", e);
        }
        
        return ftinfo;
    }

    /**
     * Sets the projection policy for a resource based on the following rules:
     * <ul>
     * <li>If getSRS() returns a non null value it is set to {@Link
     * ProjectionPolicy#FORCE_DECLARED}
     * <li>If getSRS() returns a null value it is set to {@link ProjectionPolicy#NONE}
     * </ul>
     * 
     * TODO: make this method smarter, and compare the native crs to figure out if prejection
     * actually needs to be done, and sync it up with setting proj policy on coverage layers.
     */
    public void setupProjectionPolicy(ResourceInfo rinfo) {
        if (rinfo.getSRS() != null) {
            rinfo.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        } else {
            rinfo.setProjectionPolicy(ProjectionPolicy.NONE);
        }
    }

    /**
     * Computes the native bounds for a {@link FeatureTypeInfo} explicitly providing the feature
     *  source.
     * <p>
     * This method calls through to {@link #doSetupBounds(ResourceInfo, Object)}.
     * </p>
     */
    public void setupBounds(FeatureTypeInfo ftinfo, FeatureSource featureSource) throws IOException {
        doSetupBounds(ftinfo, featureSource);
    }

    /**
     * Computes the native bounds for a {@link CoverageInfo} explicitly providing the coverage 
     *  reader.
     * <p>
     * This method calls through to {@link #doSetupBounds(ResourceInfo, Object)}.
     * </p>
     */
    public void setupBounds(CoverageInfo cinfo, AbstractGridCoverage2DReader coverageReader) 
        throws IOException {
        doSetupBounds(cinfo, coverageReader);
    }

    /**
     * Given a {@link ResourceInfo} this method:
     * <ul>
     * <li>computes, if missing, the native bounds (warning, this might be very expensive, cases in
     * which this case take minutes are not uncommon if the data set is made of million of features)
     * </li>
     * <li>updates, if possible, the geographic bounds accordingly by re-projecting the native
     * bounds into WGS84</li>
     * 
     * @param ftinfo
     * @throws IOException
     *             if computing the native bounds fails or if a transformation error occurs during
     *             the geographic bounds computation
     */
    public void setupBounds(ResourceInfo rinfo) throws IOException {
        doSetupBounds(rinfo, null);
    }

    /*
     * Helper method for setupBounds() methods which can optionally take a "data" object rather
     * than access it through the catalog. This allows for this method to be called for info objects
     * that might not be part of the catalog.
     */
    void doSetupBounds(ResourceInfo rinfo, Object data) throws IOException {
        // setup the native bbox if needed
        if (rinfo.getNativeBoundingBox() == null) {
            ReferencedEnvelope bounds = getNativeBounds(rinfo, data);
            rinfo.setNativeBoundingBox(bounds);
        }

        // setup the geographic bbox if missing and we have enough info
        rinfo.setLatLonBoundingBox(getLatLonBounds(rinfo.getNativeBoundingBox(), rinfo.getCRS()));
    }

    /**
     * Fills in metadata on the {@link FeatureTypeInfo} from an underlying feature source.
     */
    public void setupMetadata(FeatureTypeInfo ftinfo, FeatureSource featureSource) 
        throws IOException {

        org.geotools.data.ResourceInfo rinfo = null;
        try {
            rinfo = featureSource.getInfo();
        }
        catch(Exception ignore) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Unable to get resource info from feature source", ignore);
            }
        }

        if (ftinfo.getTitle() == null) {
            ftinfo.setTitle(rinfo != null ? rinfo.getTitle() : ftinfo.getName());
        }
        if (rinfo != null && ftinfo.getDescription() == null) {
            ftinfo.setDescription(rinfo.getDescription());
        }
        if (rinfo != null && (ftinfo.getKeywords() == null || ftinfo.getKeywords().isEmpty())) {
            if (rinfo.getKeywords() != null) {
                if (ftinfo.getKeywords() == null) {
                    ((FeatureTypeInfoImpl)ftinfo).setKeywords(new ArrayList());
                }
                for (String kw : rinfo.getKeywords()) {
                    if (kw == null || "".equals(kw.trim())) {
                        LOGGER.fine("Empty keyword ignored");
                        continue;
                    }
                    ftinfo.getKeywords().add(new Keyword(kw));
                }
            }
        }
    }

    /**
     * Computes the geographic bounds of a {@link ResourceInfo} by reprojecting the available native
     * bounds
     * 
     * @param rinfo
     * @return the geographic bounds, or null if the native bounds are not available
     * @throws IOException
     */
    public ReferencedEnvelope getLatLonBounds(ReferencedEnvelope nativeBounds,
            CoordinateReferenceSystem declaredCRS) throws IOException {
        if (nativeBounds != null && declaredCRS != null) {
            // make sure we use the declared CRS, not the native one, the may differ
            if (!CRS.equalsIgnoreMetadata(DefaultGeographicCRS.WGS84, declaredCRS)) {
                // transform
                try {
                    ReferencedEnvelope bounds = new ReferencedEnvelope(nativeBounds, declaredCRS);
                    return bounds.transform(DefaultGeographicCRS.WGS84, true);
                } catch (Exception e) {
                    throw (IOException) new IOException("transform error").initCause(e);
                }
            } else {
                return new ReferencedEnvelope(nativeBounds, DefaultGeographicCRS.WGS84);
            }
        }
        return null;
    }

    /**
     * Computes the native bounds of a {@link ResourceInfo} taking into account the nature of the
     * data and the reprojection policy in act
     * 
     * @param rinfo
     * @return the native bounds, or null if the could not be computed
     * @throws IOException
     */
    public ReferencedEnvelope getNativeBounds(ResourceInfo rinfo) throws IOException {
        return getNativeBounds(rinfo, null);
    }

    /*
     * Helper method for getNativeBounds() methods which can optionally take a "data" object rather
     * than access it through the catalog. This allows for this method to be called for info objects
     * that might not be part of the catalog.
     */
    ReferencedEnvelope getNativeBounds(ResourceInfo rinfo, Object data) throws IOException {
        ReferencedEnvelope bounds = null;
        if (rinfo instanceof FeatureTypeInfo) {
            FeatureTypeInfo ftinfo = (FeatureTypeInfo) rinfo;

            // bounds
            if (data instanceof FeatureSource) {
                bounds = ((FeatureSource)data).getBounds();
            }
            else {
                bounds = ftinfo.getFeatureSource(null, null).getBounds();
            }

            // fix the native bounds if necessary, some datastores do
            // not build a proper referenced envelope
            CoordinateReferenceSystem crs = ftinfo.getNativeCRS();
            if (bounds != null && bounds.getCoordinateReferenceSystem() == null && crs != null) {
                bounds = new ReferencedEnvelope(bounds, crs);
            }

            if (bounds != null) {
                // expansion factor if the bounds are empty or one dimensional
                double expandBy = 1; // 1 meter
                if (bounds.getCoordinateReferenceSystem() instanceof GeographicCRS) {
                    expandBy = 0.0001;
                }
                if (bounds.getWidth() == 0 || bounds.getHeight() == 0) {
                    bounds.expandBy(expandBy);
                }
            }

        } else if (rinfo instanceof CoverageInfo) {
            // the coverage bounds computation path is a bit more linear, the
            // readers always return the bounds and in the proper CRS (afaik)
            CoverageInfo cinfo = (CoverageInfo) rinfo;            
            AbstractGridCoverage2DReader reader = null;
            if (data instanceof AbstractGridCoverage2DReader) {
                reader = (AbstractGridCoverage2DReader) data;
            }
            else {
                reader = (AbstractGridCoverage2DReader) 
                    cinfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
            }

            // get  bounds
            bounds = new ReferencedEnvelope(reader.getOriginalEnvelope());
           
        }

        // apply the bounds, taking into account the reprojection policy if need be
        if (rinfo.getProjectionPolicy() == ProjectionPolicy.REPROJECT_TO_DECLARED && bounds != null) {
            try {
                bounds = bounds.transform(rinfo.getCRS(), true);
            } catch (Exception e) {
                throw (IOException) new IOException("transform error").initCause(e);
            }
        }

        return bounds;
    }

    /**
     * Looks up and sets the SRS based on the feature type info native
     * {@link CoordinateReferenceSystem}
     * 
     * @param ftinfo
     * @param extensive
     *            if true an extenstive lookup will be performed (more accurate, but might take
     *            various seconds)
     * @throws IOException
     */
    public void lookupSRS(FeatureTypeInfo ftinfo, boolean extensive) throws IOException {
        lookupSRS(ftinfo, null, extensive);
    }

    /**
     * Looks up and sets the SRS based on the feature type info native 
     * {@link CoordinateReferenceSystem}, obtained from an optional feature source.
     * 
     * @param ftinfo
     * @param data A feature source (possibily null)
     * @param extensive
     *            if true an extenstive lookup will be performed (more accurate, but might take
     *            various seconds)
     * @throws IOException
     */
    public void lookupSRS(FeatureTypeInfo ftinfo, FeatureSource data, boolean extensive) 
            throws IOException {
        CoordinateReferenceSystem crs = ftinfo.getNativeCRS();
        if (crs == null) {
            if (data != null) {
                crs = data.getSchema().getCoordinateReferenceSystem();
            }
            else {
                crs = ftinfo.getFeatureType().getCoordinateReferenceSystem();
            }
        }
        if (crs != null) {
            try {
                Integer code = CRS.lookupEpsgCode(crs, extensive);
                if (code != null)
                    ftinfo.setSRS("EPSG:" + code);
            } catch (FactoryException e) {
                throw (IOException) new IOException().initCause(e);
            }
        }
    }

    /**
     * Initializes basic resource info.
     */
    private void initResourceInfo(ResourceInfo resInfo) throws Exception {
    	// set the name
    	if (resInfo.getNativeName() == null && resInfo.getName() != null) {
    		resInfo.setNativeName(resInfo.getName());
    	}
    	if (resInfo.getNativeName() != null && resInfo.getName() == null) {
    		resInfo.setName(resInfo.getNativeName());
    	}
    }

    /**
     * Initializes a feature type object setting any info that has not been set.
     */
    public void initFeatureType(FeatureTypeInfo featureType) throws Exception {
        if (featureType.getCatalog() == null) {
            featureType.setCatalog(catalog);
        }

        initResourceInfo(featureType);

        // setup the srs if missing
        if (featureType.getSRS() == null) {
            lookupSRS(featureType, true);
        }
        if (featureType.getProjectionPolicy() == null) {
            setupProjectionPolicy(featureType);
        }

        // deal with bounding boxes as possible
        CoordinateReferenceSystem crs = featureType.getCRS();
        if (featureType.getLatLonBoundingBox() == null
                && featureType.getNativeBoundingBox() == null) {
            // both missing, we compute them
            setupBounds(featureType);
        } else if (featureType.getLatLonBoundingBox() == null) {
            // native available but geographic to be computed
            setupBounds(featureType);
        } else if (featureType.getNativeBoundingBox() == null && crs != null) {
            // we know the geographic and we can reproject back to native
            ReferencedEnvelope boundsLatLon = featureType.getLatLonBoundingBox();
            featureType.setNativeBoundingBox(boundsLatLon.transform(crs, true));
        }
    }

    /**
     * Initializes a wms layer object setting any info that has not been set.
     */
    public void initWMSLayer(WMSLayerInfo wmsLayer) throws Exception {
        wmsLayer.setCatalog(catalog);

        initResourceInfo(wmsLayer);
        OwsUtils.resolveCollections(wmsLayer);

        // get a fully initialized version we can copy from
        WMSLayerInfo full = buildWMSLayer(wmsLayer.getNativeName());

        // setup the srs if missing
        if (wmsLayer.getSRS() == null) {
            wmsLayer.setSRS(full.getSRS());
        }
        if (wmsLayer.getNativeCRS() == null) {
            wmsLayer.setNativeCRS(full.getNativeCRS());
        }
        if (wmsLayer.getProjectionPolicy() == null) {
            wmsLayer.setProjectionPolicy(full.getProjectionPolicy());
        }

        // deal with bounding boxes as possible
        if (wmsLayer.getLatLonBoundingBox() == null
                && wmsLayer.getNativeBoundingBox() == null) {
            // both missing, we copy them
            wmsLayer.setLatLonBoundingBox(full.getLatLonBoundingBox());
            wmsLayer.setNativeBoundingBox(full.getNativeBoundingBox());
        } else if (wmsLayer.getLatLonBoundingBox() == null) {
            // native available but geographic to be computed
            setupBounds(wmsLayer);
        } else if (wmsLayer.getNativeBoundingBox() == null && wmsLayer.getNativeCRS() != null) {
            // we know the geographic and we can reproject back to native
            ReferencedEnvelope boundsLatLon = wmsLayer.getLatLonBoundingBox();
            wmsLayer.setNativeBoundingBox(boundsLatLon.transform(wmsLayer.getNativeCRS(), true));
        }

        //fill in missing metadata
        if (wmsLayer.getTitle() == null) {
            wmsLayer.setTitle(full.getTitle());
        }
        if (wmsLayer.getDescription() == null) {
            wmsLayer.setDescription(full.getDescription());
        }
        if (wmsLayer.getAbstract() == null) {
            wmsLayer.setAbstract(full.getAbstract());
        }
        if (wmsLayer.getKeywords().isEmpty()) {
            wmsLayer.getKeywords().addAll(full.getKeywords());
        }
    }

    /**
     * Initialize a coverage object and set any unset info.
     */
    public void initCoverage(CoverageInfo cinfo) throws Exception {
    	CoverageStoreInfo csinfo = (CoverageStoreInfo) store;
        AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) catalog
            	.getResourcePool().getGridCoverageReader(csinfo, GeoTools.getDefaultHints());

        initResourceInfo(cinfo);

        if (reader == null)
            throw new Exception("Unable to acquire a reader for this coverage with format: "
                    + csinfo.getFormat().getName());

        if (cinfo.getNativeCRS() == null) {
        	cinfo.setNativeCRS(reader.getCrs());
        }

        CoordinateReferenceSystem nativeCRS = cinfo.getNativeCRS();

        if (cinfo.getSRS() == null) {
        	cinfo.setSRS(nativeCRS.getIdentifiers().toArray()[0].toString());
        }

        if (cinfo.getProjectionPolicy() == null) {
            if (nativeCRS != null && !nativeCRS.getIdentifiers().isEmpty()) {
                cinfo.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
            }
            if (nativeCRS == null) {
                cinfo.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
            }
        }

    	if (cinfo.getLatLonBoundingBox() == null
    			&& cinfo.getNativeBoundingBox() == null) {
    		GeneralEnvelope envelope = reader.getOriginalEnvelope();

    		cinfo.setNativeBoundingBox(new ReferencedEnvelope(envelope));
    		cinfo.setLatLonBoundingBox(new ReferencedEnvelope(CoverageStoreUtils.getWGS84LonLatEnvelope(envelope)));
    	} else if (cinfo.getLatLonBoundingBox() == null) {
    		setupBounds(cinfo);
    	} else if (cinfo.getNativeBoundingBox() == null && cinfo.getNativeCRS() != null) {
    		ReferencedEnvelope boundsLatLon = cinfo.getLatLonBoundingBox();
    		cinfo.setNativeBoundingBox(boundsLatLon.transform(cinfo.getNativeCRS(), true));
    	}

        if (cinfo.getGrid() == null) {
            GridEnvelope originalRange = reader.getOriginalGridRange();
            cinfo.setGrid(new GridGeometry2D(originalRange, reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER), nativeCRS));
        }
    }

    /**
     * Builds the default coverage contained in the current store
     * 
     * @return
     * @throws Exception
     */
    public CoverageInfo buildCoverage() throws Exception {
        if (store == null || !(store instanceof CoverageStoreInfo)) {
            throw new IllegalStateException("Coverage store not set.");
        }

        CoverageStoreInfo csinfo = (CoverageStoreInfo) store;
        AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) catalog
                .getResourcePool().getGridCoverageReader(csinfo, GeoTools.getDefaultHints());

        if (reader == null)
            throw new Exception("Unable to acquire a reader for this coverage with format: "
                    + csinfo.getFormat().getName());

        return buildCoverage(reader, null);
    }

    /**
     * Builds a coverage from a geotools grid coverage reader.
     * @param customParameters 
     */
    public CoverageInfo buildCoverage(AbstractGridCoverage2DReader reader, Map customParameters) throws Exception {
        if (store == null || !(store instanceof CoverageStoreInfo)) {
            throw new IllegalStateException("Coverage store not set.");
        }

        CoverageStoreInfo csinfo = (CoverageStoreInfo) store;
        CoverageInfo cinfo = catalog.getFactory().createCoverage();

        cinfo.setStore(csinfo);
        cinfo.setEnabled(true);

        WorkspaceInfo workspace = store.getWorkspace();
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspace.getName());
        if (namespace == null) {
            namespace = catalog.getDefaultNamespace();
        }
        cinfo.setNamespace(namespace);

        CoordinateReferenceSystem nativeCRS = reader.getCrs();
        cinfo.setNativeCRS(nativeCRS);

        // mind the default projection policy, Coverages do not have a flexible
        // handling as feature types, they do reproject if the native srs is set,
        // force if missing
        if (nativeCRS != null && !nativeCRS.getIdentifiers().isEmpty()) {
            cinfo.setSRS(nativeCRS.getIdentifiers().toArray()[0].toString());
            cinfo.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
        }
        if (nativeCRS == null) {
            cinfo.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
        }

        GeneralEnvelope envelope = reader.getOriginalEnvelope();
        cinfo.setNativeBoundingBox(new ReferencedEnvelope(envelope));
        cinfo.setLatLonBoundingBox(new ReferencedEnvelope(CoverageStoreUtils.getWGS84LonLatEnvelope(envelope)));

        GridEnvelope originalRange = reader.getOriginalGridRange();
        cinfo.setGrid(new GridGeometry2D(originalRange, reader.getOriginalGridToWorld(PixelInCell.CELL_CENTER), nativeCRS));

        // /////////////////////////////////////////////////////////////////////
        //
        // Now reading a fake small GridCoverage just to retrieve meta
        // information about bands:
        //
        // - calculating a new envelope which is just 5x5 pixels
        // - if it's a mosaic, limit the number of tiles we're going to read to one 
        //   (with time and elevation there might be hundreds of superimposed tiles)
        // - reading the GridCoverage subset
        //
        // /////////////////////////////////////////////////////////////////////
        Format format = csinfo.getFormat();
        final GridCoverage2D gc;

        final ParameterValueGroup readParams = format.getReadParameters();
        final Map parameters = CoverageUtils.getParametersKVP(readParams);
        final int minX = originalRange.getLow(0);
        final int minY = originalRange.getLow(1);
        final int width = originalRange.getSpan(0);
        final int height = originalRange.getSpan(1);
        final int maxX = minX + (width <= 5 ? width : 5);
        final int maxY = minY + (height <= 5 ? height : 5);

        // we have to be sure that we are working against a valid grid range.
        final GridEnvelope2D testRange = new GridEnvelope2D(minX, minY, maxX, maxY);

        // build the corresponding envelope
        final MathTransform gridToWorldCorner = reader.getOriginalGridToWorld(PixelInCell.CELL_CORNER);
        final GeneralEnvelope testEnvelope = CRS.transform(gridToWorldCorner, new GeneralEnvelope(testRange.getBounds()));
        testEnvelope.setCoordinateReferenceSystem(nativeCRS);

        if (customParameters != null) {
        	parameters.putAll(customParameters);
        }
        
        // make sure mosaics with many superimposed tiles won't blow up with 
        // a "too many open files" exception
        String maxAllowedTiles = ImageMosaicFormat.MAX_ALLOWED_TILES.getName().toString();
        if(parameters.keySet().contains(maxAllowedTiles)) {
            parameters.put(maxAllowedTiles, 1);
        }
        
        parameters.put(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString(), new GridGeometry2D(testRange, testEnvelope));

        // try to read this coverage
        gc = (GridCoverage2D) reader.read(CoverageUtils.getParameters(readParams, parameters, true));
        if (gc == null) {
            throw new Exception("Unable to acquire test coverage for format:" + format.getName());
        }

        // remove read grid geometry since it is request specific
        parameters.remove(AbstractGridFormat.READ_GRIDGEOMETRY2D.getName().toString());

        cinfo.getDimensions().addAll(getCoverageDimensions(gc.getSampleDimensions()));
        // TODO:
        // dimentionNames = getDimensionNames(gc);
        /*
         * StringBuilder cvName =null; int count = 0; while (true) { final StringBuilder key = new
         * StringBuilder(gc.getName().toString()); if (count > 0) { key.append("_").append(count); }
         * 
         * Map coverages = dataConfig.getCoverages(); Set cvKeySet = coverages.keySet(); boolean
         * key_exists = false;
         * 
         * for (Iterator it = cvKeySet.iterator(); it.hasNext();) { String cvKey = ((String)
         * it.next()).toLowerCase(); if (cvKey.endsWith(key.toString().toLowerCase())) { key_exists
         * = true; } }
         * 
         * if (!key_exists) { cvName = key; break; } else { count++; } }
         * 
         * String name = cvName.toString();
         */
        String name = gc.getName().toString();
        cinfo.setName(name);
        cinfo.setNativeName(name);
        cinfo.setTitle(name);
        cinfo.setDescription(new StringBuilder("Generated from ").append(format.getName()).toString());

        // keywords
        cinfo.getKeywords().add(new Keyword("WCS"));
        cinfo.getKeywords().add(new Keyword(format.getName()));
        cinfo.getKeywords().add(new Keyword(name));

        // native format name
        cinfo.setNativeFormat(format.getName());
        cinfo.getMetadata().put("dirName", new StringBuilder(store.getName()).append("_").append(name).toString());

        // request SRS's
        if ((gc.getCoordinateReferenceSystem2D().getIdentifiers() != null)
                && !gc.getCoordinateReferenceSystem2D().getIdentifiers().isEmpty()) {
            cinfo.getRequestSRS().add(((Identifier) gc.getCoordinateReferenceSystem2D().getIdentifiers().toArray()[0]).toString());
        }

        // response SRS's
        if ((gc.getCoordinateReferenceSystem2D().getIdentifiers() != null)
                && !gc.getCoordinateReferenceSystem2D().getIdentifiers().isEmpty()) {
            cinfo.getResponseSRS().add(((Identifier) gc.getCoordinateReferenceSystem2D().getIdentifiers().toArray()[0]).toString());
        }

        // supported formats
        final List formats = CoverageStoreUtils.listDataFormats();
        for (Iterator i = formats.iterator(); i.hasNext();) {
            final Format fTmp = (Format) i.next();
            final String fName = fTmp.getName();

            if (fName.equalsIgnoreCase("WorldImage")) {
                // TODO check if coverage can encode Format
                cinfo.getSupportedFormats().add("GIF");
                cinfo.getSupportedFormats().add("PNG");
                cinfo.getSupportedFormats().add("JPEG");
                cinfo.getSupportedFormats().add("TIFF");
            } else if (fName.toLowerCase().startsWith("geotiff")) {
                // TODO check if coverage can encode Format
                cinfo.getSupportedFormats().add("GEOTIFF");
            } else {
                // TODO check if coverage can encode Format
                cinfo.getSupportedFormats().add(fName);
            }
        }

        // interpolation methods
        cinfo.setDefaultInterpolationMethod("nearest neighbor");
        cinfo.getInterpolationMethods().add("nearest neighbor");
        cinfo.getInterpolationMethods().add("bilinear");
        cinfo.getInterpolationMethods().add("bicubic");

        // read parameters (get the params again since we altered the map to optimize the 
        // coverage read)
        cinfo.getParameters().putAll(CoverageUtils.getParametersKVP(readParams));

        /// dispose coverage 
        gc.dispose(true);
        if(gc.getRenderedImage() instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) gc.getRenderedImage());
        }

        return cinfo;
    }

    List<CoverageDimensionInfo> getCoverageDimensions(GridSampleDimension[] sampleDimensions) {

        final int length = sampleDimensions.length;
        List<CoverageDimensionInfo> dims = new ArrayList<CoverageDimensionInfo>();

        for (int i = 0; i < length; i++) {
            CoverageDimensionInfo dim = catalog.getFactory().createCoverageDimension();
            dim.setName(sampleDimensions[i].getDescription().toString(Locale.getDefault()));

            StringBuilder label = new StringBuilder("GridSampleDimension".intern());
            final Unit uom = sampleDimensions[i].getUnits();

            if (uom != null) {
                label.append("(".intern());
                parseUOM(label, uom);
                label.append(")".intern());
            }

            label.append("[".intern());
            label.append(sampleDimensions[i].getMinimumValue());
            label.append(",".intern());
            label.append(sampleDimensions[i].getMaximumValue());
            label.append("]".intern());

            dim.setDescription(label.toString());
            dim.setRange(sampleDimensions[i].getRange());

            final List<Category> categories = sampleDimensions[i].getCategories();
            if (categories != null) {
                for (Category cat : categories) {

                    if ((cat != null) && cat.getName().toString().equalsIgnoreCase("no data")) {
                        double min = cat.getRange().getMinimum();
                        double max = cat.getRange().getMaximum();

                        dim.getNullValues().add(min);
                        if (min != max) {
                            dim.getNullValues().add(max);
                        }
                    }
                }
            }

            dims.add(dim);
        }

        return dims;
    }

    public WMSLayerInfo buildWMSLayer(String layerName) throws IOException {
        if (store == null || !(store instanceof WMSStoreInfo)) {
            throw new IllegalStateException("WMS store not set.");
        }

        WMSStoreInfo wms = (WMSStoreInfo) store;
        WMSLayerInfo wli = catalog.getFactory().createWMSLayer();

        wli.setName(layerName);
        wli.setNativeName(layerName);

        wli.setStore(store);
        wli.setEnabled(true);

        WorkspaceInfo workspace = store.getWorkspace();
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(workspace.getName());
        if (namespace == null) {
            namespace = catalog.getDefaultNamespace();
        }
        wli.setNamespace(namespace);

        Layer layer = wli.getWMSLayer(null);

        // try to get the native SRS -> we use the bounding boxes, GeoServer will publish all of the
        // supported SRS in the root, if we use getSRS() we'll get them all
        for (String srs : layer.getBoundingBoxes().keySet()) {
            try {
                CoordinateReferenceSystem crs = CRS.decode(srs);
                wli.setSRS(srs);
                wli.setNativeCRS(crs);
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Skipping " + srs
                        + " definition, it was not recognized by the referencing subsystem");
            }
        }
        
        // fall back on WGS84 if necessary, and handle well known WMS CRS codes
        String srs = wli.getSRS();
        try {
            if (srs == null || srs.equals("CRS:84")) {
                wli.setSRS("EPSG:4326");
                srs = "EPSG:4326";
                wli.setNativeCRS(CRS.decode("EPSG:4326"));
            } else if(srs.equals("CRS:83")) {
                wli.setSRS("EPSG:4269");
                srs = "EPSG:4269";
                wli.setNativeCRS(CRS.decode("EPSG:4269"));
            } else if(srs.equals("CRS:27")) {
                wli.setSRS("EPSG:4267");
                srs = "EPSG:4267";
                wli.setNativeCRS(CRS.decode("EPSG:4267"));
            }
        } catch(Exception e) {
            throw (IOException) new IOException("Failed to compute the layer declared SRS code").initCause(e);
        }
        wli.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);

        // try to grab the envelope
        GeneralEnvelope envelope = layer.getEnvelope(wli.getNativeCRS());
        if (envelope != null) {
            ReferencedEnvelope re = new ReferencedEnvelope(envelope.getMinimum(0), envelope
                    .getMaximum(0), envelope.getMinimum(1), envelope.getMaximum(1), wli
                    .getNativeCRS());
            // are we in crazy wms 1.3 land?
            WebMapServer mapServer = wms.getWebMapServer(null);
            Version version = new Version(mapServer.getCapabilities().getVersion());
            if(axisFlipped(version, srs)) {
                // flip axis, the wms code won't actually use the crs
                double minx = re.getMinX();
                double miny = re.getMinY();
                double maxx = re.getMaxX();
                double maxy = re.getMaxY();
                re = new ReferencedEnvelope(miny, maxy, minx, maxx, wli.getNativeCRS());
            }
            wli.setNativeBoundingBox(re);
        }
        CRSEnvelope llbbox = layer.getLatLonBoundingBox();
        if (llbbox != null) {
            ReferencedEnvelope re = new ReferencedEnvelope(llbbox.getMinX(), llbbox.getMaxX(),
                    llbbox.getMinY(), llbbox.getMaxY(), DefaultGeographicCRS.WGS84);
            wli.setLatLonBoundingBox(re);
        } else if (wli.getNativeBoundingBox() != null) {
            try {
                wli.setLatLonBoundingBox(wli.getNativeBoundingBox().transform(
                        DefaultGeographicCRS.WGS84, true));
            } catch (Exception e) {
                LOGGER.log(Level.INFO, "Could not transform native bbox into a lat/lon one", e);
            }
        }

        // reflect all the metadata that we can grab
        wli.setAbstract(layer.get_abstract());
        wli.setDescription(layer.get_abstract());
        wli.setTitle(layer.getTitle());
        if (layer.getKeywords() != null) {
            for (String kw : layer.getKeywords()) {
                if(kw != null){
                    wli.getKeywords().add(new Keyword(kw));
                }
            }
        }

        // strip off the prefix if we're cascading from a server that does add them
        String published = wli.getName();
        if (published.contains(":")) {
            wli.setName(published.substring(published.lastIndexOf(':') + 1));
        }

        return wli;
    }
    
    private boolean axisFlipped(Version version, String srsName) {
        if(version.compareTo(new Version("1.3.0")) < 0) {
            // aah, sheer simplicity
            return false;
        } else {
            // gah, hell gates breaking loose
            if(srsName.startsWith("EPSG:")) {
                try {
                    String epsgNative =  "urn:x-ogc:def:crs:EPSG:".concat(srsName.substring(5));
                    return CRS.getAxisOrder(CRS.decode(epsgNative)) == AxisOrder.NORTH_EAST;
                } catch(Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to determine axis order for " 
                            + srsName + ", assuming east/north", e);
                    return false;
                }
            } else {
                // CRS or AUTO, none of them is flipped so far
                return false;
            }
        }
    }

    void parseUOM(StringBuilder label, Unit uom) {
        String uomString = uom.toString();
        uomString = uomString.replaceAll("�", "^2");
        uomString = uomString.replaceAll("�", "^3");
        uomString = uomString.replaceAll("�", "A");
        uomString = uomString.replaceAll("�", "");
        label.append(uomString);
    }

    /**
     * Builds a layer for a feature type.
     * <p>
     * The resulting object is not added to the catalog, it must be done by the calling code after
     * the fact.
     * </p>
     */
    public LayerInfo buildLayer(FeatureTypeInfo featureType) throws IOException {
        // also create a layer for the feautre type
        LayerInfo layer = buildLayer((ResourceInfo) featureType);

        StyleInfo style = getDefaultStyle(featureType);
        layer.setDefaultStyle(style);

        return layer;
    }

    /**
     * Builds a layer for a coverage.
     * <p>
     * The resulting object is not added to the catalog, it must be done by the calling code after
     * the fact.
     * </p>
     */
    public LayerInfo buildLayer(CoverageInfo coverage) throws IOException {
        LayerInfo layer = buildLayer((ResourceInfo) coverage);

        layer.setDefaultStyle(getDefaultStyle(coverage));

        return layer;
    }

    /**
     * Builds a layer wrapping a WMS layer resource
     * <p>
     * The resulting object is not added to the catalog, it must be done by the calling code after
     * the fact.
     * </p>
     */
    public LayerInfo buildLayer(WMSLayerInfo wms) throws IOException {
        LayerInfo layer = buildLayer((ResourceInfo) wms);
        
        layer.setDefaultStyle(getDefaultStyle(wms));
        
        return layer;
    }

    /**
     * Returns the default style for the specified resource, or null if the layer is vector and
     * geometryless
     * 
     * @param resource
     * @return
     * @throws IOException
     */
    public StyleInfo getDefaultStyle(ResourceInfo resource) throws IOException {
        // raster wise, only one style
        if (resource instanceof CoverageInfo || resource instanceof WMSLayerInfo)
            return catalog.getStyleByName(StyleInfo.DEFAULT_RASTER);

        // for vectors we depend on the the nature of the default geometry
        String styleName;
        FeatureTypeInfo featureType = (FeatureTypeInfo) resource;
        if (featureType.getFeatureType() == null) {
            return null;
        }
        GeometryDescriptor gd = featureType.getFeatureType().getGeometryDescriptor();
        if (gd == null) {
            return null;
        }

        Class gtype = gd.getType().getBinding();
        if (Point.class.isAssignableFrom(gtype) || MultiPoint.class.isAssignableFrom(gtype)) {
            styleName = StyleInfo.DEFAULT_POINT;
        } else if (LineString.class.isAssignableFrom(gtype)
                || MultiLineString.class.isAssignableFrom(gtype)) {
            styleName = StyleInfo.DEFAULT_LINE;
        } else if (Polygon.class.isAssignableFrom(gtype)
                || MultiPolygon.class.isAssignableFrom(gtype)) {
            styleName = StyleInfo.DEFAULT_POLYGON;
        } else {
            // fall back to point
            styleName = StyleInfo.DEFAULT_POINT;
        }

        return catalog.getStyleByName(styleName);
    }

    public LayerInfo buildLayer(ResourceInfo resource) {
        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(resource);
        layer.setName(resource.getName());
        layer.setEnabled(true);

        // setup the layer type
        if (layer.getResource() instanceof FeatureTypeInfo) {
            layer.setType(LayerInfo.Type.VECTOR);
        } else if (layer.getResource() instanceof CoverageInfo) {
            layer.setType(LayerInfo.Type.RASTER);
        } else if (layer.getResource() instanceof WMSLayerInfo) {
            layer.setType(LayerInfo.Type.WMS);
        }

        return layer;
    }

    /**
     * Calculates the bounds of a layer group specifying a particular crs.
     */
    public void calculateLayerGroupBounds(LayerGroupInfo lg, CoordinateReferenceSystem crs)
            throws Exception {

        if (lg.getLayers().isEmpty()) {
            return;
        }

        LayerInfo l = lg.getLayers().get(0);
        ReferencedEnvelope bounds = transform(l.getResource().getLatLonBoundingBox(), crs);

        for (int i = 1; i < lg.getLayers().size(); i++) {
            l = lg.getLayers().get(i);
            bounds.expandToInclude(transform(l.getResource().getLatLonBoundingBox(), crs));
        }
        lg.setBounds(bounds);
    }

    /**
     * Calculates the bounds of a layer group by aggregating the bounds of each layer. TODO: move
     * this method to a utility class, it should not be on a builder.
     */
    public void calculateLayerGroupBounds(LayerGroupInfo lg) throws Exception {
        if (lg.getLayers().isEmpty()) {
            return;
        }

        LayerInfo l = lg.getLayers().get(0);
        ReferencedEnvelope bounds = l.getResource().boundingBox();
        boolean latlon = false;
        if (bounds == null) {
            bounds = l.getResource().getLatLonBoundingBox();
            latlon = true;
        }

        if (bounds == null) {
            throw new IllegalArgumentException(
                    "Could not calculate bounds from layer with no bounds, " + l.getName());
        }

        for (int i = 1; i < lg.getLayers().size(); i++) {
            l = lg.getLayers().get(i);

            ReferencedEnvelope re;
            if (latlon) {
                re = l.getResource().getLatLonBoundingBox();
            } else {
                re = l.getResource().boundingBox();
            }

            re = transform(re, bounds.getCoordinateReferenceSystem());
            if (re == null) {
                throw new IllegalArgumentException(
                        "Could not calculate bounds from layer with no bounds, " + l.getName());
            }
            bounds.expandToInclude(re);
        }

        lg.setBounds(bounds);
    }

    /**
     * Helper method for transforming an envelope.
     */
    ReferencedEnvelope transform(ReferencedEnvelope e, CoordinateReferenceSystem crs)
            throws TransformException, FactoryException {
        if (!CRS.equalsIgnoreMetadata(crs, e.getCoordinateReferenceSystem())) {
            return e.transform(crs, true);
        }
        return e;
    }

    //
    // remove methods
    //

    /**
     * Removes a workspace from the catalog.
     * <p>
     * The <tt>recursive</tt> flag controls whether objects linked to the workspace such as stores
     * should also be deleted.
     * </p>
     */
    public void removeWorkspace(WorkspaceInfo workspace, boolean recursive) {
        if (recursive) {
            workspace.accept(new CascadeDeleteVisitor(catalog));
        } else {
            catalog.remove(workspace);
        }
    }

    /**
     * Removes a store from the catalog.
     * <p>
     * The <tt>recursive</tt> flag controls whether objects linked to the store such as resources
     * should also be deleted.
     * </p>
     */
    public void removeStore(StoreInfo store, boolean recursive) {
        if (recursive) {
            store.accept(new CascadeDeleteVisitor(catalog));
        } else {
            catalog.remove(store);
        }
    }

    /**
     * Removes a resource from the catalog.
     * <p>
     * The <tt>recursive</tt> flag controls whether objects linked to the resource such as layers
     * should also be deleted.
     * </p>
     */
    public void removeResource(ResourceInfo resource, boolean recursive) {
        if (recursive) {
            resource.accept(new CascadeDeleteVisitor(catalog));
        } else {
            catalog.remove(resource);
        }
    }

    /**
     * Reattaches a serialized {@link StoreInfo} to the catalog
     */
    public void attach(StoreInfo storeInfo) {
        storeInfo = ModificationProxy.unwrap(storeInfo);
        ((StoreInfoImpl) storeInfo).setCatalog(catalog);
    }

    /**
     * Reattaches a serialized {@link ResourceInfo} to the catalog
     */
    public void attach(ResourceInfo resourceInfo) {
        resourceInfo = ModificationProxy.unwrap(resourceInfo);
        ((ResourceInfoImpl) resourceInfo).setCatalog(catalog);
    }

    /**
     * Reattaches a serialized {@link LayerInfo} to the catalog
     */
    public void attach(LayerInfo layerInfo) {
        attach(layerInfo.getResource());
    }

    /**
     * Reattaches a serialized {@link MapInfo} to the catalog
     */
    public void attach(MapInfo mapInfo) {
        // hmmm... mapInfo has a list of layers inside? Not names?
        for (LayerInfo layer : mapInfo.getLayers()) {
            attach(layer);
        }
    }

    /**
     * Reattaches a serialized {@link LayerGroupInfo} to the catalog
     */
    public void attach(LayerGroupInfo groupInfo) {
        for (LayerInfo layer : groupInfo.getLayers()) {
            attach(layer);
        }
        for (StyleInfo style : groupInfo.getStyles()) {
            if (style != null)
                attach(style);
        }
    }

    /**
     * Reattaches a serialized {@link StyleInfo} to the catalog
     */
    public void attach(StyleInfo styleInfo) {
        styleInfo = ModificationProxy.unwrap(styleInfo);
        ((StyleInfoImpl) styleInfo).setCatalog(catalog);
    }

    /**
     * Reattaches a serialized {@link NamespaceInfo} to the catalog
     */
    public void attach(NamespaceInfo nsInfo) {
        // nothing to do
    }

    /**
     * Reattaches a serialized {@link WorkspaceInfo} to the catalog
     */
    public void attach(WorkspaceInfo wsInfo) {
        // nothing to do
    }
}
