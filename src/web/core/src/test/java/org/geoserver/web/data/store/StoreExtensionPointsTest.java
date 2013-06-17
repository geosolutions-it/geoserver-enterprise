package org.geoserver.web.data.store;

import java.io.IOException;
import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.file.File;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.TestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.raster.DirectoryRasterEditPanel;
import org.geoserver.web.data.store.shape.ShapefileStoreEditPanel;
import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;

public class StoreExtensionPointsTest extends GeoServerWicketTestSupport {

    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

    @Override
    protected void populateDataDirectory(MockData dataDirectory)
            throws Exception {
        super.populateDataDirectory(dataDirectory);
        
        dataDirectory.addCoverage(WATTEMP, TestData.class.getResource("watertemp.zip"),
                null, null);
    }

    public void testShapefile() throws IOException {
        DataStoreInfo si = getCatalog().getFactory().createDataStore();
        si.setName("testShape");
        URL url = DataUtilities.fileToURL(new File("./target/testShape.shp").getCanonicalFile());
        si.getConnectionParameters().put(ShapefileDataStoreFactory.URLP.key, url.toExternalForm());
        getCatalog().add(si);
        
        Form form = new Form("testForm");
        form.setModel(new Model(si));
        StoreEditPanel panel = StoreExtensionPoints.getStoreEditPanel("test", form, si, getGeoServerApplication());
        assertTrue(panel instanceof ShapefileStoreEditPanel);
    }
    
    public void testMosaic() throws IOException {
        CoverageStoreInfo si = getCatalog().getCoverageStoreByName(WATTEMP.getLocalPart()); 
        
        Form form = new Form("testForm");
        form.setModel(new Model(si));
        StoreEditPanel panel = StoreExtensionPoints.getStoreEditPanel("test", form, si, getGeoServerApplication());
        assertTrue(panel instanceof DirectoryRasterEditPanel);
    }

}
