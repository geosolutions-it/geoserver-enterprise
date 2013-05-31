/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.xml.xs;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.geotools.xml.impl.DatatypeConverterImpl;
import org.geotools.xs.bindings.XSDateBinding;

/**
 * Override of binding for xs:date that forces date to be encoded in UTC
 * timezone.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class DateBinding extends XSDateBinding {
    public String encode(Object object, String value) throws Exception {
        Date date = (Date) object;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

        return DatatypeConverterImpl.getInstance().printDate(calendar);
    }
}
