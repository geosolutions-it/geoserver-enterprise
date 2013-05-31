package org.geoserver.wcs;

import java.util.Map;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.LegacyServiceLoader;
import org.geoserver.config.util.LegacyServicesReader;
import org.geotools.util.Version;

/**
 * Configuration loader for Web Coverage Service.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * 
 */
public class WCSLoader extends LegacyServiceLoader<WCSInfo> {

    public Class<WCSInfo> getServiceClass() {
        return WCSInfo.class;
    }

    public WCSInfo load(LegacyServicesReader reader, GeoServer gs) throws Exception {

        WCSInfoImpl wcs = new WCSInfoImpl();
        wcs.setId("wcs");

        Map<String, Object> map = reader.wcs();
        readCommon(wcs, map, gs);

        // wcs.setGMLPrefixing((Boolean)map.get( "gmlPrefixing"));
        wcs.getVersions().add(new Version("1.0.0"));
        wcs.getVersions().add(new Version("1.1.1"));

        return wcs;
    }

    public void save(WCSInfo service, GeoServer gs) throws Exception {
    }
}
