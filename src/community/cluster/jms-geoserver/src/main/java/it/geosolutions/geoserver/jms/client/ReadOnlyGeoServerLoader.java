package it.geosolutions.geoserver.jms.client;

import java.io.IOException;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.ConfigurationListener;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.GeoServerPersister;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;

public class ReadOnlyGeoServerLoader extends GeoServerLoader {//implements BeanFactoryPostProcessor {

//    private ConfigurationListener listener;
//
//    private DummyGeoServerPersister persister;

    public ReadOnlyGeoServerLoader(final GeoServerResourceLoader resourceLoader, GeoServer geoserver) {
        super(resourceLoader,geoserver,new XStreamPersisterFactory().createXMLPersister());
    }

    protected void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        catalog.setResourceLoader(resourceLoader);

        readCatalog(catalog, xp);

        // add the listener which will persist changes
//        catalog.addListener(new GeoServerPersister(resourceLoader, xp));
    }


//    @Autowired
//    public List<XStreamServiceLoader> loaders;
    
    protected void loadGeoServer(final GeoServer geoServer, XStreamPersister xp) throws Exception {
//        if (listener == null && loaders!=null) {
//            listener = new DummyServicePersister(geoserver, loaders);
//            geoServer.addListener(listener);
//        }

//        try {
//            if (this.persister != null) {
//                // avoid having the persister write down new config files while we read the config,
//                // otherwise it'll dump it back in xml files
//                geoserver.removeListener(persister);
//            } else {
//                // lazy creation of the persister at the first need
//                this.persister = new DummyGeoServerPersister();
//            }
            readConfiguration(geoServer, xp);
//        } finally {
//            // attach back the persister
//            geoserver.addListener(persister);
//        }
    }

    @Override
    protected void initializeStyles(Catalog catalog, XStreamPersister xp) throws IOException {
//        // add a persister temporarily in case the styles don't exist on disk
//        DummyGeoServerPersister p = new DummyGeoServerPersister();
//        catalog.addListener(p);

        super.initializeStyles(catalog, xp);

//        catalog.removeListener(p);
    }
}
