/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LayerInfo.Type;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.factory.GeoTools;
import org.geotools.feature.SchemaException;
import org.geotools.resources.coverage.FeatureUtilities;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.operation.TransformException;

/**
 * Key/Value pair set parsed for a GetLegendGraphic request. When calling <code>getRequest</code>
 * produces a {@linkPlain org.vfny.geoserver.requests.wms.GetLegendGraphicRequest}
 * <p>
 * See {@linkplain org.org.geoserver.wms.GetLegendGraphicRequest} for a complete list of
 * expected request parameters.
 * </p>
 * 
 * @author Gabriel Roldan
 * @version $Id$
 * @see org.org.geoserver.wms.GetLegendGraphicRequest
 */
public class GetLegendGraphicKvpReader extends KvpRequestReader {

    private static final Logger LOGGER = Logging.getLogger(GetLegendGraphicKvpReader.class);

    /**
     * Factory to create styles from inline or remote SLD documents (aka, from SLD_BODY or SLD
     * parameters).
     */
    private static final StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(GeoTools
            .getDefaultHints());

    private WMS wms;

    /**
     * Creates a new GetLegendGraphicKvpReader object.
     * 
     * @param params
     *            map of key/value pairs with the parameters for a GetLegendGraphic request
     * @param wms
     *            WMS config object.
     */
    public GetLegendGraphicKvpReader(WMS wms) {
        super(GetLegendGraphicRequest.class);
        this.wms = wms;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public GetLegendGraphicRequest read(Object req, Map kvp, Map rawKvp) throws Exception {

        GetLegendGraphicRequest request = (GetLegendGraphicRequest) super.read(req, kvp, rawKvp);
        request.setRawKvp(rawKvp);

        if (request.getVersion() == null || request.getVersion().length() == 0) {
            String version = (String) rawKvp.get("WMTVER");
            if (version == null) {
                version = wms.getVersion();
            }
            request.setVersion(version);
        }

        final String language = (String) rawKvp.get("LANGUAGE");
        if(language != null) {
            request.setLocale(new Locale(language));
        }

        // Fix for http://jira.codehaus.org/browse/GEOS-710
        // Since at the moment none of the other request do check the version
        // numbers, we
        // disable this check for the moment, and wait for a proper fix once the
        // we support more than one version of WMS/WFS specs
        // if (!GetLegendGraphicRequest.SLD_VERSION.equals(version)) {
        // throw new WmsException("Invalid SLD version number \"" + version
        // + "\"");
        // }
        final String layer = (String) rawKvp.get("LAYER");
        final boolean strict = rawKvp.containsKey("STRICT") ? Boolean.valueOf((String) rawKvp
                .get("STRICT")) : request.isStrict();
        request.setStrict(strict);
        if (strict && layer == null) {
            throw new ServiceException("LAYER parameter not present for GetLegendGraphic",
                    "LayerNotDefined");
        }
        if (strict && request.getFormat() == null) {
            throw new ServiceException("Missing FORMAT parameter for GetLegendGraphic",
                    "MissingFormat");
        }

        // object representing the layer or layer group requested
        Object infoObject=null;
        
        // list of layers to render in the legend (we can have more
        // than one if a layergroup is requested)
        List<FeatureType> layers = new ArrayList<FeatureType>();
        if (layer != null) {
            try {
                LayerInfo layerInfo = wms.getLayerByName(layer);
                if (layerInfo != null) {
                    addLayer(layers,layerInfo,request);
                    infoObject=layerInfo;
                } else {
                    LayerGroupInfo layerGroupInfo = wms.getLayerGroupByName(layer);
                    if(layerGroupInfo != null) {
                        // add all single layers of the group
                        for(LayerInfo singleLayer : layerGroupInfo.getLayers()) {
                            addLayer(layers,singleLayer,request);
                        }
                        infoObject=layerGroupInfo;
                    } else {
                        throw new ServiceException(layer + " layer does not exist.");
                    }
                }
                request.setLayers(layers);
            } catch (IOException e) {
                throw new ServiceException(e);
            } catch (NoSuchElementException ne) {
                throw new ServiceException(new StringBuffer(layer)
                        .append(" layer does not exists.").toString(), ne);
            } catch (Exception te) {
                throw new ServiceException("Can't obtain the schema for the required layer.", te);
            }
        } else {
            layers.add(null);
            request.setLayers(layers);
        }

        if (request.getFormat() == null) {
            request.setFormat(GetLegendGraphicRequest.DEFAULT_FORMAT);
        }
        if (null == wms.getLegendGraphicOutputFormat(request.getFormat())) {
            throw new ServiceException(new StringBuffer("Invalid graphic format: ").append(
                    request.getFormat()).toString(), "InvalidFormat");
        }

        try {
            parseOptionalParameters(request, infoObject, rawKvp);
            
            if (layers.size() != request.getStyles().size()) {
                String msg = layers.size() + " layers requested, but found " + request.getStyles().size()
                        + " styles specified. ";
                throw new ServiceException(msg, getClass().getName());
            }
            
            if (request.getRules().size()>0 && layers.size() != request.getRules().size()) {
                String msg = layers.size() + " layers requested, but found " + request.getRules().size()
                        + " rules specified. ";
                throw new ServiceException(msg, getClass().getName());
            }
        } catch (IOException e) {
            throw new ServiceException(e);
        }
        
        return request;
    }

    /**
     * Adds a new layer to the current list of layers to be drawn
     * on the legend, and maps a title for it.
     * 
     * @param layers list of layers the current layer has to be added to
     * @param layerInfo layer description
     * @param req the GetLegendGrap
     * @throws FactoryRegistryException
     * @throws IOException
     * @throws TransformException
     * @throws SchemaException
     */
    private void addLayer(List<FeatureType> layers,
            LayerInfo layerInfo, GetLegendGraphicRequest req) throws FactoryRegistryException, IOException, TransformException, SchemaException {
        FeatureType featureType=getLayerFeatureType(layerInfo);
        if(featureType != null) {
            MapLayerInfo mli=new MapLayerInfo(layerInfo);
            // maps a title, if label is defined on layer
            if(mli.getLabel() != null) {
                req.setTitle(featureType.getName(),mli.getLabel());
            }
            layers.add(featureType);
        } else {
            throw new ServiceException("Cannot get FeatureType for Layer",
                    "MissingFeatureType");
        }
    }

    /**
     * Extracts a FeatureType for a given layer
     * 
     * @param layerInfo vector or raster layer
     * @return the FeatureType for the given layer
     * @throws IOException
     * @throws FactoryRegistryException
     * @throws TransformException
     * @throws SchemaException
     */
    private FeatureType getLayerFeatureType(LayerInfo layerInfo) throws IOException, FactoryRegistryException, TransformException, SchemaException {
        MapLayerInfo mli=new MapLayerInfo(layerInfo);
        if (layerInfo.getType() == Type.VECTOR) {
            FeatureType featureType = mli.getFeature().getFeatureType();
            return featureType;
        } else if (layerInfo.getType() == Type.RASTER) {
            CoverageInfo coverageInfo = mli.getCoverage();
            // it much safer to wrap a reader rather than a coverage in most cases, OOM can
            // occur otherwise
            final AbstractGridCoverage2DReader reader;
            reader = (AbstractGridCoverage2DReader) coverageInfo.getGridCoverageReader(
                    new NullProgressListener(), GeoTools.getDefaultHints());
            final SimpleFeatureCollection feature;
            feature = FeatureUtilities.wrapGridCoverageReader(reader, null);
            return feature.getSchema();
        }
        return null;
    }

    /**
     * Parses the GetLegendGraphic optional parameters.
     * <p>
     * The parameters parsed by this method are:
     * <ul>
     * <li>FEATURETYPE for the {@link GetLegendGraphicRequest#getFeatureType() featureType}
     * property.</li>
     * <li>SCALE for the {@link GetLegendGraphicRequest#getScale() scale} property.</li>
     * <li>WIDTH for the {@link GetLegendGraphicRequest#getWidth() width} property.</li>
     * <li>HEIGHT for the {@link GetLegendGraphicRequest#getHeight() height} property.</li>
     * <li>EXCEPTIONS for the {@link GetLegendGraphicRequest#getExceptions() exceptions} property.</li>
     * <li>TRANSPARENT for the {@link GetLegendGraphicRequest#isTransparent() transparent} property.
     * </li>
     * <li>LEGEND_OPTIONS for the {@link GetLegendGraphicRequest#getLegendOptions() legendOptions}
     * property.</li>
     * </ul>
     * </p>
     * 
     * @param req
     *            The request to set the properties to.
     * @param infoObj   a {@link LayerInfo layer} or a {@link LayerGroupInfo layerGroup}
     *                  for which the legend graphic is to be produced,
     *                  from where to extract the style information.
     * @throws IOException
     * 
     * @task TODO: validate EXCEPTIONS parameter
     */
    private void parseOptionalParameters(GetLegendGraphicRequest req, Object infoObj, Map rawKvp)
            throws IOException {
        parseStyleAndRule(req, infoObj, rawKvp);
    }

    /**
     * Parses the STYLE, SLD and SLD_BODY parameters, as well as RULE.
     * 
     * <p>
     * STYLE, SLD and SLD_BODY are mutually exclusive. STYLE refers to a named style known by the
     * server and applicable to the requested layer (i.e., it is exposed as one of the layer's
     * styles in the Capabilities document). SLD is a URL to an externally available SLD document,
     * and SLD_BODY is a string containing the SLD document itself.
     * </p>
     * 
     * <p>
     * As I don't completelly understand which takes priority over which from the spec, I assume the
     * precedence order as follow: SLD, SLD_BODY, STYLE, in decrecent order of precedence.
     * </p>
     * 
     * @param req
     * @param ftype
     * @throws IOException
     */
    private void parseStyleAndRule(GetLegendGraphicRequest req, Object infoObj, Map rawKvp)
            throws IOException {
        // gets the list of styles requested
        String listOfStyles = (String) rawKvp.get("STYLE");
        if(listOfStyles == null) {
            listOfStyles = "";
        }
        List<String> styleNames = KvpUtils.readFlat(listOfStyles);
        
        
        String sldUrl = (String) rawKvp.get("SLD");
        String sldBody = (String) rawKvp.get("SLD_BODY");

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(new StringBuffer("looking for styles ").append(listOfStyles).toString());
        }

        List<Style> sldStyles = new ArrayList<Style>();

        if (sldUrl != null) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("taking style from SLD parameter");
            }

            addStylesFrom(sldStyles,styleNames,loadRemoteStyle(sldUrl));
            
        } else if (sldBody != null) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("taking style from SLD_BODY parameter");
            }
            addStylesFrom(sldStyles,styleNames,parseSldBody(sldBody));
            
        } else if (styleNames.size() > 0) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("taking style from STYLE parameter");
            }
            int pos=0;
            for(String styleName : styleNames) {
                // if we have a layer group and no style is specified
                // use the default one for the layer in the current position
                if (styleName.equals("") && infoObj instanceof LayerGroupInfo) {
                    LayerGroupInfo layerGroupInfo = (LayerGroupInfo) infoObj;
                    if (pos < layerGroupInfo.getLayers().size()) {
                        sldStyles.add(getStyleFromLayer(layerGroupInfo.getLayers()
                                .get(pos)));
                    }
                } else {
                    sldStyles.add(wms.getStyleByName(styleName));
                }
                pos++;
            }
            
        } else {
            if(infoObj instanceof LayerInfo) {
                sldStyles.add(getStyleFromLayer((LayerInfo)infoObj));
            } else if(infoObj instanceof LayerGroupInfo) {
                LayerGroupInfo layerGroupInfo=(LayerGroupInfo)infoObj;
                for (int count = 0; count < layerGroupInfo.getLayers().size(); count++) {
                    if (count < layerGroupInfo.getStyles().size()
                            && layerGroupInfo.getStyles().get(count) != null) {
                        sldStyles.add(layerGroupInfo.getStyles().get(count)
                                .getStyle());
                    } else {
                        LayerInfo layerInfo = layerGroupInfo.getLayers().get(count);
                        sldStyles.add(getStyleFromLayer(layerInfo));
                    }
                }
            }
        }

        req.setStyles(sldStyles);

        String rule = (String) rawKvp.get("RULE");
        
        if (rule != null) {
            List<String> ruleNames = KvpUtils.readFlat(rule);
            req.setRules(ruleNames);
        }
    }

    /**
     * Gets the default style for the given layer
     * @param layerInfo layer requested
     * @return default style of the layer
     */
    private Style getStyleFromLayer(LayerInfo layerInfo) {
        MapLayerInfo mli=new MapLayerInfo(layerInfo);
        return mli.getDefaultStyle();
    }

    /**
     * Adds styles whose name matches names from a given source of styles.
     * 
     * @param sldStyles final styles container
     * @param styleNames names of styles to find in the given source
     * @param source list of styles from a given source
     */
    private void addStylesFrom(List<Style> sldStyles, List<String> styleNames, Style[] source) {
        if(styleNames.size() == 0) { 
            sldStyles.add(findStyle(null, source));
        } else {
            for(String styleName : styleNames) {
                sldStyles.add(findStyle(styleName, source));
            }
        }
        
    }

    /**
     * Finds the Style named <code>styleName</code> in <code>styles</code>.
     * 
     * @param styleName
     *            name of style to search for in the list of styles. If <code>null</code>, it is
     *            assumed the request is made in literal mode and the user has requested the first
     *            style.
     * @param styles
     *            non null, non empty, list of styles
     * @return
     * @throws NoSuchElementException
     *             if no style named <code>styleName</code> is found in <code>styles</code>
     */
    private Style findStyle(String styleName, Style[] styles) throws NoSuchElementException {
        if ((styles == null) || (styles.length == 0)) {
            throw new NoSuchElementException("No styles have been provided to search for "
                    + styleName);
        }

        if (styleName == null) {
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("styleName is null, request in literal mode, returning first style");
            }

            return styles[0];
        }

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer(new StringBuffer("request in library mode, looking for style ").append(
                    styleName).toString());
        }

        StringBuffer noMatchNames = new StringBuffer();

        for (int i = 0; i < styles.length; i++) {
            if ((styles[i] != null) && styleName.equals(styles[i].getName())) {
                return styles[i];
            }

            noMatchNames.append(styles[i].getName());

            if (i < styles.length) {
                noMatchNames.append(", ");
            }
        }

        throw new NoSuchElementException(styleName + " not found. Provided style names: "
                + noMatchNames);
    }

    /**
     * Loads a remote SLD document and parses it to a Style object
     * 
     * @param sldUrl
     *            an URL to a SLD document
     * 
     * @return the document parsed to a Style object
     * 
     * @throws WmsException
     *             if <code>sldUrl</code> is not a valid URL, a stream can't be opened or a parsing
     *             error occurs
     */
    private Style[] loadRemoteStyle(String sldUrl) throws ServiceException {
        InputStream in;

        try {
            URL url = new URL(sldUrl);
            in = url.openStream();
        } catch (MalformedURLException e) {
            throw new ServiceException(e, "Not a valid URL to an SLD document " + sldUrl,
                    "loadRemoteStyle");
        } catch (IOException e) {
            throw new ServiceException(e, "Can't open the SLD URL " + sldUrl, "loadRemoteStyle");
        }

        return parseSld(new InputStreamReader(in));
    }

    /**
     * Parses a SLD Style from a xml string
     * 
     * @param sldBody
     *            the string containing the SLD document
     * 
     * @return the SLD document string parsed to a Style object
     * 
     * @throws WmsException
     *             if a parsing error occurs.
     */
    private Style[] parseSldBody(String sldBody) throws ServiceException {
        // return parseSld(new StringBufferInputStream(sldBody));
        return parseSld(new StringReader(sldBody));
    }

    /**
     * Parses the content of the given input stream to an SLD Style, provided that a valid SLD
     * document can be read from <code>xmlIn</code>.
     * 
     * @param xmlIn
     *            where to read the SLD document from.
     * 
     * @return the parsed Style
     * 
     * @throws WmsException
     *             if a parsing error occurs
     */
    private Style[] parseSld(Reader xmlIn) throws ServiceException {
        SLDParser parser = new SLDParser(styleFactory, xmlIn);
        Style[] styles = null;

        try {
            styles = parser.readXML();
        } catch (RuntimeException e) {
            throw new ServiceException(e);
        }

        if ((styles == null) || (styles.length == 0)) {
            throw new ServiceException("Document contains no styles");
        }

        return styles;
    }

    
}
