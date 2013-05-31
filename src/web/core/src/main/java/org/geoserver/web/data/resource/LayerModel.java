package org.geoserver.web.data.resource;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.web.GeoServerApplication;

/**
 * A model that serializes the layer fully, and re-attaches it to the catalog
 * on deserialization
 * @author Andrea Aime - OpenGeo
 *
 */
@SuppressWarnings("serial")
public class LayerModel implements IModel {
    LayerInfo layerInfo;
    
    public LayerModel(LayerInfo layerInfo) {
        setObject(layerInfo);
    }

    public Object getObject() {
        if(layerInfo.getResource().getCatalog() == null)
            new CatalogBuilder(GeoServerApplication.get().getCatalog()).attach(layerInfo);
        return layerInfo;
    }

    public void setObject(Object object) {
        //workaround for dbconfig, by "dettaching" we force hibernate to reload the object
        // fully initialized with no lazy lists or proxies
        this.layerInfo = GeoServerApplication.get().getCatalog().detach((LayerInfo) object);
    }

    public void detach() {
        // nothing specific to do
    }

    

}
