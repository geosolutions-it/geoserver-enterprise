/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.event.impl.CatalogPostModifyEventImpl;
import org.geoserver.gwc.GWC;
import org.geowebcache.filter.parameters.ParameterFilter;
import org.geowebcache.filter.parameters.StringParameterFilter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class CatalogStyleChangeListenerTest extends TestCase {

    private final String STYLE_NAME = "highways";

    private String STYLE_NAME_MODIFIED = STYLE_NAME + "_modified";

    private final String PREFIXED_RESOURCE_NAME = "mock:Layer";

    private GWC mockMediator;

    private ResourceInfo mockResourceInfo;

    private LayerInfo mockLayerInfo;

    private StyleInfo mockStyle;

    private GeoServerTileLayer mockTileLayer;

    private GeoServerTileLayerInfoImpl mockTileLayerInfo;

    private CatalogModifyEventImpl styleNameModifyEvent;

    private CatalogStyleChangeListener listener;

    protected void setUp() throws Exception {
        mockMediator = mock(GWC.class);
        mockStyle = mock(StyleInfo.class);
        when(mockStyle.getName()).thenReturn(STYLE_NAME);

        mockResourceInfo = mock(FeatureTypeInfo.class);
        when(mockResourceInfo.prefixedName()).thenReturn(PREFIXED_RESOURCE_NAME);

        mockLayerInfo = mock(LayerInfo.class);
        when(mockLayerInfo.getResource()).thenReturn(mockResourceInfo);

        mockTileLayer = mock(GeoServerTileLayer.class);

        mockTileLayerInfo = mock(GeoServerTileLayerInfoImpl.class);
        ImmutableSet<String> empty = ImmutableSet.of();
        when(mockTileLayerInfo.cachedStyles()).thenReturn(empty);

        when(mockTileLayer.getLayerInfo()).thenReturn(mockLayerInfo);
        when(mockTileLayer.getInfo()).thenReturn(mockTileLayerInfo);
        when(mockTileLayer.getName()).thenReturn(PREFIXED_RESOURCE_NAME);
        when(mockMediator.getTileLayersForStyle(eq(STYLE_NAME))).thenReturn(
                Collections.singletonList(mockTileLayer));

        listener = new CatalogStyleChangeListener(mockMediator);

        styleNameModifyEvent = new CatalogModifyEventImpl();
        styleNameModifyEvent.setSource(mockStyle);
        styleNameModifyEvent.setPropertyNames(Arrays.asList("name"));
        styleNameModifyEvent.setOldValues(Arrays.asList(STYLE_NAME));
        styleNameModifyEvent.setNewValues(Arrays.asList(STYLE_NAME_MODIFIED));
    }

    public void testIgnorableChange() throws Exception {

        // not a name change
        styleNameModifyEvent.setPropertyNames(Arrays.asList("fileName"));
        listener.handleModifyEvent(styleNameModifyEvent);

        // name didn't change at all
        styleNameModifyEvent.setPropertyNames(Arrays.asList("name"));
        styleNameModifyEvent.setOldValues(Arrays.asList(STYLE_NAME));
        styleNameModifyEvent.setNewValues(Arrays.asList(STYLE_NAME));
        listener.handleModifyEvent(styleNameModifyEvent);

        // not a style change
        styleNameModifyEvent.setSource(mock(LayerInfo.class));
        listener.handleModifyEvent(styleNameModifyEvent);

        // a change in the name of the default style should not cause a truncate
        verify(mockMediator, never()).truncateByLayerAndStyle(anyString(), anyString());
        // nor a save, as the default style name is dynamic
        verify(mockMediator, never()).save((GeoServerTileLayer) anyObject());

        verify(mockTileLayer, never()).getInfo();
        verify(mockTileLayerInfo, never()).cachedStyles();
    }

    public void testRenameDefaultStyle() throws Exception {
        // this is another case of an ignorable change. Renaming the default style shall have no
        // impact.
        listener.handleModifyEvent(styleNameModifyEvent);
        // a change in the name of the default style should not cause a truncate
        verify(mockMediator, never()).truncateByLayerAndStyle(anyString(), anyString());
        // nor a save, as the default style name is dynamic
        verify(mockMediator, never()).save((GeoServerTileLayer) anyObject());

        verify(mockTileLayer, atLeastOnce()).getInfo();
        verify(mockTileLayerInfo, atLeastOnce()).cachedStyles();
    }

    public void testRenameAlternateStyle() throws Exception {

        Set<ParameterFilter> params = new HashSet<ParameterFilter>();
        when(mockTileLayerInfo.getParameterFilters()).thenReturn(params);
        TileLayerInfoUtil.setCachedStyles(mockTileLayerInfo, null, ImmutableSet.of(STYLE_NAME));
        assertEquals(1, params.size());

        ImmutableSet<String> styles = ImmutableSet.of(STYLE_NAME);
        when(mockTileLayerInfo.cachedStyles()).thenReturn(styles);

        listener.handleModifyEvent(styleNameModifyEvent);

        assertEquals(1, params.size());
        ParameterFilter updated = params.iterator().next();
        assertTrue(updated instanceof StringParameterFilter);
        assertEquals(Lists.newArrayList(STYLE_NAME_MODIFIED),
                ((StringParameterFilter) updated).getValues());

        verify(mockTileLayer, times(1)).resetParameterFilters();
        verify(mockMediator, times(1)).truncateByLayerAndStyle(eq(PREFIXED_RESOURCE_NAME),
                eq(STYLE_NAME));
        verify(mockMediator, times(1)).save(same(mockTileLayer));
    }

    @SuppressWarnings("unchecked")
    public void testLayerInfoDefaultOrAlternateStyleChanged() throws Exception {
        when(mockMediator.getLayerInfosFor(same(mockStyle))).thenReturn(
                Collections.singleton(mockLayerInfo));
        when(mockMediator.getLayerGroupsFor(same(mockStyle))).thenReturn(Collections.EMPTY_LIST);

        CatalogPostModifyEventImpl postModifyEvent = new CatalogPostModifyEventImpl();
        postModifyEvent.setSource(mockStyle);
        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockMediator, times(1)).truncateByLayerAndStyle(eq(PREFIXED_RESOURCE_NAME),
                eq(STYLE_NAME));
    }

    @SuppressWarnings("unchecked")
    public void testLayerGroupInfoImplicitOrExplicitStyleChanged() throws Exception {
        LayerGroupInfo mockGroup = mock(LayerGroupInfo.class);
        when(GWC.tileLayerName(mockGroup)).thenReturn("mockGroup");

        when(mockMediator.getLayerInfosFor(same(mockStyle))).thenReturn(Collections.EMPTY_LIST);
        when(mockMediator.getLayerGroupsFor(same(mockStyle))).thenReturn(
                Collections.singleton(mockGroup));

        CatalogPostModifyEventImpl postModifyEvent = new CatalogPostModifyEventImpl();
        postModifyEvent.setSource(mockStyle);
        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockMediator, times(1)).truncate(eq("mockGroup"));
    }
}
