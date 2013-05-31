/* 
 * Copyright (c) 2001 - 20089 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wms.featureinfo;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import org.geotools.feature.CollectionListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

/**
 * This class is just a mean trick to smuggle in the resource name in to 
 * a FeatureCollection, when returned as result of a GetFeatureInfo request
 * Previously this was assumed to be equal to the name of the type of the FeatureCollection
 * But this is not the case for complex features in app-schema.
 *  
 * The other thing this does is providing an implementation of size(), since the
 * app-schema one always returns 0. This is used for limiting features to a maximum.
 * 
 * The decorator never needs to be used for SimpleFeatureCollections.
 * 
 * @author Niels Charlier, Curtin University of Technology
 *
 */
@SuppressWarnings("unchecked")
public class FeatureCollectionDecorator implements FeatureCollection<FeatureType, Feature> {

    /**
     * Get Resource Name of a Feature Collection
     * 
     * @param fc Feature Collection
     * @return Name of Resource
     */
    public static Name getName(FeatureCollection fc) {        
        if (fc instanceof FeatureCollectionDecorator){
            return ((FeatureCollectionDecorator) fc).getName();
        } else {
            return fc.getSchema().getName();
        }
    }
    
    
    protected FeatureCollection fc;
    protected Name name;
    
    public FeatureCollectionDecorator(Name name, FeatureCollection fc){
        this.name = name;
        this.fc = fc;
    }
    
    public Name getName() {
        return name;
    }
    
    public FeatureIterator<Feature> features() {
        return (FeatureIterator<Feature>) fc.features();
    }
    
    @SuppressWarnings("deprecation")
    public void close(FeatureIterator<Feature> close) {
       fc.close(close);        
    }

    @SuppressWarnings("deprecation")
    public void close(Iterator<Feature> close) {
        fc.close(close);        
    }

    public void addListener(CollectionListener listener) throws NullPointerException {
        fc.addListener(listener);        
    }

    public void removeListener(CollectionListener listener) throws NullPointerException {
        fc.removeListener(listener);        
    }

    public FeatureType getSchema() {
        return fc.getSchema();
    }

    public String getID() {
        return fc.getID();
    }

    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        fc.accepts(visitor, progress);
        
    }

    public FeatureCollection<FeatureType, Feature> subCollection(Filter filter) {
        return fc.subCollection(filter);
    }

    public FeatureCollection<FeatureType, Feature> sort(SortBy order) {
        return fc.sort(order);
    }

    public ReferencedEnvelope getBounds() {
        return fc.getBounds();
    }

    @SuppressWarnings("deprecation")
    public Iterator<Feature> iterator() {
        return fc.iterator();
    }

    @SuppressWarnings("deprecation")
    public void purge() {
        fc.purge();        
    }

    public boolean add(Feature obj) {
        return fc.add(obj);
    }

    public boolean addAll(Collection<? extends Feature> collection) {
       return fc.addAll(collection);
    }

    public boolean addAll(FeatureCollection<? extends FeatureType, ? extends Feature> resource) {
       return fc.addAll(resource);
    }

    public void clear() {
        fc.clear();        
    }

    public boolean contains(Object o) {
        return fc.contains(o);
    }

    public boolean containsAll(Collection<?> o) {
        return fc.containsAll(o);
    }

    public boolean isEmpty() {
        return fc.isEmpty();
    }

    public boolean remove(Object o) {
        return fc.remove(o);
    }

    public boolean removeAll(Collection<?> c) {
        return fc.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return fc.retainAll(c);
    }

    public int size() {
        //overriding size implementation
        //simply counting!
        FeatureIterator iterator = features();
        int count = 0;
        while (iterator.hasNext()) {
            count++;
            iterator.next();
        }
        return count;
    }

    public Object[] toArray() {
        return fc.toArray();
    }

    public <O> O[] toArray(O[] a) {
        return (O[]) fc.toArray(a);
    }

}
