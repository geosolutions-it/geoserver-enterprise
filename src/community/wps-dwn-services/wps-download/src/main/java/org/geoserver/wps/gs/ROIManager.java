/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.gs;

import java.io.IOException;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Manages the ROI and its CRS
 * 
 * @author Simone Giannecchini, GeoSolutions
 *
 */
final class ROIManager {
    
    final Geometry roi;
    
    Geometry roiInNativeCRS;
    
    Geometry safeRoiInNativeCRS;
    
    CoordinateReferenceSystem nativeCRS;
    
    Geometry roiInTargetCRS;
    
    Geometry safeRoiInTargetCRS;
    
    final CoordinateReferenceSystem roiCRS;
    
    CoordinateReferenceSystem targetCRS;
    
    final boolean isROIBBOX;

    /**
     * Constructor.
     * 
     * @param roi original ROI as a JTS geometry
     * @param roiCRS {@link CoordinateReferenceSystem} for the provided geometry. If this is null the CRS must be provided with the USerData of the
     *        roi
     */
    public ROIManager(Geometry roi, CoordinateReferenceSystem roiCRS) {
        this.roi = roi;
        DownloadUtilities.checkPolygonROI(roi);
        if (roiCRS == null) {
            if(!(roi.getUserData()instanceof CoordinateReferenceSystem)){
                throw new IllegalArgumentException("ROI without a CRS is not usable!");
            }
            this.roiCRS=(CoordinateReferenceSystem) roi.getUserData();
        }else{
            this.roiCRS = roiCRS;
        }
        roi.setUserData(this.roiCRS);
        // is this a bbox 
        isROIBBOX=roi.isRectangle();
    }    

    /**
     * Reproject the initial roi to the provided CRS which is supposely the native CRS
     * of the data to clip.
     * 
     * @param nativeCRS a valid instance of {@link CoordinateReferenceSystem}
     * @throws IOException in case something bad happens.
     */
    public void useNativeCRS(final CoordinateReferenceSystem nativeCRS) throws IOException{
        if (nativeCRS == null) {
            throw new IllegalArgumentException("The provided nativeCRS is null");
        }
        roiInNativeCRS=DownloadUtilities.transformGeometry(roi, nativeCRS);
        DownloadUtilities.checkPolygonROI(roiInNativeCRS);
        if(isROIBBOX){
            // if the ROI is a BBOX we tend to preserve the fact that it is a BBOX
            safeRoiInNativeCRS=roiInNativeCRS.getEnvelope();
        }else{
            safeRoiInNativeCRS=roiInNativeCRS;
        }
        safeRoiInNativeCRS.setUserData(nativeCRS);
        this.nativeCRS=nativeCRS;
    }
  
    /**
     * Reproject the initial roi to the provided CRS which is supposely the target CRS
     * as per the request.
     * 
     * <p>
     * This method should be called once the native CRS has been set, that is the {@link #useNativeCRS(CoordinateReferenceSystem)}
     * has been called.
     * 
     * @param targetCRS a valid instance of {@link CoordinateReferenceSystem}
     * @throws IOException in case something bad happens.
     */    
    public void useTargetCRS(final CoordinateReferenceSystem targetCRS) throws IOException{
        if (targetCRS == null) {
            throw new IllegalArgumentException("The provided targetCRS is null");
        }
        if(roiInNativeCRS==null){
            throw new IllegalStateException("It looks like useNativeCRS has not been called yet");
        }
        this.targetCRS=targetCRS;
        if(isROIBBOX){
            // we need to use a larget bbox in native CRS
            roiInTargetCRS=DownloadUtilities.transformGeometry(safeRoiInNativeCRS, targetCRS);
            DownloadUtilities.checkPolygonROI(roiInTargetCRS);
            safeRoiInTargetCRS=roiInTargetCRS.getEnvelope();
            safeRoiInTargetCRS.setUserData(targetCRS);
            
            // touch safeRoiInNativeCRS
            safeRoiInNativeCRS=DownloadUtilities.transformGeometry(safeRoiInTargetCRS, nativeCRS);
            DownloadUtilities.checkPolygonROI(safeRoiInNativeCRS);
            safeRoiInNativeCRS=safeRoiInNativeCRS.getEnvelope();
            safeRoiInNativeCRS.setUserData(nativeCRS);
        }else{
            roiInTargetCRS=DownloadUtilities.transformGeometry(roiInNativeCRS, targetCRS);
            safeRoiInTargetCRS=roiInTargetCRS;
        }
        

    }
    /**
     * @return the isBBOX
     */
    public boolean isROIBBOX() {
        return isROIBBOX;
    }

    /**
     * @return the roi
     */
    public Geometry getRoi() {
        return roi;
    }

    /**
     * @return the roiInNativeCRS
     */
    public Geometry getRoiInNativeCRS() {
        return roiInNativeCRS;
    }

    /**
     * @return the safeRoiInNativeCRS
     */
    public Geometry getSafeRoiInNativeCRS() {
        return safeRoiInNativeCRS;
    }

    /**
     * @return the roiInTargetCRS
     */
    public Geometry getRoiInTargetCRS() {
        return roiInTargetCRS;
    }

    /**
     * @return the safeRoiInTargetCRS
     */
    public Geometry getSafeRoiInTargetCRS() {
        return safeRoiInTargetCRS;
    }

    /**
     * @return the roiCRS
     */
    public CoordinateReferenceSystem getRoiCRS() {
        return roiCRS;
    }

    /**
     * @return the targetCRS
     */
    public CoordinateReferenceSystem getTargetCRS() {
        return targetCRS;
    }
    
}
