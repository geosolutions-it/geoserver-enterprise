/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.kml;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipFile;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.geoserver.data.test.MockData;
import org.geoserver.kml.KMZMapResponse.KMZMap;
import org.geoserver.ows.Response;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;

public class KMZMapProducerTest extends WMSTestSupport {
    KMZMapOutputFormat mapProducer;
    KMZMapResponse mapEncoder;
    KMZMap producedMap;

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new KMZMapProducerTest());
    }

    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        // create a map context
        WMSMapContent mapContent = new WMSMapContent();
        mapContent.addLayer(createMapLayer(MockData.BASIC_POLYGONS));
        mapContent.addLayer(createMapLayer(MockData.BUILDINGS));
        mapContent.setMapHeight(256);
        mapContent.setMapWidth(256);

        GetMapRequest getMapRequest = createGetMapRequest(new QName[] { MockData.BASIC_POLYGONS,
                MockData.BUILDINGS });
        mapContent.setRequest(getMapRequest);

        // create hte map producer
        mapProducer = new KMZMapOutputFormat(getWMS());
        mapEncoder = new KMZMapResponse(getWMS());
        producedMap = mapProducer.produceMap(mapContent);
    }

    public void test() throws Exception {
        // create the kmz
        File temp = File.createTempFile("test", "kmz");
        temp.delete();
        temp.mkdir();
        temp.deleteOnExit();

        File zip = new File(temp, "kmz.zip");
        zip.deleteOnExit();

        FileOutputStream output = new FileOutputStream(zip);
        mapEncoder.write(producedMap, output, null);

        output.flush();
        output.close();

        assertTrue(zip.exists());

        // unzip and test it
        ZipFile zipFile = new ZipFile(zip);

        assertNotNull(zipFile.getEntry("wms.kml"));
        assertNotNull(zipFile.getEntry("images/layer_0.png"));
        assertNotNull(zipFile.getEntry("images/layer_1.png"));

        zipFile.close();
    }

}
