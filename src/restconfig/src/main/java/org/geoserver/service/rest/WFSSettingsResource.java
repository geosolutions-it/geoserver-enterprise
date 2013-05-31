/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.service.rest;

import java.util.Map;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.rest.CatalogFreemarkerHTMLFormat;
import org.geoserver.config.GeoServer;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.wfs.GMLInfoImpl;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSInfoImpl;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

import freemarker.template.Configuration;
import freemarker.template.SimpleHash;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class WFSSettingsResource extends ServiceSettingsResource {

    public WFSSettingsResource(Context context, Request request, Response response, Class clazz,
            GeoServer geoServer) {
        super(context, request, response, clazz, geoServer);
    }

    @Override
    protected DataFormat createHTMLFormat(Request request, Response response) {
        return new WFSSettingsHTMLFormat(request, response, this);
    }

    @Override
    protected void configurePersister(XStreamPersister persister, DataFormat format) {
        persister.setHideFeatureTypeAttributes();
        persister.getXStream().alias("wfs", WFSInfoImpl.class);
        persister.getXStream().alias("version", WFSInfo.Version.class);
        persister.getXStream().alias("gml", GMLInfoImpl.class);
    }

    static class WFSSettingsHTMLFormat extends CatalogFreemarkerHTMLFormat {

        public WFSSettingsHTMLFormat(Request request, Response response, Resource resource) {
            super(SettingsInfo.class, request, response, resource);
        }

        @Override
        protected String getTemplateName(Object data) {
            return "wfsSettings";
        }

        @Override
        protected Configuration createConfiguration(Object data, Class clazz) {
            Configuration cfg = super.createConfiguration(data, clazz);
            cfg.setClassForTemplateLoading(getClass(), "templates");
            cfg.setObjectWrapper(new ObjectToMapWrapper<WFSInfo>(WFSInfo.class) {
                @Override
                protected void wrapInternal(Map properties, SimpleHash model, WFSInfo wfsInfo) {
                    WorkspaceInfo workspaceInfo = wfsInfo.getWorkspace();
                    properties.put("workspaceName", workspaceInfo != null ? workspaceInfo.getName() : "NO_WORKSPACE");
                    properties.put("enabled", wfsInfo.isEnabled() ? "true" : "false");
                    properties.put("name", wfsInfo.getName());
                    properties.put("title", wfsInfo.getTitle());
                    properties.put("maintainer", wfsInfo.getMaintainer());
                    properties.put("abstract", wfsInfo.getAbstract());
                    properties.put("accessConstraints", wfsInfo.getAccessConstraints());
                    properties.put("fees", wfsInfo.getFees());
                    properties.put("versions", wfsInfo.getVersions());
                    properties.put("keywords", wfsInfo.getKeywords());
                    properties.put("metadataLink", wfsInfo.getMetadataLink());
                    properties.put("citeCompliant", wfsInfo.isCiteCompliant() ? "true" : "false");
                    properties.put("onlineResource", wfsInfo.getOnlineResource());
                    properties.put("schemaBaseURL", wfsInfo.getSchemaBaseURL());
                    properties.put("verbose", wfsInfo.isVerbose() ? "true" : "false");
                    properties.put("maxFeatures", String.valueOf(wfsInfo.getMaxFeatures()));
                    properties.put("isFeatureBounding", wfsInfo.isFeatureBounding() ? "true" : "false");
                    properties.put("serviceLevel", wfsInfo.getServiceLevel());
                    properties.put("isCanonicalSchemaLocation", wfsInfo.isCanonicalSchemaLocation() ? "true" : "false");
                    properties.put("encodeFeatureMember", wfsInfo.isEncodeFeatureMember() ? "true" : "false");
                }
            });
            return cfg;
        }
    }
}
