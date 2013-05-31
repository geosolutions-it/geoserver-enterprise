/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.AbstractList;
import java.util.List;

/**
 * An unmodifiable list proxy in which each element in the list is wrapped in a 
 * proxy of its own.
 * <p>
 * Subclasses should implement {@link #createProxy(Object, Class)}. 
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public abstract class ProxyList extends AbstractList {

    protected List proxyList;
    protected Class proxyInterface;
    
    public ProxyList(List proxyList, Class proxyInterface) {
        this.proxyList = proxyList;
        this.proxyInterface = proxyInterface;
    }
    
    public Object get(int index) {
        Object proxyObject = proxyList.get( index );
        return createProxy(proxyObject, proxyInterface);
    }
    
    @Override
    public Object set(int index, Object element) {
            throw new IllegalArgumentException( 
                "Object is not a proxy, or not a proxy of the correct type");
    }
    
    public int size() {
        return proxyList.size();
    }
    
    /**
     * Wraps an object from the underlying list in the proxy.
     */
    abstract protected <T> T createProxy( T proxyObject, Class<T> proxyInterface );
    
    /**
     * Unwraps a proxy object or insertion into the underlying list.
     * <p>
     * Note: This method should handle the case of the object not being a proxy instance, 
     * but an regular instance of proxyInterface. 
     */
    abstract protected <T> T unwrapProxy( T proxy, Class<T> proxyInterface );
}
