/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs.kvp;

import java.util.Map;

import net.opengis.wcs10.DescribeCoverageType;
import net.opengis.wcs10.Wcs10Factory;

import org.geoserver.catalog.Catalog;
import org.geoserver.ows.kvp.EMFKvpRequestReader;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.WcsException.WcsExceptionCode;

/**
 * Describe coverage kvp reader TODO: check if this reader class is really necessary
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class Wcs10DescribeCoverageKvpRequestReader extends EMFKvpRequestReader {
    private Catalog catalog;

    public Wcs10DescribeCoverageKvpRequestReader(Catalog catalog) {
        super(DescribeCoverageType.class, Wcs10Factory.eINSTANCE);
        this.catalog = catalog;
    }

    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        // let super do its thing
        request = super.read(request, kvp, rawKvp);

        DescribeCoverageType describeCoverage = (DescribeCoverageType) request;
        // if not specified, throw a resounding exception (by spec)
        if (!describeCoverage.isSetVersion())
            throw new WcsException("Version has not been specified",
                    WcsExceptionCode.MissingParameterValue, "version");

        return request;
    }
}
