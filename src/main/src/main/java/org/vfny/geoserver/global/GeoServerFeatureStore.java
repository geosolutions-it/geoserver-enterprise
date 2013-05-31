/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import java.io.IOException;
import java.util.List;

import org.geoserver.feature.RetypingFeatureCollection;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureStore;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;


/**
 * GeoServer wrapper for backend Geotools2 DataStore.
 *
 * <p>
 * Support FeatureSource decorator for FeatureTypeInfo that takes care of
 * mapping the FeatureTypeInfo's FeatureSource with the schema and definition
 * query configured for it.
 * </p>
 *
 * <p>
 * Because GeoServer requires that attributes always be returned in the same
 * order we need a way to smoothly inforce this. Could we use this class to do
 * so? It would need to support writing and locking though.
 * </p>
 *
 * @author Gabriel Rold?n
 * @version $Id$
 */
public class GeoServerFeatureStore extends GeoServerFeatureSource implements SimpleFeatureStore {
    /**
     * Creates a new DEFQueryFeatureLocking object.
     *
     * @param store GeoTools2 FeatureSource
     * @param schema FeatureType served by source
     * @param definitionQuery Filter that constrains source
     * @param declaredCRS Geometries will be forced to this CRS (or null, if no forcing is needed)
     * @param srsHandling
     */
    GeoServerFeatureStore(FeatureStore<SimpleFeatureType, SimpleFeature> store,
            SimpleFeatureType schema, Filter definitionQuery,
            CoordinateReferenceSystem declaredCRS, int srsHandling) {
        super(store, schema, definitionQuery, declaredCRS, srsHandling);
    }

    /**
     * FeatureStore access (to save casting)
     *
     * @return DOCUMENT ME!
     */
    SimpleFeatureStore store() {
        return (SimpleFeatureStore) source;
    }

    /**
     * see interface for details.
     * @param fc
     * @return
     * @throws IOException
     */
    public List<FeatureId> addFeatures(FeatureCollection<SimpleFeatureType, SimpleFeature> fc) throws IOException {
        FeatureStore<SimpleFeatureType, SimpleFeature> store = store();

        //check if the feature collection needs to be retyped
        if (!store.getSchema().equals(fc.getSchema())) {
            fc = new RetypingFeatureCollection(DataUtilities.simple(fc), store.getSchema());
        }

        return store().addFeatures(fc);
    }

    /**
     * DOCUMENT ME!
     *
     * @param filter DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public void removeFeatures(Filter filter) throws IOException {
        filter = makeDefinitionFilter(filter);

        store().removeFeatures(filter);
    }

    /**
     * DOCUMENT ME!
     *
     * @param type DOCUMENT ME!
     * @param value DOCUMENT ME!
     * @param filter DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     *
     * @task REVISIT: should we check that non exposed attributes are requiered
     *       in <code>type</code>?
     */
    public void modifyFeatures(AttributeDescriptor[] type, Object[] value, Filter filter)
        throws IOException {
        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(type, value, filter);
    }

    /**
     * DOCUMENT ME!
     *
     * @param type DOCUMENT ME!
     * @param value DOCUMENT ME!
     * @param filter DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public void modifyFeatures(AttributeDescriptor type, Object value, Filter filter)
        throws IOException {
        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(type, value, filter);
    }

    /**
     * DOCUMENT ME!
     *
     * @param reader DOCUMENT ME!
     *
     * @throws IOException DOCUMENT ME!
     */
    public void setFeatures(FeatureReader<SimpleFeatureType, SimpleFeature> reader) throws IOException {
        FeatureStore<SimpleFeatureType, SimpleFeature> store = store();

        //check if the feature reader needs to be retyped
        if (!store.getSchema().equals(reader.getFeatureType())) {
            reader = new RetypingFeatureCollection.RetypingFeatureReader(reader, store.getSchema());
        }

        store().setFeatures(reader);
    }

    /**
     * DOCUMENT ME!
     *
     * @param transaction DOCUMENT ME!
     */
    public void setTransaction(Transaction transaction) {
        store().setTransaction(transaction);
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public Transaction getTransaction() {
        return store().getTransaction();
    }

    public void modifyFeatures(String name, Object attributeValue, Filter filter)
            throws IOException {
        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(name, attributeValue, filter);
        
    }

    public void modifyFeatures(String[] names, Object[] attributeValues, Filter filter)
            throws IOException {
        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(names, attributeValues, filter);
        
    }

    public void modifyFeatures(Name[] attributeNames, Object[] attributeValues, Filter filter)
            throws IOException {
        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(attributeNames, attributeValues, filter);
        
    }

    public void modifyFeatures(Name attributeName, Object attributeValue, Filter filter)
            throws IOException {
        filter = makeDefinitionFilter(filter);

        store().modifyFeatures(attributeName, attributeValue, filter);
        
    }
}
