/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.data.layergroup.AbstractLayerGroupPage.LayerListPanel;
import org.geoserver.web.data.layergroup.AbstractLayerGroupPage.StyleListPanel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/**
 * Allows to edit the list of layers contained in a layer group
 */
@SuppressWarnings("serial")
public class LayerGroupEntryPanel extends Panel {

    ModalWindow popupWindow;
    LayerGroupEntryProvider entryProvider;
    GeoServerTablePanel<LayerGroupEntry> layerTable;
    List<LayerGroupEntry> items;
    
    public LayerGroupEntryPanel( String id, LayerGroupInfo layerGroup ) {
        super( id );
        
        items = new ArrayList();
        for ( int i = 0; i < layerGroup.getLayers().size(); i++ ) {
            LayerInfo layer = layerGroup.getLayers().get( i );
            StyleInfo style = layerGroup.getStyles().get( i );
            items.add( new LayerGroupEntry( layer, style ) );
        }
        
        add( popupWindow = new ModalWindow( "popup" ) );
        
        //layers
        entryProvider = new LayerGroupEntryProvider( items );
        add( layerTable = new GeoServerTablePanel<LayerGroupEntry>("layers",entryProvider) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<LayerGroupEntry> property) {
                if ( property == LayerGroupEntryProvider.LAYER ) {
                    return layerLink( id, itemModel );
                }
                if ( property == LayerGroupEntryProvider.DEFAULT_STYLE) {
                    return defaultStyleCheckbox( id, itemModel );
                }
                if ( property == LayerGroupEntryProvider.STYLE ) {
                    return styleLink( id, itemModel );
                }
                if ( property == LayerGroupEntryProvider.REMOVE ) {
                    return removeLink( id, itemModel );
                }
                if ( property == LayerGroupEntryProvider.POSITION ) {
                    return positionPanel( id, itemModel ); 
                }
                
                return null;
            }
        }.setFilterable( false ));
        layerTable.setOutputMarkupId( true );
        
        add( new AjaxLink( "add" ) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setInitialHeight( 375 );
                popupWindow.setInitialWidth( 525 );
                popupWindow.setTitle(new ParamResourceModel("chooseLayer", this));
                popupWindow.setContent( new LayerListPanel(popupWindow.getContentId()) {
                    @Override
                    protected void handleLayer(LayerInfo layer, AjaxRequestTarget target) {
                        popupWindow.close( target );
                        
                        entryProvider.getItems().add(
                            new LayerGroupEntry( layer, layer.getDefaultStyle() ) );
                        
                        //getCatalog().save( lg );
                        target.addComponent( layerTable );
                    }
                });
                
                popupWindow.show(target);
            }
        });
    }
    
    public List<LayerGroupEntry> getEntries() {
        return items;
    }
    
    Component layerLink(String id, IModel itemModel) {
        LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
        return new Label( id, entry.getLayer().getResource().getPrefixedName() );
    }
    
    Component defaultStyleCheckbox(String id, IModel itemModel) {
        final LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
        Fragment f = new Fragment(id, "defaultStyle", this);
        CheckBox ds = new CheckBox("checkbox", new Model(entry.isDefaultStyle()));
        ds.add(new OnChangeAjaxBehavior() {
            
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Boolean useDefault = (Boolean) getComponent().getDefaultModelObject();
                entry.setDefaultStyle(useDefault);
                target.addComponent(layerTable);
                
            }
        });
        f.add(ds);
        return f;
    }
    
    Component styleLink(String id, final IModel itemModel) {
        // decide if the style is the default and the current style name
        LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
        String styleName;
        boolean defaultStyle = true;
        if(entry.getStyle() != null) {
            styleName = entry.getStyle().getName();
            defaultStyle = false;
        } else if(entry.getLayer().getDefaultStyle() != null) {
            styleName = entry.getLayer().getDefaultStyle().getName();
        } else {
            styleName = null;
        }
            
        // build and returns the link, but disable it if the style is the default
        SimpleAjaxLink link = new SimpleAjaxLink( id, new Model(styleName)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                popupWindow.setInitialHeight( 375 );
                popupWindow.setInitialWidth( 525 );
                popupWindow.setTitle(new ParamResourceModel("chooseStyle", this));
                popupWindow.setContent( new StyleListPanel( popupWindow.getContentId() ) {
                    @Override
                    protected void handleStyle(StyleInfo style, AjaxRequestTarget target) {
                        popupWindow.close( target );
                        
                        LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
                        entry.setStyle( style );
                        
                        //redraw
                        target.addComponent( layerTable );
                    }
                });
                popupWindow.show(target);
            }

        };
        link.getLink().setEnabled(!defaultStyle);
        return link;
    }
    
    Component removeLink(String id, IModel itemModel) {
        final LayerGroupEntry entry = (LayerGroupEntry) itemModel.getObject();
        ImageAjaxLink link = new ImageAjaxLink( id, new ResourceReference( getClass(), "../../img/icons/silk/delete.png") ) {
            @Override
            protected void onClick(AjaxRequestTarget target) {
                
                items.remove( entry );
                target.addComponent( layerTable );
            }
        };
        link.getImage().add(new AttributeModifier("alt", true, new ParamResourceModel("AbstractLayerGroupPage.th.remove", link)));
        return link;
    }
    
    Component positionPanel(String id, IModel itemModel) {
        return new PositionPanel( id, (LayerGroupEntry) itemModel.getObject() );
    }
  
    static class LayerGroupEntryProvider extends GeoServerDataProvider<LayerGroupEntry> {

        public static Property<LayerGroupEntry> LAYER = 
            new PropertyPlaceholder<LayerGroupEntry>( "layer" );
        
        public static Property<LayerGroupEntry> DEFAULT_STYLE = 
            new PropertyPlaceholder<LayerGroupEntry>( "defaultStyle" );

        public static Property<LayerGroupEntry> STYLE = 
            new PropertyPlaceholder<LayerGroupEntry>( "style" );
        
        public static Property<LayerGroupEntry> REMOVE = 
            new PropertyPlaceholder<LayerGroupEntry>( "remove" );
        
        public static Property<LayerGroupEntry> POSITION = 
            new PropertyPlaceholder<LayerGroupEntry>( "position" );

        static List PROPERTIES = Arrays.asList( POSITION, LAYER, DEFAULT_STYLE, STYLE, REMOVE );
        
        List<LayerGroupEntry> items;
        
        public LayerGroupEntryProvider( List<LayerGroupEntry> items ) {
            this.items = items;
        }
        
        @Override
        protected List<LayerGroupEntry> getItems() {
            return items; 
        }

        @Override
        protected List<Property<LayerGroupEntry>> getProperties() {
            return PROPERTIES;
        }

    }

    class PositionPanel extends Panel {
        
        LayerGroupEntry entry;
        private ImageAjaxLink upLink;
        private ImageAjaxLink downLink;        
        
        public PositionPanel( String id, final LayerGroupEntry entry ) {
            super( id );
            this.entry = entry;
            this.setOutputMarkupId(true);
            
            upLink = new ImageAjaxLink( "up", new ResourceReference( getClass(), "../../img/icons/silk/arrow_up.png") ) {
                @Override
                protected void onClick(AjaxRequestTarget target) {
                    int index = items.indexOf( PositionPanel.this.entry );
                    items.remove( index );
                    items.add(Math.max(0, index - 1), PositionPanel.this.entry);
                    target.addComponent( layerTable );
                    target.addComponent(this);
                    target.addComponent(downLink);   
                    target.addComponent(upLink);                    
                }
                
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    if ( items.indexOf( entry ) == 0 ) {
                        tag.put("style", "visibility:hidden");
                    } else {
                        tag.put("style", "visibility:visible");
                    }
                }
            };
            upLink.getImage().add(new AttributeModifier("alt", true, new ParamResourceModel("up", upLink)));
            upLink.setOutputMarkupId(true);
            add( upLink);            

            downLink = new ImageAjaxLink( "down", new ResourceReference( getClass(), "../../img/icons/silk/arrow_down.png") ) {
                @Override
                protected void onClick(AjaxRequestTarget target) {
                    int index = items.indexOf( PositionPanel.this.entry );
                    items.remove( index );
                    items.add(Math.min(items.size(), index + 1), PositionPanel.this.entry);
                    target.addComponent( layerTable );
                    target.addComponent(this);                    
                    target.addComponent(downLink);   
                    target.addComponent(upLink);                    
                }
                
                @Override
                protected void onComponentTag(ComponentTag tag) {
                    if ( items.indexOf( entry ) == items.size() - 1) {
                        tag.put("style", "visibility:hidden");
                    } else {
                        tag.put("style", "visibility:visible");
                    }
                }
            };
            downLink.getImage().add(new AttributeModifier("alt", true, new ParamResourceModel("down", downLink)));
            downLink.setOutputMarkupId(true);
            add( downLink);
        }
    }
}
