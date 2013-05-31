/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.config;

import static org.geoserver.gwc.GWCTestHelpers.mockGroup;
import static org.geoserver.gwc.GWCTestHelpers.mockLayer;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl;
import org.geoserver.gwc.layer.LegacyTileLayerInfoLoader;
import org.geoserver.gwc.layer.TileLayerCatalog;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSInfoImpl;
import org.mockito.ArgumentCaptor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class GWCInitializerTest extends TestCase {

    private GWCInitializer initializer;

    private GWCConfigPersister configPersister;

    private GeoServer geoServer;

    private Catalog rawCatalog;

    private TileLayerCatalog tileLayerCatalog;

    @Override
    protected void setUp() throws Exception {

        configPersister = mock(GWCConfigPersister.class);
        when(configPersister.getConfig()).thenReturn(GWCConfig.getOldDefaults());

        rawCatalog = mock(Catalog.class);
        tileLayerCatalog = mock(TileLayerCatalog.class);
        initializer = new GWCInitializer(configPersister, rawCatalog, tileLayerCatalog);

        geoServer = mock(GeoServer.class);

    }

    public void testInitializeLayersToOldDefaults() throws Exception {
        // no gwc-gs.xml exists
        when(configPersister.findConfigFile()).thenReturn(null);
        // ignore the upgrade of the direct wms integration flag on this test
        when(geoServer.getService(eq(WMSInfo.class))).thenReturn(null);

        // let the catalog have something to initialize
        LayerInfo layer = mockLayer("testLayer", new String[]{}, LayerInfo.Type.RASTER);
        LayerGroupInfo group = mockGroup("testGroup", layer);
        when(rawCatalog.getLayers()).thenReturn(Lists.newArrayList(layer));
        when(rawCatalog.getLayerGroups()).thenReturn(Lists.newArrayList(group));

        // run layer initialization
        initializer.initialize(geoServer);

        // make sure default tile layers were created
        GWCConfig oldDefaults = GWCConfig.getOldDefaults();
        GeoServerTileLayerInfo tileLayer = TileLayerInfoUtil.loadOrCreate(layer, oldDefaults);
        GeoServerTileLayerInfo tileLayerGroup = TileLayerInfoUtil.loadOrCreate(group, oldDefaults);

        verify(tileLayerCatalog, times(1)).save(eq(tileLayer));
        verify(tileLayerCatalog, times(1)).save(eq(tileLayerGroup));
    }

    public void testUpgradeDirectWMSIntegrationFlag() throws Exception {
        // no gwc-gs.xml exists, so that initialization runs
        when(configPersister.findConfigFile()).thenReturn(null);

        // no catalog layers for this test
        List<LayerInfo> layers = ImmutableList.of();
        List<LayerGroupInfo> groups = ImmutableList.of();
        when(rawCatalog.getLayers()).thenReturn(layers);
        when(rawCatalog.getLayerGroups()).thenReturn(groups);

        WMSInfoImpl wmsInfo = new WMSInfoImpl();
        // initialize wmsInfo with a value for the old direct wms integration flag
        wmsInfo.getMetadata().put(GWCInitializer.WMS_INTEGRATION_ENABLED_KEY, Boolean.TRUE);

        // make sure WMSInfo exists
        when(geoServer.getService(eq(WMSInfo.class))).thenReturn(wmsInfo);

        ArgumentCaptor<GWCConfig> captor = ArgumentCaptor.forClass(GWCConfig.class);
        // run layer initialization
        initializer.initialize(geoServer);

        verify(configPersister, times(2)).save(captor.capture());
        assertTrue(captor.getAllValues().get(0).isDirectWMSIntegrationEnabled());

        assertFalse(wmsInfo.getMetadata().containsKey(GWCInitializer.WMS_INTEGRATION_ENABLED_KEY));
        verify(geoServer).save(same(wmsInfo));

    }

    public void testUpgradeFromTileLayerInfosToTileLayerCatalog() throws Exception {
        // do have gwc-gs.xml, so it doesn't go through the createDefaultTileLayerInfos path
        File fakeConfig = new File("target", "gwc-gs.xml");
        when(configPersister.findConfigFile()).thenReturn(fakeConfig);

        GWCConfig defaults = GWCConfig.getOldDefaults();
        defaults.setCacheLayersByDefault(true);
        when(configPersister.getConfig()).thenReturn(defaults);

        // let the catalog have something to initialize
        LayerInfo layer = mockLayer("testLayer", new String[]{}, LayerInfo.Type.RASTER);
        LayerGroupInfo group = mockGroup("testGroup", layer);
        when(rawCatalog.getLayers()).thenReturn(Lists.newArrayList(layer));
        when(rawCatalog.getLayerGroups()).thenReturn(Lists.newArrayList(group));

        GeoServerTileLayerInfoImpl layerInfo = TileLayerInfoUtil.loadOrCreate(layer, defaults);
        GeoServerTileLayerInfoImpl groupInfo = TileLayerInfoUtil.loadOrCreate(group, defaults);

        LegacyTileLayerInfoLoader.save(layerInfo, layer.getMetadata());
        LegacyTileLayerInfoLoader.save(groupInfo, group.getMetadata());

        // run layer initialization
        initializer.initialize(geoServer);

        verify(tileLayerCatalog, times(1)).save(eq(layerInfo));
        assertFalse(LegacyTileLayerInfoLoader.hasTileLayerDef(layer.getMetadata()));
        verify(rawCatalog, times(1)).save(eq(layer));

        verify(tileLayerCatalog, times(1)).save(eq(groupInfo));
        assertFalse(LegacyTileLayerInfoLoader.hasTileLayerDef(group.getMetadata()));
        verify(rawCatalog, times(1)).save(eq(group));

    }
}
