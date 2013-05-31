package org.geoserver.web.admin;

import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerHomePage;

public class ContactPage extends ServerAdminPage {
    public ContactPage(){
        final IModel geoServerModel = getGeoServerModel();
        final IModel contactModel = getContactInfoModel();

        Form form = new Form("form", new CompoundPropertyModel(contactModel));
        add(form);

        form.add(new ContactPanel("contact", contactModel));
        form.add(new Button("submit") {
            @Override
            public void onSubmit() {
                GeoServer gs = (GeoServer)geoServerModel.getObject();
                GeoServerInfo global = gs.getGlobal();
                global.setContact((ContactInfo)contactModel.getObject());
                gs.save(global);
                doReturn();
            }
        });
        form.add(new Button("cancel") {
            @Override
            public void onSubmit() {
                doReturn();
            }
        });
    }
}
