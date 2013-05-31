/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.StreamDataFormat;
import org.geoserver.rest.util.RESTUtils;
import org.geotools.data.DataAccessFactory;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.data.FeatureStore;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.Transaction;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.vfny.geoserver.util.DataStoreUtils;

public class DataStoreFileResource extends StoreFileResource {

    protected static HashMap<String,String> formatToDataStoreFactory = new HashMap();
    static {
        formatToDataStoreFactory.put( "shp", "org.geotools.data.shapefile.ShapefileDataStoreFactory");
        formatToDataStoreFactory.put( "properties", "org.geotools.data.property.PropertyDataStoreFactory");
        formatToDataStoreFactory.put( "h2", "org.geotools.data.h2.H2DataStoreFactory");
        formatToDataStoreFactory.put( "spatialite", "org.geotools.data.spatialite.SpatiaLiteDataStoreFactory");
    }
    
    protected static HashMap<String,Map> dataStoreFactoryToDefaultParams = new HashMap();
    static {
        HashMap map = new HashMap();
        map.put("database", "@DATA_DIR@/@NAME@");
        map.put("dbtype", "h2");
        
        dataStoreFactoryToDefaultParams.put("org.geotools.data.h2.H2DataStoreFactory", map);
        
        map = new HashMap();
        map.put("database", "@DATA_DIR@/@NAME@");
        map.put("dbtype", "spatialite");
        
        dataStoreFactoryToDefaultParams.put("org.geotools.data.spatialite.SpatiaLiteDataStoreFactory", map);
    }
    
    public static DataStoreFactorySpi lookupDataStoreFactory(String format) {
        // first try and see if we know about this format directly
        String factoryClassName = formatToDataStoreFactory.get(format);
        if (factoryClassName != null) {
            try {
                Class factoryClass = Class.forName(factoryClassName);
                DataStoreFactorySpi factory = (DataStoreFactorySpi) factoryClass.newInstance();
                return factory;
            } catch (Exception e) {
                throw new RestletException("Datastore format unavailable: " + factoryClassName,
                        Status.SERVER_ERROR_INTERNAL);
            }
        }

        // if not, let's see if we have a file data store factory that knows about the extension
        String extension = "." + format;
        for (DataAccessFactory dataAccessFactory : DataStoreUtils.getAvailableDataStoreFactories()) {
            if (dataAccessFactory instanceof FileDataStoreFactorySpi) {
                FileDataStoreFactorySpi factory = (FileDataStoreFactorySpi) dataAccessFactory;
                for (String handledExtension : factory.getFileExtensions()) {
                    if (extension.equalsIgnoreCase(handledExtension)) {
                        return factory;
                    }
                }
            }
        }

        throw new RestletException("Unsupported format: " + format, Status.CLIENT_ERROR_BAD_REQUEST);
    }
    
    public static String lookupDataStoreFactoryFormat(String type) {
        for (DataAccessFactory factory : DataStoreUtils.getAvailableDataStoreFactories()) {
            if (!(factory instanceof DataStoreFactorySpi)) {
                continue;
            }
            
            if (factory.getDisplayName().equals(type)) {
                for(Map.Entry e: formatToDataStoreFactory.entrySet()) {
                    if (e.getValue().equals(factory.getClass().getCanonicalName())) {
                        return (String) e.getKey();
                    }
                }
                
                return factory.getDisplayName();
            }
        }
        
        return null;
    }
    
    String dataStoreFormat;
    DataStoreFactorySpi factory;
    
    public DataStoreFileResource( Request request, Response response, String dataStoreFormat, Catalog catalog ) {
        super( request, response, catalog );
        this.dataStoreFormat = dataStoreFormat;
        this.factory = lookupDataStoreFactory(dataStoreFormat);
    }
    
    @Override
    public void handleGet() {
        String workspace = getAttribute("workspace");
        String datastore = getAttribute("datastore");
        String format = getAttribute("format");

        //find the directory from teh datastore connection parameters
        DataStoreInfo info = catalog.getDataStoreByName(workspace, datastore);
        if (info == null) {
            throw new RestletException("No such datastore " + datastore, Status.CLIENT_ERROR_NOT_FOUND);
        }
        
        Map<String,Serializable> params = info.getConnectionParameters();
        File directory = null;
        for (Map.Entry<String, Serializable> e : params.entrySet()) {
            if (e.getValue() instanceof File) {
                directory = (File) e.getValue();
            }
            else if (e.getValue() instanceof URL) {
                directory = new File(((URL)e.getValue()).getFile());
            }
            if (directory != null && !"directory".equals(e.getKey())) {
                directory = directory.getParentFile();
            }
            
            if (directory != null) {
                break;
            }
        }
        
        if ( directory == null || !directory.exists() || !directory.isDirectory() ) {
            throw new RestletException( "No files for datastore " + datastore, Status.CLIENT_ERROR_NOT_FOUND );
        }
        
        //zip up all the files in the directory
        StreamDataFormat fmt = new StreamDataFormat(MediaType.APPLICATION_ZIP) {

            @Override
            protected Object read(InputStream in) throws IOException {
                return null;
            }

            @Override
            protected void write(Object object, OutputStream out)
                    throws IOException {
                ZipOutputStream zout = new ZipOutputStream( out );
            
                File directory = (File) object;
                for ( File f : directory.listFiles() ) {
                    ZipEntry entry = new ZipEntry( f.getName() );
                    zout.putNextEntry(entry);
                    FileInputStream fis = null;
                    try {
                        fis = new FileInputStream(f);
                        IOUtils.copy(fis, zout);
                    } finally {
                        IOUtils.closeQuietly(fis);
                    }
                    
                    zout.closeEntry();
                }
                zout.flush();
                zout.close();
            }
        };
        getResponse().setEntity( fmt.toRepresentation( directory ) );
    }
    
    @Override
    public void handlePut() {
        String workspace = getAttribute("workspace");
        String datastore = getAttribute("datastore");
        String format = getAttribute("format");
        String method = getUploadMethod(getRequest());
        
        getResponse().setStatus(Status.SUCCESS_ACCEPTED);
        Form form = getRequest().getResourceRef().getQueryAsForm();

        File uploadedFile = doFileUpload(method, workspace, datastore, format);
        
        //look up the target datastore type specified by user
        String sourceDataStoreFormat = dataStoreFormat; 
        String targetDataStoreFormat = RESTUtils.getQueryStringValue(getRequest(), "target");
        if (targetDataStoreFormat == null) {
            //set the same type as the source
            targetDataStoreFormat = sourceDataStoreFormat;
        }
        
        sourceDataStoreFormat = sourceDataStoreFormat.toLowerCase();
        targetDataStoreFormat = targetDataStoreFormat.toLowerCase();
        
        //create a builder to help build catalog objects
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.setWorkspace( catalog.getWorkspaceByName( workspace ) );
        
        //does the target datastore already exist?
        DataStoreInfo info = catalog.getDataStoreByName( workspace, datastore );

        // set the namespace uri
        NamespaceInfo namespace = catalog.getNamespaceByPrefix( workspace );

        boolean add = false;
        boolean save = false;
        boolean canRemoveFiles = false;
        
        String charset = form.getFirstValue("charset");
        
        if (info == null) {
            LOGGER.info("Auto-configuring datastore: " + datastore);
            
            info = builder.buildDataStore( datastore );
            add = true;
            
            //TODO: should check if the store actually supports charset
            if (charset != null && charset.length() > 0) {
                info.getConnectionParameters().put("charset", charset);
            }
            DataStoreFactorySpi targetFactory = factory;
            if (!targetDataStoreFormat.equals(sourceDataStoreFormat)) {
                //target is different, we need to create it
                targetFactory = lookupDataStoreFactory(targetDataStoreFormat);
                if (targetFactory == null) {
                    throw new RestletException( "Unable to create data store of type "
                        + targetDataStoreFormat, Status.CLIENT_ERROR_BAD_REQUEST );
                }
                
                autoCreateParameters(info, namespace, targetFactory);
                canRemoveFiles = true;
            }
            else {
                updateParameters(info, namespace, targetFactory, uploadedFile);
            }
            
            info.setType(targetFactory.getDisplayName());
        }
        else {
            LOGGER.info("Using existing datastore: " + datastore);
            
            // look up the target data store factory
            targetDataStoreFormat = lookupDataStoreFactoryFormat(info.getType());
            if (targetDataStoreFormat == null) {
                throw new RuntimeException("Unable to locate data store factory of type " + info.getType());
            }
            
            if (targetDataStoreFormat.equals(sourceDataStoreFormat)) {
                save = true;
                updateParameters(info, namespace, factory, uploadedFile);
            }
            else {
                canRemoveFiles = true;
            }
        }
        builder.setStore(info);
        
        //add or update the datastore info
        if ( add ) {
            catalog.add( info );
        }
        else {
            if (save) {
                catalog.save( info );
            }
        }
        
        //create an instanceof the source datastore
        HashMap params = new HashMap();
        if (charset != null && charset.length() > 0) {
            params.put("charset",charset);	
        }
        updateParameters(params, factory, uploadedFile);
        DataStore source;
        try {
            source = factory.createDataStore(params);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create source data store", e);
        }
        
        try {
            DataStore ds = (DataStore) info.getDataStore(null);
            //synchronized(ds) {
            //if it is the case that the source does not match the target we need to 
            // copy the data into the target
            if (!targetDataStoreFormat.equals(sourceDataStoreFormat)) {
                //copy over the feature types
                for (String featureTypeName : source.getTypeNames()) {
                    SimpleFeatureType featureType = null;
                    
                    //does the feature type already exist in the target?
                    try {
                        featureType = ds.getSchema(featureTypeName); 
                    }
                    catch(Exception e) {
                        LOGGER.info(featureTypeName + " does not exist in data store " + datastore +
                            ". Attempting to create it");
                        
                        //schema does not exist, create it by first creating an instance of 
                        // the source datastore and copying over its schema
                        ds.createSchema(source.getSchema(featureTypeName));
                        featureType = source.getSchema(featureTypeName);
                    }
    
                    FeatureSource featureSource = ds.getFeatureSource(featureTypeName);
                    if (!(featureSource instanceof FeatureStore)) {
                        LOGGER.warning(featureTypeName + " is not writable, skipping");
                        continue;
                    }
                    
                    Transaction tx = new DefaultTransaction();
                    FeatureStore featureStore = (FeatureStore) featureSource;
                    featureStore.setTransaction(tx);
                    
                    try {
                        //figure out update mode, whether we should kill existing data or append
                        String update = form.getFirstValue("update");
                        if ("overwrite".equalsIgnoreCase(update)) {
                            LOGGER.fine("Removing existing features from " + featureTypeName);
                            //kill all features
                            featureStore.removeFeatures(Filter.INCLUDE);
                        }
                        
                        LOGGER.fine("Adding features to " + featureTypeName);
                        FeatureCollection features = source.getFeatureSource(featureTypeName).getFeatures();
                        featureStore.addFeatures(features);
                        
                        tx.commit();
                    }
                    catch(Exception e) {
                        tx.rollback();
                    }
                    finally {
                        tx.close();
                    }
                }
            }

            //check configure parameter, if set to none to not try to configure
            // data feature types
            String configure = form.getFirstValue( "configure" );
            if ( "none".equalsIgnoreCase( configure ) ) {
                getResponse().setStatus( Status.SUCCESS_CREATED );
                return;
            }
            
            //load the target datastore
            //DataStore ds = (DataStore) info.getDataStore(null);
            Map<String, FeatureTypeInfo> featureTypesByNativeName =
                new HashMap<String, FeatureTypeInfo>();
            for (FeatureTypeInfo ftInfo : catalog.getFeatureTypesByDataStore(info)) {
                featureTypesByNativeName.put(ftInfo.getNativeName(), ftInfo);
            }
            
            String[] featureTypeNames = source.getTypeNames();
            for ( int i = 0; i < featureTypeNames.length; i++ ) {
                
                //unless configure specified "all", only configure the first feature type
                if ( !"all".equalsIgnoreCase( configure ) && i > 0 ) {
                    break;
                }
                
                FeatureSource fs = ds.getFeatureSource(featureTypeNames[i]); 
                FeatureTypeInfo ftinfo = featureTypesByNativeName.get(featureTypeNames[i]);
                
                if ( ftinfo == null) {
                    //auto configure the feature type as well
                    ftinfo = builder.buildFeatureType(fs);
                    builder.lookupSRS(ftinfo, true);
                    builder.setupBounds(ftinfo);
                }
                
                //update the bounds
                ReferencedEnvelope bounds = fs.getBounds();
                ftinfo.setNativeBoundingBox( bounds );
                
                //TODO: set lat lon bounding box
                
                if ( ftinfo.getId() == null ) {
                    
                    //do a check for a type already named this name in the catalog, if it is already
                    // there try to rename it
                    if (catalog.getFeatureTypeByName(namespace, ftinfo.getName()) != null) {
                        LOGGER.warning(String.format("Feature type %s already exists in namespace %s, " +
                            "attempting to rename", ftinfo.getName(), namespace.getPrefix()));
                        int x = 1;
                        String originalName = ftinfo.getName();
                        do {
                            ftinfo.setName(originalName + i);
                            i++;
                        }
                        while(i < 10 && catalog.getFeatureTypeByName(namespace, ftinfo.getName()) != null);
                    }
                    catalog.add( ftinfo );
                    
                    //add a layer for the feature type as well
                    LayerInfo layer = builder.buildLayer(ftinfo);

                    boolean valid = true;
                    try { 
                        if (!catalog.validate(layer, true).isEmpty()) {
                            valid = false;
                        }
                    } catch (Exception e) {
                        valid = false;
                    }
                    
                    layer.setEnabled(valid);
                    catalog.add(layer);
                    
                    LOGGER.info("Added feature type " + ftinfo.getName());
                   
                }
                else {
                    LOGGER.info("Updated feature type " + ftinfo.getName());
                    catalog.save( ftinfo );
                }
                
                getResponse().setStatus( Status.SUCCESS_CREATED );
            }
            //}
        } 
        catch (Exception e) {
            //TODO: report a proper error code
            throw new RuntimeException ( e );
        }
        
        //dispose the datastore
        source.dispose();
        
        //clean up the files if we can
        if (isInlineUpload(method) && canRemoveFiles) {
            if (uploadedFile.isFile()) uploadedFile = uploadedFile.getParentFile();
            try {
                FileUtils.deleteDirectory(uploadedFile);
            } 
            catch (IOException e) {
                LOGGER.info("Unable to delete " + uploadedFile.getAbsolutePath());
                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.log(Level.FINE, "", e);
                }
            }
        }
    }

    @Override
    protected File findPrimaryFile(File directory, String format) {
        if ("shp".equalsIgnoreCase(format)) {
            //special case for shapefiles, since shapefile datastore can handle directories just 
            // return the directory, this handles the case of a user uploading a zip with multiple
            // shapefiles in it
            return directory;
        }
        else {
            return super.findPrimaryFile(directory, format);
        }
    }
    
    void updateParameters(DataStoreInfo info, NamespaceInfo namespace, DataStoreFactorySpi factory, File uploadedFile) {
        Map connectionParameters = info.getConnectionParameters();
        updateParameters(connectionParameters, factory, uploadedFile);

        connectionParameters.put( "namespace", namespace.getURI() );
        // ensure the parameters are valid
        if ( !factory.canProcess( connectionParameters ) ) {
            //TODO: log the parameters at the debug level
            throw new RestletException( "Unable to configure datastore, bad parameters.", Status.SERVER_ERROR_INTERNAL );
        }
    }
    
    void updateParameters(Map connectionParameters, DataStoreFactorySpi factory, File uploadedFile) {

        for ( Param p : factory.getParametersInfo() ) {
            //the nasty url / file hack
            if ( File.class == p.type || URL.class == p.type ) {
                File f = uploadedFile;
                
                if ( "directory".equals( p.key ) ) {
                    //set the value to be the directory
                    f = f.getParentFile();
                }
                
                //convert to the required type
                //TODO: use geotools converter
                Object converted = null;
                if ( URI.class.equals( p.type  ) ) {
                    converted = f.toURI();
                }
                else if ( URL.class.equals( p.type ) ) {
                    try {
                        converted = f.toURL();
                    } 
                    catch (MalformedURLException e) {
                    }
                }
                //Converters.convert( f.getAbsolutePath(), p.type );
                
                if ( converted != null ) {
                    connectionParameters.put( p.key, converted );    
                }
                else {
                    connectionParameters.put( p.key, f );
                }
                
                continue;
            }
            
            if ( p.required ) {
                try {
                    p.lookUp( connectionParameters );
                }
                catch( Exception e ) {
                    //set the sample value
                    connectionParameters.put( p.key, p.sample );
                }    
            }
        }
    }
    
    void autoCreateParameters(DataStoreInfo info, NamespaceInfo namespace, DataStoreFactorySpi factory) {
        Map defaultParams = dataStoreFactoryToDefaultParams.get(factory.getClass().getCanonicalName());
        if (defaultParams == null) {
            throw new RuntimeException("Unable to auto create parameters for " + factory.getDisplayName());
        }
        
        HashMap params = new HashMap(defaultParams);
        
        // replace any replacable parameters
        String dataDirRoot = catalog.getResourceLoader().getBaseDirectory().getAbsolutePath();
        for (Object o : params.entrySet()) {
            Map.Entry e = (Map.Entry)o;
            if (e.getValue() instanceof String) {
                String string = (String) e.getValue();
                string = string.replace("@NAME@", info.getName()).replace("@DATA_DIR@", dataDirRoot);
                e.setValue(string);
            }
        }
        
        //TODO: namespace?
        params.put("namespace", namespace.getURI());
        info.getConnectionParameters().putAll(params);
        
    }
}
