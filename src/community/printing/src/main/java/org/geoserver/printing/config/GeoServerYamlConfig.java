/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.geoserver.printing.config;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.geoserver.printing.config.blocks.ImageBlock;
import org.ho.util.BiDirectionalMap;
import org.ho.yaml.CustomYamlConfig;
import org.mapfish.print.config.AddressHostMatcher;
import org.mapfish.print.config.ColorWrapper;
import org.mapfish.print.config.CustomEnumWrapper;
import org.mapfish.print.config.DnsHostMatcher;
import org.mapfish.print.config.Key;
import org.mapfish.print.config.LocalHostMatcher;
import org.mapfish.print.config.layout.AttributesBlock;
import org.mapfish.print.config.layout.ColumnDefs;
import org.mapfish.print.config.layout.ColumnsBlock;
import org.mapfish.print.config.layout.Exceptions;
import org.mapfish.print.config.layout.HorizontalAlign;
import org.mapfish.print.config.layout.Layouts;
import org.mapfish.print.config.layout.LegendsBlock;
import org.mapfish.print.config.layout.MapBlock;
import org.mapfish.print.config.layout.ScalebarBlock;
import org.mapfish.print.config.layout.TextBlock;
import org.mapfish.print.config.layout.VerticalAlign;
import org.mapfish.print.scalebar.Direction;
import org.mapfish.print.scalebar.Type;
import org.mapfish.print.utils.DistanceUnit;

public class GeoServerYamlConfig extends CustomYamlConfig {
    public GeoServerYamlConfig() {
        Map<String, Object> handlers = new HashMap<String, Object>();
        handlers.put(Layouts.class.getName(), Layouts.Wrapper.class.getName());
        handlers.put(ColumnDefs.class.getName(), ColumnDefs.Wrapper.class.getName());
        handlers.put(Exceptions.class.getName(), Exceptions.Wrapper.class.getName());
        handlers.put(Color.class.getName(), ColorWrapper.class.getName());

        //special enum parser
        handlers.put(HorizontalAlign.class.getName(), CustomEnumWrapper.class.getName());
        handlers.put(VerticalAlign.class.getName(), CustomEnumWrapper.class.getName());
        handlers.put(Direction.class.getName(), CustomEnumWrapper.class.getName());
        handlers.put(Type.class.getName(), CustomEnumWrapper.class.getName());
        handlers.put(DistanceUnit.class.getName(), DistanceUnit.Wrapper.class.getName());

        setHandlers(handlers);

        BiDirectionalMap<String, String> transfers = new BiDirectionalMap<String, String>();

        //blocks
        transfers.put("text", TextBlock.class.getName());
        transfers.put("image", ImageBlock.class.getName());
        transfers.put("columns", ColumnsBlock.class.getName());
        transfers.put("table", ColumnsBlock.class.getName());
        transfers.put("map", MapBlock.class.getName());
        transfers.put("attributes", AttributesBlock.class.getName());
        transfers.put("scalebar", ScalebarBlock.class.getName());
        transfers.put("legends", LegendsBlock.class.getName());
        //transfers.put("features", FeaturesBlock.class.getName());

        //hosts matchers
        transfers.put("localMatch", LocalHostMatcher.class.getName());
        transfers.put("ipMatch", AddressHostMatcher.class.getName());
        transfers.put("dnsMatch", DnsHostMatcher.class.getName());

        transfers.put("key", Key.class.getName());

        setTransfers(transfers);
    }

}
