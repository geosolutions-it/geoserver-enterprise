/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.SRSProvider.SRS;

/**
 * A panel which contains a list of all coordinate reference systems available to GeoServer.
 * <p>
 * Using this compontent in a page would look like:
 * 
 * <pre>
 * public class MyPage {
 * 
 *     public MyPage() {
 *     ...
 *     add( new SRSListPanel( &quot;srsList&quot; ) );
 *     ...
 *   }
 * }
 * </pre>
 * 
 * And the markup:
 * 
 * <pre>
 * ...
 *  &lt;body&gt;
 *    &lt;div wicket:id=&quot;srsList&gt;&lt;/div&gt;
 *  &lt;/body&gt;
 *  ...
 * </pre>
 * 
 * </p>
 * <p>
 * Client could should override the method {@link #createLinkForCode(String, IModel)} to provide
 * some action when the user clicks on a SRS code in the list.
 * </p>
 * 
 * @author Andrea Aime, OpenGeo
 * @author Justin Deoliveira, OpenGeo
 * @authos Gabriel Roldan, OpenGeo
 * 
 */
@SuppressWarnings("serial")
public abstract class SRSListPanel extends Panel {
    
    GeoServerTablePanel<SRS> table;

    /**
     * Creates the new SRS list panel.
     */
    public SRSListPanel(String id) {
        super(id);

        table = new GeoServerTablePanel<SRS>("table", new SRSProvider()) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel,
                    Property<SRS> property) {

                SRS srs = (SRS) itemModel.getObject();

                if (SRSProvider.CODE.equals(property)) {

                    Component linkForCode = createLinkForCode(id, itemModel);

                    return linkForCode;

                } else if (SRSProvider.DESCRIPTION.equals(property)) {
                    String description = srs.getDescription();
                    return new Label(id, description.trim());

                } else {
                    throw new IllegalArgumentException("Unknown property: " + property);
                }
            }

        };

        add(table);
    }
    
    /**
     * Hides the top pager so that the panel shows nicely in a small space (such as in a 
     * popup window)
     * @param compact
     */
    public void setCompactMode(boolean compact) {
        table.getTopPager().setVisible(!compact);
    }

    /**
     * Creates a link for an epsgCode.
     * <p>
     * Subclasses may override to perform an action when an epsg code has been selected. This
     * default implementation returns a link that does nothing.
     * </p>
     * 
     * @param linkId
     *            The id of the link component to be created.
     * @param itemModel
     *            The epsg code (integer).
     * 
     */
    protected Component createLinkForCode(String linkId, IModel itemModel) {
        return new SimpleAjaxLink(linkId, SRSProvider.CODE.getModel(itemModel)) {

            @Override
            protected void onClick(AjaxRequestTarget target) {
                onCodeClicked(target, getDefaultModelObjectAsString());
                
            }
        };
    }
    
    /**
     * Suclasses must override and perform whatever they see fit when a SRS code link is clicked 
     * @param target
     * @param modelObject
     */
    protected abstract void onCodeClicked(AjaxRequestTarget target, String epsgCode);
    
}