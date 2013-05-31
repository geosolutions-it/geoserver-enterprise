/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import java.io.IOException;
import java.util.logging.Level;

import org.geotools.data.ResourceInfo;
import org.geotools.data.ServiceInfo;
import org.geotools.data.ows.GetCapabilitiesRequest;
import org.geotools.data.ows.GetCapabilitiesResponse;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.WebMapServer;
import org.geotools.data.wms.request.DescribeLayerRequest;
import org.geotools.data.wms.request.GetFeatureInfoRequest;
import org.geotools.data.wms.request.GetLegendGraphicRequest;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.request.GetStylesRequest;
import org.geotools.data.wms.request.PutStylesRequest;
import org.geotools.data.wms.response.DescribeLayerResponse;
import org.geotools.data.wms.response.GetFeatureInfoResponse;
import org.geotools.data.wms.response.GetLegendGraphicResponse;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.data.wms.response.GetStylesResponse;
import org.geotools.data.wms.response.PutStylesResponse;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.ows.ServiceException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Applies security around the web map server
 * @author Andrea Aime - GeoSolutions
 *
 */
public class SecuredWebMapServer extends WebMapServer {

    WebMapServer delegate;

    public SecuredWebMapServer(WebMapServer delegate) throws IOException, ServiceException {
        super(delegate.getCapabilities());
        this.delegate = delegate;
    }
    
    public GetFeatureInfoRequest createGetFeatureInfoRequest(GetMapRequest getMapRequest) {
        return new SecuredGetFeatureInfoRequest(delegate.createGetFeatureInfoRequest(getMapRequest), getMapRequest);
    }
    
    public GetMapRequest createGetMapRequest() {
        return new SecuredGetMapRequest(delegate.createGetMapRequest());
    }
    
    // -------------------------------------------------------------------------------------------
    //
    // Purely delegated methods
    //
    // -------------------------------------------------------------------------------------------

    public GetStylesResponse issueRequest(GetStylesRequest request) throws IOException,
            ServiceException {
        return delegate.issueRequest(request);
    }

    public PutStylesResponse issueRequest(PutStylesRequest request) throws IOException,
            ServiceException {
        return delegate.issueRequest(request);
    }

    public GetLegendGraphicResponse issueRequest(GetLegendGraphicRequest request)
            throws IOException, ServiceException {

        return delegate.issueRequest(request);
    }

    public DescribeLayerResponse issueRequest(DescribeLayerRequest request) throws IOException,
            ServiceException {
        return delegate.issueRequest(request);
    }

    public GetCapabilitiesResponse issueRequest(GetCapabilitiesRequest request) throws IOException,
            ServiceException {
        return delegate.issueRequest(request);
    }

    public GetFeatureInfoResponse issueRequest(GetFeatureInfoRequest request) throws IOException,
            ServiceException {
        return delegate.issueRequest(request);
    }

    public GetMapResponse issueRequest(GetMapRequest request) throws IOException, ServiceException {
        return delegate.issueRequest(request);
    }

    public DescribeLayerRequest createDescribeLayerRequest() throws UnsupportedOperationException {
        return delegate.createDescribeLayerRequest();
    }

    public GetLegendGraphicRequest createGetLegendGraphicRequest()
            throws UnsupportedOperationException {
        return delegate.createGetLegendGraphicRequest();
    }

    public GetStylesRequest createGetStylesRequest() throws UnsupportedOperationException {
        return delegate.createGetStylesRequest();
    }

    public PutStylesRequest createPutStylesRequest() throws UnsupportedOperationException {
        return delegate.createPutStylesRequest();
    }

    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    public WMSCapabilities getCapabilities() {
        return delegate.getCapabilities();
    }

    public GeneralEnvelope getEnvelope(Layer layer, CoordinateReferenceSystem crs) {
        return delegate.getEnvelope(layer, crs);
    }

    public ServiceInfo getInfo() {
        return delegate.getInfo();
    }

    public ResourceInfo getInfo(Layer resource) {
        return delegate.getInfo(resource);
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public void setLoggingLevel(Level newLevel) {
        delegate.setLoggingLevel(newLevel);
    }

    public String toString() {
        return "SecuredWebMapServer " + delegate.toString();
    }

}
