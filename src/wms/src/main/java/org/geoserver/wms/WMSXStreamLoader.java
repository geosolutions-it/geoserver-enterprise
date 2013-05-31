/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.AuthorityURLInfoInfoListConverter;
import org.geoserver.config.util.LayerIdentifierInfoListConverter;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.Version;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Loads and persist the {@link WMSInfo} object to and from xstream persistence.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public class WMSXStreamLoader extends XStreamServiceLoader<WMSInfo> {

    public WMSXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "wms");
    }

    public Class<WMSInfo> getServiceClass() {
        return WMSInfo.class;
    }

    protected WMSInfo createServiceFromScratch(GeoServer gs) {
        WMSInfo wms = new WMSInfoImpl();
        wms.setName("WMS");
        return wms;
    }

    @Override
    protected void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        super.initXStreamPersister(xp, gs);
        xp.getXStream().alias("wms", WMSInfo.class, WMSInfoImpl.class);
        xp.getXStream().registerConverter(new WMSInfoConverter(xp.getXStream()));
    }

    @Override
    protected WMSInfo initialize(WMSInfo service) {
        super.initialize(service);

        final Version version_1_1_1 = WMS.VERSION_1_1_1;
        final Version version_1_3_0 = WMS.VERSION_1_3_0;

        if (!service.getVersions().contains(version_1_1_1)) {
            service.getVersions().add(version_1_1_1);
        }
        if (!service.getVersions().contains(version_1_3_0)) {
            service.getVersions().add(version_1_3_0);
        }
        if (service.getSRS() == null) {
            ((WMSInfoImpl) service).setSRS(new ArrayList<String>());
        }
        return service;
    }
    
    
    /**
     * Converter for WMSInfo, stores authority urls and identifiers under metadata map in the 2.1.x
     * series.
     * 
     * @since 2.1.3
     */
    class WMSInfoConverter extends ReflectionConverter {

        public WMSInfoConverter(XStream xs) {
            super(xs.getMapper(), xs.getReflectionProvider());
        }

        @Override
        public boolean canConvert(Class type) {
            return WMSInfo.class.isAssignableFrom(type);
        }

        /**
         * @since 2.1.3
         */
        @Override
        protected void doMarshal(Object source, HierarchicalStreamWriter writer,
                MarshallingContext context) {

//            WMSInfo service = (WMSInfo) source;
//            {
//                String authUrlsSerializedForm = AuthorityURLInfoInfoListConverter.toString(service
//                        .getAuthorityURLs());
//                if (null != authUrlsSerializedForm) {
//                    service.getMetadata().put("authorityURLs", authUrlsSerializedForm);
//                }
//            }
//
//            {
//                String identifiersSerializedForm = LayerIdentifierInfoListConverter
//                        .toString(service.getIdentifiers());
//                if (null != identifiersSerializedForm) {
//                    service.getMetadata().put("identifiers", identifiersSerializedForm);
//                }
//            }

            super.doMarshal(source, writer, context);
        }

        @Override
        public Object doUnmarshal(Object result, HierarchicalStreamReader reader,
                UnmarshallingContext context) {

            WMSInfoImpl service = (WMSInfoImpl) super.doUnmarshal(result, reader, context);
            MetadataMap metadata = service.getMetadata();
            // for backwards compatibility with 2.1.3+ data directories, check if the auth urls and
            // identifiers are stored in the metadata map
            if (service.getAuthorityURLs() == null && metadata != null) {
                String serialized = metadata.get("authorityURLs", String.class);
                List<AuthorityURLInfo> authorities;
                if (serialized == null) {
                    authorities = new ArrayList<AuthorityURLInfo>(1);
                } else {
                    authorities = AuthorityURLInfoInfoListConverter.fromString(serialized);
                }
                service.setAuthorityURLs(authorities);
            }
            if (service.getIdentifiers() == null && metadata != null) {
                String serialized = metadata.get("identifiers", String.class);
                List<LayerIdentifierInfo> identifiers;
                if (serialized == null) {
                    identifiers = new ArrayList<LayerIdentifierInfo>(1);
                } else {
                    identifiers = LayerIdentifierInfoListConverter.fromString(serialized);
                }
                service.setIdentifiers(identifiers);
            }
            return service;
        }
    }
}
