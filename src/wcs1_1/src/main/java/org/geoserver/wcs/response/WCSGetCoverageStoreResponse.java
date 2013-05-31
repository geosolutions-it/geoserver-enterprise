/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import static org.geoserver.ows.util.ResponseUtils.appendPath;
import static org.geoserver.ows.util.ResponseUtils.buildURL;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import net.opengis.wcs11.GetCoverageType;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Response;
import org.geoserver.ows.URLMangler.URLType;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.WCSInfo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.coverage.grid.GridCoverage;
import org.vfny.geoserver.global.GeoserverDataDirectory;
import org.vfny.geoserver.wcs.WcsException;
import org.vfny.geoserver.wcs.responses.CoverageResponseDelegate;
import org.vfny.geoserver.wcs.responses.CoverageResponseDelegateFactory;

/**
 * Response object for the store=true path, that is, one that stores the coverage
 * on disk and returns its path thru the Coverages document
 * @author Andrea Aime - TOPP
 */
public class WCSGetCoverageStoreResponse extends Response {
    
    GeoServer geoServer;
    Catalog catalog;
    
    public WCSGetCoverageStoreResponse(GeoServer gs) {
        super(GridCoverage[].class);
        this.geoServer = gs;
        this.catalog = gs.getCatalog();
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return "application/xml";
    }
    
    @Override
    public boolean canHandle(Operation operation) {
        // this one can handle GetCoverage responses where store = false
        if(!(operation.getParameters()[0] instanceof GetCoverageType))
            return false;
        
        GetCoverageType getCoverage = (GetCoverageType) operation.getParameters()[0];
        return getCoverage.getOutput().isStore();
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException,
            ServiceException {
        GridCoverage[] coverages = (GridCoverage[]) value;

        // grab the delegate for coverage encoding
        GetCoverageType request = (GetCoverageType) operation.getParameters()[0];
        String outputFormat = request.getOutput().getFormat();
        CoverageResponseDelegate delegate = CoverageResponseDelegateFactory
                .encoderFor(outputFormat);
        if (delegate == null)
            throw new WcsException("Could not find encoder for output format " + outputFormat);

        // grab the coverage info for Coverages document encoding
        final GridCoverage2D coverage = (GridCoverage2D) coverages[0];
        CoverageInfo coverageInfo = catalog.getCoverageByName(request.getIdentifier().getValue());
        
        // write the coverage to temporary storage in the data dir
        File wcsStore = null;
        try {
            File temp = GeoserverDataDirectory.findCreateConfigDir("temp");
            wcsStore = new File(temp, "wcs");
            if(!wcsStore.exists())
                wcsStore.mkdir();
        } catch(Exception e) {
            throw new WcsException("Could not create the temporary storage directory for WCS");
        }
        
        // Make sure we create a file name that's not already there (even if splitting the same nanosecond
        // with two requests should not ever happen...)
        File coverageFile = null;
        while(true) {
            // TODO: find a way to get good extensions
            coverageFile = new File(wcsStore, coverageInfo.getName().replace(':', '_') + "_" + System.nanoTime() + "." + delegate.getFileExtension());
            if(!coverageFile.exists())
                break;
        }
       
        // store the coverage
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(coverageFile));
            delegate.prepare(outputFormat, coverage);
            delegate.encode(os);
            os.flush();
        } finally {
            if(os != null) os.close();
        }
        System.out.println(coverageFile);
        
        // build the path where the clients will be able to retrieve the coverage files
        final String coverageLocation = buildURL(request.getBaseUrl(), 
                appendPath("temp/wcs", coverageFile.getName()), null, URLType.RESOURCE);
        
        // build the response
        WCSInfo wcs = geoServer.getService(WCSInfo.class);
        CoveragesTransformer tx = new CoveragesTransformer(wcs, request, coverageLocation);
        try {
            tx.transform(coverageInfo, output);
        } catch (TransformerException e) {
            throw new WcsException("Failure trying to encode Coverages response", e);
        }
    }

}
