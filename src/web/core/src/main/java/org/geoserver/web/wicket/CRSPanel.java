/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A form component for a {@link CoordinateReferenceSystem} object.
 * <p>
 * This panel provides the following functionality/information:
 * <ul>
 *   <li>The SRS (epsg code) of the CRS
 *   <li>View the full WKT of the CRS.
 *   <li>A mechanism to guess the SRS (epsg code) from the CRS
 *   <li>A lookup for browsing for a particular CRS 
 * </ul>
 * </p>
 * @author Justin Deoliveira, OpenGeo
 */
@SuppressWarnings("serial")
public class CRSPanel extends FormComponentPanel {
    private static Logger LOGGER = Logging.getLogger(CRSPanel.class);
    private static final long serialVersionUID = -6677103383336166008L;
    
    private static IBehavior READ_ONLY = new AttributeModifier("readonly", true, new Model("readonly"));

    /** pop-up window for WKT and SRS list */
    protected ModalWindow popupWindow;
    
    /** srs/epsg code text field */
    protected TextField srsTextField;
    
    /** find link */
    protected AjaxLink findLink;
    
    /** wkt label */
    protected Label wktLabel;

    /** the wkt link that contains the wkt label **/
    protected GeoServerAjaxFormLink wktLink;
    
    /**
     * Constructs the CRS panel.
     * <p>
     * This constructor should be used if the panel is to inherit from a parent model 
     * (ie a CompoundPropertyModel). If no such model is available the CRS will be left 
     * uninitialized. To avoid inheriting from a parent model the constructor {@link #CRSPanel(String, IModel)}
     * should be used, specifying explicitly an uninitialized model. 
     *</p>
     * @param id The component id.
     */
    public CRSPanel(String id) {
        super(id);
        initComponents();
    }

    /**
     * Constructs the CRS panel with an explicit model.
     *
     * @param id The component id.
     * @param model The model, usually a {@link PropertyModel}.
     */
    public CRSPanel(String id, IModel model) {
        super(id, model);
        initComponents();
    }
    
    /**
     * Constructs the CRS panel specifying the underlying CRS explicitly.
     * <p>
     * When this constructor is used the {@link #getCRS()} method should be used 
     * after the form is submitted to retrieve the final value of the CRS.
     * </p>
     * @param id The component id.
     * @param crs The underlying CRS object.
     */
    public CRSPanel(String id, CoordinateReferenceSystem crs ) {
        //JD: while the CoordinateReferenceSystem interface does not implement Serializable
        // all the CRS objects we use do, hence the cast
        super(id, new Model((Serializable) crs));
        initComponents();
        setConvertedInput(crs);
    }
    
    /*
     * helper for internally creating the panel. 
     */
    void initComponents() {
            
        popupWindow = new ModalWindow("popup");
        add( popupWindow );
        
        srsTextField = new TextField( "srs", new Model() );
        add( srsTextField );
        srsTextField.setOutputMarkupId( true );
        
        srsTextField.add(new AjaxFormComponentUpdatingBehavior("onblur") {
            
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                convertInput();
                
                CoordinateReferenceSystem crs = (CoordinateReferenceSystem) getConvertedInput();
                if(crs != null) {
                    setModelObject(crs);
                    wktLabel.setDefaultModelObject(crs.getName().toString());
                    wktLink.setEnabled(true);
                } else {
                    wktLabel.setDefaultModelObject(null);
                    wktLink.setEnabled(false);
                }
                target.addComponent(wktLink);
                
                onSRSUpdated(toSRS(crs), target);
            }
        });
        
        findLink = new AjaxLink( "find" ) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setContent(srsListPanel());
                popupWindow.setTitle(new ParamResourceModel("selectSRS", CRSPanel.this));
                popupWindow.show(target);
            }
        };
        add(findLink);
        
        wktLink = new GeoServerAjaxFormLink("wkt") {
            @Override
            public void onClick(AjaxRequestTarget target, Form form) {
                popupWindow.setInitialHeight( 375 );
                popupWindow.setInitialWidth( 525 );
                popupWindow.setContent(new WKTPanel( popupWindow.getContentId(), getCRS()));
                CoordinateReferenceSystem crs = (CoordinateReferenceSystem) CRSPanel.this.getModelObject();
                if(crs != null)
                    popupWindow.setTitle(crs.getName().toString());
                popupWindow.show(target);
            }
        };
        wktLink.setEnabled(getModelObject() != null);
        add(wktLink);
        
        wktLabel = new Label( "wktLabel", new Model());
        wktLink.add( wktLabel );
        wktLabel.setOutputMarkupId( true );
    }
    
    @Override
    protected void onBeforeRender() {
        CoordinateReferenceSystem crs = (CoordinateReferenceSystem) getModelObject();
        if ( crs != null ) {
            srsTextField.setModelObject( toSRS(crs) );
            wktLabel.setDefaultModelObject( crs.getName().toString() );    
        } else {
            wktLabel.setDefaultModelObject(null);
            wktLink.setEnabled(false);
        }
        
        super.onBeforeRender();
    }
    
    @Override
    protected void convertInput() {
        String srs = srsTextField.getInput();
        CoordinateReferenceSystem crs = null;
        if ( srs != null && !"".equals(srs)) {
            if ( "UNKNOWN".equals( srs ) ) {
                //leave underlying crs unchanged
                if ( getModelObject() instanceof CoordinateReferenceSystem ) {
                    setConvertedInput(getModelObject());
                }
                return;
            }
            crs = fromSRS( srs );
        }
        setConvertedInput( crs );
    }
    
    
    /**
     * Subclasses can override to perform custom behaviors when the SRS is updated, which happens
     * either when the text field is left or when the find dialog returns
     * @param target 
     */
    protected void onSRSUpdated(String srs, AjaxRequestTarget target) {
        // do nothing by default
    }
    
    /**
     * Sets the panel to be read only.
     */
    public CRSPanel setReadOnly( boolean readOnly ) {
        if(readOnly)
            srsTextField.add(READ_ONLY);
        else
            srsTextField.remove(READ_ONLY);
        findLink.setVisible( !readOnly );
        return this;
    }
    
    /**
     * Returns the underlying CRS for the panel.
     * <p>
     * This method is convenience for:
     * <pre>
     * (CoordinateReferenceSystem) this.getModelObject();
     * </pre>
     * </p>
     */
    public CoordinateReferenceSystem getCRS() {
        convertInput();
        return (CoordinateReferenceSystem) getConvertedInput();
    }
    
    /*
     * Goes from CRS to SRS. 
     */
    String toSRS( CoordinateReferenceSystem crs ) {
        try {
            if(crs != null) {
                Integer epsgCode = CRS.lookupEpsgCode(crs, false);
                return epsgCode != null ? "EPSG:" + epsgCode : "UNKNOWN";
            } else {
                return "UNKNOWN";
            }
        } 
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not succesffully lookup an EPSG code", e);
            return null;
        }
    }
    
    /*
     * Goes from SRS to CRS.
     */
    protected CoordinateReferenceSystem fromSRS( String srs ) {
        try {
            return CRS.decode( srs );
        } 
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "Unknown EPSG code " + srs, e);
            return null;
        }
    }
    
    /*
     * Builds the srs list panel component.
     */
    @SuppressWarnings("serial")
    protected SRSListPanel srsListPanel() {
        SRSListPanel srsList = new SRSListPanel(popupWindow.getContentId()) {
            
            @Override
            protected void onCodeClicked(AjaxRequestTarget target, String epsgCode) {
                popupWindow.close(target);
                
                String srs =  "EPSG:" + epsgCode ;
                srsTextField.setModelObject( srs );
                target.addComponent( srsTextField );
                
                CoordinateReferenceSystem crs = fromSRS( srs );
                wktLabel.setDefaultModelObject( crs.getName().toString() );
                wktLink.setEnabled(true);
                target.addComponent( wktLink );
                
                onSRSUpdated(srs, target);
            }
        };
        srsList.setCompactMode(true);
        return srsList;
    }
    
    /*
     * Panel for displaying the well known text for a CRS.
     */
    public static class WKTPanel extends Panel {

        public WKTPanel(String id, CoordinateReferenceSystem crs) {
            super(id);
            
            MultiLineLabel wktLabel = new MultiLineLabel("wkt");
            
            add( wktLabel );
            
            if ( crs != null ) {
                wktLabel.setDefaultModel( new Model( crs.toString() ) );
            }
        }
    }
}
