package org.geoserver.web.importer;

import java.util.Collections;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.XMLNameValidator;


/**
 *
 * @author Andrea Aime, GeoSolutions SAS
 *
 *
 */
@SuppressWarnings("serial")
class NewWorkspacePanel extends Panel
{


    String workspace;

    public NewWorkspacePanel(String id)
    {
        super(id);
        add(new FeedbackPanel("feedback").setOutputMarkupId(true));

        TextField wst = new TextField("workspace", new PropertyModel(this, "workspace"));
        wst.setRequired(true);

        wst.add(new AbstractValidator()
            {

                @Override
                protected void onValidate(IValidatable validatable)
                {
                    String value = (String) validatable.getValue();
                    if (GeoServerApplication.get().getCatalog().getWorkspaceByName(value) != null)
                    {
                        error(validatable, "NewWorkspacePanel.duplicateWorkspace", Collections.singletonMap("workspace", value));
                    }

                }
            });
        wst.add(new XMLNameValidator());

        this.add(wst);
    }

}
