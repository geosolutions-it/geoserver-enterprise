/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.map;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.EnumerationUtils;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.ows.HttpServletRequestAware;
import org.geoserver.ows.KvpRequestReader;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSErrorCode;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.crs.ForceCoordinateSystemFeatureReader;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureTypes;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.NamedStyle;
import org.geotools.styling.RemoteOWS;
import org.geotools.styling.Style;
import org.geotools.styling.StyleAttributeExtractor;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.util.Version;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.util.Requests;
import org.vfny.geoserver.util.SLDValidator;

public class GetMapKvpRequestReader extends KvpRequestReader implements HttpServletRequestAware {

    /**
     * current request
     */
    private HttpServletRequest httpRequest;

    /**
     * style factory
     */
    private StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);

    /**
     * filter factory
     */
    private FilterFactory filterFactory = CommonFactoryFinder.getFilterFactory(null);

    /**
     * Flag to control wether styles shall be parsed.
     */
    private boolean parseStyles = true;

    /**
     * The WMS configuration facade, that we use to pick up base layer definitions
     */
    private WMS wms;

    /**
     * This flags allows the kvp reader to go beyond the SLD library mode specification and match
     * the first style that can be applied to a given layer. This is for backwards compatibility
     */
    private boolean laxStyleMatchAllowed = true;

    public GetMapKvpRequestReader(WMS wms) {
        super(GetMapRequest.class);
        this.wms = wms;
    }

    /**
     * Implements {@link HttpServletRequestAware#setHttpRequest(HttpServletRequest)} to gather
     * request information for some properties like {@link GetMapRequest#isGet()} and
     * {@link GetMapRequest#getRequestCharset()}.
     * 
     * @see org.geoserver.ows.HttpServletRequestAware#setHttpRequest(javax.servlet.http.HttpServletRequest)
     */
    public void setHttpRequest(HttpServletRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public void setStyleFactory(StyleFactory styleFactory) {
        this.styleFactory = styleFactory;
    }

    public void setFilterFactory(FilterFactory filterFactory) {
        this.filterFactory = filterFactory;
    }

    public boolean isParseStyle() {
        return parseStyles;
    }

    public void setParseStyle(boolean styleRequired) {
        this.parseStyles = styleRequired;
    }

    @SuppressWarnings("unchecked")
    @Override
    public GetMapRequest createRequest() throws Exception {
        GetMapRequest request = new GetMapRequest();
        if (httpRequest != null) {
            request.setRequestCharset(httpRequest.getCharacterEncoding());
            request.setGet("GET".equalsIgnoreCase(httpRequest.getMethod()));
            List<String> headerNames = (List<String>) EnumerationUtils.toList(httpRequest
                    .getHeaderNames());
            for (String headerName : headerNames) {
                request.putHttpRequestHeader(headerName, httpRequest.getHeader(headerName));
            }
        }
        return request;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public GetMapRequest read(Object request, Map kvp, Map rawKvp) throws Exception {
        GetMapRequest getMap = (GetMapRequest) super.read(request, kvp, rawKvp);
        // set the raw params used to create the request
        getMap.setRawKvp(rawKvp);

        // wms 1.3, srs changed to crs
        if (kvp.containsKey("crs")) {
            getMap.setSRS((String)kvp.get("crs"));
        }
        // do some additional checks

        // srs
        String epsgCode = getMap.getSRS();
        epsgCode = WMS.toInternalSRS(epsgCode, WMS.version(getMap.getVersion()));
        getMap.setSRS(epsgCode);
        
        if (epsgCode != null) {
            try {
                // set the crs as well
                CoordinateReferenceSystem mapcrs = CRS.decode(epsgCode);
                getMap.setCrs(mapcrs);
            } catch (Exception e) {
                // couldnt make it - we send off a service exception with the
                // correct info
                throw new ServiceException("Error occurred decoding the espg code " + epsgCode, e,
                    WMSErrorCode.INVALID_CRS.get(getMap.getVersion()));
            }
        }

        // remote OWS
        String remoteOwsType = getMap.getRemoteOwsType();
        remoteOwsType = remoteOwsType != null ? remoteOwsType.toUpperCase() : null;
        if (remoteOwsType != null && !"WFS".equals(remoteOwsType)) {
            throw new ServiceException("Unsupported remote OWS type '" + remoteOwsType + "'");
        }

        // remote OWS url
        URL remoteOwsUrl = getMap.getRemoteOwsURL();
        if (remoteOwsUrl != null && remoteOwsType == null) {
            throw new ServiceException("REMOTE_OWS_URL specified, but REMOTE_OWS_TYPE is missing");
        }

        final List<Object> requestedLayerInfos = new ArrayList<Object>();
        // layers
        String layerParam = (String) rawKvp.get("LAYERS");
        if (layerParam != null) {
            List<String> layerNames = KvpUtils.readFlat(layerParam);
            requestedLayerInfos.addAll(parseLayers(layerNames, remoteOwsUrl, remoteOwsType));

            List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();
            for (Object o : requestedLayerInfos) {
                if (o instanceof LayerInfo) {
                    layers.add(new MapLayerInfo((LayerInfo) o));
                } else if (o instanceof LayerGroupInfo) {
                    for (LayerInfo l : ((LayerGroupInfo) o).getLayers()) {
                        layers.add(new MapLayerInfo(l));
                    }
                } else if (o instanceof MapLayerInfo) {
                    // it was a remote OWS layer, add it directly
                    layers.add((MapLayerInfo) o);
                }
            }
            getMap.setLayers(layers);
        }

        // raw styles parameter
        String stylesParam = (String) kvp.get("STYLES");
        List<String> styleNameList = new ArrayList<String>();
        if (stylesParam != null) {
            styleNameList.addAll(KvpUtils.readFlat(stylesParam));
        }

        // pre parse filters
        List<Filter> filters = parseFilters(getMap);

        // styles
        // process SLD_BODY, SLD, then STYLES parameter
        if (getMap.getSldBody() != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Getting layers and styles from SLD_BODY");
            }

            if (getMap.getValidateSchema().booleanValue()) {
                ByteArrayInputStream stream = new ByteArrayInputStream(getMap.getSldBody()
                        .getBytes());
                List errors = validateSld(stream, getMap);

                if (errors.size() != 0) {
                    throw new ServiceException(SLDValidator.getErrorMessage(
                            new ByteArrayInputStream(getMap.getSldBody().getBytes()), errors));
                }
            }

            InputStream input = new ByteArrayInputStream(getMap.getSldBody().getBytes());
            StyledLayerDescriptor sld = parseSld(getMap, input);
            processSld(getMap, requestedLayerInfos, sld, styleNameList);

            // set filter in, we'll check consistency later
            getMap.setFilter(filters);
        } else if (getMap.getSld() != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Getting layers and styles from reomte SLD");
            }

            URL sldUrl = getMap.getSld();

            if (getMap.getValidateSchema().booleanValue()) {
                InputStream input = Requests.getInputStream(sldUrl);
                List errors = null;

                try {
                    errors = validateSld(input, getMap);
                } finally {
                    input.close();
                }

                if ((errors != null) && (errors.size() != 0)) {
                    input = Requests.getInputStream(sldUrl);

                    try {
                        throw new ServiceException(SLDValidator.getErrorMessage(input, errors));
                    } finally {
                        input.close();
                    }
                }
            }

            // JD: GEOS-420, Wrap the sldUrl in getINputStream method in order
            // to do compression
            InputStream input = Requests.getInputStream(sldUrl);

            try {
                StyledLayerDescriptor sld = parseSld(getMap, input);
                processSld(getMap, requestedLayerInfos, sld, styleNameList);
            } finally {
                input.close();
            }

            // set filter in, we'll check consistency later
            getMap.setFilter(filters);
        } else {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Getting layers and styles from LAYERS and STYLES");
            }

            // ok, parse the styles parameter in isolation
            if (styleNameList.size() > 0) {
                List<Style> parseStyles = parseStyles(styleNameList);
                getMap.setStyles(parseStyles);
            }

            // first, expand base layers and default styles
            if (isParseStyle() && requestedLayerInfos.size() > 0) {
                List<Style> oldStyles = getMap.getStyles() != null ? new ArrayList(
                        getMap.getStyles()) : new ArrayList();
                List<Style> newStyles = new ArrayList<Style>();
                List<Filter> newFilters = filters == null ? null : new ArrayList<Filter>();

                for (int i = 0; i < requestedLayerInfos.size(); i++) {
                    Object o = requestedLayerInfos.get(i);
                    Style style = oldStyles.isEmpty() ? null : (Style) oldStyles.get(i);

                    if (o instanceof LayerGroupInfo) {
                        LayerGroupInfo groupInfo = (LayerGroupInfo) o;
                        for (int j = 0; j < groupInfo.getStyles().size(); j++) {
                            StyleInfo si = groupInfo.getStyles().get(j);
                            if (si != null){
                                newStyles.add(si.getStyle());
                            } else {
                                LayerInfo layer = groupInfo.getLayers().get(j);
                                newStyles.add(getDefaultStyle(layer));
                            }
                        }
                        // expand the filter on the layer group to all its sublayers
                        if (filters != null) {
                            for (int j = 0; j < groupInfo.getLayers().size(); j++) {
                                newFilters.add(getFilter(filters, i));
                            }
                        }
                    } else if (o instanceof LayerInfo) {
                        style = oldStyles.size() > 0 ? oldStyles.get(i) : null;
                        if (style != null) {
                            newStyles.add(style);
                        } else {
                            LayerInfo layer = (LayerInfo) o;
                            newStyles.add(getDefaultStyle(layer));
                        }
                        // add filter if needed
                        if (filters != null)
                            newFilters.add(getFilter(filters, i));
                    } else if (o instanceof MapLayerInfo) {
                        style = oldStyles.size() > 0 ? oldStyles.get(i) : null;
                        if (style != null) {
                            newStyles.add(style);
                        } else {
                            throw new ServiceException("no style requested for layer "
                                    + ((MapLayerInfo) o).getName(), "NoDefaultStyle");
                        }
                        // add filter if needed
                        if (filters != null)
                            newFilters.add(getFilter(filters, i));
                    }
                }
                getMap.setStyles(newStyles);
                getMap.setFilter(newFilters);
            }

            // then proceed with standard processing
            List<MapLayerInfo> layers = getMap.getLayers();
            if (isParseStyle() && (layers != null) && (layers.size() > 0)) {
                final List styles = getMap.getStyles();

                if (layers.size() != styles.size()) {
                    String msg = layers.size() + " layers requested, but found " + styles.size()
                            + " styles specified. ";
                    throw new ServiceException(msg, getClass().getName());
                }

                for (int i = 0; i < styles.size(); i++) {
                    Style currStyle = (Style) getMap.getStyles().get(i);
                    if (currStyle == null)
                        throw new ServiceException(
                                "Could not find a style for layer "
                                        + getMap.getLayers().get(i).getName()
                                        + ", either none was specified or no default style is available for it",
                                "NoDefaultStyle");
                    checkStyle(currStyle, layers.get(i));
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine(new StringBuffer("establishing ").append(currStyle.getName())
                                .append(" style for ").append(layers.get(i).getName()).toString());
                    }
                }
            }

            // check filter size matches with the layer list size
            List mapFilters = getMap.getFilter();
            List<MapLayerInfo> mapLayers = getMap.getLayers();
            if (mapFilters != null && mapFilters.size() != mapLayers.size()) {
                String msg = mapLayers.size() + " layers requested, but found " + mapFilters.size()
                        + " filters specified. ";
                throw new ServiceException(msg, getClass().getName());
            }
        }
        
        // check the view params
        List<Map<String, String>> viewParams = getMap.getViewParams();
        if(viewParams != null && viewParams.size() > 0) {
            int layerCount = getMap.getLayers().size();
            
            // if we have just one replicate over all layers
            if(viewParams.size() == 1 && layerCount > 1) {
                List<Map<String, String>> replacement = new ArrayList<Map<String,String>>();
                for (int i = 0; i < layerCount; i++) {
                    replacement.add(viewParams.get(0));
                }
                getMap.setViewParams(replacement);
            } else if(viewParams.size() != layerCount) {
                String msg = layerCount + " layers requested, but found " + viewParams.size()
                + " view params specified. ";
                throw new ServiceException(msg, getClass().getName());
            }
        }
        
        // check if layers have time/elevation support
        boolean hasTime = false;
        boolean hasElevation = false;
        for (MapLayerInfo layer : getMap.getLayers()) {
            if (layer.getType() == MapLayerInfo.TYPE_VECTOR) {
                MetadataMap metadata = layer.getResource().getMetadata();
		DimensionInfo elevationInfo = metadata.get(ResourceInfo.ELEVATION, DimensionInfo.class);
                hasElevation |= elevationInfo != null && elevationInfo.isEnabled();
                DimensionInfo timeInfo = metadata.get(ResourceInfo.TIME, DimensionInfo.class);
                hasTime |= timeInfo != null && timeInfo.isEnabled();
            } else if (layer.getType() == MapLayerInfo.TYPE_RASTER) {
                MetadataMap metadata = layer.getResource().getMetadata();
                //
                // Adding a coverage layer
                //
                AbstractGridCoverage2DReader reader;
                try {
                    reader = (AbstractGridCoverage2DReader) layer.getCoverageReader();
                } catch (IOException e) {
                    throw new ServiceException(e);
                }
                if (reader != null) {
                    ReaderDimensionsAccessor dimensions = new ReaderDimensionsAccessor(reader);
                    DimensionInfo elevationInfo = metadata.get(ResourceInfo.ELEVATION, DimensionInfo.class);
                    hasElevation |= elevationInfo != null && elevationInfo.isEnabled() && dimensions.hasElevation();
                    DimensionInfo timeInfo = metadata.get(ResourceInfo.TIME, DimensionInfo.class);
                    hasTime |= timeInfo != null && timeInfo.isEnabled() && dimensions.hasTime();
                }
            }
        }
        
        // force in the default if nothing was requested
        if(hasTime && (getMap.getTime() == null || getMap.getTime().isEmpty())) {
            // ask for "CURRENT"
            getMap.setTime(Arrays.asList((Object) null));
        }
        if(hasElevation && (getMap.getElevation() == null || getMap.getElevation().isEmpty())) {
            // ask for "DEFAULT"
            getMap.setElevation(Arrays.asList((Object) null));
        }
        
        // check that we don't have double dimensions listing
        if((getMap.getElevation() != null && getMap.getElevation().size() > 1) &&
           (getMap.getTime() != null && getMap.getTime().size() > 1)) {
            throw new ServiceException("TIME and ELEVATION values cannot be both multivalued");
        }

        return getMap;
    }

    private Style getDefaultStyle (LayerInfo layer) throws IOException{
        if (layer.getResource() instanceof WMSLayerInfo) {
            // NamedStyle is a subclass of Style -> we use it as a way to convey
            // cascaded WMS layer styles
            NamedStyle namedStyle = CommonFactoryFinder.getStyleFactory(null)
                    .createNamedStyle();
            namedStyle.setName(null);
            return namedStyle;
        } else {
            StyleInfo defaultStyle = layer.getDefaultStyle();
            return defaultStyle.getStyle();
        }
    }

    Filter getFilter(List<Filter> filters, int index) {
        if (filters.size() == 1 && filters.get(0) instanceof FeatureId) {
            // feature id filters must be expanded to all layers
            return filters.get(0);
        } else if (index < filters.size()) {
            return filters.get(index);
        } else {
            throw new ServiceException("Layers and filters are mismatched, "
                    + "you need to provide one filter for each layer");
        }
    }

    /**
     * Checks the various options, OGC filter, fid filter, CQL filter, and returns a list of parsed
     * filters
     * 
     * @param getMap
     * @return the list of parsed filters, or null if none was found
     */
    private List<Filter> parseFilters(GetMapRequest getMap) {
        List<Filter> filters = (getMap.getFilter() != null) ? getMap.getFilter() : Collections
                .emptyList();
        List cqlFilters = (getMap.getCQLFilter() != null) ? getMap.getCQLFilter()
                : Collections.EMPTY_LIST;
        List featureId = (getMap.getFeatureId() != null) ? getMap.getFeatureId()
                : Collections.EMPTY_LIST;

        if (!featureId.isEmpty()) {
            if (!filters.isEmpty()) {
                throw new ServiceException("GetMap KVP request contained "
                        + "conflicting filters.  Filter: " + filters + ", fid: " + featureId);
            }

            Set ids = new HashSet();
            for (Iterator i = featureId.iterator(); i.hasNext();) {
                ids.add(filterFactory.featureId((String) i.next()));
            }
            filters = Collections.singletonList((Filter) filterFactory.id(ids));
        }

        if (!cqlFilters.isEmpty()) {
            if (!filters.isEmpty()) {
                throw new ServiceException("GetMap KVP request contained "
                        + "conflicting filters.  Filter: " + filters + ", fid: " + featureId
                        + ", cql: " + cqlFilters);
            }

            filters = cqlFilters;
        }

        // return null in case we found no filters
        if (filters.size() == 0) {
            filters = null;
        }
        return filters;
    }

    /**
     * validates an sld document.
     * 
     */
    private List validateSld(InputStream stream, GetMapRequest getMap) {
        try {
            if (getMap.getSldVersion() != null) {
                return Styles.validate(stream, new Version(getMap.getSldVersion()));
            }
            else {
                return Styles.validate(stream);
            }
        } 
        catch (IOException e) {
            throw new ServiceException("Error validating style", e);
        }
    }

    /**
     * Parses an sld document.
     */
    private StyledLayerDescriptor parseSld(GetMapRequest getMap, InputStream stream) {
       
        StyledLayerDescriptor sld;
        try {
            if (getMap.getSldVersion() != null) {
                sld = Styles.parse(stream, new Version(getMap.getSldVersion()));
            }
            else {
                sld = Styles.parse(stream);
            }
        }
        catch(IOException e) {
            throw new ServiceException("Error parsing style", e);
        }
       
        return sld;
    }

    private void processSld(final GetMapRequest request, final List<?> requestedLayers,
            final StyledLayerDescriptor sld, final List styleNames) throws ServiceException,
            IOException {
        if (requestedLayers.size() == 0) {
            processStandaloneSld(wms, request, sld);
        } else {
            processLibrarySld(request, sld, requestedLayers, styleNames);
        }
    }

    /**
     * Looks in <code>sld</code> for the layers and styles to use in the map composition and sets
     * them to the <code>request</code>
     * 
     * <p>
     * This method processes SLD in library mode Library mode engages when "SLD" or "SLD_BODY" are
     * used in conjuction with LAYERS and STYLES. From the spec: <br>
     * <cite> When an SLD is used as a style library, the STYLES CGI parameter is interpreted in the
     * usual way in the GetMap request, except that the handling of the style names is organized so
     * that the styles defined in the SLD take precedence over the named styles stored within the
     * map server. The user-defined SLD styles can be given names and they can be marked as being
     * the default style for a layer. To be more specific, if a style named �CenterLine� is
     * referenced for a layer and a style with that name is defined for the corresponding layer in
     * the SLD, then the SLD style definition is used. Otherwise, the standard named-style mechanism
     * built into the map server is used. If the use of a default style is specified and a style is
     * marked as being the default for the corresponding layer in the SLD, then the default style
     * from the SLD is used; otherwise, the standard default style in the map server is used.
     * </cite>
     * 
     * @param request
     *            the GetMap request to which to set the layers and styles
     * @param sld
     *            a SLD document to take layers and styles from, following the "literal" or
     *            "library" rule.
     * @param requestedLayers
     *            the list of {@link LayerInfo} and {@link LayerGroupInfo} as requested by the
     *            LAYERS param
     * @param styleNames
     *            the list of requested style names
     */
    private void processLibrarySld(final GetMapRequest request, final StyledLayerDescriptor sld,
            final List<?> requestedLayers, List<String> styleNames) throws ServiceException,
            IOException {
        final StyledLayer[] styledLayers = sld.getStyledLayers();
        final int slCount = styledLayers.length;

        if (slCount == 0) {
            throw new ServiceException("SLD document contains no layers");
        }

        final List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();
        final List<Style> styles = new ArrayList<Style>();

        MapLayerInfo currLayer = null;
        String styleName = null;

        for (int i = 0; i < requestedLayers.size(); i++) {
            if (styleNames != null && styleNames.size() > 0) {
                styleName = styleNames.get(i);
            }
            Object o = requestedLayers.get(i);
            if (o instanceof LayerInfo) {
                currLayer = new MapLayerInfo((LayerInfo) o);

                if (styledLayers[i] instanceof NamedLayer) {
                    NamedLayer namedLayer = ((NamedLayer) styledLayers[i]);
                    currLayer.setLayerFeatureConstraints(namedLayer.getLayerFeatureConstraints());
                }

                layers.add(currLayer);
                Style style = findStyleOf(request, currLayer, styleName, styledLayers);
                styles.add(style);
            } else if (o instanceof LayerGroupInfo) {
                List<LayerInfo> subLayers = ((LayerGroupInfo) o).getLayers();
                for (LayerInfo layer : subLayers) {
                    currLayer = new MapLayerInfo(layer);
                    layers.add(currLayer);
                    Style style = findStyleOf(request, currLayer, styleName, styledLayers);
                    styles.add(style);
                }
            } else {
                throw new IllegalArgumentException("Unknown layer info type: " + o);
            }
        }

        request.setLayers(layers);
        request.setStyles(styles);
    }

    /**
     * This one processes an SLD in non library mode, that is, it assumes it's the definition of the
     * map
     * 
     * @param request
     * @param sld
     * @throws IOException
     */
    public static void processStandaloneSld(final WMS wms, final GetMapRequest request,
            final StyledLayerDescriptor sld) throws IOException {
        final StyledLayer[] styledLayers = sld.getStyledLayers();
        final int slCount = styledLayers.length;

        if (slCount == 0) {
            throw new ServiceException("SLD document contains no layers");
        }

        final List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();
        final List<Style> styles = new ArrayList<Style>();
        MapLayerInfo currLayer = null;
        Style currStyle = null;

        String layerName;
        UserLayer ul;
        for (StyledLayer sl : styledLayers) {
            layerName = sl.getName();

            if (null == layerName) {
                throw new ServiceException("A UserLayer without layer name was passed");
            }

            if (sl instanceof UserLayer && ((((UserLayer) sl)).getRemoteOWS() != null)) {
                // this beast can define multiple feature sources and multiple styles, we'll
                // have to mix and match them (ugh)
                ul = ((UserLayer) sl);
                try {
                    addRemoteLayersFromUserLayer(request, ul, layers, styles);
                } catch (IOException e) {
                    throw new ServiceException("Error accessing remote layers", e,
                            "RemoteAccessFailed");
                }
            } else {
                // simpler case, one layer, eventually multiple styles
                currLayer = null;
                // handle the InLineFeature stuff
                if ((sl instanceof UserLayer)
                        && ((((UserLayer) sl)).getInlineFeatureDatastore() != null)) {
                    // SPECIAL CASE - we make the temporary version
                    ul = ((UserLayer) sl);

                    try {
                        currLayer = initializeInlineFeatureLayer(request, ul);
                    } catch (Exception e) {
                        throw new ServiceException(e);
                    }
                } else {
                    if (wms.getLayerGroupByName(layerName) != null) {
                        LayerGroupInfo group = wms.getLayerGroupByName(layerName);
                        for (int i = 0; i < group.getLayers().size(); i++) {
                            LayerInfo layer = group.getLayers().get(i);
                            layers.add(new MapLayerInfo(layer));
                            StyleInfo style = group.getStyles().get(i);
                            if (style != null) {
                                styles.add(style.getStyle());
                            } else {
                                styles.add(layer.getDefaultStyle().getStyle());
                            }
                        }
                        // move to the next named layer
                        continue;
                    } else {
                        LayerInfo layerInfo = wms.getLayerByName(layerName);

                        if (layerInfo == null)
                            throw new ServiceException("Unknown layer: " + layerName);

                        currLayer = new MapLayerInfo(layerInfo);
                        if (sl instanceof NamedLayer) {
                            NamedLayer namedLayer = ((NamedLayer) sl);
                            currLayer.setLayerFeatureConstraints(namedLayer
                                    .getLayerFeatureConstraints());
                        }
                    }
                }

                if (currLayer.getType() == MapLayerInfo.TYPE_RASTER) {
                    try {
                        addStyles(wms, request, currLayer, sl, layers, styles);
                    } catch (ServiceException wm) {
                        // hmm, well, the style they specified in the wms
                        // request
                        // wasn't found. Let's try the default raster style
                        // named 'raster'
                        currStyle = findStyle(wms, request, "raster");
                        if (currStyle == null) {
                            // nope, no default raster style either. Give up.
                            throw new ServiceException(wm.getMessage() + "  Also tried to use "
                                    + "the generic raster style 'raster', but it wasn't available.");
                        }
                        layers.add(currLayer);
                        styles.add(currStyle);
                    }
                } else {
                    addStyles(wms, request, currLayer, sl, layers, styles);
                }
            }
        }

        request.setLayers(layers);
        request.setStyles(styles);
    }

    private static void addRemoteLayersFromUserLayer(GetMapRequest request, UserLayer ul,
            List layers, List styles) throws ServiceException, IOException {
        RemoteOWS service = ul.getRemoteOWS();
        if (!service.getService().equalsIgnoreCase("WFS"))
            throw new ServiceException("GeoServer only supports WFS as remoteOWS service");
        if (service.getOnlineResource() == null)
            throw new ServiceException("OnlineResource for remote WFS not specified in SLD");
        final FeatureTypeConstraint[] featureConstraints = ul.getLayerFeatureConstraints();
        if (featureConstraints == null || featureConstraints.length == 0)
            throw new ServiceException(
                    "No FeatureTypeConstraint specified, no layer can be loaded for this UserStyle");

        DataStore remoteWFS = null;
        List remoteTypeNames = null;
        try {
            URL url = new URL(service.getOnlineResource());
            remoteWFS = connectRemoteWFS(url);
            remoteTypeNames = new ArrayList(Arrays.asList(remoteWFS.getTypeNames()));
            Collections.sort(remoteTypeNames);
        } catch (MalformedURLException e) {
            throw new ServiceException("Invalid online resource url: '"
                    + service.getOnlineResource() + "'");
        }

        Style[] layerStyles = ul.getUserStyles();
        if (request.getFilter() == null)
            request.setFilter(new ArrayList());
        for (int i = 0; i < featureConstraints.length; i++) {
            // make sure the layer is there
            String name = featureConstraints[i].getFeatureTypeName();
            if (Collections.binarySearch(remoteTypeNames, name) < 0) {
                throw new ServiceException("Could not find layer feature type '" + name
                        + "' on remote WFS '" + service.getOnlineResource());
            }

            // grab the filter
            Filter filter = featureConstraints[i].getFilter();
            if (filter == null)
                filter = Filter.INCLUDE;

            // connect the layer
            SimpleFeatureSource fs = remoteWFS.getFeatureSource(name);

            // this is messy, why the spec allows for multiple constraints and multiple
            // styles is beyond me... we'll style each remote layer with all possible
            // styles, feauture type style matching will do the rest during rendering
            for (int j = 0; j < layerStyles.length; j++) {
                Style style = layerStyles[i];
                MapLayerInfo info = new MapLayerInfo(fs);
                layers.add(info);
                styles.add(style);
                // treat it like an externally provided filter... ugly I know, but
                // the sane thing (adding a filter as a MapLayerInfo field) would
                // break havoc in GetFeatureInfo
                request.getFilter().add(filter);
            }
        }
    }

    /**
     * the correct thing to do its grab the style from styledLayers[i] inside the styledLayers[i]
     * will either be : a) nothing - in which case grab the layer's default style b) a set of: i)
     * NameStyle -- grab it from the pre-loaded styles ii)UserStyle -- grab it from the sld the user
     * uploaded
     * 
     * NOTE: we're going to get a set of layer->style pairs for (b). these are added to
     * layers,styles
     * 
     * NOTE: we also handle some featuretypeconstraints
     * 
     * @param request
     * @param currLayer
     * @param layer
     * @param layers
     * @param styles
     * @throws IOException
     */
    public static void addStyles(WMS wms, GetMapRequest request, MapLayerInfo currLayer,
            StyledLayer layer, List layers, List styles) throws ServiceException, IOException {
        if (currLayer == null) {
            return; // protection
        }

        Style[] layerStyles = null;
        FeatureTypeConstraint[] ftcs = null;

        if (layer instanceof NamedLayer) {
            ftcs = ((NamedLayer) layer).getLayerFeatureConstraints();
            layerStyles = ((NamedLayer) layer).getStyles();
        } else if (layer instanceof UserLayer) {
            ftcs = ((UserLayer) layer).getLayerFeatureConstraints();
            layerStyles = ((UserLayer) layer).getUserStyles();
        }

        // DJB: TODO: this needs to do the whole thing, not just names
        if (ftcs != null) {
            FeatureTypeConstraint ftc;
            final int length = ftcs.length;

            for (int t = 0; t < length; t++) {
                ftc = ftcs[t];

                if (ftc.getFeatureTypeName() != null) {
                    String ftc_name = ftc.getFeatureTypeName();

                    // taken from lite renderer
                    boolean matches;

                    try {
                        final FeatureType featureType = currLayer.getFeature().getFeatureType();
                        matches = FeatureTypes.isDecendedFrom(featureType, null, ftc_name)
                                || featureType.getName().getLocalPart().equalsIgnoreCase(ftc_name);
                    } catch (Exception e) {
                        matches = false; // bad news
                    }

                    if (!matches) {
                        continue; // this layer is fitered out
                    }
                }
            }
        }

        // handle no styles -- use default
        if ((layerStyles == null) || (layerStyles.length == 0)) {
            layers.add(currLayer);
            styles.add(currLayer.getDefaultStyle());

            return;
        }

        final int length = layerStyles.length;
        Style s;

        for (int t = 0; t < length; t++) {
            if (layerStyles[t] instanceof NamedStyle) {
                layers.add(currLayer);
                s = findStyle(wms, request, ((NamedStyle) layerStyles[t]).getName());

                if (s == null) {
                    throw new ServiceException("couldnt find style named '"
                            + ((NamedStyle) layerStyles[t]).getName() + "'");
                }

                styles.add(s);
            } else {
                layers.add(currLayer);
                styles.add(layerStyles[t]);
            }
        }
    }

    /**
     * @param request
     * @param currStyleName
     * 
     * @return the configured style named <code>currStyleName</code> or <code>null</code> if such a
     *         style does not exist on this server.
     * @throws IOException
     */
    private static Style findStyle(final WMS wms, GetMapRequest request, String currStyleName)
            throws IOException {
        // Style currStyle;
        // Map configuredStyles = request.getWMS().getData().getStyles();
        //
        // currStyle = (Style) configuredStyles.get(currStyleName);
        //
        // return currStyle;
        return wms.getStyleByName(currStyleName);
    }

    /**
     * Finds the style for <code>layer</code> in <code>styledLayers</code> or the layer's default
     * style if <code>styledLayers</code> has no a UserLayer or a NamedLayer with the same name than
     * <code>layer</code>
     * <p>
     * This method is used to parse the style of a layer for SLD and SLD_BODY parameters, both in
     * library and literal mode. Thus, once the declared style for the given layer is found, it is
     * checked for validity of appliance for that layer (i.e., whether the featuretype contains the
     * attributes needed for executing the style filters).
     * </p>
     * 
     * @param request
     *            used to find out an internally configured style when referenced by name by a
     *            NamedLayer
     * 
     * @param layer
     *            one of the layers that was requested through the LAYERS parameter or through and
     *            SLD document when the request is in literal mode.
     * @param styledLayers
     *            a set of StyledLayers from where to find the SLD layer with the same name as
     *            <code>layer</code> and extract the style to apply.
     * 
     * @return the Style applicable to <code>layer</code> extracted from <code>styledLayers</code>.
     * 
     * @throws RuntimeException
     *             if one of the StyledLayers is neither a UserLayer nor a NamedLayer. This
     *             shuoldn't happen, since the only allowed subinterfaces of StyledLayer are
     *             NamedLayer and UserLayer.
     * @throws ServiceException
     * @throws IOException
     */
    private Style findStyleOf(GetMapRequest request, MapLayerInfo layer, String styleName,
            StyledLayer[] styledLayers) throws ServiceException, IOException {
        Style style = null;
        String layerName = layer.getName();
        StyledLayer sl;

        for (int i = 0; i < styledLayers.length; i++) {
            sl = styledLayers[i];

            if (layerName.equals(sl.getName())) {
                if (sl instanceof UserLayer) {
                    Style[] styles = ((UserLayer) sl).getUserStyles();

                    // if the style name has not been specified, look it up
                    // the default style, otherwise lookup the one requested
                    for (int j = 0; style == null && styles != null && j < styles.length; j++) {
                        if (styleName == null || styleName.equals("") && styles[j].isDefault())
                            style = styles[j];
                        else if (styleName != null && styleName.equals(styles[j].getName()))
                            style = styles[j];
                    }
                } else if (sl instanceof NamedLayer) {
                    Style[] styles = ((NamedLayer) sl).getStyles();

                    // if the style name has not been specified, look it up
                    // the default style, otherwise lookup the one requested
                    for (int j = 0; style == null && styles != null && j < styles.length; j++) {
                        if ((styleName == null || styleName.equals("")) && styles[j].isDefault())
                            style = styles[j];
                        else if (styleName != null && styleName.equals(styles[j].getName()))
                            style = styles[j];
                    }

                    if (style instanceof NamedStyle) {
                        style = findStyle(wms, request, style.getName());
                    }
                } else {
                    throw new RuntimeException("Unknown layer type: " + sl);
                }

                break;
            }
        }

        // fallback on the old GeoServer behaviour, if the style is not found find
        // the first style that matches the type name
        // TODO: would be nice to have a switch to turn this off since it's out of the spec
        if (style == null && laxStyleMatchAllowed) {
            for (int i = 0; i < styledLayers.length; i++) {
                sl = styledLayers[i];

                if (layerName.equals(sl.getName())) {
                    if (sl instanceof UserLayer) {
                        Style[] styles = ((UserLayer) sl).getUserStyles();

                        if ((null != styles) && (0 < styles.length)) {
                            style = styles[0];
                        }
                    } else if (sl instanceof NamedLayer) {
                        Style[] styles = ((NamedLayer) sl).getStyles();

                        if ((null != styles) && (0 < styles.length)) {
                            style = styles[0];
                        }

                        if (style instanceof NamedStyle) {
                            style = findStyle(wms, request, style.getName());
                        }
                    } else {
                        throw new RuntimeException("Unknown layer type: " + sl);
                    }

                    break;
                }
            }
        }

        // still not found? Fall back on the server default ones
        if (style == null) {
            if (styleName == null || "".equals(styleName)) {
                style = layer.getDefaultStyle();
                if (style == null)
                    throw new ServiceException("Could not find a default style for "
                            + layer.getName());
            } else {
                style = wms.getStyleByName(styleName);
                if (style == null) {
                    String msg = "No such style: " + styleName;
                    throw new ServiceException(msg, "StyleNotDefined");
                }
            }
        }

        checkStyle(style, layer);

        return style;
    }

    /**
     * Checks to make sure that the style passed in can process the FeatureType.
     * 
     * @param style
     *            The style to check
     * @param mapLayerInfo
     *            The source requested.
     * 
     * @throws ServiceException
     */
    private static void checkStyle(Style style, MapLayerInfo mapLayerInfo) throws ServiceException {
        if (mapLayerInfo.getType() == mapLayerInfo.TYPE_RASTER) {
            // REVISIT: hey, don't we have to check it for rasters now that we support raster
            // symbolizer?
            return;
        }
        // if a rendering transform is present don't check the attributes, since they may be changed
        if (hasTransformation(style)) 
            return;  

        // extract attributes used in the style
        StyleAttributeExtractor sae = new StyleAttributeExtractor();
        sae.visit(style);
        Set<PropertyName> styleAttributes = sae.getAttributes();

        // see if we can collect any attribute out of the provided layer
       // Set attributes = new HashSet();
        FeatureType type = null;
        if (mapLayerInfo.getType() == MapLayerInfo.TYPE_VECTOR
                || mapLayerInfo.getType() == MapLayerInfo.TYPE_REMOTE_VECTOR) {
            try {                
                if (mapLayerInfo.getType() == MapLayerInfo.TYPE_VECTOR)
                    type = mapLayerInfo.getFeature().getFeatureType();
                else
                    type = mapLayerInfo.getRemoteFeatureSource().getSchema();
            } catch (IOException ioe) {
                throw new RuntimeException("Error getting FeatureType, this should never happen!",
                        ioe);
            }
        }

        // check all attributes required by the style are available
        for (PropertyName attName : styleAttributes) {
            if ( attName.evaluate(type) == null ) {
                throw new ServiceException(
                        "The requested Style can not be used with this layer.  The style specifies "
                                + "an attribute of " + attName + " and the layer is: "
                                + mapLayerInfo.getName());
            }
        }
    }

    /**
     * Tests whether a style contains a Rendering Transformation.
     * 
     * @param style the style to check
     * @return true if the style contains a rendering transformation
     */
    private static boolean hasTransformation(Style style)
    {
        for (FeatureTypeStyle fs : style.featureTypeStyles()) {
            if (fs.getTransformation() != null) 
                return true;
        }
        return false;
    }
    /**
     * Method to initialize a user layer which contains inline features.
     * 
     * @param httpRequest
     *            The request
     * @param mapLayer
     *            The map layer.
     * 
     * @throws Exception
     */

    // JD: the reason this method is static is to share logic among the xml
    // and kvp reader, ugh...
    private static MapLayerInfo initializeInlineFeatureLayer(GetMapRequest getMapRequest,
            UserLayer ul) throws Exception {

        SimpleFeatureSource featureSource;

        // what if they didn't put an "srsName" on their geometry in their
        // inlinefeature?
        // I guess we should assume they mean their geometry to exist in the
        // output SRS of the
        // request they're making.
        if (ul.getInlineFeatureType().getCoordinateReferenceSystem() == null) {
            LOGGER.warning("No CRS set on inline features default geometry.  Assuming the requestor has their inlinefeatures in the boundingbox CRS.");

            SimpleFeatureType currFt = ul.getInlineFeatureType();
            Query q = new Query(currFt.getTypeName(), Filter.INCLUDE);
            FeatureReader<SimpleFeatureType, SimpleFeature> ilReader;
            DataStore inlineFeatureDatastore = ul.getInlineFeatureDatastore();
            ilReader = inlineFeatureDatastore.getFeatureReader(q, Transaction.AUTO_COMMIT);
            CoordinateReferenceSystem crs = (getMapRequest.getCrs() == null) ? DefaultGeographicCRS.WGS84
                    : getMapRequest.getCrs();
            String typeName = inlineFeatureDatastore.getTypeNames()[0];
            MemoryDataStore reTypedDS = new MemoryDataStore(new ForceCoordinateSystemFeatureReader(
                    ilReader, crs));
            featureSource = reTypedDS.getFeatureSource(typeName);
        } else {
            DataStore inlineFeatureDatastore = ul.getInlineFeatureDatastore();
            String typeName = inlineFeatureDatastore.getTypeNames()[0];
            featureSource = inlineFeatureDatastore.getFeatureSource(typeName);
        }
        return new MapLayerInfo(featureSource);
    }

    /**
     * Returns the list of, possibly mixed, {@link MapLayerInfo} objects of a requested layer is a
     * registered {@link LayerInfo} or a remoteOWS one, or {@link LayerGroupInfo} objects for a
     * requested layer name that refers to a layer group.
     */
    protected List<?> parseLayers(final List<String> requestedLayerNames, final URL remoteOwsUrl,
            final String remoteOwsType) throws Exception {

        List<Object> layersOrGroups = new ArrayList<Object>();

        // Grab remote OWS data store if needed
        DataStore remoteWFS = null;
        final List<String> remoteTypeNames = new ArrayList<String>();
        if ("WFS".equals(remoteOwsType) && remoteOwsUrl != null) {
            remoteWFS = connectRemoteWFS(remoteOwsUrl);
            remoteTypeNames.addAll(Arrays.asList(remoteWFS.getTypeNames()));
            Collections.sort(remoteTypeNames);
        }

        // //
        // Layer lookup requires to:
        // * Look into the remote OWS first
        // * Look among the local layers
        // * expand local grouped layers (flatten them)
        // //
        for (String layerName : requestedLayerNames) {
            // search into the remote WFS if there is any
            if (remoteTypeNames.contains(layerName)) {
                SimpleFeatureSource remoteSource;
                remoteSource = remoteWFS.getFeatureSource(layerName);
                if (remoteSource != null) {
                    layersOrGroups.add(new MapLayerInfo(remoteSource));
                    continue;
                }
            }

            // not a remote layer, lets look up for a registered one
            LayerInfo layerInfo = wms.getLayerByName(layerName);
            if (layerInfo != null) {
                layersOrGroups.add(layerInfo);
            } else {
                LayerGroupInfo layerGroup = wms.getLayerGroupByName(layerName);
                if (layerGroup == null) {
                    throw new ServiceException("Could not find layer " + layerName,
                            "LayerNotDefined");
                }
                layersOrGroups.add(layerGroup);
            }
        }
        // pre GEOS-2652
        // Integer layerType = catalog.getLayerType(layerName);
        // if (layerType != null) {
        // layers.add(buildMapLayerInfo(layerName));
        // } else {
        // if(wms.getBaseMapLayers().containsKey(layerName)) {
        // layers.add(buildMapLayerInfo(layerName));
        // } else {
        // ////
        // // Search for grouped layers (attention: heavy process)
        // ////
        // boolean found = false;
        // String catalogLayerName = null;
        //
        // for (Iterator c_keys = catalog.getLayerNames().iterator(); c_keys.hasNext();) {
        // catalogLayerName = (String) c_keys.next();
        //
        // try {
        // FeatureTypeInfo ftype = findFeatureLayer(catalogLayerName);
        // String wmsPath = ftype.getWmsPath();
        //
        // if ((wmsPath != null) && wmsPath.matches(".*/" + layerName)) {
        // layers.add(buildMapLayerInfo(catalogLayerName));
        // found = true;
        // }
        // } catch (Exception e_1) {
        // try {
        // CoverageInfo cv = findCoverageLayer(catalogLayerName);
        // String wmsPath = cv.getWmsPath();
        //
        // if ((wmsPath != null) && wmsPath.matches(".*/" + layerName)) {
        // layers.add(buildMapLayerInfo(catalogLayerName));
        // found = true;
        // }
        // } catch (Exception e_2) {
        // }
        // }
        // }
        // if(!found)
        // throw new ServiceException("Could not find layer " + layerName,"LayerNotDefined");
        // }

        // }
        // }

        if (layersOrGroups.size() == 0) {
            throw new ServiceException("No LAYERS has been requested", getClass().getName());
        }
        return layersOrGroups;
    }

    private static DataStore connectRemoteWFS(URL remoteOwsUrl) throws ServiceException {
        try {
            WFSDataStoreFactory factory = new WFSDataStoreFactory();
            Map params = new HashMap(factory.getImplementationHints());
            params.put(WFSDataStoreFactory.URL.key, remoteOwsUrl
                    + "&request=GetCapabilities&service=WFS");
            params.put(WFSDataStoreFactory.TRY_GZIP.key, Boolean.TRUE);
            return factory.createDataStore(params);
        } catch (Exception e) {
            throw new ServiceException("Could not connect to remote OWS", e, "RemoteOWSFailure");
        }
    }

    // pre GEOS-2652:
    // private MapLayerInfo buildMapLayerInfo(String layerName) throws Exception {
    // MapLayerInfo li = new MapLayerInfo();
    //
    // FeatureTypeInfo ftype = findFeatureLayer(layerName);
    // if (ftype != null) {
    // li.setFeature(ftype);
    // } else {
    // CoverageInfo cv = findCoverageLayer(layerName);
    // if (cv != null) {
    // li.setCoverage(cv);
    // } else {
    // if (wms.getBaseMapLayers().containsKey(layerName)) {
    // String styleCsl = (String) wms.getBaseMapStyles().get(layerName);
    // String layerCsl = (String) wms.getBaseMapLayers().get(layerName);
    // MapLayerInfo[] layerArray = (MapLayerInfo[]) parseLayers(KvpUtils
    // .readFlat(layerCsl), null, null);
    // List styleList = (List) parseStyles(KvpUtils.readFlat(styleCsl));
    // li.setBase(layerName, new ArrayList(Arrays.asList(layerArray)), styleList);
    // } else {
    // throw new ServiceException("Layer " + layerName + " could not be found");
    // }
    // }
    // }
    // return li;
    // }

    // FeatureTypeInfo findFeatureLayer(String layerName) throws ServiceException {
    // FeatureTypeInfo ftype = null;
    // Integer layerType = catalog.getLayerType(layerName);
    //
    // if (Data.TYPE_VECTOR != layerType) {
    // return null;
    // } else {
    // ftype = catalog.getFeatureTypeInfo(layerName);
    // }
    //
    // return ftype;
    // }
    //
    // CoverageInfo findCoverageLayer(String layerName) throws ServiceException {
    // CoverageInfo cv = null;
    // Integer layerType = catalog.getLayerType(layerName);
    //
    // if (Data.TYPE_RASTER != layerType) {
    // return null;
    // } else {
    // cv = catalog.getCoverageInfo(layerName);
    // }
    //
    // return cv;
    // }

    protected List<Style> parseStyles(List<String> styleNames) throws Exception {
        List<Style> styles = new ArrayList<Style>();
        for (String styleName : styleNames) {
            if ("".equals(styleName)) {
                // return null, this should flag request reader to use default for
                // the associated layer
                styles.add(null);
            } else {
                final Style style = wms.getStyleByName(styleName);
                if (style == null) {
                    String msg = "No such style: " + styleName;
                    throw new ServiceException(msg, "StyleNotDefined");
                }
                styles.add(style);
            }
        }
        return styles;
    }

    /**
     * This flags allows the kvp reader to go beyond the SLD library mode specification and match
     * the first style that can be applied to a given layer. This is for backwards compatibility
     */
    public boolean isLaxStyleMatchAllowed() {
        return laxStyleMatchAllowed;
    }

    public void setLaxStyleMatchAllowed(boolean laxStyleMatchAllowed) {
        this.laxStyleMatchAllowed = laxStyleMatchAllowed;
    }
}
