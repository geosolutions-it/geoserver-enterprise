/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.Collection;
import java.util.List;

import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.impl.CatalogAddEventImpl;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogPostModifyEventImpl;
import org.geoserver.platform.GeoServerResourceLoader;
import org.opengis.feature.type.Name;

/**
 * The GeoServer catalog which provides access to meta information about the 
 * data served by GeoServer.
 * <p>
 * The following types of metadata are stored:
 * <ul>
 *   <li>namespaces and workspaces
 *   <li>coverage (raster) and data (vector) stores
 *   <li>coverages and feature resoures
 *   <li>styles 
 * </ul>
 * </p>
 * <h2>Workspaces</h2>
 * <p>
 * A workspace is a container for any number of stores. All workspaces can be
 * obtained with the {@link #getWorkspaces()}. A workspace is identified by its 
 * name ({@link WorkspaceInfo#getName()}). A workspace can be looked up by its 
 * name with the {@link #getWorkspaceByName(String)} method. 
 * </p>
 * <h2>Stores</h2>
 * <p>
 *  The {@link #getStores(Class)} method provides access to all the stores in 
 *  the catalog of a specific type. For instance, the following would obtain 
 *  all datstores from the catalog:
 *  <pre>
 *  //get all datastores
 *  List<DataStoreInfo> dataStores = catalog.getStores( DataStoreInfo.class );
 *  </pre>
 *  The methods {@link #getDataStores()} and {@link #getCoverageStores()} provide
 *  a convenience for the two well known types.
 * </p>
 * <p>
 *  A store is contained within a workspace (see {@link StoreInfo#getWorkspace()}).
 *  The {@link #getStoresByWorkspace(WorkspaceInfo, Class)} method for only stores 
 *  contained with a specific workspace. For instance, the following would obtain 
 *  all datastores store within a particular workspace:
 *  <pre>
 *  //get a workspace
 *  WorkspaceInfo workspace = catalog.getWorkspace( "myWorkspace" );
 * 
 *  //get all datastores in that workspace
 *  List<DataStoreInfo> dataStores = catalog.getStoresByWorkspace( workspace, DataStoreInfo.class );
 *  </pre>
 * </p>
 * <h2>Resources</h2>
 * <p>
 * The {@link #getResources(Class)} method provides access to all resources in 
 * the catalog of a particular type. For instance, to acess all feature types in 
 * the catalog:
 * <pre>
 *  List<FeatureTypeInfo> featureTypes = catalog.getResources( FeatureTypeInfo.class );
 * </pre>
 * The {@link #getFeatureTypes()} and {@link #getCoverages()} methods are a convenience
 * for the well known types.
 * </p>
 * <p>
 * A resource is contained within a namespace, therefore it is identified by a 
 * namespace uri, local name pair. The {@link #getResourceByName(String, String, Class)} 
 * method provides access to a resource by its namespace qualified name. The method
 * {@link #getResourceByName(String, Class)} provides access to a resource by its 
 * unqualified name. The latter method will do an exhaustive search of all namespaces
 * for a resource with the specified name. If only a single resoure with the name
 * is found it is returned. Some examples:
 * <pre>
 *   //get a feature type by its qualified name
 *   FeatureTypeInfo ft = catalog.getResourceByName( 
 *       "http://myNamespace.org", "myFeatureType", FeatureTypeInfo.class );
 *       
 *   //get a feature type by its unqualified name
 *   ft = catalog.getResourceByName( "myFeatureType", FeatureTypeInfo.class );
 *   
 *   //get all feature types in a namespace
 *   NamespaceInfo ns = catalog.getNamespaceByURI( "http://myNamespace.org" );
 *   List<FeatureTypeInfo> featureTypes = catalog.getResourcesByNamespace( ns, FeatureTypeINfo.class );
 *  </pre>
 * </p>
 * <h2>Layers</h2>
 * <p>
 * A layers is used to publish a resource. The {@link #getLayers()} provides access 
 * to all layers in the catalog. A layer is uniquely identified by its name. The
 * {@link #getLayerByName(String)} method provides access to a layer by its name.
 * The {@link #getLayers(ResourceInfo)} return all the layers publish a specific 
 * resource. Some examples:
 * <pre>
 *  //get a layer by its name
 *  LayerInfo layer = catalog.getLayer( "myLayer" );
 * 
 *  //get all the layers for a particualr feature type
 *  FeatureTypeInfo ft = catalog.getFeatureType( "http://myNamespace", "myFeatureType" );
 *  List<LayerInfo> layers = catalog.getLayers( ft );
 *  
 * </pre>
 *
 * </p>
 * <h2>Modifing the Catalog</h2>
 * <p>
 * Catalog objects such as stores and resoures are mutable and can be modified.
 * However, any modifications made on an object do not apply until they are saved.
 * For instance, consider the following example of modifying a feature type: 
 * <pre>
 *  //get a feature type
 *  FeatureTypeInfo featureType = catalog.getFeatureType( "http://myNamespace.org", "myFeatureType" );
 *  
 *  //modify it
 *  featureType.setBoundingBox( new Envelope(...) );
 *  
 *  //save it
 *  catalog.save( featureType );
 * </pre>
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning project
 */
public interface Catalog extends CatalogInfo {
    
    /**
     * The reserved keyword used to identify the default workspace or the default store
     */
    public static String DEFAULT = "default";

    /**
     * The data access facade.
     */
    CatalogFacade getFacade();
    
    /**
     * The factory used to create catalog objects.
     * 
     */
    CatalogFactory getFactory();

    /**
     * Adds a new store.
     */
    void add(StoreInfo store);

    /**
     * Validate a store.
     *
     * @param store the StoreInfo to be validated
     * @param isNew a boolean; if true then an existing store with the same
     *     name and workspace will cause a validation error.
     * 
     * @returns List<RuntimeException> non-empty if validation fails
     */
    List<RuntimeException> validate(StoreInfo store, boolean isNew);

    /**
     * Removes an existing store.
     */
    void remove(StoreInfo store);

    /**
     * Saves a store that has been modified.
     */
    void save(StoreInfo store);

    /**
     * Detaches the store from the catalog.
     * <p>
     * This method does not remove the object from the catalog, it "unnattaches" the object 
     * resolving any proxies.
     * </p>
     * <p>
     * In the even the specified object does not exist in the catalog it itself should be returned,
     * this method should never return null.
     * </p>
     */
    <T extends StoreInfo> T detach(T store);
        
    /**
     * Returns the store with the specified id.
     * <p>
     * <tt>clazz</td> is used to determine the implementation of StoreInfo 
     * which should be returned. An example which would return a data store.
     * <pre>
     *   <code>
     * DataStoreInfo dataStore = catalog.getStore(&quot;id&quot;, DataStoreInfo.class);
     * </code>
     * </pre>
     * </p>
     * @param id The id of the store.
     * @param clazz The class of the store to return.
     * 
     * @return The store matching id, or <code>null</code> if no such store exists.
     */
    <T extends StoreInfo> T getStore(String id, Class<T> clazz);

    /**
     * Returns the store with the specified name. 
     * <p>
     * <tt>clazz</td> is used to determine the implementation of StoreInfo 
     * which should be returned. An example which would return a data store.
     * </p>
     * <p>
     * <pre>
     *   getStoreByName(null,name,clazz);
     * </pre>
     * </p>
     * @param name The name of the store. The name can be {@code null} or {@link #DEFAULT} to
     * retrieve the default store in the default workspace.
     * @param clazz The class of the store to return.
     * 
     * @return The store matching name, or <code>null</code> if no such store exists.
     */
    <T extends StoreInfo> T getStoreByName(String name, Class<T> clazz);

    /**
     * Returns the store with the specified name in the specified workspace. 
     * <p>
     * <tt>clazz</td> is used to determine the implementation of StoreInfo 
     * which should be returned. An example which would return a data store.
     * </p>
     * <p>
     * <pre>
     *   <code>
     *   DataStoreInfo dataStore = catalog.getStore(&quot;workspaceName&quot;, &quot;name&quot;, DataStoreInfo.class);
     * </code>
     * </pre>
     * </p>
     * @param workspaceName The name of the workspace containing the store. The name can be {@code null} 
     * or {@link #DEFAULT} to identify the default workspace.
     * @param name The name of the store. The name can be 
     * {@code null} or {@link #DEFAULT} to retrieve the default store in the specified workspace.
     * @param clazz The class of store to return.
     * 
     * @return The store matching name, or <code>null</code> if no such store e xists.
     */
    <T extends StoreInfo> T getStoreByName(String workspaceName, String name, Class<T> clazz );

    /**
     * Returns the store with the specified name in the specified workspace.
     * <p>
     * <tt>clazz</td> is used to determine the implementation of StoreInfo 
     * which should be returned. An example which would return a data store.
     * </p>
     * <p>
     * <pre>
     *   <code>
     *   WorkspaceInfo workspace = ...;
     *   DataStoreInfo dataStore = catalog.getStore(workspace, &quot;name&quot; , DataStoreInfo.class);
     * </code>
     * </pre>
     * </p>
     * @param workspace The workspace containing the store.
     * @param name The name of the store. The name can be {@code null} or {@link #DEFAULT} to
     * retrieve the default store in the default workspace.
     * @param clazz The class of store to return.
     * 
     * @return The store matching name, or <code>null</code> if no such store exists.
     */
    <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name, Class<T> clazz );

    /**
     * All stores in the catalog of the specified type.
     * 
     * <p>
     * The <tt>clazz</tt> parameter is used to filter the types of stores
     * returned. An example which would return all data stores:
     * 
     * <pre> 
     *   <code>
     * catalog.getStores(DataStoreInfo.class);
     * </code> 
     * </pre>
     * 
     * </p>
     * 
     */
    <T extends StoreInfo> List<T> getStores(Class<T> clazz);

    /**
     * All stores in the specified workspace of the given type.
     * <p>
     * The <tt>clazz</tt> parameter is used to filter the types of stores
     * returned. An example which would return all data stores in a specific 
     * workspace:
     * <pre> 
     *   <code>
     * WorkspaceInfo workspace = ...;
     * List<DataStoreInfo> dataStores = 
     *     catalog.getStores(workspace, DataStoreInfo.class);
     * </code> 
     * </pre>
     * 
     * </p>
     * 
     * @param workspace The workspace containing returned stores, may be null to specify the default workspace.
     * @param clazz The type of stores to lookup.
     * 
     */
    <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo workspace,Class<T> clazz);

    /**
     * All stores in the specified workspace of the given type.
     * <p>
     * This method is convenience for:
     * <pre>
     *   WorkspaceInfo ws = catalog.getWorkspaceByName( workspaceName );
     *   getStoresByWorkspace( ws , clazz ); 
     * </pre>
     * </p>
     * <p>
     * The <tt>clazz</tt> parameter is used to filter the types of stores
     * returned.
     * </p>
     * 
     * @param workspaceName The name of the workspace containing returned store s, may be null
     *   to specify the default workspace.
     * @param clazz The type of stores to lookup.
     * 
     */
    <T extends StoreInfo> List<T> getStoresByWorkspace(String workspaceName,Class<T> clazz);

    /**
     * Returns a datastore matching a particular id, or <code>null</code> if
     * no such data store could be found.
     * <p>
     * This method is convenience for:
     * <pre>
     * getStore( id, DataStoreInfo.class );
     * </pre>
     * </p>
     */
    DataStoreInfo getDataStore(String id);

    /**
     * Returns a datastore matching a particular name in the default workspace,o     * or <code>null</code> if no such data store could be found.
     * <p>
     * This method is a convenience for:
     *  <pre>
     *  getDataStoreStoreByName(null,name);
     * </pre>
     * </p>
     *
     */
    DataStoreInfo getDataStoreByName(String name);

    /**
     * Returns the datastore matching a particular name in the specified worksp ace, 
     * or <code>null</code> if no such datastore could be found.
     * <p>
     * This method is convenience for:
     * <pre>
     *   WorkspaceInfo ws = catalog.getWorkspace( workspaceName );
     *   return catalog.getDataStoreByName(ws,name);
     * </pre>
     * </p>
     * @param name The name of the datastore.
     * @param workspaceName The name of the workspace containing the datastore, may be <code>null</code> 
     * to specify the default workspace. 
     *
     * @return The store matching the name, or null if no such store could be f ound. 
     */
    DataStoreInfo getDataStoreByName(String workspaceName, String name);

    /**
     * Returns the datastore matching a particular name in the specified worksp ace, 
     * or <code>null</code> if no such datastore could be found.
     * 
     * @param name The name of the datastore.
     * @param workspace The workspace containing the datastore, may be <code>nu ll</code> to
     * specify the default workspace. 
     *
     * @return The store matching the name, or null if no such store could be f ound. 
     */
    DataStoreInfo getDataStoreByName(WorkspaceInfo workspace, String name);

    /**
     * All data stores in the specified workspace.
     * <p>
     * This method is equivalent to:
     * <pre>
     * getStoresByWorkspace( workspaceName, DataStoreInfo.class );
     * </pre>
     * </p>
     * @param workspaceName The name of the workspace.
     */
    List<DataStoreInfo> getDataStoresByWorkspace( String workspaceName );

    /**
     * All data stores in the specified workspace.
     * <p>
     * This method is equivalent to:
     * <pre>
     * getStoresByWorkspace( workspace, DataStoreInfo.class );
     * </pre>
     * </p>
     * @param workspace The name of the workspace.
     */
    List<DataStoreInfo> getDataStoresByWorkspace( WorkspaceInfo workspace );

    /**
     * All data stores in the catalog.
     * <p>
     * The resulting list should not be modified to add or remove stores, the 
     * {@link #add(StoreInfo)} and {@link #remove(StoreInfo)} are used for this
     * purpose.
     * </p>
     */
    List<DataStoreInfo> getDataStores();
    
    /**
     * The default datastore for the specified workspace
     * 
     */
    DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace);

    /**
     * Sets the default data store in the specified workspace
     * 
     * @param workspace
     * @param defaultStore
     */
    void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo defaultStore);

    /**
     * Returns a coverage store matching a particular id, or <code>null</code>
     * if no such coverage store could be found.
     * <p>
     * This method is convenience for:
     * <pre>
     * getStore( id, CoverageStoreInfo.class );
     * </pre>
     * </p>
     */
    CoverageStoreInfo getCoverageStore(String id);

    /**
     * Returns a coverage store matching a particular name, or <code>null</code>
     * if no such coverage store could be found.
     * <p>
     * This method is a convenience for: <code>
     *   <pre>
     * getCoverageStoreByName(name, CoverageStoreInfo.class)
     * </pre>
     * </code>
     * </p>
     * 
     */
    CoverageStoreInfo getCoverageStoreByName(String name);

    /**
     * Returns the coveragestore matching a particular name in the specified workspace, 
     * or <code>null</code> if no such coveragestore could be found.
     * <p>
     * This method is convenience for:
     * <pre>
     *   WorkspaceInfo ws = catalog.getWorkspace( workspaceName );
     *   return catalog.getCoverageStoreByName(ws,name);
     * </pre>
     * </p>
     * @param name The name of the coveragestore.
     * @param workspaceName The name of the workspace containing the coveragestore, may be <code>null</code> 
     * to specify the default workspace. 
     *
     * @return The store matching the name, or null if no such store could be found. 
     */
    CoverageStoreInfo getCoverageStoreByName(String workspaceName, String name);
    
    /**
     * Returns the coverageStore matching a particular name in the specified workspace, 
     * or <code>null</code> if no such coverageStore could be found.
     * 
     * @param name The name of the coverageStore.
     * @param workspace The workspace containing the coverageStore, may be <code>null</code> to
     * specify the default workspace. 
     *
     * @return The store matching the name, or null if no such store could be found. 
     */
    CoverageStoreInfo getCoverageStoreByName(WorkspaceInfo workspace, String name);
    
    /**
     * All coverage stores in the specified workspace.
     * <p>
     * This method is equivalent to:
     * <pre>
     * getStoresByWorkspace( workspaceName, CoverageStoreInfo.class );
     * </pre>
     * </p>
     * @param workspaceName The name of the workspace.
     */
    List<CoverageStoreInfo> getCoverageStoresByWorkspace(String workspaceName);
    
    /**
     * All coverage stores in the specified workspace.
     * <p>
     * This method is equivalent to:
     * <pre>
     * getStoresByWorkspace( workspace, CoverageStoreInfo.class );
     * </pre>
     * </p>
     * @param workspace The name of the workspace.
     */
    List<CoverageStoreInfo> getCoverageStoresByWorkspace(WorkspaceInfo workspace);
    
    /**
     * All coverage stores in the catalog.
     *<p>
     * The resulting list should not be modified to add or remove stores, the 
     * {@link #add(StoreInfo)} and {@link #remove(StoreInfo)} are used for this
     * purpose.
     * </p>
     */
    List<CoverageStoreInfo> getCoverageStores();

    /**
     * Returns the resource with the specified id.
     * <p>
     * <tt>clazz</td> is used to determine the implementation of ResourceInfo 
     * which should be returned. An example which would return a feature type.
     * <pre>
     *   <code>
     * FeatureTypeInfo ft = catalog.getResource(&quot;id&quot;, FeatureTypeInfo.class);
     * </code>
     * </pre>
     * </p>
     * @param id The id of the resource.
     * @param clazz The class of the resource to return.
     * 
     * @return The resource matching id, or <code>null</code> if no such resource exists.
     */
    <T extends ResourceInfo> T getResource(String id, Class<T> clazz);

    /**
     * Looks up a resource by qualified name.
     * <p>
     * <tt>ns</tt> may be specified as a namespace prefix or uri.
     * </p>
     * <p>
     * <tt>clazz</td> is used to determine the implementation of ResourceInfo 
     * which should be returned.
     * </p>
     * @param ns
     *                The prefix or uri to which the resource belongs, may be
     *                <code>null</code>.
     * @param name
     *                The name of the resource.
     * @param clazz
     *                The class of the resource.
     * 
     * @return The resource matching the name, or <code>null</code> if no such
     *         resource exists.
     */
    <T extends ResourceInfo> T getResourceByName(String ns, String name, Class<T> clazz);

    /**
     * Looks up a resource by qualified name.
     * <p>
     * <tt>clazz</td> is used to determine the implementation of ResourceInfo 
     * which should be returned.
     * </p>
     * @param ns
     *                The namespace to which the resource belongs, may be
     *                <code>null</code> to specify the default namespace.
     * @param name
     *                The name of the resource.
     * @param clazz
     *                The class of the resource.
     * 
     * @return The resource matching the name, or <code>null</code> if no such
     *         resource exists.
     */
    <T extends ResourceInfo> T getResourceByName(NamespaceInfo ns, String name, Class<T> clazz );

    /**
     * Looks up a resource by qualified name.
     * <p>
     * <tt>clazz</td> is used to determine the implementation of ResourceInfo 
     * which should be returned.
     * </p>
     * 
     * @param <T>
     * @param name The qualified name.
     */
    <T extends ResourceInfo> T getResourceByName(Name name, Class<T> clazz);
    
    /**
     * Looks up a resource by its unqualified name.
     * <p>
     * The lookup rules used by this method are as follows:
     * <ul>
     *  <li>If a resource in the default namespace is found matching the 
     *  specified name, it is returned.
     *  <li>If a single resource among all non-default namespaces is found 
     *  matching the the specified name, it is returned.  
     * </ul>
     * Care should be taken when using this method, use of {@link #getResourceByName(String, String, Class)}
     * is preferred.
     * </p>
     * 
     * @param name The name of the resource.
     * @param clazz The type of the resource.
     * 
     */
    <T extends ResourceInfo> T getResourceByName(String name, Class<T> clazz);
    
    /**
     * Adds a new resource.
     */
    void add(ResourceInfo resource);

    /**
     * Validate a resource.
     *
     * @param resource the ResourceInfo to be validated
     * @param isNew a boolean; if true then an existing resource with the same
     *     name and store will cause a validation error.
     * 
     * @returns List<RuntimeException> non-empty if validation fails
     */
    List<RuntimeException> validate(ResourceInfo resource, boolean isNew);

    /**
     * Removes an existing resource.
     */
    void remove(ResourceInfo resource);

    /**
     * Saves a resource which has been modified.
     */
    void save(ResourceInfo resource);

    /**
     * Detatches the resource from the catalog.
     * <p>
     * This method does not remove the object from the catalog, it "unnattaches" the object 
     * resolving any proxies.
     * </p>
     * <p>
     * In the even the specified object does not exist in the catalog it itself should be returned,
     * this method should never return null.
     * </p>
     */
    <T extends ResourceInfo> T detach(T resource);
    
    /**
     * All resources in the catalog of the specified type.
     * <p>
     * The <tt>clazz</tt> parameter is used to filter the types of resources
     * returned. An example which would return all feature types:
     * 
     * <pre> 
     *   <code>
     * catalog.getResources(FeatureTypeInfo.class);
     * </code> 
     * </pre>
     * 
     * </p>
     * 
     */
    <T extends ResourceInfo> List<T> getResources(Class<T> clazz);

    /**
     * All resources in the specified namespace of the specified type.
     * <p>
     * The <tt>clazz</tt> parameter is used to filter the types of resources
     * returned. An example which would return all feature types:
     * </p>
     * 
     * @param namespace
     *                The namespace.
     * @param clazz
     *                The class of resources returned.
     * 
     * @return List of resources of the specified type in the specified
     *         namespace.
     */
    <T extends ResourceInfo> List<T> getResourcesByNamespace(
            NamespaceInfo namespace, Class<T> clazz);
    
    /**
     * All resources in the specified namespace of the specified type.
     * <p>
     * The <tt>namespace</tt> may specify the prefix, or the uri of the namespace.
     * </p>
     * <p>
     * The <tt>clazz</tt> parameter is used to filter the types of resources
     * returned. An example which would return all feature types:
     * </p>
     * <p>
     * This method is convenience for:
     * 
     * <pre>
     * NamespaceInfo ns = getNamespace( namespace );
     * return getResourcesByNamespace(ns,clazz);
     * </pre>
     * </p>
     * 
     * @param namespace
     *                The namespace.
     * @param clazz
     *                The class of resources returned.
     * 
     * @return List of resources of the specified type in the specified
     *         namespace.
     */
    <T extends ResourceInfo> List<T> getResourcesByNamespace(
            String namespace, Class<T> clazz
    );
    

    /**
     * Returns the resource with the specified name originating from the store.
     * 
     * @param store The store.
     * @param name The name of the resource.
     * @param clazz The class of resource.
     */
    <T extends ResourceInfo> T getResourceByStore(StoreInfo store, String name, Class<T> clazz);

    /**
     * All resources which originate from the specified store, of the specified type.
     *
     * @param store The store to obtain resources from.
     * @param clazz The class of resources returned.
     * 
     * @return List of resources of the specified type from the specified store
     */
    <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store, Class <T> clazz);

    /**
     * Returns the feature type matching a particular id, or <code>null</code>
     * if no such feature type could be found.
     * <p>
     * This method is convenience for:
     * <pre>
     * getResource( id, FeatureTypeInfo.class );
     * </pre>
     * </p>
     * @return The feature type matching the id, or <code>null</code> if no
     *         such resource exists.
     */
    FeatureTypeInfo getFeatureType(String id);

    /**
     * Looks up a feature type by qualified name.
     * <p>
     * <tt>ns</tt> may be specified as a namespace prefix or uri.
     * </p>
     * <p>
     * This method is convenience for:
     * <pre>
     * getResourceByName( ns, name, FeatureTypeInfo.class );
     * </pre>
     * </p>
     * 
     * @param ns
     *                The prefix or uri to which the feature type belongs, may
     *                be <code>null</code> to specify the default namespace.
     * @param name
     *                The name of the feature type.
     * 
     * @return The feature type matching the name, or <code>null</code> if no
     *         such resource exists.
     */
    FeatureTypeInfo getFeatureTypeByName(String ns, String name);

    /**
     * Looks up a feature type by qualified name.
     * <p>
     * This method is convenience for:
     * <pre>
     * getResourceByName( ns, name, FeatureTypeInfo.class );
     * </pre>
     * </p>
     * 
     * @param ns
     *                The namespace to which the feature type belongs, may
     *                be <code>null</code> to specify the default namespace.
     * @param name
     *                The name of the feature type.
     * 
     * @return The feature type matching the name, or <code>null</code> if no
     *         such resource exists.
     */
    FeatureTypeInfo getFeatureTypeByName(NamespaceInfo ns, String name);
    
    /**
     * Looks up a feature type by qualified name.
     * <p>
     * This method is convenience for:
     * <pre>
     * getResourceByName( name, FeatureTypeInfo.class );
     * </pre>
     * </p>
     * @param name The qualified name.
     */
    FeatureTypeInfo getFeatureTypeByName(Name name);
    
    /**
     * Looks up a feature type by an unqualified name.
     * <p>
     * The lookup rules used by this method are as follows:
     * <ul>
     *  <li>If a feature type in the default namespace is found matching the 
     *  specified name, it is returned.
     *  <li>If a single feature type among all non-default namespaces is found 
     *  matching the the specified name, it is returned.  
     * </ul>
     * Care should be taken when using this method, use of {@link #getFeatureTypeByName(String, String)}
     * is preferred.
     * </p>
     * 
     * @param name The name of the feature type.
     * 
     * @return The single feature type matching the specified name, or <code>null</code>
     * if either none could be found or multiple were found.
     */
    FeatureTypeInfo getFeatureTypeByName( String name );

    /**
     * ALl feature types in the catalog.
     * <p>
     * This method is a convenience for:
     * 
     * <pre>
     * 	<code>
     * getResources(FeatureTypeInfo.class);
     * </code>
     * </pre>
     * 
     * </p>
     * <p>
     * The resulting list should not be used to add or remove resources from 
     * the catalog, the {@link #add(ResourceInfo)} and {@link #remove(ResourceInfo)}
     * methods are used for this purpose.
     * </p>
     */
    List<FeatureTypeInfo> getFeatureTypes();

    /**
     * All feature types in the specified namespace.
     * <p>
     * This method is a convenience for: <code>
     *   <pre>
     * getResourcesByNamespace(namespace, FeatureTypeInfo.class);
     * </pre>
     * </code>
     * </p>
     * 
     * @param namespace
     *                The namespace of feature types to return.
     * 
     * @return All the feature types in the specified namespace.
     */
    List<FeatureTypeInfo> getFeatureTypesByNamespace(NamespaceInfo namespace);

    /**
     * @deprecated use {@link #getFeatureTypesByDataStore(DataStoreInfo)}
     */
    FeatureTypeInfo getFeatureTypeByStore(DataStoreInfo dataStore, String name );
    
    /**
     * Returns the feature type with the specified name which is part of the sp ecified
     * data store.
     *  <p>
     *  This method is convenience for:
     *  <pre>
     *  return getResourceByStore(dataStore,name,FeatureTypeInfo.class);
     *  </pre>
     *  </p>
     * @param dataStore The data store.
     * @param name The feature type name.
     * 
     * @return The feature type, or <code>null</code> if no such feature type e xists.
     */
    FeatureTypeInfo getFeatureTypeByDataStore(DataStoreInfo dataStore, String name );

    /**
     * @deprecated use {@link #getFeatureTypesByDataStore(DataStoreInfo)}
     */
    List<FeatureTypeInfo> getFeatureTypesByStore(DataStoreInfo store);
    
    /**
     * All feature types which originate from the specified datastore. 
     * 
     * @param store The datastore.
     * 
     * @return A list of feature types which originate from the datastore.
     */
    List<FeatureTypeInfo> getFeatureTypesByDataStore(DataStoreInfo store);

    /**
     * Returns the coverage matching a particular id, or <code>null</code> if
     * no such coverage could be found.
     * <p>
     * This method is a convenience for:
     * <pre>
     * getResource( id, CoverageInfo.class );
     * </pre>
     * </p>
     */
    CoverageInfo getCoverage(String id);

    /**
     * Looks up a coverage by qualified name.
     * <p>
     * <tt>ns</tt> may be specified as a namespace prefix or uri.
     * </p>
     * <p>
     * This method is convenience for:
     * <pre>
     * getResourceByName(ns,name,CoverageInfo.class);
     * </pre>
     * </p>
     * 
     * @param ns
     *                The prefix or uri to which the coverage belongs, may be
     *                <code>null</code>.
     * @param name
     *                The name of the coverage.
     * 
     * @return The coverage matching the name, or <code>null</code> if no such
     *         resource exists.
     */
    CoverageInfo getCoverageByName(String ns, String name);

    /**
     * Looks up a coverage by qualified name.
     * <p>
     * This method is convenience for:
     * <pre>
     * getResourceByName(ns,name,CoverageInfo.class);
     * </pre>
     * </p>
     * 
     * @param ns
     *                The namespace to which the coverage belongs, may be
     *                <code>null</code>.
     * @param name
     *                The name of the coverage.
     * 
     * @return The coverage matching the name, or <code>null</code> if no such
     *         resource exists.
     */
    CoverageInfo getCoverageByName(NamespaceInfo ns, String name);
    
    /**
     * Looks up a coverage by qualified name.
     * <p>
     * This method is convenience for:
     * <pre>
     * getResourceByName(name,CoverageInfo.class);
     * </pre>
     * </p>
     * @param name The qualified name.
     */
    CoverageInfo getCoverageByName(Name name);
    
    /**
     * Looks up a coverage by an unqualified name.
     * <p>
     * The lookup rules used by this method are as follows:
     * <ul>
     *  <li>If a coverage in the default namespace is found matching the 
     *  specified name, it is returned.
     *  <li>If a single coverage among all non-default namespaces is found 
     *  matching the the specified name, it is returned.  
     *  
     * </ul>
     * Care should be taken when using this method, use of {@link #getCoverageByName(String, String)}
     * is preferred.
     * </p>
     * 
     * @param name The name of the coverage.
     * 
     * @return The single coverage matching the specified name, or <code>null</code>
     * if either none could be found or multiple were found. 
     */
    CoverageInfo getCoverageByName( String name );
    
    /**
     * All coverages in the catalog.
     * <p>
     * This method is a convenience for:
     * 
     * <pre>
     * 	<code>
     * getResources(CoverageInfo.class);
     * </code>
     * </pre>
     * 
     * </p>
     * 
     * <p>
     * This method should not be used to add or remove coverages from the 
     * catalog. The {@link #add(ResourceInfo)} and {@link #remove(ResourceInfo)} 
     * methods are used for this purpose. 
     * </p>
     */
    List<CoverageInfo> getCoverages();

    /**
     * All coverages in the specified namespace.
     * <p>
     * This method is a convenience for: <code>
     *   <pre>
     * getResourcesByNamespace(namespace, CoverageInfo.class);
     * </pre>
     * </code>
     * </p>
     * 
     * @param namespace
     *                The namespace of coverages to return.
     * 
     * @return All the coverages in the specified namespace.
     */
    List<CoverageInfo> getCoveragesByNamespace(NamespaceInfo namespace);
    
    /**
     * Returns the coverage with the specified name which is part of the specified
     * coverage store.
     *  <p>
     *  This method is convenience for:
     *  <pre>
     *  return getResourceByStore(coverageStore,name,CoverageInfo.class);
     *  </pre>
     *  </p>
     * @param coverageStore The coverage store.
     * @param name The coverage name.
     * 
     * @return The coverage, or <code>null</code> if no such coverage exists.
     */
    CoverageInfo getCoverageByCoverageStore( CoverageStoreInfo coverageStore, String name);
    
    /**
     * All coverages which originate from the specified coveragestore. 
     * 
     * @param store The coveragestore.
     * 
     * @return A list of coverages which originate from the coveragestore.
     */
    List<CoverageInfo> getCoveragesByCoverageStore(CoverageStoreInfo store);
    
    /**
     * Adds a new layer.
     */
    void add(LayerInfo layer);

    /**
     * Validate a layer.
     *
     * @param layer the LayerInfo to be validated
     * @param isNew a boolean; if true then an existing layer with the same
     *     name will cause a validation error.
     * 
     * @returns List<RuntimeException> non-empty if validation fails
     */
    List<RuntimeException> validate(LayerInfo layer, boolean isNew);

    /**
     * Removes an existing layer.
     */
    void remove(LayerInfo layer);

    /**
     * Saves a layer which has been modified.
     */
    void save(LayerInfo layer);
    
    /**
     * Detatches the layer from the catalog.
     * <p>
     * This method does not remove the object from the catalog, it "unnattaches" the object 
     * resolving any proxies.
     * </p>
     * <p>
     * In the even the specified object does not exist in the catalog it itself should be returned,
     * this method should never return null.
     * </p>
     */
    LayerInfo detach(LayerInfo layer);
    
    /**
     * All coverages which are part of the specified store.
     */
    List<CoverageInfo> getCoveragesByStore(CoverageStoreInfo store);
    
    /**
     * Returns the layer matching a particular id, or <code>null</code> if no
     * such layer could be found.
     */
    LayerInfo getLayer(String id);

    /**
     * Returns the layer matching a particular name, or <code>null</code> if no
     * such layer could be found. 
     */
    LayerInfo getLayerByName( String name );
    
    /**
     * Returns the layer matching a particular qualified name.
     */
    LayerInfo getLayerByName( Name name );
    
    /**
     * All layers in the catalog.
     * <p>
     * The resulting list should not be used to add or remove layers to or from
     * the catalog, the {@link #add(LayerInfo)} and {@link #remove(LayerInfo)}
     * methods are used for this purpose.
     * </p>
     * 
     */
    List<LayerInfo> getLayers();

    /**
     * All layers in the catalog that publish the specified resource.
     * 
     * @param resource The resource.
     * 
     * @return A list of layers for the resource, or an empty list.
     */
    List<LayerInfo> getLayers( ResourceInfo resource );
    
    /**
     * All layers which reference the specified style.
     * 
     * @param style The style.
     * 
     * @return A list of layers which reference the style, or an empty list.
     */
    List<LayerInfo> getLayers( StyleInfo style );
    
    /**
     * Adds a new map.
     */
    void add(MapInfo map);

    /**
     * Removes an existing map.
     */
    void remove(MapInfo map);

    /**
     * Saves a map which has been modified.
     */
    void save( MapInfo map);
    
    /**
     * Detatches the map from the catalog.
     * <p>
     * This method does not remove the object from the catalog, it "unnattaches" the object 
     * resolving any proxies.
     * </p>
     * <p>
     * In the even the specified object does not exist in the catalog it itself should be returned,
     * this method should never return null.
     * </p>
     */
    MapInfo detach(MapInfo map);

    /**
     * All maps in the catalog.
     * <p>
     * The resulting list should not be used to add or remove maps to or from
     * the catalog, the {@link #add(MapInfo)} and {@link #remove(MapInfo)} methods
     * are used for this purpose.
     * </p>
     */
    List<MapInfo> getMaps();

    /**
     * Returns the map matching a particular id, or <code>null</code> if no
     * such map could be found.
     */
    MapInfo getMap(String id);
    
    /**
     * Returns the map matching a particular name, or <code>null</code> if no
     * such map could be found.
     */
    MapInfo getMapByName(String name);

    /**
     * Adds a layer group to the catalog.
     */
    void add(LayerGroupInfo layerGroup);

    /**
     * Validate a layergroup.
     *
     * @param layerGroup the LayerGroupInfo to be validated
     * @param isNew a boolean; if true then an existing layergroup with the same
     *     name will cause a validation error.
     * 
     * @returns List<RuntimeException> non-empty if validation fails
     */
    List<RuntimeException> validate(LayerGroupInfo layerGroup, boolean isNew);
    
    /**
     * Removes a layer group from the catalog.
     */
    void remove(LayerGroupInfo layerGroup);
    
    /**
     * Saves changes to a modified layer group.
     */
    void save(LayerGroupInfo layerGroup);
    
    /**
     * Detatches the layer group from the catalog.
     * <p>
     * This method does not remove the object from the catalog, it "unnattaches" the object 
     * resolving any proxies.
     * </p>
     * <p>
     * In the even the specified object does not exist in the catalog it itself should be returned,
     * this method should never return null.
     * </p>
     */
    LayerGroupInfo detach(LayerGroupInfo layerGroup);
    
    /**
     * All layer groups in the catalog.
     */
    List<LayerGroupInfo> getLayerGroups();

    /**
     * All layer groups in the specified workspace.
     * 
     * @param workspaceName The name of the workspace containing layer groups.
     */
    List<LayerGroupInfo> getLayerGroupsByWorkspace(String workspaceName);

    /**
     * All layer groups in the specified workspace.
     * 
     * @param workspace The workspace containing layer groups.
     */
    List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace);

    /**
     * Returns the layer group matching a particular id, or <code>null</code> if no
     * such group could be found.
     */
    LayerGroupInfo getLayerGroup(String id);
    
    /**
     * Returns the global layer group matching a particular name, or <code>null</code> if no such
     * style could be found.
     * <p>
     * If {@code prefixedName} contains a workspace name prefix (like in {@code topp:tasmania}, the
     * layer group will be looked up on that specific workspace ({@code topp}), otherwise it is
     * assumed a global (with no workspace) layer group.
     * </p>
     * 
     * @param name the name of the layer group, may include a workspace name prefix or not.
     * @return the global layer group matching a particular name, or <code>null</code> if no such
     *         group could be found.
     */
    LayerGroupInfo getLayerGroupByName(String name);
    
    /**
     * Returns the layer group matching a particular name in the specified workspace, or
     * <code>null</code> if no such layer group could be found.
     * 
     * @param workspaceName The name of the workspace containing the layer group, {@code null} is
     *        allowed, meaning to look up for a global layer group
     * @param name The name of the layer group to return.
     */
    LayerGroupInfo getLayerGroupByName(String workspaceName, String name);

    /**
     * Returns the layer group matching a particular name in the specified workspace, or
     * <code>null</code> if no such layer group could be found.
     * 
     * @param workspace The workspace containing the layer group, {@code null} is allowed, meaning
     *        to look up for a global layer group.
     * @param name The name of the layer group to return.
     */
    LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name);
    
    /**
     * Adds a new style.
     * 
     */
    void add(StyleInfo style);

    /**
     * Validate a style.
     *
     * @param style the StyleInfo to be validated
     * @param isNew a boolean; if true then an existing style with the same
     *     name will cause a validation error.
     * 
     * @returns List<RuntimeException> non-empty if validation fails
     */
    List<RuntimeException> validate(StyleInfo style, boolean isNew);

    /**
     * Removes a style.
     */
    void remove(StyleInfo style);

    /**
     * Saves a style which has been modified.
     */
    void save(StyleInfo style);
    
    /**
     * Detatches the style from the catalog.
     * <p>
     * This method does not remove the object from the catalog, it "unnattaches" the object 
     * resolving any proxies.
     * </p>
     * <p>
     * In the even the specified object does not exist in the catalog it itself should be returned,
     * this method should never return null.
     * </p>
     */
    StyleInfo detach(StyleInfo style);
    
    /**
     * Returns the style matching a particular id, or <code>null</code> if no
     * such style could be found.
     */
    StyleInfo getStyle(String id);

    /**
     * Returns the style matching a particular name in the specified workspace, or <code>null</code> 
     * if no such style could be found.
     * 
     * @param workspaceName The name of the workspace containing the style, {@code null} stands for a global style.
     * @param name The name of the style to return.
     */
    StyleInfo getStyleByName(String workspaceName, String name);

    /**
     * Returns the style matching a particular name in the specified workspace, or <code>null</code> 
     * if no such style could be found.
     * 
     * @param workspace The workspace containing the style, {@code null} stands for a global style.
     * @param name The name of the style to return.
     */
    StyleInfo getStyleByName(WorkspaceInfo workspace, String name);
    
    /**
     * Returns the global style matching a particular name, or <code>null</code> if no such style
     * could be found.
     * <p>
     * Note this is a convenient method for {@link #getStyleByName(WorkspaceInfo, String)} with a
     * {@code null} workspace argument.
     * </p>
     * 
     * @param name The name of the style to return.
     */
    StyleInfo getStyleByName(String name);

    /**
     * All styles in the catalog.
     * <p>
     * The resulting list should not be used to add or remove styles, the methods
     * {@link #add(StyleInfo)} and {@link #remove(StyleInfo)} are used for that 
     * purpose.
     *  </p>
     */
    List<StyleInfo> getStyles();

    /**
     * All styles in the specified workspace.
     * 
     * @param workspaceName The name of the workspace containing styles.
     */
    List<StyleInfo> getStylesByWorkspace(String workspaceName);

    /**
     * All styles in the specified workspace.
     * 
     * @param workspace The workspace containing styles.
     */
    List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace);

    /**
     * Adds a new namespace.
     */
    void add(NamespaceInfo namespace);

    /**
     * Validate a namespace.
     *
     * @param namespace the NamespaceInfo to be validated
     * @param isNew a boolean; if true then an existing namespace with the same
     *     prefix will cause a validation error.
     * 
     * @returns List<RuntimeException> non-empty if validation fails
     */
    List<RuntimeException> validate(NamespaceInfo namespace, boolean isNew);

    /**
     * Removes an existing namespace.
     */
    void remove(NamespaceInfo namespace);

    /**
     * Saves a namespace which has been modified.
     */
    void save(NamespaceInfo namespace);
    
    /**
     * Detatches the namespace from the catalog.
     * <p>
     * This method does not remove the object from the catalog, it "unnattaches" the object 
     * resolving any proxies.
     * </p>
     * <p>
     * In the even the specified object does not exist in the catalog it itself should be returned,
     * this method should never return null.
     * </p>
     */
    NamespaceInfo detach(NamespaceInfo namespace);
    
    /**
     * Returns the namespace matching the specified id.
     * 
     */
    NamespaceInfo getNamespace(String id);

    /**
     * Looks up a namespace by its prefix. 
     * 
     * @see NamespaceInfo#getPrefix()
     * @param prefix The namespace prefix, or {@code null} or {@link #DEFAULT} to get the default 
     * namespace
     */
    NamespaceInfo getNamespaceByPrefix(String prefix);

    /**
     * Looks up a namespace by its uri.
     * 
     * @see NamespaceInfo#getURI()
     */
    NamespaceInfo getNamespaceByURI(String uri);

    /**
     * The default namespace of the catalog.
     * 
     */
    NamespaceInfo getDefaultNamespace();

    /**
     * Sets the default namespace of the catalog.
     * 
     * @param defaultNamespace
     *                The defaultNamespace to set.
     */
    void setDefaultNamespace(NamespaceInfo defaultNamespace);

    /**
     * All namespaces in the catalog.
     * <p>
     * The resulting list should not be used to add or remove namespaces from 
     * the catalog, the methods {@link #add(NamespaceInfo)} and {@link #remove(NamespaceInfo)}
     * should be used for that purpose.
     * </p>
     */
    List<NamespaceInfo> getNamespaces();

    /**
     * Adds a new workspace
     */
    void add(WorkspaceInfo workspace);
    
    /**
     * Validate a workspace.
     *
     * @param workspace the WorkspaceInfo to be validated
     * @param isNew a boolean; if true then an existing workspace with the same
     *     name will cause a validation error.
     * 
     * @returns List<RuntimeException> non-empty if validation fails
     */
    List<RuntimeException> validate(WorkspaceInfo workspace, boolean isNew);

    /**
     * Removes an existing workspace.
     */
    void remove(WorkspaceInfo workspace);
    
    /**
     * Saves changes to an existing workspace.
     */
    void save(WorkspaceInfo workspace);
    
    /**
     * Detatches the workspace from the catalog.
     * <p>
     * This method does not remove the object from the catalog, it "unnattaches" the object 
     * resolving any proxies.
     * </p>
     * <p>
     * In the even the specified object does not exist in the catalog it itself should be returned,
     * this method should never return null.
     * </p>
     */
    WorkspaceInfo detach(WorkspaceInfo workspace);
    
    /**
     * The default workspace for the catalog.
     */
    WorkspaceInfo getDefaultWorkspace();
    
    /**
     * Sets the default workspace for the catalog.
     */
    void setDefaultWorkspace( WorkspaceInfo workspace );

    /**
     * All workspaces in the catalog.
     * <p>
     * The resulting list should not be used to add or remove workspaces from 
     * the catalog, the methods {@link #add(WorkspaceInfo)} and {@link #remove(WorkspaceInfo)}
     * should be used for that purpose.
     * </p>
     */
    List<WorkspaceInfo> getWorkspaces();
    
    /**
     * Returns a workspace by id, or <code>null</code> if no such workspace
     * exists.
     */
    WorkspaceInfo getWorkspace( String id );
    
    /**
     * Returns a workspace by name, or <code>null</code> if no such workspace
     * exists.
     * @param The name of the store, or null or {@link #DEFAULT} to get the default workspace
     */
    WorkspaceInfo getWorkspaceByName( String name );

    /**
     * catalog listeners.
     * 
     */
    Collection<CatalogListener> getListeners();

    /**
     * Adds a listener to the catalog.
     */
    void addListener(CatalogListener listener);

    /**
     * Removes a listener from the catalog.
     */
    void removeListener(CatalogListener listener);

    /**
     * Fires the event for an object being added to the catalog.
     * <p>
     * This method should not be called by client code. It is meant to be called
     * interally by the catalog subsystem.
     * </p>
     */
    void fireAdded(CatalogInfo object);
    
    /**
     * Fires the event for an object being modified in the catalog.
     * <p>
     * This method should not be called by client code. It is meant to be called
     * interally by the catalog subsystem.
     * </p>
     */
    void fireModified(CatalogInfo object, List<String> propertyNames, List oldValues,
            List newValues);

    /**
     * Fires the event for an object that was modified in the catalog.
     * <p>
     * This method should not be called by client code. It is meant to be called
     * interally by the catalog subsystem.
     * </p>
     */
    void firePostModified(CatalogInfo object);
    
    /**
     * Fires the event for an object being removed from the catalog.
     * <p>
     * This method should not be called by client code. It is meant to be called
     * interally by the catalog subsystem.
     * </p>
     */
    void fireRemoved(CatalogInfo object);
    
    /**
     * Returns the pool or cache for resources.
     * <p>
     * This object is used to load physical resources like data stores, feature
     * types, styles, etc...
     * </p>
     */
    ResourcePool getResourcePool();

    /**
     * Sets the resource pool reference.
     */
    void setResourcePool( ResourcePool resourcePool );
    
    /**
     * Returns the loader for resources.
     */
    GeoServerResourceLoader getResourceLoader();
    
    /**
     * Sets the resource loader reference.
     */
    void setResourceLoader( GeoServerResourceLoader resourceLoader );
    
    /**
     * Disposes the catalog, freeing up any resources.
     */
    void dispose();
    
    /**
     * Removes all the listeners which are instances of the specified class
     * 
     * @param listenerClass
     */
    public void removeListeners(Class listenerClass);
}
