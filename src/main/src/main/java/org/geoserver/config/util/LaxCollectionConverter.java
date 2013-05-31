package org.geoserver.config.util;

import java.util.Collection;

import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * Custom collection converter.
 * <p>
 * Used because because the basic only handles a handlful of collection implementations. And
 * things like hibernate have there own implementation which leads to problems. 
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class LaxCollectionConverter extends CollectionConverter {

    public LaxCollectionConverter(Mapper mapper) {
        super(mapper);
    }
    
    @Override
    public boolean canConvert(Class type) {
        return Collection.class.isAssignableFrom(type);
    }
}
