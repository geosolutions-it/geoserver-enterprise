/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2004-2008, Open Source Geospatial Foundation (OSGeo)
 *    
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.raster.algebra;

import java.util.ArrayList;
import java.util.List;

import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.geotools.util.Converters;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.Not;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/**
 * Takes a filter and returns a simplified, equivalent one. At the moment the filter:
 * <ul>
 * <li>removes double logic negations</li>
 * </ul>
 * 
 * @author Andrea Aime - OpenGeo
 * @author Gabriel Roldan (OpenGeo)
 * @since 2.5.x
 * @version $Id$
 *
 *
 * @source $URL$
 */
public class SimplifyingFilterVisitor extends DuplicatingFilterVisitor implements FilterVisitor {

    FilterAttributeExtractor attributeExtractor;

    @Override
    public Object visit(And filter, Object extraData) {
        // scan, clone and simplify the children
        List<Filter> newChildren = new ArrayList<Filter>(filter.getChildren().size());
        for (Filter child : filter.getChildren()) {
            Filter cloned = (Filter) child.accept(this, extraData);
            
            // if any of the child filters is exclude, 
            // the whole chain of AND is equivalent to 
            // EXCLUDE
            if(cloned == Filter.EXCLUDE)
                return Filter.EXCLUDE;
            
            // these can be skipped
            if(cloned == Filter.INCLUDE)
                continue;
            
            newChildren.add(cloned);
        }
        
        // we might end up with an empty list
        if(newChildren.size() == 0)
            return Filter.INCLUDE;
        
        // remove the logic we have only one filter
        if(newChildren.size() == 1)
            return newChildren.get(0);
        
        // else return the cloned and simplified up list
        return getFactory(extraData).and(newChildren);
    }
    
    @Override
    public Object visit(Or filter, Object extraData) {
     // scan, clone and simplify the children
        List<Filter> newChildren = new ArrayList<Filter>(filter.getChildren().size());
        for (Filter child : filter.getChildren()) {
            Filter cloned = (Filter) child.accept(this, extraData);
            
            // if any of the child filters is include, 
            // the whole chain of OR is equivalent to 
            // INCLUDE
            if(cloned == Filter.INCLUDE)
                return Filter.INCLUDE;
            
            // these can be skipped
            if(cloned == Filter.EXCLUDE)
                continue;
            
            newChildren.add(cloned);
        }
        
        // we might end up with an empty list
        if(newChildren.size() == 0)
            return Filter.EXCLUDE;
        
        // remove the logic we have only one filter
        if(newChildren.size() == 1)
            return newChildren.get(0);
        
        // else return the cloned and simplified up list
        return getFactory(extraData).or(newChildren);
    }
    
    public Object visit(Not filter, Object extraData) {
        if (filter.getFilter() instanceof Not) {
            // simplify out double negation
            Not inner = (Not) filter.getFilter();
            return inner.getFilter().accept(this, extraData);
        } else {
            return super.visit(filter, extraData);
        }
    }

    /** 
     * Extracts single values.
     */
    @Override
    public Object visit(Literal expression, Object data) {
        return expression.getValue();
    }

    @Override
    public Object visit(PropertyIsBetween filter, Object data) {
        
        // parse values
        final String lowerS = (String) filter.getLowerBoundary().accept(this, data);
        final String upperS = (String) filter.getUpperBoundary().accept(this, data);
        final String name= (String) filter.getExpression().accept(this, data);
        
        // check values
        final double minValue=Double.parseDouble(lowerS);
        final double maxValue=Double.parseDouble(upperS);
        
        
        // TODO
        return null;
    }

    @Override
    public Object visit(PropertyIsEqualTo filter, Object data) {
    
        // extract from filters
        final String name= (String) filter.getExpression1().accept(this, data);
        final double value = Converters.convert(filter.getExpression2().accept(this, data),Double.class);
        
        // TODO
        return null;
    }

    @Override
    public Object visit(PropertyIsGreaterThan filter, Object data) {
        final String name= (String) filter.getExpression1().accept(this, data);
        final double value = Converters.convert(filter.getExpression2().accept(this, data),Double.class);        

        
        // TODO
        return null;
    }

    @Override
    public Object visit(PropertyIsGreaterThanOrEqualTo filter, Object data) {
        final String name= (String) filter.getExpression1().accept(this, data);
        final double value = Converters.convert(filter.getExpression2().accept(this, data),Double.class);        
        // TODO
        return null;
    }

    @Override
    public Object visit(PropertyIsLessThan filter, Object data) {
        final String name= (String) filter.getExpression1().accept(this, data);
        final double value = Converters.convert(filter.getExpression2().accept(this, data),Double.class);
        
        // TODO
        return null;
    }

    @Override
    public Object visit(PropertyIsLessThanOrEqualTo filter, Object data) {
        final String name= (String) filter.getExpression1().accept(this, data);
        final double value = Converters.convert(filter.getExpression2().accept(this, data),Double.class);
        
        // TODO
        return null;
    }

    @Override
    public Object visit(PropertyIsNotEqualTo filter, Object data) {
        
        // extract frm filters
        final String name= (String) filter.getExpression1().accept(this, data);
        final double value = Converters.convert(filter.getExpression2().accept(this, data),Double.class);
        
        // TODO
        return null;
    }

    /**
     * Collects coverage names
     */
    @Override
    public Object visit(PropertyName expression, Object data) {
        return expression.getPropertyName();
    }
    
    
    
}
