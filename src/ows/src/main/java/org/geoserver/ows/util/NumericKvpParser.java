/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.util;

import org.geoserver.ows.KvpParser;
import org.geotools.util.Converters;


/**
 * A kvp parser which parses a value into a numeric value.
 * <p>
 * The type of the number is determined by {@link #getBinding()}. It must be
 * assignable from {@link Number}.
 * </p>
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *
 */
public class NumericKvpParser extends KvpParser {
    /**
     * Constructs a numeric kvp parser with the specified key and binding.
     *
     * @param key The key to bind to.
     * @param binding The resulting type of parsed object, must be a subclass
     * of {@link Number}.
     */
    public NumericKvpParser(String key, Class binding) {
        super(key, binding);

        if (!Number.class.isAssignableFrom(binding)) {
            throw new IllegalArgumentException("Number is not assignable from: " + binding);
        }
    }

    /**
     * Parses the string into a numberic value.
     */
    public Object parse(String value) throws Exception {
        return Converters.convert(value, getBinding());
    }
}
