/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import net.opengis.ows10.AcceptFormatsType;
import net.opengis.ows10.Ows10Factory;

import org.eclipse.emf.ecore.EObject;
import org.geoserver.ows.KvpParser;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wfs.WFSInfo;
import org.geotools.xml.EMFUtils;


/**
 * Parses a kvp of the form "acceptFormats=format1,format2,...,formatN" into
 * an instance of {@link net.opengis.ows.v1_0_0.AcceptFormatsType}.
 *
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class AcceptFormatsKvpParser extends KvpParser {
    public AcceptFormatsKvpParser() {
        this(AcceptFormatsType.class);
    }

    public AcceptFormatsKvpParser(Class clazz) {
        super("acceptFormats", clazz);
        setVersion(WFSInfo.Version.V_11.getVersion());
    }

    public Object parse(String value) throws Exception {
        List values = KvpUtils.readFlat(value);

        EObject acceptFormats = createObject(); 

        for (Iterator v = values.iterator(); v.hasNext();) {
            ((Collection)EMFUtils.get(acceptFormats, "outputFormat")).add(v.next());
        }

        return acceptFormats;
    }

    protected EObject createObject() {
        return Ows10Factory.eINSTANCE.createAcceptFormatsType();
    }
}
