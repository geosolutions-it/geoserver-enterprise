/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;


/**
 * A rolling color ramp with color names
 * @author Andrea Aime, GeoSolutions SAS
 *
 */
public class ColorRamp
{
    List<String> names = new ArrayList<String>();
    List<Color> colors = new ArrayList<Color>();
    int position;

    /**
     * Builds an empty ramp. Mind, you need to call {@link #add(String, Color)} at least
     * once to make the ramp usable.
     */
    public ColorRamp()
    {

    }

    /**
     * Adds a name/color combination
     * @param name
     * @param color
     */
    public void add(String name, Color color)
    {
        names.add(name);
        colors.add(color);
    }

    /**
     * Moves to the next color in the ramp
     */
    public void next()
    {
        position++;
        if (position >= names.size())
        {
            position = 0;
        }
    }

    /**
     * The color name
     * @return
     */
    public String getName()
    {
        return names.get(position);
    }

    /**
     * The color
     * @return
     */
    public Color getColor()
    {
        return colors.get(position);
    }

}
