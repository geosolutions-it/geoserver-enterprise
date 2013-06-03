package org.geoserver.inspire;

import org.geoserver.test.GeoServerTestSupport;
import org.geotools.util.Converters;

public class InspireConverterFactoryTest extends GeoServerTestSupport{

    
    public void testCodeOnly() {
        UniqueResourceIdentifiers ids = new UniqueResourceIdentifiers();
        ids.add(new UniqueResourceIdentifier("code"));
        String str = Converters.convert(ids, String.class);
        assertEquals("code,,", str);
        UniqueResourceIdentifiers ids2 = Converters.convert(str, UniqueResourceIdentifiers.class);
        assertEquals(ids, ids2);
    }
    
    public void testCodeNamespace() {
        UniqueResourceIdentifiers ids = new UniqueResourceIdentifiers();
        ids.add(new UniqueResourceIdentifier("code", "http://www.geoserver.org"));
        String str = Converters.convert(ids, String.class);
        assertEquals("code,http://www.geoserver.org,", str);
        UniqueResourceIdentifiers ids2 = Converters.convert(str, UniqueResourceIdentifiers.class);
        assertEquals(ids, ids2);
    }
    
    public void testCodeNamespaceMetadata() {
        UniqueResourceIdentifiers ids = new UniqueResourceIdentifiers();
        ids.add(new UniqueResourceIdentifier("code", "http://www.geoserver.org", "http://www.geoserver.org/metadata"));
        String str = Converters.convert(ids, String.class);
        assertEquals("code,http://www.geoserver.org,http://www.geoserver.org/metadata", str);
        UniqueResourceIdentifiers ids2 = Converters.convert(str, UniqueResourceIdentifiers.class);
        assertEquals(ids, ids2);
    }
    
    public void testMulti() {
        UniqueResourceIdentifiers ids = new UniqueResourceIdentifiers();
        ids.add(new UniqueResourceIdentifier("code1"));
        ids.add(new UniqueResourceIdentifier("code2", "http://www.geoserver.org/1"));
        ids.add(new UniqueResourceIdentifier("code3", "http://www.geoserver.org/2", "http://www.geoserver.org/metadata"));
        String str = Converters.convert(ids, String.class);
        assertEquals("code1,,;code2,http://www.geoserver.org/1,;code3,http://www.geoserver.org/2,http://www.geoserver.org/metadata", str);
        UniqueResourceIdentifiers ids2 = Converters.convert(str, UniqueResourceIdentifiers.class);
        assertEquals(ids, ids2);
    }
}
