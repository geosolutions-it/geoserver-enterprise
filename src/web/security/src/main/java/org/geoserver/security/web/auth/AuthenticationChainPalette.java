/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.extensions.markup.html.form.palette.Palette;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.geoserver.web.GeoServerApplication;

/**
 * Palette widget for the authentication chain, allowing for setting active providers and defining
 * chain order.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class AuthenticationChainPalette extends Palette<String> {

    public AuthenticationChainPalette(String id) {
        this(id, new AvailableAuthProviderNamesModel());
    }

    public AuthenticationChainPalette(String id, IModel<List<String>> model) {
        this(id, null, model);
    }
    
    public AuthenticationChainPalette(String id, IModel<List<String>> model, 
        IModel<List<String>> choicesModel) {
        super(id, model, choicesModel, new ChoiceRenderer() {
            @Override
            public String getIdValue(Object object, int index) {
                return (String) getDisplayValue(object);
            }
            @Override
                public Object getDisplayValue(Object object) {
                     return object.toString();
                }
        }, 10, true);
    }

    static class AvailableAuthProviderNamesModel implements IModel<List<String>> {

        @Override
        public List<String> getObject() {
            try {
                return new ArrayList<String>(
                    GeoServerApplication.get().getSecurityManager().listAuthenticationProviders());
            } catch (IOException e) {
                throw new WicketRuntimeException(e);
            }
        }

        @Override
        public void detach() {
            //do nothing
        }

        @Override
        public void setObject(List<String> object) {
            throw new UnsupportedOperationException();
        }
    }
}
