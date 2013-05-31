/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.web;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.web.services.BaseServiceAdminPage;
import org.geoserver.web.util.MapModel;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.GMLInfo.SrsNameStyle;
import org.geoserver.wfs.response.ShapeZipOutputFormat;

@SuppressWarnings("serial")
public class WFSAdminPage extends BaseServiceAdminPage<WFSInfo> {

    
    public WFSAdminPage() {
        super();
    }

    public WFSAdminPage(PageParameters pageParams) {
        super(pageParams);
    }

    public WFSAdminPage(WFSInfo service) {
        super(service);
    }

    protected Class<WFSInfo> getServiceClass() {
        return WFSInfo.class;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void build(final IModel info, Form form) {
        form.add( new TextField<Integer>( "maxFeatures" ).add(new MinimumValidator<Integer>(0)) );
        form.add( new CheckBox("featureBounding") );
        
        //service level
        RadioGroup sl = new RadioGroup( "serviceLevel" );
        form.add( sl );
        sl.add( new Radio( "basic", new Model( WFSInfo.ServiceLevel.BASIC ) ) );
        sl.add( new Radio( "transactional", new Model( WFSInfo.ServiceLevel.TRANSACTIONAL  ) ) );
        sl.add( new Radio( "complete", new Model( WFSInfo.ServiceLevel.COMPLETE ) ) );
        
        IModel gml2Model = new LoadableDetachableModel(){
            public Object load(){
                return ((WFSInfo)info.getObject()).getGML().get(WFSInfo.Version.V_10);
            }
        };

        IModel gml3Model = new LoadableDetachableModel(){
            public Object load(){
                return ((WFSInfo)info.getObject()).getGML().get(WFSInfo.Version.V_11);
            }
        };

        IModel gml32Model = new LoadableDetachableModel() {
            @Override
            protected Object load() {
                return ((WFSInfo)info.getObject()).getGML().get(WFSInfo.Version.V_20);
            }
        };

        form.add(new GMLPanel("gml2", gml2Model));
        form.add(new GMLPanel("gml3", gml3Model));
        form.add(new GMLPanel("gml32", gml32Model));

        form.add( new CheckBox("canonicalSchemaLocation") );
        
        // Encode response with one featureMembers element or multiple featureMember elements
        RadioGroup eo = new RadioGroup("encodeFeatureMember");
        form.add(eo);
        eo.add(new Radio("featureMembers", new Model(Boolean.FALSE)));
        eo.add(new Radio("featureMember", new Model(Boolean.TRUE)));
        
        PropertyModel metadataModel = new PropertyModel(info, "metadata");
        IModel<Boolean> prjFormatModel = new MapModel(metadataModel,
                ShapeZipOutputFormat.SHAPE_ZIP_DEFAULT_PRJ_IS_ESRI);
        CheckBox defaultPrjFormat = new CheckBox("shapeZipPrjFormat", prjFormatModel);
        form.add(defaultPrjFormat);

        try {
            // This is a temporary meassure until we fully implement ESRI WKT support in GeoTools.
            // See discussion in GEOS-4503
            GeoServerResourceLoader resourceLoader = GeoServerExtensions
                    .bean(GeoServerResourceLoader.class);
            File esriProjs = resourceLoader.find("user_projections", "esri.properties");
            if (null == esriProjs) {
                defaultPrjFormat.setEnabled(false);
                defaultPrjFormat.getModel().setObject(Boolean.FALSE);
                defaultPrjFormat.add(new AttributeModifier("title", true, new Model(
                        "No esri.properties file "
                                + "found in the data directory's user_projections folder. "
                                + "This option is not available")));
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, e.getMessage(), e);
        }
        
    }
    
    static class GMLPanel extends Panel {

        public GMLPanel(String id, IModel gmlModel) { 
            super(id, new CompoundPropertyModel(gmlModel));
            
            //srsNameStyle
            List<GMLInfo.SrsNameStyle> choices = 
                Arrays.asList(SrsNameStyle.values());
            DropDownChoice srsNameStyle = new DropDownChoice("srsNameStyle", choices, new EnumChoiceRenderer());
            add(srsNameStyle);
            
            add(new CheckBox("overrideGMLAttributes"));
        }
        
    }

    protected String getServiceName(){
       return "WFS";
    }
}
