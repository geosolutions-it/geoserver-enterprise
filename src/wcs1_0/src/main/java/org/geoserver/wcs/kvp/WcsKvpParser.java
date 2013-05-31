package org.geoserver.wcs.kvp;

import org.geoserver.ows.KvpParser;
import org.geotools.util.Version;

/**
 * Kvp parser specific to WCS 1.0.0
 * <p>
 * This class should be extended by kvp parsers which should only engage on a wcs 1.0.0 request.
 * </p>
 * 
 * @author Justin Deoliveira, The Open Planning Project
 * @author Alessio Fabiani, GeoSolutions
 */
public abstract class WcsKvpParser extends KvpParser {

    /**
     * Constructor for use with all wcs 1.0.0 requests.
     */
    public WcsKvpParser(String key, Class<?> binding) {
        this(key, binding, null);
    }

    /**
     * Constrcutor for use with a specific wcs 1.0.0 request.
     */
    public WcsKvpParser(String key, Class<?> binding, String request) {
        super(key, binding);
        setService("wcs");
        setVersion(new Version("1.0.0"));
        setRequest(request);
    }

}
