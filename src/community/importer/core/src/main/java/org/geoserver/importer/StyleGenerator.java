/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StyleInfo;
import org.opengis.feature.type.GeometryDescriptor;


/**
 * Generates pseudo random styles using a specified color ramp
 *
 * @author Andrea Aime, GeoSolutions
 *
 */
public class StyleGenerator
{

    static final Map<GeometryType, String> TEMPLATES = new HashMap<GeometryType, String>();

    static
    {
        try
        {
            TEMPLATES.put(GeometryType.POINT, IOUtils.toString(StyleGenerator.class.getResourceAsStream("template_point.sld")));
            TEMPLATES.put(GeometryType.POLYGON, IOUtils.toString(StyleGenerator.class.getResourceAsStream("template_polygon.sld")));
            TEMPLATES.put(GeometryType.LINE, IOUtils.toString(StyleGenerator.class.getResourceAsStream("template_line.sld")));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Error loading up the style templates", e);
        }
    }

    static enum GeometryType
    {
        POINT,
        LINE,
        POLYGON
    }

    private ColorRamp ramp;

    private Catalog catalog;

    /**
     * Builds a style generator with the default color ramp
     * @param catalog
     */
    public StyleGenerator(Catalog catalog)
    {
        this.catalog = catalog;
        ramp = new ColorRamp();
        ramp.add("red", Color.decode("0xFF3300"));
        ramp.add("orange", Color.decode("0xFF6600"));
        ramp.add("dark orange", Color.decode("0xFF9900"));
        ramp.add("gold", Color.decode("0xFFCC00"));
        ramp.add("yellow", Color.decode("0xFFFF00"));
        ramp.add("dark yellow", Color.decode("0x99CC00"));
        ramp.add("teal", Color.decode("0x00CC33"));
        ramp.add("cyan", Color.decode("0x0099CC"));
        ramp.add("azure", Color.decode("0x0033CC"));
        ramp.add("violet", Color.decode("0x3300FF"));
    }

    public StyleGenerator(Catalog catalog, ColorRamp ramp)
    {
        if (ramp == null)
        {
            throw new NullPointerException("The color ramp cannot be null");
        }

        this.ramp = ramp;
        this.catalog = catalog;
    }

    public StyleInfo getStyle(FeatureTypeInfo featureType) throws IOException
    {
        // move to the next color in the ramp
        String colorName = ramp.getName();
        Color color = ramp.getColor();
        ramp.next();

        // geometryless, style it randomly
        GeometryDescriptor gd = featureType.getFeatureType().getGeometryDescriptor();
        if (gd == null)
        {
            return catalog.getStyleByName(StyleInfo.DEFAULT_POINT);
        }

        Class gtype = gd.getType().getBinding();
        GeometryType gt;
        if (LineString.class.isAssignableFrom(gtype) ||
                MultiLineString.class.isAssignableFrom(gtype))
        {
            gt = GeometryType.LINE;
        }
        else if (Polygon.class.isAssignableFrom(gtype) ||
                MultiPolygon.class.isAssignableFrom(gtype))
        {
            gt = GeometryType.POLYGON;
        }
        else
        {
            gt = GeometryType.POINT;
        }

        // find a new style name
        String styleName = featureType.getStore().getWorkspace().getName() + "_" + featureType.getName();
        StyleInfo style = catalog.getStyleByName(styleName);
        int i = 1;
        while (style != null)
        {
            styleName = featureType.getStore().getWorkspace().getName() + "_" + featureType.getName() + i;
            style = catalog.getStyleByName(styleName);
            i++;
        }

        // variable replacement
        String colorCode = Integer.toHexString(color.getRGB());
        colorCode = colorCode.substring(2, colorCode.length());

        String sld = TEMPLATES.get(gt).replace("${colorName}", colorName).replace(
                "${colorCode}", "#" + colorCode);

        // let's store it
        style = catalog.getFactory().createStyle();
        style.setName(styleName);
        style.setFilename(styleName + ".sld");
        catalog.getResourcePool().writeStyle(style, new ByteArrayInputStream(sld.getBytes()));
        catalog.add(style);

        return style;
    }

}
