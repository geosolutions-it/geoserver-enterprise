/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.web.demo;

import java.awt.Rectangle;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A form component for a {@link GridEnvelope2D} object.
 * 
 * @author Justin Deoliveira, OpenGeo
 * @author Andrea Aime, OpenGeo
 */
public class GridPanel extends FormComponentPanel {

    Integer minX,minY,maxX,maxY;
    
    public GridPanel(String id ) {
        super(id);
        
        initComponents();
    }
    
    public GridPanel(String id, GridEnvelope2D e) {
        this(id, new Model(e));
    }
    
    public GridPanel(String id, IModel model) {
        super(id, model);
        
        initComponents();
    }
    
    void initComponents() {
        updateFields();
        
        add( new TextField( "minX", new PropertyModel(this, "minX")) );
        add( new TextField( "minY", new PropertyModel(this, "minY")) );
        add( new TextField( "maxX", new PropertyModel(this, "maxX") ));
        add( new TextField( "maxY", new PropertyModel(this, "maxY")) );
    }
    
    @Override
    protected void onBeforeRender() {
        updateFields();
        super.onBeforeRender();
    }
    
    private void updateFields() {
        GridEnvelope2D e = (GridEnvelope2D) getModelObject();
        if(e != null) {
            this.minX = (int) e.getMinX();
            this.minY = (int) e.getMinY();
            this.maxX = (int) e.getMaxX();
            this.maxY = (int) e.getMaxY();
        }
    }
   
    public GridPanel setReadOnly( final boolean readOnly ) {
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
        if(minX != null && maxX != null && minY != null && maxX != null) {
            final int width = maxX - minX;
            final int height = maxY - minY;
            setConvertedInput(new GridEnvelope2D(new Rectangle(minX, minY, width, height)));
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
    
}
