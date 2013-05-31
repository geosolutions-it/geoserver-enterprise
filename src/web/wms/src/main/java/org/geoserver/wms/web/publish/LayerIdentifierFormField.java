package org.geoserver.wms.web.publish;

import java.util.List;

import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.impl.LayerIdentifier;

public class LayerIdentifierFormField extends FormComponentPanel<LayerIdentifierInfo> {

    private static final long serialVersionUID = 1L;

    private TextField<String> authority;

    private TextField<String> identifier;

    public LayerIdentifierFormField(final String id,
            final IModel<List<LayerIdentifierInfo>> identifierModel) {
        super(id);

        add((authority = new TextField<String>("authority", new PropertyModel<String>(
                identifierModel, "authority"))));
        add((identifier = new TextField<String>("identifier", new PropertyModel<String>(
                identifierModel, "identifier"))));

        add(new IValidator<LayerIdentifierInfo>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void validate(IValidatable<LayerIdentifierInfo> arg) {
                LayerIdentifierInfo value = arg.getValue();
                if (value == null) {
                    return;
                }
                if (value.getAuthority() == null || value.getIdentifier() == null) {
                    ValidationError error = new ValidationError();
                    error.setMessage(new ResourceModel("LayerIdentifierFormField.validationError")
                            .getObject());
                    arg.error(error);
                }
            }
        });
    }

    @Override
    protected void convertInput() {
        LayerIdentifierInfo info = getModelObject();
        String auth = authority.getConvertedInput();
        String id = identifier.getConvertedInput();
        if (id == null && auth == null) {
            setConvertedInput(null);
            return;
        }

        if (info == null) {
            info = new LayerIdentifier();
            setModelObject(info);
        }
        info.setAuthority(auth);
        info.setIdentifier(id);
        setConvertedInput(info);
    }

    /**
     * Pull out each field from the LayerIdentifierInfo if it exists and put the contents into the
     * fields.
     */
    @Override
    protected void onBeforeRender() {

        LayerIdentifierInfo info = getModelObject();

        if (info != null) {
            authority.setModelObject(info.getAuthority());
            identifier.setModelObject(info.getIdentifier());
        }

        super.onBeforeRender();
    }

}
