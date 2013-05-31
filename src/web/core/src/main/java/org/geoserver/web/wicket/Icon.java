/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * A simple {@link Image} in a panel. For when you need to add an icon in a repeater without
 * breaking yet another fragment.
 * 
 * @author Andrea Aime - OpenGeo
 * 
 */
@SuppressWarnings("serial")
public class Icon extends Panel {

    /**
     * Constructs an Icon from a resource reference.
     */
    public Icon(String id, ResourceReference resourceReference) {
        this(id, new Model(resourceReference));
    }

    /**
     * Constructs an icon from a resource reference for the image and resource model for the "title"
     * attribute to apply to the rendered "&lt;img>" tag.
     */
    public Icon(String id, ResourceReference resourceReference, IModel title) {
        this(id, new Model(resourceReference), title);
    }
    
    /**
     * Constructs an Icon from a model.
     */
    public Icon(String id, IModel model) {
        super(id);
        add(new Image("img", model));
    }

    /**
     * Constructs an Icon from a model for the resource reference and a resource model for the
     * "title" attribute to apply to the rendered "&lt;img>" tag. 
     */
    public Icon(String id, IModel model, IModel title) {
        super(id);
        add(new Image("img", model).add(new AttributeModifier("title", true, title)));
    }

}
