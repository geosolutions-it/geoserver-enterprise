package it.geosolutions.geoserver.jms.impl.web;

import it.geosolutions.geoserver.jms.configuration.JMSConfiguration;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.geoserver.web.wicket.GeoServerDialog;

public class NodePanel extends Panel {

    /** serialVersionUID */
    private static final long serialVersionUID = 8112885092637425915L;
    
    GeoServerDialog dialog;

    public NodePanel(String id, final JMSConfiguration configuration) {
        super(id);
        
        add(new Label("instance", (String) configuration.getConfiguration(JMSConfiguration.INSTANCE_NAME_KEY)));
        add(new Label("group",(String) configuration.getConfiguration(JMSConfiguration.GROUP_KEY)));
        
        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialHeight(255);
        dialog.setInitialWidth(300);
    }

}
