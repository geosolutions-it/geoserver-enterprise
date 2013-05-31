/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.v1_1_0;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.opengis.wfs.WfsFactory;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.ows.xml.v1_0.OWSConfiguration;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.xml.FeatureTypeSchemaBuilder;
import org.geoserver.wfs.xml.PropertyTypePropertyExtractor;
import org.geoserver.wfs.xml.WFSHandlerFactory;
import org.geoserver.wfs.xml.WFSXmlUtils;
import org.geoserver.wfs.xml.XSQNameBinding;
import org.geoserver.wfs.xml.filter.v1_1.FilterTypeBinding;
import org.geoserver.wfs.xml.filter.v1_1.PropertyNameTypeBinding;
import org.geoserver.wfs.xml.gml3.CircleTypeBinding;
import org.geoserver.wfs.xml.xs.DateBinding;
import org.geotools.data.DataAccess;
import org.geotools.filter.v1_0.OGCBBOXTypeBinding;
import org.geotools.filter.v1_1.OGC;
import org.geotools.filter.v1_1.OGCConfiguration;
import org.geotools.gml2.FeatureTypeCache;
import org.geotools.gml2.SrsSyntax;
import org.geotools.gml3.GML;
import org.geotools.gml3.GMLConfiguration;
import org.geotools.util.logging.Logging;
import org.geotools.xml.Configuration;
import org.geotools.xml.OptionalComponentParameter;
import org.geotools.xs.XS;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.Parameter;
import org.picocontainer.defaults.SetterInjectionComponentAdapter;

public class WFSConfiguration extends Configuration {
    /**
     * logger
     */
    static Logger LOGGER = Logging.getLogger( "org.geoserver.wfs");
    
    /**
     * catalog
     */
    protected Catalog catalog;

    /**
     * Schema builder
     */
    protected FeatureTypeSchemaBuilder schemaBuilder;

    public WFSConfiguration(GeoServer geoServer, FeatureTypeSchemaBuilder schemaBuilder, final WFS wfs) {
        super( wfs );

        this.catalog = geoServer.getCatalog();
        this.schemaBuilder = schemaBuilder;
        
        catalog.addListener(new CatalogListener() {

            public void handleAddEvent(CatalogAddEvent event) {
                if (event.getSource() instanceof FeatureTypeInfo) {
                    reloaded();
                }
            }

            public void handleModifyEvent(CatalogModifyEvent event) {
                if (event.getSource() instanceof DataStoreInfo ||
                    event.getSource() instanceof FeatureTypeInfo || 
                    event.getSource() instanceof NamespaceInfo) {
                    reloaded();
                }
            }

            public void handlePostModifyEvent(CatalogPostModifyEvent event) {
            }

            public void handleRemoveEvent(CatalogRemoveEvent event) {
            }

            public void reloaded() {
                wfs.dispose();
            }
                
        });
        catalog.getResourcePool().addListener(new ResourcePool.Listener() {
            
            public void disposed(FeatureTypeInfo featureType, FeatureType ft) {
            }
            
            public void disposed(CoverageStoreInfo coverageStore, GridCoverageReader gcr) {
            }
            
            public void disposed(DataStoreInfo dataStore, DataAccess da) {
                wfs.dispose();
            }
        });
        geoServer.addListener(new ConfigurationListenerAdapter() {
            
            public void reloaded() {
                wfs.dispose();
            }
            
            public void handleServiceChange(ServiceInfo service, List<String> propertyNames,
                    List<Object> oldValues, List<Object> newValues) {
                if (service instanceof WFSInfo) {
                    reloaded();
                }
            }
        });
        addDependency(new OGCConfiguration());
        addDependency(new GMLConfiguration());
        addDependency(new OWSConfiguration());
    }

    public void setSrsSyntax(SrsSyntax srsSyntax) {
        WFSXmlUtils.setSrsSyntax(this, srsSyntax);
    }

    public SrsSyntax getSrsSyntax() {
        return WFSXmlUtils.getSrsSyntax(this);
    }
    
    protected void registerBindings(MutablePicoContainer container) {
        //Types
        container.registerComponentImplementation(WFS.ACTIONTYPE, ActionTypeBinding.class);
        container.registerComponentImplementation(WFS.ALLSOMETYPE, AllSomeTypeBinding.class);
        container.registerComponentImplementation(WFS.BASE_TYPENAMELISTTYPE,
            Base_TypeNameListTypeBinding.class);
        container.registerComponentImplementation(WFS.BASEREQUESTTYPE, BaseRequestTypeBinding.class);
        container.registerComponentImplementation(WFS.DELETEELEMENTTYPE,
            DeleteElementTypeBinding.class);
        container.registerComponentImplementation(WFS.DESCRIBEFEATURETYPETYPE,
            DescribeFeatureTypeTypeBinding.class);
        container.registerComponentImplementation(WFS.FEATURECOLLECTIONTYPE,
            FeatureCollectionTypeBinding.class);
        container.registerComponentImplementation(WFS.FEATURESLOCKEDTYPE,
            FeaturesLockedTypeBinding.class);
        container.registerComponentImplementation(WFS.FEATURESNOTLOCKEDTYPE,
            FeaturesNotLockedTypeBinding.class);
        container.registerComponentImplementation(WFS.FEATURETYPELISTTYPE,
            FeatureTypeListTypeBinding.class);
        container.registerComponentImplementation(WFS.FEATURETYPETYPE, FeatureTypeTypeBinding.class);
        container.registerComponentImplementation(WFS.GETCAPABILITIESTYPE,
            GetCapabilitiesTypeBinding.class);
        container.registerComponentImplementation(WFS.GETFEATURETYPE, GetFeatureTypeBinding.class);
        container.registerComponentImplementation(WFS.GETFEATUREWITHLOCKTYPE,
            GetFeatureWithLockTypeBinding.class);
        container.registerComponentImplementation(WFS.GETGMLOBJECTTYPE,
            GetGmlObjectTypeBinding.class);
        container.registerComponentImplementation(WFS.GMLOBJECTTYPELISTTYPE,
            GMLObjectTypeListTypeBinding.class);
        container.registerComponentImplementation(WFS.GMLOBJECTTYPETYPE,
            GMLObjectTypeTypeBinding.class);
        container.registerComponentImplementation(WFS.IDENTIFIERGENERATIONOPTIONTYPE,
            IdentifierGenerationOptionTypeBinding.class);
        container.registerComponentImplementation(WFS.INSERTEDFEATURETYPE,
            InsertedFeatureTypeBinding.class);
        container.registerComponentImplementation(WFS.INSERTELEMENTTYPE,
            InsertElementTypeBinding.class);
        container.registerComponentImplementation(WFS.INSERTRESULTSTYPE,
            InsertResultTypeBinding.class);
        container.registerComponentImplementation(WFS.LOCKFEATURERESPONSETYPE,
            LockFeatureResponseTypeBinding.class);
        container.registerComponentImplementation(WFS.LOCKFEATURETYPE, LockFeatureTypeBinding.class);
        container.registerComponentImplementation(WFS.LOCKTYPE, LockTypeBinding.class);
        container.registerComponentImplementation(WFS.METADATAURLTYPE, MetadataURLTypeBinding.class);
        container.registerComponentImplementation(WFS.NATIVETYPE, NativeTypeBinding.class);
        container.registerComponentImplementation(WFS.OPERATIONSTYPE, OperationsTypeBinding.class);
        container.registerComponentImplementation(WFS.OPERATIONTYPE, OperationTypeBinding.class);
        container.registerComponentImplementation(WFS.OUTPUTFORMATLISTTYPE,
            OutputFormatListTypeBinding.class);
        container.registerComponentImplementation(WFS.PROPERTYTYPE, PropertyTypeBinding.class);
        container.registerComponentImplementation(WFS.QUERYTYPE, QueryTypeBinding.class);
        container.registerComponentImplementation(WFS.RESULTTYPETYPE, ResultTypeTypeBinding.class);
        container.registerComponentImplementation(WFS.TRANSACTIONRESPONSETYPE,
            TransactionResponseTypeBinding.class);
        container.registerComponentImplementation(WFS.TRANSACTIONRESULTSTYPE,
            TransactionResultsTypeBinding.class);
        container.registerComponentImplementation(WFS.TRANSACTIONSUMMARYTYPE,
            TransactionSummaryTypeBinding.class);
        container.registerComponentImplementation(WFS.TRANSACTIONTYPE, TransactionTypeBinding.class);
        container.registerComponentImplementation(WFS.TYPENAMELISTTYPE,
            TypeNameListTypeBinding.class);
        container.registerComponentImplementation(WFS.UPDATEELEMENTTYPE,
            UpdateElementTypeBinding.class);
        container.registerComponentImplementation(WFS.WFS_CAPABILITIESTYPE,
            WFS_CapabilitiesTypeBinding.class);
        container.registerComponentImplementation(WFS.XLINKPROPERTYNAME, 
            XlinkPropertyNameBinding.class);

        //cite specific bindings
        container.registerComponentImplementation(
            FeatureReferenceTypeBinding.FeatureReferenceType, 
            FeatureReferenceTypeBinding.class
        );
    }

    public Catalog getCatalog() {
        return catalog;
    }
    
    public void addDependency(Configuration dependency) {
        //override to make public
        super.addDependency(dependency);
    }

    protected void configureContext(MutablePicoContainer context) {
        super.configureContext(context);

        context.registerComponentInstance(WfsFactory.eINSTANCE);
        context.registerComponentInstance(new WFSHandlerFactory(catalog, schemaBuilder));
        context.registerComponentInstance(catalog);
        context.registerComponentImplementation(PropertyTypePropertyExtractor.class);
        context.registerComponentInstance(getSrsSyntax());

        //seed the cache with entries from the catalog
        FeatureTypeCache featureTypeCache = (FeatureTypeCache) context
            .getComponentInstanceOfType(FeatureTypeCache.class);

        Collection featureTypes = catalog.getFeatureTypes();
        for (Iterator f = featureTypes.iterator(); f.hasNext();) {
            FeatureTypeInfo meta = (FeatureTypeInfo) f.next();
            if ( !meta.enabled() ) {
                continue;
            }

            
            FeatureType featureType =  null;
            try {
                featureType = meta.getFeatureType();
            } catch(Exception e) {
                LOGGER.log(Level.WARNING, "Could not load underlying feature type for type " 
                        + meta.getName(), e);
                continue;
            }

            featureTypeCache.put(featureType);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configureBindings(Map bindings) {
        //register our custom bindings
        bindings.put(XS.DATE, DateBinding.class);
        bindings.put(OGC.FilterType, FilterTypeBinding.class);
        bindings.put(OGC.PropertyNameType,
            PropertyNameTypeBinding.class);
        bindings.put(GML.CircleType, CircleTypeBinding.class);

        WFSXmlUtils.registerAbstractGeometryTypeBinding(this, bindings, GML.AbstractGeometryType);

        // use setter injection for OGCBBoxTypeBinding to allow an 
        // optional crs to be set in teh binding context for parsing, this crs
        // is set by the binding of a parent element.
        // note: it is important that this component adapter is non-caching so 
        // that the setter property gets updated properly every time
        bindings.put(
            OGC.BBOXType,    
            new SetterInjectionComponentAdapter(OGC.BBOXType,
                OGCBBOXTypeBinding.class,
                new Parameter[] { new OptionalComponentParameter(CoordinateReferenceSystem.class) }));
        
        // override XSQName binding
        bindings.put(XS.QNAME, XSQNameBinding.class);
    }
}
