/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.data.FeatureSource;
import org.geotools.factory.Hints;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

@SuppressWarnings("serial")
public class FeatureTypeInfoImpl extends ResourceInfoImpl implements
        FeatureTypeInfo {

    protected List<AttributeTypeInfo> attributes = new ArrayList<AttributeTypeInfo>();
    
    protected Filter filter;

    protected int maxFeatures;
    protected int numDecimals;

    protected FeatureTypeInfoImpl() {
    }

    public FeatureTypeInfoImpl(Catalog catalog) {
        super(catalog);
    }

    public FeatureTypeInfoImpl(Catalog catalog, String id) {
        super(catalog,id);
    }

    public DataStoreInfo getStore() {
        return (DataStoreInfo) super.getStore();
    }

    public List<AttributeTypeInfo> getAttributes() {
        return attributes;
    }
    
    public void setAttributes(List<AttributeTypeInfo> attributes) {
        this.attributes = attributes;
    }
    
    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }
    
    public int getMaxFeatures() {
        return maxFeatures;
    }
    
    public void setMaxFeatures(int maxFeatures) {
        this.maxFeatures = maxFeatures;
    }
    
    public int getNumDecimals() {
        return numDecimals;
    }
    
    public void setNumDecimals(int numDecimals) {
        this.numDecimals = numDecimals;
    }
    
    public List<AttributeTypeInfo> attributes() throws IOException {
        return catalog.getResourcePool().getAttributes( this );
    }

    public FeatureType getFeatureType() throws IOException {
        return catalog.getResourcePool().getFeatureType( this );
    }
    
    public FeatureSource<? extends FeatureType, ? extends Feature> getFeatureSource(ProgressListener listener, Hints hints)
            throws IOException {
        return catalog.getResourcePool().getFeatureSource( this, hints );
    }
    
    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((filter == null) ? 0 : filter.hashCode());
        result = prime * result + maxFeatures;
        result = prime * result + numDecimals;
        return result;
    }

    public boolean equals(Object obj) {
        if ( !(obj instanceof FeatureTypeInfo ) ) {
            return false;
        }
        if ( !super.equals( obj ) ) {
            return false;
        }
        
        final FeatureTypeInfo other = (FeatureTypeInfo) obj;
        if (attributes == null) {
            if (other.getAttributes() != null)
                return false;
        } else if (!attributes.equals(other.getAttributes()))
            return false;
        if (filter == null) {
            if (other.getFilter() != null)
                return false;
        } else if (!filter.equals(other.getFilter()))
            return false;
        if (maxFeatures != other.getMaxFeatures())
            return false;
        if (numDecimals != other.getNumDecimals())
            return false;
        return true;
    }
    
    
}
