/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.MultiHashMap;
import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageDimensionInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AttributeTypeInfoImpl;
import org.geoserver.catalog.impl.AttributionInfoImpl;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.CoverageDimensionImpl;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.DefaultCatalogFacade;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.ContactInfoImpl;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.JAIInfoImpl;
import org.geoserver.config.impl.LoggingInfoImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.Geometries;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.jdbc.RegexpValidator;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;
import org.geotools.jdbc.VirtualTableParameter.Validator;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.referencing.wkt.Formattable;
import org.geotools.util.Converters;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.SingleValueConverterWrapper;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.converters.collections.MapConverter;
import com.thoughtworks.xstream.converters.reflection.FieldDictionary;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.SortableFieldKeySorter;
import com.thoughtworks.xstream.converters.reflection.Sun14ReflectionProvider;
import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriterHelper;
import com.thoughtworks.xstream.io.HierarchicalStreamDriver;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.ClassAliasingMapper;
import com.thoughtworks.xstream.mapper.Mapper;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Utility class which loads and saves catalog and configuration objects to and
 * from an xstream.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public class XStreamPersister {

    
    /**
     * Callback interface or xstream persister.
     */
    public static class Callback {
        protected void postEncodeWorkspace( WorkspaceInfo ws, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeNamespace( NamespaceInfo ns, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeDataStore( DataStoreInfo ds, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeCoverageStore( CoverageStoreInfo ds, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeFeatureType( FeatureTypeInfo ds, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeWMSLayer( WMSLayerInfo ds, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeCoverage( CoverageInfo ds, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeLayer( LayerInfo ls, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        protected void postEncodeLayerGroup( LayerGroupInfo ls, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }
        
        /**
         * @deprecated use {@link #postEncodeReference(Object, String, String, HierarchicalStreamWriter, MarshallingContext)}
         */
        protected void postEncodeReference( Object obj, String ref, HierarchicalStreamWriter writer, MarshallingContext context ) {
        }

        protected void postEncodeReference( Object obj, String ref, String prefix, HierarchicalStreamWriter writer, MarshallingContext context ) {
            if (prefix == null) {
                //call other method for backward compatability
                postEncodeReference(obj, ref, writer, context);
            }
        }

        protected void postEncodeWMSStore(WMSStoreInfo store, HierarchicalStreamWriter writer,  MarshallingContext context) {
            
        }
    }
    
    /**
     * logging instance
     */
    static Logger LOGGER = Logging.getLogger( "org.geoserver" );
    
    /**
     * internal xstream instance
     */
    XStream xs;

    /**
     * GeoServer reference used to resolve references to gloal from services
     */
    GeoServer geoserver;
    
    /**
     * Catalog reference, used to resolve references to stores, workspaces 
     * + namespaces
     */
    Catalog catalog;
    
    /**
     * Callback instance. 
     */
    Callback callback;
    
    /**
     * Flag controlling how references to objects are encoded.
     */
    boolean referenceByName = false;
    
    /**
     * The type map used in {@link BreifMapConverter} to handle complex objects
     */
    Map<String, Class<?>> forwardBreifMap = new HashMap<String, Class<?>>();
    Map<Class<?>, String> backwardBreifMap = new HashMap<Class<?>, String>();
    
    /**
     * Constructs the persister and underlying xstream.
     */
    protected XStreamPersister() {
        this(null);
    }
    
    /**
     * Constructs the persister and underlying xstream specifying the stream driver explicitly.
     */
    protected XStreamPersister(HierarchicalStreamDriver streamDriver) {
        
        //control the order in which fields are sorted
        SortableFieldKeySorter sorter = new SortableFieldKeySorter();
        //sorter.registerFieldOrder( DefaultCatalogDAO.class, new String[]{ "workspaces", "namespaces", "stores", "styles", 
            /* these we actually omit, but the sorter needs them specified */
        //    "layerGroups", "resources", "maps", "defaultStores", "listeners", "layers",  "resourcePool", "resourceLoader", "LOGGER" } ); 
        
        ReflectionProvider reflectionProvider = new CustomReflectionProvider( new FieldDictionary( sorter ) ); 
            //new Sun14ReflectionProvider( new FieldDictionary( sorter  ) ); 
        if ( streamDriver != null ) {
            xs = new XStream( reflectionProvider, streamDriver );
        }
        else {
            xs = new XStream( reflectionProvider );    
        }
        xs.setMode(XStream.NO_REFERENCES);
        
        init(xs);
    }
    
    protected void init(XStream xs) {
        // Default implementations
        initImplementationDefaults(xs);
        
        // Aliases
        xs.alias("global", GeoServerInfo.class);
        xs.alias("settings", SettingsInfo.class);
        xs.alias("logging", LoggingInfo.class);
        xs.alias("jai", JAIInfo.class);
        xs.alias("coverageAccess", CoverageAccessInfo.class);
        xs.alias("catalog", Catalog.class);
        xs.alias("namespace", NamespaceInfo.class);
        xs.alias("workspace", WorkspaceInfo.class);
        xs.alias("dataStore", DataStoreInfo.class);
        xs.alias("wmsStore", WMSStoreInfo.class);
        xs.alias("coverageStore", CoverageStoreInfo.class);
        xs.alias("style",StyleInfo.class);
        xs.alias( "featureType", FeatureTypeInfo.class);
        xs.alias( "coverage", CoverageInfo.class);
        xs.alias( "wmsLayer", WMSLayerInfo.class);
        xs.alias( "coverageDimension", CoverageDimensionInfo.class);
        xs.alias( "metadataLink", MetadataLinkInfo.class);
        xs.alias( "attribute", AttributeTypeInfo.class );
        xs.alias( "layer", LayerInfo.class);
        xs.alias( "layerGroup", LayerGroupInfo.class);
        xs.alias( "gridGeometry", GridGeometry2D.class);
        xs.alias( "projected", DefaultProjectedCRS.class);
        xs.alias( "attribution", AttributionInfo.class );
        xs.aliasField("abstract", ResourceInfoImpl.class, "_abstract" );
        xs.alias("AuthorityURL", AuthorityURLInfo.class);
        xs.alias("Identifier", LayerIdentifierInfo.class);
        
        // GeoServerInfo
        xs.omitField(impl(GeoServerInfo.class), "clientProperties");
        xs.omitField(impl(GeoServerInfo.class), "geoServer");
        xs.registerLocalConverter(impl(GeoServerInfo.class), "metadata", new MetadataMapConverter());

        // ServiceInfo
        xs.omitField(impl(ServiceInfo.class), "clientProperties");
        xs.omitField(impl(ServiceInfo.class), "geoServer");
        xs.registerLocalConverter(impl(ServiceInfo.class), "workspace", new ReferenceConverter(WorkspaceInfo.class));
        xs.registerLocalConverter(impl(ServiceInfo.class), "metadata", new MetadataMapConverter());
        xs.registerLocalConverter(impl(ServiceInfo.class), "keywords", new KeywordListConverter());

        // Catalog
        xs.omitField(impl(Catalog.class), "resourcePool");
        xs.omitField(impl(Catalog.class), "resourceLoader");
        xs.omitField(impl(Catalog.class), "listeners");
        xs.omitField(impl(Catalog.class), "LOGGER");
        
        xs.omitField(impl(DefaultCatalogFacade.class), "catalog");
        xs.omitField(impl(DefaultCatalogFacade.class), "resources");
        xs.omitField(impl(DefaultCatalogFacade.class), "layers");
        xs.omitField(impl(DefaultCatalogFacade.class), "maps");
        xs.omitField(impl(DefaultCatalogFacade.class), "layerGroups");
        
        xs.registerLocalConverter(DefaultCatalogFacade.class, "stores",
                new StoreMultiHashMapConverter());
        xs.registerLocalConverter(DefaultCatalogFacade.class, "namespaces",
                new SpaceMapConverter("namespace"));
        xs.registerLocalConverter(DefaultCatalogFacade.class, "workspaces",
                new SpaceMapConverter("workspace"));
        
        
        //WorkspaceInfo
        xs.omitField( impl(WorkspaceInfo.class), "_default");
        xs.registerLocalConverter( impl(WorkspaceInfo.class), "metadata", new MetadataMapConverter() );
        
        //NamespaceInfo
        xs.omitField( impl(NamespaceInfo.class), "catalog");
        xs.omitField( impl(NamespaceInfo.class), "_default");
        xs.registerLocalConverter( impl(NamespaceInfo.class), "metadata", new MetadataMapConverter() );
        
        // StoreInfo
        xs.omitField(impl(StoreInfo.class), "catalog");
        xs.omitField(impl(StoreInfo.class), "error");
        //xs.omitField(StoreInfo.class), "workspace"); //handled by StoreInfoConverter
        xs.registerLocalConverter(impl(StoreInfo.class), "workspace", new ReferenceConverter(WorkspaceInfo.class));
        xs.registerLocalConverter(impl(StoreInfo.class), "connectionParameters", new BreifMapConverter() );
        xs.registerLocalConverter(impl(StoreInfo.class), "metadata", new MetadataMapConverter());
        
        // StyleInfo
        xs.omitField(impl(StyleInfo.class), "catalog");
        xs.registerLocalConverter(impl(StyleInfo.class), "workspace", new ReferenceConverter(WorkspaceInfo.class));
        xs.registerLocalConverter(impl(StyleInfo.class), "metadata", new MetadataMapConverter() );
        
        // ResourceInfo
        xs.omitField( impl(ResourceInfo.class), "catalog");
        xs.omitField( impl(ResourceInfo.class), "crs" );
        xs.registerLocalConverter( impl(ResourceInfo.class), "nativeCRS", new CRSConverter());
        xs.registerLocalConverter( impl(ResourceInfo.class), "store", new ReferenceConverter(StoreInfo.class));
        xs.registerLocalConverter( impl(ResourceInfo.class), "namespace", new ReferenceConverter(NamespaceInfo.class));
        xs.registerLocalConverter( impl(ResourceInfo.class), "metadata", new MetadataMapConverter() );
        xs.registerLocalConverter( impl(ResourceInfo.class), "keywords", new KeywordListConverter());
        
        // FeatureTypeInfo
        
        // CoverageInfo
        xs.registerLocalConverter( impl(CoverageInfo.class), "supportedFormats", new LaxCollectionConverter(xs.getMapper()));
        xs.registerLocalConverter( impl(CoverageInfo.class), "requestSRS", new LaxCollectionConverter(xs.getMapper()));
        xs.registerLocalConverter( impl(CoverageInfo.class), "responseSRS", new LaxCollectionConverter(xs.getMapper()));
        xs.registerLocalConverter( impl(CoverageInfo.class), "interpolationMethods", new LaxCollectionConverter(xs.getMapper()));
        xs.registerLocalConverter( impl(CoverageInfo.class), "dimensions", new LaxCollectionConverter(xs.getMapper()));
        
        
        // CoverageDimensionInfo
        xs.registerLocalConverter( impl(CoverageDimensionInfo.class), "range", new NumberRangeConverter());
        
        // AttributeTypeInfo
        xs.omitField( impl(AttributeTypeInfo.class), "featureType");
        xs.omitField( impl(AttributeTypeInfo.class), "attribute");
        
        // LayerInfo
        //xs.omitField( LayerInfo.class), "resource");
        xs.registerLocalConverter( impl(LayerInfo.class), "resource", new ReferenceConverter( ResourceInfo.class ) );
        xs.registerLocalConverter( impl(LayerInfo.class), "defaultStyle", new ReferenceConverter( StyleInfo.class) );
        xs.registerLocalConverter( impl(LayerInfo.class), "styles", new ReferenceCollectionConverter( StyleInfo.class ) );
        xs.registerLocalConverter( impl(LayerInfo.class), "metadata", new MetadataMapConverter() );
        
        // LayerGroupInfo
        xs.registerLocalConverter(impl(LayerGroupInfo.class), "workspace", new ReferenceConverter(WorkspaceInfo.class));
        xs.registerLocalConverter(impl(LayerGroupInfo.class), "layers", new ReferenceCollectionConverter( LayerInfo.class ));
        xs.registerLocalConverter(impl(LayerGroupInfo.class), "styles", new ReferenceCollectionConverter( StyleInfo.class ));
        xs.registerLocalConverter(impl(LayerGroupInfo.class), "metadata", new MetadataMapConverter() );
        
        //ReferencedEnvelope
        xs.registerLocalConverter( ReferencedEnvelope.class, "crs", new SRSConverter() );
        xs.registerLocalConverter( GeneralEnvelope.class, "crs", new SRSConverter() );
        
        // ServiceInfo
        xs.omitField( impl(ServiceInfo.class), "geoServer" );

        // Converters
        xs.registerConverter(new SpaceInfoConverter());
        xs.registerConverter(new StoreInfoConverter());
        xs.registerConverter(new ResourceInfoConverter());
        xs.registerConverter(new FeatureTypeInfoConverter());
        xs.registerConverter(new CoverageInfoConverter());
        xs.registerConverter(new LayerInfoConverter());
        xs.registerConverter(new LayerGroupInfoConverter());
        xs.registerConverter(new GridGeometry2DConverter());
        xs.registerConverter(new ProxyCollectionConverter( xs.getMapper() ) );
        xs.registerConverter(new VirtualTableConverter());
        xs.registerConverter(new KeywordInfoConverter());

        // register VirtulaTable handling
        registerBreifMapComplexType("virtualTable", VirtualTable.class);
        registerBreifMapComplexType("dimensionInfo", DimensionInfoImpl.class);
        
        callback = new Callback();
    }
    
    /**
     * Use this method to register complex types that cannot be simply represented as a string
     * in a {@link BreifMapConverter}. The {@code typeId} will be used as a type discriminator
     * in the brief map, as well as the element root for the complex object to be converted.
     * @param typeId
     * @param clazz
     */
    public void registerBreifMapComplexType(String typeId, Class clazz) {
        forwardBreifMap.put(typeId, clazz);
        backwardBreifMap.put(clazz, typeId);
        
    }

    public XStream getXStream() {
        return xs;
    }

    public ClassAliasingMapper getClassAliasingMapper() {
        return (ClassAliasingMapper) xs.getMapper().lookupMapperOfType( ClassAliasingMapper.class );
    }
    
    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }
    
    public void setGeoServer(GeoServer geoserver) {
        this.geoserver = geoserver;
    } 

    public GeoServerSecurityManager getSecurityManager() {
        return GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }
    
    public void setReferenceByName(boolean referenceByName) {
        this.referenceByName = referenceByName;
    }
    
    public void setExcludeIds() {
        xs.omitField( WorkspaceInfoImpl.class, "id");
        xs.omitField( NamespaceInfoImpl.class, "id");
        xs.omitField(StoreInfoImpl.class, "id");
        xs.omitField(StyleInfoImpl.class, "id");
        xs.omitField( ResourceInfoImpl.class, "id");
        xs.omitField( LayerInfoImpl.class, "id");
        xs.omitField(LayerGroupInfoImpl.class, "id" );
        xs.omitField(AttributeTypeInfoImpl.class, "id");
    }
    
    public void setHideFeatureTypeAttributes() {
        xs.omitField(FeatureTypeInfoImpl.class, "attributes");
    }
    
    /**
     * Saves an object to persistence.
     * 
     * @param obj The object to save. 
     * @param out The stream to save the object to.
     * 
     * @throws IOException
     */
    public void save(Object obj, OutputStream out) throws IOException {
        //unwrap dynamic proxies
        obj = unwrapProxies( obj );
        xs.toXML(obj, new OutputStreamWriter( out, "UTF-8" ));
    }
    
    /**
     * Unwraps any proxies around the object.
     * <p>
     * If the object is not being proxied it is passed back.
     * </p>
     */
    public static Object unwrapProxies( Object obj ) {
        obj = SecureCatalogImpl.unwrap( obj );
        obj = GeoServerImpl.unwrap( obj );
        obj = CatalogImpl.unwrap( obj );
        return obj;
    }
    
    /**
     * Loads an object from peristence.
     * 
     * @param in The input stream to read the object from.
     * @param clazz The class of the expected object.
     * 
     * @throws IOException
     */
    public <T> T load(InputStream in, Class<T> clazz ) throws IOException {
        T obj = clazz.cast( xs.fromXML( in ) );
        
        //call resolve() to ensure that any references created during loading
        // get resolved to actual objects, for instance for links from datastores
        // to workspaces
        if ( obj instanceof CatalogImpl ) {
            ((CatalogImpl)obj).resolve();
        }
        
        return obj;
    }
    
    
    /**
     * Builds a converter that will marshal/unmarshal the target class by reference, that is, by
     * storing the object id as opposed to fully serializing it
     * @param clazz
     * @return
     */
    public ReferenceConverter buildReferenceConverter(Class clazz) {
        return new ReferenceConverter(clazz);
    }
    
    /**
     * Same as {@link #buildReferenceConverter(Class)}, but works against a collection of objects
     * @param clazz
     * @return
     */
    public ReferenceCollectionConverter buildReferenceCollectionConverter(Class clazz) {
        return new ReferenceCollectionConverter(clazz);
    }

    /**
     * Sets up mappings from interface to implementation classes.
     * 
     */
    protected void initImplementationDefaults(XStream xs) {
        //configuration
        xs.addDefaultImplementation(GeoServerInfoImpl.class, GeoServerInfo.class);
        xs.addDefaultImplementation(SettingsInfoImpl.class, SettingsInfo.class);
        xs.addDefaultImplementation(LoggingInfoImpl.class, LoggingInfo.class);
        xs.addDefaultImplementation(JAIInfoImpl.class, JAIInfo.class);
        xs.addDefaultImplementation(CoverageAccessInfoImpl.class, CoverageAccessInfo.class);
        xs.addDefaultImplementation(ContactInfoImpl.class, ContactInfo.class);
        xs.addDefaultImplementation(AttributionInfoImpl.class, AttributionInfo.class);
        
        //catalog
        xs.addDefaultImplementation(CatalogImpl.class, Catalog.class);
        xs.addDefaultImplementation(NamespaceInfoImpl.class, NamespaceInfo.class);
        xs.addDefaultImplementation(WorkspaceInfoImpl.class, WorkspaceInfo.class);
        xs.addDefaultImplementation(DataStoreInfoImpl.class, DataStoreInfo.class);
        xs.addDefaultImplementation(WMSStoreInfoImpl.class, WMSStoreInfo.class);
        xs.addDefaultImplementation(CoverageStoreInfoImpl.class, CoverageStoreInfo.class);
        xs.addDefaultImplementation(StyleInfoImpl.class, StyleInfo.class);
        xs.addDefaultImplementation(FeatureTypeInfoImpl.class, FeatureTypeInfo.class );
        xs.addDefaultImplementation(CoverageInfoImpl.class, CoverageInfo.class);
        xs.addDefaultImplementation(WMSLayerInfoImpl.class, WMSLayerInfo.class);
        xs.addDefaultImplementation(CoverageDimensionImpl.class, CoverageDimensionInfo.class);
        xs.addDefaultImplementation(MetadataLinkInfoImpl.class, MetadataLinkInfo.class);
        xs.addDefaultImplementation(AttributeTypeInfoImpl.class, AttributeTypeInfo.class );
        xs.addDefaultImplementation(LayerInfoImpl.class, LayerInfo.class);
        xs.addDefaultImplementation(LayerGroupInfoImpl.class, LayerGroupInfo.class );
        xs.addDefaultImplementation(LayerIdentifier.class, LayerIdentifierInfo.class );
        xs.addDefaultImplementation(AuthorityURL.class, AuthorityURLInfo.class );

        //supporting objects
        xs.addDefaultImplementation(GridGeometry2D.class, GridGeometry.class );
        xs.addDefaultImplementation(DefaultGeographicCRS.class, CoordinateReferenceSystem.class);
        
        //collections
        xs.addDefaultImplementation(ArrayList.class, List.class);
    }
    
    protected Class impl(Class interfce) {
        //special case case classes, they don't get registered as default implementations
        // only concrete classes do
        if (interfce == ServiceInfo.class) {
            return ServiceInfoImpl.class;
        }
        if (interfce == StoreInfo.class) {
            return StoreInfoImpl.class;
        }
        if (interfce == ResourceInfo.class) {
            return ResourceInfoImpl.class;
        }
        
        Class clazz = getXStream().getMapper().defaultImplementationOf(interfce); 
        if (clazz == null) {
            throw new RuntimeException("No default mapping for " + interfce);
        }
        return clazz;
    }
    
    /**
     * Custom reflection provider which unwraps proxies, and skips empty collections
     * and maps.
     */
    class CustomReflectionProvider extends Sun14ReflectionProvider {
        
        public CustomReflectionProvider( FieldDictionary fd ) {
            super( fd );
        }
        
        @Override
        public void visitSerializableFields(Object object, Visitor visitor) {
            super.visitSerializableFields(object, new VisitorWrapper(visitor));
        }
        
        class VisitorWrapper implements ReflectionProvider.Visitor {

            Visitor wrapped;
            
            public VisitorWrapper( Visitor wrapped ) {
                this.wrapped = wrapped;
            }
            
            public void visit(String name, Class type, Class definedIn,
                    Object value) {
                
                //skip empty collections + maps
                if ( value instanceof Collection && ((Collection)value).isEmpty() ) {
                    return;
                }
                if ( value instanceof Map && ((Map)value).isEmpty() ) {
                    return;
                }
                
                //unwrap any proxies
                value = unwrapProxies(value);
                wrapped.visit( name, type, definedIn, value);
            }
            
        }
    }

    //
    // custom converters
    //
    
    //simple object converters
    /**
     * Map converter which encodes a map more breifly than the standard map converter.
     */
    protected class BreifMapConverter extends MapConverter {
        
        static final String ENCRYPTED_FIELDS_KEY = "org.geoserver.config.encryptedFields";

        public BreifMapConverter() {
            super(getXStream().getMapper());
        }
        
        @Override
        public boolean canConvert(Class type) {
            //handle all types of maps
            return Map.class.isAssignableFrom(type);
        }
        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {

            Set<String> encryptionFields = (Set<String>)context.get(ENCRYPTED_FIELDS_KEY);
            if (encryptionFields==null) {
                encryptionFields=Collections.emptySet();
            }

            GeoServerSecurityManager secMgr = getSecurityManager();
            Map map = (Map) source;
            for (Iterator iterator = map.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                
                if ( entry.getValue() == null ) {
                    continue;
                }
                
                writer.startNode("entry");
                writer.addAttribute( "key", entry.getKey().toString());
                if ( entry.getValue() != null ) {
                    Object value = entry.getValue();
                    String complexTypeId = getComplexTypeId(value.getClass());
                    if(complexTypeId == null) {
                        String str = Converters.convert(value, String.class);
                        if(str == null) {
                            str = value.toString();
                        }
                        if (encryptionFields.contains(entry.getKey()) && secMgr != null) {
                            str = secMgr.getConfigPasswordEncryptionHelper().encode(str);
                        }
                        writer.setValue(str);
                    } else {
                        writer.startNode(complexTypeId);
                        context.convertAnother(value);
                        writer.endNode();
                    }
                }
                
                writer.endNode();
            }
        }

        @Override
        protected void populateMap(HierarchicalStreamReader reader,
                UnmarshallingContext context, Map map) {

            while (reader.hasMoreChildren()) {
                reader.moveDown();
                
                //we support four syntaxes here:
                // 1) <key>value</key>
                // 2) <key><type>value</type></key>
                // 3) <entry key="">value</entry>
                // 4) <entry>
                //      <type>key</type>
                //      <type>value</type>
                //    </entry>
                String key = reader.getNodeName();
                Object value = null;
                if ( "entry".equals( key ) ) {
                    if ( reader.getAttribute( "key") != null ) {
                        //this is case 3
                        key = reader.getAttribute( "key" );
                        // in this case we also support complex objects
                        if(reader.hasMoreChildren()) {
                            reader.moveDown();
                            String typeId = reader.getNodeName();
                            value = context.convertAnother(null, getComplexTypeClass(typeId));
                            reader.moveUp();
                        } else {
                            value = reader.getValue();
                        }
                    }
                    else if ( reader.hasMoreChildren() ){
                        //this is case 4
                        reader.moveDown();
                        
                        key = reader.getValue();
                        
                        reader.moveUp();
                        reader.moveDown();
                        
                        value = reader.getValue();
                        
                        reader.moveUp();
                    }

                }
                else {
                    boolean old = false;
                    if (reader.hasMoreChildren()) {
                        //this handles case 2
                        old = true;
                        reader.moveDown();    
                    }
                    
                    value = readItem(reader, context, map);
                    
                    if ( old ) {
                        reader.moveUp();    
                    }
                }
               
                map.put(key, value);

                reader.moveUp();
            }
        }
        
        @Override
        protected Object readItem(HierarchicalStreamReader reader, UnmarshallingContext context,
                Object current) {
            return reader.getValue();
        }

        private Class getComplexTypeClass(String typeId) {
            return forwardBreifMap.get(typeId);
        }
        
        private String getComplexTypeId(Class clazz) {
             String typeId = backwardBreifMap.get(clazz);
             if(typeId == null) {
                 List<Class> matches = new ArrayList<Class>();
                 collectSuperclasses(clazz, matches);
                 for (Iterator it = matches.iterator(); it.hasNext();) {
                    Class sper = (Class) it.next();
                    if(backwardBreifMap.get(sper) == null) {
                        it.remove();
                    }
                }
                 
                if(matches.size() > 1) {
                    Comparator comparator = new Comparator<Class>() {
                        public int compare(Class c1, Class c2) {
                            if (c2.isAssignableFrom(c1)) {
                                return -1;
                            } else {
                                return 1;
                            }
                        }
                    };

                    Collections.sort(matches, comparator);
                }
                
                if(matches.size() > 0) {
                    typeId = backwardBreifMap.get(matches.get(0));
                }
             }
             
             return typeId;
        }

        void collectSuperclasses(Class clazz, List<Class> matches) {
            matches.add(clazz);
            if(clazz.getSuperclass() == null && clazz.getInterfaces().length == 0) {
                return;
            }
            
            if(clazz.getSuperclass() != null) {
                collectSuperclasses(clazz.getSuperclass(), matches);
            }
            for(Class iface : clazz.getInterfaces()) {
                collectSuperclasses(iface, matches);
            }
        }
    }
    
    /**
     * Custom converter for the special metadata map.
     */
    class MetadataMapConverter extends BreifMapConverter {
        
        @Override
        public boolean canConvert(Class type) {
            return MetadataMap.class.equals(type) || super.canConvert(type);
        }
        
        @Override
        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            if ( source instanceof MetadataMap) {
                MetadataMap mdmap = (MetadataMap) source;
                source = mdmap.getMap();
            }
            
            super.marshal(source, writer, context);
        }
        
        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            Map map = (Map) super.unmarshal(reader, context);
            if ( !(map instanceof MetadataMap ) ) {
                map = new MetadataMap(map);
            }
            return map;
        }
    }
    
    /**
     * Converters which encodes an object by a reference, or its id.
     */
    //class ReferenceConverter extends AbstractSingleValueConverter {
    class ReferenceConverter implements Converter {
        Class clazz;

        public ReferenceConverter( Class clazz ) {
            this.clazz = clazz;
        }

        public boolean canConvert(Class type) {
            return clazz.isAssignableFrom( type );
        }

        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            //could be a proxy, unwrap it
            source = CatalogImpl.unwrap( source );
            
            //gets its id
            String id = (String) OwsUtils.get( source, "id" );
            if ( id != null && !referenceByName) {
                writer.startNode("id");
                writer.setValue( id );
                writer.endNode();
                callback.postEncodeReference( source, id, null, writer, context );
            }
            else {
                //use name if no id set
                String name = (String) OwsUtils.get( source, "name" );

                //use workspace name as a prefix if available
                String wsName = null;
                if (OwsUtils.has(source, "workspace")) {
                    WorkspaceInfo workspace = (WorkspaceInfo) OwsUtils.get(source, "workspace");
                    if (workspace != null) {
                        wsName = workspace.getName();
                    }
                }
                
                if ( name != null ) {
                    writer.startNode("name");
                    writer.setValue( name );
                    writer.endNode();

                    callback.postEncodeReference( source, name, wsName, writer, context );
                }
                else {
                    throw new IllegalArgumentException( "Unable to marshal reference with no id or name.");
                }
            }
            
        }
        
        public Object unmarshal(HierarchicalStreamReader reader,
                UnmarshallingContext context) {
            
            String ref = null;
            String pre = null;
            if ( reader.hasMoreChildren() ) {
                while(reader.hasMoreChildren()) {
                    reader.moveDown();
                    if ("workspace".equals(reader.getNodeName())) {
                        if (reader.hasMoreChildren()) {
                            //specified as <workspace><name>[name]</name></workspace>
                            reader.moveDown();
                            pre = reader.getValue();
                            reader.moveUp();
                        }
                        else {
                            //specified as <workspace>[name]</workspace>
                            pre = reader.getValue();
                        }
                    }
                    else {
                        ref = reader.getValue();
                    }

                    reader.moveUp();
                }
            }
            else {
                ref = reader.getValue();
            }

            Object proxy = ResolvingProxy.create( ref, pre, clazz );
            Object resolved = proxy;
            if ( catalog != null ) {
                resolved = ResolvingProxy.resolve( catalog, proxy );
            }
            
            return CatalogImpl.unwrap( resolved );
        }
    }
    class ReferenceCollectionConverter extends LaxCollectionConverter {
        Class clazz;

        public ReferenceCollectionConverter(Class clazz) {
            super( getXStream().getMapper() );
            this.clazz = clazz;
        }

        @Override
        protected void writeItem(Object item, MarshallingContext context,
                HierarchicalStreamWriter writer) {
            ClassAliasingMapper cam = 
                (ClassAliasingMapper) mapper().lookupMapperOfType( ClassAliasingMapper.class );
            
            String elementName = cam.serializedClass( clazz );
            if ( elementName == null ) {
                elementName = cam.serializedClass( item.getClass() );
            }
            writer.startNode(elementName);
            if(item != null)
                context.convertAnother( item, new ReferenceConverter( clazz ) );
            writer.endNode();
        }
        
        @Override
        protected Object readItem(HierarchicalStreamReader reader,
                UnmarshallingContext context, Object current) {
            return context.convertAnother( current, clazz, new ReferenceConverter( clazz ) );
        }
    }
    /**
     * Converter which unwraps proxies in a collection.
     */
    class ProxyCollectionConverter extends CollectionConverter {

        public ProxyCollectionConverter(Mapper mapper) {
            super(mapper);
        }
        
        @Override
        protected void writeItem(Object item, MarshallingContext context,
                HierarchicalStreamWriter writer) {
                        
            super.writeItem(unwrapProxies(item), context, writer);
        }
    }
    
    /**
     * Converter for coordinate reference system objects that converts by SRS code.
     */
    public static class SRSConverter extends AbstractSingleValueConverter {
        
        public boolean canConvert(Class type) {
            return CoordinateReferenceSystem.class.isAssignableFrom(type);
        }
        
        @Override
        public String toString(Object obj) {
            CoordinateReferenceSystem crs = (CoordinateReferenceSystem) obj;
            try {
                Integer epsg = CRS.lookupEpsgCode(crs, false);
                if (epsg != null) {
                    return "EPSG:" + epsg;
                }
            } 
            catch (FactoryException e) {
                XStreamPersister.LOGGER.warning( "Could not determine epsg code of crs, encoding as WKT");
            }
            
            return new CRSConverter().toString(crs);
        }
        
        @Override
        public Object fromString(String str) {
            if ( str.toUpperCase().startsWith( "EPSG:") ) {
                try {
                    return CRS.decode( str );
                } 
                catch (Exception e) {
                    XStreamPersister.LOGGER.log( Level.WARNING, "Error decode epsg code: "+str, e );
                }    
            }
            else {
                try {
                    return CRS.parseWKT( str );
                } 
                catch (FactoryException e) {
                    XStreamPersister.LOGGER.log( Level.WARNING, "Error decode wkt: "+str, e );
                }
            }
            return null;
        }
    }
    
    /**
     * Converter for coordinate reference system objects that converts by WKT. 
     *
     */
    public static class CRSConverter extends AbstractSingleValueConverter {

        @Override
        public boolean canConvert(Class type) {
            return CoordinateReferenceSystem.class.isAssignableFrom(type);
        }

        @Override
        public String toString(Object obj) {
            return ((Formattable)obj).toWKT(2, false);
        }
        
        @Override
        public Object fromString(String str) {
            try {
                return CRS.parseWKT( str );
            } 
            catch (Exception e) {
                try {
                    return new SRSConverter().fromString( str );
                }
                catch( Exception e1 ) {}
                
                throw new RuntimeException( e );
            }
        }
        
    }
    
    /**
     * Converter for coverage grid geometry.
     *
     */
    class GridGeometry2DConverter extends AbstractReflectionConverter {
        public GridGeometry2DConverter() {
            super( GridGeometry2D.class );
        }

        @Override
        protected void doMarshal(Object source,
                HierarchicalStreamWriter writer, MarshallingContext context) {
         
            GridGeometry2D g = (GridGeometry2D) source;
            MathTransform tx = g.getGridToCRS();

            writer.addAttribute("dimension", String.valueOf(g.getGridRange().getDimension()));
            
            //grid range
            StringBuffer low = new StringBuffer();
            StringBuffer high = new StringBuffer();
            for (int r = 0; r < g.getGridRange().getDimension(); r++) {
                low.append(g.getGridRange().getLow(r)).append(" ");
                high.append(g.getGridRange().getHigh(r)+1).append(" ");
            }
            low.setLength(low.length()-1);
            high.setLength(high.length()-1);
            
            writer.startNode("range");
            writer.startNode( "low" ); writer.setValue( low.toString() ); writer.endNode();
            writer.startNode( "high" ); writer.setValue( high.toString() ); writer.endNode();
            writer.endNode();
            
            //transform
            if (tx instanceof AffineTransform) {
                AffineTransform atx = (AffineTransform) tx;
                
                writer.startNode("transform");
                writer.startNode("scaleX"); writer.setValue(Double.toString( atx.getScaleX())); writer.endNode();
                writer.startNode("scaleY"); writer.setValue(Double.toString( atx.getScaleY())); writer.endNode();
                writer.startNode("shearX"); writer.setValue(Double.toString( atx.getShearX())); writer.endNode();
                writer.startNode("shearY"); writer.setValue(Double.toString( atx.getShearY())); writer.endNode();
                writer.startNode("translateX"); writer.setValue(Double.toString( atx.getTranslateX())); writer.endNode();
                writer.startNode("translateY"); writer.setValue(Double.toString( atx.getTranslateY())); writer.endNode();
                writer.endNode();
            }
            
            //crs
            writer.startNode("crs");
            context.convertAnother( g.getCoordinateReferenceSystem(), 
                new SingleValueConverterWrapper( new SRSConverter() ) );
            writer.endNode();
           
        }
        
        @Override
        public Object unmarshal(HierarchicalStreamReader reader,
                UnmarshallingContext context) {
             int[] high,low;
            
            //reader.moveDown(); //grid
            
            reader.moveDown(); //range
            
            reader.moveDown(); //low
            low = toIntArray( reader.getValue() );
            reader.moveUp();
            reader.moveDown(); //high
            high = toIntArray( reader.getValue() );
            reader.moveUp();
            
            reader.moveUp(); //range
            
            if ( reader.hasMoreChildren() ) {
                reader.moveDown(); //transform or crs
            }
            
            AffineTransform2D gridToCRS = null;
            if ( "transform".equals( reader.getNodeName() ) ) {
                double sx,sy,shx,shy,tx,ty;
                
                reader.moveDown(); //scaleX
                sx = Double.parseDouble( reader.getValue() );
                reader.moveUp();
                
                reader.moveDown(); //scaleY
                sy = Double.parseDouble( reader.getValue() );
                reader.moveUp();
                
                
                reader.moveDown(); //shearX
                shx = Double.parseDouble( reader.getValue() );
                reader.moveUp();
                
                reader.moveDown(); //shearY
                shy = Double.parseDouble( reader.getValue() );
                reader.moveUp();
                
                reader.moveDown(); //translateX
                tx = Double.parseDouble( reader.getValue() );
                reader.moveUp();
                
                reader.moveDown(); //translateY
                ty = Double.parseDouble( reader.getValue() );
                reader.moveUp();
                
                

                // set tranform
                gridToCRS = new AffineTransform2D(sx, shx, shy, sy, tx, ty);
                reader.moveUp();
                if ( reader.hasMoreChildren() ) {
                    reader.moveDown(); //crs
                }
            }
            
            CoordinateReferenceSystem crs = null;
            if ( "crs".equals( reader.getNodeName() ) ) {
                crs = (CoordinateReferenceSystem) context.convertAnother( null, CoordinateReferenceSystem.class, 
                    new SingleValueConverterWrapper( new SRSConverter() ));
                reader.moveUp();
            }
            
            // new grid range
            GeneralGridEnvelope gridRange = new GeneralGridEnvelope(low, high);
            
            GridGeometry2D gg = new GridGeometry2D( gridRange, gridToCRS, crs );
            return serializationMethodInvoker.callReadResolve(gg);
        }
            
        int[] toIntArray( String s ) {
            String[] split = s.split( " " );
            int[] ints = new int[split.length];
            for ( int i = 0; i < split.length; i++ ) {
                ints[i] = Integer.parseInt( split[i] );
            }
            return ints;
        }
    }
    class NumberRangeConverter extends AbstractReflectionConverter {
     
        @Override
        public boolean canConvert(Class clazz) {
            return NumberRange.class.isAssignableFrom( clazz );
        }
        
        @Override
        public void marshal(Object original, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            NumberRange range = (NumberRange) original;
            
            writer.startNode("min");
            if ( Double.isInfinite( ((Number)range.getMinValue()).doubleValue() ) ) {
                context.convertAnother( "-inf" );
            }
            else {
                context.convertAnother( range.getMinValue() );  
            }
            writer.endNode();
            
            writer.startNode("max");
            if ( Double.isInfinite( ((Number)range.getMaxValue()).doubleValue() )) {
                context.convertAnother( "inf");
            }
            else {
                context.convertAnother( range.getMaxValue() );  
            }
            writer.endNode();
        }
        
        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            //JD: we handle infinite manually b/c the json serializer chokes on inifinte values 
            // b/c JSON does not support it
            Double min = null, max = null;
            while( reader.hasMoreChildren() ) {
                reader.moveDown();
                if ( "min".equals( reader.getNodeName() ) ) {
                    if ( !"-inf".equals( reader.getValue() ) ) {
                        min = Double.parseDouble( reader.getValue() ); 
                    }
                }
                if ( "max".equals( reader.getNodeName() ) ) {
                    if ( !"inf".equals( reader.getValue() ) ) {
                        max = Double.parseDouble( reader.getValue() ); 
                    }
                }
                reader.moveUp();
            }
            
            min = min != null ? min : Double.NEGATIVE_INFINITY;
            max = max != null ? max : Double.POSITIVE_INFINITY;
            
            return NumberRange.create( min.doubleValue(), true, max.doubleValue(), true );
        }
    }
    
    //catalog object converters
    /**
     * Base class for all custom reflection based converters.
     */
    class AbstractReflectionConverter extends ReflectionConverter {
        Class clazz;

        public AbstractReflectionConverter() {
            this(Object.class);
        }

        public AbstractReflectionConverter(Class clazz) {
            super(getXStream().getMapper(),getXStream().getReflectionProvider());
            this.clazz = clazz;
        }

        @Override
        public boolean canConvert(Class type) {
            return clazz.isAssignableFrom( type );
        }
        
        @Override
        protected void doMarshal(Object source,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            super.doMarshal(source, writer, context);
            postDoMarshal(source,writer,context);
        }
        
        protected void postDoMarshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        }
    }

    /**
     * Converter for workspaces and namespaces.
     */
    class SpaceInfoConverter extends AbstractReflectionConverter {
        @Override
        public boolean canConvert(Class type) {
            return WorkspaceInfo.class.isAssignableFrom(type) || 
                NamespaceInfo.class.isAssignableFrom(type);
        }
        
        @Override
        protected void postDoMarshal(Object source,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            if ( source instanceof WorkspaceInfo ) {
                callback.postEncodeWorkspace( (WorkspaceInfo)source,writer,context );
            }
            else {
                callback.postEncodeNamespace( (NamespaceInfo) source,writer,context );
            }
        }
    }
    /**
     * Converter for {@link DataStoreInfo}, {@link CoverageStoreInfo}, and {@link WMSStoreInfo}
     */
    class StoreInfoConverter extends AbstractReflectionConverter {

        public StoreInfoConverter() {
            super(StoreInfo.class);
        }

        @Override
        protected void doMarshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            GeoServerSecurityManager secMgr = getSecurityManager();
            if (secMgr != null && secMgr.isInitialized()) {
                //set the hint for the map converter as to which fields to encode in the connection
                // parameter of this store
                context.put(BreifMapConverter.ENCRYPTED_FIELDS_KEY, 
                    secMgr.getConfigPasswordEncryptionHelper().getEncryptedFields((StoreInfo)source));
            }

            super.doMarshal(source, writer, context);
        }
        
        @Override
        protected void postDoMarshal(Object result,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            
            StoreInfo store = (StoreInfo) result;
            if ( store instanceof DataStoreInfo ) {
                callback.postEncodeDataStore( (DataStoreInfo) store, writer, context );
            } else if( store instanceof CoverageStoreInfo ){
                callback.postEncodeCoverageStore( (CoverageStoreInfo) store, writer, context );
            } else if (store instanceof WMSStoreInfo){
                callback.postEncodeWMSStore( (WMSStoreInfo) store, writer, context );
            } else {
                throw new IllegalArgumentException("Unknown store type: "
                        + (store == null ? "null" : store.getClass().getName()));
            }
        }
        
        @Override
        public Object doUnmarshal(Object result,
                HierarchicalStreamReader reader, UnmarshallingContext context) {
            StoreInfo store = (StoreInfo) super.doUnmarshal(result, reader, context);
            
            // 2.1.3+ backwards compatibility check
            if (store instanceof WMSStoreInfo) {
                WMSStoreInfo wmsStore = (WMSStoreInfo) store;
                MetadataMap metadata = wmsStore.getMetadata();
                Integer maxConnections = null;
                Integer connectTimeout = null;
                Integer readTimeout = null;
                if (metadata != null) {
                    maxConnections = metadata.get("maxConnections", Integer.class);
                    connectTimeout = metadata.get("connectTimeout", Integer.class);
                    readTimeout = metadata.get("readTimeout", Integer.class);
                    metadata.remove("maxConnections");
                    metadata.remove("connectTimeout");
                    metadata.remove("readTimeout");
                }
                if (wmsStore.getMaxConnections() <= 0) {
                    wmsStore.setMaxConnections(maxConnections != null
                            && maxConnections.intValue() > 0 ? maxConnections
                            : WMSStoreInfoImpl.DEFAULT_MAX_CONNECTIONS);
                }
                if (wmsStore.getConnectTimeout() <= 0) {
                    wmsStore.setConnectTimeout(connectTimeout != null
                            && connectTimeout.intValue() > 0 ? connectTimeout
                            : WMSStoreInfoImpl.DEFAULT_CONNECT_TIMEOUT);
                }
                if (wmsStore.getReadTimeout() <= 0) {
                    wmsStore.setReadTimeout(readTimeout != null && readTimeout.intValue() > 0 ? readTimeout
                            : WMSStoreInfoImpl.DEFAULT_READ_TIMEOUT);
                }
            }

            //process any parameters that require decryption 
            GeoServerSecurityManager secMgr = getSecurityManager();
            if (secMgr != null) {
                secMgr.getConfigPasswordEncryptionHelper().decode(store);
            }

            LOGGER.info( "Loaded store '" +  store.getName() +  "', " + (store.isEnabled() ? "enabled" : "disabled") );
            return store;
        }
    }

    /**
     * Converter for multi hash maps containing coverage stores and data stores.
     */
    static class StoreMultiHashMapConverter implements Converter {
        public boolean canConvert(Class type) {
            return MultiHashMap.class.equals(type);
        }

        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            MultiHashMap map = (MultiHashMap) source;
            for (Object v : map.values()) {
                if (v instanceof DataStoreInfo) {
                    writer.startNode("dataStore");
                    context.convertAnother(v);
                    writer.endNode();
                }
                if (v instanceof CoverageStoreInfo ) {
                    writer.startNode( "coverageStore" );
                    context.convertAnother(v);
                    writer.endNode();
                }
            }
        }

        public Object unmarshal(HierarchicalStreamReader reader,
                UnmarshallingContext context) {
            MultiHashMap map = new MultiHashMap();
            
            while( reader.hasMoreChildren() ) {
                reader.moveDown();
                
                Object o = 0;
                if ( "dataStore".equals( reader.getNodeName() ) ) {
                    o = context.convertAnother( map, DataStoreInfoImpl.class );
                }
                else {
                    o = context.convertAnother( map, CoverageStoreInfoImpl.class );
                }
                map.put( o.getClass(), o );
                
                reader.moveUp();
            }
            
            return map;
        }
    }

    /**
     * Converter for handling maps containing workspaces and namespaces.
     *
     */
    static class SpaceMapConverter implements Converter {

        String name;
        
        public SpaceMapConverter( String name ) {
            this.name = name;
        }
        
        public boolean canConvert(Class type) {
            return Map.class.isAssignableFrom(type);
        }

        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {

            Map map = (Map) source;
            
            for (Object o : map.entrySet()) {
                Map.Entry e = (Map.Entry) o;
                if ( e.getKey() == null ) {
                    continue;
                }
                
                writer.startNode(name);
                if ( map.get( null ) == e.getValue() ) {
                    writer.addAttribute("default", "true");
                }
                context.convertAnother(e.getValue());
                writer.endNode();
            }
        }

        public Object unmarshal(HierarchicalStreamReader reader,
                UnmarshallingContext context) {
            Map map = new HashMap();
            
            while( reader.hasMoreChildren() ) {
               reader.moveDown();

               boolean def = "true".equals( reader.getAttribute( "default") );
               
               if ( "namespace".equals( name ) ) {
                   NamespaceInfoImpl ns = (NamespaceInfoImpl) context.convertAnother( map, NamespaceInfoImpl.class );
                   map.put( ns.getPrefix() , ns );
                   if ( def ) {
                       map.put( null, ns );
                   }
                   LOGGER.info( "Loading namespace '" + ns.getPrefix() + "'" );
               }
               else {
                   WorkspaceInfoImpl ws = (WorkspaceInfoImpl) context.convertAnother( map, WorkspaceInfoImpl.class );
                   map.put( ws.getName() , ws );
                   if ( def ) {
                       map.put( null, ws );
                   }
                   LOGGER.info( "Loading workspace '" + ws.getName() + "'" );
               }
               
               reader.moveUp();
            }
            
            return map;
        }
    }

    /**
     * Base converter for handling resources.
     */
    class ResourceInfoConverter extends AbstractReflectionConverter {
        
        public ResourceInfoConverter() {
            this(ResourceInfo.class);
        }
        
        public ResourceInfoConverter(Class clazz ) {
            super(clazz);
        }
        
        public Object doUnmarshal(Object result,
                HierarchicalStreamReader reader, UnmarshallingContext context) {
            ResourceInfo obj = (ResourceInfo) super.doUnmarshal(result, reader, context);
            
            String enabled = obj.isEnabled() ? "enabled" : "disabled";
            String type = obj instanceof CoverageInfo ? "coverage" : 
                obj instanceof FeatureTypeInfo ? "feature type" : "resource";
            
            LOGGER.info( "Loaded " + type + " '" + obj.getName() + "', " + enabled );
            
            return obj;
        }
    }
    
    /**
     * Converter for feature types.
     */
    class FeatureTypeInfoConverter extends ResourceInfoConverter {

        public FeatureTypeInfoConverter() {
            super(FeatureTypeInfo.class);
        }
        
        @Override
        protected void postDoMarshal(Object result,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            FeatureTypeInfoImpl featureType = (FeatureTypeInfoImpl) result;
            
            //ensure null list does not result
            if ( featureType.getAttributes() == null ){
                featureType.setAttributes(new ArrayList());
            }
            if( featureType.getMetadata() == null) {
                featureType.setMetadata(new MetadataMap());
            }
            
            callback.postEncodeFeatureType(featureType, writer, context);
        }
    }
    
    /**
     * Converter for feature types.
     */
    class CoverageInfoConverter extends ResourceInfoConverter {
        public CoverageInfoConverter() {
            super( CoverageInfo.class );
        }
        
        @Override
        protected void postDoMarshal(Object result,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            callback.postEncodeCoverage((CoverageInfo)result, writer, context);
        }
    }
    
    /**
     * Converter for layers.
     */
    class LayerInfoConverter extends AbstractReflectionConverter {

        public LayerInfoConverter() {
            super( LayerInfo.class );
        }
        
        @Override
        protected void doMarshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            // write out the name, which is a derived property now
            // TODO: remove this when resource/publishing split is done
            LayerInfo l = (LayerInfo) source;
            writer.startNode("name");
            writer.setValue(l.getName());
            writer.endNode();
            
//            {
//                String authUrlsSerializedForm = AuthorityURLInfoInfoListConverter.toString(l
//                        .getAuthorityURLs());
//                if (null != authUrlsSerializedForm) {
//                    l.getMetadata().put("authorityURLs", authUrlsSerializedForm);
//                }
//            }
//
//            {
//                String identifiersSerializedForm = LayerIdentifierInfoListConverter.toString(l
//                        .getIdentifiers());
//                if (null != identifiersSerializedForm) {
//                    l.getMetadata().put("identifiers", identifiersSerializedForm);
//                }
//            }

            super.doMarshal(source, writer, context);
        }
                
        @Override
        protected void postDoMarshal(Object result,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            /*
            LayerInfo l = (LayerInfo) result;
            writer.startNode("resource");
            context.convertAnother( l.getResource(), new ReferenceConverter( ResourceInfo.class ) );
            writer.endNode();
            */
            callback.postEncodeLayer( (LayerInfo) result, writer, context );
        }

        @Override
        public Object doUnmarshal(Object result, HierarchicalStreamReader reader,
                UnmarshallingContext context) {

            LayerInfoImpl li = (LayerInfoImpl) super.doUnmarshal(result, reader, context);
            MetadataMap metadata = li.getMetadata();
            if (li.getAuthorityURLs() == null && metadata != null) {
                String serialized = metadata.get("authorityURLs", String.class);
                List<AuthorityURLInfo> authorities;
                if (serialized == null) {
                    authorities = new ArrayList<AuthorityURLInfo>(1);
                } else {
                    authorities = AuthorityURLInfoInfoListConverter.fromString(serialized);
                }
                li.setAuthorityURLs(authorities);
            }
            if (li.getIdentifiers() == null && metadata != null) {
                String serialized = metadata.get("identifiers", String.class);
                List<LayerIdentifierInfo> identifiers;
                if (serialized == null) {
                    identifiers = new ArrayList<LayerIdentifierInfo>(1);
                } else {
                    identifiers = LayerIdentifierInfoListConverter.fromString(serialized);
                }
                li.setIdentifiers(identifiers);
            }
            return li;
        }
    }
  
    /**
     * Converter for layer groups.
     */
    class LayerGroupInfoConverter extends AbstractReflectionConverter {

        public LayerGroupInfoConverter() {
            super( LayerGroupInfo.class );
        }

        @Override
        protected void postDoMarshal(Object result,
                HierarchicalStreamWriter writer, MarshallingContext context) {
            callback.postEncodeLayerGroup((LayerGroupInfo)result, writer, context);
        }
        
        @Override
        public Object doUnmarshal(Object result, HierarchicalStreamReader reader,
                UnmarshallingContext context) {

            LayerGroupInfoImpl lgi = (LayerGroupInfoImpl) super
                    .doUnmarshal(result, reader, context);
            MetadataMap metadata = lgi.getMetadata();
            if (lgi.getAuthorityURLs() == null && metadata != null) {
                String serialized = metadata.get("authorityURLs", String.class);
                List<AuthorityURLInfo> authorities;
                if (serialized == null) {
                    authorities = new ArrayList<AuthorityURLInfo>(1);
                } else {
                    authorities = AuthorityURLInfoInfoListConverter.fromString(serialized);
                }
                lgi.setAuthorityURLs(authorities);
            }
            if (lgi.getIdentifiers() == null && metadata != null) {
                String serialized = metadata.get("identifiers", String.class);
                List<LayerIdentifierInfo> identifiers;
                if (serialized == null) {
                    identifiers = new ArrayList<LayerIdentifierInfo>(1);
                } else {
                    identifiers = LayerIdentifierInfoListConverter.fromString(serialized);
                }
                lgi.setIdentifiers(identifiers);
            }
            return lgi;
        }
        
        @Override
        protected void doMarshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {

//            LayerGroupInfo l = (LayerGroupInfo) source;
//            
//            {
//                String authUrlsSerializedForm = AuthorityURLInfoInfoListConverter.toString(l
//                        .getAuthorityURLs());
//                if (null != authUrlsSerializedForm) {
//                    l.getMetadata().put("authorityURLs", authUrlsSerializedForm);
//                }
//            }
//
//            {
//                String identifiersSerializedForm = LayerIdentifierInfoListConverter.toString(l
//                        .getIdentifiers());
//                if (null != identifiersSerializedForm) {
//                    l.getMetadata().put("identifiers", identifiersSerializedForm);
//                }
//            }

            super.doMarshal(source, writer, context);
        }
    }


    class VirtualTableConverter implements Converter {

        public void marshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {
            VirtualTable vt = (VirtualTable) source;
            writer.startNode("name");
            writer.setValue(vt.getName());
            writer.endNode();
            writer.startNode("sql");
            writer.setValue(vt.getSql());
            writer.endNode();
            if(vt.getPrimaryKeyColumns() != null) {
                for(String pk : vt.getPrimaryKeyColumns()) {
                    writer.startNode("keyColumn");
                    writer.setValue(pk);
                    writer.endNode();
                }
            }
            if(vt.getGeometries() != null) {
                for (String geom : vt.getGeometries()) {
                    writer.startNode("geometry");
                    writer.startNode("name");
                    writer.setValue(geom);
                    writer.endNode();
                    writer.startNode("type");
                    writer.setValue(Geometries.getForBinding(vt.getGeometryType(geom)).getName());
                    writer.endNode();
                    writer.startNode("srid");
                    writer.setValue(String.valueOf(vt.getNativeSrid(geom)));
                    writer.endNode();
                    writer.endNode();
                }
            }
            if(vt.getParameterNames().size() > 0) {
                for(String name : vt.getParameterNames()) {
                    VirtualTableParameter param = vt.getParameter(name);
                    writer.startNode("parameter");
                    writer.startNode("name");
                    writer.setValue(name);
                    writer.endNode();
                    if(param.getDefaultValue() != null) {
                        writer.startNode("defaultValue");
                        writer.setValue(param.getDefaultValue());
                        writer.endNode();
                    }
                    if(param.getValidator() != null) {
                        if(param.getValidator() instanceof RegexpValidator) {
                            writer.startNode("regexpValidator");
                            writer.setValue(((RegexpValidator) param.getValidator()).getPattern().pattern());
                            writer.endNode();
                        } else {
                            throw new RuntimeException("Cannot handle this type of validator," +
                            		" please extend the VirtualTableConverter " + param.getValidator().getClass());
                        }
                    }
                    writer.endNode();
                }
            }
        }

        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            String name = readValue("name", String.class, reader);
            String sql = readValue("sql", String.class, reader);
            VirtualTable vt = new VirtualTable(name, sql);
            List<String> primaryKeys = new ArrayList<String>();
            while(reader.hasMoreChildren()) {
                reader.moveDown();
                if(reader.getNodeName().equals("keyColumn")) {
                    primaryKeys.add(reader.getValue());
                } else if(reader.getNodeName().equals("geometry")) {
                    String geomName = readValue("name", String.class, reader);
                    Geometries geomType = Geometries.getForName(readValue("type", String.class, reader));
                    Class type = geomType == null ? Geometry.class : geomType.getBinding();
                    int srid = readValue("srid", Integer.class, reader);
                    vt.addGeometryMetadatata(geomName, type, srid);
                } else if(reader.getNodeName().equals("parameter")) {
                    String pname = readValue("name", String.class, reader);
                    String defaultValue = null;
                    Validator validator = null;
                    while(reader.hasMoreChildren()) {
                        reader.moveDown();
                        if(reader.getNodeName().equals("defaultValue")) {
                            defaultValue = reader.getValue();
                        } else if(reader.getNodeName().equals("regexpValidator")) {
                            validator = new RegexpValidator(reader.getValue());
                        }
                        reader.moveUp();
                    }
                    
                    vt.addParameter(new VirtualTableParameter(pname, defaultValue, validator));
                }
                reader.moveUp();
            }
            vt.setPrimaryKeyColumns(primaryKeys);
            
            return vt;
        }
        
        <T> T readValue(String name, Class<T> type, HierarchicalStreamReader reader) {
           if(!reader.hasMoreChildren()) {
               throw new IllegalArgumentException("Expected element " + name + " but could not find it");
           }
           reader.moveDown();
           if(!name.equals(reader.getNodeName())) {
               throw new IllegalArgumentException("Expected element " + name + " but found " + reader.getNodeName() + " instead");
           }
           String value = reader.getValue();
           reader.moveUp();
           return Converters.convert(value, type);
        }

        public boolean canConvert(Class type) {
            return VirtualTable.class.isAssignableFrom(type);
        }
        
    }

    static class KeywordInfoConverter extends AbstractSingleValueConverter {

        static Pattern RE = Pattern.compile(
            "([^\\\\]+)(?:\\\\@language=([^\\\\]+)\\\\;)?(?:\\\\@vocabulary=([^\\\\]+)\\\\;)?");
        
        @Override
        public boolean canConvert(Class type) {
            return Keyword.class.isAssignableFrom(type);
        }

        @Override
        public Object fromString(String str) {
            Matcher m = RE.matcher(str);
            if (!m.matches()) {
                throw new IllegalArgumentException(
                    String.format("%s does not match regular expression: %s", str, RE));
            }

            KeywordInfo kw = new Keyword(m.group(1));
            if (m.group(2) != null) {
                kw.setLanguage(m.group(2));
            }
            if (m.group(3) != null) {
                kw.setVocabulary(m.group(3));
            }
            return kw;
        }

        @Override
        public String toString(Object obj) {
            KeywordInfo kw = (KeywordInfo) obj;
            
            StringBuilder sb = new StringBuilder();
            sb.append(kw.getValue());
            if (kw.getLanguage() != null) {
                sb.append("\\@language=").append(kw.getLanguage()).append("\\;");
            }
            if (kw.getVocabulary() != null) {
                sb.append("\\@vocabulary=").append(kw.getVocabulary()).append("\\;");
            }
            return sb.toString();
        }
    }

    class KeywordListConverter extends LaxCollectionConverter {

        public KeywordListConverter() {
            super(getXStream().getMapper());
        }

        @Override
        protected Object readItem(HierarchicalStreamReader reader, UnmarshallingContext context,
                Object current) {
            return context.convertAnother(current, Keyword.class);
        }

        @Override
        protected void writeItem(Object item, MarshallingContext context,
                HierarchicalStreamWriter writer) {
            ExtendedHierarchicalStreamWriterHelper.startNode(writer, "string", Keyword.class);
            context.convertAnother(item);
            writer.endNode();
        }

    }
}
