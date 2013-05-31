/* Copyright (c) 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.geotools.util.SoftValueHashMap;

/**
 * An abstract cache for resources that need to be loaded from files. Loads the resources
 * autonomously, making sure the returned items are fresh compared to the file system
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 * @param <T>
 */
abstract class FileItemCache<T> {

    /**
     * Caches objects, from file name to the actual parsed content
     */
    Map<String, CacheItem<T>> cache;

    public FileItemCache(int maxHardReferences) {
        cache = new SoftValueHashMap<String, CacheItem<T>>(maxHardReferences);
    }
    
    /**
     * Clears the cache contents
     */
    public void clear() {
        cache.clear();
    }

    public T getItem(File file) throws IOException {
        // see if we have an up to date version of the item in the cache
        String key = getFileKey(file);
        CacheItem<T> ci = cache.get(key);
        if (ci != null && ci.isUpToDate(file)) {
            return ci.getItem();
        }

        // if not, load and cache
        T item = loadItem(file);
        if(item == null) {
            return null;
        }
        ci = new CacheItem<T>(item, file);
        cache.put(key, ci);

        return item;
    }
    
    public boolean removeItem(File file) {
        String key = getFileKey(file);
        return cache.remove(key) != null;
    }

    /**
     * The key used in the item cache to represent this file. It uses the file name, assuming we are
     * going to cache files originating from the same directory. Subclasses may override to get a
     * different behavior
     * 
     * @param file
     * @return
     */
    protected String getFileKey(File file) {
        return file.getName();
    }

    /**
     * Loads an item from the file
     * 
     * @param file
     * @return
     */
    protected abstract T loadItem(File file) throws IOException;

    /**
     * Manually updates the contents of the cache
     * 
     * @param item
     * @param file
     */
    public void put(T item, File file) {
        CacheItem ci = new CacheItem<T>(item, file);
        String key = getFileKey(file);
        cache.put(key, ci);
    }

    

}
