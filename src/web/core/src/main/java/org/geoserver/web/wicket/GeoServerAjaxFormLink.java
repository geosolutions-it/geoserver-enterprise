package org.geoserver.web.wicket;

import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.feedback.IFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.geoserver.web.GeoServerBasePage;

/**
 * A custom {@link AjaxSubmitLink} that:
 * <ul>
 *   <li>does not trigger form processing, thus is invoked even if the form is not validating</li>
 *   <li>automatically adds the {@link GeoServerBasePage} feedback panel in the ajax target</li>
 * </ul>
 * When using it remember that you have to invoke {@link FormComponent#processInput()} on each
 * component you need input from as the standard form processing has been skipped
 * @author Andrea Aime
 */
@SuppressWarnings("serial")
public abstract class GeoServerAjaxFormLink extends AjaxSubmitLink {

    public GeoServerAjaxFormLink(String id) {
        super(id);
    }

    public GeoServerAjaxFormLink(String id, Form form) {
        super(id, form);
    }
    
    @Override
    public boolean getDefaultFormProcessing() {
        return false;
    }
    
    @Override
    protected final void onSubmit(AjaxRequestTarget target, Form form) {
        onClick(target, form);
        if(getPage() instanceof GeoServerBasePage) {
            target.addComponent(((GeoServerBasePage) getPage()).getFeedbackPanel());
        }
    }

    protected abstract void onClick(AjaxRequestTarget target, Form form);

}
