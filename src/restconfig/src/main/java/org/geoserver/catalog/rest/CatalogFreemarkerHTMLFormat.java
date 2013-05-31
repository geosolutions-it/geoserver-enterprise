/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.rest.format.ReflectiveHTMLFormat;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

import freemarker.template.Configuration;

public class CatalogFreemarkerHTMLFormat extends ReflectiveHTMLFormat {

    public CatalogFreemarkerHTMLFormat( Class clazz, Request request, Response response, Resource resource) {
        super( clazz, request, response, resource );
    }
    
    @Override
    protected Configuration createConfiguration(Object data, Class clazz) {
        
        Configuration cfg = super.createConfiguration(data, clazz);
        cfg.setClassForTemplateLoading( getClass(), "templates");
        return cfg;
    }
    
 
}
