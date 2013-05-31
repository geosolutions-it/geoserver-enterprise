package org.geoserver.web.admin;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.config.ContactInfo;

public class ContactPanel extends Panel {

    public ContactPanel(String id, final IModel<ContactInfo> model) {
        super(id, model);

        add(new TextField("contactPerson" ));
        add(new TextField("contactOrganization"));
        add(new TextField("contactPosition"));
        add(new TextField("addressType"));
        add(new TextField("address")); 
        add(new TextField("addressCity"));
        add(new TextField("addressState")); 
        add(new TextField("addressPostalCode"));
        add(new TextField("addressCountry"));
        add(new TextField("contactVoice"));
        add(new TextField("contactFacsimile"));
        add(new TextField("contactEmail"));
    }
}
