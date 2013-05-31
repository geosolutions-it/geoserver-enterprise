/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.util.Locale;

import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * A form component for a {@link Geometry} object, expressed either as 2-3 space separated ordinates
 * or a WKT formatted {@link Geometry}
 * 
 * @author Andrea Aime, GeoSolutions
 */
public class GeometryTextArea extends TextArea<Geometry> {
    private static final long serialVersionUID = 1L;

    protected TextArea<String> geometry;

    public GeometryTextArea(String id) {
        this(id, new Model<Geometry>(null));
    }

    public GeometryTextArea(String id, Geometry g) {
        this(id, new Model<Geometry>(g));
    }

    public GeometryTextArea(String id, IModel<Geometry> model) {
        super(id, model);
        setType(Geometry.class);
    }

    @Override
    public IConverter getConverter(Class<?> type) {
        return new GeometryConverter();
    }

    /**
     * Converts between String and Geometry
     * 
     * @author Andrea Aime - GeoSolutions
     */
    private class GeometryConverter implements IConverter {
        private static final long serialVersionUID = 5868644160487841740L;
        
        transient GeometryFactory gf = new GeometryFactory();

        transient WKTReader reader = new WKTReader(gf);

        @Override
        public Object convertToObject(String value, Locale locale) {
            try {
                return reader.read(value);
            } catch (ParseException e) {
                try {
                    String[] values = value.split("\\s+");
                    if (values.length > 0 && values.length < 3) {
                        Coordinate c = new Coordinate();
                        c.x = Double.parseDouble(values[0]);
                        c.y = Double.parseDouble(values[1]);
                        return gf.createPoint(c);
                    }
                } catch (NumberFormatException nfe) {
                    // fall through
                }

                ConversionException ce = new ConversionException((String) null);
                ce.setResourceKey(GeometryTextArea.class.getSimpleName() + ".parseError");
                throw ce;
            }
        }

        @Override
        public String convertToString(Object value, Locale locale) {
            if (value instanceof Point) {
                Coordinate c = ((Point) value).getCoordinate();
                return c.x + " " + c.y;
            } else {
                return value.toString();
            }
        }

    }

}
