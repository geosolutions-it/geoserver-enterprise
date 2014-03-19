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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Collect temp files during processing.
 * 
 * @author Simone Giannecchini, GeoSolutions
 *
 */
class TempFilesCollector {
    
    private ConcurrentLinkedQueue<File> tempFiles= new ConcurrentLinkedQueue<File>();
    
    public void addFile(File file){
        tempFiles.add(file);
    }
    
    public void clear(){
        tempFiles.clear();
    }
    
    public List<File> getAll(){
        final List<File> returnValue= new ArrayList<File>();
        returnValue.addAll(tempFiles);
        return returnValue;
    }
    
    public boolean isEmpty(){
        return tempFiles.isEmpty();
    }
    
}
