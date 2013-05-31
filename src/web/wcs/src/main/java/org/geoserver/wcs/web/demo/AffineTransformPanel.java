/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web.demo;

import java.awt.geom.AffineTransform;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * A form component for a {@link AffineTransform} object.
 * 
 * @author Justin Deoliveira, OpenGeo
 * @author Andrea Aime, GeoSolutions
 */
public class AffineTransformPanel extends FormComponentPanel<AffineTransform> {

    Double scaleX,shearX,originX,scaleY,shearY,originY;
    private WebMarkupContainer originXContainer;
    private WebMarkupContainer shearXContainer;
    private WebMarkupContainer originYContainer;
    private WebMarkupContainer shearYContainer;
    private WebMarkupContainer newline;
    
    public AffineTransformPanel(String id ) {
        super(id);
        
        initComponents();
    }
    
    public AffineTransformPanel(String id, AffineTransform e) {
        this(id, new Model(e));
    }
    
    public AffineTransformPanel(String id, IModel model) {
        super(id, model);
        
        initComponents();
    }
    
    void initComponents() {
        updateFields();
        
        originXContainer = new WebMarkupContainer("originXContainer");
        add(originXContainer);
        newline = new WebMarkupContainer("newline");
        add(newline);
        shearXContainer = new WebMarkupContainer("shearXContainer");
        add(shearXContainer);
        originYContainer = new WebMarkupContainer("originYContainer");
        add(originYContainer);
        shearYContainer = new WebMarkupContainer("shearYContainer");
        add(shearYContainer);
        
        add( new TextField( "scaleX", new PropertyModel(this, "scaleX")) );
        shearXContainer.add( new TextField( "shearX", new PropertyModel(this, "shearX")) );
        originXContainer.add( new TextField( "originX", new PropertyModel(this, "originX")) );
        add( new TextField( "scaleY", new PropertyModel(this, "scaleY")) );
        shearYContainer.add( new TextField( "shearY", new PropertyModel(this, "shearY")) );
        originYContainer.add( new TextField( "originY", new PropertyModel(this, "originY")) );
    }
    
    @Override
    protected void onBeforeRender() {
        updateFields();
        super.onBeforeRender();
    }
    
    private void updateFields() {
        AffineTransform at = getModelObject();
        if(at != null) {
            this.scaleX = at.getScaleX();
            this.shearX = at.getShearX();
            this.originX = at.getTranslateX();
            this.scaleY = at.getScaleY();
            this.shearY = at.getShearY();
            this.originY = at.getTranslateY();
        }
    }
   
    public AffineTransformPanel setReadOnly( final boolean readOnly ) {
        visitChildren( TextField.class, new org.apache.wicket.Component.IVisitor() {
            public Object component(Component component) {
                component.setEnabled( !readOnly );
                return null;
            }
        });

        return this;
    }
    
    @Override
    protected void convertInput() {
        visitChildren( TextField.class, new org.apache.wicket.Component.IVisitor() {

            public Object component(Component component) {
                ((TextField) component).processInput();
                return null;
            }
        });
        
        // update the grid envelope
        if(isResolutionModeEnabled() && scaleX != null && scaleY != null) {
            setConvertedInput(AffineTransform.getScaleInstance(scaleX, scaleY));
        } else if(scaleX != null && shearX != null && originX != null && 
           scaleY != null && shearY != null && originY != null) {
            setConvertedInput(new AffineTransform(scaleX, shearX, shearY, scaleY, originX, originY));
        } else {
            setConvertedInput(null);
        }
    }
    
    @Override
    protected void onModelChanged() {
        // when the client programmatically changed the model, update the fields
        // so that the textfields will change too
        updateFields();
        visitChildren(TextField.class, new Component.IVisitor() {
            
            public Object component(Component component) {
                ((TextField) component).clearInput();
                return CONTINUE_TRAVERSAL;
            }
        });
    }
    
    /**
     * Turns the editor in a pure resolution editor 
     * @param enabled
     */
    public void setResolutionModeEnabled(boolean enabled) {
        shearXContainer.setVisible(!enabled);
        shearYContainer.setVisible(!enabled);
        originXContainer.setVisible(!enabled);
        originYContainer.setVisible(!enabled);
        newline.setVisible(!enabled);
    }
    
    public boolean isResolutionModeEnabled() {
        return !shearXContainer.isVisible();
    }
}
