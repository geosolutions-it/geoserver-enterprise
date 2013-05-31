/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.util.List;

import org.geoserver.config.GeoServerInfo;
import org.geotools.data.FeatureSource;
import org.geotools.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

/**
 * A vector-based or feature based resource.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * @uml.dependency supplier="org.geoserver.catalog.FeatureResource"
 */
public interface FeatureTypeInfo extends ResourceInfo {
    
    /**
     * The sql view definition
     */
    static final String JDBC_VIRTUAL_TABLE = "JDBC_VIRTUAL_TABLE";
    
    /**
     * The data store the feature type is a part of.
     * <p>
     * </p>
     */
    DataStoreInfo getStore();
    
    /**
     * The attributes that the feature type exposes.
     * <p>
     * Services and client code will want to call the {@link #attributes()}
     * method over this one.
     * </p>
     */
    List<AttributeTypeInfo> getAttributes();
    
    /**
     * A filter which should be applied to all queries of the dataset
     * represented by the feature type.
     * 
     * @return A filter, or <code>null</code> if one not set.
     * @uml.property name="filter"
     */
    Filter getFilter();

    /**
     * Sets a filter which should be applied to all queries of the dataset
     * represented by the feature type.
     * 
     * @param filter
     *                A filter, can be <code>null</code>
     * @uml.property name="filter"
     */
    void setFilter(Filter filter);

    /**
     * A cap on the number of features that a query against this type can return.
     * <p>
     * Note that this value should override the global default: 
     *  {@link GeoServerInfo#getMaxFeatures()}.
     *  </p>
     */
    int getMaxFeatures();
    
    /**
     * Sets a cap on the number of features that a query against this type can return.
     * 
     */
    void setMaxFeatures( int maxFeatures );
    
    /**
     * The number of decimal places to use when encoding floating point 
     * numbers from data of this feature type.
     * <p>
     * Note that this value should override the global default: 
     *  {@link GeoServerInfo#getNumDecimals()}.
     *  </p>
     */
    int getNumDecimals();
    
    /**
     * Sets the number of decimal places to use when encoding floating point 
     * numbers from data of this feature type.
     */
    void setNumDecimals( int numDecimals );
    
    /**
     * Returns the derived set of attributes for the feature type.
     * <p>
     * This value is derived from the underlying feature, and any 
     * overrides configured via {@link #getAttributes()}.
     * </p>
     */
    List<AttributeTypeInfo> attributes() throws IOException;
    
    /**
     * Returns the underlying geotools feature type.
     * <p>
     * The returned feature type is "wrapped" to take into account "metadata", 
     * such as reprojection and name aliasing. 
     * </p>
     */
    FeatureType getFeatureType() throws IOException;
    
    /**
     * Returns the underlying feature source instance.
     * <p>
     * This method does I/O and is potentially blocking. The <tt>listener</tt>
     * may be used to report the progress of loading the feature source and also
     * to report any errors or warnings that occur.
     * </p>
     * 
     * @param listener
     *                A progress listener, may be <code>null</code>.
     * @param hints Hints to use while loading the featuer source, may be <code>null</code>.
     * 
     * @return The feature source.
     * 
     * @throws IOException
     *                 Any I/O problems.
     */
    FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource( ProgressListener listener, Hints hints )
            throws IOException;

    /**
     * The live feature resource, an instance of of {@link FeatureResource}.
     */
    //FeatureResource getResource(ProgressListener listener)
    //        throws IOException;
}
