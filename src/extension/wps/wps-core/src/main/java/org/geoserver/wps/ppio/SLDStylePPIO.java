/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.ppio;

import java.io.InputStream;
import java.io.StringReader;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.sld.SLDConfiguration;
import org.geotools.sld.bindings.SLD;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xml.Configuration;
import org.geotools.xml.Encoder;
import org.geotools.xml.Parser;
import org.xml.sax.ContentHandler;

/**
 * Handles SLD 1.0 styles
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class SLDStylePPIO extends XMLPPIO {

    Configuration sldConfiguration;

    protected SLDStylePPIO() {
        super(Style.class, Style.class, "text/xml; subtype=sld/1.0.0", SLD.STYLEDLAYERDESCRIPTOR);
        sldConfiguration = new SLDConfiguration();
    }

    @Override
    public Object decode(InputStream input) throws Exception {
        Parser p = new Parser(sldConfiguration);

        // extract the first style in the first sld
        StyledLayerDescriptor sld = (StyledLayerDescriptor) p.parse(input);
        NamedLayer styledLayer = (NamedLayer) sld.getStyledLayers()[0];
        return styledLayer.getStyles()[0];
    }

    @Override
    public void encode(Object obj, ContentHandler handler) throws Exception {
        StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);
        StyledLayerDescriptor sld = sf.createStyledLayerDescriptor();
        NamedLayer nl = sf.createNamedLayer();
        nl.setName("");
        nl.styles().add((Style) obj);
        sld.setStyledLayers(new StyledLayer[] { nl });

        Encoder e = new Encoder(sldConfiguration);
        e.encode(sld, element, handler);
    }
    
    @Override
    public Object decode(Object input) throws Exception {
        Parser p = new Parser(sldConfiguration);

        // extract the first style in the first sld
        StyledLayerDescriptor sld = (StyledLayerDescriptor) p.parse(new StringReader((String) input));
        NamedLayer styledLayer = (NamedLayer) sld.getStyledLayers()[0];
        return styledLayer.getStyles()[0];
    }

}
