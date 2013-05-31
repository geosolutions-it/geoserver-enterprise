/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents a WMS 1.1.1 GetFeatureInfo request.
 * <p>
 * The "GetMap" part of the request is represented by a <code>GetMapRequest</code> object by itself.
 * It is intended to provide enough map context information about the map over the GetFeatureInfo
 * request is performed.
 * </p>
 * 
 * @author Gabriel Roldan
 * @version $Id$
 */
public class GetFeatureInfoRequest extends WMSRequest {
    private static final String DEFAULT_EXCEPTION_FORMAT = "application/vnd.ogc.se_xml";

    private static final int DEFAULT_MAX_FEATURES = 1;

    /**
     * Holds the GetMap part of the GetFeatureInfo request, wich is meant to provide enough context
     * information about the map over the GetFeatureInfo request is being made.
     */
    private GetMapRequest getMapRequest;

    /**
     * List of FeatureTypeInfo's parsed from the <code>QUERY_LAYERS</code> mandatory parameter.
     */
    private List<MapLayerInfo> queryLayers;

    /**
     * Holder for the <code>INFO_FORMAT</code> optional parameter
     */
    private String infoFormat;

    /**
     * Holder for the <code>FEATURE_COUNT</code> optional parameter. Deafults to 1.
     */
    private int featureCount = DEFAULT_MAX_FEATURES;

    /**
     * Holds the value of the required <code>X</code> parameter
     */
    private int XPixel;

    /**
     * Holds the value of the requiered <code>Y</code> parameter
     */
    private int YPixel;
    
    /**
     * Property selection, if any (one list per layer)
     */
    private List<List<String>> propertyNames;

    /**
     * Holder for the optional <code>EXCEPTIONS</code> parameter, defaults to
     * <code>"application/vnd.ogc.se_xml"</code>
     */
    private String exeptionFormat = DEFAULT_EXCEPTION_FORMAT;

    public GetFeatureInfoRequest() {
        super("GetFeatureInfo");
    }

    /**
     * @return Returns the exeptionFormat.
     */
    public String getExceptions() {
        return exeptionFormat;
    }

    /**
     * @param exeptionFormat
     *            The exeptionFormat to set.
     */
    public void setExceptions(String exeptionFormat) {
        this.exeptionFormat = exeptionFormat;
    }

    /**
     * @return Returns the featureCount.
     */
    public int getFeatureCount() {
        return featureCount;
    }

    /**
     * @param featureCount
     *            The featureCount to set.
     */
    public void setFeatureCount(int featureCount) {
        this.featureCount = featureCount;
    }

    /**
     * @return Returns the getMapRequest.
     */
    public GetMapRequest getGetMapRequest() {
        return getMapRequest;
    }

    /**
     * @param getMapRequest
     *            The getMapRequest to set.
     */
    public void setGetMapRequest(GetMapRequest getMapRequest) {
        this.getMapRequest = getMapRequest;
    }

    /**
     * @return Returns the infoFormat.
     */
    public String getInfoFormat() {
        return infoFormat;
    }

    /**
     * @param infoFormat
     *            The infoFormat to set.
     */
    public void setInfoFormat(String infoFormat) {
        this.infoFormat = infoFormat;
    }

    /**
     * @return Returns the queryLayers.
     */
    public List<MapLayerInfo> getQueryLayers() {
        return queryLayers;
    }

    /**
     * @param queryLayers
     *            The queryLayers to set.
     */
    public void setQueryLayers(List<MapLayerInfo> queryLayers) {
        this.queryLayers = queryLayers;
    }

    /**
     * @return Returns the xPixel.
     */
    public int getXPixel() {
        return XPixel;
    }

    /**
     * @param pixel
     *            The xPixel to set.
     */
    public void setXPixel(int pixel) {
        XPixel = pixel;
    }

    /**
     * @return Returns the yPixel.
     */
    public int getYPixel() {
        return YPixel;
    }

    /**
     * @param pixel
     *            The yPixel to set.
     */
    public void setYPixel(int pixel) {
        YPixel = pixel;
    }

    /**
     * The property selection, if any
     * @return
     */
    public List<List<String>> getPropertyNames() {
        return propertyNames;
    }

    /**
     * Sets the property selection
     * @param propertyNames
     */
    public void setPropertyNames(List<List<String>> propertyNames) {
        this.propertyNames = propertyNames;
    }
}
