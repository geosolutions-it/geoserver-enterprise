/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.ServiceLoader;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.platform.GeoServerResourceLoader;

/**
 * Service loader which loads and saves a service configuration with xstream.
 *   
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public abstract class XStreamServiceLoader<T extends ServiceInfo> implements ServiceLoader<T> {
    
    GeoServerResourceLoader resourceLoader;
    String filenameBase;
    XStreamPersisterFactory xpf = new XStreamPersisterFactory();
    
    public XStreamServiceLoader(GeoServerResourceLoader resourceLoader, String filenameBase) {
        this.resourceLoader = resourceLoader;
        this.filenameBase = filenameBase;
    }

    public String getFilename() {
        return filenameBase + ".xml";
    }
    
    public void setXStreamPeristerFactory(XStreamPersisterFactory xpf) {
        this.xpf = xpf;
    }
    
    public final T load(GeoServer gs) throws Exception {
        return load(gs, null);
    }

    public final T load(GeoServer gs, File directory) throws Exception {
        //look for file matching classname
        String filename = getFilename();
        File file = resourceLoader.find(directory, filename);
        
        if ( file != null && file.exists() ) {
            //xstream it in
            BufferedInputStream in = 
                new BufferedInputStream( new FileInputStream( file ) );
            try {
                XStreamPersister xp = xpf.createXMLPersister();
                initXStreamPersister(xp, gs);
                return initialize( xp.load( in, getServiceClass() ) );
            }
            finally {
                in.close();    
            }
        }
        else {
            //create an 'empty' object
            ServiceInfo service = createServiceFromScratch( gs );
            return initialize( (T) service );
        }
    }

    /**
     * Fills in the blanks of the service object loaded by XStream. This implementation makes sure
     * all collections in {@link ServiceInfoImpl} are initialized, subclasses should override to add
     * more specific initializations (such as the actual supported versions and so on)
     * 
     * @param service
     * @return
     */
    protected T initialize(T service) {
        if (service instanceof ServiceInfoImpl) {
            // initialize all collections to 
            ServiceInfoImpl impl = (ServiceInfoImpl) service;
            if (impl.getClientProperties() == null) {
                impl.setClientProperties(new HashMap());
            }
            if (impl.getExceptionFormats() == null) {
                impl.setExceptionFormats(new ArrayList());
            }
            if (impl.getKeywords() == null) {
                impl.setKeywords(new ArrayList());
            }
            if (impl.getMetadata() == null) {
                impl.setMetadata(new MetadataMap());
            }
            if (impl.getVersions() == null) {
                impl.setVersions(new ArrayList());
            }
        }

        return service;
    }

    public final void save(T service, GeoServer gs) throws Exception {
        
    }

    public final void save(T service, GeoServer gs, File directory) throws Exception {
        String filename = getFilename();
        File file = resourceLoader.find( directory, filename );
        if ( file == null ) {
            file = resourceLoader.createFile(directory, filename);
        }
        
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        try {
            XStreamPersister xp = xpf.createXMLPersister();
            initXStreamPersister(xp, gs);
            xp.save( service, out );
            
            out.flush();
        }
        finally {
            out.close();
        }
    }
    
    /**
     * Hook for subclasses to configure the xstream.
     * <p>
     * The most common use is to do some aliasing or omit some fields. 
     * </p>
     */
    protected void initXStreamPersister( XStreamPersister xp, GeoServer gs ) {
        xp.setGeoServer( gs );
        xp.setCatalog( gs.getCatalog() );
        xp.getXStream().alias( filenameBase, getServiceClass() );
    }
    
    protected abstract T createServiceFromScratch(GeoServer gs);
}
