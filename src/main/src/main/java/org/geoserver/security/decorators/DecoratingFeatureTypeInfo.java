/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.AbstractDecorator;
import org.geotools.data.FeatureSource;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.util.ProgressListener;

/**
 * Delegates every method to the delegate feature type info. Subclasses will
 * override selected methods to perform their "decoration" job
 * 
 * @author Andrea Aime
 */
public abstract class DecoratingFeatureTypeInfo extends AbstractDecorator<FeatureTypeInfo> implements FeatureTypeInfo {

    public DecoratingFeatureTypeInfo(FeatureTypeInfo info) {
        super(info);
    }

    public FeatureSource getFeatureSource(ProgressListener listener, Hints hints)
            throws IOException {
        return delegate.getFeatureSource(listener, hints);
    }

    public DataStoreInfo getStore() {
        return delegate.getStore();
    }

    public String getAbstract() {
        return delegate.getAbstract();
    }

    public <T> T getAdapter(Class<T> adapterClass, Map<?, ?> hints) {
        return delegate.getAdapter(adapterClass, hints);
    }

    public List<String> getAlias() {
        return delegate.getAlias();
    }

    public List<AttributeTypeInfo> getAttributes() {
        return delegate.getAttributes();
    }

    public ReferencedEnvelope boundingBox() throws Exception {
        return delegate.boundingBox();
    }

    public Catalog getCatalog() {
        return delegate.getCatalog();
    }
    
    public void setCatalog(Catalog catalog) {
        delegate.setCatalog( catalog );
    }

    public CoordinateReferenceSystem getCRS() {
        return delegate.getCRS();
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public List<AttributeTypeInfo> attributes() throws IOException {
        return delegate.attributes();
    }
    
    public FeatureType getFeatureType() throws IOException {
        return delegate.getFeatureType();
    }

    public Filter getFilter() {
        return delegate.getFilter();
    }

    public String getId() {
        return delegate.getId();
    }

    @Override
    public List<KeywordInfo> getKeywords() {
        return delegate.getKeywords();
    }

    public List<String> keywordValues() {
        return delegate.keywordValues();
    }

    public ReferencedEnvelope getLatLonBoundingBox() {
        return delegate.getLatLonBoundingBox();
    }

    public int getMaxFeatures() {
        return delegate.getMaxFeatures();
    }

    public MetadataMap getMetadata() {
        return delegate.getMetadata();
    }

    public List<MetadataLinkInfo> getMetadataLinks() {
        return delegate.getMetadataLinks();
    }

    public String getName() {
        return delegate.getName();
    }
    
    /**
     * @see org.geoserver.catalog.ResourceInfo#getQualifiedName()
     */
    public Name getQualifiedName() {
        return delegate.getQualifiedName();
    }

    public NamespaceInfo getNamespace() {
        return delegate.getNamespace();
    }

    public ReferencedEnvelope getNativeBoundingBox() {
        return delegate.getNativeBoundingBox();
    }

    public CoordinateReferenceSystem getNativeCRS() {
        return delegate.getNativeCRS();
    }

    public String getNativeName() {
        return delegate.getNativeName();
    }

    /**
     * @see org.geoserver.catalog.ResourceInfo#getQualifiedNativeName()
     */
    public Name getQualifiedNativeName() {
        return delegate.getQualifiedNativeName();
    }

    public int getNumDecimals() {
        return delegate.getNumDecimals();
    }

    public String getPrefixedName() {
        return delegate.getPrefixedName();
    }

    public String prefixedName() {
        return delegate.prefixedName();
    }

    public ProjectionPolicy getProjectionPolicy() {
        return delegate.getProjectionPolicy();
    }

    public String getSRS() {
        return delegate.getSRS();
    }

    public String getTitle() {
        return delegate.getTitle();
    }

    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    public boolean enabled() {
        return delegate.enabled();
    }

    public void setAbstract(String _abstract) {
        delegate.setAbstract(_abstract);
    }

    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    public void setEnabled(boolean enabled) {
        delegate.setEnabled(enabled);
    }

    public void setFilter(Filter filter) {
        delegate.setFilter(filter);
    }

    public void setLatLonBoundingBox(ReferencedEnvelope box) {
        delegate.setLatLonBoundingBox(box);
    }

    public void setMaxFeatures(int maxFeatures) {
        delegate.setMaxFeatures(maxFeatures);
    }

    public void setName(String name) {
        delegate.setName(name);
    }

    public void setNamespace(NamespaceInfo namespace) {
        delegate.setNamespace(namespace);
    }

    public void setNativeBoundingBox(ReferencedEnvelope box) {
        delegate.setNativeBoundingBox(box);
    }

    public void setNativeCRS(CoordinateReferenceSystem nativeCRS) {
        delegate.setNativeCRS(nativeCRS);
    }

    public void setNativeName(String nativeName) {
        delegate.setNativeName(nativeName);
    }

    public void setNumDecimals(int numDecimals) {
        delegate.setNumDecimals(numDecimals);
    }

    public void setProjectionPolicy(ProjectionPolicy policy) {
        delegate.setProjectionPolicy(policy);
    }

    public void setSRS(String srs) {
        delegate.setSRS(srs);
    }

    public void setStore(StoreInfo store) {
        delegate.setStore(store);
    }

    public void setTitle(String title) {
        delegate.setTitle(title);
    }
   
    public void accept(CatalogVisitor visitor) {
        delegate.accept(visitor);
    }
    
    @Override
    public boolean isAdvertised() {
        return delegate.isAdvertised();
    }
    
    @Override
    public void setAdvertised(boolean advertised) {
        delegate.setAdvertised(advertised);
    }

}
