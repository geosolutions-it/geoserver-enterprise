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
package org.geoserver.bkprst.rest.test;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 * Utils for testing
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
final class Utils {

    static String parseTaskID(String outputStreamContent) throws JDOMException, IOException{
        final SAXBuilder builder = new SAXBuilder();
        Document document = (Document) builder.build(new StringReader(outputStreamContent));
        final String taskID = document.getRootElement().getTextTrim();
        return taskID;
    }

    static int getNumFiles(File dir) {
        return FileUtils.listFiles(dir, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).size();
    }
    
    static String prepareBackupDir(Object caller) throws Exception{
        final File dir=new File(new File(caller.getClass().getResource(".").toURI()),"backup"+System.nanoTime());
        Assert.assertTrue(dir.mkdir());
        return dir.getCanonicalPath();    
    }
}
