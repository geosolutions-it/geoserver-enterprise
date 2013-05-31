/* Copyright (c) 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.config.impl.ServiceInfoImpl;

public class WMSInfoImpl extends ServiceInfoImpl implements WMSInfo {

    List<String> srs = new ArrayList<String>();

    Boolean bboxForEachCRS;

    WatermarkInfo watermark = new WatermarkInfoImpl();

    WMSInterpolation interpolation;
    

    /**
     * This property is transient in 2.1.x series and stored under the metadata map with key
     * "authorityURLs", and a not transient in the 2.2.x series.
     * 
     * @since 2.1.3
     */
    protected List<AuthorityURLInfo> authorityURLs = new ArrayList<AuthorityURLInfo>(2);
    

    /**
     * This property is transient in 2.1.x series and stored under the metadata map with key
     * "identifiers", and a not transient in the 2.2.x series.
     * 
     * @since 2.1.3
     */
    protected List<LayerIdentifierInfo> identifiers = new ArrayList<LayerIdentifierInfo>(2);

    int maxBuffer;

    int maxRequestMemory;

    int maxRenderingTime;

    int maxRenderingErrors;

    private String capabilitiesErrorHandling;

    public WMSInfoImpl() {
        setId("wms");
        authorityURLs = new ArrayList<AuthorityURLInfo>(2);
        identifiers = new ArrayList<LayerIdentifierInfo>(2);
    }

    public int getMaxRequestMemory() {
        return maxRequestMemory;
    }

    public void setMaxRequestMemory(int maxRequestMemory) {
        this.maxRequestMemory = maxRequestMemory;
    }

    public WatermarkInfo getWatermark() {
        return watermark;
    }

    public void setWatermark(WatermarkInfo watermark) {
        this.watermark = watermark;
    }

    public void setInterpolation(WMSInterpolation interpolation) {
        this.interpolation = interpolation;
    }

    public WMSInterpolation getInterpolation() {
        return interpolation;
    }

    public List<String> getSRS() {
        return srs;
    }

    public void setSRS(List<String> srs) {
        this.srs = srs;
    }

    public Boolean isBBOXForEachCRS() {
        if (bboxForEachCRS != null) {
            return bboxForEachCRS;
        }
        
        //check the metadata map if upgrading from 2.1.x
        Boolean bool = getMetadata().get("bboxForEachCRS", Boolean.class);
        return bool != null && bool;
    }

    public void setBBOXForEachCRS(Boolean bboxForEachCRS) {
        this.bboxForEachCRS = bboxForEachCRS;
    }

    public int getMaxBuffer() {
        return maxBuffer;
    }

    public void setMaxBuffer(int maxBuffer) {
        this.maxBuffer = maxBuffer;
    }

    public int getMaxRenderingTime() {
        return maxRenderingTime;
    }

    public void setMaxRenderingTime(int maxRenderingTime) {
        this.maxRenderingTime = maxRenderingTime;
    }

    public int getMaxRenderingErrors() {
        return maxRenderingErrors;
    }

    public void setMaxRenderingErrors(int maxRenderingErrors) {
        this.maxRenderingErrors = maxRenderingErrors;
    }

    @Override
    public List<AuthorityURLInfo> getAuthorityURLs() {
        return authorityURLs;
    }

    public void setAuthorityURLs(List<AuthorityURLInfo> urls) {
        this.authorityURLs = urls;
    }

    @Override
    public List<LayerIdentifierInfo> getIdentifiers() {
        return identifiers;
    }

    public void setIdentifiers(List<LayerIdentifierInfo> identifiers) {
        this.identifiers = identifiers;
    }
}
