/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * Adapter for CatalogVisitor which stubs all methods allowing subclasses to 
 * selectively implement visit methods of relevance.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class CatalogVisitorAdapter implements CatalogVisitor {

    public void visit(Catalog catalog) {
    
    }
    
    public void visit( WorkspaceInfo workspace ) {
        
    }
    
    public void visit( NamespaceInfo workspace ) {
        
    }
    
    public void visit( DataStoreInfo dataStore ) {
        
    }
    
    public void visit( CoverageStoreInfo coverageStore ) {
        
    }
    
    public void visit( WMSStoreInfo wmsStore ) {
        
    }
   
    public void visit( FeatureTypeInfo featureType ) {
        
    }
    
    public void visit( CoverageInfo coverage ) {
        
    }
    
    public void visit( LayerInfo layer ) {
        
    }
    
    public void visit( LayerGroupInfo layerGroup ) {
        
    }
    
    public void visit( StyleInfo style ) {
        
    }

    public void visit(WMSLayerInfo wmsLayerInfoImpl) {
                
    }
}
