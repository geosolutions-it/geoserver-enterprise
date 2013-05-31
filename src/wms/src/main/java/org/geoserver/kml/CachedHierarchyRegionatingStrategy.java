/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.kml;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.HttpErrorCodeException;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSMapContent;
import org.geotools.data.FeatureSource;
import org.geotools.data.jdbc.JDBCUtils;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.geotools.util.CanonicalSet;
import org.geotools.util.logging.Logging;
import org.h2.tools.DeleteDbFiles;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.identity.FeatureId;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.vfny.geoserver.global.GeoserverDataDirectory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

/**
 * Base class for regionating strategies. Common functionality provided:
 * <ul>
 * <li>tiling based on the TMS tiling recommendation</li>
 * <li>caching the assignment of a feature in a specific tile in an H2 database
 * stored in the data directory</li>
 * <li>
 * 
 * @author Andrea Aime - OpenGeo
 * @author David Winslow - OpenGeo
 * @author Arne Kepp - OpenGeo
 */
public abstract class CachedHierarchyRegionatingStrategy implements
        RegionatingStrategy {
    static Logger LOGGER = Logging.getLogger("org.geoserver.geosearch");

    static final CoordinateReferenceSystem WGS84;

    static final ReferencedEnvelope WORLD_BOUNDS;

    static final double MAX_TILE_WIDTH;

    static final double MAX_ERROR = 0.02;

    static final Set<String> NO_FIDS = Collections.emptySet();

    /**
     * This structure is used to make sure that multiple threads end up using
     * the same table name object, so that we can use it as a synchonization
     * token
     */
    static CanonicalSet<String> canonicalizer = CanonicalSet
            .newInstance(String.class);

    static {
        try {
            // common geographic info
            WGS84 = CRS.decode("EPSG:4326");
            WORLD_BOUNDS = new ReferencedEnvelope(new Envelope(180.0, -180.0,
                    90.0, -90.0), WGS84);
            MAX_TILE_WIDTH = WORLD_BOUNDS.getWidth() / 2.0;

            // make sure, once and for all, that H2 is around
            Class.forName("org.h2.Driver");
        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not initialize the class constants", e);
        }
    }

 

    /**
     * The original area occupied by the data
     */
    protected ReferencedEnvelope dataEnvelope;

    /**
     * Reference to the layer being regionated
     */
    protected FeatureTypeInfo featureType;

    /**
     * The max number of features per tile
     */
    protected Integer featuresPerTile;

    /**
     * The name of the database that will contain the fid to tile cache
     */
    protected String tableName;

    /**
     * geoserver configuration
     */
    protected GeoServer gs;
    
    protected CachedHierarchyRegionatingStrategy(GeoServer gs) {
        this.gs = gs;
    }
    
    public Filter getFilter(WMSMapContent context, Layer layer) {
        Catalog catalog = gs.getCatalog();
        Set<String> featuresInTile = Collections.emptySet();
        try {
            // grab information needed to reach the db and get a hold to a db
            // connection
            FeatureSource featureSource = layer.getFeatureSource();
            featureType = catalog.getFeatureTypeByName(featureSource.getName());
            
            String dataDir = catalog.getResourceLoader().getBaseDirectory().getCanonicalPath();
            tableName = getDatabaseName(context, layer);

            // grab the features per tile, use a default if user did not
            // provide a decent value. The default should fill up the
            // tile when it shows up.
            featuresPerTile = featureType.getMetadata().get( "kml.regionateFeatureLimit",Integer.class );
            if (featuresPerTile == null || featuresPerTile.intValue() <= 1)
                featuresPerTile = 64;

            // sanity check, the layer is not geometryless
            if (featureType.getFeatureType().getGeometryDescriptor() == null)
                throw new ServiceException(featureType.getName()
                        + " is geometryless, cannot generate KML!");

            // make sure the request is within the data bounds, allowing for a
            // small error
            ReferencedEnvelope requestedEnvelope = context.getRenderingArea().transform(WGS84, true);
            LOGGER.log(Level.FINE, "Requested tile: {0}", requestedEnvelope);
            dataEnvelope = featureType.getLatLonBoundingBox(); 

            // decide which tile we need to load/compute, and make sure
            // it's a valid tile request, that is, that is does fit with
            // the general tiling scheme (minus an eventual small error)
            Tile tile = new Tile(requestedEnvelope);
            ReferencedEnvelope tileEnvelope = tile.getEnvelope();
            if (!envelopeMatch(tileEnvelope, requestedEnvelope))
                throw new ServiceException(
                        "Invalid bounding box request, it does not fit "
                                + "the nearest regionating tile. Requested area: "
                                + requestedEnvelope + ", " + "nearest tile: "
                                + tileEnvelope);

            // oki doki, let's compute the fids in the requested tile
            featuresInTile = getFeaturesForTile(dataDir, tile);
            LOGGER.log(Level.FINE, "Found "+featuresInTile.size() + " features in tile " + tile.toString());
        } catch (Throwable t) {
            LOGGER.log(Level.SEVERE,
                    "Error occurred while pre-processing regionated features",
                    t);
            throw new ServiceException("Failure while pre-processing regionated features");
        }

        // This okay, just means the tile is empty
        if (featuresInTile.size() == 0) {
            throw new HttpErrorCodeException(204);
        } else {
            FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);
            Set<FeatureId> ids = new HashSet<FeatureId>();
            for (String fid : featuresInTile) {
                ids.add(ff.featureId(fid));
            }
            return ff.id(ids);
        }
    }

    public void clearCache(FeatureTypeInfo cfg){
        try{
            DeleteDbFiles.execute(
                GeoserverDataDirectory.findCreateConfigDir("geosearch").getCanonicalPath(),
                "h2cache_" + getDatabaseName(cfg),
                true
                );
        } catch (Exception ioe) {
            LOGGER.severe("Couldn't clear out config dir due to: " + ioe);
        }
    }

    /**
     * Returns true if the two envelope roughly match, that is, they are about
     * the same size and about the same location. The max difference allowed is
     * {@link #MAX_ERROR}, evaluated as a percentage of the width and height of
     * the envelope.
     * The method assumes both envelopes are in the same CRS
     * 
     * @param tileEnvelope
     * @param expectedEnvelope 
     * @return
     */
    private boolean envelopeMatch(ReferencedEnvelope tileEnvelope,
            ReferencedEnvelope expectedEnvelope) {
        double widthRatio = Math.abs(1.0 - tileEnvelope.getWidth()
                / expectedEnvelope.getWidth());
        double heightRatio = Math.abs(1.0 - tileEnvelope.getHeight()
                / expectedEnvelope.getHeight());
        double xRatio = Math.abs((tileEnvelope.getMinX() - expectedEnvelope
                .getMinX())
                / tileEnvelope.getWidth());
        double yRatio = Math.abs((tileEnvelope.getMinY() - expectedEnvelope
                .getMinY())
                / tileEnvelope.getHeight());
        return widthRatio < MAX_ERROR && heightRatio < MAX_ERROR
                && xRatio < MAX_ERROR && yRatio < MAX_ERROR;
    }

    /**
     * Open/creates the db and then reads/computes the tile features
     * 
     * @param dataDir
     * @param tile
     * @return
     * @throws Exception
     */
    private Set<String> getFeaturesForTile(String dataDir, Tile tile)
            throws Exception {
        Connection conn = null;
        Statement st = null;

        // build the synchonization token
        canonicalizer.add(tableName);
        tableName = canonicalizer.get(tableName);

        try {
            // make sure no two thread in parallel can build the same db
            synchronized (tableName) {
                // get a hold to the database that contains the cache (this will
                // eventually create the db)
                conn = DriverManager.getConnection("jdbc:h2:file:" + dataDir
                        + "/geosearch/h2cache_" + tableName, "geoserver",
                        "geopass");

                // try to create the table, if it's already there this will fail
                st = conn.createStatement();
                st.execute("CREATE TABLE IF NOT EXISTS TILECACHE( " //
                        + "x BIGINT, " //
                        + "y BIGINT, " //
                        + "z INT, " //
                        + "fid varchar (64))");
                st.execute("CREATE INDEX IF NOT EXISTS IDX_TILECACHE ON TILECACHE(x, y, z)");
            }

            return readFeaturesForTile(tile, conn);
        } finally {
            JDBCUtils.close(st);
            JDBCUtils.close(conn, null, null);
        }
    }

    /**
     * Reads/computes the tile feature set
     * 
     * @param tile
     *            the Tile whose features we must find
     * @param conn
     *            the H2 connection
     * @return
     * @throws Exception
     */
    protected Set<String> readFeaturesForTile(Tile tile, Connection conn)
            throws Exception {
        // grab the fids and decide whether we have to compute them
        Set<String> fids = readCachedTileFids(tile, conn);
        if (fids != null) {
            return fids;
        } else {
            // build the synchronization token
            String tileKey = tableName + tile.x + "-" + tile.y + "-" + tile.z;
            canonicalizer.add(tileKey);
            tileKey = canonicalizer.get(tileKey);

            synchronized (tileKey) {
                // might have been built while we were waiting
                fids = readCachedTileFids(tile, conn);
                if (fids != null)
                    return fids;

                // still missing, we need to compute them
                fids = computeFids(tile, conn);
                storeFids(tile, fids, conn);

                // optimization, if we did not manage to fill up this tile,
                // the ones below it will be empty -> mark them as such right
                // away
                if (fids.size() < featuresPerTile)
                    for (Tile child : tile.getChildren())
                        storeFids(child, NO_FIDS, conn);
            }
        }
        return fids;
    }

    /**
     * Store the fids inside
     * 
     * @param t
     * @param fids
     * @param conn
     * @throws SQLException
     */
    private void storeFids(Tile t, Set<String> fids, Connection conn)
            throws SQLException {
        PreparedStatement ps = null;
        try {
            // we are going to execute this one many times, 
            // let's prepare it so that the db engine does 
            // not have to parse it at every call
            String stmt = "INSERT INTO TILECACHE VALUES (" + t.x + ", " + t.y
                    + ", " + t.z + ", ?)";
            ps = conn.prepareStatement(stmt);

            if (fids.size() == 0) {
                // we just have to mark the tile as empty
                ps.setString(1, null);
                ps.execute();
            } else {
                // store all the fids
                conn.setAutoCommit(false);
                for (String fid : fids) {
                    ps.setString(1, fid);
                    ps.execute();
                }
                conn.commit();
            }
        } finally {
            conn.setAutoCommit(true);
            JDBCUtils.close(ps);
        }
    }

    /**
     * Computes the fids that will be stored in the specified tile
     * 
     * @param tileCoords
     * @param st
     * @return
     * @throws SQLException
     */
    private Set<String> computeFids(Tile tile, Connection conn)
            throws Exception {
        Set<String> parentFids = getUpwardFids(tile.getParent(), conn);
        Set<String> currFids = new HashSet<String>();
        FeatureIterator fi = null;
        try {
            // grab the features
            FeatureSource fs = featureType.getFeatureSource(null,null);
            GeometryDescriptor geom = fs.getSchema().getGeometryDescriptor();
            CoordinateReferenceSystem nativeCrs = geom
                    .getCoordinateReferenceSystem();

            ReferencedEnvelope nativeTileEnvelope = null;

            if (!CRS.equalsIgnoreMetadata(WGS84, nativeCrs)) {
                try {
                    nativeTileEnvelope = tile.getEnvelope().transform(nativeCrs, true);
                } catch (ProjectionException pe) {
                    // the WGS84 envelope of the tile is too big for this project,
                    // let's intersect it with the declared lat/lon bounds then
                    LOGGER.log(Level.INFO, "Could not reproject the current tile bounds " 
                            + tile.getEnvelope() + " to the native SRS, intersecting with " 
                            + "the layer declared lat/lon bounds and retrying");
                    
                    // let's compare against the declared data bounds then
                    ReferencedEnvelope llEnv = featureType.getLatLonBoundingBox();
                    Envelope reduced = tile.getEnvelope().intersection(llEnv);
                    if(reduced.isNull() || reduced.getWidth() == 0 || reduced.getHeight() == 0) {
                        // no overlap, no party, the tile will be empty
                        return Collections.emptySet();
                    }
                    
                    // there is some overlap, let's try the reprojection again.
                    // if even this fails, the user has evidently setup the 
                    // geographics bounds improperly
                    ReferencedEnvelope refRed = new ReferencedEnvelope(reduced, 
                            tile.getEnvelope().getCoordinateReferenceSystem());
                    nativeTileEnvelope = refRed.transform(nativeCrs, true);
                }
            } else {
                nativeTileEnvelope = tile.getEnvelope();
            }

            fi = getSortedFeatures(geom, tile.getEnvelope(), nativeTileEnvelope, conn);

            // if the crs is not wgs84, we'll need to transform the point
            MathTransform tx = null;
            double[] coords = new double[2];

            // scan counting how many fids we've collected
            boolean first = true;
            while (fi.hasNext() && currFids.size() < featuresPerTile) {
                // grab the feature, skip it if it's already in a parent element
                SimpleFeature f = (SimpleFeature) fi.next();
                if (parentFids.contains(f.getID()))
                    continue;

                // check the need for a transformation
                if (first) {
                    first = false;
                    CoordinateReferenceSystem nativeCRS = f.getType()
                            .getCoordinateReferenceSystem();
                    featureType.getFeatureType().getCoordinateReferenceSystem();
                    if (nativeCRS != null
                            && !CRS.equalsIgnoreMetadata(nativeCRS, WGS84)) {
                        tx = CRS.findMathTransform(nativeCRS, WGS84);
                    }
                }

                // see if the features is to be included in this tile
                Point p = ((Geometry) f.getDefaultGeometry()).getCentroid();
                coords[0] = p.getX();
                coords[1] = p.getY();
                if (tx != null)
                    tx.transform(coords, 0, coords, 0, 1);
                if (tile.contains(coords[0], coords[1]))
                    currFids.add(f.getID());
            }
        } finally {
            if (fi != null)
                fi.close();
        }
        return currFids;
    }

    /**
     * Returns all the features in the specified envelope, sorted according to
     * the priority used for regionating. The features returned do not have to
     * be the feature type ones, it's sufficient that they have the same FID and
     * a geometry whose centroid is the same as the original feature one.
     * 
     * @param envelope
     * @param indexConnection
     *            a connection to the feature id cache db
     * @return
     * @throws Exception
     */
    protected abstract FeatureIterator getSortedFeatures(
    		GeometryDescriptor geom, ReferencedEnvelope latLongEnvelope, 
    		ReferencedEnvelope nativeEnvelope, Connection indexConnection)
            throws Exception;

    /**
     * Returns a set of all the fids in the specified tile and in the parents of
     * it, recursing up to the root tile
     * 
     * @param tile
     * @param st
     * @return
     * @throws SQLException
     */
    private Set<String> getUpwardFids(Tile tile, Connection conn)
            throws Exception {
        // recursion stop condition
        if (tile == null)
            return Collections.EMPTY_SET;

        // return the curren tile fids, and recurse up to the parent
        Set<String> fids = new HashSet();
        fids.addAll(readFeaturesForTile(tile, conn));
        fids.addAll(getUpwardFids(tile.getParent(), conn));
        return fids;
    }

    /**
     * Here we have three cases
     * <ul>
     * <li>the tile was already computed, and it resulted to be empty. We leave
     * a "x,y,z,null" marker to know if that happened, and in this case the
     * returned set will be empty</li> <li>the tile was already computed, and we
     * have data, the returned sest will be non empty</li> <li>the tile is new,
     * the db contains nothing, in this case we return "null"</li>
     * <ul>
     * 
     * @param tileCoords
     * @param conn
     * @throws SQLException
     */
    protected Set<String> readCachedTileFids(Tile tile, Connection conn)
            throws SQLException {
        Set<String> fids = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            st = conn.createStatement();
            rs = st.executeQuery("SELECT fid FROM TILECACHE where x = "
                    + tile.x + " AND y = " + tile.y + " and z = " + tile.z);
            // decide whether we have to collect the fids or just to
            // return that the tile was empty
            if (rs.next()) {
                String fid = rs.getString(1);
                if (fid == null) {
                    return Collections.emptySet();
                } else {
                    fids = new HashSet<String>();
                    fids.add(fid);
                }
            }
            // fill the set with the collected fids
            while (rs.next()) {
                fids.add(rs.getString(1));
            }
        } finally {
            JDBCUtils.close(rs);
            JDBCUtils.close(st);
        }

        return fids;
    }

    /**
     * Returns the name to be used for the database. Should be unique for this
     * specific regionated layer.
     * 
     * @param con
     * @param layer
     * @return
     */
    protected String getDatabaseName(WMSMapContent con, Layer layer)
        throws Exception {
            int index =  con.layers().indexOf(layer);
            return getDatabaseName(featureType);
    }

    protected String getDatabaseName(FeatureTypeInfo cfg)
        throws Exception {
            return cfg.getNamespace().getPrefix() + "_" + cfg.getName();
    }

    /**
     * A regionating tile identified by its coordinates
     * 
     * @author Andrea Aime
     */
    protected class Tile {
        long x;

        long y;

        long z;

        ReferencedEnvelope envelope;

        /**
         * Creates a new tile with the given coordinates
         * 
         * @param x
         * @param y
         * @param z
         */
        public Tile(long x, long y, long z) {
            this.x = x;
            this.y = y;
            this.z = z;
            envelope = envelope(x, y, z);
        }

        /**
         * Tile containment check is not trivial due to a couple of issues:
         * <ul>
         * <li>centroids sitting on the tile borders must be associated to exactly one tile,
         *     so we have to consider only two borders as inclusive in general (S and W)
         *     but add on occasion the other two when we reach the extent of our data set</li>
         * <li>coordinates going beyond the natural lat/lon range</li>
         * </ul>
         * This code takes care of the first, whilst the second issue remains as a TODO
         * @param x
         * @param y
         * @return
         */
        public boolean contains(double x, double y) {
            double minx = envelope.getMinX();
            double maxx = envelope.getMaxX();
            double miny = envelope.getMinY();
            double maxy = envelope.getMaxY();
            // standard borders, N and W in, E and S out
            if(x >= minx && x < maxx && y >= miny && y < maxy)
                return true;
            
            // else check if we are on a border tile and the point
            // happens to sit right on the border we usually don't include
            if(x == maxx && x >= dataEnvelope.getMaxX())
                return true;
            if(y == maxy && y >= dataEnvelope.getMaxY())
                return true;
            return false;
        }

        private ReferencedEnvelope envelope(long x, long y, long z) {
            double tileSize = MAX_TILE_WIDTH / Math.pow(2, z);
            double xMin = x * tileSize + WORLD_BOUNDS.getMinX();
            double yMin = y * tileSize + WORLD_BOUNDS.getMinY();
            return new ReferencedEnvelope(xMin, xMin + tileSize, yMin, yMin
                    + tileSize, WGS84);
        }

        /**
         * Builds the best matching tile for the specified envelope
         */
        public Tile(ReferencedEnvelope wgs84Envelope) {
            z = Math.round(Math.log(MAX_TILE_WIDTH / wgs84Envelope.getWidth())
                    / Math.log(2));
            x = Math.round(((wgs84Envelope.getMinimum(0) - WORLD_BOUNDS
                    .getMinimum(0)) / MAX_TILE_WIDTH)
                    * Math.pow(2, z));
            y = Math.round(((wgs84Envelope.getMinimum(1) - WORLD_BOUNDS
                    .getMinimum(1)) / MAX_TILE_WIDTH)
                    * Math.pow(2, z));
            envelope = envelope(x, y, z);
        }

        /**
         * Returns the parent of this tile, or null if this tile is (one of) the
         * root of the current dataset
         * 
         * @return
         */
        public Tile getParent() {
            // if we got to one of the root tiles for this data set, just stop
            if (z == 0 || envelope.contains((BoundingBox) dataEnvelope))
                return null;
            else
                return new Tile((long) Math.floor(x / 2.0), (long) Math
                        .floor(y / 2.0), z - 1);
        }

        /**
         * Returns the four direct children of this tile
         * 
         * @return
         */
        public Tile[] getChildren() {
            Tile[] result = new Tile[4];
            result[0] = new Tile(x * 2, y * 2, z + 1);
            result[1] = new Tile(x * 2 + 1, y * 2, z + 1);
            result[2] = new Tile(x * 2, y * 2 + 1, z + 1);
            result[3] = new Tile(x * 2 + 1, y * 2 + 1, z + 1);
            return result;
        }

        /**
         * Returns the WGS84 envelope of this tile
         * 
         * @return
         */
        public ReferencedEnvelope getEnvelope() {
            return envelope;
        }

        @Override
        public String toString() {
            return "Tile X: " + x + ", Y: " + y + ", Z: " + z + " (" + envelope
                    + ")";
        }
    }

}
