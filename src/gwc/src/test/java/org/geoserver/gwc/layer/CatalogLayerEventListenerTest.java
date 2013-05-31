/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.geoserver.gwc.GWC.tileLayerName;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.impl.CatalogAddEventImpl;
import org.geoserver.catalog.event.impl.CatalogRemoveEventImpl;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geowebcache.grid.GridSetBroker;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableSet;

/**
 * @author groldan
 * 
 */
public class CatalogLayerEventListenerTest extends TestCase {

    private static final String NAMESPACE_PREFIX = "mock";

    private static final String RESOURCE_NAME = "Layer";

    private static final String PREFIXED_RESOURCE_NAME = "mock:Layer";

    private static final String LAYER_GROUP_NAME = "LayerGroupName";

    private GWC mockMediator;

    private LayerInfo mockLayerInfo;

    private ResourceInfo mockResourceInfo;

    private NamespaceInfo mockNamespaceInfo;

    private LayerGroupInfo mockLayerGroupInfo;

    private CatalogLayerEventListener listener;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        GWCConfig configDefaults = GWCConfig.getOldDefaults();
        mockMediator = mock(GWC.class);
        when(mockMediator.getConfig()).thenReturn(configDefaults);

        GridSetBroker gridsets = new GridSetBroker(true, true);
        when(mockMediator.getGridSetBroker()).thenReturn(gridsets);

        mockLayerInfo = mock(LayerInfo.class);

        MetadataMap layerMetadata = new MetadataMap();
        when(mockLayerInfo.getMetadata()).thenReturn(layerMetadata);

        mockLayerGroupInfo = mock(LayerGroupInfo.class);
        MetadataMap groupMetadata = new MetadataMap();
        when(mockLayerGroupInfo.getMetadata()).thenReturn(groupMetadata);

        mockResourceInfo = mock(FeatureTypeInfo.class);
        mockNamespaceInfo = mock(NamespaceInfo.class);

        when(mockLayerGroupInfo.getName()).thenReturn(LAYER_GROUP_NAME);
        when(mockLayerGroupInfo.prefixedName()).thenReturn(LAYER_GROUP_NAME);
        when(mockResourceInfo.prefixedName()).thenReturn(PREFIXED_RESOURCE_NAME);
        when(mockResourceInfo.getName()).thenReturn(RESOURCE_NAME);
        when(mockResourceInfo.getNamespace()).thenReturn(mockNamespaceInfo);
        when(mockNamespaceInfo.getPrefix()).thenReturn(NAMESPACE_PREFIX);
        when(mockLayerInfo.getResource()).thenReturn(mockResourceInfo);

        listener = new CatalogLayerEventListener(mockMediator);
    }

    public void testLayerInfoAdded() throws Exception {
        CatalogAddEventImpl event = new CatalogAddEventImpl();
        event.setSource(mockLayerInfo);

        listener.handleAddEvent(event);

        verify(mockMediator).add(Mockito.any(GeoServerTileLayer.class));
    }

    public void testLayerGroupInfoAdded() throws Exception {

        CatalogAddEventImpl event = new CatalogAddEventImpl();
        event.setSource(mockLayerGroupInfo);

        listener.handleAddEvent(event);

        verify(mockMediator).add(Mockito.any(GeoServerTileLayer.class));
    }

    public void testLayerInfoRemoved() throws Exception {
        CatalogRemoveEventImpl event = new CatalogRemoveEventImpl();
        event.setSource(mockLayerInfo);

        when(mockMediator.hasTileLayer(same(mockLayerInfo))).thenReturn(true);
        listener.handleRemoveEvent(event);

        verify(mockMediator).removeTileLayers(eq(Arrays.asList(mockResourceInfo.prefixedName())));
    }

    public void testLayerGroupInfoRemoved() throws Exception {
        CatalogRemoveEventImpl event = new CatalogRemoveEventImpl();
        event.setSource(mockLayerGroupInfo);

        when(mockMediator.hasTileLayer(same(mockLayerGroupInfo))).thenReturn(true);
        listener.handleRemoveEvent(event);

        verify(mockMediator).removeTileLayers(
                eq(Arrays.asList(GWC.tileLayerName(mockLayerGroupInfo))));
    }

    public void testResourceInfoRenamed() throws Exception {

        final String oldTileLayerName = mockResourceInfo.prefixedName();
        final String renamedResouceName = RESOURCE_NAME + "_Renamed";
        final String renamedPrefixedResouceName = PREFIXED_RESOURCE_NAME + "_Renamed";

        // rename mockResourceInfo
        when(mockResourceInfo.getName()).thenReturn(renamedResouceName);
        when(mockResourceInfo.prefixedName()).thenReturn(renamedPrefixedResouceName);

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockResourceInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("name"));
        when(modifyEvent.getOldValues()).thenReturn(Arrays.asList((Object) RESOURCE_NAME));
        when(modifyEvent.getNewValues()).thenReturn(Arrays.asList((Object) renamedResouceName));

        GeoServerTileLayerInfo info = TileLayerInfoUtil.loadOrCreate(mockLayerInfo,
                GWCConfig.getOldDefaults());
        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        when(mockMediator.hasTileLayer(same(mockResourceInfo))).thenReturn(true);
        when(tileLayer.getInfo()).thenReturn(info);
        when(tileLayer.getLayerInfo()).thenReturn(mockLayerInfo);

        when(mockMediator.getTileLayer(same(mockResourceInfo))).thenReturn(tileLayer);
        when(mockMediator.getTileLayerByName(eq(oldTileLayerName))).thenReturn(tileLayer);

        listener.handleModifyEvent(modifyEvent);
        verify(mockMediator, times(1)).hasTileLayer(same(mockResourceInfo));

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockResourceInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        ArgumentCaptor<GeoServerTileLayer> captor = ArgumentCaptor
                .forClass(GeoServerTileLayer.class);
        verify(mockMediator).save(captor.capture());

        GeoServerTileLayer saved = captor.getValue();
        assertNotNull(saved);
        assertNotNull(saved.getInfo());
        GeoServerTileLayerInfo savedInfo = saved.getInfo();
        assertSame(info, savedInfo);
        assertEquals(renamedPrefixedResouceName, savedInfo.getName());
    }

    public void testLayerGroupInfoRenamed() throws Exception {
        final String oldGroupName = LAYER_GROUP_NAME;
        final String renamedGroupName = LAYER_GROUP_NAME + "_Renamed";

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockLayerGroupInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("name"));
        when(modifyEvent.getOldValues()).thenReturn(Arrays.asList((Object) LAYER_GROUP_NAME));
        when(modifyEvent.getNewValues()).thenReturn(Arrays.asList((Object) renamedGroupName));

        GeoServerTileLayerInfo info = TileLayerInfoUtil.loadOrCreate(mockLayerGroupInfo,
                GWCConfig.getOldDefaults());
        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        when(tileLayer.getInfo()).thenReturn(info);
        when(tileLayer.getLayerGroupInfo()).thenReturn(mockLayerGroupInfo);

        when(mockMediator.hasTileLayer(same(mockLayerGroupInfo))).thenReturn(true);
        when(mockMediator.getTileLayer(same(mockLayerGroupInfo))).thenReturn(tileLayer);
        when(mockMediator.getTileLayerByName(eq(oldGroupName))).thenReturn(tileLayer);

        // rename mockResourceInfo
        when(GWC.tileLayerName(mockLayerGroupInfo)).thenReturn(renamedGroupName);

        listener.handleModifyEvent(modifyEvent);

        verify(mockMediator, times(1)).hasTileLayer(same(mockLayerGroupInfo));
        verify(mockMediator, times(1)).getTileLayer(same(mockLayerGroupInfo));

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockLayerGroupInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        ArgumentCaptor<GeoServerTileLayer> captor = ArgumentCaptor
                .forClass(GeoServerTileLayer.class);
        verify(mockMediator).save(captor.capture());

        GeoServerTileLayer saved = captor.getValue();
        assertNotNull(saved);
        assertNotNull(saved.getInfo());
        GeoServerTileLayerInfo savedInfo = saved.getInfo();
        assertSame(info, savedInfo);
        assertEquals(renamedGroupName, savedInfo.getName());
    }

    public void testLayerGroupInfoRenamedDueToWorkspaceChanged() throws Exception {

        WorkspaceInfo workspace = mock(WorkspaceInfo.class);
        when(workspace.getName()).thenReturn("mockWs");

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockLayerGroupInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("workspace"));
        when(modifyEvent.getOldValues()).thenReturn(Arrays.asList((Object) null));
        when(modifyEvent.getNewValues()).thenReturn(Arrays.asList((Object) workspace));

        GeoServerTileLayerInfo info = TileLayerInfoUtil.loadOrCreate(mockLayerGroupInfo,
                GWCConfig.getOldDefaults());

        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        when(tileLayer.getInfo()).thenReturn(info);
        when(tileLayer.getLayerGroupInfo()).thenReturn(mockLayerGroupInfo);

        when(mockMediator.hasTileLayer(same(mockLayerGroupInfo))).thenReturn(true);
        when(mockMediator.getTileLayer(same(mockLayerGroupInfo))).thenReturn(tileLayer);
        final String oldLayerName = tileLayerName(mockLayerGroupInfo);
        when(mockMediator.getTileLayerByName(eq(oldLayerName))).thenReturn(tileLayer);

        listener.handleModifyEvent(modifyEvent);

        verify(mockMediator, times(1)).hasTileLayer(same(mockLayerGroupInfo));
        verify(mockMediator, times(1)).getTileLayer(same(mockLayerGroupInfo));

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockLayerGroupInfo);

        // change group workspace
        when(mockLayerGroupInfo.getWorkspace()).thenReturn(workspace);
        String prefixedName = workspace.getName() + ":" + mockLayerGroupInfo.getName();
        when(mockLayerGroupInfo.prefixedName()).thenReturn(prefixedName);

        listener.handlePostModifyEvent(postModifyEvent);

        ArgumentCaptor<GeoServerTileLayer> captor = ArgumentCaptor
                .forClass(GeoServerTileLayer.class);
        verify(mockMediator).save(captor.capture());

        GeoServerTileLayer saved = captor.getValue();
        assertNotNull(saved);
        assertNotNull(saved.getInfo());
        GeoServerTileLayerInfo savedInfo = saved.getInfo();
        assertSame(info, savedInfo);
        String tileLayerName = tileLayerName(mockLayerGroupInfo);
        String actual = savedInfo.getName();
        assertEquals(tileLayerName, actual);
    }

    public void testResourceInfoNamespaceChanged() throws Exception {
        NamespaceInfo newNamespace = mock(NamespaceInfo.class);
        when(newNamespace.getPrefix()).thenReturn("newMock");

        final String oldPrefixedName = mockResourceInfo.prefixedName();
        final String newPrefixedName = newNamespace.getPrefix() + ":" + mockResourceInfo.getName();

        // set the new namespace
        when(mockResourceInfo.getNamespace()).thenReturn(newNamespace);
        when(mockResourceInfo.prefixedName()).thenReturn(newPrefixedName);

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockResourceInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("namespace"));
        when(modifyEvent.getOldValues()).thenReturn(Arrays.asList((Object) mockNamespaceInfo));
        when(modifyEvent.getNewValues()).thenReturn(Arrays.asList((Object) newNamespace));

        GeoServerTileLayerInfo info = TileLayerInfoUtil.loadOrCreate(mockLayerInfo,
                GWCConfig.getOldDefaults());
        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        when(tileLayer.getInfo()).thenReturn(info);
        when(tileLayer.getLayerInfo()).thenReturn(mockLayerInfo);

        when(mockMediator.hasTileLayer(same(mockResourceInfo))).thenReturn(true);
        when(mockMediator.getTileLayer(same(mockResourceInfo))).thenReturn(tileLayer);
        when(mockMediator.getTileLayerByName(eq(oldPrefixedName))).thenReturn(tileLayer);

        listener.handleModifyEvent(modifyEvent);

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockResourceInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        ArgumentCaptor<GeoServerTileLayer> captor = ArgumentCaptor
                .forClass(GeoServerTileLayer.class);
        verify(mockMediator).save(captor.capture());

        GeoServerTileLayer saved = captor.getValue();
        assertNotNull(saved);
        assertNotNull(saved.getInfo());
        GeoServerTileLayerInfo savedInfo = saved.getInfo();
        assertSame(info, savedInfo);
        assertEquals(newPrefixedName, savedInfo.getName());
    }

    public void testLayerGroupInfoLayersChanged() throws Exception {
        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockLayerGroupInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("layers"));

        List<LayerInfo> oldLayers = Collections.emptyList();
        List<LayerInfo> newLayers = Collections.singletonList(mockLayerInfo);

        when(modifyEvent.getOldValues()).thenReturn(Collections.singletonList((Object) oldLayers));
        when(modifyEvent.getNewValues()).thenReturn(Collections.singletonList((Object) newLayers));

        // the tile layer must exist otherwise the event will be ignored
        GeoServerTileLayerInfo tileLayerInfo = TileLayerInfoUtil.loadOrCreate(mockLayerGroupInfo,
                mockMediator.getConfig());

        GridSetBroker gridsets = new GridSetBroker(true, true);
        GeoServerTileLayer tileLayer = new GeoServerTileLayer(mockLayerGroupInfo, gridsets,
                tileLayerInfo);

        when(mockMediator.hasTileLayer(same(mockLayerGroupInfo))).thenReturn(true);
        when(mockMediator.getTileLayer(same(mockLayerGroupInfo))).thenReturn(tileLayer);

        listener.handleModifyEvent(modifyEvent);

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockLayerGroupInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockMediator).truncate(eq(LAYER_GROUP_NAME));
    }

    public void testLayerGroupInfoStylesChanged() throws Exception {

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockLayerGroupInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("styles"));
        List<StyleInfo> oldStyles = Collections.emptyList();
        StyleInfo newStyle = mock(StyleInfo.class);
        List<StyleInfo> newStyles = Collections.singletonList(newStyle);
        when(modifyEvent.getOldValues()).thenReturn(Collections.singletonList((Object) oldStyles));
        when(modifyEvent.getNewValues()).thenReturn(Collections.singletonList((Object) newStyles));

        // the tile layer must exist on the layer metadata otherwise the event will be ignored
        GeoServerTileLayerInfo info = TileLayerInfoUtil.loadOrCreate(mockLayerGroupInfo,
                GWCConfig.getOldDefaults());
        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        when(tileLayer.getInfo()).thenReturn(info);

        when(mockMediator.hasTileLayer(same(mockLayerGroupInfo))).thenReturn(true);
        when(mockMediator.getTileLayer(same(mockLayerGroupInfo))).thenReturn(tileLayer);

        listener.handleModifyEvent(modifyEvent);

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockLayerGroupInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockMediator).truncate(eq(LAYER_GROUP_NAME));
    }

    public void testLayerInfoDefaultStyleChanged() throws Exception {
        final String oldName = "oldStyle";
        final String newName = "newStyle";

        StyleInfo oldStyle = mock(StyleInfo.class);
        when(oldStyle.getName()).thenReturn(oldName);
        StyleInfo newStyle = mock(StyleInfo.class);
        when(newStyle.getName()).thenReturn(newName);

        when(mockLayerInfo.getDefaultStyle()).thenReturn(newStyle);

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockLayerInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("defaultStyle"));
        when(modifyEvent.getOldValues()).thenReturn(Collections.singletonList((Object) oldStyle));
        when(modifyEvent.getNewValues()).thenReturn(Collections.singletonList((Object) newStyle));

        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        when(mockMediator.getTileLayerByName(eq(PREFIXED_RESOURCE_NAME))).thenReturn(tileLayer);

        // the tile layer must exist on the layer metadata otherwise the event will be ignored
        GeoServerTileLayerInfo info = TileLayerInfoUtil.loadOrCreate(mockLayerInfo,
                GWCConfig.getOldDefaults());
        when(tileLayer.getInfo()).thenReturn(info);

        when(mockMediator.hasTileLayer(same(mockLayerInfo))).thenReturn(true);
        when(mockMediator.getTileLayer(same(mockLayerInfo))).thenReturn(tileLayer);

        listener.handleModifyEvent(modifyEvent);

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);
        when(postModifyEvent.getSource()).thenReturn(mockLayerInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        verify(mockMediator).truncateByLayerAndStyle(eq(PREFIXED_RESOURCE_NAME), eq(oldName));
        verify(mockMediator).save(any(GeoServerTileLayer.class));
    }

    public void testLayerInfoAlternateStylesChanged() throws Exception {

        StyleInfo removedStyle = mock(StyleInfo.class);
        when(removedStyle.getName()).thenReturn("removedStyleName");

        StyleInfo remainingStyle = mock(StyleInfo.class);
        when(remainingStyle.getName()).thenReturn("remainingStyle");

        final Set<StyleInfo> oldStyles = new HashSet<StyleInfo>(Arrays.asList(remainingStyle,
                removedStyle));
        when(mockLayerInfo.getStyles()).thenReturn(oldStyles);

        StyleInfo addedStyle = mock(StyleInfo.class);
        when(addedStyle.getName()).thenReturn("addedStyleName");
        final Set<StyleInfo> newStyles = new HashSet<StyleInfo>(Arrays.asList(addedStyle,
                remainingStyle));

        CatalogModifyEvent modifyEvent = mock(CatalogModifyEvent.class);
        when(modifyEvent.getSource()).thenReturn(mockLayerInfo);
        when(modifyEvent.getPropertyNames()).thenReturn(Arrays.asList("styles"));
        when(modifyEvent.getOldValues()).thenReturn(Collections.singletonList((Object) oldStyles));
        when(modifyEvent.getNewValues()).thenReturn(Collections.singletonList((Object) newStyles));

        GeoServerTileLayerInfo info = mock(GeoServerTileLayerInfo.class);
        when(info.cachedStyles()).thenReturn(ImmutableSet.of("remainingStyle", "removedStyleName"));
        when(info.isAutoCacheStyles()).thenReturn(true);

        GeoServerTileLayer tileLayer = mock(GeoServerTileLayer.class);
        when(tileLayer.getInfo()).thenReturn(info);

        when(mockMediator.hasTileLayer(any(CatalogInfo.class))).thenReturn(true);
        when(mockMediator.getTileLayer(any(CatalogInfo.class))).thenReturn(tileLayer);

        listener.handleModifyEvent(modifyEvent);

        CatalogPostModifyEvent postModifyEvent = mock(CatalogPostModifyEvent.class);

        // the tile layer must exist on the layer metadata otherwise the event will be ignored
        when(mockLayerInfo.getStyles()).thenReturn(newStyles);
        when(postModifyEvent.getSource()).thenReturn(mockLayerInfo);

        listener.handlePostModifyEvent(postModifyEvent);

        // check removedStyleName was truncated
        verify(mockMediator).truncateByLayerAndStyle(eq(PREFIXED_RESOURCE_NAME),
                eq("removedStyleName"));
        // check no other style was truncated
        verify(mockMediator, atMost(1)).truncateByLayerAndStyle(anyString(), anyString());
        verify(mockMediator).save(any(GeoServerTileLayer.class));
    }
}
