package org.geoserver.ows.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Map which makes keys case insensitive.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class KvpMap extends HashMap {

    private static final long serialVersionUID = 1L;

    public KvpMap() {
        super();
    }
    
    public KvpMap( Map other ) {
        this();
        for ( Iterator e = other.entrySet().iterator(); e.hasNext(); ) {
            Map.Entry entry = (Map.Entry) e.next();
            put( entry.getKey(), entry.getValue() );
        }
    }
    public boolean containsKey(Object key) {
        return super.containsKey(upper(key));
    }

    public Object get(Object key) {
        return super.get(upper(key));
    }

    public Object put(Object key, Object value) {
        return super.put(upper(key), value);
    }

    Object upper(Object key) {
        if ((key != null) && key instanceof String) {
            return ((String) key).toUpperCase();
        }

        return key;
    }
}
