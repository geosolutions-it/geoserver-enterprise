/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs.kvp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.xml.XMLConstants;

import org.geoserver.ows.FlatKvpParser;
import org.geoserver.ows.KvpParser;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Parses a list of namespace declarations of the form {@code
 * <xmlns(foo=http://name.space1)[,xmlns(bar=http://name.space2)]+> } into a
 * {@link NamespaceSupport}.
 * 
 * @author groldan
 * 
 */
public class NamespaceKvpParser extends KvpParser {

    public NamespaceKvpParser(String key) {
        super(key, NamespaceSupport.class);
    }

    /**
     * @param value
     *            a list of namespace declarations of the form {@code
     *            <xmlns(foo=http://name.space1)[,xmlns(bar=http://name.space2)]+> }
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public NamespaceSupport parse(final String value) throws Exception {
        List<String> decls = (List<String>) new FlatKvpParser("", String.class).parse(value);
        NamespaceSupport ctx = new NamespaceSupport();

        String[] parts;
        String prefix;
        String uri;
        for (String decl : decls) {
            decl = decl.trim();
            if (!decl.startsWith("xmlns(") || !decl.endsWith(")")) {
                throw new IllegalArgumentException("Illegal namespace declaration, "
                        + "should be of the form xmlns(<prefix>=<ns uri>): " + decl);
            }
            decl = decl.substring("xmlns(".length());
            decl = decl.substring(0, decl.length() - 1);
            parts = decl.split("=");
            if (parts.length == 1) {
                prefix = XMLConstants.DEFAULT_NS_PREFIX;
                uri = parts[0];
            } else if (parts.length == 2) {
                prefix = parts[0];
                uri = parts[1];
            } else {
                throw new IllegalArgumentException("Illegal namespace declaration, "
                        + "should be of the form prefix=<namespace uri>: " + decl);
            }

            try {
                new URI(uri);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Illegal namespace declaration: " + decl);
            }
            ctx.declarePrefix(prefix, uri);
        }

        return ctx;
    }
}
