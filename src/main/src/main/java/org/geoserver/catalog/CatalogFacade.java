package org.geoserver.catalog;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.impl.DefaultCatalogFacade;

/**
 * Data access facade for the catalog.
 * 
 * @author ETj <etj at geo-solutions.it>
 * @author Justin Deoliveira, OpenGeo 
 */
public interface CatalogFacade {

    static WorkspaceInfo ANY_WORKSPACE = DefaultCatalogFacade.ANY_WORKSPACE;
    
    static NamespaceInfo ANY_NAMESPACE = DefaultCatalogFacade.ANY_NAMESPACE;

    static WorkspaceInfo NO_WORKSPACE = DefaultCatalogFacade.NO_WORKSPACE;

    /**
     * The containing catalog.
     */
    Catalog getCatalog();

    /**
     * Sets the containing catalog.
     */
    void setCatalog(Catalog catalog);

    //
    // Stores
    //
    /**
     * Adds a store to persistent storage.
     */
    StoreInfo add(StoreInfo store);

    /**
     * Removes a store to persistent storage.
     */
    void remove(StoreInfo store);

    /**
     * Persists any modifications to a store to persistent storage.
     * <p>
     * DAO implementations are responsible for triggering a {@link CatalogModifyEvent} by calling
     * {@link Catalog#fireModified(CatalogInfo, List, List, List)}. This is the responsibility of 
     * the dao because it is best suited to knowing and tracking which attributes of the object have 
     * changed. 
     * </p>
     */
    void save(StoreInfo store);

    /**
     * Detaches a store from the underlying persistence layer.
     * <p>
     * "Detaching" is specific to the underlying storage engine. But in general when an object 
     * is detached any proxies or uninitialized state of the object should be resolved.
     * </p>
     *
     *  @see Catalog#detach(StoreInfo)
     */
    <T extends StoreInfo> T detach(T store);
    
    /**
     * Loads a store from persistent storage by specifying its identifier.
     * <p>
     * The <tt>clazz</tt> parameter is used to type narrow the returned object to a 
     * specific type of store. If the type of store is unknown the client may simply 
     * specify StoreInfo.class. The dao should still create the correct type of store. 
     * </p>
     * @param id The unique identifier of the store
     * @param clazz The class of the store.
     * 
     * @return The store, or <code>null</code> if no such store exists.
     */
    <T extends StoreInfo> T getStore(String id, Class<T> clazz);

    /**
     * Loads a store from persistent storage by specifying its name and containing workspace.
     * <p>
     * The <tt>clazz</tt> parameter is used to type narrow the returned object to a 
     * specific type of store. If the type of store is unknown the client may simply 
     * specify StoreInfo.class. The dao should still create the correct type of store. 
     * </p>
     * @param workspace THe containing workspace of the store.
     * @param name The name of the store.
     * @param clazz The class of the store.
     * 
     * @return The store, or <code>null</code> if no such store exists.
     */
    <T extends StoreInfo> T getStoreByName(WorkspaceInfo workspace, String name,
            Class<T> clazz);

    /**
     * Loads all stores from persistent storage in the specified workspace.
     * <p>
     * The <tt>clazz</tt> parameter is used to type narrow/filter the returned objects  
     * to a specific type of store. Specifying StoreInfo.class will return all types of 
     * stores. 
     * </p>
     * @param workspace The containing workspace.
     * @param clazz The class of the stores to return.
     * 
     * @return A list of stores, possibly empty.
     */
    <T extends StoreInfo> List<T> getStoresByWorkspace(WorkspaceInfo workspace,
            Class<T> clazz);

    /**
     * Loads all stores from persistent storage.
     * <p>
     * The <tt>clazz</tt> parameter is used to type narrow/filter the returned objects  
     * to a specific type of store. Specifying StoreInfo.class will return all types of 
     * stores. 
     * </p>
     * 
     * @param clazz The class of the stores to return.
     * 
     * @return A list of stores, possibly empty.
     */
    <T extends StoreInfo> List<T> getStores(Class<T> clazz);

    /**
     * Loads the default data store for the specified workspace.
     *  
     * @param workspace The workspace.
     * 
     * @return The default data store, or null if none is set.
     */
    DataStoreInfo getDefaultDataStore(WorkspaceInfo workspace);

    /**
     * Sets the default data store for the specified workspace.
     * <p>
     * DAO implementations are responsible for triggering a {@link CatalogModifyEvent} by calling
     * {@link Catalog#fireModified(CatalogInfo, List, List, List)}. The source of the event is the 
     * workspace itself and changed property is "defaultDataStore". The changed old/new values should 
     * the old/new datastores respectively.  
     * </p>
     * @param workspace The workspace.
     * @param store The default data store.
     */
    void setDefaultDataStore(WorkspaceInfo workspace, DataStoreInfo store);

    //
    // Resources
    //
    /**
     * Adds a resource to persistent storage.
     */
    ResourceInfo add(ResourceInfo resource);

    /**
     * Removes a resource from persistent storage.
     */
    void remove(ResourceInfo resource);

    /**
     * Persists any modifications to a resource to persistent storage.
     * <p>
     * DAO implementations are responsible for triggering a {@link CatalogModifyEvent} by calling
     * {@link Catalog#fireModified(CatalogInfo, List, List, List)}. This is the responsibility of 
     * the dao because it is best suited to knowing and tracking which attributes of the object have 
     * changed. 
     * </p>
     */
    void save(ResourceInfo resource);

    /**
     * Detaches a resource from the underlying persistence layer.
     * <p>
     * "Detaching" is specific to the underlying storage engine. But in general when an object 
     * is detached any proxies or uninitialized state of the object should be resolved.
     * </p>
     *
     *  @see Catalog#detach(ResourceInfo)
     */
    <T extends ResourceInfo> T detach(T resource);
    
    /**
     * Loads a resource from persistent storage by specifying its identifier.
     * <p>
     * The <tt>clazz</tt> parameter is used to type narrow the returned object to a 
     * specific type of resource. If the type of resource is unknown the client may simply 
     * specify ResourceInfo.class. The dao should still create the correct type of resource. 
     * </p>
     * @param id The unique identifier of the resource
     * @param clazz The class of the resource.
     * 
     * @return The resource, or <code>null</code> if no such store exists.
     */
    <T extends ResourceInfo> T getResource(String id, Class<T> clazz);

    /**
     * Loads a resource from persistent storage by specifying its qualified name.
     * <p>
     * The <tt>clazz</tt> parameter is used to type narrow the returned object to a 
     * specific type of resource. If the type of resource is unknown the client may simply 
     * specify ResourceInfo.class. The dao should still create the correct type of resource. 
     * </p>
     * @param namespace The namespace of the resource
     * @param name The local name of the resource
     * @param clazz The class of the resource
     * 
     * @return The resource, or <code>null</code> if no such store exists.
     */
    <T extends ResourceInfo> T getResourceByName(NamespaceInfo namespace,
            String name, Class<T> clazz);

    /**
     * Loads all resources from persistent storage.
     * <p>
     * The <tt>clazz</tt> parameter is used to type narrow/filter the returned objects  
     * to a specific type of store. Specifying StoreInfo.class will return all types of 
     * stores. 
     * </p>
     * 
     * @param clazz The class of the resources to return.
     * 
     * @return A list of resources, possibly empty.
     */
    <T extends ResourceInfo> List<T> getResources(Class<T> clazz);

    /**
     * Loads all resources from persistent storage contained with a specified namespace.
     * <p>
     * The <tt>clazz</tt> parameter is used to type narrow/filter the returned objects  
     * to a specific type of store. Specifying StoreInfo.class will return all types of 
     * stores. 
     * </p>
     * 
     * @param namespace The namespace of resources
     * @param clazz The class of the resources to return.
     * 
     * @return A list of resources, possibly empty.
     */
    <T extends ResourceInfo> List<T> getResourcesByNamespace(NamespaceInfo namespace, Class<T> clazz);

    /**
     * Loads a resource from persistent storage by specifying its name and containing store.
     * <p>
     * The <tt>clazz</tt> parameter is used to type narrow the returned object to a 
     * specific type of resource. If the type of resource is unknown the client may simply 
     * specify ResourceInfo.class. The dao should still create the correct type of resource. 
     * </p>
     * @param store The containing store
     * @param name The local name of the resource
     * @param clazz The class of the resource
     * 
     * @return The resource, or <code>null</code> if no such store exists.
     */
    <T extends ResourceInfo> T getResourceByStore(StoreInfo store, String name,
            Class<T> clazz);

    /**
     * Loads all resources from persistent storage that are contained within a specified store.
     * <p>
     * The <tt>clazz</tt> parameter is used to type narrow/filter the returned objects  
     * to a specific type of store. Specifying StoreInfo.class will return all types of 
     * stores. 
     * </p>
     * 
     * @param store The containing store
     * @param clazz The class of the resources to return
     * 
     * @return A list of resources, possibly empty.
     */
    <T extends ResourceInfo> List<T> getResourcesByStore(StoreInfo store,
            Class<T> clazz);

    //
    // Layers
    //
    /**
     * Adds a layer to persistent storage.
     */
    LayerInfo add(LayerInfo layer);

    /**
     * 
     * Removes a layer from persistent storage.
     */
    void remove(LayerInfo layer);

    /**
     * Persists any modifications to a layer to persistent storage.
     * <p>
     * DAO implementations are responsible for triggering a {@link CatalogModifyEvent} by calling
     * {@link Catalog#fireModified(CatalogInfo, List, List, List)}. This is the responsibility of 
     * the dao because it is best suited to knowing and tracking which attributes of the object have 
     * changed. 
     * </p>
     */
    void save(LayerInfo layer);
    
    /**
     * Detaches a layer from the underlying persistence layer.
     * <p>
     * "Detaching" is specific to the underlying storage engine. But in general when an object 
     * is detached any proxies or uninitialized state of the object should be resolved.
     * </p>
     *
     *  @see Catalog#detach(LayerInfo)
     */
    LayerInfo detach(LayerInfo layer);

    /**
     * Loads a layer from persistent storage by specifying its identifier.
     *
     * @param id The unique identifier of the layer
     * 
     * @return The layer, or <code>null</code> if no such layer exists.
     */
    LayerInfo getLayer(String id);

    /**
     * Loads a layer from persistent storage by specifying its name.
     *
     * @param name The name of the layer
     * 
     * @return The layer, or <code>null</code> if no such layer exists.
     */
    LayerInfo getLayerByName(String name);

    /**
     * Loads all layers from persistent storage that publish a specified resource.
     *
     * @param resource The published resource
     * 
     * @return List of layers, possibly empty
     */
    List<LayerInfo> getLayers(ResourceInfo resource);

    /**
     * Loads all layers from persistent storage that reference a specified style.
     *
     * @param style The referenced style
     * 
     * @return List of layers, possibly empty
     */
    List<LayerInfo> getLayers(StyleInfo style);

    /**
     * Loads all layers from persistent storage.
     * 
     * @return List of layers, possibly empty
     */
    List<LayerInfo> getLayers();

    //
    // Maps
    //
    /**
     * Adds a map to persistent storage.
     */
    MapInfo add(MapInfo map);

    /**
     * Removes a map from persistent storage.
    */
    void remove(MapInfo map);

    /**
     * Persists any modifications to a map to persistent storage.
     * <p>
     * DAO implementations are responsible for triggering a {@link CatalogModifyEvent} by calling
     * {@link Catalog#fireModified(CatalogInfo, List, List, List)}. This is the responsibility of 
     * the dao because it is best suited to knowing and tracking which attributes of the object have 
     * changed. 
     * </p>
     */
    void save(MapInfo map);

    /**
     * Detaches a map from the underlying persistence layer.
     * <p>
     * "Detaching" is specific to the underlying storage engine. But in general when an object 
     * is detached any proxies or uninitialized state of the object should be resolved.
     * </p>
     *
     *  @see Catalog#detach(MapInfo)
     */
    MapInfo detach(MapInfo map);
    
    /**
     * Loads a map from persistent storage by its identifier. 
     * 
     * @param id The unique identifier of the map
     * 
     * @return The map, or <code>null</code> if no such map exists
     */
    MapInfo getMap(String id);

    /**
     * Loads a map from persistent storage by its name. 
     * 
     * @param name The name of the map
     * 
     * @return The map, or <code>null</code> if no such map exists
     */
    MapInfo getMapByName(String name);

    /**
     * Lists all maps from persistent storage.
     * 
     * @return A list of maps, possibly empty
     */
    List<MapInfo> getMaps();

    //
    // Layer groups
    //
    /**
     * Adds a layer group to persistent storage.
     */
    LayerGroupInfo add(LayerGroupInfo layerGroup);

    /**
     * 
     * Removes a layer group from persistent storage.
     */
    void remove(LayerGroupInfo layerGroup);

    /**
     * Persists any modifications to a layer group to persistent storage.
     * <p>
     * DAO implementations are responsible for triggering a {@link CatalogModifyEvent} by calling
     * {@link Catalog#fireModified(CatalogInfo, List, List, List)}. This is the responsibility of 
     * the dao because it is best suited to knowing and tracking which attributes of the object have 
     * changed. 
     * </p>
     */
    void save(LayerGroupInfo layerGroup);
    
    /**
     * Detaches a layer group from the underlying persistence layer.
     * <p>
     * "Detaching" is specific to the underlying storage engine. But in general when an object 
     * is detached any proxies or uninitialized state of the object should be resolved.
     * </p>
     *
     *  @see Catalog#detach(LayerGroupInfo)
     */
    LayerGroupInfo detach(LayerGroupInfo layerGroup);

    /**
     * Loads a layer group from persistent storage by specifying its identifier.
     * 
     * @param id The unique identifier for the layer group
     *  
     * @return The layer group, or <code>null</code> if it does not exist
     */
    LayerGroupInfo getLayerGroup(String id);

    /**
     * Loads a global layer group from persistent storage by specifying its name.
     * 
     * @param name The name of the layer group.
     * 
     * @return The layer group, or <code>null</code. if it does not exist
     */
    LayerGroupInfo getLayerGroupByName(String name);

    /**
     * Returns the layer group matching a particular name in the specified workspace, or
     * <code>null</code> if no such layer group could be found.
     * 
     * @param workspace The workspace containing the layer group. Not {@code null}, use
     *        {@link DefaultCatalogFacade#NO_WORKSPACE} or
     *        {@link DefaultCatalogFacade#ANY_WORKSPACE} to be explicit about what you're looking
     *        for.
     * @param name The name of the layer group to return.
     */
    LayerGroupInfo getLayerGroupByName(WorkspaceInfo workspace, String name);

    /**
     * Loads all layer groups from persistent storage.
     * 
     * @return A list of layer groups, possibly empty.
     */
    List<LayerGroupInfo> getLayerGroups();

    /**
     * All layer groups in the specified workspace.
     * 
     * @param workspace The workspace containing layer groups.
     */
    List<LayerGroupInfo> getLayerGroupsByWorkspace(WorkspaceInfo workspace);

    //
    // Namespaces
    //
    /**
     * Adds a namespace to persistent storage.
     */
    NamespaceInfo add(NamespaceInfo namespace);
   
    /**
     * Removes a namespace from persistent storage.
     */
    void remove(NamespaceInfo namespace);

    /**
     * Persists any modifications to a namespace to persistent storage.
     * <p>
     * DAO implementations are responsible for triggering a {@link CatalogModifyEvent} by calling
     * {@link Catalog#fireModified(CatalogInfo, List, List, List)}. This is the responsibility of 
     * the dao because it is best suited to knowing and tracking which attributes of the object have 
     * changed. 
     * </p>
     */
    void save(NamespaceInfo namespace);

    /**
     * Detaches a namespace from the underlying persistence layer.
     * <p>
     * "Detaching" is specific to the underlying storage engine. But in general when an object 
     * is detached any proxies or uninitialized state of the object should be resolved.
     * </p>
     *
     *  @see Catalog#detach(NamespaceInfo)
     */
    NamespaceInfo detach(NamespaceInfo namespace);
    
    /**
     * Loads the default namespace from persistent storage.
     * 
     * @return The namespace, or <code>null</code> if there is no default namespace set
     */
    NamespaceInfo getDefaultNamespace();

    /**
     * Sets the default namespace.
     * <p>
     * DAO implementations are responsible for triggering a {@link CatalogModifyEvent} by calling
     * {@link Catalog#fireModified(CatalogInfo, List, List, List)}. The source of the event is the 
     * catalog itself and changed property is "defaultNamespace". The changed old/new values should 
     * the old/new namespaces respectively.  
     * </p>
     */
    void setDefaultNamespace(NamespaceInfo defaultNamespace);

    /**
     * Loads a namespace from persistent storage by specifying its identifier.
     * 
     * @param id The unique identifier of the namespace.
     * 
     * @return The namespace, or <code>null</code> if no such namespace exists
     */
    NamespaceInfo getNamespace(String id);

    /**
     * Loads a namespace from persistent storage by specifying its prefix.
     * 
     * @param prefix The prefix of the namespace.
     * 
     * @return The namespace, or <code>null</code> if no such namespace exists
     */
    NamespaceInfo getNamespaceByPrefix(String prefix);

    /**
     * Loads a namespace from persistent storage by specifying its uri.
     * 
     * @param uri The uri of the namespace.
     * 
     * @return The namespace, or <code>null</code> if no such namespace exists
     */
    NamespaceInfo getNamespaceByURI(String uri);

    /**
     * Loads all namespaces from persistent storage.
     * 
     * @return A list of namespaces, possibilty empty
     */
    List<NamespaceInfo> getNamespaces();

    //
    // Workspaces
    //
    /**
     * Adds a workspace to persistent storage.
     */
    WorkspaceInfo add(WorkspaceInfo workspace);

    /**
     * Removes a workspace from persistent storage.
     */
    void remove(WorkspaceInfo workspace);

    /**
     * Persists any modifications to a workspace to persistent storage.
     * <p>
     * DAO implementations are responsible for triggering a {@link CatalogModifyEvent} by calling
     * {@link Catalog#fireModified(CatalogInfo, List, List, List)}. This is the responsibility of 
     * the dao because it is best suited to knowing and tracking which attributes of the object have 
     * changed. 
     * </p>
     */
    void save(WorkspaceInfo workspace);

    /**
     * Detaches a workspace from the underlying persistence layer.
     * <p>
     * "Detaching" is specific to the underlying storage engine. But in general when an object 
     * is detached any proxies or uninitialized state of the object should be resolved.
     * </p>
     *
     *  @see Catalog#detach(WorkspaceInfo)
     */
    WorkspaceInfo detach(WorkspaceInfo workspace);
    
    /**
     * Loads the default workspace from persistent storage.
     * 
     * @return The workspace, or <code>null</code> if there is no default workspace set
     */
    WorkspaceInfo getDefaultWorkspace();

    /**
     * Sets the default workspace.
     * <p>
     * DAO implementations are responsible for triggering a {@link CatalogModifyEvent} by calling
     * {@link Catalog#fireModified(CatalogInfo, List, List, List)}. The source of the event is the 
     * catalog itself and changed property is "defaultWorkspace". The changed old/new values should 
     * the old/new workspaces respectively.  
     * </p>
     */
    void setDefaultWorkspace(WorkspaceInfo workspace);

    /**
     * Loads a workspace from persistent storage by specifying its identifier.
     * 
     * @param id The unique identifier of the workspace.
     * 
     * @return THe workspace, or <code>null</code> if no such workspace exists.
     */
    WorkspaceInfo getWorkspace(String id);

    /**
     * Loads a workspace from persistent storage by specifying its name.
     * 
     * @param name The name of the workspace.
     * 
     * @return THe workspace, or <code>null</code> if no such workspace exists.
     */
    WorkspaceInfo getWorkspaceByName(String name);
    
    /**
     * Loads all workspaces from persistent storage.
     * 
     * @return A list of workspaces, possibly empty.
     */
    List<WorkspaceInfo> getWorkspaces();

    //
    // Styles
    //
    /**
     * Adds a style to persistent storage.
     */
    StyleInfo add(StyleInfo style);

    /**
     * Removes a style from persistent storage.
     */
    void remove(StyleInfo style);

    /**
     * Persists any modifications to a style to persistent storage.
     * <p>
     * DAO implementations are responsible for triggering a {@link CatalogModifyEvent} by calling
     * {@link Catalog#fireModified(CatalogInfo, List, List, List)}. This is the responsibility of 
     * the dao because it is best suited to knowing and tracking which attributes of the object have 
     * changed. 
     * </p>
     */
    void save(StyleInfo style);

    /**
     * Detaches a style from the underlying persistence layer.
     * <p>
     * "Detaching" is specific to the underlying storage engine. But in general when an object 
     * is detached any proxies or uninitialized state of the object should be resolved.
     * </p>
     *
     *  @see Catalog#detach(StyleInfo)
     */
    StyleInfo detach(StyleInfo style);
    
    /**
     * Loads a style from persistent storage by specifying its identifier.
     * 
     * @param id The unique identifier of the style.
     * 
     * @return The style, or <code>null</code> if no such style exists
     */
    StyleInfo getStyle(String id);

    /**
     * Loads a style from persistent storage by specifying its name.
     * 
     * @param name The name of the style.
     * 
     * @return The style, or <code>null</code> if no such style exists
     */
    StyleInfo getStyleByName(String name);

    /**
     * Returns the style matching a particular name in the specified workspace, or <code>null</code>
     * if no such style could be found.
     * 
     * @param workspace The workspace containing the style; non {@code null}, use
     *        {@value #ANY_WORKSPACE} or {@link #NO_WORKSPACE} as appropriate.
     * @param name The name of the style to return.
     */
    StyleInfo getStyleByName(WorkspaceInfo workspace, String name);

    /**
     * Loads all styles from persistent storage.
     * 
     * @return A list of styles, possibly empty.
     */
    List<StyleInfo> getStyles();

    /**
     * All styles in the specified workspace.
     * 
     * @param workspace The workspace containing styles.
     */
    List<StyleInfo> getStylesByWorkspace(WorkspaceInfo workspace);

    /**
     * Disposes the dao.
     * <p>
     * Called from {@link Catalog#dispose()} and forces the dao to release any resources. 
     * </p>
     */
    void dispose();

    /**
     * Called after the catalog has been initially started/loaded.
     * <p>
     * This method gives the dao a chance to resolve any proxies or uninitialized state that are
     * created during the loading process.
     * </p>
     */
    void resolve();

    /**
     * Pushes the data stored by this dao into another dao.
     */
    void syncTo(CatalogFacade other);
}
