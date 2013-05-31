/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web;

import static org.geoserver.gwc.web.GWCSettingsPage.checkbox;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.gwc.config.GWCConfig;

public class GWCServicesPanel extends Panel {

    private static final long serialVersionUID = 1L;

    public GWCServicesPanel(final String id, final IModel<GWCConfig> gwcConfigModel) {
        super(id, gwcConfigModel);

        final IModel<Boolean> wmsIntegrationEnabledModel = new PropertyModel<Boolean>(
                gwcConfigModel, "directWMSIntegrationEnabled");

        final IModel<Boolean> wmsCEnabledModel = new PropertyModel<Boolean>(gwcConfigModel,
                "WMSCEnabled");
        final IModel<Boolean> wmtsEnabledModel = new PropertyModel<Boolean>(gwcConfigModel,
                "WMTSEnabled");
        final IModel<Boolean> tmsEnabledModel = new PropertyModel<Boolean>(gwcConfigModel,
                "TMSEnabled");

        add(checkbox("enableWMSIntegration", wmsIntegrationEnabledModel,
                "GWCSettingsPage.enableWMSIntegration.title"));

        add(checkbox("enableWMSC", wmsCEnabledModel, "GWCSettingsPage.enableWMSC.title"));
        add(checkbox("enableWMTS", wmtsEnabledModel, "GWCSettingsPage.enableWMTS.title"));
        add(checkbox("enableTMS", tmsEnabledModel, "GWCSettingsPage.enableTMS.title"));

    }
}
