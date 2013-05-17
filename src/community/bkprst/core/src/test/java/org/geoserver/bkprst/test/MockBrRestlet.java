/* 
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.bkprst.test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StringFormat;
import org.geotools.util.logging.Logging;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

public class MockBrRestlet extends AbstractResource {

    static Logger LOGGER = Logging.getLogger("org.geoserver.catalog.rest");

    private String path;

    private int progress;

    public MockBrRestlet(String path, int progress) {
        super();
        this.path = path;
        this.progress = progress;
    }

        public synchronized final String getPath() {
        return path;
    }

    public synchronized final void setPath(String path) {
        this.path = path;
    }

    public synchronized final int getProgress() {
        return progress;
    }

    public synchronized final void setProgress(int progress) {
        this.progress = progress;
    }

    @Override
    public void handleGet() {
       //get the appropriate format
       DataFormat format = getFormatGet();

       //transform the string "Hello World" to the appropriate response
       getResponse().setEntity(format.toRepresentation("Hello World"));
    }
    
    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {

        List<DataFormat> formats = new ArrayList();
        formats.add(new StringFormat(MediaType.TEXT_PLAIN));

        return formats;
    }

}
